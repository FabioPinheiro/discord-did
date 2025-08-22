# Discord DID Bot - System Context

> **Purpose**:
This document provides comprehensive system context for the Discord DID Bot.
It defines the bot's role, responsibilities, flows, and technical architecture.

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [Actors](#actors)
4. [Communication Protocols](#communication-protocols)

---

## System Overview

### What is the Discord DID Bot?

The **Discord DID Bot** is a Discord bot that implements DIDComm (Decentralized Identity Communication) authentication using DID Peer methods.

### Role in the Ecosystem

```
┌─────────────────────────────────────┐
│       Identity Wallet (Holder DID)  │
│  - Stores credentials               │
│  - Receives DIDComm messages        │
│  - Communicates via DIDComm         │
└──────────────────┬──────────────────┘
                   │
                   │ DIDComm
                   │
┌──────────────────▼──────────────────┐
│    Discord DID Bot                  │
│         (Agent DID)                 │
│  - Sends DIDComm messages           │
│  - Manages authentication flows     │
│  - Grants Discord roles             │
│  - Stores user records              │
└──────────────────┬──────────────────┘
                   │
                   │ Discord API
                   │
┌──────────────────▼──────────────────┐
│           Discord Server            │
│  - Hosts users                      │
│  - Manages roles                    │
│  - Displays bot commands            │
└─────────────────────────────────────┘
```

**Key Points**:
- The bot implements DIDComm authentication
- It does NOT issue credentials
- It grants Discord roles based on authentication

---

## Architecture

### High-Level Architecture

```
┌──────────────────────────────────────────┐
│              Discord DID Bot             │
│                                          │
│  ┌─────────────────────────────────────┐ │
│  │           Discord Bot Handler       │ │
│  │  - Slash commands (/login)          │ │
│  │  - Message handlers                 │ │
│  │  - Role management                  │ │
│  └─────────────┬───────────────────────┘ │
│                │                         │
│  ┌─────────────▼───────────────────────┐ │
│  │      Authentication Orchestrator    │ │
│  │  - Manages authentication flows     │ │
│  │  - Generates auth requests          │ │
│  │  - Handles responses                │ │
│  └─────────────┬───────────────────────┘ │
│                │                         │
│  ┌─────────────▼───────────────────────┐ │
│  │      DIDComm Service                │ │
│  │  - Manages DID Agent                │ │
│  │  - Sends/receives DIDComm messages  │ │
│  └─────────────┬───────────────────────┘ │
│                │                         │
│  ┌─────────────▼───────────────────────┐ │
│  │      User Records Store             │ │
│  │  - Discord User ID → Holder DID     │ │
│  │  - Authentication timestamp         │ │
│  └─────────────────────────────────────┘ │
│                                          │
└──────────────────────────────────────────┘
         │                       │
         │ Discord API           │ DIDComm
         ▼                       ▼
  Discord Server          Identity Wallet
```

### Components

1. **Discord Bot Handler**: Handles Discord interactions (slash commands, messages)
2. **Authentication Orchestrator**: Manages the authentication flows (QR/deep link or DIDComm)
3. **DIDComm Service**: Manages DID Agent and DIDComm messaging
4. **User Records Store**: Stores user records (in-memory for PoC)

---

## Actors

### 1. Discord User (Holder)

**Identity**: Has an identity wallet with a **Holder DID**

**Role**: Wants to authenticate using their DID to gain access to Discord server features

**Capabilities**:
- Initiates authentication via `/login` command or QR code
- Opens identity wallet when prompted
- Responds to DIDComm messages from the bot

### 2. Discord DID Bot (Agent)

**Identity**: Has an **Agent DID** (did:peer:... or did:prism:...)

**Role**: Manages DIDComm authentication and grants Discord roles

**Capabilities**:
- Generates authentication requests (QR/deep link or DIDComm)
- Sends and receives DIDComm messages
- Validates DIDComm message signatures
- Grants Discord roles on successful authentication
- Stores user records

### 3. Discord Server

**Role**: Platform hosting the bot and managing user roles

**Capabilities**:
- Hosts the bot
- Displays slash commands
- Manages user roles

---

## Communication Protocols

### 1. Discord API

**Purpose**: Bot interacts with Discord server and users

**Key Operations**:
- Register slash commands (`/login`)
- Send messages to users (DMs)
- Assign roles to users
- Handle user interactions

**Library**: JDA (Java Discord API)

### 2. DIDComm v2

**Purpose**: Secure, authenticated communication between bot and wallet

**Usage**: For authentication flows

**Message Types**:
- Custom authentication messages
- DIDComm protocol messages

### 3. Deep Link / QR Code Flow

**Purpose**: Authentication flow using deep links and QR codes

**Protocol**: Custom authentication protocol using QR codes

**Flow**:
1. Bot generates authentication request
2. Bot creates QR code or deep link
3. User scans QR or clicks link
4. Identity wallet opens with authentication request
5. User responds via DIDComm
6. Bot receives response and authenticates user


---

## Summary

The **Discord DID Bot** implements DIDComm authentication that:
- Authenticates users using DID Peer methods
- Uses scala-did library for DID operations
- Supports authentication flows: QR/deep link and `/login` slash command
- Grants Discord roles on successful authentication
- Stores user records in-memory (PoC)

**For PoC**:
- Single Discord server
- In-memory storage
- Configurable role name
- Basic authentication flows

**Technology**: Scala 3.3.6, scala-did, JDA, ZIO HTTP, ZIO 2.1.20

---

**Next Steps**: See implementation details and acceptance criteria in project documentation.
