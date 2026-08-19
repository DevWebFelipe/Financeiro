import {
  allocationTypeLabel,
  cashFlowTypeLabel,
  categoryTypeLabel,
  dateTypeLabel,
  expenseStatusLabel,
  formatQuarter,
  incomeStatusLabel,
  invoiceStatusLabel,
  natureLabel,
  originLabel,
  paymentMethodLabel,
  responsibleTypeLabel,
} from './reports-format';

describe('reports format labels', () => {
  it('labels expense statuses', () => {
    expect(expenseStatusLabel('OPEN')).toBe('Aberta');
    expect(expenseStatusLabel('PARTIALLY_PAID')).toBe('Parcialmente paga');
    expect(expenseStatusLabel('PAID')).toBe('Paga');
    expect(expenseStatusLabel('CANCELLED')).toBe('Cancelada');
    expect(expenseStatusLabel('REFUNDED')).toBe('Estornada');
  });

  it('labels income statuses and payment methods', () => {
    expect(incomeStatusLabel('EXPECTED')).toBe('Esperada');
    expect(incomeStatusLabel('RECEIVED')).toBe('Recebida');
    expect(paymentMethodLabel('ACCOUNT')).toBe('Conta');
    expect(paymentMethodLabel('CREDIT_CARD')).toBe('Cartão');
    expect(paymentMethodLabel('NONE')).toBe('Sem conta');
  });

  it('labels responsible types, origin and date type', () => {
    expect(responsibleTypeLabel(null)).toBe('—');
    expect(responsibleTypeLabel('MINE')).toBe('Minha');
    expect(originLabel('PURCHASE')).toBe('Compra');
    expect(originLabel('AGREEMENT')).toBe('Acordo');
    expect(dateTypeLabel('EXPECTED')).toBe('Competência (esperada)');
    expect(dateTypeLabel('RECEIVED')).toBe('Recebimento');
  });

  it('labels cash-flow types, invoice status and allocations', () => {
    expect(cashFlowTypeLabel('INCOME_RECEIPT')).toBe('Recebimento de receita');
    expect(cashFlowTypeLabel('BALANCE_ADJUSTMENT')).toBe('Acerto de saldos');
    expect(invoiceStatusLabel('SETTLED_BY_AGREEMENT')).toBe('Liquidada por acordo');
    expect(allocationTypeLabel('SETTLEMENT')).toBe('Liquidação');
    expect(natureLabel('BOTH')).toBe('Despesas e receitas');
    expect(categoryTypeLabel('EXPENSE')).toBe('Despesa');
    expect(formatQuarter('2026-Q3')).toBe('3º trimestre de 2026');
  });
});
