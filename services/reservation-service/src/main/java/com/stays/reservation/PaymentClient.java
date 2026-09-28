package com.stays.reservation;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaymentClient {
    private final RestClient client;

    public PaymentClient(RestClient.Builder builder, ServiceUrls urls) {
        this.client = builder.baseUrl(urls.payments()).build();
    }

    public Payment charge(PaymentRequest request) {
        return client.post().uri("/api/payments").body(request).retrieve().body(Payment.class);
    }

    public Payment refund(UUID reservationId) {
        return client.put().uri("/api/payments/{reservationId}/refund", reservationId)
                .retrieve().body(Payment.class);
    }
}
