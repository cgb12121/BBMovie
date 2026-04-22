Use the **checkout endpoint** — in your service, “create payment” is done by `POST /api/v1/payments/checkout`.

## 1) Start service with PayPal env set
Make sure these env vars are loaded (from your `.env` or system env):

- `PAYPAL_CLIENT_ID`
- `PAYPAL_CLIENT_SECRET`
- `PAYPAL_WEBHOOK_ID`
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` (MySQL)

And service runs on your configured port (`8999` in current `application.properties`).
zrok share public http://127.0.0.1:8999

stripe listen --forward-to localhost:8999/api/v1/webhooks/stripe
stripe trigger payment_intent.succeeded or charge.refunded

---

## 2) Postman request: create checkout for PayPal

**Method:** `POST`  
**URL:** `http://localhost:8999/api/v1/payments/checkout`

### Headers
- `Content-Type: application/json`
- `Idempotency-Key: checkout-paypal-001`  
  (must be unique per new checkout attempt; reuse same key to replay same result)

### Body (raw JSON)
```json
{
  "userId": "user_123",
  "userEmail": "user123@example.com",
  "provider": "PAYPAL",
  "amount": 9.99,
  "currency": "USD",
  "purpose": "BBMovie Premium Monthly",
  "metadata": {
    "planId": "premium_monthly",
    "source": "postman"
  }
}
```

---

## 3) Expected response

You should get `200 OK` with shape like:

```json
{
  "orchestratorPaymentId": "0196....",
  "provider": "PAYPAL",
  "providerPaymentId": "5O190127TN364715T",
  "status": "PENDING",
  "paymentUrl": "https://www.sandbox.paypal.com/checkoutnow?token=...",
  "clientSecret": null,
  "providerMetadata": {
    "providerStatus": "CREATED"
  }
}
```

For PayPal, the important field is usually `paymentUrl` (approve URL). Open it in browser to continue payer approval.

---

## 4) Optional: test webhook callback (after approval)
Your webhook endpoint is:

- `POST /api/v1/webhooks/{provider}`
- For PayPal: `POST /api/v1/webhooks/paypal`

But for real signature verification, PayPal needs proper webhook setup and correct headers/body from PayPal itself (or simulator + valid config).

---

If you want, I can also give you a ready-to-import **Postman Collection JSON** (checkout + refund + webhook test requests).