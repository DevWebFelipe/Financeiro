import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL, joinApiUrl } from '../../core/config/api-config';
import {
  parseInvoice,
  parseInvoiceAdjustment,
  parseInvoiceAdjustmentList,
  parseInvoiceItemList,
  parseInvoiceList,
  parseInvoicePayment,
  parseInvoicePaymentList,
} from './invoices-parse';
import {
  CreateInvoiceAdjustmentRequest,
  Invoice,
  InvoiceAdjustment,
  InvoiceItem,
  InvoiceListParams,
  InvoicePayment,
  PayInvoiceRequest,
} from './invoices.models';

@Injectable({ providedIn: 'root' })
export class InvoicesService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listByCard(cardId: string, params: InvoiceListParams = {}): Observable<Invoice[]> {
    let httpParams = new HttpParams();
    if (params.year != null) {
      httpParams = httpParams.set('year', String(params.year));
    }
    if (params.month != null) {
      httpParams = httpParams.set('month', String(params.month));
    }
    if (params.status != null) {
      httpParams = httpParams.set('status', params.status);
    }

    const path = `/credit-cards/${encodeURIComponent(cardId)}/invoices`;
    return this.http.get<unknown>(joinApiUrl(this.apiBaseUrl, path), { params: httpParams }).pipe(
      map((body) => {
        const parsed = parseInvoiceList(body);
        if (parsed == null) {
          throw new Error('Invoices response did not match the expected contract.');
        }
        return parsed;
      }),
    );
  }

  get(invoiceId: string): Observable<Invoice> {
    return this.http
      .get<unknown>(joinApiUrl(this.apiBaseUrl, `/invoices/${encodeURIComponent(invoiceId)}`))
      .pipe(
        map((body) => {
          const parsed = parseInvoice(body);
          if (parsed == null) {
            throw new Error('Invoice response did not match the expected contract.');
          }
          return parsed;
        }),
      );
  }

  listItems(invoiceId: string): Observable<InvoiceItem[]> {
    const path = `/invoices/${encodeURIComponent(invoiceId)}/items`;
    return this.http.get<unknown>(joinApiUrl(this.apiBaseUrl, path)).pipe(
      map((body) => {
        const parsed = parseInvoiceItemList(body);
        if (parsed == null) {
          throw new Error('Invoice items response did not match the expected contract.');
        }
        return parsed;
      }),
    );
  }

  listPayments(invoiceId: string): Observable<InvoicePayment[]> {
    const path = `/invoices/${encodeURIComponent(invoiceId)}/payments`;
    return this.http.get<unknown>(joinApiUrl(this.apiBaseUrl, path)).pipe(
      map((body) => {
        const parsed = parseInvoicePaymentList(body);
        if (parsed == null) {
          throw new Error('Invoice payments response did not match the expected contract.');
        }
        return parsed;
      }),
    );
  }

  createPayment(invoiceId: string, request: PayInvoiceRequest): Observable<InvoicePayment> {
    const path = `/invoices/${encodeURIComponent(invoiceId)}/payments`;
    return this.http.post<unknown>(joinApiUrl(this.apiBaseUrl, path), request).pipe(
      map((body) => {
        const parsed = parseInvoicePayment(body);
        if (parsed == null) {
          throw new Error('Invoice payment response did not match the expected contract.');
        }
        return parsed;
      }),
    );
  }

  reversePayment(invoiceId: string, paymentId: string): Observable<InvoicePayment> {
    const path = `/invoices/${encodeURIComponent(invoiceId)}/payments/${encodeURIComponent(paymentId)}/reverse`;
    return this.http.post<unknown>(joinApiUrl(this.apiBaseUrl, path), {}).pipe(
      map((body) => {
        const parsed = parseInvoicePayment(body);
        if (parsed == null) {
          throw new Error('Invoice payment reverse response did not match the expected contract.');
        }
        return parsed;
      }),
    );
  }

  listAdjustments(invoiceId: string): Observable<InvoiceAdjustment[]> {
    const path = `/invoices/${encodeURIComponent(invoiceId)}/adjustments`;
    return this.http.get<unknown>(joinApiUrl(this.apiBaseUrl, path)).pipe(
      map((body) => {
        const parsed = parseInvoiceAdjustmentList(body);
        if (parsed == null) {
          throw new Error('Invoice adjustments response did not match the expected contract.');
        }
        return parsed;
      }),
    );
  }

  createAdjustment(
    invoiceId: string,
    request: CreateInvoiceAdjustmentRequest,
  ): Observable<InvoiceAdjustment> {
    const path = `/invoices/${encodeURIComponent(invoiceId)}/adjustments`;
    return this.http.post<unknown>(joinApiUrl(this.apiBaseUrl, path), request).pipe(
      map((body) => {
        const parsed = parseInvoiceAdjustment(body);
        if (parsed == null) {
          throw new Error('Invoice adjustment response did not match the expected contract.');
        }
        return parsed;
      }),
    );
  }

  reverseAdjustment(invoiceId: string, adjustmentId: string): Observable<InvoiceAdjustment> {
    const path = `/invoices/${encodeURIComponent(invoiceId)}/adjustments/${encodeURIComponent(adjustmentId)}/reverse`;
    return this.http.post<unknown>(joinApiUrl(this.apiBaseUrl, path), {}).pipe(
      map((body) => {
        const parsed = parseInvoiceAdjustment(body);
        if (parsed == null) {
          throw new Error(
            'Invoice adjustment reverse response did not match the expected contract.',
          );
        }
        return parsed;
      }),
    );
  }
}
