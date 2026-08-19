import { toSafeInternalUrl } from './internal-url';

describe('toSafeInternalUrl', () => {
  it('accepts an internal application path', () => {
    expect(toSafeInternalUrl('/expenses/123')).toBe('/expenses/123');
    expect(toSafeInternalUrl('/accounts?tab=1')).toBe('/accounts?tab=1');
  });

  it('rejects external URLs and protocol-relative open redirects', () => {
    expect(toSafeInternalUrl('https://site-malicioso.com')).toBeNull();
    expect(toSafeInternalUrl('http://site-malicioso.com')).toBeNull();
    expect(toSafeInternalUrl('//site-malicioso.com')).toBeNull();
    expect(toSafeInternalUrl('/\\site-malicioso.com')).toBeNull();
    expect(toSafeInternalUrl('javascript:alert(1)')).toBeNull();
  });

  it('rejects auth routes so login cannot loop', () => {
    expect(toSafeInternalUrl('/login')).toBeNull();
    expect(toSafeInternalUrl('/register')).toBeNull();
    expect(toSafeInternalUrl('/login?x=1')).toBeNull();
  });
});
