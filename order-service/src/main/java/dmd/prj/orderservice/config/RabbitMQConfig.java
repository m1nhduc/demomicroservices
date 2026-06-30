package dmd.prj.orderservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static dmd.prj.common.constant.RabbitMQConstant.*;

@Configuration
public class RabbitMQConfig {

    // ============================================
    // REDEEM COMMAND QUEUE - Wallet Mock (in order-service) consumes this
    // ============================================
    @Bean
    public DirectExchange redeemCommandExchange() {
        return new DirectExchange(REDEEM_COMMAND_EXCHANGE, true, false);
    }

    @Bean
    public Queue redeemCommandQueue() {
        return QueueBuilder.durable(REDEEM_COMMAND_QUEUE)
                .deadLetterExchange(REDEEM_COMMAND_DLQ_EXCHANGE)
                .build();
    }

    @Bean
    public Binding redeemCommandBinding(Queue redeemCommandQueue, DirectExchange redeemCommandExchange) {
        return BindingBuilder.bind(redeemCommandQueue).to(redeemCommandExchange).with(REDEEM_COMMAND_ROUTING_KEY);
    }

    // Redeem Command Dead Letter Queue
    @Bean
    public DirectExchange redeemCommandDlqExchange() {
        return new DirectExchange(REDEEM_COMMAND_DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue redeemCommandDlqQueue() {
        return QueueBuilder.durable(REDEEM_COMMAND_DLQ_QUEUE).build();
    }

    @Bean
    public Binding redeemCommandDlqBinding(Queue redeemCommandDlqQueue, DirectExchange redeemCommandDlqExchange) {
        return BindingBuilder.bind(redeemCommandDlqQueue).to(redeemCommandDlqExchange).with("redeem.command.dlq");
    }
}
