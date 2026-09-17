# 🛡️ CipherVault

<div align="center">

```
  ██████╗██╗██████╗ ██╗  ██╗███████╗██████╗ ██╗   ██╗ █████╗ ██╗   ██╗██╗  ████████╗
 ██╔════╝██║██╔══██╗██║  ██║██╔════╝██╔══██╗██║   ██║██╔══██╗██║   ██║██║  ╚══██╔══╝
 ██║     ██║██████╔╝███████║█████╗  ██████╔╝██║   ██║███████║██║   ██║██║     ██║   
 ██║     ██║██╔═══╝ ██╔══██║██╔══╝  ██╔══██╗╚██╗ ██╔╝██╔══██║██║   ██║██║     ██║   
 ╚██████╗██║██║     ██║  ██║███████╗██║  ██║ ╚████╔╝ ██║  ██║╚██████╔╝███████╗██║   
  ╚═════╝╚═╝╚═╝     ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝  ╚═══╝  ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝   
```

**Military-Grade 3-Layer Cascade Cryptosystem & Anti-Forensic Security Enclave for Android**

[![Platform](https://img.shields.io/badge/Platform-Android%2024%2B-00ff66.svg?style=for-the-badge&logo=android&logoColor=black)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Security](https://img.shields.io/badge/Security-Cascade%20AES256%20%2B%20ChaCha20-00e5ff.svg?style=for-the-badge&logo=shield&logoColor=black)](#cryptographic-engine)
[![Offline](https://img.shields.io/badge/Network-100%25%20Offline%20%2F%20Zero--Telemetry-ffb700.svg?style=for-the-badge)](https://en.wikipedia.org/wiki/Zero-knowledge_proof)
[![License](https://img.shields.io/badge/License-Apache%202.0-33ffaa.svg?style=for-the-badge)](LICENSE)

[Features](#key-features) • [Architecture](#cryptographic-architecture) • [Mathematical Infeasibility](#mathematical-cryptanalysis) • [OpSec Guide](#operational-security-opsec-protocols) • [Building](#getting-started--build-instructions) • [Wiki Handbook](#offline-technical-handbook)

</div>

---

## 📖 Overview

**CipherVault** is a zero-knowledge, offline-first cryptographic vault designed for investigative journalists, security researchers, and privacy-conscious users. It combines modern UI aesthetics with defense-in-depth cryptography.

Unlike conventional encryption tools that rely on a single primitive, CipherVault processes confidential text and multi-gigabyte files through a **3-Layer Sequential Cascade Super-Encryption Pipeline** (`AES-256-GCM` $\rightarrow$ `ChaCha20-Poly1305` $\rightarrow$ `SHA-256 Bitwise Permutation`). Every session is hardened by **600,000 PBKDF2 rounds**, dual-factor hardware **Secret Pepper** isolation, and hardware-accelerated memory zeroization.

CipherVault requires **zero internet permissions**, contains **zero telemetry trackers**, and possesses **zero backdoors**.

---

## ✨ Key Features

### 🔐 3-Layer Cascade Super-Encryption
- **Layer 1 (AEAD Block Cipher):** `AES-256-GCM` with 96-bit unique IV and 128-bit authentication tag, leveraging hardware `AES-NI` crypto extensions.
- **Layer 2 (AEAD Stream Cipher):** `ChaCha20-Poly1305` (RFC 8439) with a secondary derived 256-bit key, providing immunity against CPU cache-timing side-channel attacks.
- **Layer 3 (Bit-Level Diffusion):** Dynamic bitwise XOR transformation coupled with SHA-256 feedback to maximize Shannon entropy and break ciphertext correlation patterns.

### 🔑 Dual-Factor Key Isolation (Secret Pepper)
- Decryption requires both your **user passphrase** and an isolated **32-character hexadecimal Secret Pepper** securely stored on your device.
- **Standalone Mode:** Allows exchanging files/text with third parties who do not have your Secret Pepper by relying exclusively on strict high-entropy passphrases (enforcing 14+ characters, mixed case, numbers, and symbols).

### 🚀 128KB Chunked File Streaming Engine (1GB+)
- Streams files incrementally in **128KB chunks**, eliminating memory crashes and allowing multi-gigabyte disks, 4K videos, and databases to be encrypted smoothly.
- Encapsulates encrypted data into custom `.cvault` container packages, preserving original filenames, extensions, and timestamps inside authenticated encrypted headers.
- Plausible deniability mode with support for stealth extensions (`.cenc`, `.enc`, or custom user extensions).

### 🛡️ Anti-Forensic Defense Enclave
- **`FLAG_SECURE` Hardening:** Blocks OS-level screenshots, screen recording malware, and task switcher preview leaks.
- **Memory Zeroization:** Automatically sanitizes key buffers and byte arrays immediately following cryptographic operations (`java.util.Arrays.fill(..., 0.toByte())`).
- **Volatile Clipboard Purge:** Automatically wipes copied decrypted text from the Android clipboard after a 30-second security countdown.
- **Biometric & PIN Lock:** Protects app entry with Android BiometricPrompt (Fingerprint/Face) and PBKDF2-HMAC-SHA256 salted PIN protection.

### 📝 Local Encrypted Room Database (Projects)
- Offline project scratchpad to compose, format, draft, and organize sensitive texts before one-click encryption and distribution.
- Backed by Android Jetpack Room with Kotlin Coroutines and StateFlow.

### 📚 Offline Technical Handbook & Wiki (Bilingual)
- Built-in comprehensive interactive guide directly embedded in app assets.
- Full support for **English** and **Persian (فارسی)** with bidirectional layout switching (LTR/RTL).
- Includes an interactive **Live Password Entropy & Crack Time Estimator**, visual SVG flowcharts, and security FAQ.

---

## 🔬 Cryptographic Architecture

### Super-Encryption Pipeline

```
+-------------------------------------------------------------------------------+
|                             PLAINTEXT INPUT                                   |
|                     (Raw Text or Streamed File Bytes)                         |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                      KEY DERIVATION FUNCTION (KDF)                            |
|                                                                               |
|   Password + [16-Byte CSPRNG Salt] + [32-Char Hardware Secret Pepper]          |
|                                     |                                         |
|                                     v                                         |
|                 PBKDF2-HMAC-SHA256 (600,000 Iterations)                       |
|                                     |                                         |
|        +----------------------------+----------------------------+            |
|        |                                                         |            |
|        v                                                         v            |
|  Key 1: AES-256 (256-bit)                             Key 2: ChaCha20 (256-bit)|
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                       CASCADE LAYER 1: AES-256-GCM                            |
|             (Galois/Counter Mode with 96-bit Unique Nonce + Poly Tag)         |
|              * Hardware accelerated via AES-NI / ARM Cryptography *           |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                    CASCADE LAYER 2: CHACHA20-POLY1305                         |
|            (RFC 8439 Authenticated Stream Cipher with 96-bit Nonce)           |
|            * Immune to cache-timing side-channel attacks on modern CPUs *      |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                   CASCADE LAYER 3: BITWISE XOR DIFFUSION                      |
|                  (Dynamic Permutation with SHA-256 Feedback)                  |
|                   * Maximizes Entropy & Eliminates Predictability *           |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                       ARMORED CIPHERTEXT CONTAINER                            |
|   [Magic Header] + [Salt (16B)] + [IV (12B)] + [Tag (16B)] + [Encrypted Data] |
|              Output: Base64 Armored String OR .cvault File Package            |
+-------------------------------------------------------------------------------+
```

### Cryptographic Parameter Specifications

| Parameter | Specification | Cryptographic Role & Standard |
|---|---|---|
| **Primary Cipher** | `AES-256` in GCM Mode | AEAD block cipher; NSA Suite B / Top Secret standard |
| **Secondary Cipher** | `ChaCha20` with `Poly1305` | RFC 8439 stream cipher; constant-time ARX operations |
| **Tertiary Permutation** | Bitwise XOR + SHA-256 Stream | High-diffusion stream transformation; maximizes Shannon entropy |
| **KDF Engine** | `PBKDF2-HMAC-SHA256` | 600,000 computational rounds (exceeds OWASP recommendation of 310,000) |
| **Salt Generation** | 16 Bytes (128 bits) | Cryptographically secure pseudo-random number generator (`SecureRandom`) |
| **Nonce / IV Length** | 12 Bytes (96 bits) | Unique per operation; prevents IV reuse attacks |
| **Auth Tag Length** | 16 Bytes (128 bits) | Poly1305 / GCM GMAC integrity verification tag |
| **File Chunk Size** | 128 KB (131,072 bytes) | Low memory footprint; permits streaming of multi-GB files |

---

## 🧮 Mathematical Cryptanalysis

### Why Exhaustive Brute-Force Search is Infeasible

A 256-bit symmetric key space possesses:
$$2^{256} = 115,792,089,237,316,195,423,570,985,008,687,907,853,269,984,665,640,564,039,457,584,007,913,129,639,936 \approx 1.1579 \times 10^{77} \text{ keys}$$

To illustrate this magnitude:
- The total number of atoms in the entire observable universe is estimated at $\approx 10^{80}$.
- If an adversary deploys a supercomputing cluster capable of testing **100 Trillion ($10^{14}$) keys per second**:

$$\text{Seconds to exhaust} = \frac{1.1579 \times 10^{77}}{10^{14}} = 1.1579 \times 10^{63} \text{ seconds}$$
$$\text{Years to exhaust} \approx 3.67 \times 10^{55} \text{ years}$$

Given that the age of the universe is approximately $1.38 \times 10^{10}$ years, testing the keyspace would require **$2.66 \times 10^{45}$ times the age of the universe**.

### The 600,000 Iteration PBKDF2 Barrier

Specialized GPU rigs (e.g., arrays of NVIDIA RTX 4090s or ASIC miners) can compute billions of single-iteration hashes (such as MD5 or single-round SHA-256) per second. 

CipherVault requires **600,000 rounds of HMAC-SHA256** for every password evaluation:
- Each password guess requires $600,000 \times 2 = 1,200,000$ inner SHA-256 transforms.
- An attacker testing passphrases against a captured CipherVault payload is throttled to fewer than 50 to 100 guesses per second per GPU core.
- An alphanumeric password of 12 characters under this workload would take centuries to dictionary-attack.

### AEAD Integrity Guard

Both `AES-GCM` and `Poly1305` compute an **Authentication Tag** over the ciphertext and associated data. If an adversary flips or tampers with even a single bit during transit:
1. The authentication tag check fails in constant time.
2. The decryption engine aborts immediately before any plaintext is released.
3. Cryptanalytic padding-oracle and chosen-ciphertext attacks (CCA) are rendered impossible.

---

## 🛡️ Operational Security (OpSec) Protocols

```
  +-------------------------------------------------------------------------+
  |                        COLD STORAGE PROTOCOL                            |
  |                                                                         |
  |  [Settings] -> [Secret Pepper] -> 32-Character Hex Key                  |
  |                                                                         |
  |  1. Transcribe key onto acid-free paper or stamp on a metal plate.      |
  |  2. Deposit physical plate inside a fireproof security safe.            |
  |  3. NEVER save in iCloud, Google Drive, WhatsApp, or Telegram.          |
  +-------------------------------------------------------------------------+
```

1. **Zero-Knowledge Guarantee:** There is no server, no cloud, and no master recovery key. If you forget your password or modify your Secret Pepper without a backup, **your data cannot be recovered**.
2. **Cold Storage Rule:** Navigate to `Settings > Secret Pepper`, transcribe the 32-character hexadecimal code onto physical archival paper or engrave it onto a metal plate, and store it in a physical safe.
3. **Cross-Device Decryption:**
   - To decrypt on multiple devices running CipherVault, both devices must have the **exact same 32-character Secret Pepper** configured in Settings.
   - If sending an encrypted payload to a colleague who does not have your Secret Pepper, use **Standalone Mode** (requires a high-entropy password of at least 14 characters).
4. **Active Screenshot Guard:** Keep `FLAG_SECURE` enabled in Settings to prevent third-party accessibility services, spyware, or Android task previews from capturing your cleartext.

---

## 💻 Tech Stack & Dependencies

- **Language:** Kotlin 2.0+ (100% Kotlin)
- **UI Toolkit:** Jetpack Compose (Material Design 3, Cyberpunk Matrix Palette)
- **Local Persistence:** Android Jetpack Room with Kotlin Coroutines & Flow
- **Security & Cryptography:** 
  - Java Cryptography Extension (JCE) & BouncyCastle Provider
  - `javax.crypto.Cipher` (`AES/GCM/NoPadding`)
  - `org.bouncycastle.crypto.engines.ChaCha20Poly1305`
  - `javax.crypto.spec.PBEKeySpec` (`PBKDF2WithHmacSHA256`)
- **System Hardening:** Android BiometricPrompt Enclave, `WindowManager.LayoutParams.FLAG_SECURE`
- **Build System:** Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalog (`libs.versions.toml`)

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or later / Meerkat
- **JDK:** OpenJDK 17 or 21
- **Android SDK:** 
  - `minSdk`: 24 (Android 7.0 Nougat)
  - `targetSdk`: 36 (Android 15+)
  - `compileSdk`: 36

### Cloning & Building

```bash
# 1. Clone the repository
git clone https://github.com/your-username/ciphervault.git
cd ciphervault

# 2. Build the debug APK via Gradle
gradle :app:assembleDebug

# 3. Locate the output APK
# File will be generated at: app/build/outputs/apk/debug/app-debug.apk
```

### Running Unit & Robolectric Tests

```bash
# Run local JVM unit tests
gradle :app:testDebugUnitTest

# Verify Roborazzi screenshot tests (if configured)
gradle :app:verifyRoborazziDebug
```

---

## 📂 Project Structure

```
CipherVault/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   └── wiki/
│   │   │   │       └── index.html         # Embedded Bilingual Offline Wiki & Entropy Calc
│   │   │   ├── java/com/kafappstore/ciphervault/
│   │   │   │   ├── crypto/
│   │   │   │   │   ├── CryptoEngine.kt     # 3-Layer Cascade Super-Encryption Engine
│   │   │   │   │   └── StreamCrypto.kt     # 128KB Chunked Streaming File Processor
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/             # Room Database Entities & DAOs
│   │   │   │   │   └── repository/        # Project & Encryption State Repositories
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/        # Cyberpunk UI, Buttons, Overlays, File Streamers
│   │   │   │   │   ├── screens/           # Text, File, Projects, Settings, About, Wiki
│   │   │   │   │   └── theme/             # Cyber Green, Cyan, Dark Matrix Theme Tokens
│   │   │   │   ├── utils/                 # Security SharedPreferences, Memory Zeroizers
│   │   │   │   ├── MainActivity.kt        # Edge-to-Edge Entry Point & Enclave Check
│   │   │   │   └── CipherVaultApp.kt      # Application Lifecycle Configuration
│   │   │   └── res/                       # Drawables, Strings, XML Resources
│   │   └── test/                          # Unit and Robolectric Tests
│   └── build.gradle.kts                   # App module build configuration
├── gradle/
│   └── libs.versions.toml                 # Version Catalog
├── build.gradle.kts                       # Root build configuration
├── settings.gradle.kts                    # Root project settings
└── README.md                              # Technical Handbook & Repository Documentation
```

---

## 📖 Offline Technical Handbook

CipherVault ships with a self-contained, offline HTML/JS/CSS documentation suite located in `app/src/main/assets/wiki/index.html`. 

Users can access this handbook directly inside the app by going to **About > Technical Handbook & Wiki**. It features:
- **Interactive Language Toggle:** Seamless switching between **English** and **Persian (فارسی)** with dynamic RTL adaptation.
- **Interactive Password Entropy Calculator:** Live client-side calculation of information entropy bits, character pool sizes, and PBKDF2 cluster crack times.
- **Visual Vector Pipeline Diagrams:** Native SVG schematics of key derivation and data transformation.
- **Search & Filter Engine:** Instant keyword filtering across all cryptographic specifications and OpSec rules.

---

## 🔒 Threat Model & Security Disclosures

CipherVault is engineered against the following threat vectors:
- ✅ **Remote Man-in-the-Middle (MitM) & Eavesdropping:** Nullified (zero network stack; authenticated ciphertext).
- ✅ **Mass GPU Cracking Arrays:** Throttled to computational futility via 600k PBKDF2 iterations.
- ✅ **Targeted Endpoint Credential Theft:** Protected via 32-character isolated hardware Secret Pepper.
- ✅ **RAM Dump & Cold Boot Forensics:** Mitigated via active memory zeroization buffers.
- ✅ **Screen Scraping & Recent App Preview Malware:** Prevented via OS-level `FLAG_SECURE`.

### Reporting Vulnerabilities
If you discover a security vulnerability or cryptographic flaw within CipherVault, please submit a responsible disclosure report via GitHub Security Advisories or reach out to the development team.

---

## 📄 License

```
Copyright 2026 CipherVault Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
