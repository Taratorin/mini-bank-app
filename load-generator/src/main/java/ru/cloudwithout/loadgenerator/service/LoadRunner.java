package ru.cloudwithout.loadgenerator.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import ru.cloudwithout.loadgenerator.config.LoadGeneratorProperties;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoadRunner {

    private final LoadGeneratorProperties properties;
    private final LoadScenarioService loadScenarioService;

    private final AtomicLong dispatched = new AtomicLong();
    private final AtomicLong completed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong businessErrors = new AtomicLong();

    private Disposable subscription;
    private final CountDownLatch keepAlive = new CountDownLatch(1);

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.isEnabled()) {
            log.info("Load generator отключён (bank.load.enabled=false)");
            return;
        }

        long rps = Math.max(1, Math.round(properties.getRequestsPerSecond()));
        Duration interval = Duration.ofNanos(1_000_000_000L / rps);

        log.info(
                "Запускаем load generator: {} RPS, concurrency={}, gateway={}",
                rps,
                properties.getConcurrency(),
                properties.getGatewayBaseUrl()
        );

        subscription = Flux.interval(interval)
                .onBackpressureDrop(dropped -> log.warn("Пропущено {} запросов из-за backpressure", dropped))
                .flatMap(tick -> {
                    dispatched.incrementAndGet();
                    return loadScenarioService.executeRandomScenario()
                            .doOnSuccess(response -> {
                                completed.incrementAndGet();
                                if (response != null && response.getErrors() != null && !response.getErrors().isEmpty()) {
                                    businessErrors.incrementAndGet();
                                }
                            })
                            .onErrorResume(error -> {
                                long failures = failed.incrementAndGet();
                                if (failures <= 3 || failures % 500 == 0) {
                                    log.warn("Request failed (#{}): {}", failures, error.toString());
                                }
                                return reactor.core.publisher.Mono.empty();
                            });
                }, properties.getConcurrency())
                .subscribe();

        try {
            keepAlive.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.info("Load generator остановлен");
        }
    }

    @Scheduled(fixedRate = 10_000)
    void logStats() {
        if (!properties.isEnabled()) {
            return;
        }
        log.info(
                "Load generator stats: dispatched={}, completed={}, failed={}, businessErrors={}",
                dispatched.get(),
                completed.get(),
                failed.get(),
                businessErrors.get()
        );
    }

    @PreDestroy
    void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
        keepAlive.countDown();
    }
}