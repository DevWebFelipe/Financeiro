CREATE TABLE transfers (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    source_account_id UUID NOT NULL,
    destination_account_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    transfer_date DATE NOT NULL,
    description VARCHAR,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_transfers PRIMARY KEY (id),
    CONSTRAINT ck_transfers_amount CHECK (amount > 0),
    CONSTRAINT ck_transfers_different_accounts CHECK (source_account_id <> destination_account_id),
    CONSTRAINT fk_transfers_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_transfers_source_account FOREIGN KEY (source_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transfers_source_account_ownership FOREIGN KEY (source_account_id, user_id)
        REFERENCES accounts (id, user_id),
    CONSTRAINT fk_transfers_destination_account FOREIGN KEY (destination_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transfers_destination_account_ownership FOREIGN KEY (destination_account_id, user_id)
        REFERENCES accounts (id, user_id)
);

CREATE INDEX idx_transfers_user_id ON transfers (user_id);
CREATE INDEX idx_transfers_source_account_id ON transfers (source_account_id);
CREATE INDEX idx_transfers_destination_account_id ON transfers (destination_account_id);
