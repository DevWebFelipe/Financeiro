CREATE TABLE goal_contributions (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    goal_id UUID NOT NULL,
    account_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    contribution_date DATE NOT NULL,
    notes VARCHAR,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_goal_contributions PRIMARY KEY (id),
    CONSTRAINT ck_goal_contributions_amount CHECK (amount > 0),
    CONSTRAINT fk_goal_contributions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_goal_contributions_goal FOREIGN KEY (goal_id) REFERENCES financial_goals (id),
    CONSTRAINT fk_goal_contributions_goal_ownership FOREIGN KEY (goal_id, user_id)
        REFERENCES financial_goals (id, user_id),
    CONSTRAINT fk_goal_contributions_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_goal_contributions_account_ownership FOREIGN KEY (account_id, user_id)
        REFERENCES accounts (id, user_id)
);

CREATE INDEX idx_goal_contributions_user_id ON goal_contributions (user_id);
CREATE INDEX idx_goal_contributions_goal_id ON goal_contributions (goal_id);
CREATE INDEX idx_goal_contributions_account_id ON goal_contributions (account_id);
