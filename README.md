# jBPM Engine

Spring Boot application that acts as a workflow orchestrator using **jBPM 7 KIE Server REST API** — the jBPM equivalent of the Camunda-based `workflow-engine` project.

## Architecture

This service communicates with jBPM KIE Server via its REST API to:
- Start and manage process instances
- Assign, claim, start, and complete user tasks
- Query process definitions and task lists
- Signal running process instances

It also integrates with the same external microservices as the Camunda workflow-engine:
- **Document Service** (port 8003)
- **Person Service** (port 8004)
- **Property Service** (port 8005)
- **Notification Service** (port 8002)
- **Verification Service** (port 8000)

## Port Mapping

| Service | Port |
|---------|------|
| jBPM Engine (this app) | 8006 |
| jBPM Server (Business Central + KIE Server) | 8090 (→ 8080) |
| jBPM Postgres | 5434 (→ 5432) |

## Quick Start

### With Docker Compose (recommended)

```bash
# Build the application first
./gradlew bootJar

# Start everything (jBPM Server, PostgreSQL, and this engine)
docker compose up -d
```

### Local Development

```bash
# Start jBPM server separately first
docker compose up -d jbpm-server-full jbpm-postgres

# Run the Spring Boot app locally
./gradlew bootRun
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/processes/{processId}/start` | Start a process instance |
| DELETE | `/api/v1/processes/{processInstanceId}` | Cancel/abort a process instance |
| GET | `/api/v1/processes/definitions` | List all process definitions |
| GET | `/api/v1/processes/instances/{id}` | Get process instance by ID |
| GET | `/api/v1/processes/instances/{id}/variables` | Get process instance variables |
| POST | `/api/v1/processes/instances/{id}/signal/{signalName}` | Signal a process |
| PATCH | `/api/v1/tasks/{taskId}/assign/{personId}` | Assign a task |
| PATCH | `/api/v1/tasks/{taskId}/complete` | Complete a task |
| POST | `/api/v1/tasks/{taskId}/start` | Start a task |
| POST | `/api/v1/tasks/{taskId}/reassign` | Reassign a task |
| GET | `/api/v1/tasks/user/{userId}?state=` | Get user's tasks |
| GET | `/api/v1/tasks/process/instance/{id}?state=` | Get tasks by process instance |

### Swagger UI
Available at: `http://localhost:8006/swagger-ui.html`

## Comparison with Camunda Workflow Engine

| Feature | Camunda (`workflow-engine`) | jBPM (`jbpm-engine`) |
|---------|----------------------------|----------------------|
| Engine | Camunda 8 (Zeebe) | jBPM 7 (KIE Server) |
| Protocol | gRPC + REST (Zeebe/Operate/Tasklist) | REST (KIE Server API) |
| Port | 8001 | 8006 |
| Job Workers | `@JobWorker` annotations | KIE Server work item handlers |
| Task API | Camunda Tasklist REST | KIE Server Task REST |
| Process Queries | Camunda Operate REST | KIE Server Queries REST |

## Configuration

Key properties in `application.properties`:

```properties
jbpm.kieserver.url=http://localhost:8090/kie-server/services/rest/server
jbpm.kieserver.user=admin
jbpm.kieserver.password=admin
jbpm.container.id=property-platform
```
