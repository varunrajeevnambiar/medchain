# MedChain — Patient-Controlled Medical Data Blockchain System

MedChain is a decentralized, cryptographically secured blockchain architecture that puts **patients in complete control of their healthcare records**.

---

## 🏛️ Architecture & Security Model

```
 ┌───────────────────────────────────────────────────────────────┐
 │                   Patient (Data Owner)                        │
 │  - Holds RSA-2048 Private Key                                 │
 │  - Encrypts records with AES-256                              │
 │  - Grants / Revokes time-bound or record-specific access      │
 └───────────────────────────────┬───────────────────────────────┘
                                 │
                  ┌──────────────┴──────────────┐
                  ▼                             ▼
   ┌─────────────────────────────┐┌──────────────────────────────┐
   │ AccessControl Smart Contract││    Encrypted Data Store      │
   │ - Validates signatures      ││ - AES-256-CBC Ciphertext     │
   │ - Verifies active grants    ││ - Off-chain privacy-preserving│
   │ - Enforces expiry & scopes  ││   storage                    │
   └──────────────┬──────────────┘└──────────────────────────────┘
                  │
                  ▼
   ┌─────────────────────────────────────────────────────────────┐
   │                     MedChain Blockchain                     │
   │  [Block 0: Genesis] ◄── [Block 1] ◄── [Block 2] ...         │
   │  - Proof-of-Work Mining (SHA-256, Configurable Difficulty)  │
   │  - Merkle Tree transaction integrity                        │
   │  - Immutable Audit Trail of all grants, revokes & accesses  │
   └─────────────────────────────────────────────────────────────┘
```

---

## 🔐 Cryptographic Pillars

1. **Identity & Non-Repudiation (RSA-2048)**:
   - Each patient and doctor generates an asymmetric RSA-2048 key pair upon registration.
   - Private keys remain on the client/owner device; only public keys are registered on-chain.
   - All transactions (registering, adding records, granting access, revoking access) are digitally signed with `SHA256withRSA`.

2. **Record Confidentiality (AES-256-CBC)**:
   - Medical record payloads (diagnoses, lab results, prescriptions, imaging) are encrypted off-chain using symmetrical AES-256 with random IV prepended.
   - Plaintext medical information is **never** written to the public blockchain.

3. **Secure Key Exchange**:
   - When a patient grants access to a doctor, the patient encrypts the record's AES symmetric key with the doctor's RSA public key (`RSA/ECB/PKCS1Padding`).
   - Only the designated doctor holding the matching RSA private key can decrypt the AES key, and thus decrypt the medical record.

4. **Tamper-Evident Integrity (SHA-256 & Merkle Trees)**:
   - Every block calculates a binary Merkle Root from its transaction hashes.
   - Blocks are chained via `previousHash`.
   - Proof-of-Work requires block hashes to satisfy the difficulty target (e.g. leading zeros `00`).

---

## 📂 Source Code Structure

All classes are located under `c:\Users\Dithin Mukkati\java1\src\com\medchain\`:

| Package | Class | Purpose |
|---|---|---|
| `crypto` | `CryptoUtils.java` | RSA-2048, AES-256-CBC, SHA-256, digital signatures, key serialization |
| `blockchain` | `TransactionType.java` | Enum: `REGISTER_PATIENT`, `REGISTER_DOCTOR`, `ADD_MEDICAL_RECORD`, `GRANT_ACCESS`, `REVOKE_ACCESS`, `ACCESS_MEDICAL_RECORD`, `UPDATE_MEDICAL_RECORD` |
| `blockchain` | `Transaction.java` | Immutable on-chain record with timestamp, digital signature & payload hash |
| `blockchain` | `MerkleTree.java` | Computes cryptographic Merkle tree root hash from transaction list |
| `blockchain` | `Block.java` | Block structure containing transactions, Merkle root, PoW mining (`mine()`) |
| `blockchain` | `Blockchain.java` | Ledger management, pending transaction pool, chain validation (`isChainValid()`) |
| `model` | `Patient.java` | Patient entity with cryptographic identity and metadata |
| `model` | `Doctor.java` | Doctor entity with medical license and cryptographic identity |
| `model` | `MedicalRecord.java` | Encrypted health record with hash and key envelope |
| `model` | `AccessLevel.java` | Enum: `READ_ONLY`, `READ_WRITE` |
| `model` | `AccessGrant.java` | Patient-issued grant specifying doctor, allowed records, level, and expiry |
| `contract` | `AccessControlContract.java` | Smart contract executing grant verification, record access, and audit emission |
| `storage` | `EncryptedDataStore.java` | Off-chain data store managing encrypted records and access index |
| `network` | `Node.java` / `PeerNetwork.java` | Distributed node simulation with consensus and peer synchronization |
| `service` | `PatientService.java` | High-level patient workflows (encrypt record, grant/revoke access) |
| `service` | `DoctorService.java` | Doctor operations (read permitted records, write prescriptions) |
| `service` | `AuditService.java` | Query and format complete immutable access audit trails |
| `web` | `MedChainWebServer.java` | Built-in HTTP REST API (port `8081`) |
| Root | `MedChainDemo.java` | Comprehensive 15-step end-to-end interactive demo |

---

## 🚀 How to Run

### Step 1: Compile
Double-click `compile_blockchain.bat` or run from command prompt:
```cmd
cd "c:\Users\Dithin Mukkati\java1"
compile_blockchain.bat
```

### Step 2: Run the Demo & Web Server
Double-click `run_blockchain.bat` or run from command prompt:
```cmd
cd "c:\Users\Dithin Mukkati\java1"
run_blockchain.bat
```

---

## 🧪 What the Demo Tests

1. **Initialization**: Creates peer node, mines genesis block.
2. **Registration**: Registers patients (Alice, Bob) and doctors (Dr. Smith, Dr. Jones, Dr. Patel) with RSA-2048 keypairs.
3. **Encrypted Record Creation**: Adds encrypted diagnoses, lab panels, and imaging reports.
4. **Selective Access Delegation**:
   - Alice grants Dr. Smith `READ_ONLY` access to all records.
   - Alice grants Dr. Jones `READ_WRITE` access to specific records with a 30-day expiry.
   - Bob grants Dr. Patel `READ_WRITE` access.
5. **Enforcement & Security Tests**:
   - Dr. Smith reads Alice's records ➔ **ALLOWED**
   - Dr. Jones adds an authorized prescription for Alice ➔ **ALLOWED**
   - Dr. Patel attempts to access Alice's records without grant ➔ **DENIED** 🛡️
   - Dr. Smith attempts to access Bob's records without grant ➔ **DENIED** 🛡️
6. **Revocation**: Alice revokes Dr. Smith. Subsequent access by Dr. Smith ➔ **DENIED** 🛡️
7. **Audit & Mining**: Mines pending transactions into blocks, verifies Merkle roots, prints audit reports, and validates full blockchain integrity.
8. **REST API**: Starts HTTP service on `http://localhost:8081`.

---

## 🌐 REST API Endpoints

Once the application is running, the following endpoints are available:

- `GET /status` — Get node status, block height, difficulty, and pending transactions.
- `GET /blockchain/chain` — Inspect the full blockchain and blocks in JSON format.
- `POST /patients/register` — Register a new patient and receive keypair.
- `POST /doctors/register` — Register a new doctor.
- `POST /records/add` — Add an encrypted medical record.
- `POST /access/grant` — Grant a doctor access to records.
- `POST /access/revoke` — Revoke access permissions.
- `GET /records/{patientId}` — Fetch encrypted medical records.
- `GET /grants/{patientId}` — View active access grants.
- `GET /audit/{patientId}` — Retrieve complete immutable audit trail.
- `POST /mine` — Mine pending transactions into a new block.
