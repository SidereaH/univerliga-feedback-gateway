# univerliga-gateway

BFF API Gateway на Java 21 + Spring Boot 3.3 (MVC), с моками CRM/Feedback/Reporting и реальной JWT-авторизацией через Keycloak.

## Запуск

```bash
docker compose up --build
```

Сервисы:
- Gateway: `http://localhost:8080`
- Keycloak: `http://localhost:8081`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## Получение токена через gateway

Gateway проксирует авторизацию в Keycloak. Клиент в realm: `univerliga-gateway`, secret: `gateway-secret`.

```bash
curl -s -X POST 'http://localhost:8080/api/v1/auth/token' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "employee",
    "password": "employee"
  }'
```

Из ответа возьмите `data.accessToken`.

## Примеры вызовов

```bash
TOKEN=<access_token>

curl -s 'http://localhost:8080/api/v1/me' \
  -H "Authorization: Bearer $TOKEN"

curl -s 'http://localhost:8080/api/v1/crm/tasks?page=1&size=5' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Request-Id: demo-123'

curl -s 'http://localhost:8080/api/v1/feedback/categories' \
  -H "Authorization: Bearer $TOKEN"

curl -s -X POST 'http://localhost:8080/api/v1/feedback' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "targetPersonId":"p_4",
    "contextType":"TASK",
    "contextRef":"task_1",
    "rating":5,
    "tagIds":["sub_comm_good","sub_help_explain"],
    "comment":"Strong contribution"
  }'

curl -s 'http://localhost:8080/api/v1/reports/summary?periodFrom=2026-01-01&periodTo=2026-03-31' \
  -H "Authorization: Bearer $TOKEN"

curl -s 'http://localhost:8080/api/v1/reports/insights/top-tags?periodFrom=2026-01-01&periodTo=2026-03-31&limit=5' \
  -H "Authorization: Bearer $TOKEN"
```

## Роли и доступ

- `ROLE_ADMIN`: полный доступ
- `ROLE_HR`: отчёты + доступ к `/feedback/raw`
- `ROLE_MANAGER`: отчёты + чтение/редактирование задач
- `ROLE_EMPLOYEE`: профиль, свои задачи и feedback (без `/reports/**`, без `/feedback/raw`)

## Архитектура

- `controller/*` — REST API `/api/v1`
- `service/*` — бизнес-правила и role checks
- `client/*` — интерфейсы внешних сервисов
- `client/mock/*` — in-memory заглушки (10 people, 10 tasks, 30 feedback)
- `client/mock/*` — in-memory заглушки с context-based reviews, polarity tags и аналитикой
- `client/real/*` — каркас для реальных HTTP-вызовов
- `security/*` — JWT converter (realm roles -> `ROLE_*`)
- `error/*` — унифицированный формат ошибок
- `util/*` — `X-Request-Id` filter

## Конфигурация

`application.yaml`:
- `gateway.mode=mock|real` (default `mock`)
- `gateway.clients.crm.base-url`
- `gateway.clients.feedback.base-url`
- `gateway.clients.reporting.base-url`
- `gateway.clients.analytics.base-url`
- `spring.security.oauth2.resourceserver.jwt.issuer-uri`
