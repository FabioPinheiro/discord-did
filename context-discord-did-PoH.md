# Discord DID Bot - System Context

> **Purpose**:
This document provides comprehensive system context for the Discord Proof of Humanity Verification Bot from the **Verifier's perspective**.
It defines the bot's role, responsibilities, flows, and technical architecture.

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [Actors](#actors)
4. [Communication Protocols](#communication-protocols)
5. [Verification Flows](#verification-flows)
6. [Application Responsibilities](#application-responsibilities)
7. [Technology Stack](#technology-stack)
8. [Out of Scope](#out-of-scope)

---

## System Overview

### What is the Discord PoH Verification Bot?

The **Discord PoH Verification Bot** is a Discord bot that acts as a **W3C Verifiable Credential Verifier** to verify users' Proof of Humanity credentials and grant Discord roles based on successful verification.

### Role in the Ecosystem

```
┌─────────────────────────────────────────────┐
│       LaceID Wallet (Holder DID)            │
│  - Stores PoH credential                    │
│  - Receives presentation request            │
│  - Presents credential to verifier          │
└──────────────────┬──────────────────────────┘
                   │
                   │ DIDComm / Deep Link
                   │ (credential presentation)
                   │
┌──────────────────▼──────────────────────────┐
│    Discord PoH Verification Bot             │
│         (Verifier DID)                      │
│  - Requests PoH credential presentation     │
│  - Verifies credential using Identus SDK    │
│  - Validates signature, issuer, expiration  │
│  - Grants Discord role on success           │
│  - Stores verification record               │
└──────────────────┬──────────────────────────┘
                   │
                   │ Discord API
                   │
┌──────────────────▼──────────────────────────┐
│           Discord Server                    │
│  - Hosts verified users                     │
│  - Manages roles                            │
│  - Displays bot commands                    │
└─────────────────────────────────────────────┘
```

**Key Points**:
- The bot is a **Verifier** in the W3C VC ecosystem
- It does NOT issue credentials (that's the KYC Issuer Agent's job)
- It verifies credentials issued by trusted issuers
- It grants Discord roles based on verified credentials

---

## Architecture

### High-Level Architecture

```
┌────────────────────────────────────────────────────────┐
│              Discord PoH Verification Bot              │
│                                                        │
│  ┌────────────────────────────────────────────────┐    │
│  │           Discord Bot Handler                  │    │
│  │  - Slash commands (/verify)                    │    │
│  │  - Message handlers                            │    │
│  │  - Role management                             │    │
│  └─────────────┬──────────────────────────────────┘    │
│                │                                       │
│  ┌─────────────▼──────────────────────────────────┐    │
│  │      Verification Orchestrator                 │    │
│  │  - Manages verification flows                  │    │
│  │  - Generates presentation requests             │    │
│  │  - Handles responses                           │    │
│  └─────────────┬──────────────────────────────────┘    │
│                │                                       │
│  ┌─────────────▼──────────────────────────────────┐    │
│  │      VC Verification Service                   │    │
│  │  - Verifies W3C VCs using Identus SDK          │    │
│  │  - Validates signatures                        │    │
│  │  - Checks issuer DID                           │    │
│  │  - Validates expiration                        │    │
│  └─────────────┬──────────────────────────────────┘    │
│                │                                       │
│  ┌─────────────▼──────────────────────────────────┐    │
│  │      Verification Records Store                │    │
│  │  - Discord User ID → Holder DID                │    │
│  │  - Verification timestamp                      │    │
│  │  - In-memory (PoC) or database (production)    │    │
│  └────────────────────────────────────────────────┘    │
│                                                        │
└────────────────────────────────────────────────────────┘
         │                           │
         │ Discord API               │ DIDComm (optional)
         ▼                           ▼
  Discord Server              LaceID Wallet
```

### Components

1. **Discord Bot Handler**: Handles Discord interactions (slash commands, messages)
2. **Verification Orchestrator**: Manages the verification flows (QR/deep link or DIDComm)
3. **VC Verification Service**: Uses Identus SDK to verify W3C Verifiable Credentials
4. **Verification Records Store**: Stores verification records (in-memory for PoC)

---

## Actors

### 1. Discord User (Holder)

**Identity**: Has a LaceID wallet with a **Holder DID** and PoH credential

**Role**: Wants to verify their Proof of Humanity to gain access to Discord server features

**Capabilities**:
- Initiates verification via `/verify` command or QR code
- Opens LaceID wallet when prompted
- Presents PoH credential to the bot

### 2. Discord PoH Verification Bot (Verifier)

**Identity**: Has a **Verifier DID** (managed by Identus SDK)

**Role**: Verifies PoH credentials and grants Discord roles

**Capabilities**:
- Generates presentation requests (QR/deep link or DIDComm)
- Verifies W3C VCs using Identus SDK
- Validates credential signatures, issuer, and expiration
- Grants Discord roles on successful verification
- Stores verification records

### 3. Discord Server

**Role**: Platform hosting the bot and managing user roles

**Capabilities**:
- Hosts the bot
- Displays slash commands
- Manages user roles

### 4. Trusted Issuers (e.g., KYC Issuer Agent)

**Identity**: Have **Issuer DIDs**

**Role**: Issue PoH credentials (external to this bot)

**Note**: The bot is configured with a list of trusted issuer DIDs. It only accepts credentials from these issuers.

---

## Communication Protocols

### 1. Discord API

**Purpose**: Bot interacts with Discord server and users

**Key Operations**:
- Register slash commands (`/verify`)
- Send messages to users (DMs)
- Assign roles to users
- Handle user interactions

**Library**: Discord4S (or similar Scala Discord library)

### 2. DIDComm v2 (Optional Flow)

**Purpose**: Secure, authenticated communication between bot and wallet

**Usage**: For `/verify` slash command flow (if using DIDComm)

**Message Types**:
- `https://didcomm.org/present-proof/3.0/request-presentation`
- `https://didcomm.org/present-proof/3.0/presentation`

**Note**: For PoC, may use simpler deep link flow instead of DIDComm

### 3. Deep Link / QR Code Flow (Alternative to DIDComm)

**Purpose**: Simpler verification flow using deep links

**Protocol**: `laceid://verify?request=<base64-encoded-presentation-request>`

**Flow**:
1. Bot generates presentation request
2. Bot creates QR code or deep link
3. User scans QR or clicks link
4. LaceID wallet opens and presents credential
5. Bot receives credential via callback URL

---

## Verification Flows

### Flow 1: QR Code / Deep Link Verification

**Trigger**: User scans QR code or clicks verification link

**Steps**:

1. **User initiates**: User scans QR code or clicks verification link in Discord
2. **Bot generates request**: Bot creates a presentation request for PoH credential
3. **QR/Deep link created**: Bot generates QR code or deep link (`laceid://verify?request=...`)
4. **User scans/clicks**: User scans QR or clicks link
5. **Wallet opens**: LaceID wallet opens with presentation request
6. **User presents**: User approves and presents PoH credential
7. **Bot receives credential**: Bot receives credential via callback URL
8. **Bot verifies**: Bot verifies credential using Identus SDK
   - Check signature validity
   - Verify issuer DID is trusted
   - Check credential type is `ProofOfHumanity`
   - Validate expiration date
9. **Bot grants role**: On success, bot grants Discord role
10. **Bot stores record**: Bot stores verification record (Discord User ID → Holder DID)
11. **User notified**: Bot sends DM to user confirming verification

**Success Criteria**:
- QR/deep link generated
- Wallet opens with correct request
- Credential verified successfully
- Role granted
- User notified

### Flow 2: Slash Command `/verify` Verification

**Trigger**: User types `/verify` in Discord

**Steps**:

1. **User initiates**: User types `/verify` command in Discord
2. **Bot acknowledges**: Bot responds with "Initiating verification..."
3. **Bot generates DIDComm request** (if using DIDComm):
   - Creates `request-presentation` message
   - Includes challenge/nonce for replay protection
   - Specifies PoH credential type
4. **Bot sends request**:
   - **DIDComm flow**: Bot sends DIDComm message to user's wallet (requires user to have shared their DID)
   - **Fallback**: Bot sends deep link in DM
5. **User presents**: User opens wallet and presents credential
6. **Bot receives presentation**: Bot receives credential presentation
7. **Bot verifies**: Same verification as Flow 1
8. **Bot grants role**: On success, bot grants Discord role
9. **Bot stores record**: Bot stores verification record
10. **User notified**: Bot updates slash command response with success message

**Success Criteria**:
- Slash command registered
- Presentation request sent
- Credential received and verified
- Role granted
- User notified

**Note**: For PoC, this flow may use deep link instead of DIDComm for simplicity.

---

## Application Responsibilities

### What the Discord Bot DOES

1. **Registers Discord slash commands** (`/verify`)
2. **Generates presentation requests** for PoH credentials
3. **Creates QR codes or deep links** for wallet integration
4. **Receives credential presentations** from LaceID wallets
5. **Verifies W3C VCs** using Identus SDK:
   - Signature validation
   - Issuer DID validation (against trusted issuer list)
   - Credential type validation (`ProofOfHumanity`)
   - Expiration date validation
6. **Grants Discord roles** on successful verification
7. **Stores verification records** (Discord User ID → Holder DID, timestamp)
8. **Sends notifications** to users (DMs)

### What the Discord Bot DOESN'T Do

1. **Does NOT issue credentials** (that's the KYC Issuer Agent's job)
2. **Does NOT perform KYC verification** (relies on credentials issued by trusted issuers)
3. **Does NOT manage user wallets** (users manage their own LaceID wallets)
4. **Does NOT store sensitive PII** (only stores verification records, not credential data)
5. **Does NOT revoke credentials** (credential issuers handle revocation)
6. **Does NOT support multi-server** (PoC is single-server only)

---

## Technology Stack

### Core Technologies

- **Language**: Scala 3
- **DID/VC Library**: Identus SDK (formerly Atala PRISM SDK)
- **Discord Library**: Discord4S or Ackcord (Scala Discord libraries)
- **HTTP Server**: http4s (for webhook callbacks)
- **JSON Processing**: circe
- **Effect System**: Cats Effect or ZIO

### Data Storage (PoC)

- **In-memory storage**: `Map[DiscordUserId, VerificationRecord]`
- **Future**: Database for production

### Configuration

```scala
case class BotConfig(
  discordToken: String,
  verifierDid: String,
  trustedIssuerDids: List[String], // List of trusted issuer DIDs
  verifiedRoleName: String, // Configurable role name (e.g., "Verified Human")
  callbackUrl: String // For deep link flow
)
```

### Key Data Models

```scala
// Verification record stored in-memory
case class VerificationRecord(
  discordUserId: String,
  holderDid: String,
  verifiedAt: Instant,
  credentialType: String = "ProofOfHumanity"
)

// Presentation request
case class PresentationRequest(
  id: String,
  requestedCredentialType: String = "ProofOfHumanity",
  challenge: String, // For replay protection
  trustedIssuers: List[String]
)

// Verification result
sealed trait VerificationResult
case class VerificationSuccess(holderDid: String, claims: Map[String, String]) extends VerificationResult
case class VerificationFailure(reason: String) extends VerificationResult
```

---

## Out of Scope

### PoC Out of Scope

The following are **explicitly out of scope** for the PoC:

1. **Revocation checking**: No periodic or on-demand revocation status checks
2. **Multi-server support**: Bot only supports a single Discord server
3. **Persistent database**: In-memory storage only (verification records lost on restart)
4. **Advanced role management**: Only grants a single configurable role
5. **Credential expiration monitoring**: No background jobs to check expiration
6. **User wallet registration**: No DID registration or management
7. **Custom credential types**: Only supports `ProofOfHumanity`
8. **Audit logging**: No comprehensive audit trail (basic logging only)

### Future Enhancements (Post-PoC)

1. **Revocation checking**: Periodic background job to check credential status
2. **Multi-server support**: Support multiple Discord servers with separate configs
3. **Database persistence**: PostgreSQL for verification records
4. **Advanced role management**: Multiple roles based on credential types/claims
5. **Admin dashboard**: Web UI for bot configuration and monitoring
6. **Credential refresh**: Automatic re-verification when credentials expire
7. **Zero-knowledge proofs**: Support for selective disclosure of claims

---

## Summary

The **Discord PoH Verification Bot** is a **Verifier** in the W3C VC ecosystem that:
- Verifies Proof of Humanity credentials from LaceID wallets
- Uses Identus SDK for W3C VC verification
- Supports two verification flows: QR/deep link and `/verify` slash command
- Grants Discord roles on successful verification
- Stores verification records in-memory (PoC)

**For PoC**:
- Single Discord server
- In-memory storage
- No revocation checking
- Configurable role name
- Basic verification flows

**Technology**: Scala 3, Identus SDK, Discord4S/Ackcord, http4s, Cats Effect/ZIO

**Relationship to KYC Issuer Agent**: Independent service that verifies credentials issued by the KYC Issuer Agent (or any other trusted issuer).

---

**Next Steps**: See main epic ticket for implementation details and acceptance criteria.
