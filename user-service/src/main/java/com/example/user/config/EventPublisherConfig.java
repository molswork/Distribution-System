package com.example.user.config;

import com.example.common.event.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventPublisherConfig {

    @Bean
    public DomainEventPublisher domainEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Autowired(required = false) ObjectMapper objectMapper) {
        if (objectMapper != null) {
            return new DomainEventPublisher(rabbitTemplate, objectMapper);
        }
        return new DomainEventPublisher(rabbitTemplate);
    }
}

