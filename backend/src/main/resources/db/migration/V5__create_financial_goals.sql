CREATE TABLE financial_goals (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    name VARCHAR NOT NULL,
    description VARCHAR,
    target_amount NUMERIC(19, 2) NOT NULL,
    target_date DATE,
    status VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_financial_goals PRIMARY KEY (id),
    CONSTRAINT uq_financial_goals_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_financial_goals_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_financial_goals_target_amount CHECK (target_amount > 0),
    CONSTRAINT fk_financial_goals_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_financial_goals_user_id ON financial_goals (user_id);
