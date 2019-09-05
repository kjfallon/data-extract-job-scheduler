package org.example.extractpublisher.components;

import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartUpTasks {

    @Value("${org.example.db.url}")
    private String dbUrl;
    @Value("${org.example.db.schema}")
    private String dbSchema;

    @Value("${spring.rabbitmq.host}")
    private String amqpHost;
    @Value("${spring.cloud.stream.bindings.input.destination}")
    private String amqpInboundExchange;
    @Value("${spring.cloud.stream.bindings.output.destination}")
    private String amqpOutboundExchange;

    public void appLaunchTasks() {

        //Arrays.stream(springBootAppContext.getBeanDefinitionNames()).sorted().forEach(System.out::println);
        log.info("extract-publisher: started");
        log.info("extract-amqp-publisher: using database: " + dbUrl);
        log.info("extract-amqp-publisher: using database schema: " + dbSchema);
        log.info("extract-amqp-publisher: will receive AMQP events from the " + amqpInboundExchange + " exchange on host " + amqpHost);
        log.info("extract-amqp-publisher: will send AMQP events to the " + amqpOutboundExchange + " exchange on host " + amqpHost);

        // Uncomment this to demo encrypting a string for use in a property file
        //String string1ToEncrypt = "";
        //String string2ToEncrypt = "";
        //log.info("The string " + string1ToEncrypt + " encrypts to " + (encryptionDemonstrationBean.encrypt(string1ToEncrypt)));
        //log.info("The string " + string2ToEncrypt + " encrypts to " + (encryptionDemonstrationBean.encrypt(string2ToEncrypt)));
    }

    @Autowired
    StringEncryptor encryptionDemonstrationBean;

    @Bean(name="encryptionDemonstrationBean")
    static public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword("changeme");
        config.setAlgorithm("PBEWithHmacSHA512AndAES_256");
        config.setKeyObtentionIterations("3000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }

}