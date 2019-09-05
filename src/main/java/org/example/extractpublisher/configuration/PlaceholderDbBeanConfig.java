package org.example.extractpublisher.configuration;

import lombok.extern.slf4j.Slf4j;
import org.example.extractpublisher.services.PlaceholderDbService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Slf4j
@Configuration
public class PlaceholderDbBeanConfig {

    @Value("${org.example.db.driver}")
    private String dummydbDriver;
    @Value("${org.example.db.schema}")
    private String dummydbSchema;
    @Value("${org.example.db.url}")
    private String dummydbUrl;
    @Value("${org.example.db.user}")
    private String dummydbUser;
    @Value("${org.example.db.password}")
    private String dummydbPassword;

    @Bean
    PlaceholderDbService dummydb() {

        PlaceholderDbService dummydb = new PlaceholderDbService();
        dummydb.setDBdriver(dummydbDriver);
        dummydb.setDBschema(dummydbSchema);
        dummydb.setDBurl(dummydbUrl);
        dummydb.setDBuser(dummydbUser);
        dummydb.setDBpassword(dummydbPassword);
        log.info("Loaded Placeholder DB bean");

        return dummydb;
    }
}
