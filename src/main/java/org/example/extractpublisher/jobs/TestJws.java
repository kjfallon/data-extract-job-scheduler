package org.example.extractpublisher.jobs;

import org.example.extractpublisher.components.JwtUtils;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TestJws {

    @Autowired
    JwtUtils jwtUtils;

    // scheduled to run every 1 minutes
    //@Scheduled(cron="0 */1 * * * ?")
    public void run() {

        log.info("Creating signed JWT...");
        String signingKeyId = "extract-publisher-application";
        Integer minutesUntilExpiry = 10;
        String issuer = signingKeyId;
        String subject = "ThisIsTheSubject";
        String audience = "Extract-Publisher";
        // custom claims
        Claims claims = Jwts.claims();
        claims.put("command", "something");

        String jws = jwtUtils.createRsaSignedJwtWithClaims(issuer, subject, audience, signingKeyId, minutesUntilExpiry, claims);

        validateJwsSignature(jws);
        validateJwsAndDisplay(jws);

    }

    private void validateJwsAndDisplay(String jws) {
        jwtUtils.printJwsHeader(jwtUtils.readHeaderFromJws(jws, ""));
        jwtUtils.printClaims(jwtUtils.readClaimsFromJws(jws, ""));
    }

    private void validateJwsSignature(String jws) {
        log.info("signature is valid? " + jwtUtils.validateSignature(jws,""));
    }

}
