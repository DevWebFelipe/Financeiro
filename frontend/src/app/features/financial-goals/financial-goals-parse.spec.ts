import {
  parseCreateGoalContributionResult,
  parseCreateGoalRedemptionResult,
  parseFinancialGoal,
  parseFinancialGoalPage,
  parseGoalContributionList,
  parseGoalRedemptionList,
} from './financial-goals-parse';

const GOAL_ID = '01900000-0000-7000-8000-000000000050';
const ACCOUNT_ID = '01900000-0000-7000-8000-000000000003';
const CONTRIBUTION_ID = '01900000-0000-7000-8000-000000000051';
const REDEMPTION_ID = '01900000-0000-7000-8000-000000000052';

function goalBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: GOAL_ID,
    accountId: ACCOUNT_ID,
    name: 'Viagem Chile',
    description: 'Férias de julho',
    targetAmount: 5000,
    targetDate: '2026-12-20',
    status: 'ACTIVE',
    currentAmount: 500,
    progressPercent: 10,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

function contributionBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: CONTRIBUTION_ID,
    goalId: GOAL_ID,
    amount: 500,
    contributionDate: '2026-08-17',
    notes: 'Primeiro aporte',
    createdAt: '2026-08-17T12:00:00Z',
    ...overrides,
  };
}

function redemptionBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: REDEMPTION_ID,
    goalId: GOAL_ID,
    amount: 200,
    redemptionDate: '2026-08-18',
    notes: null,
    createdAt: '2026-08-18T12:00:00Z',
    ...overrides,
  };
}

describe('financial-goals parse', () => {
  it('parses a valid financial goal page', () => {
    const parsed = parseFinancialGoalPage({
      items: [goalBody()],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
    });

    expect(parsed).toMatchObject({
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
    });
    expect(parsed?.items[0]?.name).toBe('Viagem Chile');
    expect(parsed?.items[0]?.status).toBe('ACTIVE');
    expect(parsed?.items[0]?.progressPercent).toBe(10);
  });

  it('keeps official currentAmount and progressPercent without deriving them', () => {
    const parsed = parseFinancialGoal(
      goalBody({ currentAmount: 100, targetAmount: 400, progressPercent: 12.5 }),
    );
    expect(parsed?.currentAmount).toBe(100);
    expect(parsed?.targetAmount).toBe(400);
    expect(parsed?.progressPercent).toBe(12.5);
  });

  it('parses nullable description and targetDate', () => {
    const parsed = parseFinancialGoal(goalBody({ description: null, targetDate: null }));
    expect(parsed?.description).toBeNull();
    expect(parsed?.targetDate).toBeNull();
  });

  it('preserves contribution list order', () => {
    const parsed = parseGoalContributionList([
      contributionBody({ id: 'a', contributionDate: '2026-08-01' }),
      contributionBody({ id: 'b', contributionDate: '2026-08-02' }),
    ]);
    expect(parsed?.map((item) => item.id)).toEqual(['a', 'b']);
  });

  it('preserves redemption list order', () => {
    const parsed = parseGoalRedemptionList([
      redemptionBody({ id: 'r1' }),
      redemptionBody({ id: 'r2' }),
    ]);
    expect(parsed?.map((item) => item.id)).toEqual(['r1', 'r2']);
  });

  it('parses create contribution response with both contribution and goal', () => {
    const parsed = parseCreateGoalContributionResult({
      contribution: contributionBody(),
      goal: goalBody({ currentAmount: 500, progressPercent: 10 }),
    });
    expect(parsed?.contribution.amount).toBe(500);
    expect(parsed?.goal.currentAmount).toBe(500);
  });

  it('parses create redemption response with both redemption and goal', () => {
    const parsed = parseCreateGoalRedemptionResult({
      redemption: redemptionBody(),
      goal: goalBody({ currentAmount: 300, progressPercent: 6 }),
    });
    expect(parsed?.redemption.amount).toBe(200);
    expect(parsed?.goal.currentAmount).toBe(300);
  });

  it('rejects a page missing required fields', () => {
    expect(parseFinancialGoalPage({ items: [goalBody()], page: 0 })).toBeNull();
  });

  it('rejects an invalid targetDate', () => {
    expect(parseFinancialGoal(goalBody({ targetDate: '20/12/2026' }))).toBeNull();
  });

  it('rejects an unknown status', () => {
    expect(parseFinancialGoal(goalBody({ status: 'OPEN' }))).toBeNull();
  });

  it('rejects a contribution envelope missing the goal', () => {
    expect(parseCreateGoalContributionResult({ contribution: contributionBody() })).toBeNull();
  });
});
