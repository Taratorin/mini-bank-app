package ru.cloudwithout.accountsservice.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CommonResponse {
    private String login;
    private String firstLastName;
    private LocalDate birthDate;
    private BigDecimal sum;
    private final List<AccountDto> accounts;

    private List<String> errors;
    private String info;
}
