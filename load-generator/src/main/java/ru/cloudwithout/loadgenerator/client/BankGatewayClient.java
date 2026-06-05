package ru.cloudwithout.loadgenerator.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.cloudwithout.commonmodels.common.dto.CashAction;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;

import java.time.LocalDate;

@Component
public class BankGatewayClient {

    private final WebClient accountsWebClient;
    private final WebClient cashWebClient;
    private final WebClient transferWebClient;

    public BankGatewayClient(
            @Qualifier("accountsGatewayWebClient") WebClient accountsWebClient,
            @Qualifier("cashGatewayWebClient") WebClient cashWebClient,
            @Qualifier("transferGatewayWebClient") WebClient transferWebClient) {
        this.accountsWebClient = accountsWebClient;
        this.cashWebClient = cashWebClient;
        this.transferWebClient = transferWebClient;
    }

    public Mono<CommonResponse> getAccount(String login) {
        return accountsWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/accounts/login")
                        .queryParam("login", login)
                        .build())
                .retrieve()
                .bodyToMono(CommonResponse.class);
    }

    public Mono<CommonResponse> editAccount(String login, String name, LocalDate birthdate) {
        return accountsWebClient.post()
                .uri(uriBuilder -> uriBuilder.path("/accounts/edit")
                        .queryParam("login", login)
                        .queryParam("name", name)
                        .queryParam("birthdate", birthdate)
                        .build())
                .retrieve()
                .bodyToMono(CommonResponse.class);
    }

    public Mono<CommonResponse> editCash(String login, int value, CashAction action) {
        return cashWebClient.post()
                .uri(uriBuilder -> uriBuilder.path("/cash")
                        .queryParam("login", login)
                        .queryParam("value", value)
                        .queryParam("action", action)
                        .build())
                .retrieve()
                .bodyToMono(CommonResponse.class);
    }

    public Mono<CommonResponse> transfer(String from, int value, String to) {
        return transferWebClient.post()
                .uri(uriBuilder -> uriBuilder.path("/transfer")
                        .queryParam("from", from)
                        .queryParam("value", value)
                        .queryParam("to", to)
                        .build())
                .retrieve()
                .bodyToMono(CommonResponse.class);
    }
}