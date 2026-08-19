import { isRecord } from '../../core/errors/api-error';
import {
  CreateGoalContributionResult,
  CreateGoalRedemptionResult,
  FinancialGoal,
  FinancialGoalPage,
  FinancialGoalStatus,
  GoalContribution,
  GoalRedemption,
} from './financial-goals.models';

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

export function parseFinancialGoalPage(body: unknown): FinancialGoalPage | null {
  if (!isRecord(body)) {
    return null;
  }

  const items = parseFinancialGoalList(body['items']);
  const page = parseCount(body['page']);
  const size = parseCount(body['size']);
  const totalItems = parseLongCount(body['totalItems']);
  const totalPages = parseCount(body['totalPages']);

  if (items == null || page == null || size == null || totalItems == null || totalPages == null) {
    return null;
  }

  return { items, page, size, totalItems, totalPages };
}

export function parseFinancialGoalList(body: unknown): FinancialGoal[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const items: FinancialGoal[] = [];
  for (const item of body) {
    const parsed = parseFinancialGoal(item);
    if (parsed == null) {
      return null;
    }
    items.push(parsed);
  }
  return items;
}

export function parseFinancialGoal(value: unknown): FinancialGoal | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const accountId = parseId(value['accountId']);
  const name = parseString(value['name']);
  const description = parseNullableString(value['description']);
  const targetAmount = parseMoney(value['targetAmount']);
  const targetDate = parseOptionalIsoDate(value['targetDate']);
  const status = parseStatus(value['status']);
  const currentAmount = parseMoney(value['currentAmount']);
  const progressPercent = parseProgressPercent(value['progressPercent']);
  const createdAt = parseInstant(value['createdAt']);
  const updatedAt = parseInstant(value['updatedAt']);

  if (
    id == null ||
    accountId == null ||
    name == null ||
    targetAmount == null ||
    targetDate === undefined ||
    status == null ||
    currentAmount == null ||
    progressPercent == null ||
    createdAt == null ||
    updatedAt == null
  ) {
    return null;
  }

  return {
    id,
    accountId,
    name,
    description,
    targetAmount,
    targetDate,
    status,
    currentAmount,
    progressPercent,
    createdAt,
    updatedAt,
  };
}

export function parseGoalContributionList(body: unknown): GoalContribution[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const items: GoalContribution[] = [];
  for (const item of body) {
    const parsed = parseGoalContribution(item);
    if (parsed == null) {
      return null;
    }
    items.push(parsed);
  }
  return items;
}

export function parseGoalContribution(value: unknown): GoalContribution | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const goalId = parseId(value['goalId']);
  const amount = parseMoney(value['amount']);
  const contributionDate = parseIsoDate(value['contributionDate']);
  const notes = parseNullableString(value['notes']);
  const createdAt = parseInstant(value['createdAt']);

  if (
    id == null ||
    goalId == null ||
    amount == null ||
    contributionDate == null ||
    createdAt == null
  ) {
    return null;
  }

  return { id, goalId, amount, contributionDate, notes, createdAt };
}

export function parseGoalRedemptionList(body: unknown): GoalRedemption[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const items: GoalRedemption[] = [];
  for (const item of body) {
    const parsed = parseGoalRedemption(item);
    if (parsed == null) {
      return null;
    }
    items.push(parsed);
  }
  return items;
}

export function parseGoalRedemption(value: unknown): GoalRedemption | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = parseId(value['id']);
  const goalId = parseId(value['goalId']);
  const amount = parseMoney(value['amount']);
  const redemptionDate = parseIsoDate(value['redemptionDate']);
  const notes = parseNullableString(value['notes']);
  const createdAt = parseInstant(value['createdAt']);

  if (
    id == null ||
    goalId == null ||
    amount == null ||
    redemptionDate == null ||
    createdAt == null
  ) {
    return null;
  }

  return { id, goalId, amount, redemptionDate, notes, createdAt };
}

export function parseCreateGoalContributionResult(
  body: unknown,
): CreateGoalContributionResult | null {
  if (!isRecord(body)) {
    return null;
  }

  const contribution = parseGoalContribution(body['contribution']);
  const goal = parseFinancialGoal(body['goal']);
  if (contribution == null || goal == null) {
    return null;
  }

  return { contribution, goal };
}

export function parseCreateGoalRedemptionResult(body: unknown): CreateGoalRedemptionResult | null {
  if (!isRecord(body)) {
    return null;
  }

  const redemption = parseGoalRedemption(body['redemption']);
  const goal = parseFinancialGoal(body['goal']);
  if (redemption == null || goal == null) {
    return null;
  }

  return { redemption, goal };
}

function parseId(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseString(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseNullableString(value: unknown): string | null {
  if (value == null) {
    return null;
  }
  return typeof value === 'string' ? value : null;
}

function parseMoney(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function parseProgressPercent(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function parseCount(value: unknown): number | null {
  return typeof value === 'number' && Number.isInteger(value) && value >= 0 ? value : null;
}

function parseLongCount(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : null;
}

function parseIsoDate(value: unknown): string | null {
  return typeof value === 'string' && ISO_DATE.test(value) ? value : null;
}

function parseOptionalIsoDate(value: unknown): string | null | undefined {
  if (value == null) {
    return null;
  }
  return parseIsoDate(value) ?? undefined;
}

function parseInstant(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

function parseStatus(value: unknown): FinancialGoalStatus | null {
  if (value === 'ACTIVE' || value === 'COMPLETED' || value === 'CANCELLED') {
    return value;
  }
  return null;
}
