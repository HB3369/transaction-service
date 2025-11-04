package com.benas.transactions.controller;

import com.benas.transactions.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    String accountId,
    BigDecimal amount,
    String type,
    String status,
    LocalDateTime createdAt
) {
    
    // Factory method: convierte Entity -> Response
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getAccountId(),
            transaction.getAmount(),
            transaction.getType(),
            transaction.getStatus(),
            transaction.getCreatedAt()
        );
    }
}
