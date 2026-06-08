package ru.cloudwithout.accountsservice.service;

import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.cloudwithout.accountsservice.model.Account;
import ru.cloudwithout.accountsservice.repository.AccountRepository;
import ru.cloudwithout.accountsservice.support.AccountsIntegrationTest;
import ru.cloudwithout.commonmodels.common.dto.CommonResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMoneyServiceConcurrentTransferTest extends AccountsIntegrationTest {

    @Autowired
    private AccountMoneyService accountMoneyService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUpAccounts() {
        transactionTemplate.executeWithoutResult(status -> {
            accountRepository.deleteAll();
            accountRepository.save(account("serg", new BigDecimal("100.00")));
            accountRepository.save(account("alex", BigDecimal.ZERO));
            accountRepository.save(account("maria", BigDecimal.ZERO));
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void transferShouldProtectBalanceUnderConcurrentWithdrawals()
            throws InterruptedException, ExecutionException, TimeoutException {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<CommonResponse>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threads; i++) {
                String to = i == 0 ? "alex" : "maria";
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return accountMoneyService.transfer("serg", 50, to);
                }));
            }

            ready.await();
            start.countDown();

            List<CommonResponse> responses = new ArrayList<>();
            int optimisticLockFailures = 0;
            for (Future<CommonResponse> future : futures) {
                try {
                    responses.add(future.get(30, TimeUnit.SECONDS));
                } catch (ExecutionException exception) {
                    if (isOptimisticLockFailure(exception.getCause())) {
                        optimisticLockFailures++;
                    } else {
                        throw exception;
                    }
                }
            }

            transactionTemplate.executeWithoutResult(status ->
                    assertThat(accountRepository.findByLogin("serg").orElseThrow().getSum())
                            .isEqualByComparingTo("50.00"));

            long successCount = responses.stream()
                    .filter(response -> "Перевод выполнен успешно".equals(response.getInfo()))
                    .count();
            assertThat(successCount).isEqualTo(1);

            long rejectedCount = responses.stream()
                    .filter(response -> response.getErrors() != null && !response.getErrors().isEmpty())
                    .count();
            assertThat(rejectedCount + optimisticLockFailures).isEqualTo(1);

            responses.stream()
                    .filter(response -> response.getErrors() != null && !response.getErrors().isEmpty())
                    .forEach(response -> assertThat(response.getErrors()).anySatisfy(error -> assertThat(error).isIn(
                            "Не удалось выполнить операцию, попробуйте снова",
                            "Недостаточно средств на счёте"
                    )));
        } finally {
            executor.shutdownNow();
        }
    }

    private static boolean isOptimisticLockFailure(Throwable throwable) {
        return throwable instanceof OptimisticLockException
                || throwable instanceof ObjectOptimisticLockingFailureException
                || throwable instanceof OptimisticLockingFailureException;
    }

    private Account account(String login, BigDecimal sum) {
        Account account = new Account();
        account.setLogin(login);
        account.setFirstLastName(login);
        account.setBirthDate(LocalDate.of(1990, 1, 1));
        account.setSum(sum);
        return account;
    }
}