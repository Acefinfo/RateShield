# Commands for the Ratelimit test

## Setup the projects

```powershell
mvn clean compile
```

```powershell
mvn test
```

```powershell
mvn exec:java -Dexec.mainClass="com.apigateway.gateway.ApiGateway"
```

--- 

#### 1\. Basic Request:

```powershell
$r = Invoke-WebRequest `
    -Uri "http://localhost:8080/" `
    -Headers @{"X-Client-ID"="demo-client"} `
    -UseBasicParsing

Write-Host "Status               : $($r.StatusCode)"
Write-Host "RateLimit-Limit      : $($r.Headers['X-ratelimit-limit'])"
Write-Host "RateLimit-Remaining  : $($r.Headers['X-ratelimit-remaining'])"
Write-Host "RateLimit-Reset      : $($r.Headers['X-ratelimit-reset'])"
```

---

#### 2\. Hit the rate limit

```powershell
1..11 | ForEach-Object {
    try {
        $r = Invoke-WebRequest `
            -Uri "http://localhost:8080/" `
            -Headers @{"X-Client-ID"="demo-client"} `
            -UseBasicParsing
        Write-Host "Request $_ : 200 OK" -ForegroundColor Green
    } catch {
        Write-Host "Request $_ : 429 Too Many Requests - BLOCKED" -ForegroundColor Red
    }
}
```

---

#### 3\. Show the blocked response Headers

```powershell
try {
    Invoke-WebRequest `
        -Uri "http://localhost:8080/" `
        -Headers @{"X-Client-ID"="demo-client"} `
        -UseBasicParsing
} catch {
    $response = $_.Exception.Response
    Write-Host "Status      : 429 Too Many Requests" -ForegroundColor Red
    Write-Host "Retry-After : $($response.Headers['Retry-After']) seconds"
}
```

---

#### 4\. Client isolation 

```powershell
# Drain client-A completely
Write-Host "--- Sending 11 requests as client-A ---" -ForegroundColor Yellow
1..11 | ForEach-Object {
    try {
        $r = Invoke-WebRequest `
            -Uri "http://localhost:8080/" `
            -Headers @{"X-Client-ID"="client-A"} `
            -UseBasicParsing
        Write-Host "client-A Request $_ : 200 OK" -ForegroundColor Green
    } catch {
        Write-Host "client-A Request $_ : 429 BLOCKED" -ForegroundColor Red
    }
}
```

```powershell
# Now show client-B is completely unaffected
Write-Host "`n--- Now sending as client-B ---" -ForegroundColor Yellow
try {
    $r = Invoke-WebRequest `
        -Uri "http://localhost:8080/" `
        -Headers @{"X-Client-ID"="client-B"} `
        -UseBasicParsing
    Write-Host "client-B : $($r.StatusCode) OK - completely independent!" -ForegroundColor Green
} catch {
    Write-Host "client-B : blocked" -ForegroundColor Red
}
```

---

#### 5\. Multiple client simultaneously


```powershell
# Simulate 3 different clients making requests
Write-Host "--- Multiple clients ---" -ForegroundColor Yellow

foreach ($client in @("mobile-app", "web-app", "api-service")) {
    try {
        $r = Invoke-WebRequest `
            -Uri "http://localhost:8080/" `
            -Headers @{"X-Client-ID"=$client} `
            -UseBasicParsing
        Write-Host "$client : $($r.StatusCode) OK   Remaining: $($r.Headers['X-ratelimit-remaining'])" -ForegroundColor Green
    } catch {
        Write-Host "$client : 429 BLOCKED" -ForegroundColor Red
    }
}
```

---

#### 6\. Live metrics


```powershell
$m = Invoke-RestMethod `
    -Uri "http://localhost:8080/metrics" `
    -UseBasicParsing

Write-Host "===== GATEWAY METRICS =====" -ForegroundColor Cyan
Write-Host "Total Requests   : $($m.totalRequests)"
Write-Host "Allowed          : $($m.allowedRequests)" -ForegroundColor Green
Write-Host "Blocked          : $($m.blockedRequests)" -ForegroundColor Red
Write-Host "Active Clients   : $($m.activeClients)"
Write-Host "Strategy         : $($m.currentStrategy)" -ForegroundColor Yellow
Write-Host "===========================" -ForegroundColor Cyan
```

---

#### 7\. Watch Metrics Update live


```powershell
# Send 5 requests then check metrics after each one
1..5 | ForEach-Object {
    try {
        Invoke-WebRequest `
            -Uri "http://localhost:8080/" `
            -Headers @{"X-Client-ID"="watcher"} `
            -UseBasicParsing | Out-Null
    } catch {}

    $m = Invoke-RestMethod -Uri "http://localhost:8080/metrics" -UseBasicParsing
    Write-Host "After request $_ → Total: $($m.totalRequests)  Allowed: $($m.allowedRequests)  Blocked: $($m.blockedRequests)"
}
```

---

## Changing to token bucket project

```powershell
mvn clean compile

mvn exec:java -Dexec.mainClass="com.apigateway.gateway.ApiGateway"
```


```java
RateLimitConfig config = new RateLimitConfig.Builder()
    .algorithmType("TOKEN_BUCKET")  // ← change this one line
    .tokenCapacity(5)
    .refillTokens(2)
    .refillDuration(Duration.ofSeconds(5))
    .build();
```


#### Drain the bucket

```powershell
1..6 | ForEach-Object {
    try {
        $r = Invoke-WebRequest `
            -Uri "http://localhost:8080/" `
            -Headers @{"X-Client-ID"="bucket-test"} `
            -UseBasicParsing
        Write-Host "Request $_ : 200 OK  Tokens left: $($r.Headers['X-ratelimit-remaining'])" -ForegroundColor Green
    } catch {
        Write-Host "Request $_ : 429 Bucket empty - BLOCKED" -ForegroundColor Red
    }
}
```













