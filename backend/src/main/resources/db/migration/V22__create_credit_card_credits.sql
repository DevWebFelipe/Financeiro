-- Phase 9 card credits (RN246) and applications (allocation facts).

CREATE TABLE credit_card_credits (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    credit_card_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    reason VARCHAR NOT NULL,
    origin VARCHAR NOT NULL,
    expense_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_credit_card_credits PRIMARY KEY (id),
    CONSTRAINT uq_credit_card_credits_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_credit_card_credits_amount CHECK (amount > 0),
    CONSTRAINT ck_credit_card_credits_origin CHECK (
        origin IN ('MANUAL', 'CARD_PURCHASE_REFUND')
    ),
    CONSTRAINT fk_credit_card_credits_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_credit_card_credits_card FOREIGN KEY (credit_card_id) REFERENCES credit_cards (id),
    CONSTRAINT fk_credit_card_credits_card_ownership
        FOREIGN KEY (credit_card_id, user_id) REFERENCES credit_cards (id, user_id),
    CONSTRAINT fk_credit_card_credits_expense FOREIGN KEY (expense_id) REFERENCES expenses (id),
    CONSTRAINT fk_credit_card_credits_expense_ownership
        FOREIGN KEY (expense_id, user_id) REFERENCES expenses (id, user_id)
);

CREATE INDEX idx_credit_card_credits_card ON credit_card_credits (credit_card_id);
CREATE INDEX idx_credit_card_credits_user_created
    ON credit_card_credits (user_id, created_at, id);

CREATE TABLE credit_card_credit_applications (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    credit_id UUID NOT NULL,
    invoice_id UUID NOT NULL,
    installment_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_credit_card_credit_applications PRIMARY KEY (id),
    CONSTRAINT uq_credit_card_credit_applications_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_credit_card_credit_applications_amount CHECK (amount > 0),
    CONSTRAINT fk_credit_card_credit_applications_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_credit_card_credit_applications_credit
        FOREIGN KEY (credit_id) REFERENCES credit_card_credits (id),
    CONSTRAINT fk_credit_card_credit_applications_credit_ownership
        FOREIGN KEY (credit_id, user_id) REFERENCES credit_card_credits (id, user_id),
    CONSTRAINT fk_credit_card_credit_applications_invoice
        FOREIGN KEY (invoice_id) REFERENCES credit_card_invoices (id),
    CONSTRAINT fk_credit_card_credit_applications_invoice_ownership
        FOREIGN KEY (invoice_id, user_id) REFERENCES credit_card_invoices (id, user_id),
    CONSTRAINT fk_credit_card_credit_applications_installment
        FOREIGN KEY (installment_id) REFERENCES expense_installments (id),
    CONSTRAINT fk_credit_card_credit_applications_installment_ownership
        FOREIGN KEY (installment_id, user_id) REFERENCES expense_installments (id, user_id)
);

CREATE INDEX idx_credit_card_credit_applications_credit
    ON credit_card_credit_applications (credit_id);
CREATE INDEX idx_credit_card_credit_applications_invoice
    ON credit_card_credit_applications (invoice_id);
CREATE INDEX idx_credit_card_credit_applications_installment
    ON credit_card_credit_applications (installment_id);
