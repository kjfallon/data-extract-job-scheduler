package org.example.extractpublisher.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.integration.support.MessageBuilder;

@Slf4j
@EnableBinding(Source.class)
public class AmqpEventPublisher {

    @Autowired
    private Source source;

    public void sendMessage(String messageBody) {
        source.output().send(MessageBuilder.withPayload(messageBody).build());
        log.info("AMQP Published message '{}'", messageBody);
    }
}