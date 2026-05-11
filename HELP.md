# Mini Bank App

Микросервисное приложение "Банк".

Фронт позволяет:
- смотреть и редактировать профиль клиента банка
- пополнять и снимать деньги с условного счёта клиента
- переводить деньги другому клиенту банка

## Основные микросервисы

- `bank-ui` - веб-интерфейс (Spring MVC + Thymeleaf)
- `bank-gateway` - API Gateway (Spring Cloud Gateway)
- `accounts-service` - аккаунты и баланс (Spring Data JPA + PostgreSQL)
- `cash-service` - пополнение и снятие
- `transfer-service` - переводы между счетами
- `notifications-service` - уведомления

## Технологии

- Java 21
- Spring Boot
- Spring Security + OAuth2
- Spring Cloud (Gateway, Consul Config, Consul Discovery)
- Keycloak
- PostgreSQL
- Gradle (multimodule)
- Docker + Docker Compose

## Быстрый запуск инфраструктуры

Из корня проекта:

- `docker compose -f auxiliary/docker-compose.yml up -d`

Это поднимет:

- PostgreSQL (`localhost:5434`)
- Keycloak (`localhost:8080`)
- Consul (`localhost:8500`)

Важно:
- все вспомогательные сервисы запускаются через `auxiliary/docker-compose.yml`
- основные сервисы запускаются отдельно, каждый из своего `Dockerfile` в модуле

## Настройка Consul KV

После запуска Consul добавьте настройки сервисов в Key/Value:
- откройте `http://localhost:8500`
- создайте ключи и вставьте значения из файлов в `auxiliary/consul`

Ключи:

- `config/accounts-service/data`
- `config/cash-service/data`
- `config/transfer-service/data`
- `config/bank-gateway/data`
- `config/bank-ui/data`
- `config/notifications-service/data`

## Локальный запуск через Gradle

Из корня проекта:

- `./gradlew clean build`

Запуск сервисов (каждый в отдельном терминале):

- `./gradlew :notifications-service:bootRun`
- `./gradlew :accounts-service:bootRun`
- `./gradlew :cash-service:bootRun`
- `./gradlew :transfer-service:bootRun`
- `./gradlew :bank-gateway:bootRun`
- `./gradlew :bank-ui:bootRun`

## Запуск основных сервисов в Docker

Каждый основной сервис собирается и запускается отдельно из своего каталога:

- `accounts-service/Dockerfile`
- `cash-service/Dockerfile`
- `transfer-service/Dockerfile`
- `notifications-service/Dockerfile`
- `bank-gateway/Dockerfile`
- `bank-ui/Dockerfile`

Пример для одного сервиса:

- `docker build -t accounts-service:local ./accounts-service`
- `docker run --rm --network mini-bank-app --name accounts-service accounts-service:local`