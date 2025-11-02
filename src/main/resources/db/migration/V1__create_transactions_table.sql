CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_type_valid CHECK (type IN ('TRANSFER', 'PAYMENT', 'WITHDRAWAL')),
    CONSTRAINT chk_status_valid CHECK (status IN ('PENDING', 'COMPLETED', 'REJECTED'))
);

CREATE INDEX idx_account_id ON transactions(account_id);
CREATE INDEX idx_created_at ON transactions(created_at DESC);

COMMENT ON TABLE transactions IS 'Tabla de transacciones bancarias';
COMMENT ON COLUMN transactions.account_id IS 'ID de la cuenta origen';
COMMENT ON COLUMN transactions.amount IS 'Monto de la transacción';
COMMENT ON COLUMN transactions.type IS 'Tipo: TRANSFER, PAYMENT, WITHDRAWAL';
COMMENT ON COLUMN transactions.status IS 'Estado: PENDING, COMPLETED, REJECTED';
