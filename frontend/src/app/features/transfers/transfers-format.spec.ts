import { canReverseTransfer, transferStatusLabel } from './transfers-format';
import { Transfer } from './transfers.models';

function transfer(overrides: Partial<Transfer> = {}): Transfer {
  return {
    id: '01900000-0000-7000-8000-000000000070',
    sourceAccountId: '01900000-0000-7000-8000-000000000003',
    destinationAccountId: '01900000-0000-7000-8000-000000000004',
    amount: 500,
    transferDate: '2026-08-10',
    description: 'Transferência',
    status: 'ACTIVE',
    createdAt: '2026-08-10T15:00:00Z',
    ...overrides,
  };
}

describe('transfers-format', () => {
  it('labels official transfer statuses', () => {
    expect(transferStatusLabel('ACTIVE')).toBe('Ativa');
    expect(transferStatusLabel('REVERSED')).toBe('Estornada');
  });

  it('allows reverse only while the transfer is ACTIVE', () => {
    expect(canReverseTransfer(transfer({ status: 'ACTIVE' }))).toBe(true);
    expect(canReverseTransfer(transfer({ status: 'REVERSED' }))).toBe(false);
  });
});
