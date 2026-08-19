export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  fields?: Record<string, string>;
}

/**
 * Frontend-only transport codes. They are not backend semantic codes.
 * Do not use them to identify business rules.
 */
export const HttpClientErrorCode = {
  Transport: 'HTTP_TRANSPORT_ERROR',
  UnparseableResponse: 'HTTP_UNPARSEABLE_RESPONSE',
} as const;

export type HttpClientErrorCode = (typeof HttpClientErrorCode)[keyof typeof HttpClientErrorCode];

export function isApiError(value: unknown): value is ApiError {
  if (!isRecord(value)) {
    return false;
  }

  return (
    typeof value['timestamp'] === 'string' &&
    typeof value['status'] === 'number' &&
    typeof value['code'] === 'string' &&
    typeof value['message'] === 'string' &&
    typeof value['path'] === 'string' &&
    (value['fields'] === undefined || isStringRecord(value['fields']))
  );
}

export function isHttpTransportError(error: ApiError): boolean {
  return error.code === HttpClientErrorCode.Transport;
}

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function isStringRecord(value: unknown): value is Record<string, string> {
  if (!isRecord(value)) {
    return false;
  }

  return Object.values(value).every((field) => typeof field === 'string');
}
