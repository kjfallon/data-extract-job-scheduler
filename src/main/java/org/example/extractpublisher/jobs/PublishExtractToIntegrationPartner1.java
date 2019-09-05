package org.example.extractpublisher.jobs;

import org.example.extractpublisher.components.CsvTools;
import org.example.extractpublisher.configuration.SftpIntegrationPartner1JobConfig;
import org.example.extractpublisher.entities.ItemSummary;
import org.example.extractpublisher.services.AmqpEventPublisher;
import org.example.extractpublisher.services.EmailService;
import org.example.extractpublisher.services.PlaceholderDbService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.time.Duration;
import java.util.List;

@Slf4j
@Configuration
public class PublishExtractToIntegrationPartner1 {

    @Autowired
    ConfigurableApplicationContext context;
    @Autowired
    PlaceholderDbService dummydb;
    @Autowired
    CsvTools csvtools;
    @Autowired
    private AmqpEventPublisher amqpPublisher;
    @Autowired
    EmailService emailService;

    @Value("${sftp.integration1.remote.directory:/}")
    private String sftpRemoteDirectory;
    @Value("${sftp.integration1.remote.filename}")
    private String sftpRemoteFilename;
    @Value("${sftp.integration1.local.temp.directory:/}")
    private String tempDir;

    @Value("${sftp.integration1.mail.send.on.success}")
    private Boolean integration1SendSuccessMail;
    @Value("${sftp.integration1.mail.send.on.failure}")
    private Boolean integration1SendFailureMail;
    @Value("${sftp.integration1.mail.success.to}")
    private String integration1SuccessMailTo;
    @Value("${sftp.integration1.mail.success.subject}")
    private String integration1SuccessMailSubject;
    @Value("${sftp.integration1.mail.failure.to}")
    private String integration1FailureMailTo;
    @Value("${sftp.integration1.mail.failure.subject}")
    private String integration1FailureMailSubject;

    // Define bucket to limit requested operations to 1 run per minute
    Integer iterations = 1;
    Integer time = 1;
    Bandwidth limit = Bandwidth.simple(iterations, Duration.ofMinutes(time));
    Bucket bucket = Bucket4j.builder().addLimit(limit).build();

    // scheduled to run every 10 minutes
    @Scheduled(cron="0 */100 * * * ?")
    public void run() {

        long startTime = System.nanoTime();
        log.info("START scheduled task Integration1");
        log.trace("Consuming one token from rate limiting bucket, waiting if need be...");
        // will block until the refill adds one to the bucket.
        try {
            bucket.asScheduler().consume(1);
        } catch (InterruptedException e) {
            log.error(e.toString());
            e.printStackTrace();
        }

        // Perform work
        Boolean result = publish();

        // Report results
        long duration = System.nanoTime() - startTime;
        double durationSeconds = (double)duration / 1000000000.0;
        log.info("STOP scheduled task Integration1, execution time: " + Precision.round(durationSeconds, 2)
                + " seconds (" + Precision.round((durationSeconds/60), 2) + "min)");
        if (integration1SendSuccessMail && result) {
            emailService.sendMail(integration1SuccessMailTo, integration1SuccessMailSubject, "success message body");
        }
        else if (integration1SendFailureMail && !result) {
            emailService.sendMail(integration1FailureMailTo, integration1FailureMailSubject, "success message body");
        }
    }

    public boolean publish() {
        log.info("Publishing  data extract to Integration1...");

        List<ItemSummary> itemList = null;
        try {
        log.info("retrieving data...");
        long dataRetrievalStartTime = System.nanoTime();
            itemList = dummydb.queryDataExtract("partner1");
        long dataRetrievalDuration = System.nanoTime() - dataRetrievalStartTime;
        double dataRetrievalDurationSeconds = (double)dataRetrievalDuration / 1000000000.0;
        log.info("data retrieved in: " + Precision.round(dataRetrievalDurationSeconds, 2)
                + " seconds (" + Precision.round((dataRetrievalDurationSeconds/60), 2) +" min)");
        }
        catch (Exception e) {
            log.error("failure in Integration1 job during retrieving data");
            e.printStackTrace();
            return false;
        }

        String fullPathToLocalFile = null;
        try {
        log.info("processing data...");
        long dataProcessingStartTime = System.nanoTime();
        fullPathToLocalFile = tempDir + File.separator + sftpRemoteFilename;
        csvtools.writeGzipCsvFileFromBeanList(itemList, fullPathToLocalFile);
        long dataProcessingDuration = System.nanoTime() - dataProcessingStartTime;
        double dataProcessingDurationSeconds = (double)dataProcessingDuration / 1000000000.0;
        log.info("data processed in: " + Precision.round(dataProcessingDurationSeconds, 2)
                + " seconds (" + Precision.round((dataProcessingDurationSeconds/60), 2) +" min)");
        }
        catch (Exception e) {
            log.error("failure in Integration1 job during processing data");
            e.printStackTrace();
            return false;
        }

        try {
        log.info("transmitting data...");
        long dataTransmittalStartTime = System.nanoTime();
        SftpIntegrationPartner1JobConfig.UploadGatewayIntegration1 gateway = context.getBean(SftpIntegrationPartner1JobConfig.UploadGatewayIntegration1.class);
        gateway.sendToSftp(new File(fullPathToLocalFile + ".gz"));
        long dataTransmittalDuration = System.nanoTime() - dataTransmittalStartTime;
        double dataTransmittalDurationSeconds = (double)dataTransmittalDuration / 1000000000.0;
        log.info("data transmitted in: " + Precision.round(dataTransmittalDurationSeconds, 2)
                + " seconds (" + Precision.round((dataTransmittalDurationSeconds/60), 2) +" min)");
        }
        catch (Exception e) {
            log.error("failure in Integration1 job during transmitting data");
            e.printStackTrace();
            return false;
        }
        // delete local copy of data that was sent
        File transmittedFile = new File(fullPathToLocalFile + ".gz");
        transmittedFile.delete();

        amqpPublisher.sendMessage("Integration1 Extract Complete");
        return true;
    }

}
