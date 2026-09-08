$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8081'
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function SafeRequest($method, $uri, $bodyJson, $headers, $webSession) {
    try {
        if ($bodyJson) {
            $r = Invoke-WebRequest -Uri $uri -Method $method -Body $bodyJson -ContentType 'application/json' -Headers $headers -WebSession $webSession -UseBasicParsing -ErrorAction Stop
        } else {
            $r = Invoke-WebRequest -Uri $uri -Method $method -Headers $headers -WebSession $webSession -UseBasicParsing -ErrorAction Stop
        }
        return @{ status = $r.StatusCode; content = $r.Content; response = $r }
    } catch {
        $resp = $_.Exception.Response
        if ($resp) {
            $status = $resp.StatusCode.value__
            $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
            $body = $reader.ReadToEnd()
            return @{ status = $status; content = $body; response = $null }
        } else {
            return @{ status = 'NO_RESPONSE'; content = $_.Exception.Message; response = $null }
        }
    }
}

Write-Host 'STEP:1 LOGIN'
$loginBody = @{ email = 'testpatient@example.com'; password = 'Test1234' } | ConvertTo-Json
$login = SafeRequest 'POST' "$base/api/v1/auth/login" $loginBody $null $session
Write-Host "LOGIN_STATUS:$($login.status)"
Write-Host "LOGIN_BODY:$($login.content)"

$cookies = $session.Cookies.GetCookies($base) | ForEach-Object { "COOKIE:$($_.Name)=$($_.Value); Path=$($_.Path); HttpOnly=$($_.HttpOnly); Secure=$($_.Secure)" }
$cookies | ForEach-Object { Write-Host $_ }

$accessToken = $null
try {
    $j = $login.content | ConvertFrom-Json
    if ($j -and $j.data -and $j.data.accessToken) { $accessToken = $j.data.accessToken }
} catch { $accessToken = $null }

if ($accessToken) { Write-Host 'EXTRACTED_ACCESS_TOKEN:yes' } else { Write-Host 'EXTRACTED_ACCESS_TOKEN:no' }

Write-Host 'STEP:2 ME'
if ($accessToken) {
    $hdrs = @{ Authorization = "Bearer $accessToken" }
    $me = SafeRequest 'GET' "$base/api/v1/auth/me" $null $hdrs $session
    Write-Host "ME_STATUS:$($me.status)"
    Write-Host "ME_BODY:$($me.content)"
} else {
    Write-Host 'ME_STATUS:SKIPPED_NO_TOKEN'
}

Write-Host 'STEP:3 REFRESH'
$beforeRefreshCookie = ($session.Cookies.GetCookies($base) | Where-Object { $_.Name -eq 'physioconnect_refresh_token' })
$beforeVal = $null
if ($beforeRefreshCookie) { $beforeVal = $beforeRefreshCookie.Value; Write-Host "BEFORE_REFRESH_COOKIE:$beforeVal" } else { Write-Host 'BEFORE_REFRESH_COOKIE:missing' }

$refresh = SafeRequest 'POST' "$base/api/v1/auth/refresh-token" $null $null $session
Write-Host "REFRESH_STATUS:$($refresh.status)"
Write-Host "REFRESH_BODY:$($refresh.content)"

$afterRefreshCookie = ($session.Cookies.GetCookies($base) | Where-Object { $_.Name -eq 'physioconnect_refresh_token' })
$afterVal = $null
if ($afterRefreshCookie) { $afterVal = $afterRefreshCookie.Value; Write-Host "AFTER_REFRESH_COOKIE:$afterVal" } else { Write-Host 'AFTER_REFRESH_COOKIE:missing' }

if ($beforeVal -and $afterVal) {
    if ($beforeVal -ne $afterVal) { Write-Host 'REFRESH_ROTATED:YES' } else { Write-Host 'REFRESH_ROTATED:NO' }
} else { Write-Host 'REFRESH_ROTATED:UNKNOWN' }

Write-Host 'STEP:4 REUSE_OLD_REFRESH'
if ($beforeVal) {
    $sessionOld = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $cookie = New-Object System.Net.Cookie('physioconnect_refresh_token',$beforeVal,'/api/v1/auth','localhost')
    $sessionOld.Cookies.Add($cookie)
    $reuse = SafeRequest 'POST' "$base/api/v1/auth/refresh-token" $null $null $sessionOld
    Write-Host "REUSE_OLD_REFRESH_STATUS:$($reuse.status)"
    Write-Host "REUSE_OLD_REFRESH_BODY:$($reuse.content)"
} else {
    Write-Host 'REUSE_OLD_REFRESH_STATUS:SKIPPED_NO_OLD_COOKIE'
}

Write-Host 'STEP:5 LOGOUT'
$hdrs = @{ Authorization = "Bearer $accessToken" }
$logout = SafeRequest 'POST' "$base/api/v1/auth/logout" $null $hdrs $session
Write-Host "LOGOUT_STATUS:$($logout.status)"
Write-Host "LOGOUT_BODY:$($logout.content)"

$postLogoutCookie = ($session.Cookies.GetCookies($base) | Where-Object { $_.Name -eq 'physioconnect_refresh_token' })
if ($postLogoutCookie) { Write-Host "POST_LOGOUT_COOKIE:$($postLogoutCookie.Value)" } else { Write-Host 'POST_LOGOUT_COOKIE:missing' }

Write-Host 'STEP:6 ACCESS_AFTER_LOGOUT'
$accessAfter = SafeRequest 'GET' "$base/api/v1/auth/me" $null @{ Authorization = "Bearer $accessToken" } $session
Write-Host "ACCESS_AFTER_LOGOUT_STATUS:$($accessAfter.status)"
Write-Host "ACCESS_AFTER_LOGOUT_BODY:$($accessAfter.content)"

Write-Host 'STEP:7 REFRESH_AFTER_LOGOUT'
$trySession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$cookieVal = $null
if ($postLogoutCookie) { $cookieVal = $postLogoutCookie.Value } elseif ($afterVal) { $cookieVal = $afterVal } elseif ($beforeVal) { $cookieVal = $beforeVal }
if ($cookieVal) {
    $cookie = New-Object System.Net.Cookie('physioconnect_refresh_token',$cookieVal,'/api/v1/auth','localhost')
    $trySession.Cookies.Add($cookie)
    $refreshAfterLogout = SafeRequest 'POST' "$base/api/v1/auth/refresh-token" $null $null $trySession
    Write-Host "REFRESH_AFTER_LOGOUT_STATUS:$($refreshAfterLogout.status)"
    Write-Host "REFRESH_AFTER_LOGOUT_BODY:$($refreshAfterLogout.content)"
} else { Write-Host 'REFRESH_AFTER_LOGOUT_STATUS:SKIPPED_NO_COOKIE' }

Write-Host 'STEP:8 INVALID_LOGIN'
$badBody = @{ email = 'testpatient@example.com'; password = 'definitely-wrong-password' } | ConvertTo-Json
$bad = SafeRequest 'POST' "$base/api/v1/auth/login" $badBody $null $session
Write-Host "INVALID_LOGIN_STATUS:$($bad.status)"
Write-Host "INVALID_LOGIN_BODY:$($bad.content)"

Write-Host 'DONE' 
