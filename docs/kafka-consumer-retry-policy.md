# Kafka Consumer Retry Policy

## Scope

This policy applies to `payment-topic` and `payment-bulk-topic` consumers in `notice-server`.

## Current Policy

| Failure case | Retry | Handling |
| --- | --- | --- |
| Invalid JSON payload | No | Log the parse failure and commit the consumed record. Replaying the same malformed payload will not recover it. |
| Notification setting lookup failure | Yes | Let the exception escape the listener and retry through the Kafka error handler. |
| Token lookup failure | Yes | Let the exception escape the listener and retry through the Kafka error handler. |
| No notification setting or disabled setting | No | Treat as a business skip. |
| No FCM token | No | Treat as a business skip. |
| FCM batch request exception | Yes | Wrap as `NotificationDeliveryException` and retry through the Kafka error handler. |
| Per-token FCM send failure in a batch response | No | Log each failed token result. Token cleanup is handled by the separate FCM token cleanup backlog. |

## Retry Settings

- Retry interval: 1 second fixed backoff.
- Retry count: 2 retries after the first listener attempt.
- Total listener attempts: 3.
- Final failure: log topic, partition, offset, key, and exception after retries are exhausted.

## Notes

- Retryable failures can cause the same Kafka record to be processed more than once. The eventId based duplicate handling backlog must provide the idempotency boundary before retry attempts can be considered fully safe for user-visible notifications.
- DLQ is not enabled yet. If final failure recovery becomes operationally important, the DLQ backlog should replace the current final-failure logging path with a dead-letter publish decision.
