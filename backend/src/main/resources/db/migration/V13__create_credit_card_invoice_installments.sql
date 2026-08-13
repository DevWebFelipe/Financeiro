-- status values for invoice-remainder installments are not listed in docs/20–28.
-- Column exists as VARCHAR without CHECK or PostgreSQL ENUM (docs/23 §269 governance).
CREATE TABLE credit_card_invoice_installments (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    invoice_id UUID NOT NULL,
    installment_number INTEGER NOT NULL,
    total_installments INTEGER NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_credit_card_invoice_installments PRIMARY KEY (id),
    CONSTRAINT ck_credit_card_invoice_installments_amount CHECK (amount > 0),
    CONSTRAINT ck_credit_card_invoice_installments_numbers CHECK (
        installment_number > 0
        AND total_installments > 0
        AND installment_number <= total_installments
    ),
    CONSTRAINT fk_credit_card_invoice_installments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_credit_card_invoice_installments_invoice FOREIGN KEY (invoice_id)
        REFERENCES credit_card_invoices (id),
    CONSTRAINT fk_credit_card_invoice_installments_invoice_ownership FOREIGN KEY (invoice_id, user_id)
        REFERENCES credit_card_invoices (id, user_id)
);

CREATE INDEX idx_credit_card_invoice_installments_user_id ON credit_card_invoice_installments (user_id);
CREATE INDEX idx_credit_card_invoice_installments_invoice_id ON credit_card_invoice_installments (invoice_id);
