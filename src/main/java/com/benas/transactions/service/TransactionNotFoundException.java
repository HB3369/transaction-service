package com.benas.transactions.service;

public class TransactionNotFoundException extends RuntimeException {
    
    public TransactionNotFoundException(Long id) {
        super("Transaction not found: id=" + id);
    }
}
