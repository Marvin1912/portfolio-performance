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

# Portfolio Performance Build and Docker Integration Guide

## Overview

This document describes both the official Portfolio Performance release process and how to build custom local Docker images with your own changes. The integration allows developers to test modifications before creating pull requests while maintaining compatibility with the existing Docker workflow.

## Official Release Process

Portfolio Performance follows a structured release process using Maven with Tycho for building Eclipse RCP applications. The release process includes automated builds, multi-platform packaging, code signing, and distribution through GitHub releases and Eclipse p2 update sites.

### Build System

#### Core Configuration
- **Build Tool**: Maven with Tycho plugin
- **Java Version**: Java 21 required
- **Main POM**: `portfolio-app/pom.xml`
- **Current Version**: 0.80.4-SNAPSHOT
- **Target Platform**: Supports Linux (x86_64/aarch64), macOS (x86_64/aarch64), Windows (x86_64/aarch64)

#### Build Profiles
- **Default**: Full build with tests, coverage, and style checking
- **Local Development**: `-Plocal-dev` - Skips tests and coverage for faster builds
- **Distribution**: `-Ppackage-distro` - Creates release packages with signing

#### Build Commands
```bash
# Full build with tests
mvn -f portfolio-app/pom.xml clean verify -Plocal-dev

# Build specific modules
mvn -f portfolio-app/pom.xml clean compile -Plocal-dev -pl :portfolio-target-definition,:name.abuchen.portfolio -am -amd

# Run tests
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o -pl :name.abuchen.portfolio.tests -am -amd
```

### CI/CD Pipeline

#### GitHub Actions Workflow
- **Location**: `.github/workflows/main.yml`
- **Triggers**: Push to master branch, pull requests
- **Build Jobs**:
  - `build`: Standard Maven build for PRs
  - `build-analyze`: Maven build + SonarCloud analysis for master branch

#### Build Artifacts
- **Linux**: `.tar.gz` files (x86_64/aarch64)
- **Windows**: `.zip` files + NSIS installer (x86_64/aarch64)
- **macOS**: `.dmg` files (x86_64/aarch64)

### Release Scripts

#### Location
All release scripts are located in `portfolio-app/releng/`:

#### 1. publish-updatesite.sh
- **Purpose**: Publishes Eclipse p2 update site to GitHub Pages
- **Domain**: `updates.portfolio-performance.info`
- **Process**:
  - Copies artifacts from `portfolio-product/target/repository/`
  - Creates CNAME file for custom domain
  - Commits to GitHub Pages branch

#### 2. publish-to-github.sh
- **Purpose**: Creates GitHub releases and uploads binaries
- **Repository**: `buchen/portfolio`
- **Uploads**:
  - Windows: Setup executable, ZIP files
  - Linux: Tar.gz files
  - macOS: DMG files
  - GPG signatures for all artifacts

#### 3. create-release-notes.sh
- **Purpose**: Generates release notes from metainfo
- **Source**: `portfolio-product/metainfo/info.portfolio_performance.PortfolioPerformance.metainfo.xml`
- **Output**: Plain text and HTML formats
- **Tool**: Java-based `portfolio-releng` utility

#### 4. archive-binaries.sh
- **Purpose**: Archives release binaries locally
- **Storage**: `Archive/[year]/[version]/` directory structure
- **Includes**: Original and GPG-signed artifacts

#### 5. codesign-macos-product.sh
- **Purpose**: Signs macOS applications
- **Features**:
  - Apple Developer ID signing
  - Timestamp embedding
  - Deep signing with runtime entitlements
  - Automatic notarization submission

### Version Management

#### Version Format
- **Pattern**: Semantic versioning (major.minor.patch)
- **Development**: `0.XX.Y-SNAPSHOT`
- **Release**: `0.XX.Y`
- **Eclipse Format**: `${unqualifiedVersion}.${buildQualifier}`

#### Release Versioning Process
1. **Development**: Work on `0.XX.Y-SNAPSHOT` branch
2. **Preparation**: Update version to `0.XX.Y` (remove -SNAPSHOT)
3. **Build**: Execute Maven build with packaging profile
4. **Release**: Create GitHub release and publish update site
5. **Post-Release**: Update version back to -SNAPSHOT

### Translation Management

#### Multi-Language Support
- **Languages**: 17 languages including English, German, Spanish, Dutch, Portuguese, French, Italian, Czech, Russian, Slovak, Polish, Chinese, Danish, Turkish, Vietnamese, Catalan
- **Management**: POEditor.com integration
- **Script**: `poeditor.sh` for synchronization
- **Process**: Download latest translations before each release

#### Translation Workflow
1. Developers update translation keys in code
2. Script uploads new terms to POEditor
3. Translators work via POEditor web interface
4. Script downloads completed translations before release

### Security and Signing

#### Code Signing
- **Windows**: Code signing with digital certificate
- **macOS**: Apple Developer ID signing + notarization
- **GPG**: All artifacts signed with GPG for integrity verification

#### Security Features
- **OAuth 2.0**: Secure authentication with PKCE
- **Token Management**: Secure storage and automatic refresh
- **Local Callback Server**: Prevents redirect URI attacks

## Local Docker Build Integration

### Overview

The local Docker build process allows developers to test custom changes by building Portfolio Performance from source and creating Docker images with the custom artifacts instead of downloading pre-built releases from GitHub.

### Building Local tar.gz File

#### 1. Source Code Preparation
```bash
# Clone the repository
git clone https://github.com/buchen/portfolio.git
cd portfolio

# Create a feature branch for your changes
git checkout -b feature/my-custom-changes

# Make your code changes here...
# Edit OAuth flow, UI components, business logic, etc.
```

#### 2. Local Maven Build
```bash
# Build the application using the local development profile
mvn -f portfolio-app/pom.xml clean verify -Plocal-dev

# The built tar.gz will be located at:
# name.abuchen.portfolio/target/portfolio-VERSION-linux.gtk.x86_64.tar.gz
```

#### 3. Build Artifact Location
After successful build, locate your custom tar.gz:
```
name.abuchen.portfolio/target/portfolio-<VERSION>-linux.gtk.x86_64.tar.gz
```

### Docker Integration Methods

#### Method 1: Direct Copy Approach
Modify the existing Dockerfile to use local artifacts:

```dockerfile
# Replace the URL download section:
# ENV ARCHIVE=https://github.com/buchen/portfolio/releases/download/${VERSION}/PortfolioPerformance-${VERSION}-${ARCHITECTURE}.tar.gz
# RUN cd /opt && wget ${ARCHIVE} && tar xvzf PortfolioPerformance-${VERSION}-${ARCHITECTURE}.tar.gz

# With local copy:
COPY ./portfolio-<VERSION>-linux.gtk.x86_64.tar.gz /tmp/
RUN cd /opt && tar xzf /tmp/portfolio-<VERSION>-linux.gtk.x86_64.tar.gz && \
    rm /tmp/portfolio-<VERSION>-linux.gtk.x86_64.tar.gz
```

#### Method 2: Build Argument Approach
Create a flexible Dockerfile that supports both local and release builds:

```dockerfile
# Add build argument for local file
ARG LOCAL_TAR_GZ_PATH=""

# Conditional build logic
RUN if [ -n "$LOCAL_TAR_GZ_PATH" ]; then \
    cd /opt && tar xzf ${LOCAL_TAR_GZ_PATH} && \
    rm ${LOCAL_TAR_GZ_PATH}; \
    else \
    cd /opt && wget ${ARCHIVE} && tar xvzf PortfolioPerformance-${VERSION}-${ARCHITECTURE}.tar.gz && \
    rm PortfolioPerformance-${VERSION}-${ARCHITECTURE}.tar.gz; \
    fi
```

#### Method 3: Multi-Stage Build
Create a comprehensive local development Dockerfile:

```dockerfile
# Stage 1: Build stage
FROM debian:12-slim AS builder
RUN apt-get update && apt-get install -y maven openjdk-21-jdk git
WORKDIR /build
COPY ./portfolio /build/portfolio/
WORKDIR /build/portfolio/portfolio-app
RUN mvn clean verify -Plocal-dev -pl :name.abuchen.portfolio -am -amd
RUN mkdir -p /output && tar -xzf name.abuchen.portfolio/target/portfolio-*-linux.gtk.x86_64.tar.gz -C /output

# Stage 2: Runtime stage (based on original Dockerfile)
FROM jlesage/baseimage-gui:debian-12-v4
COPY --from=builder /output /opt/portfolio
# ... continue with the rest of the original Dockerfile
```

### Local Docker Build Workflow

#### Step 1: Build Custom Application
```bash
# Navigate to Portfolio Performance source directory
cd /path/to/portfolio

# Make your code changes
# Edit OAuth flow, UI components, etc.

# Build the application
mvn -f portfolio-app/pom.xml clean verify -Plocal-dev
```

#### Step 2: Prepare Docker Context
```bash
# Navigate to Docker directory
cd /path/to/portfolio-performance-image

# Copy your built artifact to Docker context
cp ../portfolio/name.abuchen.portfolio/target/portfolio-*-linux.gtk.x86_64.tar.gz ./
```

#### Step 3: Build Docker Image
```bash
# Using direct copy method
docker build \
  --build-arg VERSION=local-dev \
  --build-arg TARGETARCH=amd64 \
  --build-arg PACKAGING=none \
  -t portfolio-performance:custom .

# Using build argument method
docker build \
  --build-arg VERSION=local-dev \
  --build-arg LOCAL_TAR_GZ_PATH=/tmp/portfolio-local.tar.gz \
  -v $(pwd)/portfolio-*-linux.gtk.x86_64.tar.gz:/tmp/portfolio-local.tar.gz:ro \
  -t portfolio-performance:custom .
```

### Docker Compose for Local Development

Create a development-specific docker-compose configuration:

```yaml
version: "3"
services:
  portfolio-performance-dev:
    build:
      context: .
      dockerfile: Dockerfile.local
      args:
        VERSION: local-dev
        PACKAGING: firefox
    container_name: portfolio-dev
    volumes:
      - ./config:/config
      - ./workspace:/opt/portfolio/workspace
    environment:
      USER_ID: 1000
      GROUP_ID: 1000
      DISPLAY_WIDTH: 1920
      DISPLAY_HEIGHT: 1080
    ports:
      - "5800:5800"
```

### Benefits of Local Docker Integration

#### Development Advantages
- **Full Control**: Test any custom changes before creating PRs
- **Faster Iteration**: No need to wait for GitHub releases
- **OAuth Testing**: Perfect for testing authentication flow changes
- **UI Development**: Immediate feedback on interface modifications
- **Business Logic**: Verify algorithm changes in containerized environment

#### Technical Benefits
- **Version Control**: Use local version strings like `local-dev` or `feature-branch-name`
- **Consistency**: Same runtime environment as production Docker images
- **Isolation**: Clean testing environment without local system dependencies
- **Portability**: Share custom builds with team members

### Integration with Existing Workflow

#### Compatibility
- **Existing Dockerfile**: Remains unchanged for production builds
- **Docker Compose**: Original configuration continues to work with released versions
- **Build Scripts**: Official release process is not affected
- **CI/CD Pipeline**: GitHub Actions workflow remains functional

#### Development Process
1. **Make Changes**: Edit Portfolio Performance source code
2. **Local Build**: Use Maven to build custom application
3. **Docker Integration**: Create local Docker image with custom artifacts
4. **Testing**: Verify changes in containerized environment
5. **Pull Request**: Submit changes to official repository after testing

### Security Considerations

#### Local Development Security
- **Source Code**: Ensure no sensitive data is committed to local branches
- **Build Artifacts**: Clean up local tar.gz files after testing
- **Docker Images**: Use appropriate tagging to distinguish local from official images
- **Network Access**: Local builds may have different network requirements

#### Distribution Security
- **Local Images**: Do not push local development images to public registries
- **Version Tagging**: Use clear versioning to avoid confusion with official releases
- **Code Review**: Ensure all changes are properly reviewed before submission

### Troubleshooting

#### Common Issues
- **Build Failures**: Check Java version (requires Java 21)
- **Missing Dependencies**: Verify Maven dependencies are available
- **Architecture Mismatch**: Ensure Dockerfile ARCHITECTURE matches build target
- **Permission Errors**: Check file permissions on copied artifacts

#### Debugging Steps
1. Verify Maven build completion: `ls name.abuchen.portfolio/target/`
2. Check tar.gz contents: `tar -tzf portfolio-*-linux.gtk.x86_64.tar.gz`
3. Validate Docker build: `docker build --no-cache -t test .`
4. Test container: `docker run -p 5800:5800 portfolio-performance:custom`

This comprehensive integration guide enables developers to seamlessly test custom Portfolio Performance changes in Docker containers while maintaining compatibility with the official release workflow and Docker ecosystem.