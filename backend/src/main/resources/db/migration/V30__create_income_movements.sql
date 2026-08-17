-- Fase 17 Parte 2: fatos financeiros de receita (ACCRUAL / RECEIPT).
-- UUID: sem DEFAULT na coluna. Backfill one-shot usa uuidv7() do PostgreSQL 18
-- (não é geração contínua pelo banco).
-- D83: cada Income RECEIVED recebe exatamente um RECEIPT ACTIVE equivalente.

CREATE TABLE income_movements (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    income_id UUID NOT NULL,
    type VARCHAR NOT NULL,
    status VARCHAR NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    movement_date DATE NOT NULL,
    account_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    reversed_at TIMESTAMPTZ,
    CONSTRAINT pk_income_movements PRIMARY KEY (id),
    CONSTRAINT uq_income_movements_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_income_movements_type CHECK (type IN ('ACCRUAL', 'RECEIPT')),
    CONSTRAINT ck_income_movements_status CHECK (status IN ('ACTIVE', 'REVERSED')),
    CONSTRAINT ck_income_movements_amount CHECK (amount > 0),
    CONSTRAINT ck_income_movements_account_by_type CHECK (
        (type = 'ACCRUAL' AND account_id IS NULL)
        OR (type = 'RECEIPT' AND account_id IS NOT NULL)
    ),
    CONSTRAINT ck_income_movements_reversed_at CHECK (
        (status = 'ACTIVE' AND reversed_at IS NULL)
        OR (status = 'REVERSED' AND reversed_at IS NOT NULL)
    ),
    CONSTRAINT fk_income_movements_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_income_movements_income FOREIGN KEY (income_id) REFERENCES incomes (id),
    CONSTRAINT fk_income_movements_income_ownership FOREIGN KEY (income_id, user_id)
        REFERENCES incomes (id, user_id),
    CONSTRAINT fk_income_movements_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_income_movements_account_ownership FOREIGN KEY (account_id, user_id)
        REFERENCES accounts (id, user_id)
);

CREATE INDEX idx_income_movements_user_income ON income_movements (user_id, income_id);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM incomes
        WHERE status = 'RECEIVED'
          AND (account_id IS NULL OR received_date IS NULL)
    ) THEN
        RAISE EXCEPTION
            'D83 backfill aborted: RECEIVED income is missing account_id or received_date';
    END IF;
END
$$;

INSERT INTO income_movements (
    id,
    user_id,
    income_id,
    type,
    status,
    amount,
    movement_date,
    account_id,
    created_at,
    updated_at,
    reversed_at
)
SELECT
    uuidv7(),
    i.user_id,
    i.id,
    'RECEIPT',
    'ACTIVE',
    i.amount,
    i.received_date,
    i.account_id,
    NOW(),
    NOW(),
    NULL
FROM incomes i
WHERE i.status = 'RECEIVED'
  AND NOT EXISTS (
      SELECT 1
      FROM income_movements m
      WHERE m.income_id = i.id
        AND m.type = 'RECEIPT'
  );

DO $$
DECLARE
    received_count bigint;
    receipt_count bigint;
BEGIN
    SELECT COUNT(*) INTO received_count FROM incomes WHERE status = 'RECEIVED';
    SELECT COUNT(*) INTO receipt_count
    FROM incomes i
    JOIN income_movements m
      ON m.income_id = i.id
     AND m.user_id = i.user_id
     AND m.type = 'RECEIPT'
     AND m.status = 'ACTIVE'
    WHERE i.status = 'RECEIVED';

    IF received_count <> receipt_count THEN
        RAISE EXCEPTION
            'D83 backfill aborted: RECEIVED count (%) does not match ACTIVE RECEIPT count (%)',
            received_count,
            receipt_count;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM incomes i
        JOIN income_movements m
          ON m.income_id = i.id
         AND m.user_id = i.user_id
         AND m.type = 'RECEIPT'
         AND m.status = 'ACTIVE'
        WHERE i.status = 'RECEIVED'
          AND (
              m.amount <> i.amount
              OR m.account_id IS DISTINCT FROM i.account_id
              OR m.movement_date IS DISTINCT FROM i.received_date
          )
    ) THEN
        RAISE EXCEPTION
            'D83 backfill aborted: ACTIVE RECEIPT does not match RECEIVED header amount/account/date';
    END IF;
END
$$;
