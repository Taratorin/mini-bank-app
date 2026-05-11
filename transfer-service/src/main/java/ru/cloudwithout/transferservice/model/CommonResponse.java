package ru.cloudwithout.transferservice.model;

import lombok.Data;
import ru.cloudwithout.transferservice.model.dto.AccountDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CommonResponse {
    private String login;
    private String firstLastName;
    private LocalDate birthDate;
    private BigDecimal sum;
    private final List<AccountDto> accounts;

    private List<String> errors = List.of();
    private String info;
}