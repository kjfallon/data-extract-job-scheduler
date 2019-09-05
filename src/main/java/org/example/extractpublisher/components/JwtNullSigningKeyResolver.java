package org.example.extractpublisher.components;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.PublicKey;

@Slf4j
@Component
public class JwtNullSigningKeyResolver extends SigningKeyResolverAdapter {

    @Autowired
    JwtUtils jwtUtils;

    @Override
    public Key resolveSigningKey(JwsHeader jwsHeader, Claims claims) {
        log.info("JwtNullSigningKeyResolver is intentionally returning a key that will not validate signature");

        // We have access to the header and claims before signature validation
        jwtUtils.printJwsHeader(jwsHeader);
        jwtUtils.printClaims(claims);

        return new PublicKey() {
            @Override
            public String getAlgorithm() {
                return null;
            }

            @Override
            public String getFormat() {
                return null;
            }

            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }
        };

    }

}
