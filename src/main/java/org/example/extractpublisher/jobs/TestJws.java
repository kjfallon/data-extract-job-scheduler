package org.example.extractpublisher.jobs;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.example.extractpublisher.components.JwtUtils;
import org.example.extractpublisher.entities.JwtMessage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class TestJws {

    @Autowired
    JwtUtils jwtUtils;

    @Value("${java.keystore.private.key.alias}")
    private String privateKeyAlias;

    // scheduled to run every 1 minutes
    @Scheduled(cron="0 */1 * * * ?")
    public void run() {

        // Create
        log.info("Creating JWE");
        String signingKeyId = privateKeyAlias;
        String recipientKeyId = privateKeyAlias;
        Integer minutesUntilExpiry = 10;
        String issuer = signingKeyId;
        String subject = "ThisIsTheSubject";
        List<String> audienceList = new ArrayList<String>();
        audienceList.add("Extract-Publisher");
        List<Pair<String, Object>> customClaimList = new ArrayList<Pair<String, Object>>();
        Pair<String, Object> customClaim = new ImmutablePair<>("command","no-op");
        customClaimList.add(customClaim);
        String jwe = jwtUtils.createSignedJweWithCustomClaims(issuer, subject, audienceList, signingKeyId, recipientKeyId, minutesUntilExpiry, customClaimList);

        // Validate
        log.info("Validating JWE");
        JwtMessage jwtMessage = jwtUtils.readJwtMessageFromJwe(jwe,"");
        log.info("JWS signature valid? : " + jwtMessage.getSignatureValid());
        jwtUtils.printJwsHeader(jwtMessage.getHeader());
        jwtUtils.printClaims(jwtMessage.getClaims());

    }



}