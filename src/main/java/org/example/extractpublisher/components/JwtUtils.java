package org.example.extractpublisher.components;

import org.apache.commons.lang3.StringUtils;
import org.example.extractpublisher.entities.JwtMessage;
import io.jsonwebtoken.*;

import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.io.DecodingException;
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
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtUtils {

    @Autowired
    JwtSigningKeyResolver signingKeyResolver;
    @Autowired
    JwtNullSigningKeyResolver nullSigningKeyResolver;
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

    KeyStore keystore = null;

    private KeyStore getKeystore() {
        if (keystore == null) {

            if (keystoreFetchRemote) {
                byte[] binData = httpUtils.getHttpGetBinaryData(keystoreRemoteSource);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(binData);
                try {
                    keystore = KeyStore.getInstance("PKCS12");
                    keystore.load(inputStream, keystorePassword.toCharArray());
                } catch (KeyStoreException e) {
                    log.error(e.toString());
                    e.printStackTrace();
                } catch (CertificateException e) {
                    log.error(e.toString());
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    log.error(e.toString());
                    e.printStackTrace();
                } catch (IOException e) {
                    log.error(e.toString());
                    e.printStackTrace();
                }
            }
            else {
                Resource resource = new ClassPathResource(keystoreFilename);
                try {
                    keystore = KeyStore.getInstance("PKCS12");
                    keystore.load(resource.getInputStream(), keystorePassword.toCharArray());
                } catch (KeyStoreException e) {
                    log.error(e.toString());
                    e.printStackTrace();
                } catch (CertificateException e) {
                    log.error(e.toString());
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    log.error(e.toString());
                    e.printStackTrace();
                } catch (IOException e) {
                    log.error(e.toString());
                    e.printStackTrace();
                }
            }
        }
        if (keystore == null)
            log.error("Could not load keystore, application will fail to verify and sign JWS");

        return keystore;
    }

    public String createRsaSignedJwtWithClaims(String issuer,
                                               String subject,
                                               String audience,
                                               String signingKeyAlias,
                                               Integer minutesUntilExpiry,
                                               Claims claims) {

        String jws = "";

        // Add any extra claims
        claims.put("user", "TEST-API");

        Key privateKey = getPrivateKeyFromKeystoreByAlias(signingKeyAlias);
        JwtBuilder builder = Jwts.builder()
                .setHeaderParam("kid", signingKeyAlias)
                .setClaims(claims)
                .setIssuedAt(new Date())
                .signWith(privateKey, SignatureAlgorithm.RS512);

        builder.setNotBefore(new Date());
        if (minutesUntilExpiry > 0) {
            builder.setExpiration(new Date(System.currentTimeMillis() + (minutesUntilExpiry * 60000)));
        }
        if (StringUtils.isNotEmpty(issuer)) {
            builder.setIssuer(issuer);
        }
        if (StringUtils.isNotEmpty(subject)) {
            builder.setSubject(subject);
        }
        if (StringUtils.isNotEmpty(audience)) {
            builder.setAudience(audience);
        }

        // create JWT string from builder
        try {
            jws = builder.compact();
        }
        catch (JwtException e) {
            log.error("Unable to create JWT");
            log.error(e.toString());
            e.printStackTrace();
        }
        log.info("Created JWT: " + jws);

        return jws;
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
        catch (NoSuchAlgorithmException e) {
            log.error(e.toString());
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            log.error(e.toString());
            e.printStackTrace();
        } catch (KeyStoreException e) {
            log.error(e.toString());
            e.printStackTrace();
        }

        return privateKey;

    }

    public Boolean validateSignature(String jws, String signingKeyAlias){
        Boolean trust = false;

        if (StringUtils.isNotEmpty(signingKeyAlias)) {
            PublicKey publicKey = getPublicKeyFromKeystoreByAlias(signingKeyAlias);
            try {
                Jwts.parser().setSigningKey(publicKey).parse(jws);
                trust = true;
            } catch (DecodingException e) {
                log.error("error base64 decoding JWT");
                log.error(e.toString());
            } catch (ExpiredJwtException e) {
                log.error("jwt has expired");
                log.error(e.toString());
            } catch (JwtException e) {
                log.error("jwt exception");
                e.printStackTrace();
            } catch(IllegalArgumentException e) {
                log.error(e.toString());
            }catch(Exception e) {
                log.error(e.toString());
                e.printStackTrace();
            }
        }
        else {
            try {
                Jwts.parser().setSigningKeyResolver(signingKeyResolver).parse(jws);
                trust = true;
            } catch (DecodingException e) {
                log.error("error base64 decoding JWT");
                log.error(e.toString());
            } catch (ExpiredJwtException e) {
                log.error("jwt has expired");
                log.error(e.toString());
            } catch (JwtException e) {
                log.error("jwt exception");
                e.printStackTrace();
            } catch(IllegalArgumentException e) {
                log.error(e.toString());
            }catch(Exception e) {
                log.error(e.toString());
                e.printStackTrace();
            }
        }

        if (!trust) log.error("Unable to verify signature");
        return trust;
    }

    public Claims readClaimsFromJws(String jws, String signingKeyAlias) {
        Claims claims = null;

        if (StringUtils.isNotEmpty(signingKeyAlias)) {
            PublicKey publicKey = getPublicKeyFromKeystoreByAlias(signingKeyAlias);
            try {
                claims = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(jws).getBody();
            } catch (DecodingException e) {
                log.error("error base64 decoding JWT");
                log.error(e.toString());
            } catch (ExpiredJwtException e) {
                log.error("jwt has expired");
                log.error(e.toString());
            } catch (JwtException e) {
                log.error("jwt exception");
                e.printStackTrace();
            } catch(IllegalArgumentException e) {
                log.error(e.toString());
            }catch(Exception e) {
                log.error(e.toString());
                e.printStackTrace();
            }
        }
        else {
            try {
                claims = Jwts.parser().setSigningKeyResolver(signingKeyResolver).parseClaimsJws(jws).getBody();
            } catch (DecodingException e) {
                log.error("error base64 decoding JWT");
                log.error(e.toString());
            } catch (ExpiredJwtException e) {
                log.error("jwt has expired");
                log.error(e.toString());
            } catch (JwtException e) {
                log.error("jwt exception");
                e.printStackTrace();
            } catch(IllegalArgumentException e) {
                log.error(e.toString());
            }catch(Exception e) {
                log.error(e.toString());
                e.printStackTrace();
            }
        }
        return claims;
    }

    public JwtMessage readJwtMessageFromJws(String jws, String signingKeyAlias) {

        JwtMessage jwtMessage = new JwtMessage();
        Claims claims = null;
        JwsHeader header = null;

        if (StringUtils.isNotEmpty(signingKeyAlias)) {
            PublicKey publicKey = getPublicKeyFromKeystoreByAlias(signingKeyAlias);
            try {
                claims = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(jws).getBody();
                header = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(jws).getHeader();
            } catch (DecodingException e) {
                log.error("error base64 decoding JWT");
                log.error(e.toString());
            } catch (ExpiredJwtException e) {
                log.error("jwt has expired");
                log.error(e.toString());
            } catch (JwtException e) {
                log.error("jwt exception");
                e.printStackTrace();
            } catch(IllegalArgumentException e) {
                log.error(e.toString());
            }catch(Exception e) {
                log.error(e.toString());
                e.printStackTrace();
            }
        }
        else {
            try {
                claims = Jwts.parser().setSigningKeyResolver(signingKeyResolver).parseClaimsJws(jws).getBody();
                header = Jwts.parser().setSigningKeyResolver(signingKeyResolver).parseClaimsJws(jws).getHeader();
            } catch (DecodingException e) {
                log.error("error base64 decoding JWT");
                log.error(e.toString());
            } catch (ExpiredJwtException e) {
                log.error("jwt has expired");
                log.error(e.toString());
            } catch (JwtException e) {
                log.error("jwt exception");
                e.printStackTrace();
            } catch(IllegalArgumentException e) {
                log.error(e.toString());
            } catch(Exception e) {
                log.error(e.toString());
                e.printStackTrace();
            }
        }

        if (claims != null) {
            jwtMessage.setSignatureValid(true);
            jwtMessage.setJws(jws);
            jwtMessage.setHeader(header);
            jwtMessage.setAlgorithm(header.getAlgorithm());
            jwtMessage.setKeyId(header.getKeyId());
            jwtMessage.setClaims(claims);
            jwtMessage.setIssuer(claims.getIssuer());
            jwtMessage.setSubject(claims.getSubject());
            jwtMessage.setAudience(claims.getAudience());
            jwtMessage.setExpiration(claims.getExpiration());
            jwtMessage.setNotBefore(claims.getNotBefore());
            jwtMessage.setIssuedAt(claims.getIssuedAt());
            jwtMessage.setId(claims.getId());
            jwtMessage.setCommand((String) claims.get("command"));
        }
        return jwtMessage;
    }

    public JwsHeader readHeaderFromJws(String jws, String signingKeyAlias) {
        JwsHeader header = null;

        if (StringUtils.isNotEmpty(signingKeyAlias)) {
            PublicKey publicKey = getPublicKeyFromKeystoreByAlias(signingKeyAlias);
            try {
                header = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(jws).getHeader();
            } catch (DecodingException e) {
                log.error("error base64 decoding JWT");
                log.error(e.toString());
            } catch (ExpiredJwtException e) {
                log.error("jwt has expired");
                log.error(e.toString());
            } catch (JwtException e) {
                log.error("jwt exception");
                e.printStackTrace();
            } catch(IllegalArgumentException e) {
                log.error(e.toString());
            }catch(Exception e) {
                log.error(e.toString());
                e.printStackTrace();
            }
        }
        else {
            try {
                header = Jwts.parser().setSigningKeyResolver(signingKeyResolver).parseClaimsJws(jws).getHeader();
            } catch (DecodingException e) {
                log.error("error base64 decoding JWT");
                log.error(e.toString());
            } catch (ExpiredJwtException e) {
                log.error("jwt has expired");
                log.error(e.toString());
            } catch (JwtException e) {
                log.error("jwt exception");
                e.printStackTrace();
            } catch(IllegalArgumentException e) {
                log.error(e.toString());
            }catch(Exception e) {
                log.error(e.toString());
                e.printStackTrace();
            }
        }
        return header;
    }

    public void printJwsHeader(JwsHeader header) {
        if (header == null) {
            log.error("header object is null");
        } else if (header.size() == 0) {
            log.warn("header is empty");
        } else {
            log.info("header:");
            log.info("--algo: " + header.getAlgorithm());
            log.info("--keyid: " + header.getKeyId());
        }
    }

    public void printClaims(Claims claims) {
        if (claims == null) {
            log.error("claims object is null");
        } else if (claims.size() == 0) {
            log.warn("claims are empty");
        } else {
            log.info("claims:");
            for (Map.Entry<String, Object> claim : claims.entrySet()) {
                log.info("--claim (" + claim.getKey() + " : " + claim.getValue() + ")");
            }
        }
    }

}
