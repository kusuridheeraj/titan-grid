# 🐍 Python Environment Cleanup Guide

**Issue:** Multiple Python versions detected  
**Goal:** Use only Python 3.13.0  
**Impact:** Prevents version conflicts in Nexus module

---

## Current Situation

You have two Python installations:

```
python  → Python 3.13.0 (C:\Users\kusur\AppData\Local\Programs\Python\Python313)
python3 → Python 3.11.9 (C:\msys64\mingw64\bin)
```

---

## Solution: Priority-Based Approach

### ✅ Option 1: Update PATH (Recommended - No Uninstall Needed)

This approach keeps both installations but makes Python 3.13 the default.

#### Steps:

1. **Open Environment Variables**
   - Press `Win + X`
   - Select **"System"**
   - Click **"Advanced system settings"**
   - Click **"Environment Variables"**

2. **Edit User PATH**
   - Under **"User variables for kusur"**, select **`Path`**
   - Click **"Edit"**

3. **Reorder Entries**
   - Find these entries:
     ```
     C:\Users\kusur\AppData\Local\Programs\Python\Python313
     C:\Users\kusur\AppData\Local\Programs\Python\Python313\Scripts
     ```
   - Use the **"Move Up"** button to move them to the **TOP**

4. **Remove MSYS2 Python from PATH** (Optional)
   - Find: `C:\msys64\mingw64\bin`
   - **Option A:** Delete it (if you don't use MSYS2 tools)
   - **Option B:** Move it BELOW the Python 3.13 entries

5. **Click "OK"** on all dialogs

6. **CRITICAL: Restart Terminal**
   - Close ALL PowerShell windows
   - Open a NEW PowerShell window

7. **Verify**
   ```powershell
   python --version    # Should show: Python 3.13.0
   python3 --version   # Should show: Python 3.13.0 (or error if MSYS removed)
   ```

---

### Option 2: Uninstall MSYS2 Python (If Not Needed)

If you don't actively use MSYS2 development tools:

#### Steps:

1. **Open MSYS2 Terminal**
   - Search for "MSYS2" in Start Menu
   - Click "MSYS2 MSYS"

2. **Remove Python Package**
   ```bash
   pacman -R mingw-w64-x86_64-python
   ```
   
3. **Verify Removal**
   ```bash
   which python3  # Should show error
   ```

4. **In PowerShell:**
   ```powershell
   python --version   # Python 3.13.0
   python3 --version  # Should error (MSYS2 version removed)
   ```

---

## Why This Matters for Titan Grid

### Nexus Module Requirements
- **FastAPI:** Requires Python 3.10+
- **MCP SDK:** Requires Python 3.11+
- **Type Hints:** Python 3.13 has better performance

### Potential Issues with Multiple Versions
1. **Virtual Environments:** May use wrong Python interpreter
2. **Package Installation:** `pip` might install to wrong location
3. **IDE Confusion:** VSCode/PyCharm may not detect correct version
4. **Dependency Conflicts:** Some packages require specific versions

---

## Post-Cleanup: Set Up Virtual Environment

Once Python 3.13 is the default:

```powershell
# Navigate to Nexus module
cd c:\PlayStation\assets\titan-grid\nexus

# Create virtual environment with Python 3.13
python -m venv venv

# Activate it
.\venv\Scripts\Activate

# Verify
python --version  # Should show: Python 3.13.0

# Install dependencies (we'll create requirements.txt later)
pip install fastapi uvicorn
```

---

## Verification Script

Create `c:\PlayStation\assets\titan-grid\scripts\check_python.ps1`:

```powershell
Write-Host "=== Python Environment Check ===" -ForegroundColor Cyan

# Check python command
Write-Host "`nPython command:" -ForegroundColor Yellow
Get-Command python | Select-Object -ExpandProperty Source
python --version

# Check python3 command
Write-Host "`nPython3 command:" -ForegroundColor Yellow
Get-Command python3 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source
python3 --version

# Check pip
Write-Host "`nPip command:" -ForegroundColor Yellow
Get-Command pip | Select-Object -ExpandProperty Source
pip --version

# Check PATH priority
Write-Host "`nPATH entries (Python related):" -ForegroundColor Yellow
$env:Path -split ';' | Where-Object { $_ -like '*Python*' -or $_ -like '*msys*' }
```

Run it:
```powershell
.\scripts\check_python.ps1
```

---

## Expected Output (After Cleanup)

```
=== Python Environment Check ===

Python command:
C:\Users\kusur\AppData\Local\Programs\Python\Python313\python.exe
Python 3.13.0

Python3 command:
python3 : The term 'python3' is not recognized...

Pip command:
C:\Users\kusur\AppData\Local\Programs\Python\Python313\Scripts\pip.exe
pip 24.2 from C:\Users\kusur\AppData\Local\Programs\Python\Python313\Lib\site-packages\pip (python 3.13)

PATH entries (Python related):
C:\Users\kusur\AppData\Local\Programs\Python\Python313
C:\Users\kusur\AppData\Local\Programs\Python\Python313\Scripts
```

---

## Troubleshooting

### Still Shows Python 3.11
**Solution:**
```powershell
# Refresh environment variables in current session
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

# Or restart terminal
```

### Both Versions Still Accessible
**This is fine** as long as `python` points to 3.13. You can explicitly call:
```powershell
python    # Uses 3.13
py -3.13  # Explicitly uses 3.13
py -3.11  # Explicitly uses 3.11 (if you need it)
```

---

## ✅ Recommended Path

For Titan Grid development:

1. **Use Option 1** (Reorder PATH) - No uninstall needed
2. Keep MSYS2 in PATH but lower priority
3. Always use virtual environments for Python projects
4. Verify with the check script above

---

**Status After Cleanup:**
- [ ] Python 3.13 is default `python` command
- [ ] PATH reordered or MSYS2 Python removed
- [ ] Terminal restarted
- [ ] Verification script shows correct version

**Next:** [AWS Account Setup](./AWS_SETUP.md)
