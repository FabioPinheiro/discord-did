# Discord DID Bot

A Discord bot that implements DIDComm (Decentralized Identity Communication) authentication using DID Peer methods. Users can authenticate via Discord using decentralized identifiers and QR codes.

## Features

- **DIDComm Authentication**: Secure authentication using DID Peer 4 protocol
- **QR Code Login**: Users scan QR codes to authenticate with their identity wallets
- **Slash Commands**: Simple Discord slash command interface (`/login`)
- **In-Memory State**: Fast, lightweight user session management (PoC)

## Tech Stack

- **Scala**
- **ZIO** - Effect management
- **ZIO HTTP** - HTTP server
- **JDA** - Discord integration
- **scala-did** - DID operations
- **QRGen** - QR code generation

## Quick Start

### Prerequisites

- JDK 11 or higher
- sbt 1.x

### Build & Run

```bash
# Compile the project
sbt compile

# Run the bot
sbt run

# Run tests
sbt test
```

### Development

```bash
# Start Scala REPL with project loaded
sbt console

# Format code
sbt scalafmt

# Check code formatting
sbt scalafmtCheck
```

## Architecture

### Components

**Main Application** (`src/main/scala/Main.scala`)
- `DiscordDID`: ZIO application entry point, initializes DID agent and Discord bot
- `SlashCommandListener`: Handles Discord slash commands and authentication flow
- `MessageReceiveListener`: Logs received messages

### DID Agent

The bot uses a DID Peer 4 agent with:
- **X25519** key for encryption
- **Ed25519** key for signing
- DIDComm relay endpoints (HTTPS and WebSocket)
- Relay: `https://relay.fmgp.app`

### Authentication Flow

1. User runs `/login` command in Discord
2. Bot checks in-memory database for existing user
3. For new users:
   - Generates `AuthRequest` DIDComm message
   - Signs message with bot's DID
   - Creates QR code with signed message
4. User scans QR code with identity wallet
5. Wallet responds via DIDComm
6. Bot authenticates user and updates status

### Available Commands

- `/login` - Start authentication flow with QR code
- `/say` - Echo command (testing)
- `/db` - Show in-memory database (debug only)
- `/leave` - Make bot leave server (admin)

## Configuration

### Dependencies

The project uses:
- JitPack resolver for custom dependencies
- scala-did modules: did, did-imp, did-comm-protocols, did-method-prism, did-method-peer
- `MultiFallbackResolver` with `DidPeerResolver` for DID resolution

## Important Notes

⚠️ **Security**: Discord bot token is currently hardcoded. Move to environment variables or config file for production.

⚠️ **State Management**: User data is stored in-memory and will be lost on restart. Implement persistent storage for production.

⚠️ **PoC Status**: This is a proof-of-concept implementation with single-server support and basic features.

