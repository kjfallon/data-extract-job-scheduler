package org.example.extractpublisher.components;

import org.example.extractpublisher.entities.JwtMessage;
import org.example.extractpublisher.jobs.PublishExtractToIntegrationPartner1;
import org.example.extractpublisher.jobs.PublishExtractToIntegrationPartner2;
import org.example.extractpublisher.jobs.PublishExtractToIntegrationPartner3;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component("commandProcessor")
public class CommandProcessor {

    // expect RS512 signed JWT messages with commands to execute, validate them and apply access control

    @Autowired
    JwtUtils jwtUtils;
    @Autowired
    PublishExtractToIntegrationPartner2 publishIntegration2;
    @Autowired
    PublishExtractToIntegrationPartner3 publishIntegration3;
    @Autowired
    PublishExtractToIntegrationPartner1 publishIntegration1;

    static final String THIS_APPLICAION_JWT_AUDIENCE = "Extract-Publisher";

    public Boolean parseNewInboundCommand(String message) {

        Boolean commandSuccess = false;
        Boolean commandAuthorized = false;

        // The message should be a JWS
        log.info("Command processor parsing JWS message");
        // validate signature
        JwtMessage jwtMessage = jwtUtils.readJwtMessageFromJws(message, "");
        jwtUtils.printJwsHeader(jwtMessage.getHeader());
        jwtUtils.printClaims(jwtMessage.getClaims());

        if (jwtMessage.getSignatureValid()) {
            log.info("JWS message signature is valid");

            // check if this application is the audience
            if (!THIS_APPLICAION_JWT_AUDIENCE.equalsIgnoreCase(jwtMessage.getAudience())) {
                log.warn("Message audience of '" + jwtMessage.getAudience() + "' does not match this application, rejecting message");
                return false;
            }

            // check if message is expired or not yet valid
            Date now = new Date();
            if ( now.before(jwtMessage.getNotBefore()) || now.after(jwtMessage.getExpiration()) ) {
                log.warn("Message is either expired or not yet valid, rejecting message");
                return false;
            }

            // verify the issuer asserted in the jwt matches the key alias used to sign the jwt
            Boolean issuerValid  = jwtUtils.validateSignature(message, jwtMessage.getIssuer());
            if (!issuerValid) {
                log.warn("Message issuer that was asserted in JWT does not match signing key, rejecting message");
                return false;
            }
        }
        else {
            log.warn("Command processor rejecting message");
            return false;
        }

        // Assess whether this issuer is allowed to use this command, and if so then execute it
        commandAuthorized = commandAccessControl(jwtMessage.getIssuer(), jwtMessage.getCommand());
        if (commandAuthorized) {
            log.info("Processing command...");
            commandSuccess = processValidCommand(jwtMessage.getCommand());
        }
        else {
            log.warn("Command not authorized for signatory, rejecting message");
        }

        return commandSuccess;
    }

    private boolean commandAccessControl(String issuer, String command) {
        // TODO implement access control lookup
        // allow any issuer with accepted signature to issue any command
        log.info("The issuer '" + issuer + "' is allowed to execute command '" + command + "'");
        return true;
    }

    private Boolean processValidCommand(String command) {

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
        }

        log.info("Command execution complete");
        return commandResult;
    }

}
