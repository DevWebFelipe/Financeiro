-- Phase 9: invoice payment status ACTIVE/REVERSED and ownership unique for composite FKs.
-- Does not alter payments.type.

ALTER TABLE credit_card_invoice_payments
    ADD COLUMN status VARCHAR NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE credit_card_invoice_payments
    ADD CONSTRAINT ck_credit_card_invoice_payments_status
        CHECK (status IN ('ACTIVE', 'REVERSED'));

ALTER TABLE credit_card_invoice_payments
    ADD CONSTRAINT uq_credit_card_invoice_payments_id_user UNIQUE (id, user_id);
