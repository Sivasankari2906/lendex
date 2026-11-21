# Lendex

Loan and EMI tracker: borrowers, loans, interest payments, EMIs, reminders and email OTP sign-up.

- **Backend:** Spring Boot 3, Spring Security, JWT, PostgreSQL (`backend`)
- **Frontend:** React (`frontend`)

## Configuration

No secrets are stored in the repo. The backend reads them from environment variables:

| Variable | Required | Description |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | yes | PostgreSQL connection |
| `LENDEX_JWT_SECRET` | yes | JWT signing secret, at least 32 characters. The backend will not start without it. |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | for email | Gmail address and app password for OTP and reset emails |
| `TEXTBEE_API_KEY`, `TEXTBEE_DEVICE_ID`, `TEXTBEE_BASE_URL` | for SMS | [textbee](https://github.com/textbee/textbee) gateway settings. Without a key, OTPs go by email. |
| `FRONTEND_URL` | no | Base URL for reset links, defaults to `http://localhost:3000` |

Copy `.env.example` to `.env` and fill it in. On a hosting platform (Render, Railway), set the same variables in its settings.

## SMS setup (textbee, open source)

1. Install the textbee Android app on a phone with a SIM (from textbee.dev/download), or self-host the server from the textbee repo.
2. Create an account, add the phone as a device and copy the API key.
3. Set `TEXTBEE_API_KEY` (and `TEXTBEE_DEVICE_ID` to choose a phone). OTPs and reminders then go out by SMS from that phone.

## Run locally

```bash
docker compose up --build        # uses .env
```

Or run the backend with `cd backend && ./mvnw spring-boot:run` after exporting the variables, and the frontend with `cd frontend && npm install && npm start`.

## Tests

```bash
cd backend && ./mvnw test        # uses an in-memory H2 database, no external services needed
```
