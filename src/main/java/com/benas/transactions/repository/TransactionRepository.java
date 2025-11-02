package com.benas.transactions.repository;

import com.benas.transactions.domain.Transaction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TransactionRepository extends ReactiveCrudRepository<Transaction, Long> {
    
    Flux<Transaction> findByAccountId(String accountId);
    
    Flux<Transaction> findByStatus(String status);
}
