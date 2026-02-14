# 🚀 TITAN GRID - Complete Setup Guide

**Last Updated:** 2026-02-05  
**Environment:** Windows 11 | Docker 27.2.0 | Java 17 | Python 3.13

---

## 🎯 PHASE 0: PRE-DEVELOPMENT SETUP

### ✅ Prerequisites Check
You already have:
- ✓ Docker Desktop installed (v27.2.0)
- ✓ Java 17 installed (17.0.11 LTS)
- ✓ Python 3.13.0 installed
- ✓ Git installed

---

## 🐍 STEP 1: PYTHON ENVIRONMENT CLEANUP

### Problem
You currently have two Python installations:
- `python` → Python 3.13.0 (at `C:\Users\kusur\AppData\Local\Programs\Python\Python313`)
- `python3` → Python 3.11.9 (at `C:\msys64\mingw64\bin`)

### Solution: Keep Python 3.13, Remove Python 3.11

#### Option A: Update PATH Environment Variable (Recommended)
1. **Open Environment Variables:**
   - Press `Win + X`, select "System"
   - Click "Advanced system settings"
   - Click "Environment Variables"

2. **Edit PATH:**
   - Under "User variables", select `Path`, click "Edit"
   - **Remove** any entry containing `C:\msys64\mingw64\bin`
   - **Ensure** this entry is at the TOP:
     ```
     C:\Users\kusur\AppData\Local\Programs\Python\Python313
     C:\Users\kusur\AppData\Local\Programs\Python\Python313\Scripts
     ```

3. **Restart Terminal and Verify:**
   ```powershell
   python --version   # Should show: Python 3.13.0
   python3 --version  # Should show error or same as above
   ```

#### Option B: Uninstall MSYS2 Python (If Not Needed)
If you don't actively use MSYS2, you can remove the conflicting Python:
```powershell
# This won't break MSYS2, just removes Python 3.11
pacman -R mingw-w64-x86_64-python
```

---

## ☁️ STEP 2: CLOUD ACCOUNT SETUP

### 2.1 AWS Free Tier (For S3 Storage)

#### Create Account
1. Go to: https://aws.amazon.com/free/
2. Click "Create a Free Account"
3. Fill in:
   - Email address
   - Password
   - AWS Account name (use: `titan-grid-dev`)
4. **Contact Information:**
   - Account type: **Personal**
   - Full name, phone, address
5. **Payment Information:**
   - Add credit/debit card (₹2 will be charged temporarily for verification)
6. **Identity Verification:**
   - Choose "Text message (SMS)" or "Voice call"
   - Enter the verification code
7. **Select Support Plan:**
   - Choose "Basic support - Free"

#### Create S3 Bucket
1. **Log in to AWS Console:** https://console.aws.amazon.com
2. **Navigate to S3:**
   - Search for "S3" in the top search bar
   - Click "Create bucket"
3. **Bucket Configuration:**
   - **Bucket name:** `titan-grid-storage-<your-initials>` (e.g., `titan-grid-storage-dr`)
   - **Region:** `ap-south-1` (Asia Pacific - Mumbai) — lowest latency for India
   - **Block Public Access:** Keep ALL checkboxes CHECKED (security best practice)
   - **Bucket Versioning:** Disabled (to save costs)
   - **Encryption:** Enable (Server-side encryption with Amazon S3 managed keys)
4. Click "Create bucket"

#### Create IAM Access Keys
1. **Navigate to IAM:** Search "IAM" → Click "Users" → "Create user"
2. **User Details:**
   - User name: `titan-grid-service`
   - Click "Next"
3. **Permissions:**
   - Select "Attach policies directly"
   - Search and select: `AmazonS3FullAccess`
   - Click "Next" → "Create user"
4. **Generate Access Keys:**
   - Click on the newly created user
   - Go to "Security credentials" tab
   - Click "Create access key"
   - Select "Application running outside AWS"
   - Click "Next" → "Create access key"
5. **SAVE THESE CREDENTIALS SECURELY:**
   ```
   AWS_ACCESS_KEY_ID=AKIA...
   AWS_SECRET_ACCESS_KEY=wJalrX...
   AWS_REGION=ap-south-1
   S3_BUCKET_NAME=titan-grid-storage-dr
   ```

---

### 2.2 Azure Free Tier (For Entra ID / OAuth)

#### Create Account
1. Go to: https://azure.microsoft.com/free/
2. Click "Start free"
3. **Sign in with Microsoft Account:**
   - Use your existing Outlook/Hotmail/Personal Microsoft account
   - OR create a new one
4. **Verification:**
   - Phone verification
   - Credit card verification (₹2 temporary charge)
5. **Survey:** Complete the quick survey

#### Register Application in Entra ID
1. **Navigate to Azure Portal:** https://portal.azure.com
2. **Open Microsoft Entra ID:**
   - Search "Microsoft Entra ID" in top search bar
   - Click on it
3. **App Registration:**
   - Click "App registrations" in left menu
   - Click "+ New registration"
4. **Registration Details:**
   - **Name:** `Titan Grid API`
   - **Supported account types:** "Accounts in this organizational directory only"
   - **Redirect URI:** Leave blank for now
   - Click "Register"
5. **Copy Essential IDs:**
   - On the Overview page, copy:
     ```
     AZURE_CLIENT_ID=<Application (client) ID>
     AZURE_TENANT_ID=<Directory (tenant) ID>
     ```
6. **Create Client Secret:**
   - Click "Certificates & secrets"
   - Click "+ New client secret"
   - Description: `Titan Grid Secret`
   - Expires: 12 months
   - Click "Add"
   - **IMMEDIATELY COPY THE VALUE** (you can't see it again):
     ```
     AZURE_CLIENT_SECRET=<secret-value>
     ```

---

## 🤖 STEP 3: INSTALL OLLAMA (Local AI)

### Download and Install
1. Go to: https://ollama.com/download/windows
2. Download `OllamaSetup.exe`
3. Run the installer (It will install to `C:\Users\kusur\AppData\Local\Programs\Ollama`)
4. **Verify Installation:**
   ```powershell
   ollama --version
   ```

### Download Llama 3 Model
```powershell
# This will download ~4GB model (optimized for your RTX 4050)
ollama pull llama3

# Test it
ollama run llama3
# Type: "Hello, what can you do?"
# Press Ctrl+D to exit
```

---

## 📦 STEP 4: MAVEN SETUP (Optional - Already Bundled with Spring Boot)

Check if Maven is installed:
```powershell
mvn -version
```

If not installed:
1. Download: https://maven.apache.org/download.cgi
2. Extract to `C:\Program Files\Apache\Maven`
3. Add to PATH: `C:\Program Files\Apache\Maven\bin`

---

## 🔐 STEP 5: CREATE ENVIRONMENT SECRETS FILE

Create a `.env` file in the project root to store all secrets:

```powershell
# Navigate to project root
cd c:\PlayStation\assets\titan-grid

# Create .env file (we'll populate it as we go)
New-Item -ItemType File -Path .env -Force
```

**CRITICAL:** Add `.env` to `.gitignore` immediately:
```powershell
echo ".env" >> .gitignore
```

Populate `.env` with your credentials:
```env
# AWS Credentials
AWS_ACCESS_KEY_ID=your-access-key-here
AWS_SECRET_ACCESS_KEY=your-secret-key-here
AWS_REGION=ap-south-1
S3_BUCKET_NAME=titan-grid-storage-dr

# Azure Credentials
AZURE_CLIENT_ID=your-client-id-here
AZURE_TENANT_ID=your-tenant-id-here
AZURE_CLIENT_SECRET=your-client-secret-here

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=titan_redis_2026

# Postgres Configuration
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_USER=titan_admin
POSTGRES_PASSWORD=titan_postgres_2026
POSTGRES_DB=titan_grid

# Vault Configuration
VAULT_ADDR=http://localhost:8200
VAULT_TOKEN=dev-root-token-12345
```

---

## ✅ VERIFICATION CHECKLIST

Before proceeding to Day 1, ensure:
- [ ] Docker Desktop is running
- [ ] Python 3.13 is the default `python` command
- [ ] AWS S3 bucket created and credentials saved
- [ ] Azure Entra ID app registered and credentials saved
- [ ] Ollama installed and Llama3 model downloaded
- [ ] `.env` file created with all credentials
- [ ] `.env` added to `.gitignore`

---

## 🎯 NEXT STEPS

Once all setup is complete, you're ready for **Day 1: Foundation (Docker Compose)**.

**Remember:** This setup is a one-time effort. The quality of this foundation determines the smoothness of your 21-day journey.

---

## 🆘 TROUBLESHOOTING

### Python Still Shows 3.11
```powershell
# Restart terminal after PATH changes
# Or manually specify:
python.exe --version
```

### Docker Not Starting
- Ensure WSL2 is installed: `wsl --status`
- Enable "Use the WSL 2 based engine" in Docker Desktop settings

### Ollama GPU Not Detected
- Update NVIDIA drivers: https://www.nvidia.com/download/index.aspx
- Ollama automatically uses GPU if available; check with: `ollama run llama3 --verbose`
