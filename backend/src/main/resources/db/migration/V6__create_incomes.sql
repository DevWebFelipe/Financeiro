CREATE TABLE incomes (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    account_id UUID,
    description VARCHAR NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    expected_date DATE NOT NULL,
    received_date DATE,
    status VARCHAR NOT NULL,
    responsible_type VARCHAR NOT NULL,
    responsible_name VARCHAR,
    notes VARCHAR,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_incomes PRIMARY KEY (id),
    CONSTRAINT uq_incomes_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_incomes_status CHECK (status IN ('EXPECTED', 'RECEIVED', 'CANCELLED')),
    CONSTRAINT ck_incomes_amount CHECK (amount > 0),
    CONSTRAINT ck_incomes_responsible_type CHECK (
        responsible_type IN ('MINE', 'GIULIA', 'EDERSON', 'ELISIANE', 'OTHER')
    ),
    CONSTRAINT ck_incomes_responsible_name CHECK (
        responsible_type <> 'OTHER' OR responsible_name IS NOT NULL
    ),
    CONSTRAINT fk_incomes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_incomes_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_incomes_category_ownership FOREIGN KEY (category_id, user_id) REFERENCES categories (id, user_id),
    CONSTRAINT fk_incomes_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_incomes_account_ownership FOREIGN KEY (account_id, user_id) REFERENCES accounts (id, user_id)
);

CREATE INDEX idx_incomes_user_id ON incomes (user_id);
CREATE INDEX idx_incomes_user_status ON incomes (user_id, status);
