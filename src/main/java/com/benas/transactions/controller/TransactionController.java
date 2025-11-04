package com.benas.transactions.controller;

import com.benas.transactions.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    
    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService service;
    
    public TransactionController(TransactionService service) {
        this.service = service;
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        log.info("POST /transactions - account: {}, amount: {}", request.accountId(), request.amount());
        
        return service.createTransaction(request)
            .map(TransactionResponse::from);
    }
    
    @GetMapping("/{id}")
    public Mono<TransactionResponse> getTransaction(@PathVariable Long id) {
        log.info("GET /transactions/{}", id);
        
        return service.getTransaction(id)
            .map(TransactionResponse::from);
    }
    
    @GetMapping
    public Flux<TransactionResponse> getTransactionsByAccount(@RequestParam String accountId) {
        log.info("GET /transactions?accountId={}", accountId);
        
        return service.getTransactionsByAccount(accountId)
            .map(TransactionResponse::from);
    }
}
