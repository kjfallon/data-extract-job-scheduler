package org.example.extractpublisher;

import org.example.extractpublisher.components.StartUpTasks;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@IntegrationComponentScan
@EnableIntegration
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class ExtractPublisherApplication {

	public static void main(String[] args) throws InterruptedException {
		ApplicationContext springBootAppContext = SpringApplication.run(ExtractPublisherApplication.class, args);

		// Display information and perform any required housekeeping at launch
		springBootAppContext.getBean(StartUpTasks.class).appLaunchTasks();

	}

}
