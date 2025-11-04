# Transaction Service

Microservicio reactivo para gestión de transacciones bancarias.

## Arquitectura

- **Framework**: Spring Boot 3.2 + WebFlux
- **Database**: PostgreSQL 15 con R2DBC (reactive)
- **Observability**: Actuator + Prometheus metrics
- **Validation**: Bean Validation (Jakarta)

## Requisitos

- Java 17+
- Docker & Docker Compose
- Maven 3.8+

## Setup Local
```bash
# 1. Levanta Postgres
docker-compose up -d

# 2. Ejecuta la aplicación
./mvnw spring-boot:run

# 3. Verifica health
curl http://localhost:8080/actuator/health
```

## API Endpoints

### Crear Transacción
```bash
POST /api/v1/transactions
Content-Type: application/json

{
  "accountId": "ACC001",
  "amount": 100.50,
  "type": "TRANSFER"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "accountId": "ACC001",
  "amount": 100.50,
  "type": "TRANSFER",
  "status": "PENDING",
  "createdAt": "2024-11-04T10:30:00"
}
```

### Obtener Transacción
```bash
GET /api/v1/transactions/{id}
```

### Listar por Cuenta
```bash
GET /api/v1/transactions?accountId=ACC001
```

## Testing
```bash
# Unit tests
./mvnw test

# Latency measurement
./scripts/measure-latency.sh
```

## Performance (Baseline - Semana 1)

- **Latency promedio**: 153ms
- **Endpoint**: POST /api/v1/transactions
- **Environment**: Local (Docker Compose)
- **Tests**: 50 requests

## Roadmap

- [x] Setup básico (Día 1)
- [x] API REST completa (Día 2)
- [x] Validaciones y exception handling (Día 2)
- [ ] Profiling y optimización (Día 3-4)
- [ ] Caché distribuida Redis (Semana 3)
- [ ] Event-driven con Kafka (Semana 4)
- [ ] Observabilidad completa (Semana 5-6)
- [ ] Load testing (Semana 7)
- [ ] Resiliencia (Semana 8)

## 👤 Hossam Benassar

Benas - Backend Engineer
