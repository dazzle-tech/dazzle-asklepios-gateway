package com.dazzle.asklepios.web.rest.vm;

/**
 * Body of {@code POST /api/account/forgot-password}: the user name (or email) typed on the sign-in screen.
 */
public record ForgotPasswordVM(String login) {}
