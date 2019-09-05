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
public class SftpIntegrationPartner2JobConfig {

    @Value("${sftp.integration2.host.fqdn}")
    private String sftpHost;
    @Value("${sftp.integration2.timeout:0}")
    private int sftpTimeout;
    @Value("${sftp.integration2.port:22}")
    private int sftpPort;
    @Value("${sftp.integration2.user}")
    private String sftpUser;
    @Value("${sftp.integration2.privateKeyFile:#{null}}")
    private Resource sftpPrivateKey;
    @Value("${sftp.integration2.privateKeyPassphrase:}")
    private String sftpPrivateKeyPassphrase;
    @Value("${sftp.integration2.password:#{null}}")
    private String sftpPasword;
    @Value("${sftp.integration2.remote.directory:/}")
    private String sftpRemoteDirectory;
    @Value("${sftp.integration2.knownHostsFile:#{null}}")
    private Resource knownHostsFile;

    @Bean
    public SessionFactory<LsEntry> sftpSessionFactoryIntegration2() {
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
    @ServiceActivator(inputChannel = "toSftpChannelIntegration2")
    public MessageHandler sftpMessageHandlerIntegration2() {
        SftpMessageHandler handler = new SftpMessageHandler(sftpSessionFactoryIntegration2());
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
    public interface UploadGatewayIntegration2 {
        @Gateway(requestChannel = "toSftpChannelIntegration2")
        void sendToSftp(File file);
    }

}
