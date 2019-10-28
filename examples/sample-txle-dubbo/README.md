# sample-txle-dubbo

tx示例工程：基于Dubbo框架的业务工程。
consumer为主业务工程；
transfer、user和merchant为子业务工程；

部署步骤
1.下载txle服务工程并成功启动
2.修改consumer、transfer、user、merchant工程的配置文件中数据库连接信息、alpha.cluster.address地址为txle服务工程地址，其它配置可酌情调整
3.启动全部工程后可依据下面地址进行测试(ip、port自行调整)，其它场景依据需要逐步开发
    手动补偿成功场景测试：http://localhost:8000/testGlobalTransaction/1/100/1
    手动补偿失败场景测试：http://localhost:8000/testGlobalTransaction/1/1000/1
    自动补偿成功场景测试：http://localhost:8000/testGlobalTransactionAuto/1/100/1
    自动补偿失败场景测试：http://localhost:8000/testGlobalTransactionAuto/1/1000/1