package br.com.financialcontrol.transfers.dto;

import br.com.financialcontrol.transfers.Transfer;
import br.com.financialcontrol.transfers.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransferResponse(
    UUID id,
    UUID sourceAccountId,
    UUID destinationAccountId,
    BigDecimal amount,
    LocalDate transferDate,
    String description,
    TransferStatus status,
    Instant createdAt) {

  public static TransferResponse from(Transfer transfer) {
    return new TransferResponse(
        transfer.getId(),
        transfer.getSourceAccount().getId(),
        transfer.getDestinationAccount().getId(),
        transfer.getAmount(),
        transfer.getTransferDate(),
        transfer.getDescription(),
        transfer.getStatus(),
        transfer.getCreatedAt());
  }
}
