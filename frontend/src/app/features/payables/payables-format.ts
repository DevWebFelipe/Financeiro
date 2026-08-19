import { PayableItem, PayableItemType, PaymentMethod, ResponsibleType } from './payables.models';

export function payableTypeLabel(type: PayableItemType): string {
  switch (type) {
    case 'INSTALLMENT':
      return 'Parcela';
    case 'INVOICE':
      return 'Fatura';
    default:
      return type;
  }
}

export function payableStatusLabel(status: string): string {
  switch (status) {
    case 'OPEN':
      return 'Em aberto';
    case 'PARTIALLY_PAID':
      return 'Parcialmente paga';
    case 'PAID':
      return 'Paga';
    case 'CANCELLED':
      return 'Cancelada';
    case 'REFUNDED':
      return 'Estornada';
    case 'SCHEDULED':
      return 'Agendada';
    case 'CLOSED':
      return 'Fechada';
    case 'SETTLED_BY_AGREEMENT':
      return 'Liquidada por acordo';
    default:
      return status;
  }
}

export function paymentMethodLabel(method: PaymentMethod | null): string {
  if (method == null) {
    return '—';
  }
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

export function payableOriginLabel(item: PayableItem): string {
  return item.type === 'INVOICE' ? 'Fatura de cartão' : 'Parcela de despesa';
}
