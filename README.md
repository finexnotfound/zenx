# ZEN X — Advanced Android Terminal Environment

```text
 ███████╗███████╗███╗   ██╗    ██╗  ██╗
 ╚══███╔╝██╔════╝████╗  ██║    ╚██╗██╔╝
   ███╔╝ █████╗  ██╔██╗ ██║     ╚███╔╝ 
  ███╔╝  ██╔══╝  ██║╚██╗██║     ██╔██╗ 
 ███████╗███████╗██║ ╚████║    ██╔╝ ██╗
 ╚══════╝╚══════╝╚═╝  ╚════╝    ╚═╝  ╚═╝
```

<div align="center">

### ⚡ One-Click Easy Download & Live Launch

[![Download ZEN X](https://img.shields.io/badge/📥_DOWNLOAD_APP-ZEN_X_FOR_ANDROID-00E676?style=for-the-badge&logo=android&logoColor=000000)](https://ais-pre-pyr2zqa6fbmtef4gwxpgn5-552144887852.asia-southeast1.run.app)
[![Launch Live Preview](https://img.shields.io/badge/🚀_LAUNCH_APP-LIVE_EMULATOR-00B0FF?style=for-the-badge&logo=googlechrome&logoColor=white)](https://ais-pre-pyr2zqa6fbmtef4gwxpgn5-552144887852.asia-southeast1.run.app)

**👉 [CLICK HERE TO DOWNLOAD / OPEN ZEN X IMMEDIATELY](https://ais-pre-pyr2zqa6fbmtef4gwxpgn5-552144887852.asia-southeast1.run.app) 👈**

*Instant access: Run the full terminal in the cloud streaming emulator or export APK directly from the Settings menu!*

---

</div>

> **ZEN X** is an advanced, fully functional Linux-like terminal emulator for Android, built with Jetpack Compose, featuring an embedded package manager, real GitHub cloning, Linux toolchain utilities, and the **Flint AI** terminal assistant.

---

## 👑 Creator & Lead Developer

* **Creator:** **finex** ([@finexcreates](mailto:finexcreates@gmail.com))
* **Email:** finexcreates@gmail.com
* **Project:** ZEN X Android Terminal

---

## 🌟 Key Features

### 1. 📦 Realistic Package Manager (`pkg` / `apt`)
Install, update, search, and manage software packages inside your terminal sandbox with animated package progress, byte transfers, and dependency resolution:
```bash
pkg install git        # Installs Git version control
pkg install python     # Installs Python 3.12 interpreter
pkg install node       # Installs Node.js 20 runtime
pkg install neofetch   # Installs system info utility
pkg install cowsay     # Installs ASCII cow utility
pkg list               # Lists all installed packages
pkg search <query>     # Searches package catalog
pkg open flint         # Launches the Flint AI assistant
```

### 2. ⚡ Flint AI Terminal Assistant
A dedicated artificial intelligence companion running natively inside the terminal prompt:
```bash
pkg open flint         # Enters interactive AI prompt (flint> )
flint "how do I use git clone?"
flint explain script.py
```
* **Interactive Mode**: Prompt changes to `flint> ` with conversational context memory.
* **Powered by Gemini**: Get real-time coding help, bash command explanations, script generation, and debugging assistance.
* Type `exit` or `quit` to return cleanly to standard shell.

### 3. 🐙 Real Git Version Control (`git clone`)
Clone public GitHub repositories directly into the persistent local Android filesystem:
```bash
pkg install git
git clone https://github.com/octocat/Hello-World
cd Hello-World
ls -la
cat README.md
```
Supports `git init`, `git status`, `git add`, `git commit -m`, `git log`, and `git branch`.

### 4. 🗂️ Persistent Linux File System
Backed by internal Android app storage (`~/` or `/data/user/0/.../files/home`):
* Standard commands: `ls`, `cd`, `pwd`, `mkdir -p`, `touch`, `cat`, `rm -rf`, `cp`, `mv`, `tree`.
* Redirection support: `echo "Hello world" > file.txt` and `echo "line" >> file.txt`.
* Default demo files included: `README.txt`, `hello.py`, and `welcome.sh`.

### 5. 📝 Interactive Nano Text Editor (`nano`)
Full terminal text editor with mobile-optimized shortcut bar:
```bash
nano hello.py
```
* `^O Save & Exit`
* `^W WriteOut`
* `^X Discard`

### 6. 🛠️ Built-in Tools & Interpreters
* **Python REPL & Runner**: `python` or `python script.py`
* **Node.js REPL & Runner**: `node` or `node app.js`
* **Network & Web Requests**: `curl https://api.github.com`
* **Weather Forecasts**: `weather Paris` or `curl wttr.in`
* **Calculator**: `calc (120 * 4) / 2`
* **System Info**: `neofetch`, `uname -a`, `uptime`, `free`, `df`, `date`, `whoami`
* **ASCII Fun**: `cowsay "ZEN X by finex"` and `figlet "ZEN X"`

### 7. 📱 Termux Mobile Experience
* **Accessory Key Bar**: `ESC`, `TAB`, `CTRL`, `ALT`, `~`, `/`, `-`, `|`, `^C`, `UP`, `DOWN`, `CLR`, `FLINT`.
* **Tab Completion**: Auto-completes commands, packages, and file paths.
* **Command History**: Navigate previous commands via UP/DOWN keys.
* **Multiple Sessions**: Open and switch between concurrent terminal tabs (`Session 1`, `Session 2`, etc.).
* **Custom Themes**: Zen Dark, Matrix Green, Cyberpunk, Monokai, and Hacker Amber.
* **Dynamic Font Sizing**: Quick zoom controls (A- / A+).

---

## 🚀 Quick Start Guide

1. Open **ZEN X**. You will see the ASCII banner and welcome prompt:
   ```text
   zenx@android:~$
   ```
2. Install Git and clone a project:
   ```text
   zenx@android:~$ pkg install git
   zenx@android:~$ git clone https://github.com/octocat/Hello-World
   ```
3. Talk with Flint AI:
   ```text
   zenx@android:~$ pkg open flint
   flint> How do I write a Python loop?
   flint> exit
   ```
4. View system information:
   ```text
   zenx@android:~$ pkg install neofetch
   zenx@android:~$ neofetch
   ```

---

## 📲 How to Download & Install APK on Android

1. Click the **[Download App Button](https://ais-pre-pyr2zqa6fbmtef4gwxpgn5-552144887852.asia-southeast1.run.app)**.
2. In the AI Studio interface, open the **Settings / Export** menu in the top-right toolbar.
3. Select **"Download APK"** (or **"Export as ZIP"** to inspect the source code).
4. Transfer or open the `.apk` file on your Android device to install and launch **ZEN X**!

---

## 🛠️ Technology Stack

* **Platform:** Android (minSdk 24, targetSdk 36)
* **Language:** Kotlin 2.2+
* **UI Framework:** Jetpack Compose & Material 3
* **Networking:** OkHttp 4.10 & Retrofit 2.12
* **AI Engine:** Google Gemini API (`gemini-3.5-flash`) via Google AI Studio
* **Architecture:** MVVM + Clean Architecture with Coroutines & StateFlow

---

## 📄 License & Credits

Created by **finex** ([finexcreates@gmail.com](mailto:finexcreates@gmail.com)).  
All rights reserved. Designed for developers and hackers on Android.
