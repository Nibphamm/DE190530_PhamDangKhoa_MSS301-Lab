param([switch]$TestTokenExpiry)
$ErrorActionPreference = 'Stop'
$results = [System.Collections.Generic.List[object]]::new()
$gateway = 'http://localhost:9000'
$realm = 'http://localhost:8181/realms/spring-microservices-realm'

function Request($Method, $Url, $Body = $null, $Token = $null, $ContentType = 'application/json') {
    $parameters = @{ Uri = $Url; Method = $Method; UseBasicParsing = $true }
    if ($Body) { $parameters.Body = $Body; $parameters.ContentType = $ContentType }
    if ($Token) { $parameters.Headers = @{ Authorization = "Bearer $Token" } }
    try {
        $response = Invoke-WebRequest @parameters
        $bodyText = $response.Content
        if ($bodyText -is [byte[]]) { $bodyText = [Text.Encoding]::UTF8.GetString($bodyText) }
        return @{ Status = [int]$response.StatusCode; Body = $bodyText; Headers = $response.Headers }
    } catch {
        if (-not $_.Exception.Response) { throw }
        $response = $_.Exception.Response
        $reader = [IO.StreamReader]::new($response.GetResponseStream())
        try { $bodyText = $reader.ReadToEnd() } finally { $reader.Dispose() }
        return @{ Status = [int]$response.StatusCode; Body = $bodyText; Headers = $response.Headers }
    }
}

function Check($Name, [bool]$Passed) {
    $results.Add(@{ Test = $Name; Passed = $Passed })
    if (-not $Passed) { throw "FAIL: $Name" }
    Write-Host "PASS: $Name"
}

function OrderCount {
    $count = docker exec -e MYSQL_PWD=mysql slot7-mysql mysql -uroot -N -e 'SELECT COUNT(*) FROM order_service.t_orders;'
    if ($LASTEXITCODE -ne 0) { throw 'Cannot query order database' }
    return [int]$count
}

function DecodePayload($Token) {
    $part = $Token.Split('.')[1].Replace('-', '+').Replace('_', '/')
    $part = $part.PadRight($part.Length + ((4 - $part.Length % 4) % 4), '=')
    return [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($part)) | ConvertFrom-Json
}

try {
    $discovery = Invoke-RestMethod "$realm/.well-known/openid-configuration"
    Check 'T6.3 discovery issuer and endpoints' ($discovery.issuer -eq $realm -and
        $discovery.token_endpoint -eq "$realm/protocol/openid-connect/token" -and
        $discovery.jwks_uri -eq "$realm/protocol/openid-connect/certs")
    $tokenResponse = Invoke-RestMethod -Method Post -Uri $discovery.token_endpoint -Body @{
        grant_type = 'client_credentials'; client_id = 'spring-microservices-client';
        client_secret = 'mss301-dev-secret-change-me'
    }
    $token = $tokenResponse.access_token
    $payload = DecodePayload $token
    Check 'T6.4 client credentials token and 300 second lifespan' ($tokenResponse.expires_in -eq 300 -and
        $tokenResponse.token_type -eq 'Bearer' -and $payload.iss -eq $realm -and
        $payload.azp -eq 'spring-microservices-client' -and $payload.exp - $payload.iat -eq 300)
    $r = Request Post $discovery.token_endpoint @{
        grant_type = 'client_credentials'; client_id = 'spring-microservices-client'; client_secret = 'wrong-secret'
    } $null 'application/x-www-form-urlencoded'
    Check 'T6.6 wrong client secret rejected' ($r.Status -eq 401)
    $r = Request Get "$gateway/actuator/health"
    Check 'T7.6 public health endpoint' ($r.Status -eq 200 -and ($r.Body | ConvertFrom-Json).status -eq 'UP')
    $r = Request Get "$gateway/api/products"
    Check 'T7.1 missing bearer token rejected' ($r.Status -eq 401 -and $r.Headers['WWW-Authenticate'] -like 'Bearer*')
    $r = Request Get "$gateway/api/products" $null 'abc.def.ghi'
    Check 'T7.2 malformed token rejected' ($r.Status -eq 401 -and $r.Headers['WWW-Authenticate'] -like '*invalid_token*')
    $parts = $token.Split('.')
    $tamperedPayload = ([Text.Encoding]::UTF8.GetString([Convert]::FromBase64String(
        $parts[1].Replace('-', '+').Replace('_', '/').PadRight($parts[1].Length + ((4 - $parts[1].Length % 4) % 4), '='))))
    $tamperedPayload = $tamperedPayload.Replace('spring-microservices-client', 'spring-microservices-hacker')
    $parts[1] = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($tamperedPayload)).TrimEnd('=').Replace('+', '-').Replace('/', '_')
    $r = Request Get "$gateway/api/products" $null ($parts -join '.')
    Check 'T7.3 altered signed payload rejected' ($r.Status -eq 401)
    $before = OrderCount
    $r = Request Post "$gateway/api/order" '{"skuCode":"iphone_15","price":1000,"quantity":1}'
    Check 'T7.5 unauthenticated order not persisted' ($r.Status -eq 401 -and (OrderCount) -eq $before)
    foreach ($case in @(@{ Quantity = 100; Expected = 'true' }, @{ Quantity = 101; Expected = 'false' })) {
        $r = Request Get "http://localhost:8082/api/inventory?skuCode=iphone_15&quantity=$($case.Quantity)"
        Check "T2 inventory quantity $($case.Quantity)" ($r.Status -eq 200 -and $r.Body -eq $case.Expected)
    }
    $r = Request Get 'http://localhost:8082/api/inventory?skuCode=unknown_sku&quantity=1'
    Check 'T2.3 unknown inventory sku' ($r.Status -eq 200 -and $r.Body -eq 'false')
    $before = OrderCount
    $r = Request Post 'http://localhost:8081/api/order' '{"skuCode":"iphone_15","price":1000,"quantity":100}'
    Check 'T3.1 direct order quantity 100 persisted' ($r.Status -eq 201 -and (OrderCount) -eq $before + 1)
    $before = OrderCount
    $r = Request Post 'http://localhost:8081/api/order' '{"skuCode":"iphone_15","price":1000,"quantity":101}'
    Check 'T3.3 direct out of stock not persisted' ($r.Status -eq 500 -and (OrderCount) -eq $before)
    $r = Request Post 'http://localhost:8081/api/order' '{"skuCode":"nokia_3310","price":50,"quantity":1}'
    Check 'T3.4 unknown sku order not persisted' ($r.Status -eq 500 -and (OrderCount) -eq $before)
    $r = Request Post "$gateway/api/products" '{"name":"iPhone 15","description":"Apple smartphone","price":1000}' $token
    Check 'T5.2 authenticated product creation' ($r.Status -eq 201 -and ($r.Body | ConvertFrom-Json).id)
    $r = Request Get "$gateway/api/products" $null $token
    $direct = Request Get 'http://localhost:8080/api/products'
    Check 'T8.1 authenticated product routing matches direct service' ($r.Status -eq 200 -and $r.Body -eq $direct.Body)
    $r = Request Get "$gateway/api/inventory?skuCode=iphone_15&quantity=1" $null $token
    Check 'T8.2 inventory routing preserves query' ($r.Status -eq 200 -and $r.Body -eq 'true')
    $before = OrderCount
    $r = Request Post "$gateway/api/order" '{"skuCode":"iphone_15","price":1000,"quantity":1}' $token
    Check 'T8.3 end to end authenticated order persisted' ($r.Status -eq 201 -and
        $r.Body -eq 'Order Placed Successfully' -and (OrderCount) -eq $before + 1)
    $before = OrderCount
    $r = Request Post "$gateway/api/order" '{"skuCode":"iphone_15","price":1000,"quantity":101}' $token
    Check 'T8.4 authenticated out of stock not persisted' ($r.Status -eq 500 -and (OrderCount) -eq $before)
    $r = Request Get "$gateway/api/khong-co-route" $null $token
    Check 'T5.5 authenticated unknown route returns 404' ($r.Status -eq 404)
    $otherToken = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8181/realms/spring-microservices-realm/protocol/openid-connect/token' -Body @{
        grant_type = 'client_credentials'; client_id = 'spring-microservices-client'; client_secret = 'mss301-dev-secret-change-me'
    }
    $r = Request Get "$gateway/api/products" $null $otherToken.access_token
    Check 'T7.7 different issuer rejected' ($r.Status -eq 401)
    if ($TestTokenExpiry) {
        Write-Host 'Waiting for original token expiry, including decoder clock skew...'
        $expireAt = [DateTimeOffset]::FromUnixTimeSeconds([long]$payload.exp).AddSeconds(65)
        while ([DateTimeOffset]::UtcNow -lt $expireAt) { Start-Sleep -Seconds 1 }
        $r = Request Get "$gateway/api/products" $null $token
        Check 'T7.4 expired token rejected' ($r.Status -eq 401 -and $r.Headers['WWW-Authenticate'] -like '*invalid_token*')
    }
} finally {
    $results | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $PSScriptRoot '../verification-results.json')
}
