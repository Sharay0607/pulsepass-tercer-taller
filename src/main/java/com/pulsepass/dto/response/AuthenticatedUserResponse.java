package com.pulsepass.dto.response;

import java.util.Set;

/** Información MÍNIMA del principal: sin correo, sin hash, sin secretos. */
public record AuthenticatedUserResponse(Long userId, String username, Set<String> roles) {
}
