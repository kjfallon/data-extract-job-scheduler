package org.example.extractpublisher.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Service
public class AuditLogger {

    @Value("${java.keystore.private.key.alias}")
    private String privateKeyAlias;

    // The Logback configuration sends log events from this Class to the the Syslog appender where they can be provided
    // to Spunk, ELK, etc.

    public void syslog(String appAction, String appActionAttribute1, String appActionAttribute2, String appReason, String appStatus) {

        String processUserName = System.getProperty("user.name");
        InetAddress ip;
        String hostname ="";
        try {
            ip = InetAddress.getLocalHost();
            hostname = ip.getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        log.info("app_name=\"" + privateKeyAlias + "\"," +
                "app_host_name=\"" + hostname + "\"," +
                "app_process_user=\"" + processUserName + "\"," +
                "app_action=\"" + appAction + "\"," +
                "app_action_attr1=\"" + appActionAttribute1 + "\"," +
                "app_action_attr2=\"" + appActionAttribute2 + "\"," +
                "app_reason=\"" + appReason + "\"," +
                "app_status=\"" +appStatus + "\"");
    }

}
