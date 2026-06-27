package dmd.prj.orderservice.controller;

import dmd.prj.orderservice.dto.CreateOrderRequest;
import dmd.prj.orderservice.dto.CreateOrderResponse;
import dmd.prj.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(userId, request));
    }
}
