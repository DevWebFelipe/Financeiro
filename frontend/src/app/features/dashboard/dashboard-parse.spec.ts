import { parseDashboardResponse } from './dashboard-parse';

describe('parseDashboardResponse', () => {
  it('returns null for an incomplete envelope', () => {
    expect(parseDashboardResponse({ asOfDate: '2026-08-17' })).toBeNull();
    expect(parseDashboardResponse(null)).toBeNull();
  });

  it('rejects a local-datetime string in DATE fields', () => {
    expect(
      parseDashboardResponse({
        asOfDate: '2026-08-17T00:00:00',
        startDate: '2026-08-17',
        endDate: '2027-07-31',
      }),
    ).toBeNull();
  });
});
