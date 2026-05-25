package com.example.TestAPI.Repository;

import com.example.TestAPI.Model.Enum.PaymentStatus;
import com.example.TestAPI.Model.Transaction;
import com.example.TestAPI.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByJobId(UUID jobId);

    List<Transaction> findByBuyerOrderByCreatedAtDesc(User buyer);

    List<Transaction> findBySellerOrderByCreatedAtDesc(User seller);

    List<Transaction> findByStatus(PaymentStatus status);
}
