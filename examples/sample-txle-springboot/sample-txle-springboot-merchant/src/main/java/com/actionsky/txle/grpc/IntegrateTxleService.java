/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IntegrateTxleService {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrateTxleService.class);

    private TxleTransactionServiceGrpc.TxleTransactionServiceStub stubService;
    private TxleTransactionServiceGrpc.TxleTransactionServiceBlockingStub stubBlockingService;
    private TxleGrpcServerStreamObserver serverStreamObserver;
    private StreamObserver<TxleGrpcClientStream> clientStreamObserver;
    private String dbSchema;
    private Map<String, String> dbMD5InfoMap = new HashMap<>();

    @Value("${alpha.cluster.address:127.0.0.1:8080}")
    private String txleGrpcServerAddress;

    @Autowired
    private CustomRepository customRepository;

    @PostConstruct
    void init() {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(txleGrpcServerAddress).usePlaintext()/*.maxInboundMessageSize(10 * 1024 * 1024)*/.build();
        this.stubService = TxleTransactionServiceGrpc.newStub(channel);
        this.stubBlockingService = TxleTransactionServiceGrpc.newBlockingStub(channel);
        this.serverStreamObserver = new TxleGrpcServerStreamObserver(this);

        this.onInitialize();
        this.initDBSchema();
        new Thread(() -> this.onSynDatabaseFullDose()).start();
    }

    private void onInitialize() {
        this.clientStreamObserver = stubService.onBuildBidirectionalStream(this.serverStreamObserver);
        serverStreamObserver.setClientStreamObserver(clientStreamObserver);
    }

    private void initDBSchema() {
        List list = this.customRepository.executeQuery("select database()");
        if (list != null && !list.isEmpty()) {
            this.dbSchema = list.get(0).toString();
        }
    }

    public String getTxleGrpcServerAddress() {
        return txleGrpcServerAddress;
    }

    public CustomRepository getCustomRepository() {
        return this.customRepository;
    }

    public void onReconnect() {
        this.onInitialize();
    }

    public boolean executeSqlUnderTxleTransaction(String globalTxId, boolean isCanOver, String localTxId, String sql, int timeout, int retries) {
        boolean result = this.onStartTransaction(globalTxId, localTxId, sql, timeout, retries);
        return this.onEndTransaction(globalTxId, isCanOver, localTxId, result);
    }

    private boolean onStartTransaction(String globalTxId, String localTxId, String sql, int timeout, int retries) {
        boolean isSuccessful = true;
        try {
            TxleTransactionStart.Builder startTx = TxleTransactionStart.newBuilder().setServiceName("sample-txle-springboot-user").setServiceIP("0.0.0.0").setGlobalTxId(globalTxId).setTimeout(0);
            startTx.addSubTxInfo(TxleSubTransactionStart.newBuilder().setSql(sql).setLocalTxId(localTxId).setDbNodeId("10.186.62.75").setDbSchema(dbSchema).setTimeout(timeout).setRetries(retries).build());

            TxleTxStartAck startAck = stubBlockingService.onStartTransaction(startTx.build());
            switch (startAck.getStatus().ordinal()) {
                case TxleTxStartAck.TransactionStatus.NORMAL_VALUE:
                    LOG.info("Successfully started global transaction [" + globalTxId + "].");
                    startAck.getSubTxSqlList().forEach(subSql -> customRepository.executeSubTxSqls(subSql.getSubTxSqlList()));
                    break;
                case TxleTxStartAck.TransactionStatus.ABORTED_VALUE:
                    isSuccessful = false;
                    LOG.error("Occur an exception when starting global transaction [" + globalTxId + "].");
                default:
                    break;
            }
        } catch (Exception e) {
            isSuccessful = false;
            LOG.error("Occur an exception when starting global transaction [{}].", globalTxId, e);
        }
        return isSuccessful;
    }

    private boolean onEndTransaction(String globalTxId, boolean isCanOver, String localTxId, boolean isSuccessful) {
        boolean isFinalSuccessful = true;
        try {
            TxleTransactionEnd.Builder transaction = TxleTransactionEnd.newBuilder().setIsCanOver(isCanOver).setGlobalTxId(globalTxId)
                    .addSubTxInfo(TxleSubTransactionEnd.newBuilder().setLocalTxId(localTxId).setIsSuccessful(isSuccessful).build());

            TxleTxEndAck txEndAck = stubBlockingService.onEndTransaction(transaction.build());
            if (TxleTxEndAck.TransactionStatus.ABORTED.equals(txEndAck.getStatus())) {
                isFinalSuccessful = false;
                LOG.error("Occur an exception when ending global transaction [" + globalTxId + "].");
            } else {
                LOG.info("Successfully started global transaction [" + globalTxId + "].");
            }
        } catch (Exception e) {
            isFinalSuccessful = false;
            LOG.error("Occur an exception when ending global transaction [{}].", globalTxId, e);
        }
        return isFinalSuccessful;
    }

    public void onSynDatabaseFullDose() {
        try {
            long timestamp = System.currentTimeMillis();
            this.synDatabase(true, timestamp, dbSchema, "txle_sample_merchant");

            // 全量同步成功后，才启动增量同步
            this.onSynDatabase();
        } catch (RuntimeException e) {
            LOG.error("Failed to synchronize the full-dose data.", e);
            throw new RuntimeException(e);
        }
    }

    public void onSynDatabase() {
        while (true) {
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long timestamp = System.currentTimeMillis();
            this.synDatabase(false, timestamp, dbSchema, "txle_sample_merchant");
        }
    }

    private void synDatabase(boolean isFullDose, long timestamp, String dbSchema, String tableName) {
        // 模拟第三方简易程序，此处仅检测两张表，而非整库
        String tempMD5Info = getMD5Digest(dbSchema, tableName), tableNameWithSchema = dbSchema + "." + tableName;
        String dbMD5Info = dbMD5InfoMap.get(tableNameWithSchema);
        if (dbMD5Info == null || !dbMD5Info.equals(tempMD5Info)) {
            System.err.println("数据库表" + dbSchema + "." + tableName + "表结构发生变化。。。。" + System.currentTimeMillis());

            TxleBusinessDBInfo.Builder databaseSetBuilder = TxleBusinessDBInfo.newBuilder();
            TxleBusinessTable.Builder table = TxleBusinessTable.newBuilder().setName(tableName);
            String sql = "SELECT column_name, data_type, column_key FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
            List columnList = this.customRepository.executeQuery(sql, dbSchema, tableName);

            if (columnList != null && !columnList.isEmpty()) {
                columnList.forEach(column -> {
                    Object[] columns = (Object[]) column;
                    table.addField(TxleBusinessField.newBuilder().setName(columns[0].toString()).setType(columns[1].toString()).setIsPrimaryKey("PRI".equals(columns[2])).build());
                });
            }

            TxleBusinessDatabase database0 = TxleBusinessDatabase.newBuilder().setName(dbSchema).addTable(0, table.build()).build();
            TxleBusinessNode node = TxleBusinessNode.newBuilder().setId("10.186.62.75").addDatabase(0, database0).build();

            databaseSetBuilder.addNode(node);
            databaseSetBuilder.setTimestamp(timestamp);
            databaseSetBuilder.setIsFullDose(isFullDose);

            TxleBasicAck txleBasicAck = stubBlockingService.onSynDatabase(databaseSetBuilder.build());
            System.err.println("syn " + dbSchema + " - txleBasicAck: received = " + txleBasicAck.getIsReceived() + ", result = " + txleBasicAck.getIsSuccessful());
            // txle端同步成功才赋值，若同步不成功，则不赋值即后续还会尝试本次同步
            if (txleBasicAck.getIsSuccessful()) {
                dbMD5InfoMap.put(tableNameWithSchema, tempMD5Info);
            }
        }
    }

    private String getMD5Digest(String dbSchema, String tableName) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            List dbInfoList = customRepository.executeQuery("SELECT table_name, column_name, column_default, is_nullable, column_type, extra FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + dbSchema + "' AND TABLE_NAME = '" + tableName + "'");
            md5.update(convertToByteFromList(dbInfoList));

            byte[] digest = md5.digest();
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                int v = digest[i] & 0xFF;
                if (v < 16) {
                    hex.append(0);
                }
                hex.append(Integer.toString(v, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Failed to get MD5 info for database. schema = " + dbSchema);
    }

    private byte[] convertToByteFromList(List list) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(byteOut);
            for (Object obj : list) {
                out.writeObject(obj);
            }
            return byteOut.toByteArray();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                byteOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
