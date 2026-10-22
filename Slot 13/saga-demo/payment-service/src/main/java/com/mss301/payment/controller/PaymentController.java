package com.mss301.payment.controller;

import com.mss301.payment.dto.PaymentRequest;
import com.mss301.payment.entity.*;
import com.mss301.payment.repository.*;
import com.mss301.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${saga.demo.response-delay-ms:0}")
    private long responseDelayMs;
    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;

    @PostMapping("/pay")
    public Payment pay(@RequestBody PaymentRequest req) {
        // Service proxy has committed before the response delay begins.
        Payment payment = paymentService.pay(req);
        if (responseDelayMs > 0) {
            try {
                Thread.sleep(responseDelayMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        return payment;
    }

    @PostMapping("/refund/{orderId}")
    public String refund(@PathVariable Long orderId) {
        paymentService.refund(orderId);
        return "REFUNDED";
    }

    @GetMapping("/wallets")
    public List<Wallet> wallets() {
        return walletRepository.findAll();
    }

    @GetMapping
    public List<Payment> payments() {
        return paymentRepository.findAll();
    }
}
