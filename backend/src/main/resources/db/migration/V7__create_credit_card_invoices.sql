CREATE TABLE credit_card_invoices (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    credit_card_id UUID NOT NULL,
    reference_year INTEGER NOT NULL,
    reference_month INTEGER NOT NULL,
    closing_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_credit_card_invoices PRIMARY KEY (id),
    CONSTRAINT uq_credit_card_invoices_id_user UNIQUE (id, user_id),
    CONSTRAINT uq_credit_card_invoices_card_cycle UNIQUE (credit_card_id, reference_year, reference_month),
    CONSTRAINT ck_credit_card_invoices_status CHECK (
        status IN ('OPEN', 'CLOSED', 'PARTIALLY_PAID', 'PAID')
    ),
    CONSTRAINT ck_credit_card_invoices_reference_month CHECK (reference_month BETWEEN 1 AND 12),
    CONSTRAINT fk_credit_card_invoices_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_credit_card_invoices_card FOREIGN KEY (credit_card_id) REFERENCES credit_cards (id),
    CONSTRAINT fk_credit_card_invoices_card_ownership FOREIGN KEY (credit_card_id, user_id)
        REFERENCES credit_cards (id, user_id)
);

CREATE INDEX idx_credit_card_invoices_user_card_cycle
    ON credit_card_invoices (user_id, credit_card_id, reference_year, reference_month);
