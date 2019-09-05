package org.example.extractpublisher.configuration;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.outbound.SftpMessageHandler;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import java.io.File;
import java.io.IOException;

@Configuration
public class SftpIntegrationPartner3JobConfig {

    @Value("${sftp.integration3.host.fqdn}")
    private String sftpHost;
    @Value("${sftp.integration3.timeout:0}")
    private int sftpTimeout;
    @Value("${sftp.integration3.port:22}")
    private int sftpPort;
    @Value("${sftp.integration3.user}")
    private String sftpUser;
    @Value("${sftp.integration3.privateKeyFile:#{null}}")
    private Resource sftpPrivateKey;
    @Value("${sftp.integration3.privateKeyPassphrase:}")
    private String sftpPrivateKeyPassphrase;
    @Value("${sftp.integration3.password:#{null}}")
    private String sftpPasword;
    @Value("${sftp.integration3.remote.directory:/}")
    private String sftpRemoteDirectory;
    @Value("${sftp.integration3.knownHostsFile:#{null}}")
    private Resource knownHostsFile;

    @Bean
    public SessionFactory<LsEntry> sftpSessionFactoryIntegration3() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(sftpHost);
        factory.setTimeout(sftpTimeout);
        factory.setPort(sftpPort);
        factory.setUser(sftpUser);
        if (sftpPrivateKey != null) {
            factory.setPrivateKey(sftpPrivateKey);
            factory.setPrivateKeyPassphrase(sftpPrivateKeyPassphrase);
        } else {
            factory.setPassword(sftpPasword);
        }
        factory.setAllowUnknownKeys(false);
        try {
            factory.setKnownHosts(knownHostsFile.getFile().getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        factory.setHostKeyAlias(sftpHost);
        return factory;
    }

    @Bean
    @ServiceActivator(inputChannel = "toSftpChannelIntegration3")
    public MessageHandler sftpMessageHandlerIntegration3() {
        SftpMessageHandler handler = new SftpMessageHandler(sftpSessionFactoryIntegration3());
        handler.setRemoteDirectoryExpression(new LiteralExpression(sftpRemoteDirectory));
        handler.setFileNameGenerator(new FileNameGenerator() {
            @Override
            public String generateFileName(Message<?> message) {
                if (message.getPayload() instanceof File) {
                    return ((File) message.getPayload()).getName();
                } else {
                    throw new IllegalArgumentException("File expected as payload.");
                }
            }
        });
        return handler;
    }

    @MessagingGateway
    public interface UploadGatewayIntegration3 {
        @Gateway(requestChannel = "toSftpChannelIntegration3")
        void sendToSftp(File file);
    }

}
