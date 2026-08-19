export function invoiceStatusLabel(status: string): string {
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

export function invoiceItemStatusLabel(status: string): string {
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

export function invoicePeriodKey(year: number, month: number): string {
  return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}`;
}

export function invoicePaymentStatusLabel(status: string): string {
  switch (status) {
    case 'ACTIVE':
      return 'Ativo';
    case 'REVERSED':
      return 'Estornado';
    default:
      return status;
  }
}

export function canPayInvoice(status: string): boolean {
  return status === 'OPEN' || status === 'CLOSED';
}

export function canReverseInvoicePayment(invoiceStatus: string, paymentStatus: string): boolean {
  return canPayInvoice(invoiceStatus) && paymentStatus === 'ACTIVE';
}

export function invoiceAdjustmentTypeLabel(type: string): string {
  switch (type) {
    case 'DISCOUNT':
      return 'Desconto';
    case 'SURCHARGE':
      return 'Acréscimo';
    default:
      return type;
  }
}

export function invoiceAdjustmentStatusLabel(status: string): string {
  switch (status) {
    case 'ACTIVE':
      return 'Ativo';
    case 'REVERSED':
      return 'Estornado';
    default:
      return status;
  }
}

export function canAdjustInvoice(status: string): boolean {
  return status !== 'PAID' && status !== 'SETTLED_BY_AGREEMENT';
}

export function canReverseInvoiceAdjustment(
  invoiceStatus: string,
  adjustmentStatus: string,
): boolean {
  return canAdjustInvoice(invoiceStatus) && adjustmentStatus === 'ACTIVE';
}

export function canCreateInvoiceSurcharge(remainingAmount: number): boolean {
  return remainingAmount > 0;
}

export function invoiceAgreementStatusLabel(status: string): string {
  switch (status) {
    case 'ACTIVE':
      return 'Ativo';
    case 'COMPLETED':
      return 'Concluído';
    case 'RENEGOTIATED':
      return 'Renegociado';
    case 'CANCELLED':
      return 'Cancelado';
    default:
      return status;
  }
}

export function canCreateInvoiceAgreement(status: string, remainingAmount: number): boolean {
  return status === 'CLOSED' && remainingAmount > 0;
}

export function canPayAgreementInstallment(remainingAmount: number): boolean {
  return remainingAmount > 0;
}

export function formatAdditionalCostPercent(fraction: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'percent',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(fraction);
}

export function formatInvoiceInstantDate(value: string): string {
  const instant = new Date(value);
  if (Number.isNaN(instant.getTime())) {
    return value;
  }

  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(instant);

  const year = parts.find((part) => part.type === 'year')?.value ?? '';
  const month = parts.find((part) => part.type === 'month')?.value ?? '';
  const day = parts.find((part) => part.type === 'day')?.value ?? '';
  return `${day}/${month}/${year}`;
}
