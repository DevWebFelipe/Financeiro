CREATE TABLE expense_installments (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    expense_id UUID NOT NULL,
    invoice_id UUID,
    installment_number INTEGER NOT NULL,
    total_installments INTEGER NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_expense_installments PRIMARY KEY (id),
    CONSTRAINT uq_expense_installments_id_user UNIQUE (id, user_id),
    CONSTRAINT uq_expense_installments_id_expense UNIQUE (id, expense_id),
    CONSTRAINT ck_expense_installments_status CHECK (
        status IN ('OPEN', 'PARTIALLY_PAID', 'PAID', 'CANCELLED', 'REFUNDED')
    ),
    CONSTRAINT ck_expense_installments_amount CHECK (amount > 0),
    CONSTRAINT ck_expense_installments_numbers CHECK (
        installment_number > 0
        AND total_installments > 0
        AND installment_number <= total_installments
    ),
    CONSTRAINT fk_expense_installments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_expense_installments_expense FOREIGN KEY (expense_id) REFERENCES expenses (id),
    CONSTRAINT fk_expense_installments_expense_ownership FOREIGN KEY (expense_id, user_id)
        REFERENCES expenses (id, user_id),
    CONSTRAINT fk_expense_installments_invoice FOREIGN KEY (invoice_id) REFERENCES credit_card_invoices (id),
    CONSTRAINT fk_expense_installments_invoice_ownership FOREIGN KEY (invoice_id, user_id)
        REFERENCES credit_card_invoices (id, user_id)
);

CREATE INDEX idx_expense_installments_user_invoice
    ON expense_installments (user_id, invoice_id);
CREATE INDEX idx_expense_installments_user_due_date
    ON expense_installments (user_id, due_date);
CREATE INDEX idx_expense_installments_expense_id ON expense_installments (expense_id);
