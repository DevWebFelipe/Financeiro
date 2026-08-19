import { HttpErrorResponse } from '@angular/common/http';
import { ApiError, HttpClientErrorCode, isApiError, isRecord } from './api-error';

export function normalizeHttpError(error: unknown, requestUrl = ''): ApiError {
  if (isApiError(error)) {
    return error;
  }

  if (error instanceof HttpErrorResponse) {
    return normalizeHttpErrorResponse(error, requestUrl);
  }

  return createTransportError(requestUrl, technicalMessage(error));
}

function normalizeHttpErrorResponse(error: HttpErrorResponse, requestUrl: string): ApiError {
  const path = error.url || requestUrl;
  const body = parseErrorBody(error.error);

  if (isTransportFailure(error)) {
    return createTransportError(path, technicalMessage(error));
  }

  if (isApiError(body)) {
    return copyApiError(body);
  }

  if (isRecord(body)) {
    const code = body['code'];
    if (typeof code === 'string' && code.length > 0) {
      return createApiErrorFromPartialBody(body, code, error.status, path);
    }
  }

  return createUnparseableError(error.status, path, diagnosticMessage(error, body));
}

function parseErrorBody(raw: unknown): unknown {
  if (typeof raw !== 'string') {
    return raw;
  }

  const trimmed = raw.trim();
  if (trimmed.length === 0) {
    return raw;
  }

  try {
    return JSON.parse(trimmed);
  } catch {
    return raw;
  }
}

function isTransportFailure(error: HttpErrorResponse): boolean {
  return error.status === 0 || error.error instanceof ProgressEvent;
}

function copyApiError(error: ApiError): ApiError {
  const copy: ApiError = {
    timestamp: error.timestamp,
    status: error.status,
    code: error.code,
    message: error.message,
    path: error.path,
  };

  if (error.fields !== undefined) {
    copy.fields = { ...error.fields };
  }

  return copy;
}

function createApiErrorFromPartialBody(
  body: Record<string, unknown>,
  code: string,
  httpStatus: number,
  fallbackPath: string,
): ApiError {
  const fields = parseFields(body['fields']);
  const status = typeof body['status'] === 'number' ? body['status'] : httpStatus;
  const path =
    typeof body['path'] === 'string' && body['path'].length > 0 ? body['path'] : fallbackPath;
  const timestamp =
    typeof body['timestamp'] === 'string' && body['timestamp'].length > 0
      ? body['timestamp']
      : new Date().toISOString();
  const message =
    typeof body['message'] === 'string' && body['message'].length > 0
      ? body['message']
      : `HTTP ${status}`;

  const apiError: ApiError = {
    timestamp,
    status,
    code,
    message,
    path,
  };

  if (fields !== undefined) {
    apiError.fields = fields;
  }

  return apiError;
}

function parseFields(value: unknown): Record<string, string> | undefined {
  if (!isRecord(value)) {
    return undefined;
  }

  const fields: Record<string, string> = {};
  for (const [key, fieldValue] of Object.entries(value)) {
    if (typeof fieldValue === 'string') {
      fields[key] = fieldValue;
    }
  }

  if (Object.keys(value).length === 0) {
    return {};
  }

  return Object.keys(fields).length > 0 ? fields : undefined;
}

function createTransportError(path: string, message: string): ApiError {
  return {
    timestamp: new Date().toISOString(),
    status: 0,
    code: HttpClientErrorCode.Transport,
    message,
    path,
  };
}

function createUnparseableError(status: number, path: string, message: string): ApiError {
  return {
    timestamp: new Date().toISOString(),
    status,
    code: HttpClientErrorCode.UnparseableResponse,
    message,
    path,
  };
}

function diagnosticMessage(error: HttpErrorResponse, body: unknown): string {
  if (typeof body === 'string' && body.trim().length > 0) {
    return truncate(`HTTP ${error.status}: ${body.trim()}`);
  }

  if (error.statusText) {
    return `HTTP ${error.status} ${error.statusText}`;
  }

  return `HTTP ${error.status}: resposta incompatível com o contrato ApiError`;
}

function technicalMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    return error.message || 'Falha de rede ao comunicar com a API.';
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return 'Falha de rede ao comunicar com a API.';
}

function truncate(value: string, maxLength = 300): string {
  if (value.length <= maxLength) {
    return value;
  }

  return `${value.slice(0, maxLength)}…`;
}
