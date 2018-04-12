package main;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

final class ChatConstants {
    private static InetAddress INET_ADDRESS;
    private static final int PORT = 8283;

    private ChatConstants() {
    }

    private static final Logger log = Logger.getLogger(ChatConstants.class);

    static InetAddress getLocalHost() {
        try {
            INET_ADDRESS = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            log.error("Error with getting local host address!");
        }
        return INET_ADDRESS;
    }

    static int getDefaultPort() {
        return PORT;
    }
}
