package ru.cloudwithout.bankui.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class UiModelException extends RuntimeException {

    private final List<String> errors;
    private final String info;

    public UiModelException(String errorMessage) {
        super(errorMessage);
        this.errors = List.of(errorMessage);
        this.info = null;
    }

    public UiModelException(List<String> errors, String info) {
        super(String.join("; ", errors));
        this.errors = errors;
        this.info = info;
    }

}