package org.example.extractpublisher.entities;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class JwtMessage {

    SignedJWT signedJWT = null;
    JWSHeader header = null;
    JWTClaimsSet claims = null;

    private String jws = "";
    private Boolean signatureValid = false;
    private String signingKeyAlias = "";

    // standard header values
    private JWSAlgorithm algorithm = null;
    private String keyId = "";

    // standard claim values
    private String issuer = "";
    private String subject = "";
    private List<String> audience = new ArrayList<String>();
    private Date expiration = null;
    private Date notBefore = null;
    private Date issuedAt = null;
    private String id = null;

    // custom claim values
    private String command = "";

}
