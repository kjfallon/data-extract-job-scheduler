package org.example.extractpublisher.services;

import org.example.extractpublisher.components.CommandProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;

@Slf4j
@EnableBinding(Sink.class)
public class AmqpEventConsumer {

    @Autowired
    CommandProcessor commandProcessor;

    @Value("${spring.cloud.stream.bindings.input.group}")
    private String amqpGroup;
    @Value("${spring.cloud.stream.bindings.input.destination}")
    private String amqpExchange;


    @StreamListener(target = Sink.INPUT)
    public void processMessage(String message) {
        log.info("Received from AMQP queue " + amqpExchange + "." + amqpGroup + " the message: " + message );
        commandProcessor.parseNewInboundCommand(message);
    }

}