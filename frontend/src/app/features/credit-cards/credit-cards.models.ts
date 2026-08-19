export interface CreditCard {
  readonly id: string;
  readonly name: string;
  readonly holderName: string;
  readonly lastFourDigits: string | null;
  readonly creditLimit: number;
  readonly closingDay: number;
  readonly dueDay: number;
  readonly active: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreditCardLimit {
  readonly creditLimit: number;
  readonly usedLimit: number;
  readonly availableLimit: number;
}

export interface CreditCardWithLimit {
  readonly card: CreditCard;
  readonly limit: CreditCardLimit;
}

export interface CreditCardListParams {
  readonly holderName?: string;
}

export interface CreateCreditCardRequest {
  readonly name: string;
  readonly holderName: string;
  readonly lastFourDigits?: string;
  readonly creditLimit: number;
  readonly closingDay: number;
  readonly dueDay: number;
}

export interface UpdateCreditCardRequest {
  readonly name: string;
  readonly holderName: string;
  readonly lastFourDigits?: string;
  readonly creditLimit: number;
  readonly closingDay: number;
  readonly dueDay: number;
}

export type CreditCardCreditOrigin = 'MANUAL' | 'CARD_PURCHASE_REFUND';

export interface CreditCardCredit {
  readonly id: string;
  readonly creditCardId: string;
  readonly amount: number;
  readonly remainingAmount: number;
  readonly reason: string;
  readonly origin: CreditCardCreditOrigin;
  readonly expenseId: string | null;
  readonly createdAt: string;
}

export interface CreateCreditCardCreditRequest {
  readonly amount: number;
  readonly reason: string;
}
