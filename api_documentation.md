# Finly Backend API Documentation

All business APIs use the `/api` prefix.

## Base Rules
- Base URL (local): `http://localhost:8080`
- API prefix: `/api`
- Protected endpoints require header:
  - `Authorization: Bearer <accessToken>`
- JSON endpoints use:
  - `Content-Type: application/json`

Standard response wrapper:
```json
{
  "success": true,
  "data": {}
}
```

---

## 1. Authentication
Base: `/api/auth`

### POST /api/auth/register
```json
{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "secret123"
}
```

### POST /api/auth/login
```json
{
  "email": "admin1@finly.com",
  "password": "admin1"
}
```
Response `data` contains:
```json
{
  "accessToken": "...",
  "refreshToken": "..."
}
```

### POST /api/auth/token/refresh
```json
{
  "refreshToken": "<refreshToken>"
}
```

---

## 2. Users
Base: `/api/users`

- `GET /api/users/me`

---

## 3. Accounts
Base: `/api/accounts`

- `POST /api/accounts`
- `GET /api/accounts`
- `GET /api/accounts/{id}`
- `PUT /api/accounts/{id}`
- `DELETE /api/accounts/{id}`

### POST /api/accounts body
```json
{
  "name": "Main Wallet",
  "type": "CASH",
  "currency": "USD",
  "initialBalance": 1200.50,
  "cardNumber": null,
  "cardType": null,
  "expiryDate": null
}
```

---

## 4. Categories
Base: `/api/categories`

- `POST /api/categories`
- `GET /api/categories`
- `GET /api/categories/{id}`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`

### POST /api/categories body
```json
{
  "name": "Food",
  "type": "EXPENSE"
}
```

---

## 5. Expenses
Base: `/api/expenses`

- `POST /api/expenses`
- `GET /api/expenses?accountId=&categoryId=&startDate=&endDate=`
- `GET /api/expenses/{id}`
- `PUT /api/expenses/{id}`
- `DELETE /api/expenses/{id}`

### POST /api/expenses body
```json
{
  "amount": 55.75,
  "currency": "USD",
  "description": "Lunch",
  "expenseDate": "2026-03-05",
  "categoryId": "<category-uuid>",
  "accountId": "<account-uuid>"
}
```

`currency` values: `USD`, `EUR`, `UZS`  
If `currency` is not sent, backend uses selected account currency by default.

### Expense currency conversion behavior
- If expense `currency` is different from the selected account currency, backend automatically converts by exchange rate and deducts converted amount from account balance.
- Conversion is applied on create, update, and delete (refund).
- Current internal reference rates:
  - `USD = 1.0000`
  - `EUR = 1.0900` (to USD)
  - `UZS = 0.000079` (to USD)

### PUT /api/expenses/{id} body
```json
{
  "amount": 60.00,
  "currency": "UZS",
  "description": "Updated lunch",
  "expenseDate": "2026-03-05",
  "categoryId": "<category-uuid>",
  "accountId": "<account-uuid>"
}
```

---

## 6. Incomes
Base: `/api/incomes`

- `POST /api/incomes`
- `GET /api/incomes?accountId=&startDate=&endDate=`
- `GET /api/incomes/{id}`
- `PUT /api/incomes/{id}`
- `DELETE /api/incomes/{id}`

### POST /api/incomes body
```json
{
  "amount": 3200.00,
  "currency": "USD",
  "description": "Salary",
  "incomeDate": "2026-03-01",
  "categoryId": "<category-uuid>",
  "accountId": "<account-uuid>"
}
```

`currency` values: `USD`, `EUR`, `UZS`  
If `currency` is different from account currency, backend auto-converts by exchange rate before applying account balance change.

---

## 7. Transfers
Base: `/api/transfers`

- `POST /api/transfers`
- `GET /api/transfers?accountId=&startDate=&endDate=`
- `GET /api/transfers/{id}`
- `DELETE /api/transfers/{id}`

### POST /api/transfers body
```json
{
  "fromAccountId": "<from-account-uuid>",
  "toAccountId": "<to-account-uuid>",
  "amount": 200.00,
  "description": "Move to savings",
  "transferDate": "2026-03-05",
  "exchangeRate": 1.0
}
```

---

## 8. Budgets
Base: `/api/budgets`

- `POST /api/budgets`
- `GET /api/budgets?year=&month=`
- `DELETE /api/budgets/{id}`

### POST /api/budgets body
```json
{
  "categoryId": "<category-uuid>",
  "type": "MONTHLY",
  "monthlyLimit": 800.00,
  "year": 2026,
  "month": 3
}
```

---

## 9. Debts
Base: `/api/debts`

- `POST /api/debts`
- `GET /api/debts?type=&status=`
- `GET /api/debts/{id}`
- `POST /api/debts/{id}/repay`
- `DELETE /api/debts/{id}`

`type` values:
- `DEBT` = siz boshqalarga qarzdorsiz
- `RECEIVABLE` = boshqalar sizga qarzdor (haqdorlik)

`status` values:
- `OPEN`
- `CLOSED`

### POST /api/debts body
```json
{
  "personName": "Ali",
  "type": "RECEIVABLE",
  "currency": "UZS",
  "accountId": "<optional-account-uuid>",
  "amount": 500.00,
  "description": "Short-term loan",
  "dueDate": "2026-04-01"
}
```

`currency` values: `USD`, `EUR`, `UZS`  
`accountId` optional:
- `DEBT` bo'lsa: summa accountga tushadi (konvertatsiya bilan).
- `RECEIVABLE` bo'lsa: summa accountdan chiqadi (konvertatsiya bilan).

### POST /api/debts/{id}/repay body
```json
{
  "paymentAmount": 100.00,
  "accountId": "<optional-account-uuid>"
}
```

---

## 10. Analytics
Base: `/api/analytics`

- `GET /api/analytics/summary`
- `GET /api/analytics/expenses-by-category?from=&to=`
- `GET /api/analytics/monthly-expenses`
- `GET /api/analytics/income-vs-expense`
- `GET /api/analytics/account-balances`
- `GET /api/analytics/dashboard?startDate=&endDate=`
- `GET /api/analytics/categories?type=&startDate=&endDate=`
- `GET /api/analytics/timeseries?period=&startDate=&endDate=`

### GET /api/analytics/summary
No params.

Example:
`GET /api/analytics/summary`

Response:
```json
{
  "success": true,
  "data": {
    "totalBalance": 3500,
    "totalIncome": 2000,
    "totalExpense": 1200,
    "savings": 800
  }
}
```

### GET /api/analytics/expenses-by-category
Params:
- `from` (optional, `YYYY-MM-DD`, default: first day of current month)
- `to` (optional, `YYYY-MM-DD`, default: today)

Example:
`GET /api/analytics/expenses-by-category?from=2025-01-01&to=2025-01-31`

Response `data`:
```json
[
  { "category": "Food", "amount": 300 },
  { "category": "Transport", "amount": 150 }
]
```

### GET /api/analytics/monthly-expenses
Params:
- `from` (optional, `YYYY-MM-DD`)
- `to` (optional, `YYYY-MM-DD`)

Example:
`GET /api/analytics/monthly-expenses?from=2025-01-01&to=2025-12-31`

Response `data`:
```json
[
  { "month": "2025-01", "amount": 1200 },
  { "month": "2025-02", "amount": 900 }
]
```

### GET /api/analytics/income-vs-expense
Params:
- `from` (optional, `YYYY-MM-DD`)
- `to` (optional, `YYYY-MM-DD`)

Example:
`GET /api/analytics/income-vs-expense?from=2025-01-01&to=2025-12-31`

Response `data`:
```json
[
  { "month": "2025-01", "income": 2000, "expense": 1200 }
]
```

### GET /api/analytics/account-balances
No params.

Example:
`GET /api/analytics/account-balances`

Response `data`:
```json
[
  { "accountName": "Cash", "balance": 200 },
  { "accountName": "Bank Card", "balance": 1500 }
]
```

### GET /api/analytics/dashboard
Params:
- `startDate` (optional, `YYYY-MM-DD`, default: endDate - 1 month)
- `endDate` (optional, `YYYY-MM-DD`, default: today)

Example:
`GET /api/analytics/dashboard?startDate=2026-02-01&endDate=2026-03-01`

### GET /api/analytics/categories
Params:
- `type` (optional, default: `EXPENSE`)
  - accepted: `EXPENSE`, `EXPENSES`, `INCOME`, `INCOMES`
- `startDate` (optional, `YYYY-MM-DD`, default: endDate - 1 month)
- `endDate` (optional, `YYYY-MM-DD`, default: today)

Example:
`GET /api/analytics/categories?type=EXPENSE&startDate=2026-02-01&endDate=2026-03-01`

### GET /api/analytics/timeseries
Params:
- `period` (required)
  - accepted: `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`
  - aliases: `DAY`, `WEEK`, `MONTH`, `YEAR`, `D`, `W`, `M`, `Y`
- `startDate` (optional, `YYYY-MM-DD`, default: endDate - 1 month)
- `endDate` (optional, `YYYY-MM-DD`, default: today)

Example:
`GET /api/analytics/timeseries?period=MONTHLY&startDate=2025-01-01&endDate=2026-03-01`

---

## 11. Notifications
Base: `/api/notifications`

- `GET /api/notifications`
- `POST /api/notifications/{id}/read`

### POST /api/notifications/{id}/read
Request body: none

---

## 12. Exchange Rates
Base: `/api/exchange-rates` (Admin only)

- `GET /api/exchange-rates` (effective rates, ADMIN override > API)
- `POST /api/exchange-rates` (create admin override)
- `PUT /api/exchange-rates/{id}` (update admin override)
- `POST /api/exchange-rates/refresh` (fetch from internet API and store)

### POST /api/exchange-rates body
```json
{
  "baseCurrency": "USD",
  "targetCurrency": "UZS",
  "rate": 12658.2278
}
```

### Data source policy
- Recommended mode: `Internet + Admin Override`
- Source priority: `ADMIN` override first, then latest `API` rate.
- Database table:
  - `exchange_rates`
  - `id`, `base_currency`, `target_currency`, `rate`, `source`, `created_at`

---

## 13. System Connect
Base: `/api/connect`

- `POST /api/connect/{endpoint}/{method}`

### POST /api/connect/{endpoint}/{method} body
```json
{
  "raw": "{\"any\":\"json\"}"
}
```

---

## cURL Examples (POST)

### Login
```bash
curl -X POST 'http://localhost:8080/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin1@finly.com","password":"admin1"}'
```

### Create Account
```bash
curl -X POST 'http://localhost:8080/api/accounts' \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Wallet","type":"CASH","currency":"USD","initialBalance":100}'
```

### Create Expense
```bash
curl -X POST 'http://localhost:8080/api/expenses' \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"amount":10,"currency":"USD","description":"Coffee","expenseDate":"2026-03-05","categoryId":"<category-uuid>","accountId":"<account-uuid>"}'
```

### Repay Debt
```bash
curl -X POST 'http://localhost:8080/api/debts/<debt-uuid>/repay' \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"paymentAmount":50,"accountId":"<account-uuid>"}'
```

---

## Admin UI Routes (Vaadin)
- `/admin`
- `/admin/dashboard`
- `/admin/users`
- `/admin/accounts`
- `/admin/incomes`
- `/admin/expenses`
- `/admin/transfers`
- `/admin/budgets`
- `/admin/debts`
- `/admin/categories`
- `/admin/analytics`
- `/admin/notifications`
- `/admin/exchange-rates`
- `/admin/system`

Admin UI internally calls `/api/...` endpoints with Bearer token.
