CREATE TABLE credit_card_invoice_payments (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    invoice_id UUID NOT NULL,
    account_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    payment_date DATE NOT NULL,
    notes VARCHAR,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_credit_card_invoice_payments PRIMARY KEY (id),
    CONSTRAINT ck_credit_card_invoice_payments_amount CHECK (amount > 0),
    CONSTRAINT fk_credit_card_invoice_payments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_credit_card_invoice_payments_invoice FOREIGN KEY (invoice_id)
        REFERENCES credit_card_invoices (id),
    CONSTRAINT fk_credit_card_invoice_payments_invoice_ownership FOREIGN KEY (invoice_id, user_id)
        REFERENCES credit_card_invoices (id, user_id),
    CONSTRAINT fk_credit_card_invoice_payments_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_credit_card_invoice_payments_account_ownership FOREIGN KEY (account_id, user_id)
        REFERENCES accounts (id, user_id)
);

CREATE INDEX idx_credit_card_invoice_payments_user_id ON credit_card_invoice_payments (user_id);
CREATE INDEX idx_credit_card_invoice_payments_invoice_id ON credit_card_invoice_payments (invoice_id);
CREATE INDEX idx_credit_card_invoice_payments_account_id ON credit_card_invoice_payments (account_id);
