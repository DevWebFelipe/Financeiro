-- Phase 14: transfer status + account balance adjustments (Acerto de Saldos)
-- + initial_balance_locked for RN010A (income reverse clears account_id; historical lock must persist)

ALTER TABLE accounts
    ADD COLUMN initial_balance_locked BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE transfers
    ADD COLUMN status VARCHAR NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE transfers
    ADD CONSTRAINT ck_transfers_status CHECK (status IN ('ACTIVE', 'REVERSED'));

CREATE INDEX idx_transfers_user_status ON transfers (user_id, status);
CREATE INDEX idx_transfers_transfer_date ON transfers (transfer_date);

CREATE TABLE account_balance_adjustments (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    account_id UUID NOT NULL,
    adjustment_date DATE NOT NULL,
    calculated_balance NUMERIC(19, 2) NOT NULL,
    reported_balance NUMERIC(19, 2) NOT NULL,
    adjustment_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_account_balance_adjustments PRIMARY KEY (id),
    CONSTRAINT uq_account_balance_adjustments_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_account_balance_adjustments_status CHECK (status IN ('ACTIVE', 'REVERSED')),
    CONSTRAINT ck_account_balance_adjustments_reported_balance CHECK (reported_balance >= 0),
    CONSTRAINT fk_account_balance_adjustments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_account_balance_adjustments_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_account_balance_adjustments_account_ownership FOREIGN KEY (account_id, user_id)
        REFERENCES accounts (id, user_id)
);

CREATE INDEX idx_account_balance_adjustments_user_id ON account_balance_adjustments (user_id);
CREATE INDEX idx_account_balance_adjustments_account_id ON account_balance_adjustments (account_id);
CREATE INDEX idx_account_balance_adjustments_account_date ON account_balance_adjustments (account_id, adjustment_date);

-- RN010A backfill: lock accounts that already have historically detectable financial movements.
-- Includes REVERSED payments/transfers/invoice payments/refunds (rows keep account_id).
-- Incomes: only currently RECEIVED still reference account_id (reverse clears it by Phase 6 contract);
-- those cases rely on application lock-on-receive going forward.
UPDATE accounts a
SET initial_balance_locked = TRUE
WHERE a.initial_balance_locked = FALSE
  AND (
    EXISTS (
        SELECT 1
        FROM incomes i
        WHERE i.account_id = a.id
          AND i.user_id = a.user_id
          AND i.status = 'RECEIVED'
    )
    OR EXISTS (
        SELECT 1
        FROM payments p
        WHERE p.account_id = a.id
          AND p.user_id = a.user_id
    )
    OR EXISTS (
        SELECT 1
        FROM credit_card_invoice_payments cip
        WHERE cip.account_id = a.id
          AND cip.user_id = a.user_id
    )
    OR EXISTS (
        SELECT 1
        FROM card_purchase_account_refunds r
        WHERE r.account_id = a.id
          AND r.user_id = a.user_id
    )
    OR EXISTS (
        SELECT 1
        FROM transfers t
        WHERE t.user_id = a.user_id
          AND (t.source_account_id = a.id OR t.destination_account_id = a.id)
    )
    OR EXISTS (
        SELECT 1
        FROM account_balance_adjustments ba
        WHERE ba.account_id = a.id
          AND ba.user_id = a.user_id
    )
  );
