# Infrastructure

The root `docker-compose.yml` is development-only (loopback database ports,
development credentials, Swagger enabled). Production runs the same backend image
with managed PostgreSQL including pgvector and Redis, no `dev` Spring profile,
TLS termination, secret injection and an external OIDC issuer. No Kubernetes or
message broker is needed. Never deploy the development Compose file publicly.
