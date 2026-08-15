-- Phase 8 persistence foundation (contract RN230, RN232, RN233, RN242).
-- Does not alter payments.type (docs/23 §269.1).
-- Does not create derived amount columns.

ALTER TABLE payments
    ADD COLUMN status VARCHAR NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_status CHECK (status IN ('ACTIVE', 'REVERSED'));

CREATE TABLE expense_installment_adjustments (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    installment_id UUID NOT NULL,
    type VARCHAR NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_expense_installment_adjustments PRIMARY KEY (id),
    CONSTRAINT ck_expense_installment_adjustments_type CHECK (
        type IN ('DISCOUNT', 'SURCHARGE')
    ),
    CONSTRAINT ck_expense_installment_adjustments_status CHECK (
        status IN ('ACTIVE', 'REVERSED')
    ),
    CONSTRAINT ck_expense_installment_adjustments_amount CHECK (amount > 0),
    CONSTRAINT fk_expense_installment_adjustments_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_expense_installment_adjustments_installment
        FOREIGN KEY (installment_id) REFERENCES expense_installments (id),
    CONSTRAINT fk_expense_installment_adjustments_installment_ownership
        FOREIGN KEY (installment_id, user_id)
        REFERENCES expense_installments (id, user_id)
);

CREATE INDEX idx_expense_installment_adjustments_user_id
    ON expense_installment_adjustments (user_id);
CREATE INDEX idx_expense_installment_adjustments_installment_id
    ON expense_installment_adjustments (installment_id);

ALTER TABLE expense_installments
    ADD CONSTRAINT uq_expense_installments_expense_number
    UNIQUE (expense_id, installment_number);
