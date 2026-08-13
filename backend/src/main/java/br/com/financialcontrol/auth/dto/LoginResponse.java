package br.com.financialcontrol.auth.dto;

public record LoginResponse(String accessToken, String tokenType, int expiresIn) {}
