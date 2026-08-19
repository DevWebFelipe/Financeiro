export type FinancialGoalStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface FinancialGoal {
  readonly id: string;
  readonly accountId: string;
  readonly name: string;
  readonly description: string | null;
  readonly targetAmount: number;
  readonly targetDate: string | null;
  readonly status: FinancialGoalStatus;
  readonly currentAmount: number;
  readonly progressPercent: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface FinancialGoalPage {
  readonly items: FinancialGoal[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
}

export interface GoalContribution {
  readonly id: string;
  readonly goalId: string;
  readonly amount: number;
  readonly contributionDate: string;
  readonly notes: string | null;
  readonly createdAt: string;
}

export interface GoalRedemption {
  readonly id: string;
  readonly goalId: string;
  readonly amount: number;
  readonly redemptionDate: string;
  readonly notes: string | null;
  readonly createdAt: string;
}

export interface CreateGoalContributionResult {
  readonly contribution: GoalContribution;
  readonly goal: FinancialGoal;
}

export interface CreateGoalRedemptionResult {
  readonly redemption: GoalRedemption;
  readonly goal: FinancialGoal;
}

export interface FinancialGoalListParams {
  readonly status?: FinancialGoalStatus;
  readonly page?: number;
  readonly size?: number;
}

export interface CreateFinancialGoalRequest {
  readonly accountId: string;
  readonly name: string;
  readonly description?: string;
  readonly targetAmount: number;
  readonly targetDate?: string;
}

export interface UpdateFinancialGoalRequest {
  readonly name: string;
  readonly description?: string;
  readonly targetAmount: number;
  readonly targetDate?: string;
}

export interface CreateGoalContributionRequest {
  readonly amount: number;
  readonly contributionDate: string;
  readonly notes?: string;
}

export interface CreateGoalRedemptionRequest {
  readonly amount: number;
  readonly redemptionDate: string;
  readonly notes?: string;
}

export type FinancialGoalStatusFilter = '' | FinancialGoalStatus;

export const FINANCIAL_GOAL_STATUS_OPTIONS: readonly {
  readonly value: FinancialGoalStatus;
  readonly label: string;
}[] = [
  { value: 'ACTIVE', label: 'Ativa' },
  { value: 'COMPLETED', label: 'Concluída' },
  { value: 'CANCELLED', label: 'Cancelada' },
];
