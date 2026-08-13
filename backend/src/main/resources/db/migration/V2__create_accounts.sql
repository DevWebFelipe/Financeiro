CREATE TABLE accounts (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    name VARCHAR NOT NULL,
    type VARCHAR NOT NULL,
    initial_balance NUMERIC(19, 2) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uq_accounts_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_accounts_type CHECK (type IN ('BANK_ACCOUNT', 'CASH')),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_accounts_user_id ON accounts (user_id);
