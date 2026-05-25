package com.example.TestAPI.Controller;

import com.example.TestAPI.DTO.Payment.ConfirmDeliveryRequest;
import com.example.TestAPI.DTO.Payment.InitiatePaymentRequest;
import com.example.TestAPI.DTO.Payment.PaymentResponse;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Service.Payment.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody InitiatePaymentRequest request) {

        PaymentResponse response = paymentService.initiatePayment(currentUser, request.jobId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> confirmDelivery(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ConfirmDeliveryRequest request) {

        PaymentResponse response = paymentService.confirmDelivery(currentUser, request.transactionId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> getTransaction(@PathVariable UUID transactionId) {
        PaymentResponse response = paymentService.getTransaction(transactionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentResponse>> getMyTransactions(
            @AuthenticationPrincipal User currentUser) {

        List<PaymentResponse> transactions = paymentService.getMyTransactions(currentUser);
        return ResponseEntity.ok(transactions);
    }
}
