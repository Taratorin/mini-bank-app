package ru.cloudwithout.commonmodels.common.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class CommonResponse {
    private String login;
    private String firstLastName;
    private LocalDate birthDate;
    private BigDecimal sum;
    private List<AccountDto> accounts;

    private List<String> errors;
    private String info;

    public CommonResponse(List<AccountDto> accounts) {
        this.accounts = accounts;
    }
}