# SprintSense Backend

> AI-powered sprint retrospective analysis. Paste a messy sprint-meeting transcript, get back a prioritized list of blockers, risks, recommendations, and assigned action items — so no idea gets lost and no task falls through the cracks.

**Live demo:** https://sprintsense-ai.vercel.app — paste a transcript and try it in the browser
**Live API:** https://sprintsense-backend-2.onrender.com
**Frontend repo:** https://github.com/saumya25ch-rgb/sprintsense-ai

---

## Hackathon context

**Theme:** *AI at Work — Productivity & Teamwork Reimagined.*

Sprint retros are where teams accumulate the most actionable signal — and where most of it is lost the moment the meeting ends. SprintSense ingests the raw transcript and produces a structured, judgement-ready summary the team can act on in seconds.

**Microsoft AI stack components**

| Component | Role |
| --- | --- |
| **GitHub Models** (Microsoft-hosted) | LLM inference for `/analyze` and `/chat`, backed by Azure OpenAI infrastructure. |
| **Spring AI** (Azure-aligned) | `ChatClient` abstraction; structured-output binding from LLM JSON into typed Java DTOs. |
| **GitHub** | Public source hosting, secret scanning, submission surface. |

---

## What it does

- **`POST /api/ai/analyze`** — accepts a transcript (plain text), returns structured JSON: `summary`, `blockers`, `risks`, `recommendations`, and `actionItems` (each with `task`, `owner` extracted from the transcript, and `priority` of High/Medium/Low).
- **`POST /api/ai/chat`** — answers follow-up questions reasoning over a prior analysis (e.g. *"Which blocker is most urgent and why?"*). No retrieval, no keyword matching — the LLM reasons over the structured JSON as context.
- **`GET /api/health`** — liveness probe.

See the live API for full request/response examples.

---

## Architecture

```
+----------------------------+
| React frontend on Vercel   |  https://sprintsense-ai.vercel.app
+-------------+--------------+
              | HTTPS, CORS allowlisted
              v
+----------------------------+
| Spring Boot 3.5 on Render  |  https://sprintsense-backend-2.onrender.com
|  - github profile (real)   |  + Spring AI ChatClient
|  - mock profile (offline)  |  + structured-output -> DTO
+-------------+--------------+
              | Authorization: Bearer $GITHUB_TOKEN
              v
+----------------------------+
| GitHub Models              |  openai/gpt-4o-mini
| (MS AI stack)              |  on Azure OpenAI infra
+----------------------------+
```

The `mock` profile is the default: serves canned responses with no network calls, so the app boots without credentials. `SPRING_PROFILES_ACTIVE=github` plus `GITHUB_TOKEN` flips to live LLM mode without code changes.

---

## Engineering highlights

- **Structured-output binding, not regex parsing.** Spring AI's `ChatClient` binds LLM JSON directly to typed Java DTOs; malformed responses fail cleanly instead of producing half-parsed output.
- **Graceful degradation.** Mock profile is the default — judges can boot and inspect the API surface without provisioning a token.
- **Production-hardened request path.** `@Valid`/`@NotBlank` inputs, `@RestControllerAdvice` global handler that strips stack traces, per-IP rate limit (`30 req/min` on `/api/ai/*`, reads `X-Forwarded-For` so it works behind Render's load balancer). `NonTransientAiException` maps to `502 Bad Gateway`.
- **Zero-trust deploy.** No secrets in source control; credentials via Render env vars only. Classic-PAT requirement documented — fine-grained PATs have a known gap for Models inference.

---

## Quick start

**Run locally with the mock profile** (no credentials needed):
```bash
git clone https://github.com/saumya25ch-rgb/sprintsense-backend.git
cd sprintsense-backend
./mvnw spring-boot:run          # serves canned responses on :8080
```

**Run with live LLM:** generate a GitHub *classic* PAT at <https://github.com/settings/tokens/new> (no specific scopes required), then:
```bash
export GITHUB_TOKEN='ghp_...'
./mvnw spring-boot:run -Dspring-boot.run.profiles=github
```

**Hit the deployed API:**
```bash
curl -X POST https://sprintsense-backend-2.onrender.com/api/ai/analyze \
  -H 'Content-Type: text/plain' \
  -d "Sprint 14: Alice finished the auth refactor but is blocked on Bob's review."
```

---

## Configuration

All config is env-var driven. **No secrets are committed.**

| Variable | Purpose | Required | Default |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `github` (live LLM) or `mock` (canned) | No | `mock` |
| `GITHUB_TOKEN` | Classic GitHub PAT for Models inference | With `github` profile | — |
| `GITHUB_MODEL` | Model id override | No | `openai/gpt-4o-mini` |
| `SPRINTSENSE_CORS_ALLOWED_ORIGINS` | Comma-separated browser origin allowlist | No | `http://localhost:5173` |
| `SPRINTSENSE_RATELIMIT_REQUESTS_PER_MINUTE` | Per-IP rate limit on `/api/ai/*` | No | `30` |
| `PORT` | Bind port (Render injects this) | No | `8080` |

---

## Tech stack / dependencies

- **Java 17**, **Spring Boot 3.5.3**
- **Spring AI 1.0.0** with the OpenAI-compatible starter, pointed at GitHub Models
- **Maven** (wrapper included)
- **Multi-stage Dockerfile** — JDK builder → JRE runtime, non-root user
- **Render** (Docker, free tier) for backend; **Vercel** (React + Vite) for frontend

---

## AI tools used

- **Claude Code** (Anthropic's CLI) — pair-programming assistant for Spring Boot scaffolding, Spring AI integration, profile configuration, deployment troubleshooting, hardening (validation, exception handling, rate limiting), and this README.

All final design choices, prompt engineering, profile structure, and integration code were authored and reviewed by me. No code was committed without human review.

---

## Roadmap

Intentionally deprioritized for this submission:

- **Persistence (MongoDB)** — dependency is wired; sprint-over-sprint trend analysis ("this blocker has appeared in 4 sprints") is the natural next step.
- **Microsoft Graph integration** — pull Teams transcripts directly, removing the copy-paste step.
- **Microsoft Planner / Azure DevOps push** — auto-create action items as tracked work items.

---

## Team

| Name | Role |
| --- | --- |
| **Saumya Chirania** | Solo build — backend (Spring Boot, Spring AI, GitHub Models integration, hardening), frontend (React + Vite), deployment (Render + Vercel), and documentation |

---

## License

All Rights Reserved. See [`LICENSE`](./LICENSE). Source is published publicly only to satisfy the hackathon's submission rules; reuse, redistribution, modification, or commercial exploitation without prior written permission is not permitted.
