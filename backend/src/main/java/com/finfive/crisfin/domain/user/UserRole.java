package com.finfive.crisfin.domain.user;

/**
 * Application-level roles controlling access to secured endpoints.
 */
public enum UserRole {

    /** Standard authenticated user. */
    USER,

    /** Administrator with elevated privileges. */
    ADMIN
}
