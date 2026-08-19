import {
  formatProjectionQuarter,
  projectionAccountAssignmentLabel,
  projectionDirectionLabel,
  projectionEventTypeLabel,
  projectionOverdueLabel,
  undatedEventDateLabel,
} from './projections-format';

describe('projections-format', () => {
  it('labels official event types', () => {
    expect(projectionEventTypeLabel('INCOME')).toBe('Receita');
    expect(projectionEventTypeLabel('EXPENSE')).toBe('Despesa');
    expect(projectionEventTypeLabel('CREDIT_CARD_INVOICE')).toBe('Fatura de cartão');
    expect(projectionEventTypeLabel('TRANSFER')).toBe('Transferência');
  });

  it('labels official directions', () => {
    expect(projectionDirectionLabel('IN')).toBe('Entrada');
    expect(projectionDirectionLabel('OUT')).toBe('Saída');
  });

  it('labels official account assignment and overdue without inferring values', () => {
    expect(projectionAccountAssignmentLabel('UNASSIGNED')).toBe('Sem conta determinada');
    expect(projectionOverdueLabel(true)).toBe('Vencido');
    expect(projectionOverdueLabel(false)).toBe('No prazo');
  });

  it('uses Sem data for undated events instead of a fabricated date', () => {
    expect(undatedEventDateLabel()).toBe('Sem data');
  });

  it('formats a calendar quarter for presentation', () => {
    expect(formatProjectionQuarter('2026-Q4')).toBe('4º trimestre de 2026');
    expect(formatProjectionQuarter('2026-Q1')).toBe('1º trimestre de 2026');
  });
});
