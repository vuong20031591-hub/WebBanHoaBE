# Forgot Password Setup

This project now supports forgot-password with a 6-digit code sent by email.

## Backend flow

1. `POST /api/auth/forgot-password/request`
   Sends a 6-digit code to the user's email.
2. `POST /api/auth/forgot-password/confirm`
   Verifies the code and updates the backend password.

## Gmail SMTP configuration

Set these values in `.env`:

```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=yourgmail@gmail.com
MAIL_PASSWORD=YOUR_GMAIL_APP_PASSWORD
MAIL_FROM=yourgmail@gmail.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
PASSWORD_RESET_CODE_EXPIRY_MINUTES=10
```

## Gmail requirements

1. Turn on 2-Step Verification for the Gmail account.
2. Create an App Password in Google Account security settings.
3. Paste the generated App Password into `MAIL_PASSWORD`.

## Frontend

Use `http://localhost:3000/forgot-password`.

Step 1 sends the code to Gmail.
Step 2 accepts the 6-digit code and the new password.
