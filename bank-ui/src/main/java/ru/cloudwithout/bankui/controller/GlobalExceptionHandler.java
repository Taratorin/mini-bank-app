package ru.cloudwithout.bankui.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.cloudwithout.bankui.exception.UiModelException;
import ru.cloudwithout.bankui.model.MainPageModelFiller;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MainPageModelFiller mainPageModelFiller;

    @ExceptionHandler(UiModelException.class)
    public String handleUiModelException(UiModelException exception, Model model) {
        log.warn("Бизнес-ошибка UI: {}", exception.getMessage());
        mainPageModelFiller.fillModel(model, null, exception.getErrors(), exception.getInfo());
        return "main";
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpectedException(Exception exception, Model model) {
        log.error("Необработанная ошибка UI", exception);
        mainPageModelFiller.fillModel(
                model,
                null,
                List.of("Внутренняя ошибка приложения"),
                null
        );
        return "main";
    }
}