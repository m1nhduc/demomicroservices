package dmd.prj.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/auth/**")
                        .uri("lb://auth-service"))
                .route("order-service", r -> r.path("/orders/**")
                        .uri("lb://order-service"))
                .route("coupon-service", r -> r.path("/coupons/**")
                        .uri("lb://coupon-service"))
                .route("notification-service", r -> r.path("/notifications/**")
                        .uri("lb://notification-service"))
                .build();
    }
}
