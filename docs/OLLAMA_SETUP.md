# Ollama Installation & Setup - Complete Guide

**Service:** Ollama (Local LLM Runtime)  
**Purpose:** Run Llama 3 locally for Nexus AI Agent  
**Cost:** FREE (Uses your RTX 4050 GPU)  
**Hardware:** Works on CPU, optimized for NVIDIA GPUs

---

## Step 1: System Requirements

### Minimum Requirements
- **OS:** Windows 10/11
- **RAM:** 8GB (16GB recommended)
- **Disk Space:** 10GB for Llama 3 model
- **GPU:** Optional, but NVIDIA GPU significantly faster

### ✅ Your System
- **GPU:** NVIDIA GeForce RTX 4050 ✅
- **VRAM:** 6GB (Llama 3 will use ~4GB)
- **Expected Speed:** ~30-50 tokens/second

---

## Step 2: Install NVIDIA Drivers (If Not Updated)

### 2.1 Check Current Driver Version
```powershell
nvidia-smi
```

If you see output with driver version, you're good. If not installed:

### 2.2 Download Latest Driver
1. Go to: https://www.nvidia.com/download/index.aspx
2. **Product Type:** GeForce
3. **Product Series:** GeForce RTX 40 Series (Notebooks)
4. **Product:** GeForce RTX 4050 Laptop GPU
5. **Operating System:** Windows 11
6. Click **"Search"** → **"Download"**
7. Run the installer and select **"Express Installation"**

---

## Step 3: Install Ollama

### 3.1 Download Ollama
1. Go to: https://ollama.com/download/windows
2. Click **"Download for Windows"**
3. File: `OllamaSetup.exe` (~200MB)

### 3.2 Run Installer
1. Double-click `OllamaSetup.exe`
2. **User Account Control:** Click **"Yes"**
3. The installer will:
   - Install to: `C:\Users\kusur\AppData\Local\Programs\Ollama`
   - Add to PATH automatically
   - Install Windows Service
4. Installation takes ~1 minute

### 3.3 Verify Installation
Open **new** PowerShell window:
```powershell
ollama --version
```

Expected output:
```
ollama version 0.x.x
```

---

## Step 4: Download Llama 3 Model

### 4.1 Pull Llama 3 (8B Parameters)
```powershell
ollama pull llama3
```

**Download Details:**
- Size: ~4.7GB
- Time: 5-15 minutes (depending on internet speed)
- Storage: `C:\Users\kusur\.ollama\models`

You'll see progress:
```
pulling manifest
pulling 8934d96d3f08... 100% ▕████████████████▏ 4.7 GB
pulling 8c17c2ebb0ea... 100% ▕████████████████▏ 7.0 KB
pulling 7c23fb36d801... 100% ▕████████████████▏ 4.8 KB
pulling 2e0493f67d0c... 100% ▕████████████████▏  59 B
pulling fa304d675061... 100% ▕████████████████▏  91 B
pulling 42347cd80dc8... 100% ▕████████████████▏ 557 B
verifying sha256 digest
writing manifest
success
```

---

## Step 5: Test Ollama

### 5.1 Interactive Chat
```powershell
ollama run llama3
```

You'll see:
```
>>> 
```

Try these prompts:
```
>>> Hello, what can you help me with?
>>> Explain what a rate limiter is in distributed systems
>>> Write a Python function to reverse a string
```

**Exit the chat:**
```
>>> /bye
```

Or press `Ctrl + D`

### 5.2 Test GPU Usage
While Ollama is running, check GPU utilization:
```powershell
# In a NEW terminal window
nvidia-smi
```

You should see:
```
+-------------------------------------------------------------------------+
| NVIDIA-SMI 5XX.XX       Driver Version: 5XX.XX       CUDA Version: 12.X |
|-------------------------------+----------------------+------------------+
| GPU  Name            TCC/WDDM | Bus-Id        Disp.A | Volatile Uncorr. |
| Fan  Temp  Perf  Pwr:Usage/Cap|         Memory-Usage | GPU-Util  Compute|
|===============================+======================+==================|
|   0  NVIDIA GeForce ... WDDM  | 00000000:01:00.0 Off |                N/A |
| N/A   55C    P0    25W /  N/A |   4200MiB /  6144MiB |     98%      Default |
+-------------------------------+----------------------+------------------+
```

✅ If you see high GPU-Util and Memory-Usage, GPU acceleration is working!

---

## Step 6: Ollama API Testing

Ollama runs a local API server on `http://localhost:11434`

### 6.1 Test with PowerShell
```powershell
$body = @{
    model = "llama3"
    prompt = "What is the capital of France?"
    stream = $false
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:11434/api/generate" `
    -Method POST `
    -Body $body `
    -ContentType "application/json"
```

Expected response:
```json
{
  "model": "llama3",
  "response": "The capital of France is Paris.",
  "done": true
}
```

### 6.2 Test with Python (Optional)
Create `test_ollama.py`:
```python
import requests

response = requests.post('http://localhost:11434/api/generate',
    json={
        "model": "llama3",
        "prompt": "Explain rate limiting in 2 sentences.",
        "stream": False
    }
)

print(response.json()['response'])
```

Run:
```powershell
python test_ollama.py
```

---

## Step 7: Configure for Nexus Module

### 7.1 Update `.env`
Add to `c:\PlayStation\assets\titan-grid\.env`:
```env
# Ollama Configuration
OLLAMA_HOST=http://localhost:11434
OLLAMA_MODEL=llama3
```

### 7.2 Verify Ollama Service is Running
```powershell
# Check if Ollama service is running
Get-Service Ollama

# If stopped, start it:
Start-Service Ollama
```

---

## Advanced: Model Options

### Other Models Available
```powershell
# Smaller, faster (2B parameters, ~1.5GB)
ollama pull llama3.2

# Code-specialized model
ollama pull codellama

# Larger, more accurate (70B, requires 40GB+ RAM)
ollama pull llama3:70b
```

### List Installed Models
```powershell
ollama list
```

### Remove a Model (Free Up Space)
```powershell
ollama rm llama3:70b
```

---

## Troubleshooting

### Issue: "ollama: command not found"
**Solution:**
1. Restart your terminal (PATH needs refresh)
2. Or run: `$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine")`

### Issue: Model download fails
**Solution:**
- Check internet connection
- Check disk space: `ollama list` shows installed models
- Retry: `ollama pull llama3`

### Issue: GPU not detected
**Solution:**
1. Update NVIDIA drivers
2. Check CUDA compatibility: `nvidia-smi` should show CUDA Version
3. Ollama will fallback to CPU (slower but works)

### Issue: High CPU/GPU usage even when idle
**Solution:**
- Ollama keeps models loaded in memory for fast responses
- To unload: `ollama stop` (service still runs, model unloaded)

---

## Performance Optimization

### 1. Force CPU Usage (If GPU has issues)
```powershell
$env:OLLAMA_USE_GPU="false"
ollama run llama3
```

### 2. Limit Concurrent Requests
In `.env`:
```env
OLLAMA_MAX_LOADED_MODELS=1
OLLAMA_NUM_PARALLEL=2
```

### 3. Model Quantization (Trade accuracy for speed)
```powershell
# 4-bit quantized (faster, uses less RAM)
ollama pull llama3:4bit
```

---

## Understanding Ollama Architecture

```
┌──────────────────────────────────────────────────┐
│           Nexus (Python FastAPI)                  │
│  ┌─────────────────────────────────────────┐    │
│  │   LangChain / MCP Tools                 │    │
│  └────────────┬────────────────────────────┘    │
│               │ HTTP Request                     │
│               ▼                                  │
│  ┌─────────────────────────────────────────┐    │
│  │   Ollama Python Client                  │    │
│  │   requests.post('localhost:11434')      │    │
│  └────────────┬────────────────────────────┘    │
└───────────────┼──────────────────────────────────┘
                │
                │ HTTP (localhost:11434)
                ▼
┌──────────────────────────────────────────────────┐
│        Ollama Server (Windows Service)            │
│  ┌─────────────────────────────────────────┐    │
│  │   Model Loader (loads llama3)           │    │
│  │   Inference Engine                      │    │
│  │   GPU Scheduler (CUDA)                  │    │
│  └──────────────┬──────────────────────────┘    │
│                 │                                │
│                 ▼                                │
│  ┌─────────────────────────────────────────┐    │
│  │   NVIDIA RTX 4050 (CUDA Cores)          │    │
│  │   Tensor Operations                     │    │
│  └─────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
```

---

## ✅ Verification Checklist

- [ ] Ollama installed successfully
- [ ] `ollama --version` shows version
- [ ] Llama 3 model downloaded (~4.7GB)
- [ ] Interactive chat works (`ollama run llama3`)
- [ ] GPU detected and utilized (check `nvidia-smi`)
- [ ] API endpoint responds (`localhost:11434`)
- [ ] `.env` file updated with Ollama configuration

---

**Next:** [Day 1 - Docker Compose Setup](../infra/README.md)
