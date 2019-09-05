package org.example.extractpublisher.entities;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import lombok.Data;

import java.util.Date;

@Data
public class JwtMessage {

    private String jws = "";
    private Boolean signatureValid = false;
    private String signingKeyAlias = "";

    // standard header values
    private String algorithm = "";
    private String keyId = "";

    JwsHeader header;
    Claims claims;
    // standard claim values
    private String issuer = "";
    private String subject = "";
    private String audience = "";
    private Date expiration = null;
    private Date notBefore = null;
    private Date issuedAt = null;
    private String id = null;

    // custom claim values
    private String command = "";

}
