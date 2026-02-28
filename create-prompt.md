Ты — senior Java/Spring архитектор. Сгенерируй репозиторий "univerliga-gateway" на Java 21 + Spring Boot 3.3+.
Цель: один сервис API Gateway (BFF) с моковыми ответами для ВСЕХ микросервисов (CRM, Feedback, Reporting), но с реальной авторизацией через Keycloak (поднимаем Keycloak в docker-compose).
Нужно продакшн-качество: четкие JSON-контракты, валидация, единый формат ошибок, correlation id, OpenAPI.

СТЕК:
- Java 21
- Spring Boot 3.3+
- Spring Web (MVC)
- Spring Security (OAuth2 Resource Server JWT)
- springdoc-openapi-starter-webmvc-ui
- Lombok
- Validation (jakarta)
- Actuator
- (опционально) spring-cloud-starter-gateway НЕ использовать. Это именно BFF-gateway как обычный REST сервис (Spring MVC), чтобы проще моки и переход на реальные сервисы.
- Gradle Kotlin DSL или Maven (выбери Maven если не уверен)

ДОКЕР:
- docker-compose.yml поднимает:
    1) keycloak (последняя стабильная)
    2) postgres для keycloak
    3) gateway (этот сервис)
- Keycloak должен импортировать realm из realm-export.json автоматически.
- В realm: client "univerliga-gateway" (confidential), audience для gateway, valid redirect (не критично), roles:
    - ROLE_ADMIN
    - ROLE_MANAGER
    - ROLE_EMPLOYEE
- Создай пользователей:
    - admin/admin (ROLE_ADMIN)
    - manager/manager (ROLE_MANAGER)
    - employee/employee (ROLE_EMPLOYEE)
- Gateway должен проверять JWT токены от этого Keycloak.

ОБЩИЕ ТРЕБОВАНИЯ К API:
- Base path: /api/v1
- Все ответы: Content-Type application/json; charset=utf-8
- Все успешные ответы оборачивать в envelope:
  {
  "data": <payload>,
  "meta": { "requestId": "...", "timestamp": "ISO-8601", "version": "v1" }
  }
- Ошибки в формате:
  {
  "error": {
  "code": "STRING_CODE",
  "message": "Human readable",
  "details": [ { "field": "optional", "issue": "optional" } ],
  "requestId": "..."
  }
  }
- Реализуй global exception handler (ControllerAdvice) для:
    - validation errors -> code=VALIDATION_ERROR, details with fields
    - 401 -> UNAUTHORIZED
    - 403 -> FORBIDDEN
    - 404 -> NOT_FOUND
    - 500 -> INTERNAL_ERROR
- Correlation/request id:
    - принимай заголовок X-Request-Id, если нет — генерируй UUID
    - возвращай X-Request-Id в ответе + кладёшь в meta.requestId и error.requestId
- Добавь Actuator health: /actuator/health (без авторизации)
- Добавь Swagger UI: /swagger-ui.html
- OpenAPI должен документировать ВСЕ эндпоинты и схемы.
- Валидация входных DTO: @NotNull, @Size, @Min и т.п.

SECURITY:
- /actuator/** и /swagger-ui/** и /v3/api-docs/** доступны без токена
- Все /api/v1/** требуют JWT
- Role-based:
    - ADMIN: доступ ко всем эндпоинтам
    - MANAGER: доступ к отчетам + чтение задач/людей + чтение агрегатов
    - EMPLOYEE: доступ к своим задачам и созданию feedback; не должен иметь доступ к raw feedback других пользователей и административным CRUD
- Используй authorities mapping: realm roles -> Spring authorities вида ROLE_ADMIN etc.

МОКИ:
- Пока все эндпоинты возвращают моковые данные (in-memory stubs), но код должен быть структурирован так, чтобы потом заменить моки на реальные HTTP-вызовы микросервисов:
    - сделай слой "clients" (interfaces) для CRM/Feedback/Reporting
    - сделай мок-реализации этих clients
    - сделай service layer, который использует clients
    - добавь конфиг-флаг application.yml: gateway.mode = mock|real (по умолчанию mock)
    - для режима real подготовь RestClient/WebClient бины и base urls:
      crm.baseUrl, feedback.baseUrl, reporting.baseUrl
      но реальный вызов можно пока не реализовывать полностью (достаточно каркаса + TODO), главное чтобы архитектурно было готово.

ENDPOINTS (ВСЕ должны быть реализованы и задокументированы; ответы — стабильные продакшн JSON по схемам):

1) AUTH/ME
   GET /api/v1/me
   Ответ data:
   {
   "personId": "p_123",
   "username": "employee",
   "roles": ["ROLE_EMPLOYEE"],
   "departmentId": "d_10",
   "teamId": "t_5",
   "displayName": "Employee User"
   }

2) CRM: People (ADMIN full, MANAGER read, EMPLOYEE self-read)
   GET /api/v1/crm/people?query=&departmentId=&teamId=&page=1&size=20
   Ответ data:
   {
   "items": [ { "id":"p_1","displayName":"...","email":"...","departmentId":"d_1","teamId":"t_1","active":true,"createdAt":"..."} ],
   "page": { "page":1,"size":20,"totalItems":100,"totalPages":5 }
   }

POST /api/v1/crm/people  (ADMIN only)
body:
{ "displayName":"...", "email":"...", "departmentId":"d_1", "teamId":"t_1", "role":"EMPLOYEE|MANAGER|ADMIN" }
Ответ data:
{ "id":"p_999","displayName":"...","email":"...","departmentId":"d_1","teamId":"t_1","active":true,"identityStatus":"PENDING|PROVISIONED","createdAt":"..." }

GET /api/v1/crm/people/{personId} (ADMIN/MANAGER any, EMPLOYEE only self)
Ответ data: person object + "identityStatus" + "keycloakUserId" (только ADMIN)
PATCH /api/v1/crm/people/{personId} (ADMIN only) body: partial updates
DELETE /api/v1/crm/people/{personId} (ADMIN only) -> data: { "deleted": true }

3) CRM: Tasks/Episodes
   GET /api/v1/crm/tasks?status=&assigneeId=&participantId=&periodFrom=&periodTo=&page=&size=
   Ответ data:
   {
   "items":[
   {
   "id":"task_1",
   "title":"Quarter review",
   "description":"...",
   "status":"DRAFT|ACTIVE|CLOSED",
   "period": { "from":"2026-01-01","to":"2026-01-31" },
   "ownerId":"p_10",
   "assigneeId":"p_11",
   "participantIds":["p_11","p_12"],
   "createdAt":"..."
   }
   ],
   "page": { ... }
   }

POST /api/v1/crm/tasks (MANAGER/ADMIN)
body:
{
"title":"...",
"description":"...",
"period": { "from":"YYYY-MM-DD","to":"YYYY-MM-DD" },
"ownerId":"p_10",
"assigneeId":"p_11",
"participantIds":["p_11","p_12"]
}
Ответ data: task object

GET /api/v1/crm/tasks/{taskId} (EMPLOYEE only if participant, MANAGER/ADMIN any)
PATCH /api/v1/crm/tasks/{taskId} (MANAGER/ADMIN)
POST /api/v1/crm/tasks/{taskId}/close (MANAGER/ADMIN) -> data: { "status":"CLOSED","closedAt":"..." }

4) Feedback/Survey
   GET /api/v1/feedback/categories
   Ответ data:
   {
   "items":[
   {
   "id":"cat_1",
   "name":"Performance",
   "subcategories":[ {"id":"sub_1","name":"Communication"},{"id":"sub_2","name":"Delivery"} ]
   }
   ]
   }

POST /api/v1/feedback
(EMPLOYEE/MANAGER/ADMIN) создать отзыв (для EMPLOYEE только если он участник task)
body:
{
"taskId":"task_1",
"targetPersonId":"p_11",
"categoryId":"cat_1",
"subcategoryId":"sub_1",
"rating": 1..5,
"comment": "string, optional, max 2000"
}
Ответ data:
{
"id":"fb_1",
"taskId":"task_1",
"targetPersonId":"p_11",
"categoryId":"cat_1",
"subcategoryId":"sub_1",
"rating":5,
"comment":"...",
"createdAt":"...",
"visibility": { "authorHidden": true }
}

GET /api/v1/feedback/my?taskId=&page=&size=
Ответ: список feedback, которые оставил текущий пользователь (author perspective). data: items + page.

GET /api/v1/feedback/inbox?taskId=&page=&size=
Ответ: feedback, относящиеся к текущему пользователю как target (без author).
EMPLOYEE только свои. MANAGER может по своей команде. ADMIN все.
data items: без authorId.

(ADMIN only) GET /api/v1/feedback/raw?taskId=&targetPersonId=&authorPersonId=&page=&size=
Ответ data items: включает authorPersonId.

5) Reporting
   GET /api/v1/reports/summary?periodFrom=&periodTo=&departmentId=&teamId=
   Доступ: MANAGER/ADMIN (EMPLOYEE запрещено)
   Ответ data:
   {
   "period": { "from":"YYYY-MM-DD","to":"YYYY-MM-DD" },
   "kpis": {
   "responses": 120,
   "avgRating": 4.2,
   "positiveShare": 0.76
   }
   }

GET /api/v1/reports/charts/ratings-by-category?periodFrom=&periodTo=&teamId=
Ответ data:
{
"series":[
{ "categoryId":"cat_1","categoryName":"Performance","avgRating":4.1,"count":55 },
{ "categoryId":"cat_2","categoryName":"Culture","avgRating":4.4,"count":65 }
]
}

GET /api/v1/reports/charts/trend?metric=avgRating|responses&period=month&from=YYYY-MM-DD&to=YYYY-MM-DD&teamId=
Ответ data:
{
"metric":"avgRating",
"points":[ { "x":"2026-01","y":4.1 }, { "x":"2026-02","y":4.3 } ]
}

6) SYSTEM
   GET /api/v1/system/version -> data: { "name":"univerliga-gateway","version":"0.1.0","mode":"mock" }

ПРАВИЛА ДОСТУПА (реализовать минимум на уровне проверок в контроллерах/сервисах):
- Определи текущего пользователя из JWT: username, roles, personId (personId можно мокать: admin->p_admin, manager->p_manager, employee->p_employee)
- EMPLOYEE:
    - может GET /me, /system/version
    - может читать людей только себя: GET /crm/people/{self}
    - может читать задачи только где он participant
    - может POST feedback только если он participant в task
    - может GET /feedback/my и /feedback/inbox (только свои)
    - не может /reports/*
    - не может /feedback/raw
- MANAGER:
    - чтение людей/задач шире (все или по teamId — упростить можно на моках)
    - может создавать/редактировать tasks
    - может смотреть reports
- ADMIN:
    - всё

КОД-СТРУКТУРА:
- package: com.univerliga.gateway
- controller/*
- dto/*
- service/*
- client/* (interfaces + mock implementations)
- security/* (jwt converter, roles)
- config/*
- error/* (api error model, exception handler)
- util/* (request id filter)
- OpenAPI annotations на DTO и контроллерах.

ДАННЫЕ МОКОВ:
- Сделай in-memory списки:
    - 10 people
    - 10 tasks с разными participantIds
    - 30 feedback
    - categories fixed
- Пагинация должна реально работать (page/size).

ФИНАЛЬНО:
- Репозиторий должен запускаться командой:
    - docker compose up --build
- Дай README.md:
    - как поднять
    - как получить токен (curl to keycloak token endpoint)
    - примеры curl запросов к gateway с Authorization: Bearer
    - ссылки на swagger/actuator
- Проверь что все endpoints отвечают и что security реально работает.

Сгенерируй полный код + все файлы конфигурации.