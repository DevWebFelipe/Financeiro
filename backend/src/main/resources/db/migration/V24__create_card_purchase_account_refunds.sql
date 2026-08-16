-- Phase 9: ACCOUNT settlement refund of a card purchase (RN117). Not an income.

CREATE TABLE card_purchase_account_refunds (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    expense_id UUID NOT NULL,
    account_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_card_purchase_account_refunds PRIMARY KEY (id),
    CONSTRAINT uq_card_purchase_account_refunds_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_card_purchase_account_refunds_amount CHECK (amount > 0),
    CONSTRAINT fk_card_purchase_account_refunds_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_card_purchase_account_refunds_expense
        FOREIGN KEY (expense_id) REFERENCES expenses (id),
    CONSTRAINT fk_card_purchase_account_refunds_expense_ownership
        FOREIGN KEY (expense_id, user_id) REFERENCES expenses (id, user_id),
    CONSTRAINT fk_card_purchase_account_refunds_account
        FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_card_purchase_account_refunds_account_ownership
        FOREIGN KEY (account_id, user_id) REFERENCES accounts (id, user_id)
);

CREATE INDEX idx_card_purchase_account_refunds_account
    ON card_purchase_account_refunds (account_id);
CREATE INDEX idx_card_purchase_account_refunds_expense
    ON card_purchase_account_refunds (expense_id);
