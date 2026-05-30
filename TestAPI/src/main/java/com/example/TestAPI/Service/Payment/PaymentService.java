package com.example.TestAPI.Service.Payment;

import com.example.TestAPI.DTO.Payment.PaymentResponse;
import com.example.TestAPI.Model.User;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse initiatePayment(User currentUser, UUID jobId);

    PaymentResponse confirmDelivery(User currentUser, UUID transactionId);

    PaymentResponse getTransaction(UUID transactionId);

    List<PaymentResponse> getMyTransactions(User user);
}
