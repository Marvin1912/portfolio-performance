# Portfolio Performance OAuth Flow Analysis

## Overview

Analysis of OAuth authentication flow in Portfolio Performance, focusing on callback URL handling and token extraction.

## Authentication Flow

### Entry Point
- **UI**: "Einstellungen und API-Schlüssel" → `LoginButton.java:101`

### Core Components

#### OAuthClient (Main Controller)
**File**: `name.abuchen.portfolio/src/name/abuchen/portfolio/oauth/OAuthClient.java`
- `signIn()` - Initiates OAuth flow
- `handleSignInCallback()` - Processes callback response
- `getAccessToken()` - Retrieves access tokens

#### CallbackServer (Local HTTP Server)
**File**: `name.abuchen.portfolio/src/name/abuchen/portfolio/oauth/impl/CallbackServer.java`
- **Ports**: 49968 → 55968 → 59968 (sequential)
- **Endpoint**: `/success`
- **URL Format**: `http://localhost:PORT/success`

#### AuthorizationCode (Parameter Parser)
**File**: `name.abuchen.portfolio/src/name/abuchen/portfolio/oauth/impl/AuthorizationCode.java:47-56`
```java
public static AuthorizationCode parse(String query) {
    var parameters = Stream.of(query.split("&"))
                    .filter(s -> !s.isEmpty())
                    .map(kv -> kv.split("=", 2))
                    .filter(s -> s.length == 2)
                    .collect(Collectors.toMap(x -> x[0].toLowerCase(), x -> x[1]));
    return new AuthorizationCode(parameters.get("state"), parameters.get("code"));
}
```

## OAuth Flow Sequence

1. User clicks login → CallbackServer starts on localhost
2. OAuthClient creates auth URL with `redirect_uri=http://localhost:PORT/success`
3. Browser opens with OAuth authorization URL
4. User authenticates with identity provider
5. Identity provider redirects to callback URL with authorization code
6. `AuthorizationCode.parse()` extracts `code` and `state` from query parameters
7. OAuthClient exchanges authorization code for access token using PKCE
8. Access token stored, callback server stops

## Security Features

- **PKCE**: SHA-256 code challenge for public client security
- **State Parameter**: CSRF protection
- **Local Callback Server**: Controls redirect URI
- **Bearer Token Authentication**: JWT tokens for API requests

## Token Validity

- **Authorization Code**: 5-10 minutes (single-use, determined by identity provider)
- **Access Token**: Typically 1 hour (set by identity provider)
- **Refresh**: Automatic token refresh using refresh token

## Verification

Example callback URL: `http://localhost:49968/success?code=...&state=...&iss=https://accounts.portfolio-performance.info/oidc`
- Matches implemented flow perfectly
- Authorization code exchanged immediately within same request cycle

# Portfolio Performance Build and Docker Integration Guide

## Overview

Build Portfolio Performance locally and create custom Docker images for testing changes before creating pull requests.

## Build System

- **Tool**: Maven with Tycho (Java 21 required)
- **Main POM**: `portfolio-app/pom.xml`
- **Target**: Linux/macOS/Windows (x86_64/aarch64)

### Build Commands
```bash
# Full build (local development)
mvn -f portfolio-app/pom.xml clean verify -Plocal-dev

# Build core module only
mvn -f portfolio-app/pom.xml clean compile -Plocal-dev -pl :portfolio-target-definition,:name.abuchen.portfolio -am -amd

# Run tests
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o -pl :name.abuchen.portfolio.tests -am -amd
```

## Release Process Summary

- **CI/CD**: GitHub Actions (`.github/workflows/main.yml`)
- **Artifacts**: Linux (.tar.gz), Windows (.zip + installer), macOS (.dmg)
- **Scripts**: Located in `portfolio-app/releng/` (publish, sign, archive)
- **Translations**: 17 languages via POEditor.com
- **Security**: Code signing (Windows/macOS), GPG signatures, OAuth 2.0 with PKCE

## Local Docker Build Integration

### Build Local Artifact

```bash
# Make code changes
git checkout -b feature/my-changes

# Build locally
mvn -f portfolio-app/pom.xml clean verify -Plocal-dev

# Artifact location:
# name.abuchen.portfolio/target/portfolio-VERSION-linux.gtk.x86_64.tar.gz
```

### Docker Integration Methods

#### Method 1: Direct Copy
```dockerfile
COPY ./portfolio-VERSION-linux.gtk.x86_64.tar.gz /tmp/
RUN cd /opt && tar xzf /tmp/portfolio-VERSION-linux.gtk.x86_64.tar.gz && \
    rm /tmp/portfolio-VERSION-linux.gtk.x86_64.tar.gz
```

#### Method 2: Build Argument
```dockerfile
ARG LOCAL_TAR_GZ_PATH=""
RUN if [ -n "$LOCAL_TAR_GZ_PATH" ]; then \
    cd /opt && tar xzf ${LOCAL_TAR_GZ_PATH} && rm ${LOCAL_TAR_GZ_PATH}; \
    else \
    cd /opt && wget ${ARCHIVE} && tar xvzf PortfolioPerformance-${VERSION}-${ARCHITECTURE}.tar.gz; \
    fi
```

### Build Custom Docker Image

```bash
# Copy artifact to Docker context
cp ../portfolio/name.abuchen.portfolio/target/portfolio-*-linux.gtk.x86_64.tar.gz ./

# Build image
docker build \
  --build-arg VERSION=local-dev \
  --build-arg TARGETARCH=amd64 \
  -t portfolio-performance:custom .
```

### Docker Compose for Development

```yaml
version: "3"
services:
  portfolio-performance-dev:
    build:
      context: .
      args:
        VERSION: local-dev
    volumes:
      - ./config:/config
    ports:
      - "5800:5800"
```

### Benefits

- Test custom changes before creating PRs
- Faster iteration than waiting for releases
- Same environment as production Docker images
- Isolated testing environment

### Troubleshooting

- **Java Version**: Requires Java 21
- **Build Artifacts**: Check `name.abuchen.portfolio/target/`
- **Permissions**: Verify file permissions on copied artifacts