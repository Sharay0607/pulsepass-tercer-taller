package com.pulsepass.security;

/**
 * Se lanza cuando un JWT no cumple alguna política (firma, issuer, exp, sub, alg...).
 * Es una excepción propia para NO filtrar detalles de JJWT hacia el resto del sistema.
 */
public class InvalidJwtException extends RuntimeException {

    public InvalidJwtException(String message) {
        super(message);
    }

    public InvalidJwtException(String message, Throwable cause) {
        super(message, cause);
    }
}
