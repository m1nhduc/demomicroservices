package dmd.prj.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dmd.prj.common.event.OrderPlacedEvent;
import dmd.prj.orderservice.domain.Order;
import dmd.prj.orderservice.domain.Outbox;
import dmd.prj.orderservice.dto.CreateOrderRequest;
import dmd.prj.orderservice.dto.CreateOrderResponse;
import dmd.prj.orderservice.repository.OrderRepository;
import dmd.prj.orderservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public CreateOrderResponse createOrder(String userId, CreateOrderRequest request) {
        String txnId = generateTxnId(request.getProductType());
        
        Order order = new Order();
        order.setTxnId(txnId);
        order.setUserId(userId);
        order.setProductType(request.getProductType());
        order.setAmount(request.getAmount());
        order.setMarket(request.getMarket());
        orderRepository.save(order);

        OrderPlacedEvent event = new OrderPlacedEvent(
                txnId,
                userId,
                request.getProductType(),
                request.getAmount(),
                request.getMarket(),
                LocalDateTime.now()
        );

        Outbox outbox = new Outbox();
        outbox.setAggregateType("order");
        outbox.setAggregateId(txnId);
        outbox.setEventType("ORDER_PLACED");
        try {
            outbox.setPayload(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to serialize event", e);
        }
        outboxRepository.save(outbox);

        log.info("Order created: txnId={}, userId={}", txnId, userId);
        return new CreateOrderResponse(txnId, "ACCEPTED");
    }

    private String generateTxnId(String productType) {
        long timestamp = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("TXN_%s_%d_%s", productType, timestamp, uuid);
    }
}
