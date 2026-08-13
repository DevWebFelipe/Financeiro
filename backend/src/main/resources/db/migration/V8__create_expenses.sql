CREATE TABLE expenses (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    account_id UUID,
    credit_card_id UUID,
    description VARCHAR NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    expense_date DATE NOT NULL,
    due_date DATE NOT NULL,
    payment_method VARCHAR NOT NULL,
    status VARCHAR NOT NULL,
    responsible_type VARCHAR NOT NULL,
    responsible_name VARCHAR,
    barcode VARCHAR,
    notes VARCHAR,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_expenses PRIMARY KEY (id),
    CONSTRAINT uq_expenses_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_expenses_payment_method CHECK (payment_method IN ('ACCOUNT', 'CREDIT_CARD', 'NONE')),
    CONSTRAINT ck_expenses_status CHECK (
        status IN ('OPEN', 'PARTIALLY_PAID', 'PAID', 'CANCELLED', 'REFUNDED')
    ),
    CONSTRAINT ck_expenses_total_amount CHECK (total_amount > 0),
    CONSTRAINT ck_expenses_responsible_type CHECK (
        responsible_type IN ('MINE', 'GIULIA', 'EDERSON', 'ELISIANE', 'OTHER')
    ),
    CONSTRAINT ck_expenses_responsible_name CHECK (
        responsible_type <> 'OTHER' OR responsible_name IS NOT NULL
    ),
    CONSTRAINT ck_expenses_payment_targets CHECK (
        (
            payment_method = 'ACCOUNT'
            AND account_id IS NOT NULL
            AND credit_card_id IS NULL
        )
        OR (
            payment_method = 'CREDIT_CARD'
            AND credit_card_id IS NOT NULL
            AND account_id IS NULL
        )
        OR (
            payment_method = 'NONE'
            AND credit_card_id IS NULL
        )
    ),
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_expenses_category_ownership FOREIGN KEY (category_id, user_id)
        REFERENCES categories (id, user_id),
    CONSTRAINT fk_expenses_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_expenses_account_ownership FOREIGN KEY (account_id, user_id)
        REFERENCES accounts (id, user_id),
    CONSTRAINT fk_expenses_credit_card FOREIGN KEY (credit_card_id) REFERENCES credit_cards (id),
    CONSTRAINT fk_expenses_credit_card_ownership FOREIGN KEY (credit_card_id, user_id)
        REFERENCES credit_cards (id, user_id)
);

CREATE INDEX idx_expenses_user_id ON expenses (user_id);
CREATE INDEX idx_expenses_user_due_date ON expenses (user_id, due_date);
CREATE INDEX idx_expenses_user_status ON expenses (user_id, status);
CREATE INDEX idx_expenses_account_id ON expenses (account_id);
CREATE INDEX idx_expenses_credit_card_id ON expenses (credit_card_id);
CREATE INDEX idx_expenses_category_id ON expenses (category_id);
