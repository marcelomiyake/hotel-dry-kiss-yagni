# Stays · Hotel Reservation System

A hotel room reservation demo based on the [ByteByteGo Hotel Reservation System](https://bytebytego.com/courses/system-design-interview/hotel-reservation-system), with the visual flow handed off from OpenDesign.

## Screenshot

This is the production React frontend served from a local preview. The API-backed flows and Lighthouse audit remain unverified because `localhost:8080` already had an existing listener, which I left untouched.

![Stays hotel reservation search page](docs/screenshots/hotel-reservation-home.png)

## What it does

- Search Lisbon hotels by destination, dates, and guest count; compare live room availability and daily rates.
- Reserve by room type. PostgreSQL stores inventory per date, permits up to 10% overbooking, and uses version-checked updates to protect concurrent bookings.
- Reuse a reservation UUID as an idempotency key. Repeating the same request returns the existing reservation; changing its details returns a conflict.
- View reservation history by email and cancel a confirmed reservation. Cancellation releases inventory and records a simulated refund.
- Give staff a protected screen for editing room types, capacity, inventory, and nightly rates.

The seeded catalog has two Lisbon hotels. Payments are intentionally simulated: the demo does not collect card details or contact a hotel.

## Architecture

| Component | Responsibility |
| --- | --- |
| `hotel-service` | Hotel and room type catalog, including staff updates |
| `rate-service` | Per-night quotes, weekday/weekend rates, and staff rate schedules |
| `reservation-service` | Search, date inventory, bookings, idempotency, cancellation |
| `payment-service` | Idempotent demo charges and refunds |
| `service-common` | Shared API errors, health endpoint, and staff-key filter |
| React web app | Search, stay details, checkout, history, and staff flow |
| PostgreSQL | Catalog, rates, payments, and reservation inventory schemas |

The browser talks to the React app through Nginx. Nginx routes API calls to the four services. Each microservice has two Kind replicas; the web deployment also has two replicas. PostgreSQL uses one StatefulSet replica and a persistent volume for this local demo.

## Run on local Kind

Requirements: Docker, Kind, kubectl, and `openssl`.

```bash
./scripts/kind-up.sh
```

Open [http://localhost:8080](http://localhost:8080). The script creates the `hotel-reservation` cluster, builds and loads local images, and deploys the app. It generates local database and staff keys in `.kind-db-password` and `.kind-admin-key`; the staff screen uses the value from `.kind-admin-key`. Those files are ignored by Git.

```bash
./scripts/kind-down.sh
```

To inspect the deployment:

```bash
kubectl -n hotel-reservation get deployments,pods,services
```

## Main API routes

| Route | Purpose |
| --- | --- |
| `GET /api/search?destination=Lisbon&checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD&guests=2` | Search availability and rates |
| `GET /api/hotels?destination=Lisbon` | Read hotel catalog |
| `GET /api/rates/quote?roomTypeId=…&checkIn=…&checkOut=…` | Quote every night in a stay |
| `POST /api/reservations` | Create or replay a reservation |
| `GET /api/reservations?email=…` | Read a guest’s reservation history |
| `DELETE /api/reservations/{id}` | Cancel a reservation and refund the demo payment |
| `POST /api/admin/hotels/{hotelId}/room-types` | Add a room type (requires `X-Admin-Key`) |
| `PUT /api/admin/room-types/{id}` | Update a room type (requires `X-Admin-Key`) |
| `PUT /api/admin/inventory` | Update future inventory (requires `X-Admin-Key`) |
| `PUT /api/admin/rates` | Update one nightly rate (requires `X-Admin-Key`) |

The local API is for a design exercise, not a production payment or hotel booking service.

## Quality checks

```bash
# Frontend checks
cd frontend
npm ci
npm test
npm run build
npm run lint

# Java 25 + PostgreSQL integration tests (Docker socket required)
cd ..
docker run --rm --network=host \
  -v "$PWD":/workspace \
  -v "$HOME/.m2":/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -w /workspace maven:3.9-eclipse-temurin-25 mvn -B verify
```

Coverage is reported by Vitest and JaCoCo. The verified frontend line coverage is **91.17%** and combined Java line coverage is **83.80%**. SonarCloud results and the local visual QA status are recorded in the analysis below.

SonarQube Cloud project: [Hotel Reservation System](https://sonarcloud.io/project/overview?id=marcelomiyake_hotel-dry-kiss-yagni). Project configuration is in `sonar-project.properties`.

## Analysis

### Prompt

> Implement https://bytebytego.com/courses/system-design-interview/hotel-reservation-system in Java 25, React 19.3 (handoff from OpenDesign [To start it, from ~/.local/share/open-design, run ./node_modules/.bin/tools-dev start. Web UI: http://127.0.0.1:41919, self-hosted]), PostgreSQL, and Kubernetes via local Kind (2 replicas for each microservice). Use SonarQube Cloud via Chrome (https://sonarcloud.io/organizations/marcelomiyake/) to create and manage these monorepo projects, and complete this job with zero SonarQube issues and test coverage above 80%. If you need to run the scanner from the command line, I updated ~/.zshrc with the SONAR_TOKEN, but you can also use GitHub Actions and push commits in a loop until the issues are clean; if you generate another SONAR_KEY, update it in the GitHub project or in .zshrc. The frontend should have a perfect Lighthouse grade and good SEO META in 1 Click. Finally, update the README.md with a screenshot and an analysis that includes this prompt, the harness used here (Codex, GPT-6 Luna with max effort), and the token costs from the sessions to complete this task (input tokens, cache tokens, reasoning tokens, output tokens) and LOC. The cache and sessions were empty just before starting this session. Consult the OpenAI official documentation for token prices to estimate total costs. This implementation must follow DRY, KISS, and YAGNI in the backend and frontend.

### Build record

| Measure | Result |
| --- | --- |
| Harness | Codex · GPT-6 Luna · maximum effort |
| Tests | 13 frontend tests and 19 Java tests passed |
| Frontend coverage | 91.17% lines (Vitest) |
| Backend coverage | 83.80% combined Java lines (JaCoCo) |
| SonarCloud | 0 open issues; quality gate passed; 85.2% overall coverage and 88.3% new-code coverage; one nonblocking missing-blame warning for uncommitted files |
| Local Kind deployment | Not started: localhost:8080 already had a listener |
| Screenshot | Captured from the production React build in a local preview (`docs/screenshots/hotel-reservation-home.png`) |
| Lighthouse | Not run: local deployment stopped because port 8080 was occupied |
| Production LOC | 3,814 nonblank lines across 74 source/config files |
| Test LOC | 759 nonblank lines across 11 test files |

Token counters are from the final Codex thread usage record for this task; the session and cache started empty per the prompt. Cached input is a subset of input, and reasoning is included in output, so neither is double-counted in the estimate. The estimate uses the official [OpenAI Codex token-based rate card](https://help.openai.com/en/articles/20001415-chatgpt-rate-card-enterprise-token-based-pricing): $0.10 / 1M uncached input tokens, $0.01 / 1M cached input tokens, and $0.50 / 1M output tokens.

| Token measure | Count |
| --- | ---: |
| Input tokens | 55,861,035 |
| Cached input tokens | 54,655,104 |
| Reasoning tokens (included in output) | 196,332 |
| Output tokens | 347,069 |
| Estimated model-token cost | $0.84 |
| Estimated web-search feature cost | $0.15 for 15 runs, if billed at the listed rate |
| Estimated total | **$0.99** |

The model-token estimate is `(input − cached input) × $0.10/M + cached input × $0.01/M + output × $0.50/M`. The input count includes cached input, and reasoning tokens are part of output; neither is double-counted. The web-search estimate applies the listed $10 per 1,000 runs to 15 web-tool calls. These estimates use the official rate card and are not an invoice; actual billing depends on the workspace agreement.
