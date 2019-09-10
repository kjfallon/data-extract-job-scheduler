package org.example.extractpublisher.components;

import org.example.extractpublisher.entities.JwtMessage;
import org.example.extractpublisher.jobs.PublishExtractToIntegrationPartner1;
import org.example.extractpublisher.jobs.PublishExtractToIntegrationPartner2;
import org.example.extractpublisher.jobs.PublishExtractToIntegrationPartner3;
import lombok.extern.slf4j.Slf4j;
import org.example.extractpublisher.services.AuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component("commandProcessor")
public class CommandProcessor {

    // expect encrypted JWE containing signed JWS messages with commands to execute.  Validate and apply access control.

    @Autowired
    JwtUtils jwtUtils;
    @Autowired
    PublishExtractToIntegrationPartner2 publishIntegration2;
    @Autowired
    PublishExtractToIntegrationPartner3 publishIntegration3;
    @Autowired
    PublishExtractToIntegrationPartner1 publishIntegration1;
    @Autowired
    AuditLogger audit;

    static final String THIS_APPLICAION_JWT_AUDIENCE = "Extract-Publisher";

    public Boolean parseNewInboundCommand(String message) {

        Boolean commandSuccess = false;
        Boolean commandAuthorized = false;

        // The message should be a JWE
        log.info("Command processor parsing JWE message");
        JwtMessage jwtMessage = jwtUtils.readJwtMessageFromJwe(message, "");
        if (jwtMessage.getHeader() == null) {
            log.warn("Unable to decrypted JWS from JWE");
            audit.syslog("JWE_DECRYPTION_UNSUCCESSFUL", jwtMessage.getIssuer(), jwtMessage.getCommand(), "INFO", "OK");
        }
        else {
            log.info("Decrypted JWS from JWE");
            audit.syslog("JWE_DECRYPTION_SUCCESSFUL", jwtMessage.getIssuer(), jwtMessage.getCommand(), "INFO", "OK");
            jwtUtils.printJwsHeader(jwtMessage.getHeader());
            jwtUtils.printClaims(jwtMessage.getClaims());
        }

        if (jwtMessage.getSignatureValid()) {
            log.info("JWS message signature is valid");
            audit.syslog("JWS_WITH_VALID_SIGNATURE_RECEIVED", jwtMessage.getIssuer(), jwtMessage.getCommand(), "INFO", "OK");

            // check if this application is the audience
            if (!(jwtMessage.getAudience().contains(THIS_APPLICAION_JWT_AUDIENCE))) {
                log.warn("Message audience of '" + jwtMessage.getAudience() + "' does not match this application, rejecting message");
                audit.syslog("JWS_REJECTED_BAD_AUDIENCE", jwtMessage.getIssuer(), jwtMessage.getCommand(), "INFO", "OK");
                return false;
            }

            // check if message is expired or not yet valid
            Date now = new Date();
            if ( now.before(jwtMessage.getNotBefore()) || now.after(jwtMessage.getExpiration()) ) {
                log.warn("Message is either expired or not yet valid, rejecting message");
                audit.syslog("JWS_REJECTED_BAD_TIMESTAMP", jwtMessage.getIssuer(), jwtMessage.getCommand(), "INFO", "OK");
                return false;
            }

            // verify the issuer asserted in the jwt matches the key alias used to sign the jwt
            Boolean issuerValid  = jwtUtils.validateJwsSignature(jwtMessage.getSignedJWT(), jwtMessage.getIssuer());
            if (!issuerValid) {
                log.warn("Message issuer that was asserted in JWT does not match signing key, rejecting message");
                audit.syslog("JWS_REJECTED_ISSUER_NOT_SIGNER", jwtMessage.getIssuer(), jwtMessage.getCommand(), "INFO", "OK");
                return false;
            }
        }
        else {
            log.warn("Command processor rejecting message");
            audit.syslog("JWS_REJECTED_BAD_SIGNATURE", jwtMessage.getIssuer(), jwtMessage.getCommand(), "INFO", "OK");
            return false;
        }

        // Assess whether this issuer is allowed to use this command, and if so then execute it
        commandAuthorized = commandAccessControl(jwtMessage.getIssuer(), jwtMessage.getCommand());
        if (commandAuthorized) {
            log.info("Processing command...");
            audit.syslog("COMMAND_AUTHORIZED", jwtMessage.getIssuer(), jwtMessage.getCommand(), "INFO", "OK");
            commandSuccess = processValidCommand(jwtMessage.getIssuer(), jwtMessage.getCommand());
        }
        else {
            log.warn("Command not authorized for signatory, rejecting message");
            audit.syslog("COMMAND_NOT_AUTHORIZED", jwtMessage.getIssuer(), jwtMessage.getCommand(), "INFO", "OK");
        }

        if (commandSuccess) {
            log.info("Command execution success");
            audit.syslog("COMMAND_EXECUTION_SUCCESS", jwtMessage.getIssuer(), jwtMessage.getCommand(), "INFO", "OK");
        }
        else {
            log.warn("Command execution failure");
            audit.syslog("COMMAND_EXECUTION_FAILURE", jwtMessage.getIssuer(), jwtMessage.getCommand(), "INFO", "OK");
        }

        return commandSuccess;
    }

    private boolean commandAccessControl(String issuer, String command) {
        // implement appropriate access control lookup here
        // allow all known issuers in local keystore to issue full command set
        log.info("The issuer '" + issuer + "' is allowed to execute command '" + command + "'");
        audit.syslog("ACCESS_CONTROL_GRANT", issuer, command, "INFO", "OK");
        return true;
    }

    private Boolean processValidCommand(String issuer, String command) {

        Boolean commandResult = false;

        switch (command) {
            case "EXECUTE_INTEGRATION1_EXPORT":
                publishIntegration1.run();
                break;
            case "EXECUTE_INTEGRATION2_EXPORT":
                publishIntegration2.run();
                break;
            case "EXECUTE_INTEGRATION3_EXPORT":
                publishIntegration3.run();
                break;

            default:
                commandResult = false;
                log.warn("Not executing unknown command: " + command);
                audit.syslog("REJECTED_UNKNOWN_COMMAND", issuer, command, "INFO", "OK");
        }

        log.info("Command execution complete");
        return commandResult;
    }

}
