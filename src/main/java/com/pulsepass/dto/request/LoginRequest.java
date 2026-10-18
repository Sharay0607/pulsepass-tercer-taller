package com.pulsepass.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** identifier = username O email. */
public record LoginRequest(
        @NotBlank @Size(max = 100) String identifier,
        @NotBlank @Size(max = 72) String password) {

    /** Un record imprime todos sus campos en toString(): aquí ocultamos la contraseña. */
    @Override
    public String toString() {
        return "LoginRequest[identifier=" + identifier + ", password=***]";
    }
}
