-- Phase 13: Agreement header (D3/D4). V13 credit_card_invoice_installments remains unused.

CREATE TABLE credit_card_invoice_agreements (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    credit_card_id UUID NOT NULL,
    source_invoice_id UUID NOT NULL,
    expense_id UUID NOT NULL,
    status VARCHAR NOT NULL,
    entry_amount NUMERIC(19, 2) NOT NULL,
    financed_amount NUMERIC(19, 2) NOT NULL,
    installment_count INTEGER NOT NULL,
    installment_amount NUMERIC(19, 2) NOT NULL,
    superseded_by_agreement_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_credit_card_invoice_agreements PRIMARY KEY (id),
    CONSTRAINT uq_credit_card_invoice_agreements_id_user UNIQUE (id, user_id),
    CONSTRAINT uq_credit_card_invoice_agreements_expense UNIQUE (expense_id),
    CONSTRAINT ck_cc_invoice_agreements_status CHECK (
        status IN ('ACTIVE', 'COMPLETED', 'RENEGOTIATED', 'CANCELLED')
    ),
    CONSTRAINT ck_cc_invoice_agreements_entry CHECK (entry_amount >= 0),
    CONSTRAINT ck_cc_invoice_agreements_financed CHECK (financed_amount > 0),
    CONSTRAINT ck_cc_invoice_agreements_count CHECK (installment_count > 0),
    CONSTRAINT ck_cc_invoice_agreements_installment_amount CHECK (installment_amount > 0),
    CONSTRAINT fk_cc_invoice_agreements_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_cc_invoice_agreements_card FOREIGN KEY (credit_card_id) REFERENCES credit_cards (id),
    CONSTRAINT fk_cc_invoice_agreements_card_ownership FOREIGN KEY (credit_card_id, user_id)
        REFERENCES credit_cards (id, user_id),
    CONSTRAINT fk_cc_invoice_agreements_invoice FOREIGN KEY (source_invoice_id)
        REFERENCES credit_card_invoices (id),
    CONSTRAINT fk_cc_invoice_agreements_invoice_ownership FOREIGN KEY (source_invoice_id, user_id)
        REFERENCES credit_card_invoices (id, user_id),
    CONSTRAINT fk_cc_invoice_agreements_expense FOREIGN KEY (expense_id) REFERENCES expenses (id),
    CONSTRAINT fk_cc_invoice_agreements_expense_ownership FOREIGN KEY (expense_id, user_id)
        REFERENCES expenses (id, user_id),
    CONSTRAINT fk_cc_invoice_agreements_superseded FOREIGN KEY (superseded_by_agreement_id)
        REFERENCES credit_card_invoice_agreements (id),
    CONSTRAINT fk_cc_invoice_agreements_superseded_ownership
        FOREIGN KEY (superseded_by_agreement_id, user_id)
        REFERENCES credit_card_invoice_agreements (id, user_id)
);

CREATE INDEX idx_cc_invoice_agreements_user_id ON credit_card_invoice_agreements (user_id);
CREATE INDEX idx_cc_invoice_agreements_card_id ON credit_card_invoice_agreements (credit_card_id);
CREATE INDEX idx_cc_invoice_agreements_invoice_id ON credit_card_invoice_agreements (source_invoice_id);
CREATE INDEX idx_cc_invoice_agreements_status ON credit_card_invoice_agreements (status);
