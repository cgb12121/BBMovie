# AI Refinery Service (Rust)

A high-performance, low-footprint AI processing microservice written in Rust. This service handles CPU-intensive tasks like OCR, PDF text extraction, and Audio Transcription, designed to run efficiently on consumer hardware (e.g., laptops with limited VRAM).

## 🚀 Features

- **📄 PDF Extraction**: High-speed text extraction from PDFs.
- **👁️ OCR (Optical Character Recognition)**: Extracts text from images using Tesseract.
- **🎧 Audio Transcription**: Converts speech to text using `whisper.cpp` (runs locally on CPU).
- **🖼️ Vision Analysis**: Describes images using Ollama (e.g., `moondream` or `llava`).
- **📡 Service Discovery**: Auto-registers with Eureka (Spring Cloud).

## 🛠️ Prerequisites

This project relies on native C++ libraries (`whisper.cpp`) and external tools (`tesseract`).

### 🚨 CRITICAL NOTE FOR WINDOWS USERS 🚨

To build `whisper-rs` (C++ bindings) correctly on Windows, you **must** follow these specific steps. Standard Command Prompt, PowerShell, or IDE Terminals (VSCode/IntelliJ) will often **fail** to build the C++ dependencies.

1.  **Install Visual Studio (The Purple IDE)**
    *   Do **not** confuse this with Visual Studio Code (Blue icon). You need the full **[Visual Studio Community](https://visualstudio.microsoft.com/downloads/)** (or Pro/Enterprise).
    *   During installation, select the **"Desktop development with C++"** workload.
    *   Ensure **"MSVC ... C++ x64/x86 build tools"** and **"Windows 10/11 SDK"** are checked on the right side.

2.  **Use the "x64 Native Tools Command Prompt"**
    *   Press the **Windows Key** and type **"x64 Native Tools"**.
    *   Select **"x64 Native Tools Command Prompt for VS 2022"** (or 2019+).
    *   **ALWAYS** use this specific terminal window to run `cargo build` and `cargo run`, not x86 PowerShell or Command Prompt or any other terminals.
    *   *Why?* This terminal automatically loads the necessary environment variables (LIB, INCLUDE, etc.) for the C++ compiler that standard terminals lack.

---

### 1. Rust Toolchain
Install Rust via [rustup.rs](https://rustup.rs/).

### 2. LLVM & Clang (Required for `whisper-rs`)
Used to generate bindings for the C++ Whisper library.
- **Windows**:
  - Option A: Install via Chocolatey: `choco install llvm`
  - Option B: Download the installer from [LLVM Releases](https://github.com/llvm/llvm-project/releases).
  - **Important**: Ensure `LIBCLANG_PATH` is set or `llvm-config` is in your system `PATH`.

### 3. CMake (Required for building `whisper.cpp`)
- **Windows**: `choco install cmake` or download from [cmake.org](https://cmake.org/download/).
- Ensure `cmake` is in your system `PATH`.

### 4. Tesseract OCR (Required for `rusty-tesseract`)
- **Windows**:
  - Install via [UB-Mannheim installer](https://github.com/UB-Mannheim/tesseract/wiki).
  - **Critical Step**: Add the installation directory (e.g., `C:\Program Files\Tesseract-OCR`) to your system `PATH` environment variable.
  - You may also need to set `TESSDATA_PREFIX` to the `tessdata` folder inside the installation.

### 5. Ollama (Required for Vision)
- Download and install [Ollama](https://ollama.com/).
- Pull a lightweight vision model (recommended for low VRAM):
  ```bash
  ollama pull moondream
  ```

## ⚙️ Setup & Installation

1.  **Clone the repository**
    ```bash
    git clone <https://github.com/cgb12121/BBMovie>
    cd rust-ai-context-refinery
    ```

2.  **Download the Whisper Model**
    The service expects a GGML model file in `models/whisper-cpp/`. (the repo should include this file already, but if not, run the following commands to download it)
    ```bash
    # Create directory
    mkdir -p models/whisper-cpp
    
    # Download 'base.en' model (recommended balance of speed/accuracy)
    curl -L -o models/whisper-cpp/ggml-base.en.bin https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin
    ```

3.  **Build the Project**
    ```bash
    cargo build
    ```

## 🏃 Running the Service

```bash
cargo run
```
The service will start on port **8686** and register with Eureka at `http://localhost:8761/eureka`.

## 🔧 Configuration

| Environment Variable  | Default                        | Description                   |
|-----------------------|--------------------------------|-------------------------------|
| `PORT`                | `8686`                         | Service Port                  |
| `EUREKA_URL`          | `http://localhost:8761/eureka` | Service Discovery URL         |
| `OLLAMA_VISION_MODEL` | `moondream`                    | Model used for image analysis |

## ⚠️ Troubleshooting on Windows
Some dependencies may fail to build on Windows, they were made for Linux. Here are some common issues and their solutions:
- **"LINK : fatal error LNK1181: cannot open input file 'libclang.lib'"**:
  - Reinstall LLVM and ensure "Add LLVM to the system PATH" is selected during installation.
- **"tesseract not found"**:
  - Verify Tesseract is installed and `tesseract.exe` is accessible from PowerShell, make sure to add it to the Environment Path.
- **"ollama not found"**:
  - Verify Ollama is installed and `ollama.exe` is accessible from PowerShell, make sure to add it to the Environment Path.
- **"Is code not working in VS Code?"**:
  - VS Code uses RustAnalyzer, it uses background `cargo check` to suggest the code => might fail to build c++ dependencies when you are not configuring your environment correctly.
  - JetsBrain RustRover use their own build system ⇒ skip building c++ dependencies.
  - Use RustRover then Windows ⇒ x64 Native Tools Command Prompt => `cargo build --release` => `cargo run --release`.