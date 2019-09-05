package org.example.extractpublisher.configuration;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;

@Slf4j
@Configuration
public class SchedulerConfig {

    SchedulerConfig() {
        log.info("Loaded Scheduler config");
    }

    // Classes in the jobs package are individually annotated with @Scheduled but any application tasks

    // scheduled to run every 1 minutes
    @Scheduled(cron="0 */1 * * * ?")
    public void heartbeat() {

        long startTime = System.nanoTime();
        log.info("@1min heartbeat task complete");
        long duration = System.nanoTime() - startTime;
        double durationSeconds = (double)duration / 1000000000.0;
        log.info("heartbeat task execution time: " + Precision.round(durationSeconds, 2)
                + " seconds (" + Precision.round((durationSeconds/60), 2) + "min)");
    }

}
