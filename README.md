# SprintSense Backend

> AI-powered sprint retrospective analysis. Paste a messy sprint-meeting transcript, get back a prioritized list of blockers, risks, recommendations, and assigned action items — so no idea gets lost and no task falls through the cracks.

**Live demo:** https://sprintsense-ai.vercel.app
**Live API:** https://sprintsense-backend-2.onrender.com
**Frontend repo:** *(link to be added by team)*

---

## Hackathon context

**Theme:** *AI at Work — Productivity & Teamwork Reimagined.*

Sprint reviews and retros are where teams accumulate the most actionable signal — and where most of it is lost the moment the meeting ends. Notes get scattered, action items live in someone's head, follow-ups slip. SprintSense ingests the raw meeting transcript and produces a structured, judgement-ready summary that a team can act on in seconds.

**Microsoft AI stack components used**

| Component | Role in this project |
| --- | --- |
| **GitHub Models** (Microsoft-hosted) | LLM inference for both the analysis and chat endpoints. Backed by Azure OpenAI infrastructure. |
| **Spring AI** (Azure-aligned ecosystem) | `ChatClient` abstraction, structured-output binding from LLM JSON straight into Java DTOs. |
| **GitHub** | Source hosting, secret scanning, and CI surface for the public submission repo. |
| **GitHub Copilot / Claude Code** | AI pair-programming during development (see *AI tools disclosure* below). |

---

## What it does

- **`POST /api/ai/analyze`** — accepts a sprint-meeting transcript (plain text), returns structured JSON:
  - `summary` — one-line outcome
  - `blockers` — concrete impediments the team hit
  - `risks` — forward-looking risks for the next sprint
  - `recommendations` — actionable suggestions tied to the blockers and risks
  - `actionItems` — specific tasks with `owner` (real name extracted from the transcript) and `priority` (High/Medium/Low)
- **`POST /api/ai/chat`** — answers follow-up questions against a prior analysis (e.g. *"Which blocker is most urgent and why?"*). The LLM reasons over the analysis JSON; no retrieval, no keyword matching.
- **`GET /api/health`** — liveness probe for the host platform.

---

## How it works

```
+----------------------------+
| React frontend on Vercel   |  https://sprintsense-ai.vercel.app
+-------------+--------------+
              | HTTPS, CORS allowlisted
              v
+----------------------------+
| Spring Boot 3.5 on Render  |  https://sprintsense-backend-2.onrender.com
|                            |
|  - github profile (real)   |  + Spring AI ChatClient
|  - mock profile (offline)  |  + structured-output binding -> Java DTO
+-------------+--------------+
              | Authorization: Bearer $GITHUB_TOKEN
              v
+----------------------------+
| GitHub Models              |  openai/gpt-4o-mini
| (Microsoft AI stack)       |  hosted on Azure OpenAI infra
+----------------------------+
```

The `mock` profile is the default; it serves canned responses with no network calls, so the app boots and serves traffic immediately on any host without any LLM credentials. Switching `SPRING_PROFILES_ACTIVE=github` (plus `GITHUB_TOKEN`) flips it to live LLM mode without any code change.

---

## Quick start

### Use the deployed API

```bash
curl -X POST https://sprintsense-backend-2.onrender.com/api/ai/analyze \
  -H 'Content-Type: text/plain' \
  -d "Sprint 14: Alice finished the auth refactor but is blocked on Bob's review. Carol flagged the Stripe sandbox timing out. Eve needs design review on the analytics dashboard by Friday."
```

### Run locally with the mock profile (no credentials needed)

```bash
git clone https://github.com/saumya25ch-rgb/sprintsense-backend.git
cd sprintsense-backend
./mvnw spring-boot:run
# defaults to spring.profiles.active=mock; serves canned responses on :8080
```

### Run locally with real LLM

You need a GitHub **classic** personal access token (fine-grained tokens have a known gap for Models inference):

1. Generate a classic PAT at <https://github.com/settings/tokens/new> (no specific scopes required)
2. Export it and start the app on the `github` profile:

```bash
export GITHUB_TOKEN='ghp_...'
./mvnw spring-boot:run -Dspring-boot.run.profiles=github
```

---

## Configuration

All configuration is environment-variable-driven. **No secrets are committed.**

| Variable | Purpose | Required? | Default |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `github` for live LLM, `mock` for canned responses | No | `mock` |
| `GITHUB_TOKEN` | Classic GitHub PAT for GitHub Models inference | Only with `github` profile | — |
| `GITHUB_MODEL` | Override the model id (e.g. `openai/gpt-4.1`) | No | `openai/gpt-4o-mini` |
| `SPRINTSENSE_CORS_ALLOWED_ORIGINS` | Comma-separated allowlist for browser origins | No | `http://localhost:5173` |
| `PORT` | Bind port (Render injects this) | No | `8080` |

### Production env on Render

```
SPRING_PROFILES_ACTIVE=github
GITHUB_TOKEN=ghp_...
SPRINTSENSE_CORS_ALLOWED_ORIGINS=https://sprintsense-ai.vercel.app,http://localhost:5173
```

---

## API

### `POST /api/ai/analyze`

**Request**
```
Content-Type: text/plain

Sprint 14 standup: Alice closed the auth refactor but is blocked on
Bob's review for two days. Carol flagged the Stripe sandbox keeps
timing out. Eve raised the analytics dashboard needs design review
by Friday.
```

**Response** (`200 OK`)
```json
{
  "summary": "The team made progress with Alice completing the auth refactor, but faced blockers that could impact the sprint's timeline.",
  "blockers": [
    "Alice is blocked on Bob's review for two days.",
    "The Stripe sandbox keeps timing out."
  ],
  "risks": [
    "If Bob does not review Alice's PR soon, it could delay QA and subsequent tasks.",
    "The analytics dashboard design review needs to be completed by Friday to avoid slipping."
  ],
  "recommendations": [
    "Bob should prioritize reviewing Alice's PR to unblock her work.",
    "Investigate the Stripe sandbox timeout issue."
  ],
  "actionItems": [
    {"task": "Review Alice's PR.", "owner": "Bob", "priority": "High"},
    {"task": "Investigate the Stripe sandbox timeout issue.", "owner": "Team", "priority": "Medium"},
    {"task": "Ensure the analytics dashboard design review is completed by Friday.", "owner": "Eve", "priority": "High"}
  ]
}
```

### `POST /api/ai/chat`

**Request**
```json
{
  "question": "Which blocker should the team tackle first and why?",
  "analysisData": { /* prior SprintAnalysisResponse */ }
}
```

**Response** — plain-text natural-language answer reasoning across the supplied analysis.

### `GET /api/health`

Returns `200 SprintSense Backend is up and running!`.

---

## Project structure

```
src/main/java/com/sprintsense/backend
├── BackendApplication.java
├── config/
│   └── WebConfig.java                  # CORS, env-driven allowlist
├── controller/
│   ├── AIController.java               # /api/ai/analyze, /api/ai/chat
│   └── HealthController.java           # /api/health
├── dto/
│   ├── ActionItem.java
│   ├── ChatRequest.java
│   └── SprintAnalysisResponse.java
└── service/
    ├── AnalysisService.java            # interface
    ├── MockAIService.java              # @Profile("mock"), offline-safe demo
    └── OpenAIAnalysisService.java      # @Profile("!mock"), real LLM via Spring AI

src/main/resources/
├── application.properties              # defaults; profile=mock
├── application-github.properties       # GitHub Models config
└── application-mock.properties         # excludes OpenAI auto-config for clean boot
```

---

## Tech stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.5.3
- **AI:** Spring AI 1.0.0 with the OpenAI-compatible starter, pointed at GitHub Models
- **Build:** Maven (wrapper included)
- **Container:** Multi-stage Dockerfile (JDK builder → JRE runtime, non-root user)
- **Hosting:** Render (Docker, free tier)
- **Frontend:** React + Vite on Vercel (separate repo)

---

## AI tools disclosure

Per the hackathon rules, the following AI tools were used during development:

- **Claude Code** (Anthropic's CLI) — used as a pair-programming assistant for Spring Boot scaffolding, Spring AI integration, profile configuration, deployment troubleshooting, and writing this README.
- **GitHub Copilot** — *(declare here if used by other team members)*

All final design choices, prompt engineering, profile structure, and integration code were authored and reviewed by the team. No code was committed without human review.

---

## Roadmap / Not yet implemented

These were intentionally deprioritized to keep the submission focused, and are listed transparently for the judges:

- **Persistence (MongoDB)** — currently the dependency is present; sprint-over-sprint trend analysis ("the same blocker has appeared in 4 sprints") is the natural next feature.
- **Microsoft Graph integration** — pulling Teams meeting transcripts directly, removing the copy-paste step.
- **Push to Microsoft Planner / Azure DevOps** — auto-create action items as work items so they actually get tracked.
- **Multi-agent decomposition** — separate analyzer / risk-scorer / action-extractor agents (would also align with the *Agent Swarms* theme).
- **Input validation, global exception handling, rate limiting** — hardening for non-demo traffic.

---

## Team

- **Saumya Chirania** — *(role)*
- *(add teammates here)*

---

## Submission compliance checklist

- [x] Built between 3 May 2026 and 30 June 2026
- [x] Public GitHub repo accessible to judges
- [x] Microsoft AI stack used (GitHub Models)
- [x] Live, judge-accessible URL
- [x] README with project description, setup, dependencies, team details
- [x] AI tools used during development disclosed
- [x] No secrets, credentials, or API keys committed
- [ ] *(team members and demo video to be added before final submission)*

---

## License

This project is released under the MIT License for hackathon submission. See `LICENSE` if/when added.
