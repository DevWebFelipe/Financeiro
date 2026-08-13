-- payments.type is listed in the conceptual model. Official values are not defined
-- (docs/23 §269.1). Column is stored as VARCHAR without CHECK, enum, default, or NOT NULL
-- so the field exists without inventing semantics.
CREATE TABLE payments (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    expense_id UUID NOT NULL,
    installment_id UUID NOT NULL,
    account_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    payment_date DATE NOT NULL,
    type VARCHAR,
    notes VARCHAR,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT ck_payments_amount CHECK (amount > 0),
    CONSTRAINT fk_payments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_payments_expense FOREIGN KEY (expense_id) REFERENCES expenses (id),
    CONSTRAINT fk_payments_expense_ownership FOREIGN KEY (expense_id, user_id)
        REFERENCES expenses (id, user_id),
    CONSTRAINT fk_payments_installment FOREIGN KEY (installment_id) REFERENCES expense_installments (id),
    CONSTRAINT fk_payments_installment_ownership FOREIGN KEY (installment_id, user_id)
        REFERENCES expense_installments (id, user_id),
    CONSTRAINT fk_payments_installment_expense FOREIGN KEY (installment_id, expense_id)
        REFERENCES expense_installments (id, expense_id),
    CONSTRAINT fk_payments_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_payments_account_ownership FOREIGN KEY (account_id, user_id)
        REFERENCES accounts (id, user_id)
);

CREATE INDEX idx_payments_user_id ON payments (user_id);
CREATE INDEX idx_payments_expense_id ON payments (expense_id);
CREATE INDEX idx_payments_installment_id ON payments (installment_id);
CREATE INDEX idx_payments_account_id ON payments (account_id);
