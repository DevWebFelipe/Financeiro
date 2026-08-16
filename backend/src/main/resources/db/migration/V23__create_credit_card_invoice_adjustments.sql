-- Phase 9 invoice adjustments (RN247A) and installment adjustment reason (RN232).

CREATE TABLE credit_card_invoice_adjustments (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    invoice_id UUID NOT NULL,
    type VARCHAR NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    reason VARCHAR NOT NULL,
    status VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_credit_card_invoice_adjustments PRIMARY KEY (id),
    CONSTRAINT uq_credit_card_invoice_adjustments_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_credit_card_invoice_adjustments_type CHECK (type IN ('DISCOUNT', 'SURCHARGE')),
    CONSTRAINT ck_credit_card_invoice_adjustments_status CHECK (status IN ('ACTIVE', 'REVERSED')),
    CONSTRAINT ck_credit_card_invoice_adjustments_amount CHECK (amount > 0),
    CONSTRAINT fk_credit_card_invoice_adjustments_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_credit_card_invoice_adjustments_invoice
        FOREIGN KEY (invoice_id) REFERENCES credit_card_invoices (id),
    CONSTRAINT fk_credit_card_invoice_adjustments_invoice_ownership
        FOREIGN KEY (invoice_id, user_id) REFERENCES credit_card_invoices (id, user_id)
);

CREATE INDEX idx_credit_card_invoice_adjustments_invoice
    ON credit_card_invoice_adjustments (invoice_id);

CREATE TABLE credit_card_invoice_adjustment_allocations (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    invoice_adjustment_id UUID NOT NULL,
    installment_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_cc_invoice_adjustment_allocations PRIMARY KEY (id),
    CONSTRAINT uq_cc_invoice_adjustment_allocations_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_cc_invoice_adjustment_allocations_amount CHECK (amount > 0),
    CONSTRAINT fk_cc_invoice_adjustment_allocations_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_cc_invoice_adjustment_allocations_adjustment
        FOREIGN KEY (invoice_adjustment_id) REFERENCES credit_card_invoice_adjustments (id),
    CONSTRAINT fk_cc_invoice_adjustment_allocations_adjustment_ownership
        FOREIGN KEY (invoice_adjustment_id, user_id)
        REFERENCES credit_card_invoice_adjustments (id, user_id),
    CONSTRAINT fk_cc_invoice_adjustment_allocations_installment
        FOREIGN KEY (installment_id) REFERENCES expense_installments (id),
    CONSTRAINT fk_cc_invoice_adjustment_allocations_installment_ownership
        FOREIGN KEY (installment_id, user_id) REFERENCES expense_installments (id, user_id)
);

CREATE INDEX idx_cc_invoice_adjustment_allocations_adjustment
    ON credit_card_invoice_adjustment_allocations (invoice_adjustment_id);
CREATE INDEX idx_cc_invoice_adjustment_allocations_installment
    ON credit_card_invoice_adjustment_allocations (installment_id);

ALTER TABLE expense_installment_adjustments
    ADD COLUMN reason VARCHAR;

ALTER TABLE expense_installment_adjustments
    ADD CONSTRAINT ck_expense_installment_adjustments_reason_not_blank
        CHECK (reason IS NULL OR length(btrim(reason)) > 0);
