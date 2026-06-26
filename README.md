# RateShield — Configurable API Gateway with Pluggable Rate Limiting

A lightweight, concurrent API Gateway built in Java that protects backend
services from excessive traffic using configurable rate-limiting algorithms.

---

## Architecture

```
Client Request
       │
       ▼
   ApiGateway          ← HTTP server, routes requests
       │
       ▼
 ClientIdentifier      ← extracts who is making the request
       │
       ▼
   RateLimiter         ← Strategy Context
       │
       ▼
 RateLimitStrategy     ← interface (Strategy Pattern)
       │
  ─────────────
  │           │
  ▼           ▼
Fixed      Token
Window     Bucket
```

---

## Design Patterns Used

### Strategy Pattern
The core of the project. `RateLimitStrategy` is an interface with two
implementations: `FixedWindowStrategy` and `TokenBucketStrategy`.
The gateway depends only on the interface — never on a concrete class.
New algorithms (Sliding Window, Leaky Bucket) can be added without
touching any existing code. This satisfies the Open/Closed Principle.

### Factory Pattern
`RateLimitStrategyFactory` creates the correct strategy at runtime
based on the `algorithmType` in `RateLimitConfig`. The gateway never
calls `new FixedWindowStrategy()` directly.

### Builder Pattern
`RateLimitConfig` uses a Builder because it has many optional fields.
Cleaner and safer than a 6-parameter constructor.

---

## Rate Limiting Algorithms

### Fixed Window
- Counts requests within a fixed time window (e.g. 100 req/min)
- Resets the counter when the window expires
- Simple and predictable
- Trade-off: allows up to 2x requests at window boundaries

```
Window 1 (0s-60s)    │ Window 2 (60s-120s)
[■][■][■]...[■] 100  │ counter resets → [■][■]...
```

### Token Bucket
- Bucket holds up to N tokens (capacity)
- Each request consumes 1 token
- Tokens refill at a fixed rate (e.g. 10 tokens/second)
- Supports burst traffic up to bucket capacity
- Trade-off: slightly more complex state management

```
Full bucket:  [■■■■■■■■■■] 100 tokens
After 3 req:  [■■■■■■■   ] 97 tokens
After refill: [■■■■■■■■■ ] +10 tokens
```

---

## Thread Safety Approach

| Technique | Where Used | Why |
|-----------|-----------|-----|
| `ConcurrentHashMap` | Client state storage | Thread-safe map, bucket-level locking |
| `AtomicInteger` | Request counter per client | Lock-free atomic increment |
| `AtomicLong` | Token count | Lock-free compare-and-set |
| `volatile` | Window start, last refill time | Visibility across threads |
| `synchronized(state)` | Window reset, token refill | Guards multi-step atomic operations |
| `LongAdder` | Metrics counters | Higher throughput than AtomicLong under contention |

### Why `synchronized(state)` and not `synchronized(this)`?
Locking on the individual client state object means:
- Thread A (client-1) and Thread B (client-2) run in parallel ✅
- Thread A and Thread C (both client-1) are serialized ✅
- No unnecessary blocking between different clients

---

## Project Structure

```
src/main/java/com/apigateway/
├── clock/          ClockProvider, SystemClockProvider
├── client/         ClientIdentifier, IpAddressIdentifier, HeaderClientIdentifier
├── core/           RequestContext, RateLimitResult, RateLimitConfig
├── exception/      GatewayException, RateLimitException
├── factory/        RateLimitStrategyFactory
├── gateway/        ApiGateway
├── cleanup/        CleanupScheduler
├── metrics/        MetricsService, MetricsSnapshot
├── ratelimit/      RateLimitStrategy (interface), RateLimiter
│   └── strategy/   FixedWindowStrategy, TokenBucketStrategy
└── repository/     ClientState, FixedWindowClientState,
                    TokenBucketClientState, ClientStateRepository,
                    ConcurrentHashMapClientStateRepository
```

---

## API Endpoints

### Gateway Endpoint
```
GET /
```
Returns `200 OK` with rate limit headers, or `429 Too Many Requests`.

**Response headers (allowed):**
```
X-RateLimit-Limit:     10
X-RateLimit-Remaining: 9
X-RateLimit-Reset:     1719999999000
```

**Response headers (blocked):**
```
Retry-After: 45
```

### Metrics Endpoint
```
GET /metrics
```
Returns JSON:
```json
{
  "totalRequests": 1000,
  "allowedRequests": 900,
  "blockedRequests": 100,
  "activeClients": 50,
  "currentStrategy": "FixedWindowStrategy"
}
```

---

## How to Run

### Prerequisites
- Java 17+
- Maven 3.6+

### Run the gateway
```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.apigateway.gateway.ApiGateway"
```

### Run all tests
```bash
mvn test
```

### Test manually
```bash
# Send a request
curl -i http://localhost:8080/

# Send with custom client ID
curl -i -H "X-Client-ID: mobile-app" http://localhost:8080/

# Check metrics
curl http://localhost:8080/metrics
```

---

## Switching Algorithms

In `ApiGateway.main()`, change one line:

```java
// Use Fixed Window
.algorithmType("FIXED_WINDOW")
.maxRequests(10)
.windowSize(Duration.ofMinutes(1))

// Use Token Bucket
.algorithmType("TOKEN_BUCKET")
.tokenCapacity(100)
.refillTokens(10)
.refillDuration(Duration.ofSeconds(1))
```

No other code changes needed. That is the Strategy Pattern in action.

---

## Adding a New Algorithm (Extension Point)

1. Create `SlidingWindowStrategy implements RateLimitStrategy`
2. Add `case "SLIDING_WINDOW"` in `RateLimitStrategyFactory`
3. Done — zero changes to `ApiGateway`, `RateLimiter`, or any existing class

---

## Testing

| Test Class | What it tests |
|-----------|--------------|
| `FixedWindowStrategyTest` | Within limit, limit reached, window reset, client isolation |
| `TokenBucketStrategyTest` | Token consumption, refill, capacity cap, client isolation |
| `MetricsServiceTest` | Counter accuracy, snapshot consistency, JSON output |
| `ConcurrencyTest` | 200 parallel threads, no race conditions, correct counts |
| `ApiGatewayIntegrationTest` | HTTP 200, HTTP 429, headers, metrics endpoint |

---

## Key Design Decisions & Trade-offs

| Decision | Why | Trade-off |
|---------|-----|----------|
| `com.sun.net.httpserver` | Zero dependencies, JDK built-in | Not production-grade (use Netty in prod) |
| `synchronized(state)` over lock-free | Simpler, correct, per-client granularity | Slight overhead vs fully lock-free |
| `LongAdder` for metrics | Higher throughput under contention | `sum()` not instantly consistent |
| Manual JSON in `MetricsSnapshot` | No external library needed | Not flexible for complex JSON |
| `ClockProvider` abstraction | Deterministic unit tests | Extra interface to maintain |

---



## OOP Concepts Demonstrated

| Concept | Where |
|---------|-------|
| Abstraction | `RateLimitStrategy`, `ClientIdentifier`, `ClientStateRepository`, `ClockProvider` |
| Encapsulation | All state classes with private fields and controlled access |
| Polymorphism | `FixedWindowStrategy` and `TokenBucketStrategy` via `RateLimitStrategy` |
| Interface-driven design | Gateway depends on interfaces, never concrete classes |
| Composition over inheritance | `HeaderClientIdentifier` composes `IpAddressIdentifier` as fallback |
| Dependency Injection | All dependencies passed via constructor, wired in `main()` |