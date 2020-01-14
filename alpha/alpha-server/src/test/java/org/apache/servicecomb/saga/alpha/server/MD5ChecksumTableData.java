/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package org.apache.servicecomb.saga.alpha.server;

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gannalyo
 * @date 2020/1/10
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"server.port=8090"})
public class MD5ChecksumTableData {

    @Autowired
    private TxEventEnvelopeRepository repository;

    public static void main(String[] args) {
        String a = "4d c9 68 ff 0e e3 5c 20 95 72 d4 77 7b 72 15 87 d3 6f a7 b2 1b dc 56 b7 4a 3d c0 78 3e 7b 95 18 af bf a2 00 a8 28 4b f3 6e 8e 4b 55 b3 5f 42 75 93 d8 49 67 6d a0 d1 55 5d 83 60 fb 5f 07 fe a2";
        String b = "4d c9 68 ff 0e e3 5c 20 95 72 d4 77 7b 72 15 87 d3 6f a7 b2 1b dc 56 b7 4a 3d c0 78 3e 7b 95 18 af bf a2 02 a8 28 4b f3 6e 8e 4b 55 b3 5f 42 75 93 d8 49 67 6d a0 d1 d5 5d 83 60 fb 5f 07 fe a2";
        md5(a);
        md5(b);
    }

    private static void md5(String a) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(a.getBytes());
            byte[] digest = md5.digest();
            StringBuilder secpwd = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                int v = digest[i] & 0xFF;
                if (v < 16) secpwd.append(0);
                secpwd.append(Integer.toString(v, 16));
            }
            System.err.println(secpwd.toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void compare2TableDataByMD5() {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            int pageSize = 10;
            long count = repository.count();
            for (int i = 0; i < count; i++) {
                List<TxEvent> txList = repository.findTxList(new PageRequest(i, pageSize));
                if (txList == null || txList.isEmpty()) {
                    break;
                }
//                txList.forEach(event -> System.out.println(event.id()));
                md5.update(convertToByteFromList(txList));
            }

            byte[] digest = md5.digest();
            StringBuilder secpwd = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                int v = digest[i] & 0xFF;
                if (v < 16) secpwd.append(0);
                secpwd.append(Integer.toString(v, 16));
            }
            System.err.println(secpwd.toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] convertToByteFromList(List<TxEvent> list) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(byteOut);
            for (TxEvent event : list) {
                out.writeObject(event);
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

    private List<Map<String, Object>> convertToObject(byte[] byteArr) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(byteArr);
            ois = new ObjectInputStream(bis);
            while (bis.available() > 0) {
                list.add((Map<String, Object>) ois.readObject());
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        list.forEach(map -> {
            map.keySet().forEach(k -> {
                System.out.println(k + " = " + map.get(k));
            });
        });
        return list;
    }
}
