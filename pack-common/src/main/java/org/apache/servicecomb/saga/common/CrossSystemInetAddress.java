/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package org.apache.servicecomb.saga.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

public final class CrossSystemInetAddress {
    private static final Logger LOG = LoggerFactory.getLogger(CrossSystemInetAddress.class);

    private CrossSystemInetAddress() {
    }

    public static String readCrossSystemIPv4() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

//        InetAddress inetAddress = readCrossSystemIPv4Address();
//        if (inetAddress != null) {
//            String ipv4 = inetAddress.getHostAddress();
//            LOG.info(TxleConstants.logDebugPrefixWithTime() + "Read system ipv4 - " + ipv4);
//            return ipv4;
//        }
        return "unknown ip address";
    }

    public static InetAddress readCrossSystemIPv4Address() {
        try {
            Enumeration networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) networkInterfaces.nextElement();
                String name = ni.getName();
                if (!name.contains("docker") && !name.contains("lo")) {
                    Enumeration ipEnum = ni.getInetAddresses();
                    while (ipEnum.hasMoreElements()) {
                        InetAddress addr = (InetAddress) ipEnum.nextElement();
                        if (!addr.isLinkLocalAddress() && !addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            return addr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Failed to get the realistic address of current system.", e);
        }
        return null;
    }

}
