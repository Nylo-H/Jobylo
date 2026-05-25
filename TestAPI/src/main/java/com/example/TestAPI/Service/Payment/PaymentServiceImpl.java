package com.example.TestAPI.Service.Payment;

import com.example.TestAPI.DTO.Payment.PaymentResponse;
import com.example.TestAPI.Model.Enum.ActionType;
import com.example.TestAPI.Model.Enum.PaymentStatus;
import com.example.TestAPI.Model.JobOffer;
import com.example.TestAPI.Model.Transaction;
import com.example.TestAPI.Model.User;
import com.example.TestAPI.Repository.JobOfferRepository;
import com.example.TestAPI.Repository.TransactionRepository;
import com.example.TestAPI.Service.Audit.AuditService;
import com.example.TestAPI.Service.Audit.KycGuard;
import com.example.TestAPI.exception.BusinessException;
import com.example.TestAPI.exception.ErrorCode;
import com.example.TestAPI.utils.PaymentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final TransactionRepository transactionRepository;
    private final JobOfferRepository jobRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentProperties paymentProperties;
    private final KycGuard kycGuard;
    private final AuditService auditService;

    @Override
    public PaymentResponse initiatePayment(User buyer, UUID jobId) {
        kycGuard.requireVerified(buyer);

        JobOffer job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException("Offre non trouvée", ErrorCode.NOT_FOUND));

        if (job.getWorker() == null || !job.getWorker().getId().equals(buyer.getId())) {
            throw new BusinessException("Vous devez être le worker assigné pour payer", ErrorCode.FORBIDDEN);
        }

        if (job.getCreator().getId().equals(buyer.getId())) {
            throw new BusinessException("Vous ne pouvez pas payer votre propre offre", ErrorCode.BAD_REQUEST);
        }

        transactionRepository.findByJobId(jobId).ifPresent(t -> {
            throw new BusinessException("Un paiement existe déjà pour cette offre", ErrorCode.CONFLICT);
        });

        BigDecimal amount = job.getPrice();
        BigDecimal commissionPct = paymentProperties.getCommissionPercentage();
        BigDecimal commissionAmt = amount.multiply(commissionPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = amount.subtract(commissionAmt);

        Transaction transaction = Transaction.builder()
                .buyer(buyer)
                .seller(job.getCreator())
                .job(job)
                .amount(amount)
                .commissionPercentage(commissionPct)
                .commissionAmount(commissionAmt)
                .netAmount(netAmount)
                .status(PaymentStatus.HELD)
                .paymentMethod(paymentProperties.getDefaultMethod())
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        transaction = transactionRepository.save(transaction);
        paymentGateway.hold(transaction);

        auditService.log(buyer, ActionType.PAYMENT_INITIATED, "Tx: " + transaction.getId() + " Job: " + jobId + " Amount: " + amount);
        return toResponse(transaction);
    }

    @Override
    public PaymentResponse confirmDelivery(User buyer, UUID transactionId) {
        kycGuard.requireVerified(buyer);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("Transaction introuvable", ErrorCode.NOT_FOUND));

        if (!transaction.getBuyer().getId().equals(buyer.getId())) {
            throw new BusinessException("Seul l'acheteur peut confirmer la livraison", ErrorCode.FORBIDDEN);
        }

        if (transaction.getStatus() != PaymentStatus.HELD) {
            throw new BusinessException("La transaction n'est pas en statut HELD", ErrorCode.BAD_REQUEST);
        }

        transaction.setStatus(PaymentStatus.COMPLETED);
        transaction.setUpdatedAt(new Date());
        transaction = transactionRepository.save(transaction);

        paymentGateway.release(transaction);

        auditService.log(buyer, ActionType.PAYMENT_CONFIRMED, "Tx: " + transaction.getId() + " Net: " + transaction.getNetAmount());
        return toResponse(transaction);
    }

    @Override
    public PaymentResponse getTransaction(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("Transaction introuvable", ErrorCode.NOT_FOUND));
        return toResponse(transaction);
    }

    @Override
    public List<PaymentResponse> getMyTransactions(User user) {
        List<Transaction> asBuyer = transactionRepository.findByBuyerOrderByCreatedAtDesc(user);
        List<Transaction> asSeller = transactionRepository.findBySellerOrderByCreatedAtDesc(user);
        return List.of(asBuyer, asSeller).stream()
                .flatMap(List::stream)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse toResponse(Transaction t) {
        return new PaymentResponse(
                t.getId(),
                t.getJob().getId(),
                t.getJob().getTitle(),
                t.getBuyer().getId(),
                t.getBuyer().getUsername(),
                t.getSeller().getId(),
                t.getSeller().getUsername(),
                t.getAmount(),
                t.getCommissionPercentage(),
                t.getCommissionAmount(),
                t.getNetAmount(),
                t.getStatus().name(),
                t.getPaymentMethod(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
