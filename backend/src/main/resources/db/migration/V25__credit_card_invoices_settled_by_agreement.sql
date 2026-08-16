-- Phase 13: terminal invoice status SETTLED_BY_AGREEMENT (D1). Do not alter V19 in place.

ALTER TABLE credit_card_invoices
    DROP CONSTRAINT ck_credit_card_invoices_status;

ALTER TABLE credit_card_invoices
    ADD CONSTRAINT ck_credit_card_invoices_status CHECK (
        status IN ('SCHEDULED', 'OPEN', 'CLOSED', 'PAID', 'SETTLED_BY_AGREEMENT')
    );
