package br.com.financialcontrol.projections.dto;

import java.util.List;

public record ProjectionEventPageResponse(
    List<ProjectionEventResponse> items, int page, int size, long totalItems, int totalPages) {}
