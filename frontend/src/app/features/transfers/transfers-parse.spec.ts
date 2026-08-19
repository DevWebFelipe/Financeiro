import { parseTransfer, parseTransferList } from './transfers-parse';

const TRANSFER_ID = '01900000-0000-7000-8000-000000000070';
const SOURCE_ID = '01900000-0000-7000-8000-000000000003';
const DEST_ID = '01900000-0000-7000-8000-000000000004';

function transferBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: TRANSFER_ID,
    sourceAccountId: SOURCE_ID,
    destinationAccountId: DEST_ID,
    amount: 500,
    transferDate: '2026-08-10',
    description: 'Transferência',
    status: 'ACTIVE',
    createdAt: '2026-08-10T15:00:00Z',
    ...overrides,
  };
}

describe('transfers parse', () => {
  it('parses a valid transfer list and preserves order', () => {
    const second = transferBody({
      id: '01900000-0000-7000-8000-000000000071',
      amount: 80,
    });
    const parsed = parseTransferList([transferBody(), second]);

    expect(parsed).toHaveLength(2);
    expect(parsed?.[0]?.id).toBe(TRANSFER_ID);
    expect(parsed?.[1]?.amount).toBe(80);
  });

  it('parses a valid transfer with nullable description', () => {
    const parsed = parseTransfer(transferBody({ description: null }));
    expect(parsed).toMatchObject({
      id: TRANSFER_ID,
      amount: 500,
      transferDate: '2026-08-10',
      description: null,
      status: 'ACTIVE',
    });
  });

  it('rejects a paginated envelope instead of an array', () => {
    expect(parseTransferList({ items: [transferBody()] })).toBeNull();
  });

  it('rejects a transfer missing required fields', () => {
    expect(parseTransfer({ id: TRANSFER_ID, amount: 500 })).toBeNull();
  });

  it('rejects an invalid transferDate', () => {
    expect(parseTransfer(transferBody({ transferDate: '10/08/2026' }))).toBeNull();
  });

  it('rejects an invented status', () => {
    expect(parseTransfer(transferBody({ status: 'PENDING' }))).toBeNull();
  });

  it('rejects a non-numeric amount', () => {
    expect(parseTransfer(transferBody({ amount: '500.00' }))).toBeNull();
  });

  it('rejects a list when any item is invalid', () => {
    expect(parseTransferList([transferBody(), { id: TRANSFER_ID }])).toBeNull();
  });
});
