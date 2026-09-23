# Infrastructure

The root `docker-compose.yml` is development-only (loopback database ports,
Swagger enabled). Run `node infra/init-dev-env.mjs` once from the repository root
to create an ignored `.env` with random database and encryption keys. An existing
`.env` is never overwritten. Do not commit provider credentials or private keys.
Production runs the same backend image
with managed PostgreSQL including pgvector and Redis, no `dev` Spring profile,
TLS termination, secret injection and an external OIDC issuer. No Kubernetes or
message broker is needed. Never deploy the development Compose file publicly.
