package com.example.TestAPI.Service.Payment;

import com.example.TestAPI.Model.Transaction;

public interface PaymentGateway {

    void hold(Transaction transaction);

    void release(Transaction transaction);

    void cancel(Transaction transaction);
}
