# Verification evidence

Generated: 2026-10-22T14:50:08.011830+00:00

| Report | HTTP requests | Assertions | Failures |
|---|---:|---:|---:|
| [newman-base.json](newman-base.json) | 49 | 120 | 0 |
| [extensions-choreography-failure.json](extensions-choreography-failure.json) | 9 | 14 | 0 |
| [extensions-choreography-success.json](extensions-choreography-success.json) | 8 | 14 | 0 |
| [extensions-http-interface.json](extensions-http-interface.json) | 6 | 14 | 0 |
| [extensions-outbox-failure.json](extensions-outbox-failure.json) | 17 | 14 | 0 |
| [extensions-outbox-queued.json](extensions-outbox-queued.json) | 4 | 6 | 0 |
| [extensions-outbox-resumed.json](extensions-outbox-resumed.json) | 9 | 13 | 0 |
| [extensions-recovery.json](extensions-recovery.json) | 7 | 16 | 0 |
| [extensions-shipping-idempotency.json](extensions-shipping-idempotency.json) | 9 | 17 | 0 |
| [extensions-shipping-rejected.json](extensions-shipping-rejected.json) | 7 | 17 | 0 |
| [extensions-shipping-success.json](extensions-shipping-success.json) | 8 | 17 | 0 |
| [extensions-timeout.json](extensions-timeout.json) | 7 | 16 | 0 |

Total: **278 assertions passed**, 140 HTTP requests.
JUnit: **7 tests passed**. Real container outages KB6 and KB8 passed.

KB8 evidence records PENDING/COMPENSATING and exactly three failed compensation attempts.
The recovery report subsequently verifies stock restoration and repeat recovery safety.

## Postman Desktop results

Run started: 2026-10-22T21:39:07+07:00. Computer Use controlled Postman through `@oai/sky`.
Collection Runner: **120/120 assertions passed**, 36 selected requests, one iteration, zero errors.
Duration: 10.885 seconds; average response time: 59 ms. The manual folder was excluded.
Manual KB6: **18/18 assertions passed** across five requests with Payment actually stopped.
Order 6 ended CANCELLED / COMPENSATED; stock and wallets were unchanged. Payment was restarted.
The user approved resetting only Saga volumes before this Desktop run.
Seed: P1=10, P2=5, C1=5000, C2=100. Final: P1=8, P2=5, C1=3000, C2=100.
Historical Newman, extension, KB8 and recovery reports above were preserved before the reset.
Desktop assertions are reported separately from the 278 historical Newman assertions.
See [Desktop run metadata](postman-desktop-run.json), [Runner text](postman-runner-results.txt),
[post-Runner data](postman-desktop-data.json) and [post-KB6 data](postman-desktop-kb6-data.json).

### Unedited Computer Use screenshots

- [postman-kb1-saga-logs-completed.png](postman-kb1-saga-logs-completed.png)
- [postman-kb2-saga-logs-compensated.png](postman-kb2-saga-logs-compensated.png)
- [postman-kb2-saga-logs-start.png](postman-kb2-saga-logs-start.png)
- [postman-kb6-network-failure.png](postman-kb6-network-failure.png)
- [postman-kb6-resource-verification.png](postman-kb6-resource-verification.png)
- [postman-kb6-saga-logs.png](postman-kb6-saga-logs.png)
- [postman-runner-configuration.png](postman-runner-configuration.png)
- [postman-runner-results.png](postman-runner-results.png)
- [postman-runner-summary.png](postman-runner-summary.png)
- [postman-kb6-reservation-verification.png](postman-kb6-reservation-verification.png)

## Reproduce

Follow README.md for profile startup and test commands.
Use a fresh KB8 outage before rerunning the one-time recovery folder.
Ensure enough seed stock/balance; successful purchases consume resources.
