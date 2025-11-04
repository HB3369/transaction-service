package com.benas.transactions.service;

import com.benas.transactions.controller.TransactionRequest;
import com.benas.transactions.domain.Transaction;
import com.benas.transactions.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class TransactionService {
    
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionRepository repository;
    
    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }
    
    public Mono<Transaction> createTransaction(TransactionRequest request) {
        log.debug("Creating transaction for account: {}", request.accountId());
        
        long startTime = System.currentTimeMillis();
        
        return Mono.just(request)
            // Paso 1: Crea la entity desde el request
            .map(req -> new Transaction(req.accountId(), req.amount(), req.type()))
            
            // Paso 2: (Simulación) Validación de límites
            .flatMap(this::validateAccountLimits)
            
            // Paso 3: Guarda en DB (reactivo)
            .flatMap(repository::save)
            
            // Logging de éxito
            .doOnSuccess(saved -> {
                long duration = System.currentTimeMillis() - startTime;
                log.info("Transaction created: id={}, account={}, amount={}, duration={}ms", 
                    saved.getId(), saved.getAccountId(), saved.getAmount(), duration);
            })
            
            // Logging de error
            .doOnError(error -> {
                long duration = System.currentTimeMillis() - startTime;
                log.error("Error creating transaction: account={}, duration={}ms, error={}", 
                    request.accountId(), duration, error.getMessage());
            });
    }
    
    public Mono<Transaction> getTransaction(Long id) {
        log.debug("Fetching transaction: id={}", id);
        
        return repository.findById(id)
            .switchIfEmpty(Mono.error(new TransactionNotFoundException(id)))
            .doOnSuccess(tx -> log.debug("Transaction found: id={}", id));
    }
    
    public Flux<Transaction> getTransactionsByAccount(String accountId) {
        log.debug("Fetching transactions for account: {}", accountId);
        
        return repository.findByAccountId(accountId)
            .doOnComplete(() -> log.debug("Fetched transactions for account: {}", accountId));
    }
    
    // Simulación de validación (Semana 3 lo haremos con Redis)
    private Mono<Transaction> validateAccountLimits(Transaction transaction) {
        // Por ahora, simula 50ms de latencia de validación
        return Mono.just(transaction)
            .delayElement(Duration.ofMillis(50))
            .doOnNext(tx -> log.debug("Validated limits for account: {}", tx.getAccountId()));
    }
}
