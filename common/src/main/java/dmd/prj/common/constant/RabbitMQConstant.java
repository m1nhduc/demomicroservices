package dmd.prj.common.constant;

public class RabbitMQConstant {
    // Order Exchange
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_QUEUE = "order.queue";
    public static final String ORDER_ROUTING_KEY = "order.placed";
    
    public static final String ORDER_RETRY_EXCHANGE = "order.retry.exchange";
    public static final String ORDER_RETRY_QUEUE = "order.retry.queue";
    
    public static final String ORDER_DLQ_EXCHANGE = "order.dlq.exchange";
    public static final String ORDER_DLQ_QUEUE = "order.dlq.queue";
    
    // Redeem Command Exchange
    public static final String REDEEM_COMMAND_EXCHANGE = "redeem.command.exchange";
    public static final String REDEEM_COMMAND_QUEUE = "redeem.command.queue";
    public static final String REDEEM_COMMAND_ROUTING_KEY = "redeem.command";
    
    public static final String REDEEM_COMMAND_DLQ_EXCHANGE = "redeem.command.dlq.exchange";
    public static final String REDEEM_COMMAND_DLQ_QUEUE = "redeem.command.dlq.queue";
    
    // Redeem Result Exchange
    public static final String REDEEM_RESULT_EXCHANGE = "redeem.result.exchange";
    public static final String REDEEM_RESULT_QUEUE = "redeem.result.queue";
    public static final String REDEEM_RESULT_ROUTING_KEY = "redeem.result";
    
    public static final String REDEEM_RESULT_DLQ_EXCHANGE = "redeem.result.dlq.exchange";
    public static final String REDEEM_RESULT_DLQ_QUEUE = "redeem.result.dlq.queue";
    
    // Notification Exchange
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.event";
    
    public static final String NOTIFICATION_DLQ_EXCHANGE = "notification.dlq.exchange";
    public static final String NOTIFICATION_DLQ_QUEUE = "notification.dlq.queue";
}
