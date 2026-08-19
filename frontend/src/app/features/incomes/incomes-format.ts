import {
  Income,
  IncomeMovement,
  IncomeMovementStatus,
  IncomeMovementType,
  IncomeStatus,
  ResponsibleType,
} from './incomes.models';

export function incomeStatusLabel(status: IncomeStatus): string {
  switch (status) {
    case 'EXPECTED':
      return 'Esperada';
    case 'RECEIVED':
      return 'Recebida';
    case 'CANCELLED':
      return 'Cancelada';
    default:
      return status;
  }
}

export function incomeMovementTypeLabel(type: IncomeMovementType): string {
  switch (type) {
    case 'ACCRUAL':
      return 'Acréscimo';
    case 'RECEIPT':
      return 'Recebimento';
    default:
      return type;
  }
}

export function incomeMovementStatusLabel(status: IncomeMovementStatus): string {
  switch (status) {
    case 'ACTIVE':
      return 'Ativa';
    case 'REVERSED':
      return 'Estornada';
    default:
      return status;
  }
}

export function responsibleTypeLabel(type: ResponsibleType | null): string {
  if (type == null) {
    return '—';
  }
  switch (type) {
    case 'MINE':
      return 'Minha';
    case 'GIULIA':
      return 'Giulia';
    case 'EDERSON':
      return 'Ederson';
    case 'ELISIANE':
      return 'Elisiane';
    case 'OTHER':
      return 'Outro';
    default:
      return type;
  }
}

export function canEditIncome(income: Income): boolean {
  return income.status === 'EXPECTED';
}

export function canCancelIncome(income: Income): boolean {
  return income.status === 'EXPECTED';
}

export function canReceiveIncome(income: Income): boolean {
  return income.status === 'EXPECTED';
}

export function canReverseMovement(movement: IncomeMovement, income: Income): boolean {
  return income.status !== 'CANCELLED' && movement.status === 'ACTIVE';
}
