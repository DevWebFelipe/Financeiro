import { FinancialGoal, FinancialGoalStatus } from './financial-goals.models';

const progressFormat = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function financialGoalStatusLabel(status: FinancialGoalStatus): string {
  switch (status) {
    case 'ACTIVE':
      return 'Ativa';
    case 'COMPLETED':
      return 'Concluída';
    case 'CANCELLED':
      return 'Cancelada';
    default:
      return status;
  }
}

export function financialGoalProgressLabel(progressPercent: number): string {
  return `${progressFormat.format(progressPercent)}%`;
}

export function canEditFinancialGoal(goal: FinancialGoal): boolean {
  return goal.status === 'ACTIVE';
}

export function canContributeToFinancialGoal(goal: FinancialGoal): boolean {
  return goal.status === 'ACTIVE';
}

export function canRedeemFromFinancialGoal(goal: FinancialGoal): boolean {
  return goal.status === 'ACTIVE' || goal.status === 'COMPLETED';
}

export function canCompleteFinancialGoal(goal: FinancialGoal): boolean {
  return goal.status === 'ACTIVE';
}

export function canCancelFinancialGoal(goal: FinancialGoal): boolean {
  return goal.status === 'ACTIVE';
}
