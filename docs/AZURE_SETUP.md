# Azure AD (Entra ID) Setup - Complete Guide

**Service:** Microsoft Entra ID (formerly Azure Active Directory)  
**Purpose:** OAuth 2.0 authentication for Cryptex API  
**Cost:** FREE (Permanent, up to 50,000 users)

---

## Step 1: Create Azure Account

### 1.1 Sign Up
1. Navigate to: https://azure.microsoft.com/en-us/free/
2. Click **"Start free"**
3. **Sign in options:**
   - **Option A:** Use existing Microsoft account (Outlook, Hotmail, Xbox)
   - **Option B:** Create new Microsoft account

4. If creating new account:
   - Email: Your personal email or create `yourname@outlook.com`
   - Password: Strong password
   - Verify via email

### 1.2 About You
1. **Country/Region:** India
2. **First name & Last name**
3. **Email address:** Your contact email
4. **Phone number:** +91XXXXXXXXXX

### 1.3 Identity Verification
1. **Phone verification:**
   - Enter your mobile number
   - Click **"Text me"** or **"Call me"**
   - Enter the verification code
2. Click **"Verify code"**

### 1.4 Payment Information
1. **Credit/Debit Card:** Add card details
   - **Important:** ₹2 will be charged for verification (refunded)
   - You won't be charged unless you explicitly upgrade
2. Check "I agree to the customer agreement"
3. Click **"Sign up"**

### 1.5 Survey
1. Complete the quick survey (optional but recommended)
2. Click **"Continue"**

🎉 **Azure Account Created!** Welcome to Azure Portal.

---

## Step 2: Navigate to Microsoft Entra ID

### 2.1 Access Azure Portal
1. Go to: https://portal.azure.com/
2. Sign in with your Microsoft account

### 2.2 Find Microsoft Entra ID
1. **Method A:** Search bar
   - Type: **"Microsoft Entra ID"** in the top search bar
   - Click on it

2. **Method B:** Menu
   - Click the "hamburger menu" (☰) on the top left
   - Scroll down and click **"Microsoft Entra ID"**

---

## Step 3: Register Application

### 3.1 Start Registration
1. In Microsoft Entra ID, click **"App registrations"** (left sidebar)
2. Click **"+ New registration"**

### 3.2 Configure Application
1. **Name:** `Titan Grid API`
2. **Supported account types:**
   - Select **"Accounts in this organizational directory only (Single tenant)"**
3. **Redirect URI:**
   - Leave **blank** for now (we'll add it later for web testing)
4. Click **"Register"**

✅ **App Registered!** You'll be taken to the app's Overview page.

---

## Step 4: Copy Essential Identifiers

### 4.1 Get Application (Client) ID
1. On the **Overview** page, you'll see:
   ```
   Application (client) ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
   ```
2. Click the **"Copy to clipboard"** icon
3. **Save this to your `.env` file:**
   ```env
   AZURE_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
   ```

### 4.2 Get Directory (Tenant) ID
1. Just below the Client ID, you'll see:
   ```
   Directory (tenant) ID: yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy
   ```
2. Click the **"Copy to clipboard"** icon
3. **Save this to your `.env` file:**
   ```env
   AZURE_TENANT_ID=yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy
   ```

---

## Step 5: Create Client Secret

### 5.1 Navigate to Secrets
1. In the left sidebar, click **"Certificates & secrets"**
2. Click the **"Client secrets"** tab
3. Click **"+ New client secret"**

### 5.2 Configure Secret
1. **Description:** `Titan Grid Production Secret`
2. **Expires:** Select **"12 months"**
   - You'll need to rotate this after 1 year
3. Click **"Add"**

### 5.3 Copy Secret Value
1. **🚨 CRITICAL:** The secret value will ONLY be shown ONCE
2. Under the **"Value"** column, you'll see:
   ```
   ************************.************************
   ```
3. Click the **"Copy to clipboard"** icon
4. **Immediately save this to your `.env` file:**
   ```env
   AZURE_CLIENT_SECRET=YourSecretValueHere~....
   ```

⚠️ **WARNING:** If you navigate away without copying, you'll need to create a new secret.

---

## Step 6: Configure API Permissions (Optional)

For basic authentication, default permissions are sufficient. However, for production:

### 6.1 Add Microsoft Graph Permissions
1. Click **"API permissions"** (left sidebar)
2. Click **"+ Add a permission"**
3. Select **"Microsoft Graph"**
4. Select **"Delegated permissions"**
5. Search and add:
   - `User.Read` (already added by default)
   - `email`
   - `profile`
6. Click **"Add permissions"**

### 6.2 Grant Admin Consent
1. Click **"Grant admin consent for [Your Tenant]"**
2. Click **"Yes"**
3. You should see green checkmarks ✅ under "Status"

---

## Step 7: Configure Redirect URIs (For Testing)

### 7.1 Add Redirect URI
1. Click **"Authentication"** (left sidebar)
2. Under **"Platform configurations"**, click **"+ Add a platform"**
3. Select **"Web"**
4. **Redirect URIs:**
   ```
   http://localhost:8081/login/oauth2/code/azure
   ```
5. **Front-channel logout URL:** Leave blank
6. **Implicit grant:** Leave unchecked
7. Click **"Configure"**

---

## Step 8: Update Application `.env` File

Open `c:\PlayStation\assets\titan-grid\.env` and add:

```env
# Azure Credentials
AZURE_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
AZURE_TENANT_ID=yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy
AZURE_CLIENT_SECRET=YourSecretValueHere~....
AZURE_AUTHORITY=https://login.microsoftonline.com/yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy
```

---

## Step 9: Test with Postman (Optional)

### 9.1 Get Access Token
```bash
POST https://login.microsoftonline.com/{YOUR_TENANT_ID}/oauth2/v2.0/token

Body (x-www-form-urlencoded):
client_id: {YOUR_CLIENT_ID}
client_secret: {YOUR_CLIENT_SECRET}
scope: {YOUR_CLIENT_ID}/.default
grant_type: client_credentials
```

### 9.2 Expected Response
```json
{
  "token_type": "Bearer",
  "expires_in": 3599,
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGci..."
}
```

---

## Security Best Practices

### ✅ DO:
- Rotate client secrets every 12 months
- Use separate apps for dev/staging/prod
- Enable MFA on your Azure account
- Use Managed Identities when deploying to Azure
- Store secrets in Azure Key Vault for production

### ❌ DON'T:
- Commit `.env` to Git
- Share client secrets via email/chat
- Use same secret across multiple environments
- Grant excessive API permissions

---

## Troubleshooting

### Issue: "Invalid client secret"
- **Solution:** 
  - Verify you copied the VALUE, not the Secret ID
  - Create a new secret if lost

### Issue: "AADSTS700016: UnauthorizedClient"
- **Solution:**
  - Check that Tenant ID matches
  - Verify redirect URI is configured correctly
  - Ensure app is in the correct tenant

### Issue: "Insufficient privileges"
- **Solution:**
  - Grant admin consent for API permissions
  - Check that your account has admin rights

---

## Understanding the OAuth Flow

```
┌─────────┐                                   ┌──────────┐
│  User   │                                   │  Cryptex │
│ Browser │                                   │   API    │
└────┬────┘                                   └────┬─────┘
     │                                              │
     │  1. GET /upload                              │
     │─────────────────────────────────────────────>│
     │                                              │
     │  2. 302 Redirect to Azure AD                 │
     │<─────────────────────────────────────────────│
     │                                              │
     │  3. Enter email/password    ┌────────────┐  │
     │────────────────────────────>│  Azure AD  │  │
     │                             └─────┬──────┘  │
     │  4. Authorization code            │         │
     │<──────────────────────────────────┘         │
     │                                              │
     │  5. POST /login/oauth2 (code)                │
     │─────────────────────────────────────────────>│
     │                                              │
     │  6. Exchanges code for token at Azure AD     │
     │                                              │
     │  7. 200 OK + JWT Cookie                      │
     │<─────────────────────────────────────────────│
     │                                              │
     │  8. Upload file with JWT                     │
     │─────────────────────────────────────────────>│
```

---

## ✅ Verification Checklist

- [ ] Azure account created
- [ ] Microsoft Entra ID accessed
- [ ] App registration created: `Titan Grid API`
- [ ] Client ID copied to `.env`
- [ ] Tenant ID copied to `.env`
- [ ] Client secret created and copied to `.env`
- [ ] Redirect URI configured (optional)
- [ ] (Optional) Tested token endpoint with Postman

---

**Next:** [Ollama Installation](./OLLAMA_SETUP.md)
