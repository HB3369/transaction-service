package com.benas.transactions.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record TransactionRequest(
    
    @NotBlank(message = "accountId is required")
    @Pattern(regexp = "^ACC[0-9]{3,10}$", message = "accountId must match pattern ACC + 3-10 digits")
    String accountId,
    
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    BigDecimal amount,
    
    @NotBlank(message = "type is required")
    @Pattern(regexp = "^(TRANSFER|PAYMENT|WITHDRAWAL)$", message = "type must be TRANSFER, PAYMENT or WITHDRAWAL")
    String type
) {
    
    // Constructor compacto con validación de negocio
    public TransactionRequest {
        // Normaliza el tipo a uppercase por si viene en lowercase
        if (type != null) {
            type = type.toUpperCase();
        }
    }
}
