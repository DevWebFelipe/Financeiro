-- Phase 15 foundation: goal linked to one account; redemptions; contribution inherits account from goal.

ALTER TABLE financial_goals
    ADD COLUMN account_id UUID;

UPDATE financial_goals g
SET account_id = c.account_id
FROM (
    SELECT DISTINCT ON (goal_id) goal_id, account_id
    FROM goal_contributions
    ORDER BY goal_id, created_at ASC, id ASC
) c
WHERE g.id = c.goal_id
  AND g.account_id IS NULL;

ALTER TABLE financial_goals
    ALTER COLUMN account_id SET NOT NULL;

ALTER TABLE financial_goals
    ADD CONSTRAINT fk_financial_goals_account FOREIGN KEY (account_id) REFERENCES accounts (id);

ALTER TABLE financial_goals
    ADD CONSTRAINT fk_financial_goals_account_ownership FOREIGN KEY (account_id, user_id)
        REFERENCES accounts (id, user_id);

CREATE INDEX idx_financial_goals_account_id ON financial_goals (account_id);

ALTER TABLE goal_contributions
    DROP CONSTRAINT fk_goal_contributions_account_ownership;

ALTER TABLE goal_contributions
    DROP CONSTRAINT fk_goal_contributions_account;

DROP INDEX IF EXISTS idx_goal_contributions_account_id;

ALTER TABLE goal_contributions
    DROP COLUMN account_id;

CREATE TABLE goal_redemptions (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    goal_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    redemption_date DATE NOT NULL,
    notes VARCHAR,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_goal_redemptions PRIMARY KEY (id),
    CONSTRAINT ck_goal_redemptions_amount CHECK (amount > 0),
    CONSTRAINT fk_goal_redemptions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_goal_redemptions_goal FOREIGN KEY (goal_id) REFERENCES financial_goals (id),
    CONSTRAINT fk_goal_redemptions_goal_ownership FOREIGN KEY (goal_id, user_id)
        REFERENCES financial_goals (id, user_id)
);

CREATE INDEX idx_goal_redemptions_user_id ON goal_redemptions (user_id);
CREATE INDEX idx_goal_redemptions_goal_id ON goal_redemptions (goal_id);

UPDATE accounts a
SET initial_balance_locked = TRUE
WHERE a.initial_balance_locked = FALSE
  AND (
    EXISTS (
        SELECT 1
        FROM financial_goals g
        JOIN goal_contributions c ON c.goal_id = g.id AND c.user_id = g.user_id
        WHERE g.account_id = a.id
          AND g.user_id = a.user_id
    )
    OR EXISTS (
        SELECT 1
        FROM financial_goals g
        JOIN goal_redemptions r ON r.goal_id = g.id AND r.user_id = g.user_id
        WHERE g.account_id = a.id
          AND g.user_id = a.user_id
    )
  );
