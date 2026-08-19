import {
  canAdjustInvoice,
  canCreateInvoiceSurcharge,
  canPayInvoice,
  canReverseInvoiceAdjustment,
  canReverseInvoicePayment,
  formatInvoiceInstantDate,
  invoiceAdjustmentStatusLabel,
  invoiceAdjustmentTypeLabel,
  invoiceItemStatusLabel,
  invoicePaymentStatusLabel,
  invoicePeriodKey,
  invoiceStatusLabel,
} from './invoices-format';

describe('invoices-format', () => {
  it('labels official invoice statuses', () => {
    expect(invoiceStatusLabel('SCHEDULED')).toBe('Agendada');
    expect(invoiceStatusLabel('OPEN')).toBe('Aberta');
    expect(invoiceStatusLabel('CLOSED')).toBe('Fechada');
    expect(invoiceStatusLabel('PAID')).toBe('Paga');
    expect(invoiceStatusLabel('SETTLED_BY_AGREEMENT')).toBe('Liquidada por acordo');
  });

  it('formats the official year and month as a YYYY-MM key', () => {
    expect(invoicePeriodKey(2026, 8)).toBe('2026-08');
  });

  it('labels official installment statuses without inventing invoice statuses', () => {
    expect(invoiceItemStatusLabel('PARTIALLY_PAID')).toBe('Parcialmente paga');
    expect(invoiceItemStatusLabel('OPEN')).toBe('Aberta');
  });

  it('labels payment statuses separately from invoice statuses', () => {
    expect(invoicePaymentStatusLabel('ACTIVE')).toBe('Ativo');
    expect(invoicePaymentStatusLabel('REVERSED')).toBe('Estornado');
  });

  it('labels official adjustment types and statuses', () => {
    expect(invoiceAdjustmentTypeLabel('DISCOUNT')).toBe('Desconto');
    expect(invoiceAdjustmentTypeLabel('SURCHARGE')).toBe('Acréscimo');
    expect(invoiceAdjustmentStatusLabel('ACTIVE')).toBe('Ativo');
    expect(invoiceAdjustmentStatusLabel('REVERSED')).toBe('Estornado');
  });

  it('allows adjustments except on terminal invoices', () => {
    expect(canAdjustInvoice('OPEN')).toBe(true);
    expect(canAdjustInvoice('CLOSED')).toBe(true);
    expect(canAdjustInvoice('SCHEDULED')).toBe(true);
    expect(canAdjustInvoice('PAID')).toBe(false);
    expect(canAdjustInvoice('SETTLED_BY_AGREEMENT')).toBe(false);
  });

  it('allows reverse only for ACTIVE adjustments on non-terminal invoices', () => {
    expect(canReverseInvoiceAdjustment('CLOSED', 'ACTIVE')).toBe(true);
    expect(canReverseInvoiceAdjustment('OPEN', 'REVERSED')).toBe(false);
    expect(canReverseInvoiceAdjustment('PAID', 'ACTIVE')).toBe(false);
    expect(canReverseInvoiceAdjustment('SETTLED_BY_AGREEMENT', 'ACTIVE')).toBe(false);
  });

  it('blocks surcharge when official remaining is not greater than zero', () => {
    expect(canCreateInvoiceSurcharge(0.01)).toBe(true);
    expect(canCreateInvoiceSurcharge(0)).toBe(false);
    expect(canCreateInvoiceSurcharge(-1)).toBe(false);
  });

  it('formats adjustment createdAt as a civil date in America/Sao_Paulo', () => {
    expect(formatInvoiceInstantDate('2026-08-20T12:00:00Z')).toBe('20/08/2026');
  });

  it('allows payment only for OPEN and CLOSED invoices', () => {
    expect(canPayInvoice('OPEN')).toBe(true);
    expect(canPayInvoice('CLOSED')).toBe(true);
    expect(canPayInvoice('SCHEDULED')).toBe(false);
    expect(canPayInvoice('PAID')).toBe(false);
    expect(canPayInvoice('SETTLED_BY_AGREEMENT')).toBe(false);
  });

  it('allows reverse only for ACTIVE payments on payable invoices', () => {
    expect(canReverseInvoicePayment('CLOSED', 'ACTIVE')).toBe(true);
    expect(canReverseInvoicePayment('OPEN', 'REVERSED')).toBe(false);
    expect(canReverseInvoicePayment('PAID', 'ACTIVE')).toBe(false);
    expect(canReverseInvoicePayment('SETTLED_BY_AGREEMENT', 'ACTIVE')).toBe(false);
  });
});
