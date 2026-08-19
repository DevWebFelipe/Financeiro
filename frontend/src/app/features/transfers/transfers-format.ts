import { Transfer, TransferStatus } from './transfers.models';

export function transferStatusLabel(status: TransferStatus): string {
  switch (status) {
    case 'ACTIVE':
      return 'Ativa';
    case 'REVERSED':
      return 'Estornada';
    default:
      return status;
  }
}

export function canReverseTransfer(transfer: Transfer): boolean {
  return transfer.status === 'ACTIVE';
}
