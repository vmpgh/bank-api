# Cloud-Native Banking Backend — Spring Boot, Kafka, AWS EKS & Kubernetes

The project demonstrates a complete path from application development
to cloud deployment, observability, security, and CI/CD, 
while addressing distributed-system concerns such as idempotency, rate limiting, caching,
distributed locking, asynchronous processing, and reliable event publication.


## Architecture

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
The application runs as a Kubernetes workload on Amazon EKS and communicates
with AWS managed services including Amazon RDS, Amazon ElastiCache, and Amazon MSK Serverless.

---

## Key Features

### Application

- RESTful banking APIs built with Spring Boot
- JWT authentication and role-based authorization
- Resource ownership authorization
- PostgreSQL persistence with Spring Data JPA
- Flyway database migrations
- Transactional money transfers
- Optimistic locking for concurrent account updates
- Unit and integration testing

### Event-Driven Architecture

- Transactional Outbox pattern
- Event-driven processing with Kafka
- Amazon MSK Serverless with IAM authentication
- Reliable event publication and retry handling

### Distributed Systems

- Redis caching
- Redis-based idempotency
- Redis-based rate limiting
- Distributed locking

### Cloud & Infrastructure

- Docker containerization
- Kubernetes deployment on Amazon EKS
- AWS Application Load Balancer with Kubernetes Ingress
- Amazon RDS PostgreSQL
- Amazon ElastiCache Redis
- AWS Secrets Manager
- EKS Pod Identity
- Terraform infrastructure as code
- Jenkins CI/CD

### Observability

- OpenTelemetry
- Prometheus
- Grafana
- Jaeger
- Kafka UI
- RedisInsight
---

## Technology Stack

| Technology            | Purpose                              |
| --------------------- |--------------------------------------|
| Java                  | Backend application                  |
| Spring Boot           | Application framework                |
| Spring Security       | Authentication and authorization     |
| JWT                   | Stateless authentication             |
| Swagger / OpenAPI     | API documentation and testing        |
| PostgreSQL            | Relational persistence               |
| Flyway                | Database migrations                  |
| Redis                 | Caching and distributed coordination |
| Apache Kafka          | Event-driven messaging               |
| Amazon MSK Serverless | Managed Kafka infrastructure         |
| Docker                | Containerization                     |
| Kubernetes            | Container orchestration              |
| Amazon EKS            | Managed Kubernetes                   |
| Amazon RDS            | Managed PostgreSQL                   |
| Amazon ElastiCache    | Managed Redis                        |
| AWS Secrets Manager   | Secret management                    |
| EKS Pod Identity      | AWS IAM access from workloads        |
| Terraform             | Infrastructure as code               |
| Jenkins               | CI/CD                                |
| OpenTelemetry         | Distributed tracing                  |
| Prometheus            | Metrics                              |
| Grafana               | Metrics visualization                |
| Jaeger                | Trace visualization                  |
---

## Transaction Processing

Bank transfers are processed synchronously while downstream event
processing is handled asynchronously.

```text
Transfer Request
      │
      ▼
Transfer Service
      │
      ├── Debit sender
      ├── Credit receiver
      └── Save Outbox Event
              │
              ▼
       PostgreSQL Transaction
              │
              ▼
       Outbox Scheduler
              │
              ▼
        Amazon MSK
              │
              ▼
       Event Processing
```

The account changes and outbox event are committed within the same
database transaction.

This prevents the application from successfully committing a transfer
while losing the corresponding Kafka event.

---

## Transactional Outbox

The Transactional Outbox pattern is used to solve the dual-write
problem between PostgreSQL and Kafka.

```text
PENDING
   │
   ▼
Publish to Kafka
   │
   ├── Success ──► PUBLISHED
   │
   └── Failure
          │
          ▼
        Retry
          │
          ▼
     Retry Limit
          │
          ▼
         DLT
```

This provides reliable asynchronous event publication with retry and
dead-letter handling.

---

## Kafka

Kafka provides asynchronous event-driven processing.

The AWS deployment uses Amazon MSK Serverless with IAM authentication.

```text
Bank API
   │
   ▼
Transactional Outbox
   │
   ▼
Outbox Scheduler
   │
   ▼
Amazon MSK Serverless
   │
   ▼
Kafka Consumer
   │
   ▼
Event Processing
```

Kafka decouples downstream processing from the synchronous API request.

---

## Redis

Redis is provided by Amazon ElastiCache in the AWS environment.

The application uses Redis for:

* Account caching
* Request idempotency
* Rate limiting
* Distributed locking

Example cache flow:  

```text
GET Account
     │
     ▼
   Redis
     │
     ├── Hit ──► Return cached data
     │
     └── Miss
           │
           ▼
       PostgreSQL
           │
           ▼
       Update cache
```

---

## Security

Authentication is implemented using JWT and Spring Security.

Production secrets are stored in AWS Secrets Manager rather than in
application source code or container images.

The EKS workload accesses AWS resources through EKS Pod Identity.

```text
Bank API Pod
     │
     ▼
EKS Pod Identity
     │
     ▼
IAM Role
     │
     ▼
AWS Secrets Manager
```

This removes the need for long-lived AWS credentials inside the
application.

---

## Observability

The application exposes metrics and distributed traces through an
observability stack built around OpenTelemetry, Prometheus, Grafana,
and Jaeger.

```text
                    Bank API
                       │
              ┌────────┴────────┐
              │                 │
           Metrics            Traces
              │                 │
              ▼                 ▼
         Prometheus      OpenTelemetry
              │             Collector
              ▼                 │
           Grafana              ▼
                              Jaeger
```

The system provides visibility into areas such as:

* HTTP request rate
* HTTP latency
* HTTP errors
* JVM metrics
* Database activity
* Redis activity
* Kafka processing
* Application health
* Distributed traces

Application-specific metrics include operations such as:

```text
bank.transfer
bank.outbox.save
bank.outbox.publish
bank.kafka.publish
bank.kafka.consume
```

---

## AWS Deployment

The application is deployed to Amazon EKS.

The AWS environment uses managed services for persistence, messaging,
caching, and secret management.

```text
                         AWS
                          │
                         EKS
                          │
                      Bank API
                          │
          ┌───────────────┼────────────────┐
          │               │                │
          ▼               ▼                ▼
      Amazon RDS      ElastiCache      MSK Serverless
      PostgreSQL         Redis              Kafka
                                             │
                                             ▼
                                      Event Processing
```

External access is provided through an AWS Application Load Balancer
and Kubernetes Ingress.

---

## Infrastructure as Code

AWS infrastructure is managed using Terraform.

```text
infrastructure/
└── terraform/
```

Terraform is responsible for provisioning and configuring the AWS
infrastructure required by the application.

---

## CI/CD

Jenkins is used for CI/CD automation.

The Jenkins configuration is maintained in:

```text
infrastructure/
└── jenkins/
```

The deployment pipeline follows the general flow:

```text
Git Push
   │
   ▼
Jenkins
   │
   ├── Build
   ├── Tests
   └── Docker Build
          │
          ▼
         ECR
          │
          ▼
         EKS
          │
          ▼
    Running Application
```

---

## Local Development

The application can be run locally using Docker Compose.

Prerequisite: Docker must be installed and running.

The local environment provides all required application dependencies, 
including PostgreSQL, Kafka and Redis.

Run the following command from the project root to start the application:

`docker compose up -d`

The application is then available on:

http://localhost:8080

See the project's Docker Compose file for the full list of services.

---

## Testing

The project 67 includes automated tests covering application business
logic, security and integration scenarios.

Testing includes:

* Unit tests
* Service-layer tests
* Integration tests
* Database-related tests
* Security-related tests

The project also uses Testcontainers where integration tests require
real infrastructure dependencies.

---

## Repository Structure

```text
bank-api/
│
├── src/
│   ├── main/
│   └── test/
│
├── infrastructure/
│   ├── terraform/
│   └── jenkins/
│
├── k8s/
│
├── docs/
│   ├── architecture/
│   └── postman/
│
├── Dockerfile
├── compose.yaml
├── Jenkinsfile
├── pom.xml
├── README.md
└── .gitignore
```

---

## Documentation

```text
docs/
└── architecture/
└── postman/
```

Any additional project documentation will be stored under:

docs/

Postman collections for testing the API can be found under:

docs/postman/

