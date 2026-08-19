import {
  ProjectionAccountAssignment,
  ProjectionDirection,
  ProjectionEventType,
} from './projections.models';

export function projectionEventTypeLabel(type: ProjectionEventType): string {
  switch (type) {
    case 'INCOME':
      return 'Receita';
    case 'EXPENSE':
      return 'Despesa';
    case 'CREDIT_CARD_INVOICE':
      return 'Fatura de cartão';
    case 'TRANSFER':
      return 'Transferência';
    default:
      return type;
  }
}

export function projectionDirectionLabel(direction: ProjectionDirection): string {
  switch (direction) {
    case 'IN':
      return 'Entrada';
    case 'OUT':
      return 'Saída';
    default:
      return direction;
  }
}

export function projectionAccountAssignmentLabel(assignment: ProjectionAccountAssignment): string {
  return assignment === 'UNASSIGNED' ? 'Sem conta determinada' : assignment;
}

export function projectionOverdueLabel(overdue: boolean): string {
  return overdue ? 'Vencido' : 'No prazo';
}

export function undatedEventDateLabel(): string {
  return 'Sem data';
}

export function formatProjectionQuarter(period: string): string {
  const match = /^(\d{4})-Q([1-4])$/.exec(period);
  if (match == null) {
    return period;
  }
  return `${match[2]}º trimestre de ${match[1]}`;
}
