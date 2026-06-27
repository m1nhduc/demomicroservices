package dmd.prj.orderservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static dmd.prj.common.constant.RabbitMQConstant.*;

@Configuration
public class RabbitMQConfig {

    // Order Exchanges & Queues
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

    @Bean
    public DirectExchange orderRetryExchange() {
        return new DirectExchange(ORDER_RETRY_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderRetryQueue() {
        return QueueBuilder.durable(ORDER_RETRY_QUEUE)
                .ttl(5000)
                .deadLetterExchange(ORDER_EXCHANGE)
                .build();
    }

    @Bean
    public Binding orderRetryBinding(Queue orderRetryQueue, DirectExchange orderRetryExchange) {
        return BindingBuilder.bind(orderRetryQueue).to(orderRetryExchange).with("order.retry");
    }

    // Redeem Result Exchanges & Queues
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
}
