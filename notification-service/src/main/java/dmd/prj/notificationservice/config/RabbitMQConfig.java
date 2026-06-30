package dmd.prj.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static dmd.prj.common.constant.RabbitMQConstant.*;

@Configuration
public class RabbitMQConfig {

    // ============================================
    // NOTIFICATION QUEUE - Notification Service consumes this
    // ============================================
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .deadLetterExchange(NOTIFICATION_DLQ_EXCHANGE)
                .build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    // Notification Dead Letter Queue
    @Bean
    public DirectExchange notificationDlqExchange() {
        return new DirectExchange(NOTIFICATION_DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationDlqQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ_QUEUE).build();
    }

    @Bean
    public Binding notificationDlqBinding(Queue notificationDlqQueue, DirectExchange notificationDlqExchange) {
        return BindingBuilder.bind(notificationDlqQueue).to(notificationDlqExchange).with("notification.dlq");
    }
}
