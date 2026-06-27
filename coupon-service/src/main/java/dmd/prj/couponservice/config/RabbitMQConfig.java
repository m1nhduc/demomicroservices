package dmd.prj.couponservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static dmd.prj.common.constant.RabbitMQConstant.*;

@Configuration
public class RabbitMQConfig {

    // Order Exchanges
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).build();
    }

    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
    }

    // Redeem Command Exchanges
    @Bean
    public DirectExchange redeemCommandExchange() {
        return new DirectExchange(REDEEM_COMMAND_EXCHANGE, true, false);
    }

    @Bean
    public Queue redeemCommandQueue() {
        return QueueBuilder.durable(REDEEM_COMMAND_QUEUE).build();
    }

    @Bean
    public Binding redeemCommandBinding(Queue redeemCommandQueue, DirectExchange redeemCommandExchange) {
        return BindingBuilder.bind(redeemCommandQueue).to(redeemCommandExchange).with(REDEEM_COMMAND_ROUTING_KEY);
    }

    // Redeem Result Exchanges
    @Bean
    public DirectExchange redeemResultExchange() {
        return new DirectExchange(REDEEM_RESULT_EXCHANGE, true, false);
    }

    @Bean
    public Queue redeemResultQueue() {
        return QueueBuilder.durable(REDEEM_RESULT_QUEUE).build();
    }

    @Bean
    public Binding redeemResultBinding(Queue redeemResultQueue, DirectExchange redeemResultExchange) {
        return BindingBuilder.bind(redeemResultQueue).to(redeemResultExchange).with(REDEEM_RESULT_ROUTING_KEY);
    }

    // Notification Exchanges
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(NOTIFICATION_ROUTING_KEY);
    }
}
