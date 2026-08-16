-- Phase 9: last_four_digits optional (RN025B). Do not alter V4 in place.

ALTER TABLE credit_cards
    ALTER COLUMN last_four_digits DROP NOT NULL;
