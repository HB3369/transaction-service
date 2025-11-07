package com.benas.transactions.service;

import com.benas.transactions.controller.TransactionRequest;
import com.benas.transactions.domain.Transaction;
import com.benas.transactions.repository.TransactionRepository;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final MeterRegistry meterRegistry;
    
    public TransactionService(TransactionRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * Crea una nueva transacción.
     * 
     * Métricas:
     * - @Counted: cuenta cuántas transacciones se crean
     * - @Timed: mide cuánto tarda la creación (p50, p95, p99)
     */
    @Counted(
        value = "transactions.created",
        description = "Total number of transactions created"
    )
    @Timed(
        value = "transactions.creation.time",
        description = "Time taken to create a transaction",
        percentiles = {0.5, 0.95, 0.99}
    )
    public Mono<Transaction> createTransaction(TransactionRequest request) {
        log.debug("Creating transaction for account: {}", request.accountId());
        
        // Timer manual para medir pasos específicos
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return Mono.just(request)
            // Paso 1: Crea entity
            .map(req -> new Transaction(req.accountId(), req.amount(), req.type()))
            
            // Paso 2: Valida límites (simulado)
            .flatMap(this::validateAccountLimits)
            
            // Paso 3: Guarda en DB
            .flatMap(transaction -> {
                // Métrica: total de transacciones (manual, más confiable que @Counted)
                meterRegistry.counter("transactions.created").increment();

                // Métrica custom: cuenta por tipo de transacción
                meterRegistry.counter(
                    "transactions.created.by.type",
                    "type", transaction.getType()
                ).increment();
                
                return repository.save(transaction);
            })
            
            // Success: guarda tiempo total
            .doOnSuccess(saved -> {
                sample.stop(Timer.builder("transactions.save.time")
                    .description("Time to save transaction to DB")
                    .register(meterRegistry));
                
                log.info("Transaction created: id={}, account={}, amount={}", 
                    saved.getId(), saved.getAccountId(), saved.getAmount());
            })
            
            // Error: cuenta errores por tipo
            .doOnError(error -> {
                meterRegistry.counter(
                    "transactions.creation.errors",
                    "error", error.getClass().getSimpleName()
                ).increment();
                
                log.error("Error creating transaction: account={}, error={}", 
                    request.accountId(), error.getMessage());
            });
    }
    
    @Timed(
        value = "transactions.fetch.time",
        description = "Time to fetch a single transaction"
    )
    public Mono<Transaction> getTransaction(Long id) {
        log.debug("Fetching transaction: id={}", id);
        
        return repository.findById(id)
            .switchIfEmpty(Mono.error(new TransactionNotFoundException(id)))
            .doOnSuccess(tx -> {
                // Métrica: transacciones consultadas
                meterRegistry.counter("transactions.fetched").increment();
            });
    }
    
    @Timed(value = "transactions.fetch.by.account.time")
    public Flux<Transaction> getTransactionsByAccount(String accountId) {
        log.debug("Fetching transactions for account: {}", accountId);
        
        return repository.findByAccountId(accountId)
            .doOnComplete(() -> {
                meterRegistry.counter("transactions.queries.by.account").increment();
            });
    }
    
    /**
     * Simula validación de límites de cuenta.
     * 
     * En Semana 3 esto será una llamada real a Redis.
     * Por ahora, simula 50ms de latencia.
     */
    private Mono<Transaction> validateAccountLimits(Transaction transaction) {
        return Mono.just(transaction)
            .delayElement(Duration.ofMillis(50))
            .doOnNext(tx -> {
                log.debug("Validated limits for account: {}", tx.getAccountId());
                
                // Métrica: validaciones realizadas
                meterRegistry.counter("transactions.validations").increment();
            });
    }
}
