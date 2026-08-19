import {
  canCancelFinancialGoal,
  canCompleteFinancialGoal,
  canContributeToFinancialGoal,
  canEditFinancialGoal,
  canRedeemFromFinancialGoal,
  financialGoalProgressLabel,
  financialGoalStatusLabel,
} from './financial-goals-format';
import { FinancialGoal } from './financial-goals.models';

function goal(overrides: Partial<FinancialGoal> = {}): FinancialGoal {
  return {
    id: '01900000-0000-7000-8000-000000000050',
    accountId: '01900000-0000-7000-8000-000000000003',
    name: 'Viagem Chile',
    description: null,
    targetAmount: 400,
    targetDate: null,
    status: 'ACTIVE',
    currentAmount: 100,
    progressPercent: 12.5,
    createdAt: '2026-08-14T12:00:00Z',
    updatedAt: '2026-08-14T12:00:00Z',
    ...overrides,
  };
}

describe('financial-goals-format', () => {
  it('labels official financial goal statuses', () => {
    expect(financialGoalStatusLabel('ACTIVE')).toBe('Ativa');
    expect(financialGoalStatusLabel('COMPLETED')).toBe('Concluída');
    expect(financialGoalStatusLabel('CANCELLED')).toBe('Cancelada');
  });

  it('displays official progressPercent as-is without computing current/target', () => {
    expect(financialGoalProgressLabel(12.5)).toBe('12,50%');
    expect(financialGoalProgressLabel(goal().progressPercent)).not.toBe('25,00%');
  });

  it('allows edit, contribute, complete and cancel only while ACTIVE', () => {
    expect(canEditFinancialGoal(goal())).toBe(true);
    expect(canContributeToFinancialGoal(goal())).toBe(true);
    expect(canCompleteFinancialGoal(goal())).toBe(true);
    expect(canCancelFinancialGoal(goal({ currentAmount: 80 }))).toBe(true);
    expect(canEditFinancialGoal(goal({ status: 'COMPLETED' }))).toBe(false);
    expect(canContributeToFinancialGoal(goal({ status: 'COMPLETED' }))).toBe(false);
    expect(canCompleteFinancialGoal(goal({ status: 'COMPLETED' }))).toBe(false);
    expect(canCancelFinancialGoal(goal({ status: 'COMPLETED' }))).toBe(false);
  });

  it('allows redeem for ACTIVE and COMPLETED without computing a local remaining', () => {
    expect(canRedeemFromFinancialGoal(goal())).toBe(true);
    expect(canRedeemFromFinancialGoal(goal({ status: 'COMPLETED' }))).toBe(true);
    expect(canRedeemFromFinancialGoal(goal({ status: 'CANCELLED' }))).toBe(false);
  });
});
