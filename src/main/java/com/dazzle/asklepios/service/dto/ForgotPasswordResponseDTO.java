package com.dazzle.asklepios.service.dto;

/**
 * Answer of {@code POST /api/account/forgot-password}.
 *
 * @param maskedEmail the user's email with most of its local part hidden (e.g. {@code ha****@gmail.com}),
 *                    or {@code null} when no active user with an email matches the given login.
 */
public record ForgotPasswordResponseDTO(String maskedEmail) {}
