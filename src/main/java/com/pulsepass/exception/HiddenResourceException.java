package com.pulsepass.exception;

/**
 * El recurso existe pero NO debe revelarse a quien pregunta (p. ej. evento en borrador para un anónimo).
 * Se responde 404, idéntico al de un recurso inexistente, para no filtrar su existencia.
 */
public class HiddenResourceException extends RuntimeException {

    public HiddenResourceException(String message) {
        super(message);
    }
}
