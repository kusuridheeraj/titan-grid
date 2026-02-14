# 🤖 Gemini CLI Setup Guide

A complete reference for installing, configuring, and using the official **Google Gemini CLI** for coding tasks.

---

## 🚀 1. Installation

The CLI is a Node.js package installed globally.

### Prerequisites
- **Node.js** (v18+)
- **npm** (comes with Node.js)

### Install Command
Run this in any terminal (PowerShell, CMD, Bash):

```bash
npm install -g @google/gemini-cli
```

---

## 🔧 2. Troubleshooting "Command Not Found"

If `gemini --version` fails after installation, your global npm path is missing from your system PATH.

### ⚡ Quick Fix (Current Session Only)
```powershell
$env:Path += ";C:\Program Files\nodejs"
```

### 🛡️ Permanent Fix (Windows)
1. Search for **"Edit the system environment variables"**
2. Click **"Environment Variables"**
3. Under **"System variables"**, find `Path` and click **Edit**
4. Click **New** and add: `C:\Program Files\nodejs`
5. Click **OK** → **OK**
6. Restart your terminal.

---

## 🔑 3. Authentication

Link the CLI to your Google account:

```bash
gemini auth login
```
*This will open a browser window to authorize access.*

---

## 🧠 4. Configuring Gemini 3 Pro (High Reasoning)

By default, the CLI uses a standard model. To use the most advanced model available to you:

1. **Check available models:**
   ```bash
   gemini models list
   ```

2. **Enable Preview Features (Required for Gemini 3):**
   ```bash
   gemini settings
   # Select "Preview Features" → Toggle to TRUE
   ```

3. **Set the strongest model:**
   ```bash
   # Set to Gemini 1.5 Pro (Use this if Gemini 3 isn't listed yet)
   gemini config set model gemini-1.5-pro
   
   # OR set to Gemini 3 Pro (if available in your 'models list')
   gemini config set model gemini-exp-1206
   ```

---

## 💡 5. Common Commands

| Action | Command |
|--------|---------|
| **Chat** | `gemini` (interactive mode) |
| **One-off Prompt** | `gemini "Explain this code"` |
| **Pipe Content** | `cat file.py | gemini "Refactor this"` |
| **Check Config** | `gemini config list` |
| **Update CLI** | `npm install -g @google/gemini-cli` |

---

## 📝 Example Usage

**Generate a Python script:**
```bash
gemini "Write a script to scrape product prices from a URL"
```

**Explain a Dockerfile:**
```bash
cat Dockerfile | gemini "Explain each step in this file"
```
