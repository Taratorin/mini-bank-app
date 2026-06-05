# Mini Bank App

Микросервисное приложение «Банк»: профиль клиента, пополнение и снятие, переводы между счетами.

## Микросервисы

– `bank-ui` – веб-интерфейс (Thymeleaf)  
– `bank-gateway` – API Gateway (Spring Cloud Gateway)  
– `accounts-service` – аккаунты и баланс (PostgreSQL)  
– `cash-service` – пополнение и снятие денежных средств  
– `transfer-service` – переводы денежных средств между пользователями  
– `notifications-service` – потребитель Kafka, уведомления пишутся в лог  
– `load-generator` – утилита для имитации нагрузки (запускается локально, 
в Kubernetes не деплоится)

Keycloak для аутентификации и авторизации – 
в `auxiliary/docker-compose.yml` (снаружи кластера).  
PostgreSQL и Kafka – в Kubernetes, Helm-сабчарты `postgres` и `kafka`.  
Схема и тестовые данные БД создаются при первом запуске Postgres
(скрипты в сабчарте `postgres`, каталог `/docker-entrypoint-initdb.d/`).  
Уведомления: `accounts-service`, `cash-service`, `transfer-service` 
публикуют в топик `notifications`, `notifications-service` читает из Kafka.

Observability (на хосте, вне кластера): подпроекты `zipkin/`, `prometheus/`, 
`grafana/`, `elk/` -  
трейсы (Micrometer в Zipkin), метрики (`/actuator/prometheus`), 
логи (Log4j2 через Logstash в Elasticsearch).  
Из подов адреса задаются в `helm/bank-app/values.yaml` 
(`observability.*`, как для Keycloak - `172.18.0.1`).

## Требования

– Java 21  
– Docker  
– `kubectl` и доступ к Kubernetes-кластеру  
– Helm 4  
– плагин helm-unittest для тестов чартов  

Кластер Kubernetes может быть любым: Kind, Minikube, Rancher Desktop, облако или корпоративный кластер.  
В репозитории приведён пример для **Kind**, наличие kind не является обязательным условием развёртывания.

## Сборка и тесты

Из корня проекта:

```bash
./gradlew clean build -x test
```

Только unit-тесты:

```bash
./gradlew test
```

## Развёртывание в Kubernetes

Схема: микросервисы и UI запускаются в кластере Kubernetes, Keycloak – на хосте в Docker.

### 1. Keycloak и Observability

```bash
docker compose -f auxiliary/docker-compose.yml up -d keycloak
docker compose -f zipkin/docker-compose.yml up -d
docker compose -f prometheus/docker-compose.yml up -d
docker compose -f grafana/docker-compose.yml up -d
docker compose -f elk/docker-compose.yml up -d
```

Keycloak: `http://localhost:8080` (админ `admin` / `admin`).  
Zipkin: `http://localhost:9411` · Prometheus: `http://localhost:9090` 
· Grafana: `http://localhost:3000` (admin/admin) ·  
Kibana: `http://localhost:5602` (ES `http://localhost:9201`, Logstash TCP `:5000`).  
В Kibana: Data View `bank-logs-*`, поле `@timestamp`.

Из подов кластера Keycloak должен быть доступен по адресу
из `helm/bank-app/values.yaml` (`keycloak.issuerUri`).  
В примере это `http://172.18.0.1:8080/realms/bank-realm`.  
В рабочей среде необходимо указать URL, по которому поды достучатся до Keycloak.

### 2. Кластер и доступ к UI

**Локально (Kind)** – в проекте есть `auxiliary/kind-config.yaml` с пробросом NodePort UI на хост:

```bash
kind create cluster --name mini-bank --config auxiliary/kind-config.yaml
kubectl config use-context kind-mini-bank
```

UI доступен по адресу: `http://localhost:30277`.

**Рабочий или облачный кластер** – создайте кластер обычным для вашей среды способом.  
Откройте UI через NodePort сервиса `bank-app-bank-ui`.  
При необходимости поправьте `bank-ui.service.nodePort` в `helm/bank-app/values.yaml`.

### 3. Образы сервисов

Собрать JAR и образы:

```bash
./gradlew :bank-ui:bootJar :bank-gateway:bootJar :accounts-service:bootJar \
  :cash-service:bootJar :transfer-service:bootJar :notifications-service:bootJar

docker build -t bank-ui:latest ./bank-ui
docker build -t bank-gateway:latest ./bank-gateway
docker build -t accounts-service:latest ./accounts-service
docker build -t cash-service:latest ./cash-service
docker build -t transfer-service:latest ./transfer-service
docker build -t notifications-service:latest ./notifications-service
```

**Kind** – загрузить образы в кластер:

```bash
kind load docker-image bank-ui:latest bank-gateway:latest accounts-service:latest \
  cash-service:latest transfer-service:latest notifications-service:latest \
  --name mini-bank
```

**Другой кластер** – опубликовать образы в registry, который видит кластер, и в `helm/bank-app/values.yaml` указать `image.repository`, `image.tag`, `image.pullPolicy: IfNotPresent` (или `Always`).

### 4. Helm

```bash
helm upgrade --install bank-app ./helm/bank-app
kubectl get pods
```

Дождаться статуса `Running` у всех подов.

Метрики для Prometheus (после деплоя, отдельный терминал, `--address 0.0.0.0` 
- чтобы Prometheus в Docker видел хост):

```bash
kubectl port-forward --address 0.0.0.0 svc/bank-app-bank-gateway 8081:8081 &
kubectl port-forward --address 0.0.0.0 svc/bank-app-accounts-service 8083:8083 &
kubectl port-forward --address 0.0.0.0 svc/bank-app-bank-ui 8084:8084 &
kubectl port-forward --address 0.0.0.0 svc/bank-app-cash-service 8085:8085 &
kubectl port-forward --address 0.0.0.0 svc/bank-app-transfer-service 8086:8086 &
```

### 5. UI

Открыть UI (для Kind – `http://localhost:30277`), войти через Keycloak.

Тестовый пользователь: test/test.

Для простоты развёртывания приложения в корневой директории подготовлен скрипт
`deploy.sh`, выполняющий все необходимые действия.

## Тесты Helm-чартов

Проверка шаблонов (без кластера):

```bash
helm plugin install https://github.com/helm-unittest/helm-unittest --verify=false
helm unittest ./helm/bank-app
```

Проверка доступности сервисов в кластере (после `helm upgrade --install`):

```bash
helm test bank-app
```

PostgreSQL и Kafka поднимаются чартом (`bank-app-postgres`, `bank-app-kafka`).  
Инициализация БД - в Postgres-чарте;
`accounts-service` не выполняет `spring.sql.init` при старте.

Запуск сервисов – каждый в отдельном терминале:

```bash
./gradlew :notifications-service:bootRun
./gradlew :accounts-service:bootRun
./gradlew :cash-service:bootRun
./gradlew :transfer-service:bootRun
./gradlew :bank-gateway:bootRun
./gradlew :bank-ui:bootRun
```

Порты: UI `8084`, Gateway `8081`, Accounts `8083`, Cash `8085`, Transfer `8086`, Notifications `8087`.

## Helm-чарты

Зонтичный чарт: `helm/bank-app/`.  
Сабчарты: `postgres`, `kafka`, `bank-ui`, `bank-gateway`, `accounts-service`, `cash-service`, `transfer-service`, `notifications-service`.

Настройки – в `helm/bank-app/values.yaml` 
(образы, URL Keycloak, JDBC, маршруты Gateway, блок `observability`).

Конфигурация сервисов – ConfigMap и Secret, DNS-имена – Service Kubernetes 
(`bank-app-<имя-сервиса>`).

Кастомные метрики: `bank_cash_withdraw_failed_total` (login), 
`bank_transfer_failed_total` (from, to),  
`bank_notification_send_failed_total` (login). Алерты - `prometheus/alerts.yml`.

## Генератор нагрузки (`load-generator`)

Модуль для демонстрации observability: генерирует HTTP-трафик в `bank-gateway` 
через Client Credentials (токены сервисов Keycloak, без пользовательских сессий). 
Запускается на хосте из IDE или терминала.

**Предусловия:** подняты Keycloak, кластер с `bank-app`, работают port-forward на gateway (`8081`).

```bash
./gradlew :load-generator:bootRun
```

Каждые 10 секунд в лог пишется статистика:
`dispatched`, `completed`, `failed`, `businessErrors`.

Настройки - `load-generator/src/main/resources/application.yaml` (`bank.load.*`):

– `gateway-base-url` - адрес gateway (по умолчанию `http://localhost:8081`)  
– `requests-per-second` - целевой RPS  
– `concurrency` - параллелизм исходящих запросов  
– `logins` - пул логинов для операций

Тестовые пользователи в БД: 
`serg`, `alex`, `test`, `user01`–`user10` 
(скрипт `postgres/files/init/02-data.sql`).

Генератор нагрузки реализует сценарии:
- Пополнение
- Снятие (успех/ошибка)
- Перевод (успех/ошибка)
- Редактирование профиля
- Чтение аккаунта