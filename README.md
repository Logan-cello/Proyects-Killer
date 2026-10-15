# Proyects Killer

Proyects Killer es una propuesta de plataforma para coordinar tres asistentes de programación con IA, ejecutados desde computadoras distintas, sobre un mismo proyecto de hackatón. El objetivo es convertir asistentes independientes en un equipo de trabajo verificable: con tareas explícitas, comunicación estructurada, cambios aislados en Git y validaciones antes de integrar.

> **Estado del proyecto:** especificación inicial. Este repositorio todavía no contiene una aplicación ejecutable. La arquitectura, la pila y el flujo descritos aquí son la dirección propuesta para el desarrollo; no implican que esas capacidades ya estén implementadas.

## Problema y objetivo

Sin coordinación, los asistentes pueden duplicar trabajo, perder contexto entre conversaciones, cambiar archivos incompatibles o declarar una tarea terminada sin evidencia. Además, los cambios locales no se sincronizan automáticamente entre computadoras por el hecho de clonar un repositorio, y los asistentes disponibles tienen capacidades e interfaces diferentes.

Proyects Killer busca resolver estos problemas mediante:

- Estado centralizado de agentes, tareas, dependencias, mensajes y eventos.
- Contratos, criterios de aceptación y responsabilidades explícitos.
- Workers locales y adaptadores independientes del proveedor de IA.
- Ramas aisladas, pull requests, pruebas y revisión independiente.
- Permisos de mínimo privilegio, límites de reintentos y auditoría.

El sistema debe ayudar a analizar un reto y sus criterios de evaluación, proponer una solución, diseñar su arquitectura, dividir el trabajo, coordinar agentes, ejecutar tareas y preparar una aplicación y demo. Las decisiones importantes y las operaciones sensibles seguirán teniendo supervisión humana.

## Arquitectura inicial propuesta

```text
┌─────────────────────┐
│       GitHub        │
│ Repo / PR / CI      │
└─────────┬───────────┘
          │ Git / GitHub CLI / API
┌─────────▼───────────┐
│    Coordinador      │
│ FastAPI / SQLite    │
│ WebSocket / Policy  │
│ Engine / Scheduler  │
└──────┬───────┬──────┘
       │       │
┌──────▼───┐ ┌─▼─────────┐
│ Worker A │ │ Worker B  │
│ Agente A │ │ Agente B  │
└──────────┘ └───────────┘
             ┌───────────┐
             │ Worker C  │
             │ Agente C  │
             └───────────┘
```

Estos componentes son una propuesta, no una descripción de software ya disponible:

- **Coordinador central:** fuente de verdad para el estado global, registro y heartbeat de agentes, tareas y dependencias, asignaciones, mensajes, permisos, eventos y solicitudes de revisión. Debe integrarse con GitHub, validar evidencias, detectar desconexiones y limitar duplicados, reintentos y ciclos.
- **Worker local:** proceso en cada computadora que se registra y declara identidad, rol y capacidades; mantiene heartbeat; prepara su workspace; recibe tareas; invoca un asistente mediante un adaptador; ejecuta comandos autorizados; captura resultados, pruebas y errores; y comunica bloqueos, commits y ramas.
- **Adaptadores de asistentes:** capa que desacopla el coordinador de proveedores y herramientas concretos. El coordinador mantiene las tareas, estados, permisos y auditoría aunque un asistente cambie o no esté conectado.
- **GitHub:** repositorio compartido e historial externo para ramas, commits, PRs, revisiones y checks. Git no sincroniza automáticamente los cambios locales entre computadoras: el flujo de publicación y actualización de ramas debe ser explícito.

La dirección técnica prioriza ejecución local, bajo coste, instalación sencilla, observabilidad, seguridad, recuperación ante desconexiones y sustitución de asistentes.

## Pila tecnológica inicial propuesta

La inspección actual encontró únicamente este README y los metadatos de Git. No hay manifiestos de dependencias, código, configuración de CI/CD, `.github/` ni `.Proyects/`. Por tanto, no hay un lenguaje o framework ya establecido por el repositorio.

| Área | Propuesta inicial |
| --- | --- |
| Coordinador | Python 3.12+, FastAPI, Uvicorn y Pydantic |
| Persistencia | SQLite; SQLAlchemy o SQLModel (decisión pendiente) |
| Comunicación | WebSocket, con polling HTTP como fallback |
| CLI | Typer |
| Calidad | Pytest y Ruff |
| Worker | Python, subprocess controlado, Git, HTTP/WebSocket y adaptadores configurables |
| Integración | Git, GitHub CLI, GitHub Actions y, cuando convenga, MCP/API/SDK |
| Interfaz | CLI funcional y API observable primero; dashboard mínimo solo si el tiempo lo permite |

Esta tabla es una propuesta de diseño: no declara dependencias instaladas ni obliga a incorporar todos estos componentes en el primer incremento.

## Roles iniciales

Los roles describen responsabilidades, no modelos de IA específicos. Podrán reasignarse según las capacidades disponibles y el estado del proyecto.

| Rol | Responsabilidades |
| --- | --- |
| **Agente A — Estrategia y arquitectura** | Analizar el reto y sus criterios de evaluación; proponer valor y MVP; diseñar arquitectura y contratos; crear tareas y resolver dudas de diseño y dependencias. |
| **Agente B — Desarrollo e implementación** | Implementar tareas asignadas en una rama aislada; respetar contratos; ejecutar pruebas; informar avances y bloqueos; crear commits y preparar PRs. |
| **Agente C — Calidad e integración** | Revisar código; ejecutar pruebas independientes; detectar regresiones; validar criterios de aceptación y seguridad; solicitar correcciones; validar integración y preparar la demo. |

## MVP propuesto

El primer MVP debe demostrar un flujo completo y observable:

1. Registrar tres workers y mostrar su conexión.
2. Crear un reto de hackatón y registrar la especificación del proyecto.
3. Crear tareas con responsable, dependencias y criterios de aceptación.
4. Asignar una tarea y enviar mensajes estructurados entre agentes.
5. Actualizar y consultar el progreso.
6. Crear una rama por tarea y ejecutar una modificación controlada.
7. Crear un commit, publicar la rama y crear o registrar un pull request.
8. Solicitar revisión a otro agente y ejecutar pruebas.
9. Registrar evidencias y resultados; aprobar o rechazar la tarea.
10. Mostrar el estado final en el coordinador.

El MVP **no** incluye un enjambre ilimitado, asignación mediante aprendizaje automático, resolución automática de conflictos complejos, despliegues automáticos a producción, control universal de interfaces gráficas, integración profunda con todos los asistentes, merge autónomo de cambios sensibles, memoria semántica avanzada ni infraestructura cloud obligatoria.

## Estados y ciclo de vida de tareas

```text
BACKLOG → READY → ASSIGNED → IN_PROGRESS
                                  ├── BLOCKED
                                  └── READY_FOR_REVIEW → VALIDATING
                                                          ├── FAILED
                                                          └── PASSED → MERGED → DONE

Estados adicionales: CHANGES_REQUESTED, CANCELLED
```

Reglas del ciclo:

- Una tarea no pasa directamente de `IN_PROGRESS` a `DONE`.
- Toda tarea tiene criterios de aceptación antes de considerarse lista.
- Completar una tarea requiere evidencia verificable.
- Cada revisión está vinculada a una tarea y a los cambios revisados.
- Una aprobación sin pruebas requeridas y evidencia no es válida.
- Los fallos de validación pueden reabrir la tarea para corrección.
- Se establecen límites de reintentos y rondas de revisión para evitar ciclos infinitos.
- Las transiciones válidas, permisos y condiciones exactas se definirán en los contratos operativos.

## Protocolo de mensajes

Los mensajes tienen un sobre común para facilitar correlación, deduplicación, auditoría y control de caducidad. Ejemplo JSON válido:

```json
{
  "message_id": "msg-00042",
  "conversation_id": "conv-00007",
  "created_at": "2026-01-15T18:30:00Z",
  "sender": "agent-b",
  "recipient": "agent-c",
  "type": "REVIEW_REQUESTED",
  "task_id": "HK-003",
  "correlation_id": "run-00012",
  "status": "PENDING",
  "priority": "NORMAL",
  "payload": {
    "branch": "feature/HK-003-agent-b",
    "commit_sha": "0123456789abcdef0123456789abcdef01234567",
    "evidence": ["pytest: 24 passed"]
  },
  "requires_response": true,
  "expires_at": "2026-01-15T20:30:00Z"
}
```

El SHA y la evidencia del ejemplo son ilustrativos, no corresponden a cambios existentes.

Tipos iniciales:

```text
AGENT_REGISTERED       AGENT_HEARTBEAT       TASK_CREATED
TASK_ASSIGNED          TASK_ACCEPTED         TASK_REJECTED
TASK_PROGRESS          QUESTION              ANSWER
PROPOSAL               CONTRACT_CHANGED      CHANGE_DETECTED
TASK_BLOCKED           REVIEW_REQUESTED      REVIEW_APPROVED
REVIEW_REJECTED        TEST_STARTED          TEST_RESULT
PR_CREATED             PR_UPDATED            TASK_COMPLETED
TASK_CANCELLED         HUMAN_APPROVAL_REQUIRED
```

El coordinador deberá deduplicar mensajes por identificador, verificar que cada tarea tenga un responsable, limitar reintentos y rondas de revisión, registrar cambios y expiración, y exigir autorización y evidencia para transiciones sensibles. Las respuestas correlacionadas no deben generar nuevas tareas sin una decisión explícita.

## Especificación operativa `.Proyects/`

Se propone que `.Proyects/` sea la especificación central, portable y revisable del proyecto. **No se crea todavía** como parte de esta documentación:

```text
.Proyects/
├── project.yaml
├── architecture.md
├── contracts/
│   ├── api.yaml
│   └── events.json
├── tasks/
├── decisions/
├── acceptance/
├── agents/
└── policies/
```

Ejemplo ilustrativo de `project.yaml` (YAML válido; no representa un archivo que ya exista):

```yaml
name: Proyects Killer
status: proposed
coordinator:
  framework: FastAPI
  database: SQLite
workers:
  initial_count: 3
```

Las responsabilidades se separan así:

- **`README.md`:** visión, estado, arquitectura propuesta, flujo de desarrollo y orientación para colaboradores.
- **`.Proyects/`:** contratos y especificación operativa versionada: tareas, decisiones, aceptación, agentes y políticas.
- **SQLite:** estado runtime del coordinador cuando se implemente.
- **GitHub:** control de versiones, revisión, checks e historial externo.

## Flujo de Git propuesto

- No trabajar directamente sobre `main`; crear una rama por tarea.
- Usar nombres como `feature/HK-001-agent-b`, `fix/HK-002-agent-c` y `test/HK-003-agent-c`.
- Ejecutar `git fetch` antes de empezar y comprobar la base de la rama.
- Ejecutar las pruebas antes de solicitar revisión.
- Identificar tarea y agente en cada commit, por ejemplo:

  ```text
  feat(HK-001): implement task orchestration [agent-b]
  ```

- Publicar la rama y comunicar a los demás workers los cambios disponibles. Cada worker actualiza su copia usando el flujo acordado; un `git pull` debe hacerse solo en la rama y el workspace correctos, evitando sobrescribir trabajo local.
- Para cambios sensibles, usar pull request y revisión asociada a la tarea.
- El coordinador registra branch, SHA del commit, PR, checks y evidencias.
- No hacer force push por defecto. No integrar automáticamente operaciones sensibles.

## Seguridad y permisos

El diseño seguirá mínimo privilegio: cada worker recibirá solo las herramientas y permisos necesarios para su tarea. Los comandos autorizados deberán ser controlados y auditables.

| Operación | Política inicial |
| --- | --- |
| Leer el repositorio | Permitida |
| Modificar el workspace | Permitida dentro de la tarea |
| Ejecutar pruebas | Permitida |
| Crear commits | Permitida en una rama de tarea |
| Hacer push | Permitida a la rama de tarea |
| Crear pull requests | Permitida |
| Hacer merge | Requiere autorización humana |
| Leer secretos | Prohibida por defecto |
| Desplegar a producción | Requiere autorización humana |
| Eliminar recursos | Requiere autorización humana |
| Force push | Requiere autorización humana |

Los secretos nunca deben guardarse en el README, commits, logs, mensajes ni archivos de configuración versionados. Las credenciales deben proporcionarse fuera del repositorio y no exponerse al asistente salvo necesidad explícita y autorizada. Los límites de comandos, aprobación humana y registro de eventos deberán definirse antes de habilitar ejecución autónoma.

## Integraciones y límites reales

Los adaptadores deben cubrir estos niveles, según lo que cada herramienta permita:

1. Integración nativa mediante API o SDK.
2. Integración mediante CLI.
3. Integración mediante MCP.
4. Integración mediante ACP cuando esté disponible.
5. Modo manual asistido como fallback.

MCP puede exponer herramientas y contexto al asistente, pero no sustituye al coordinador como fuente de verdad para tareas, estados, permisos y auditoría.

Hay límites que el sistema debe asumir y comunicar:

- No todos los asistentes ofrecen API, CLI ni control externo de conversaciones.
- Planes gratuitos y servicios externos pueden imponer límites o costes.
- Las respuestas pueden parecer plausibles y aun así ser incorrectas.
- El contexto puede perderse entre sesiones y las interfaces de herramientas pueden cambiar.
- El control mediante interfaz gráfica puede ser frágil.
- Las pruebas no garantizan por sí solas una buena demo.
- Ningún agente debe afirmar que una tarea está terminada sin evidencia.

Los adaptadores y el modo manual asistido permitirán trabajar con capacidades distintas sin prometer compatibilidad universal.

## Automatización prevista

**Candidatos a automatizar inicialmente:** registro y heartbeat de agentes; asignación y mensajería de tareas; creación de ramas; ejecución de comandos permitidos; captura de resultados; commits y push; creación de PRs; consulta de checks; solicitud de revisión; registro de pruebas; detección de desconexiones y generación de reportes.

**Requiere intervención humana:** elegir la idea ganadora; aprobar cambios críticos de arquitectura; proporcionar credenciales; acceder a secretos; hacer despliegues sensibles; resolver conflictos complejos; aprobar operaciones destructivas; validar la calidad final de la demo; decidir eliminar funcionalidades y autorizar costes o servicios externos.

## Roadmap inicial

1. **Fase 0 — Documentación y contratos:** README, modelos de agentes, tareas y mensajes, estados, permisos y criterios de aceptación.
2. **Fase 1 — Coordinador mínimo:** FastAPI, SQLite, registro de agentes, tareas, mensajes, heartbeats y máquina de estados.
3. **Fase 2 — Worker local:** CLI, registro, recepción de tareas, ejecución local, captura de resultados y estado de conexión.
4. **Fase 3 — Git:** ramas, commits, push, integración con GitHub CLI y pull requests.
5. **Fase 4 — Revisión y validación:** solicitudes de revisión, pruebas, evidencias, rechazos, reintentos y criterios de aceptación.
6. **Fase 5 — Adaptadores de asistentes:** adaptador CLI genérico, MCP, API/SDK y modo manual asistido.
7. **Fase 6 — Dashboard y demo:** vistas de agentes, tareas y mensajes; estado de PRs; resultados de pruebas e informe final.

## Estado del repositorio y próximos pasos

En la inspección para esta especificación, el repositorio contiene solo este `README.md` y metadatos de Git. No se detectaron código, manifiestos de dependencias, configuración de CI/CD, `.github/` ni `.Proyects/`. Por ello, las tecnologías y funciones anteriores son propuestas de desarrollo y no capacidades implementadas o verificadas. La siguiente etapa deberá acordar los contratos y políticas de `.Proyects/` antes de implementar el coordinador.
