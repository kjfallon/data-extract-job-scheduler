package org.example.extractpublisher.components;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.extractpublisher.entities.JwtMessage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtils {

    @Autowired
    HttpUtils httpUtils;

    @Value("${java.keystore.local.filename}")
    private String keystoreFilename;
    @Value("${java.keystore.password}")
    private String keystorePassword;
    @Value("${java.keystore.private.key.password}")
    private String privatekeyPassword;
    @Value("${java.keystore.fetch.from.remote}")
    private Boolean keystoreFetchRemote;
    @Value("${java.keystore.remote.source}")
    private String keystoreRemoteSource;
    @Value("${java.keystore.private.key.alias}")
    private String privateKeyAlias;

    KeyStore keystore = null;

    private KeyStore getKeystore() {
        if (keystore == null) {

            if (keystoreFetchRemote) {
                byte[] binData = httpUtils.getHttpGetBinaryData(keystoreRemoteSource);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(binData);
                try {
                    keystore = KeyStore.getInstance("PKCS12");
                    keystore.load(inputStream, keystorePassword.toCharArray());
                } catch (Exception e) {
                    log.error(e.toString());
                    e.printStackTrace();
                }
            }
            else {
                Resource resource = new ClassPathResource(keystoreFilename);
                try {
                    keystore = KeyStore.getInstance("PKCS12");
                    keystore.load(resource.getInputStream(), keystorePassword.toCharArray());
                } catch (Exception e) {
                    log.error(e.toString());
                    e.printStackTrace();
                }
            }
        }
        if (keystore == null)
            log.error("Could not load keystore, application will fail to verify and sign JWS");

        return keystore;
    }

    // This creates a JWS signed with the producer's private key which is then encrypted to recipients's public key
    // as the payload in a JWE
    public String createSignedJweWithCustomClaims(String issuer,
                                                  String subject,
                                                  List<String> audience,
                                                  String signingKeyAlias,
                                                  String recipientKeyAlias,
                                                  Integer minutesUntilExpiry,
                                                  List<Pair<String, Object>> claimList) {

        String jweString = "";
        Date now = new Date();

        JWTClaimsSet.Builder jwtBuilder = new JWTClaimsSet.Builder();
        jwtBuilder.issuer(issuer);
        jwtBuilder.subject(subject);
        jwtBuilder.audience(audience);
        jwtBuilder.expirationTime(new Date(now.getTime()  + (1000 * 60 * minutesUntilExpiry)));
        jwtBuilder.notBeforeTime(now);
        jwtBuilder.issueTime(now);
        jwtBuilder.jwtID(UUID.randomUUID().toString());
        for(Pair<String, Object> claim : claimList) {
            jwtBuilder.claim(claim.getKey(), claim.getValue());
        }
        JWTClaimsSet jwtClaims = jwtBuilder.build();

        // specify the id (kid) of the signing key in the header so it may be validated by recipient
        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS512).keyID(signingKeyAlias).build();

        SignedJWT signedJWT = new SignedJWT(jwsHeader, jwtClaims);
        PrivateKey privateKey = (PrivateKey)getPrivateKeyFromKeystoreByAlias(signingKeyAlias);
        try {
            signedJWT.sign(new RSASSASigner(privateKey));
        } catch (JOSEException e) {
            log.error(e.toString());
            e.printStackTrace();
            return jweString;
        }

        // Create JWE object with signed JWT as payload
        JWEObject jweObject = new JWEObject(
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                        .contentType("JWT") // required to indicate nested JWT
                        .build(),
                new Payload(signedJWT));

        // Encrypt to the recipient's public key
        RSAPublicKey recipientPublicKey = (RSAPublicKey)getPublicKeyFromKeystoreByAlias(recipientKeyAlias);
        try {
            jweObject.encrypt(new RSAEncrypter(recipientPublicKey));
        } catch (JOSEException e) {
            log.error(e.toString());
            e.printStackTrace();
            return jweString;
        }

        // Serialise to JWE compact form
        jweString = jweObject.serialize();
        log.info("Created JWE: " + jweString);

        return jweString;
    }

    public PublicKey getPublicKeyFromKeystoreByAlias(String alias) {

        Certificate cert = null;
        PublicKey publicKey = null;

        // Get the public RSA key for this key alias from the application's java keystore file
        try {
            cert = getKeystore().getCertificate(alias);
            if (cert != null) {
                publicKey = cert.getPublicKey();
            }
            else log.warn("Did not find public key with alias '" + alias + "' in keystore");
        }
        catch (KeyStoreException e) {
            log.error(e.toString());
            e.printStackTrace();
        }

        return publicKey;
    }

    public Key getPrivateKeyFromKeystoreByAlias(String keyAlias) {

        Key privateKey = null;

        // Get the private RSA key for this key alias from the application's java keystore file
        try {
            privateKey = getKeystore().getKey(keyAlias, privatekeyPassword.toCharArray());
        }
        catch (Exception e) {
            log.error(e.toString());
            e.printStackTrace();
        }

        return privateKey;
    }

    public Boolean validateJwsSignature(SignedJWT signedJWT, String signingKeyAlias){
        Boolean trust = false;

        // Validate jws with specified key or default to kid from header
        if (StringUtils.isEmpty(signingKeyAlias)) signingKeyAlias = signedJWT.getHeader().getKeyID();

        if (StringUtils.isNotEmpty(signingKeyAlias)) {
            RSAPublicKey signingPublicKey = (RSAPublicKey)getPublicKeyFromKeystoreByAlias(signingKeyAlias);
            try {
                trust = signedJWT.verify(new RSASSAVerifier(signingPublicKey));
            } catch (JOSEException e) {
                log.error(e.toString());
                e.printStackTrace();
            }
        }

        if (!trust) log.warn("Unable to verify signature");
        return trust;
    }

    public JWTClaimsSet validateJwsSignatureAndReadClaimsFrom(SignedJWT signedJWT, String signingKeyAlias) {
        JWTClaimsSet claims = null;

        Boolean trust = validateJwsSignature(signedJWT, signingKeyAlias);
        if (trust) {
            try {
                claims = signedJWT.getJWTClaimsSet();
                log.info("Parsed claims from JWS");
            } catch (ParseException e) {
                log.info("Unable to parse claims from JWS");
                log.error(e.toString());
                e.printStackTrace();
            }
        }
        else {
            log.warn("Unable to verify signature");
        }
        return claims;
    }

    public SignedJWT decryptJwsFromJwe(String jweString, String recipientKeyAlias) {

        JWEObject jweObject = null;
        SignedJWT signedJWT = null;
        try {
            jweObject = JWEObject.parse(jweString);
        } catch (ParseException e) {
            log.warn("Unable to parse JWE from string");
            log.warn(e.toString());
            e.printStackTrace();
            return signedJWT;
        }

        // Decrypt with private key
        PrivateKey privateKey = (PrivateKey)getPrivateKeyFromKeystoreByAlias(recipientKeyAlias);
        if (privateKey == null) {
            log.warn("Unable to load private key from keystore: " + recipientKeyAlias);
            return signedJWT;
        }
        try {
            jweObject.decrypt(new RSADecrypter(privateKey));
        } catch (JOSEException e) {
            log.warn("Unable to decrypt JWE with recipientKeyAlias: " + recipientKeyAlias);
            log.warn(e.toString());
            e.printStackTrace();
            return signedJWT;
        }

        // Extract payload
        signedJWT = jweObject.getPayload().toSignedJWT();

        return signedJWT;
    }

    public JwtMessage readJwtMessageFromJwe(String jweString, String signingKeyAlias) {

        // first decrypt the JWS from the JWE
        SignedJWT signedJWT = decryptJwsFromJwe(jweString, privateKeyAlias);

        // second parse the JWS
        return readJwtMessageFromJws(signedJWT, signingKeyAlias);
    }

    public JwtMessage readJwtMessageFromJws(SignedJWT signedJWT, String signingKeyAlias) {

        JwtMessage jwtMessage = new JwtMessage();

        if (signedJWT == null) {
            log.warn("readJwtMessageFromJws received null signedJWT so returning empty jwtMessage");
            return jwtMessage;
        }
        JWSHeader header = signedJWT.getHeader();
        JWTClaimsSet claims = null;

        try {
            claims = signedJWT.getJWTClaimsSet();
        } catch (ParseException e) {
            log.info("Unable to parse claims from JWS");
            log.error(e.toString());
            e.printStackTrace();
        }
        if (claims != null) {
            jwtMessage.setSignatureValid(validateJwsSignature(signedJWT, signingKeyAlias));
            jwtMessage.setJws(signedJWT.serialize());
            jwtMessage.setHeader(header);
            jwtMessage.setAlgorithm(header.getAlgorithm());
            jwtMessage.setKeyId(header.getKeyID());
            jwtMessage.setClaims(claims);
            jwtMessage.setIssuer(claims.getIssuer());
            jwtMessage.setSubject(claims.getSubject());
            jwtMessage.setAudience(claims.getAudience());
            jwtMessage.setExpiration(claims.getExpirationTime());
            jwtMessage.setNotBefore(claims.getNotBeforeTime());
            jwtMessage.setIssuedAt(claims.getIssueTime());
            jwtMessage.setId(claims.getJWTID());
            jwtMessage.setCommand((String) claims.getClaims().get("command"));
        }

        return jwtMessage;
    }

    public void printJwsHeader(JWSHeader header) {
        if (header == null) {
            log.error("header object is null");
        } else {
            log.info("header:");
            log.info("--algo: " + header.getAlgorithm());
            log.info("--keyid: " + header.getKeyID());
        }
    }

    public void printClaims(JWTClaimsSet claims) {
        if (claims == null) {
            log.error("claims object is null");
        }else {
            log.info("claims:");
            for(String claimKey : claims.getClaims().keySet()) {
                log.info("--claim (" + claimKey + " : " + claims.getClaims().get(claimKey) + ")");
            }
        }
    }

}
