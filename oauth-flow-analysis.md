# Portfolio Performance OAuth Flow Analysis

## Overview

This document analyzes the OAuth authentication flow in Portfolio Performance, specifically focusing on the callback URL handling and token extraction mechanism.

## Authentication Flow

### Entry Point
- **UI Location**: "Einstellungen und API-Schlüssel" (Settings and API Keys)
- **Component**: `LoginButton.java`
- **File**: `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/util/LoginButton.java:101`

### Core Components

#### 1. OAuthClient (Main Controller)
**File**: `name.abuchen.portfolio/src/name/abuchen/portfolio/oauth/OAuthClient.java`

**Key Methods**:
- `signIn(Consumer<String> browser)` - Initiates OAuth flow
- `handleSignInCallback()` - Processes callback response
- `getAccessToken()` - Retrieves access tokens

#### 2. CallbackServer (Local HTTP Server)
**File**: `name.abuchen.portfolio/src/name/abuchen/portfolio/oauth/impl/CallbackServer.java`

**Configuration**:
- **Ports Tried**: 49968 → 55968 → 59968 (sequential)
- **Success Endpoint**: `/success`
- **Callback URL Format**: `http://localhost:PORT/success`

**Key Methods**:
- `getSuccessEndpoint()` - Returns complete callback URL (line 139)
- `start()` - Starts local HTTP server
- Sets up `SuccessHttpHandler` to process callbacks

#### 3. AuthorizationCode (Parameter Parser)
**File**: `name.abuchen.portfolio/src/name/abuchen/portfolio/oauth/impl/AuthorizationCode.java`

**Token Extraction Logic** (lines 47-56):
```java
public static AuthorizationCode parse(String query)
{
    var parameters = Stream.of(query.split("&"))
                    .filter(s -> !s.isEmpty())
                    .map(kv -> kv.split("=", 2))
                    .filter(s -> s.length == 2)
                    .collect(Collectors.toMap(x -> x[0].toLowerCase(), x -> x[1]));

    return new AuthorizationCode(parameters.get("state"), parameters.get("code"));
}
```

## Callback URL Example

The actual callback URL captured during authentication:
```
http://localhost:49968/success?code=aSBHtNgv_a8AJla92nJSRFry62prw9tVi10si9wblAD&state=c1b3ec8e-d58d-4f66-ab8f-ccb9d1b863df&iss=https%3A%2F%2Faccounts.portfolio-performance.info%2Foidc
```

**URL Breakdown**:
- **Host**: `localhost:49968` (first port in the sequence)
- **Endpoint**: `/success` (matches `ENDPOINT_SUCCESS` constant)
- **Parameters**:
  - `code`: Authorization code for token exchange
  - `state`: CSRF protection parameter
  - `iss`: Identity provider URL (URL-decoded: `https://accounts.portfolio-performance.info/oidc`)

## OAuth Flow Sequence

1. **Initiation**: User clicks login in "Einstellungen und API-Schlüssel"
2. **Callback Server Start**: Local HTTP server starts on localhost (tries ports 49968, 55968, 59968)
3. **Authorization URL**: OAuthClient creates auth URL with `redirect_uri=http://localhost:PORT/success`
4. **Browser Launch**: System browser opens with OAuth authorization URL
5. **User Authentication**: User authenticates with identity provider
6. **Callback Redirect**: Identity provider redirects to callback URL with authorization code
7. **Parameter Extraction**: `AuthorizationCode.parse()` extracts `code` and `state` from query parameters
8. **Token Exchange**: OAuthClient exchanges authorization code for access token using PKCE
9. **Token Storage**: Access token stored for API calls
10. **Server Shutdown**: Local callback server stops

## Security Features

- **PKCE (Proof Key for Code Exchange)**: SHA-256 code challenge for public client security
- **State Parameter**: Prevents CSRF attacks
- **Local Callback Server**: Ensures redirect URI is controlled by application
- **Bearer Token Authentication**: JWT tokens used in API requests

## Key File Locations

- **OAuth Controller**: `name.abuchen.portfolio/src/name/abuchen/portfolio/oauth/OAuthClient.java`
- **Callback Server**: `name.abuchen.portfolio/src/name/abuchen/portfolio/oauth/impl/CallbackServer.java`
- **Parameter Parser**: `name.abuchen.portfolio/src/name/abuchen/portfolio/oauth/impl/AuthorizationCode.java`
- **UI Component**: `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/util/LoginButton.java`

## Token Extraction Point

The critical token extraction occurs in `AuthorizationCode.parse()` method at:
**File**: `name.abuchen.portfolio/src/name/abuchen/portfolio/oauth/impl/AuthorizationCode.java:47-56`

This method parses the callback URL query parameters to extract the authorization `code` that is subsequently exchanged for an access token.

## Token Validity Periods

### Authorization Code Validity
- **Duration**: 5-10 minutes (industry standard for OAuth 2.0)
- **Purpose**: Single-use code for token exchange
- **Security**: Short lifespan prevents code replay attacks
- **Consumption**: Immediately exchanged upon receipt (within milliseconds)

**Note**: The authorization code validity is determined by the identity provider (`https://accounts.portfolio-performance.info/oidc`) and is not configurable within Portfolio Performance.

### Access Token Validity
- **Duration**: Determined by identity provider (typically 1 hour)
- **Calculation**: `expiresAt = System.currentTimeMillis() + expiresIn * 1000`
- **Storage**: Stored in `AccessToken` object with expiration timestamp
- **Automatic Refresh**: Uses refresh token when access token expires

### Token Validation Logic
```java
// TokenStorage.java:105-106
var isExpired = token.getExpiresAt() < System.currentTimeMillis();
return isExpired ? Optional.empty() : Optional.of(token);
```

## Verification

The captured callback URL perfectly matches the implemented flow:
- Port 49968 is the first port tried by CallbackServer
- `/success` endpoint matches the `ENDPOINT_SUCCESS` constant
- Parameter structure matches the OAuth 2.0 authorization code flow
- Issuer URL confirms Portfolio Performance's identity provider
- Authorization code is immediately exchanged for access token within the same request cycle