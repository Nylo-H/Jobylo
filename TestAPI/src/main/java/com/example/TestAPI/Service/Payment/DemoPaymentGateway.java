package com.example.TestAPI.Service.Payment;

import com.example.TestAPI.Model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DemoPaymentGateway implements PaymentGateway {

    @Override
    public void hold(Transaction transaction) {
        log.info("[DEMO PAIEMENT] Fonds bloqués : {} XAF (commission: {} XAF, net: {} XAF)",
                transaction.getAmount(), transaction.getCommissionAmount(), transaction.getNetAmount());
        log.info("[DEMO PAIEMENT] Transaction ID: {}", transaction.getId());
    }

    @Override
    public void release(Transaction transaction) {
        log.info("[DEMO PAIEMENT] Fonds débloqués vers le vendeur {} : {} XAF",
                transaction.getSeller().getUsername(), transaction.getNetAmount());
    }

    @Override
    public void cancel(Transaction transaction) {
        log.info("[DEMO PAIEMENT] Transaction annulée, fonds libérés : {} XAF", transaction.getAmount());
    }
}
