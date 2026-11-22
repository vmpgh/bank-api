# Architecture

```text
                               INTERNET
                                  │
                                  ▼
                           ┌──────────────┐
                           │   AWS ALB    │
                           │     :80      │
                           └──────┬───────┘
                                  │
                          bank-api-ingress
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                              AMAZON EKS                                  │
│                                                                          │
│                    ┌──────────────────────────┐                          │
│                    │        BANK API          │                          │
│                    │   Spring Boot / JWT      │                          │
│                    │       ClusterIP          │                          │
│                    └────────────┬─────────────┘                          │
│                                 │                                        │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                         OBSERVABILITY                              │  │
│  │                                                                    │  │
│  │                   OpenTelemetry Collector                          │  │
│  │                              │                                     │  │
│  │                    ┌─────────┼─────────┐                           │  │
│  │                    ▼         ▼         ▼                           │  │
│  │               Prometheus  Grafana   Jaeger                         │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                         SUPPORTING TOOLS                           │  │
│  │                                                                    │  │
│  │              Kafka UI                 RedisInsight                 │  │
│  │              ClusterIP                 ClusterIP                   │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
└──────────────────────────────────┬───────────────────────────────────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
                    ▼              ▼              ▼
             ┌────────────┐ ┌─────────────┐ ┌─────────────────┐
             │ Amazon RDS │ │ ElastiCache │ │ MSK Serverless  │
             │ PostgreSQL │ │    Redis    │ │     Kafka       │
             └────────────┘ └─────────────┘ └────────┬────────┘
                                                     │
                                                     ▼
                                               Event Processing


                    ┌──────────────────────────────┐
                    │      SECURITY / SECRETS      │
                    │                              │
                    │      EKS Pod Identity        │
                    │               │              │
                    │               ▼              │
                    │     AWS Secrets Manager      │
                    │               │              │
                    │       ┌───────┴───────┐      │
                    │       ▼               ▼      │
                    │  JWT Secret  RDS Credentials │
                    └──────────────────────────────┘


                    ┌──────────────────────────────┐
                    │       INFRASTRUCTURE         │
                    │                              │
                    │      Terraform   Jenkins     │
                    └──────────────────────────────┘
````
## Transaction history (ledger)

Every balance change writes an immutable row to `account_transactions`. The write happens in the **same database transaction** as the balance update, so the ledger and the balance can never disagree.

| Operation | Rows written |
|---|---|
| Deposit | 1 × `DEPOSIT` |
| Withdrawal | 1 × `WITHDRAWAL` |
| Transfer | `TRANSFER_OUT` on the sender + `TRANSFER_IN` on the receiver, sharing one `transfer_id` (= `TransferCompletedEvent.eventId`) |

Each row stores `amount` and `balance_after`. A transfer row also stores `counterparty_account_id`. Failed operations write nothing.

Read API: `GET /api/v1/accounts/{id}/transactions` (admin or account owner).
- Results are newest first and paged with `page` (default 0) and `size` (default 20, max 100).
- Optional filters: `type`, `from` (inclusive) and `to` (exclusive), as ISO-8601 instants.
- The response is a `PageResponse` with fields `content`, `page`, `size`, `totalElements` and `totalPages`.
- History is not cached.

History starts with migration `V7`. Earlier deposits, withdrawals and transfers were not backfilled.
