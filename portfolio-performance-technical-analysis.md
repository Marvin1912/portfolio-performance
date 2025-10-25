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

# Portfolio Performance Release Process

## Overview

Portfolio Performance follows a structured release process using Maven with Tycho for building Eclipse RCP applications. The release process includes automated builds, multi-platform packaging, code signing, and distribution through GitHub releases and Eclipse p2 update sites.

## Build System

### Core Configuration
- **Build Tool**: Maven with Tycho plugin
- **Java Version**: Java 21 required
- **Main POM**: `portfolio-app/pom.xml`
- **Current Version**: 0.80.4-SNAPSHOT
- **Target Platform**: Supports Linux (x86_64/aarch64), macOS (x86_64/aarch64), Windows (x86_64/aarch64)

### Build Profiles
- **Default**: Full build with tests, coverage, and style checking
- **Local Development**: `-Plocal-dev` - Skips tests and coverage for faster builds
- **Distribution**: `-Ppackage-distro` - Creates release packages with signing

### Build Commands
```bash
# Full build with tests
mvn -f portfolio-app/pom.xml clean verify -Plocal-dev

# Build specific modules
mvn -f portfolio-app/pom.xml clean compile -Plocal-dev -pl :portfolio-target-definition,:name.abuchen.portfolio -am -amd

# Run tests
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o -pl :name.abuchen.portfolio.tests -am -amd
```

## CI/CD Pipeline

### GitHub Actions Workflow
- **Location**: `.github/workflows/main.yml`
- **Triggers**: Push to master branch, pull requests
- **Build Jobs**:
  - `build`: Standard Maven build for PRs
  - `build-analyze`: Maven build + SonarCloud analysis for master branch

### Build Artifacts
- **Linux**: `.tar.gz` files (x86_64/aarch64)
- **Windows**: `.zip` files + NSIS installer (x86_64/aarch64)
- **macOS**: `.dmg` files (x86_64/aarch64)

## Release Scripts

### Location
All release scripts are located in `portfolio-app/releng/`:

### 1. publish-updatesite.sh
- **Purpose**: Publishes Eclipse p2 update site to GitHub Pages
- **Domain**: `updates.portfolio-performance.info`
- **Process**:
  - Copies artifacts from `portfolio-product/target/repository/`
  - Creates CNAME file for custom domain
  - Commits to GitHub Pages branch

### 2. publish-to-github.sh
- **Purpose**: Creates GitHub releases and uploads binaries
- **Repository**: `buchen/portfolio`
- **Uploads**:
  - Windows: Setup executable, ZIP files
  - Linux: Tar.gz files
  - macOS: DMG files
  - GPG signatures for all artifacts

### 3. create-release-notes.sh
- **Purpose**: Generates release notes from metainfo
- **Source**: `portfolio-product/metainfo/info.portfolio_performance.PortfolioPerformance.metainfo.xml`
- **Output**: Plain text and HTML formats
- **Tool**: Java-based `portfolio-releng` utility

### 4. archive-binaries.sh
- **Purpose**: Archives release binaries locally
- **Storage**: `Archive/[year]/[version]/` directory structure
- **Includes**: Original and GPG-signed artifacts

### 5. codesign-macos-product.sh
- **Purpose**: Signs macOS applications
- **Features**:
  - Apple Developer ID signing
  - Timestamp embedding
  - Deep signing with runtime entitlements
  - Automatic notarization submission

## Version Management

### Version Format
- **Pattern**: Semantic versioning (major.minor.patch)
- **Development**: `0.XX.Y-SNAPSHOT`
- **Release**: `0.XX.Y`
- **Eclipse Format**: `${unqualifiedVersion}.${buildQualifier}`

### Release Versioning Process
1. **Development**: Work on `0.XX.Y-SNAPSHOT` branch
2. **Preparation**: Update version to `0.XX.Y` (remove -SNAPSHOT)
3. **Build**: Execute Maven build with packaging profile
4. **Release**: Create GitHub release and publish update site
5. **Post-Release**: Update version back to -SNAPSHOT

## Translation Management

### Multi-Language Support
- **Languages**: 17 languages including English, German, Spanish, Dutch, Portuguese, French, Italian, Czech, Russian, Slovak, Polish, Chinese, Danish, Turkish, Vietnamese, Catalan
- **Management**: POEditor.com integration
- **Script**: `poeditor.sh` for synchronization
- **Process**: Download latest translations before each release

### Translation Workflow
1. Developers update translation keys in code
2. Script uploads new terms to POEditor
3. Translators work via POEditor web interface
4. Script downloads completed translations before release

## Security and Signing

### Code Signing
- **Windows**: Code signing with digital certificate
- **macOS**: Apple Developer ID signing + notarization
- **GPG**: All artifacts signed with GPG for integrity verification

### Security Features
- **OAuth 2.0**: Secure authentication with PKCE
- **Token Management**: Secure storage and automatic refresh
- **Local Callback Server**: Prevents redirect URI attacks

## Release Process Steps

### Pre-Release Preparation
1. Update version number (remove -SNAPSHOT)
2. Download latest translations via POEditor
3. Generate release notes from commit history
4. Ensure all tests pass on all platforms

### Build and Package
1. Execute Maven build with `-Ppackage-distro` profile
2. Build triggers code signing and notarization
3. Generate platform-specific installers
4. Create GPG signatures for all artifacts

### Distribution
1. Create GitHub release with `publish-to-github.sh`
2. Upload binaries and signatures to GitHub
3. Update update site with `publish-updatesite.sh`
4. Archive binaries locally with `archive-binaries.sh`

### Post-Release
1. Update version back to -SNAPSHOT
2. Archive release artifacts
3. Update documentation and changelogs
4. Monitor update site statistics

## Quality Assurance

### Testing
- **Unit Tests**: Core business logic tests
- **UI Tests**: SWTBot-based user interface tests
- **Integration Tests**: End-to-end workflow testing
- **Platform Testing**: Cross-platform compatibility verification

### Code Quality
- **SonarCloud**: Automated code analysis
- **Coverage Reports**: Test coverage tracking
- **Style Checking**: Code style enforcement
- **Security Scanning**: Dependency vulnerability checks

## Distribution Channels

### Primary Channels
- **GitHub Releases**: Main distribution platform
- **Eclipse p2 Update Site**: Automatic updates within application
- **Website**: Download links and documentation

### Update Mechanism
- **Auto-Update**: Built-in Eclipse p2 update mechanism
- **Update Site**: `updates.portfolio-performance.info`
- **Statistics**: Update request tracking via `api.portfolio-performance.info/stats/update`

This comprehensive release process ensures consistent, secure, and well-documented releases across all supported platforms while maintaining high quality standards and providing seamless update experiences for users.