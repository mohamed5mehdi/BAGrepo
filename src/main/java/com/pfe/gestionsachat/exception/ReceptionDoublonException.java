package com.pfe.gestionsachat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ReceptionDoublonException extends RuntimeException {
    public ReceptionDoublonException(String message) {
        super(message);
    }
}
