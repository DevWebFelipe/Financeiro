-- Phase 13: settlement fact (D2) — reduces installment remaining without cash movement.

CREATE TABLE credit_card_invoice_agreement_settlements (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    agreement_id UUID NOT NULL,
    invoice_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_cc_invoice_agreement_settlements PRIMARY KEY (id),
    CONSTRAINT uq_cc_invoice_agreement_settlements_id_user UNIQUE (id, user_id),
    CONSTRAINT uq_cc_invoice_agreement_settlements_agreement UNIQUE (agreement_id),
    CONSTRAINT ck_cc_invoice_agreement_settlements_amount CHECK (amount > 0),
    CONSTRAINT fk_cc_invoice_agreement_settlements_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_cc_invoice_agreement_settlements_agreement FOREIGN KEY (agreement_id)
        REFERENCES credit_card_invoice_agreements (id),
    CONSTRAINT fk_cc_invoice_agreement_settlements_agreement_ownership
        FOREIGN KEY (agreement_id, user_id)
        REFERENCES credit_card_invoice_agreements (id, user_id),
    CONSTRAINT fk_cc_invoice_agreement_settlements_invoice FOREIGN KEY (invoice_id)
        REFERENCES credit_card_invoices (id),
    CONSTRAINT fk_cc_invoice_agreement_settlements_invoice_ownership
        FOREIGN KEY (invoice_id, user_id)
        REFERENCES credit_card_invoices (id, user_id)
);

CREATE TABLE credit_card_invoice_agreement_settlement_allocations (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    settlement_id UUID NOT NULL,
    installment_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_cc_invoice_agreement_settlement_allocations PRIMARY KEY (id),
    CONSTRAINT uq_cc_agr_settlement_allocations_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_cc_agr_settlement_allocations_amount CHECK (amount > 0),
    CONSTRAINT fk_cc_agr_settlement_allocations_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_cc_agr_settlement_allocations_settlement FOREIGN KEY (settlement_id)
        REFERENCES credit_card_invoice_agreement_settlements (id),
    CONSTRAINT fk_cc_agr_settlement_allocations_settlement_ownership
        FOREIGN KEY (settlement_id, user_id)
        REFERENCES credit_card_invoice_agreement_settlements (id, user_id),
    CONSTRAINT fk_cc_agr_settlement_allocations_installment FOREIGN KEY (installment_id)
        REFERENCES expense_installments (id),
    CONSTRAINT fk_cc_agr_settlement_allocations_installment_ownership
        FOREIGN KEY (installment_id, user_id)
        REFERENCES expense_installments (id, user_id)
);

CREATE INDEX idx_cc_agr_settlements_agreement ON credit_card_invoice_agreement_settlements (agreement_id);
CREATE INDEX idx_cc_agr_settlements_invoice ON credit_card_invoice_agreement_settlements (invoice_id);
CREATE INDEX idx_cc_agr_settlement_alloc_settlement
    ON credit_card_invoice_agreement_settlement_allocations (settlement_id);
CREATE INDEX idx_cc_agr_settlement_alloc_installment
    ON credit_card_invoice_agreement_settlement_allocations (installment_id);
