package ru.cloudwithout.loadgenerator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.cloudwithout.commonmodels.common.dto.CashAction;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;
import ru.cloudwithout.loadgenerator.client.BankGatewayClient;
import ru.cloudwithout.loadgenerator.config.LoadGeneratorProperties;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class LoadScenarioService {

    private static final List<String> NAME_SUFFIXES = List.of(
            "Иванович", "Петрович", "Сергеевич", "Алексеевич", "Андреевич"
    );

    private final BankGatewayClient bankGatewayClient;
    private final LoadGeneratorProperties properties;

    public Mono<CommonResponse> executeRandomScenario() {
        Scenario scenario = pickScenario();
        String login = pickLogin();
        return switch (scenario) {
            case DEPOSIT -> bankGatewayClient.editCash(login, randomInt(100, 5000), CashAction.DEPOSIT);
            case WITHDRAW_OK -> bankGatewayClient.editCash(login, randomInt(10, 500), CashAction.WITHDRAW);
            case WITHDRAW_FAIL -> bankGatewayClient.editCash(login, randomInt(50_000, 200_000), CashAction.WITHDRAW);
            case TRANSFER_OK -> transfer(login, randomInt(10, 2000));
            case TRANSFER_FAIL -> transfer(login, randomInt(80_000, 150_000));
            case EDIT_PROFILE -> bankGatewayClient.editAccount(
                    login,
                    "Пользователь " + login + " " + pickNameSuffix(),
                    randomBirthdate()
            );
            case GET_ACCOUNT -> bankGatewayClient.getAccount(login);
        };
    }

    private Mono<CommonResponse> transfer(String from, int value) {
        String to = pickOtherLogin(from);
        return bankGatewayClient.transfer(from, value, to);
    }

    private Scenario pickScenario() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 40) {
            return Scenario.DEPOSIT;
        }
        if (roll < 65) {
            return Scenario.WITHDRAW_OK;
        }
        if (roll < 80) {
            return Scenario.WITHDRAW_FAIL;
        }
        if (roll < 90) {
            return Scenario.TRANSFER_OK;
        }
        if (roll < 95) {
            return Scenario.TRANSFER_FAIL;
        }
        if (roll < 98) {
            return Scenario.EDIT_PROFILE;
        }
        return Scenario.GET_ACCOUNT;
    }

    private String pickLogin() {
        List<String> logins = properties.getLogins();
        return logins.get(ThreadLocalRandom.current().nextInt(logins.size()));
    }

    private String pickOtherLogin(String from) {
        List<String> logins = properties.getLogins();
        String to = from;
        while (to.equals(from)) {
            to = logins.get(ThreadLocalRandom.current().nextInt(logins.size()));
        }
        return to;
    }

    private String pickNameSuffix() {
        return NAME_SUFFIXES.get(ThreadLocalRandom.current().nextInt(NAME_SUFFIXES.size()));
    }

    private LocalDate randomBirthdate() {
        int year = ThreadLocalRandom.current().nextInt(1870, 2001);
        int month = ThreadLocalRandom.current().nextInt(1, 13);
        int day = ThreadLocalRandom.current().nextInt(1, 28);
        return LocalDate.of(year, month, day);
    }

    private int randomInt(int minInclusive, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    private enum Scenario {
        DEPOSIT,
        WITHDRAW_OK,
        WITHDRAW_FAIL,
        TRANSFER_OK,
        TRANSFER_FAIL,
        EDIT_PROFILE,
        GET_ACCOUNT
    }
}