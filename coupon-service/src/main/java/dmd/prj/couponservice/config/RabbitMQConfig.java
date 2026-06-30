package dmd.prj.couponservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static dmd.prj.common.constant.RabbitMQConstant.*;

@Configuration
public class RabbitMQConfig {

    // ============================================
    // ORDER QUEUE - Coupon Service consumes this
    // ============================================
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .deadLetterExchange(ORDER_DLQ_EXCHANGE)
                .build();
    }

    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
    }

    // Order Dead Letter Queue
    @Bean
    public DirectExchange orderDlqExchange() {
        return new DirectExchange(ORDER_DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderDlqQueue() {
        return QueueBuilder.durable(ORDER_DLQ_QUEUE).build();
    }

    @Bean
    public Binding orderDlqBinding(Queue orderDlqQueue, DirectExchange orderDlqExchange) {
        return BindingBuilder.bind(orderDlqQueue).to(orderDlqExchange).with("order.dlq");
    }

    // ============================================
    // REDEEM RESULT QUEUE - Coupon Service consumes this
    // ============================================
    @Bean
    public DirectExchange redeemResultExchange() {
        return new DirectExchange(REDEEM_RESULT_EXCHANGE, true, false);
    }

    @Bean
    public Queue redeemResultQueue() {
        return QueueBuilder.durable(REDEEM_RESULT_QUEUE)
                .deadLetterExchange(REDEEM_RESULT_DLQ_EXCHANGE)
                .build();
    }

    @Bean
    public Binding redeemResultBinding(Queue redeemResultQueue, DirectExchange redeemResultExchange) {
        return BindingBuilder.bind(redeemResultQueue).to(redeemResultExchange).with(REDEEM_RESULT_ROUTING_KEY);
    }

    // Redeem Result Dead Letter Queue
    @Bean
    public DirectExchange redeemResultDlqExchange() {
        return new DirectExchange(REDEEM_RESULT_DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue redeemResultDlqQueue() {
        return QueueBuilder.durable(REDEEM_RESULT_DLQ_QUEUE).build();
    }

    @Bean
    public Binding redeemResultDlqBinding(Queue redeemResultDlqQueue, DirectExchange redeemResultDlqExchange) {
        return BindingBuilder.bind(redeemResultDlqQueue).to(redeemResultDlqExchange).with("redeem.result.dlq");
    }
}
