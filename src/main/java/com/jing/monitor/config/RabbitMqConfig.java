package com.jing.monitor.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * RabbitMQ exchange, queue, binding, and converter configuration.
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public Declarables alertQueueTopology(
            @Value("${app.rabbitmq.exchange}") String alertExchangeName,
            @Value("${app.rabbitmq.routing-key}") String alertRoutingKey,
            @Value("${app.rabbitmq.queue}") String alertQueueName,
            @Value("${app.rabbitmq.dlx-exchange}") String deadLetterExchangeName,
            @Value("${app.rabbitmq.dlq-routing-key}") String deadLetterRoutingKey,
            @Value("${app.rabbitmq.dlq}") String deadLetterQueueName
    ) {
        DirectExchange alertExchange = new DirectExchange(alertExchangeName, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(deadLetterExchangeName, true, false);

        Queue alertQueue = new Queue(
                alertQueueName,
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange", deadLetterExchangeName,
                        "x-dead-letter-routing-key", deadLetterRoutingKey
                )
        );
        Queue deadLetterQueue = new Queue(deadLetterQueueName, true);

        Binding alertBinding = BindingBuilder.bind(alertQueue).to(alertExchange).with(alertRoutingKey);
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(deadLetterRoutingKey);

        return new Declarables(alertExchange, deadLetterExchange, alertQueue, deadLetterQueue, alertBinding, deadLetterBinding);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }
}
