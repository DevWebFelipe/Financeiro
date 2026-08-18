package br.com.financialcontrol.projections.dto;

import br.com.financialcontrol.projections.ProjectionAccountAssignment;
import br.com.financialcontrol.projections.ProjectionDirection;
import br.com.financialcontrol.projections.ProjectionEventType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectionEventResponse(
    LocalDate date,
    ProjectionEventType type,
    String description,
    BigDecimal amount,
    ProjectionDirection direction,
    UUID sourceId,
    ProjectionEventType sourceType,
    boolean overdue,
    ProjectionAccountAssignment accountAssignment) {}
