-- Phase 9 persisted allocation facts (RN247). Not a derived paid_amount on installments.

CREATE TABLE credit_card_invoice_payment_allocations (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    invoice_payment_id UUID NOT NULL,
    installment_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_credit_card_invoice_payment_allocations PRIMARY KEY (id),
    CONSTRAINT uq_cc_invoice_payment_allocations_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_cc_invoice_payment_allocations_amount CHECK (amount > 0),
    CONSTRAINT fk_cc_invoice_payment_allocations_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_cc_invoice_payment_allocations_payment
        FOREIGN KEY (invoice_payment_id) REFERENCES credit_card_invoice_payments (id),
    CONSTRAINT fk_cc_invoice_payment_allocations_payment_ownership
        FOREIGN KEY (invoice_payment_id, user_id)
        REFERENCES credit_card_invoice_payments (id, user_id),
    CONSTRAINT fk_cc_invoice_payment_allocations_installment
        FOREIGN KEY (installment_id) REFERENCES expense_installments (id),
    CONSTRAINT fk_cc_invoice_payment_allocations_installment_ownership
        FOREIGN KEY (installment_id, user_id)
        REFERENCES expense_installments (id, user_id)
);

CREATE INDEX idx_cc_invoice_payment_allocations_payment
    ON credit_card_invoice_payment_allocations (invoice_payment_id);
CREATE INDEX idx_cc_invoice_payment_allocations_installment
    ON credit_card_invoice_payment_allocations (installment_id);
