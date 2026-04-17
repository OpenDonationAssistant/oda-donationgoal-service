## ODA DonationGoal Service
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/OpenDonationAssistant/oda-donationgoal-service)
![Sonar Tech Debt](https://img.shields.io/sonar/tech_debt/OpenDonationAssistant_oda-donationgoal-service?server=https%3A%2F%2Fsonarcloud.io)
![Sonar Violations](https://img.shields.io/sonar/violations/OpenDonationAssistant_oda-donationgoal-service?server=https%3A%2F%2Fsonarcloud.io)
![Sonar Tests](https://img.shields.io/sonar/tests/OpenDonationAssistant_oda-donationgoal-service?server=https%3A%2F%2Fsonarcloud.io)
![Sonar Coverage](https://img.shields.io/sonar/coverage/OpenDonationAssistant_oda-donationgoal-service?server=https%3A%2F%2Fsonarcloud.io)

### Running with Docker

The Docker image is available from GitHub Container Registry:

```bash
docker pull ghcr.io/opendonationassistant/oda-donationgoal-service:latest
```

#### Required Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `RABBITMQ_HOST` | RabbitMQ server hostname | `localhost` |
| `JDBC_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost/postgres?currentSchema=donationgoal` |
| `JDBC_USER` | Database username | `postgres` |
| `JDBC_PASSWORD` | Database password | `postgres` |

#### Example Docker Run

```bash
docker run -d \
  --name donationgoal-service \
  -p 8080:8080 \
  -e RABBITMQ_HOST=rabbitmq-host \
  -e JDBC_URL=jdbc:postgresql://postgres-host/postgres?currentSchema=donationgoal \
  -e JDBC_USER=postgres \
  -e JDBC_PASSWORD=your-password \
  ghcr.io/opendonationassistant/oda-donationgoal-service:latest
```

Make sure PostgreSQL and RabbitMQ are running and accessible before starting the service.

#### Example Docker Compose

Here's a complete `docker-compose.yml` example that sets up the donationgoal-service along with PostgreSQL and RabbitMQ:

```yaml
version: '3.8'

services:
  donationgoal-service:
    image: ghcr.io/opendonationassistant/oda-donationgoal-service:latest
    ports:
      - "8080:8080"
    environment:
      - RABBITMQ_HOST=rabbitmq
      - JDBC_URL=jdbc:postgresql://postgres:5432/postgres?currentSchema=donationgoal
      - JDBC_USER=postgres
      - JDBC_PASSWORD=postgres
    depends_on:
      - postgres
      - rabbitmq
    restart: unless-stopped

  postgres:
    image: postgres:16
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
      - POSTGRES_DB=postgres
    volumes:
      - postgres-data:/var/lib/postgresql/data
    restart: unless-stopped

  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "15672:15672"  # RabbitMQ Management UI
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq
    restart: unless-stopped

volumes:
  postgres-data:
  rabbitmq-data:
```

To run all services:

```bash
docker-compose up -d
```

The service will be available at `http://localhost:8080` and the RabbitMQ Management UI at `http://localhost:15672`.
