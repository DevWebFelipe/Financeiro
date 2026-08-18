package br.com.financialcontrol.projections;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectionEventInput(
    UUID sourceId,
    ProjectionEventType type,
    String description,
    BigDecimal amount,
    ProjectionDirection direction,
    LocalDate date,
    boolean overdue) {}
