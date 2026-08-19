import {
  CashFlowType,
  ExpenseReportOrigin,
  ExpenseStatus,
  IncomeStatus,
  InvoiceReportAllocationType,
  InvoiceStatus,
  PaymentMethod,
  ReportDateType,
  ReportNature,
  ResponsibleType,
} from './reports.models';

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

export function paymentMethodLabel(method: PaymentMethod): string {
  switch (method) {
    case 'ACCOUNT':
      return 'Conta';
    case 'CREDIT_CARD':
      return 'Cartão';
    case 'NONE':
      return 'Sem conta';
    default:
      return method;
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

export function originLabel(origin: ExpenseReportOrigin): string {
  switch (origin) {
    case 'PURCHASE':
      return 'Compra';
    case 'AGREEMENT':
      return 'Acordo';
    default:
      return origin;
  }
}

export function dateTypeLabel(dateType: ReportDateType): string {
  switch (dateType) {
    case 'EXPECTED':
      return 'Competência (esperada)';
    case 'RECEIVED':
      return 'Recebimento';
    default:
      return dateType;
  }
}

export function natureLabel(nature: ReportNature): string {
  switch (nature) {
    case 'EXPENSE':
      return 'Despesas';
    case 'INCOME':
      return 'Receitas';
    case 'BOTH':
      return 'Despesas e receitas';
    default:
      return nature;
  }
}

export function cashFlowTypeLabel(type: CashFlowType): string {
  switch (type) {
    case 'INCOME_RECEIPT':
      return 'Recebimento de receita';
    case 'EXPENSE_PAYMENT':
      return 'Pagamento de despesa';
    case 'INVOICE_PAYMENT':
      return 'Pagamento de fatura';
    case 'CARD_PURCHASE_REFUND':
      return 'Estorno de compra no cartão';
    case 'TRANSFER_IN':
      return 'Transferência recebida';
    case 'TRANSFER_OUT':
      return 'Transferência enviada';
    case 'BALANCE_ADJUSTMENT':
      return 'Acerto de saldos';
    default:
      return type;
  }
}

export function invoiceStatusLabel(status: InvoiceStatus): string {
  switch (status) {
    case 'SCHEDULED':
      return 'Agendada';
    case 'OPEN':
      return 'Aberta';
    case 'CLOSED':
      return 'Fechada';
    case 'PAID':
      return 'Paga';
    case 'SETTLED_BY_AGREEMENT':
      return 'Liquidada por acordo';
    default:
      return status;
  }
}

export function allocationTypeLabel(type: InvoiceReportAllocationType): string {
  switch (type) {
    case 'PAYMENT':
      return 'Pagamento';
    case 'INVOICE_ADJUSTMENT':
      return 'Ajuste de fatura';
    case 'CREDIT':
      return 'Crédito';
    case 'SETTLEMENT':
      return 'Liquidação';
    default:
      return type;
  }
}

export function categoryTypeLabel(type: 'INCOME' | 'EXPENSE'): string {
  return type === 'INCOME' ? 'Receita' : 'Despesa';
}

export function formatQuarter(period: string): string {
  const match = /^(\d{4})-Q([1-4])$/.exec(period);
  if (match == null) {
    return period;
  }
  return `${match[2]}º trimestre de ${match[1]}`;
}
