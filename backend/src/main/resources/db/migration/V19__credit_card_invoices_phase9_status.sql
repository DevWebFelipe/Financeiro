-- Phase 9 invoice statuses: SCHEDULED / OPEN / CLOSED / PAID (not PARTIALLY_PAID).
-- Do not alter V7 in place.

ALTER TABLE credit_card_invoices
    DROP CONSTRAINT ck_credit_card_invoices_status;

ALTER TABLE credit_card_invoices
    ADD CONSTRAINT ck_credit_card_invoices_status CHECK (
        status IN ('SCHEDULED', 'OPEN', 'CLOSED', 'PAID')
    );

CREATE UNIQUE INDEX uq_credit_card_invoices_one_open
    ON credit_card_invoices (credit_card_id)
    WHERE status = 'OPEN';
