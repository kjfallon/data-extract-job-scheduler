package org.example.extractpublisher.components;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Key;

@Slf4j
@Component
public class JwtSigningKeyResolver extends SigningKeyResolverAdapter {

    @Autowired
    JwtUtils jwtUtils;

    @Override
    public Key resolveSigningKey(JwsHeader jwsHeader, Claims claims) {

        // read the key id value that is expected to be present in the jws header
        String keyId = jwsHeader.getKeyId();

        log.info("SigningKeyResolver looking for a public RSA key with an alias of '" + keyId + "' in the application's keystore");
        Key key = jwtUtils.getPublicKeyFromKeystoreByAlias(keyId);

        return key;
    }

}