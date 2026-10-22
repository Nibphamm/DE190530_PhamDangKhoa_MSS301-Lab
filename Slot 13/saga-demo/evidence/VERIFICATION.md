# Verification evidence

Generated: 2026-10-22T12:22:45.273765+00:00

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

## Postman Desktop status

The user confirmed importing the base collection into Postman Desktop.
Desktop Runner and saga-log screenshots are still pending. Codex Computer Use returned:
`Computer Use native pipe is unavailable ... The system cannot find the file specified. (os error 2)`.
Newman results above execute the Postman collections; they are not Desktop Runner screenshots.
No screenshot has been fabricated or labelled as a successful Desktop run.

## Reproduce

Follow README.md for profile startup and test commands.
Use a fresh KB8 outage before rerunning the one-time recovery folder.
Ensure enough seed stock/balance; successful purchases consume resources.
