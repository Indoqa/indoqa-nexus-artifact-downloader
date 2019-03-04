/*
 * Licensed to the Indoqa Software Design und Beratung GmbH (Indoqa) under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Indoqa licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.indoqa.nexus.downloader.client.configuration;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Optional;

public class Hostname {

    private static final String LOCALHOST = "localhost";

    public static Optional<String> getHostname() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String name = localHost.getCanonicalHostName();
            if (!isNoHostname(name)) {
                return Optional.of(name);
            }
            name = localHost.getHostName();
            if (!isNoHostname(name)) {
                return Optional.of(name);
            }

            name = System.getenv("HOSTNAME");
            if (!isNoHostname(name)) {
                return Optional.of(name);
            }
            name = System.getenv("COMPUTERNAME");
            if (!isNoHostname(name)) {
                return Optional.of(name);
            }

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() || inetAddress.isLinkLocalAddress()) {
                        continue;
                    }
                    String canonicalHostName = inetAddress.getCanonicalHostName();
                    if (!isNoHostname(canonicalHostName)) {
                        return Optional.of(canonicalHostName);
                    }
                    String hostName = inetAddress.getHostName();
                    if (!isNoHostname(hostName)) {
                        return Optional.of(hostName);
                    }
                }
            }
        } catch (Exception e) {
            //ignore
        }
        return Optional.empty();
    }

    private static boolean isNoHostname(String name) {
        if (LOCALHOST.equalsIgnoreCase(name)) {
            return true;
        }
        return name.indexOf(':') > 0;
    }

}
