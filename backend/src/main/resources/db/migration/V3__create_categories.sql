CREATE TABLE categories (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    name VARCHAR NOT NULL,
    type VARCHAR NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories_id_user UNIQUE (id, user_id),
    CONSTRAINT uq_categories_user_name_type UNIQUE (user_id, name, type),
    CONSTRAINT ck_categories_type CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_categories_user_id ON categories (user_id);
