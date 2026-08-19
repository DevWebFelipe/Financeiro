export function toSafeInternalUrl(candidate: string | null | undefined): string | null {
  if (candidate == null) {
    return null;
  }

  const url = candidate.trim();
  if (url.length === 0 || !url.startsWith('/') || url.startsWith('//') || url.includes('\\')) {
    return null;
  }

  if (/^[a-zA-Z][a-zA-Z+.-]*:/.test(url)) {
    return null;
  }

  try {
    const parsed = new URL(url, 'https://fc.local');
    if (parsed.username || parsed.password || parsed.host !== 'fc.local') {
      return null;
    }

    const result = `${parsed.pathname}${parsed.search}${parsed.hash}`;
    if (!result.startsWith('/') || result.startsWith('//')) {
      return null;
    }

    if (isAuthPath(result)) {
      return null;
    }

    return result;
  } catch {
    return null;
  }
}

export function isAuthPath(url: string): boolean {
  const path = url.split('?')[0]?.split('#')[0] ?? url;
  return path === '/login' || path === '/register';
}
