CREATE TABLE credit_cards (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    name VARCHAR NOT NULL,
    holder_name VARCHAR NOT NULL,
    last_four_digits VARCHAR NOT NULL,
    credit_limit NUMERIC(19, 2) NOT NULL,
    closing_day INTEGER NOT NULL,
    due_day INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_credit_cards PRIMARY KEY (id),
    CONSTRAINT uq_credit_cards_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_credit_cards_closing_day CHECK (closing_day BETWEEN 1 AND 31),
    CONSTRAINT ck_credit_cards_due_day CHECK (due_day BETWEEN 1 AND 31),
    CONSTRAINT fk_credit_cards_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_credit_cards_user_id ON credit_cards (user_id);
