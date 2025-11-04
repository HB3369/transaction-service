package com.benas.transactions.controller;

import com.benas.transactions.domain.Transaction;
import com.benas.transactions.service.TransactionService;
import com.benas.transactions.service.TransactionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(TransactionController.class)
class TransactionControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private TransactionService service;
    
    @Test
    void shouldCreateTransaction() {
        // Arrange
        Transaction savedTransaction = new Transaction("ACC001", new BigDecimal("100.50"), "TRANSFER");
        savedTransaction.setId(1L);
        
        when(service.createTransaction(any())).thenReturn(Mono.just(savedTransaction));
        
        // Act & Assert
        webTestClient.post()
            .uri("/api/v1/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "accountId": "ACC001",
                    "amount": 100.50,
                    "type": "TRANSFER"
                }
                """)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.accountId").isEqualTo("ACC001")
            .jsonPath("$.amount").isEqualTo(100.50)
            .jsonPath("$.type").isEqualTo("TRANSFER")
            .jsonPath("$.status").isEqualTo("PENDING");
    }
    
    @Test
    void shouldRejectInvalidAmount() {
        webTestClient.post()
            .uri("/api/v1/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "accountId": "ACC001",
                    "amount": -50,
                    "type": "TRANSFER"
                }
                """)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.status").isEqualTo("error")
            .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }
    
    @Test
    void shouldRejectInvalidAccountId() {
        webTestClient.post()
            .uri("/api/v1/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "accountId": "INVALID",
                    "amount": 100,
                    "type": "TRANSFER"
                }
                """)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors.accountId").exists();
    }
    
    @Test
    void shouldRejectInvalidType() {
        webTestClient.post()
            .uri("/api/v1/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "accountId": "ACC001",
                    "amount": 100,
                    "type": "INVALID_TYPE"
                }
                """)
            .exchange()
            .expectStatus().isBadRequest();
    }
    
    @Test
    void shouldGetTransaction() {
        // Arrange
        Transaction transaction = new Transaction("ACC001", new BigDecimal("100.50"), "TRANSFER");
        transaction.setId(1L);
        
        when(service.getTransaction(1L)).thenReturn(Mono.just(transaction));
        
        // Act & Assert
        webTestClient.get()
            .uri("/api/v1/transactions/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.accountId").isEqualTo("ACC001");
    }
    
    @Test
    void shouldReturn404WhenTransactionNotFound() {
        // Arrange
        when(service.getTransaction(999L)).thenReturn(Mono.error(new TransactionNotFoundException(999L)));
        
        // Act & Assert
        webTestClient.get()
            .uri("/api/v1/transactions/999")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.status").isEqualTo("error")
            .jsonPath("$.code").isEqualTo("NOT_FOUND");
    }
}
