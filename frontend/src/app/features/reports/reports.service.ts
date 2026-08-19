import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL, joinApiUrl } from '../../core/config/api-config';
import {
  parseCardReport,
  parseCashFlowReport,
  parseCategoryReport,
  parseExpenseReport,
  parseIncomeReport,
  parseInvoiceReport,
  parseResponsibleReport,
} from './reports-parse';
import {
  CardReportParams,
  CardReportResponse,
  CashFlowReportParams,
  CashFlowResponse,
  CategoryReportParams,
  CategoryReportResponse,
  ExpenseReportParams,
  ExpenseReportResponse,
  IncomeReportParams,
  IncomeReportResponse,
  InvoiceReportParams,
  InvoiceReportResponse,
  ResponsibleReportParams,
  ResponsibleReportResponse,
} from './reports.models';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listExpenses(params: ExpenseReportParams = {}): Observable<ExpenseReportResponse> {
    return this.http
      .get<unknown>(this.url('/reports/expenses'), { params: this.toHttpParams(params) })
      .pipe(map((body) => this.require(parseExpenseReport(body), 'Expense report')));
  }

  listIncomes(params: IncomeReportParams): Observable<IncomeReportResponse> {
    return this.http
      .get<unknown>(this.url('/reports/incomes'), { params: this.toHttpParams(params) })
      .pipe(map((body) => this.require(parseIncomeReport(body), 'Income report')));
  }

  listCategories(params: CategoryReportParams): Observable<CategoryReportResponse> {
    return this.http
      .get<unknown>(this.url('/reports/categories'), { params: this.toHttpParams(params) })
      .pipe(map((body) => this.require(parseCategoryReport(body), 'Category report')));
  }

  listResponsibles(params: ResponsibleReportParams = {}): Observable<ResponsibleReportResponse> {
    return this.http
      .get<unknown>(this.url('/reports/responsibles'), { params: this.toHttpParams(params) })
      .pipe(map((body) => this.require(parseResponsibleReport(body), 'Responsible report')));
  }

  listCards(params: CardReportParams = {}): Observable<CardReportResponse> {
    return this.http
      .get<unknown>(this.url('/reports/cards'), { params: this.toHttpParams(params) })
      .pipe(map((body) => this.require(parseCardReport(body), 'Card report')));
  }

  listCashFlow(params: CashFlowReportParams = {}): Observable<CashFlowResponse> {
    return this.http
      .get<unknown>(this.url('/reports/cash-flow'), { params: this.toHttpParams(params) })
      .pipe(map((body) => this.require(parseCashFlowReport(body), 'Cash-flow report')));
  }

  getInvoice(
    invoiceId: string,
    params: InvoiceReportParams = {},
  ): Observable<InvoiceReportResponse> {
    return this.http
      .get<unknown>(this.url(`/reports/invoices/${encodeURIComponent(invoiceId)}`), {
        params: this.toHttpParams(params),
      })
      .pipe(map((body) => this.require(parseInvoiceReport(body), 'Invoice report')));
  }

  downloadExpensesPdf(params: ExpenseReportParams = {}): Observable<void> {
    return this.downloadPdf('/reports/expenses/pdf', this.withoutPaging(params));
  }

  downloadIncomesPdf(params: IncomeReportParams): Observable<void> {
    return this.downloadPdf('/reports/incomes/pdf', this.withoutPaging(params));
  }

  downloadCategoriesPdf(params: CategoryReportParams): Observable<void> {
    return this.downloadPdf('/reports/categories/pdf', this.withoutPaging(params));
  }

  downloadResponsiblesPdf(params: ResponsibleReportParams = {}): Observable<void> {
    return this.downloadPdf('/reports/responsibles/pdf', this.withoutPaging(params));
  }

  downloadCardsPdf(params: CardReportParams = {}): Observable<void> {
    return this.downloadPdf('/reports/cards/pdf', this.withoutPaging(params));
  }

  downloadCashFlowPdf(params: CashFlowReportParams = {}): Observable<void> {
    return this.downloadPdf('/reports/cash-flow/pdf', this.withoutPaging(params));
  }

  downloadInvoicePdf(invoiceId: string, params: InvoiceReportParams = {}): Observable<void> {
    return this.downloadPdf(`/reports/invoices/${encodeURIComponent(invoiceId)}/pdf`, params);
  }

  private downloadPdf(path: string, params: object): Observable<void> {
    return this.http
      .get(this.url(path), {
        params: this.toHttpParams(params),
        responseType: 'blob',
        observe: 'response',
      })
      .pipe(
        map((response) => {
          this.saveAttachment(response, fallbackFilename(path));
        }),
      );
  }

  private saveAttachment(response: HttpResponse<Blob>, fallback: string): void {
    const body = response.body;
    if (body == null) {
      throw new Error('PDF response did not include a file.');
    }
    const filename = parseContentDispositionFilename(
      response.headers.get('Content-Disposition'),
      fallback,
    );
    const objectUrl = URL.createObjectURL(body);
    const anchor = document.createElement('a');
    anchor.href = objectUrl;
    anchor.download = filename;
    anchor.rel = 'noopener';
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(objectUrl);
  }

  private toHttpParams(params: object): HttpParams {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params as Record<string, unknown>)) {
      if (value === undefined || value === null || value === '') {
        continue;
      }
      httpParams = httpParams.set(key, String(value));
    }
    return httpParams;
  }

  private withoutPaging<T extends { page?: number; size?: number }>(
    params: T,
  ): Omit<T, 'page' | 'size'> {
    const { page: _page, size: _size, ...rest } = params;
    return rest;
  }

  private url(path: string): string {
    return joinApiUrl(this.apiBaseUrl, path);
  }

  private require<T>(parsed: T | null, label: string): T {
    if (parsed == null) {
      throw new Error(`${label} response did not match the expected contract.`);
    }
    return parsed;
  }
}

export function parseContentDispositionFilename(header: string | null, fallback: string): string {
  if (header == null || header.length === 0) {
    return fallback;
  }
  const utfMatch = /filename\*=(?:UTF-8''|)([^;]+)/i.exec(header);
  if (utfMatch?.[1] != null) {
    try {
      return decodeURIComponent(utfMatch[1].replace(/"/g, '').trim());
    } catch {
      return utfMatch[1].replace(/"/g, '').trim();
    }
  }
  const match = /filename="([^"]+)"|filename=([^;]+)/i.exec(header);
  const raw = match?.[1] ?? match?.[2];
  return raw != null && raw.trim().length > 0 ? raw.trim() : fallback;
}

function fallbackFilename(path: string): string {
  if (path.includes('/expenses/')) {
    return 'relatorio-despesas.pdf';
  }
  if (path.includes('/incomes/')) {
    return 'relatorio-receitas.pdf';
  }
  if (path.includes('/categories/')) {
    return 'relatorio-categorias.pdf';
  }
  if (path.includes('/responsibles/')) {
    return 'relatorio-responsaveis.pdf';
  }
  if (path.includes('/cards/')) {
    return 'relatorio-cartoes.pdf';
  }
  if (path.includes('/cash-flow/')) {
    return 'relatorio-fluxo-caixa.pdf';
  }
  return 'relatorio-fatura.pdf';
}
