import {
  Expense,
  ExpenseInstallment,
  ExpenseStatus,
  PaymentMethod,
  ResponsibleType,
} from './expenses.models';

export function expenseStatusLabel(status: ExpenseStatus): string {
  switch (status) {
    case 'OPEN':
      return 'Aberta';
    case 'PARTIALLY_PAID':
      return 'Parcialmente paga';
    case 'PAID':
      return 'Paga';
    case 'CANCELLED':
      return 'Cancelada';
    case 'REFUNDED':
      return 'Estornada';
    default:
      return status;
  }
}

export function paymentMethodLabel(method: PaymentMethod): string {
  switch (method) {
    case 'ACCOUNT':
      return 'Conta';
    case 'NONE':
      return 'Sem conta';
    case 'CREDIT_CARD':
      return 'Cartão';
    default:
      return method;
  }
}

export function responsibleTypeLabel(type: ResponsibleType): string {
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

export function canEditExpense(expense: Expense): boolean {
  return expense.status === 'OPEN';
}

export function canCancelExpense(expense: Expense): boolean {
  return expense.status === 'OPEN';
}

export function canPayExpense(expense: Expense): boolean {
  return (
    (expense.status === 'OPEN' || expense.status === 'PARTIALLY_PAID') &&
    expense.paymentMethod !== 'CREDIT_CARD'
  );
}

export function canRefundExpense(expense: Expense): boolean {
  return expense.status === 'PARTIALLY_PAID' || expense.status === 'PAID';
}

export function canPayInstallment(installment: ExpenseInstallment, expense: Expense): boolean {
  return (
    canPayExpense(expense) &&
    (installment.status === 'OPEN' || installment.status === 'PARTIALLY_PAID') &&
    installment.remainingAmount > 0
  );
}

export function isSingleInstallment(installments: ExpenseInstallment[]): boolean {
  return installments.length === 1 && installments[0]?.totalInstallments === 1;
}
