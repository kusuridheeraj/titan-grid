# AWS Account Setup - Complete Guide

**Service:** Amazon S3 (Simple Storage Service)  
**Purpose:** Encrypted file storage for Cryptex module  
**Cost:** FREE (12 months, 5GB storage)

---

## Step 1: Create AWS Account

### 1.1 Sign Up
1. Navigate to: https://aws.amazon.com/free/
2. Click **"Create a Free Account"**
3. **Root user email address:** Use your personal email
4. **AWS account name:** `titan-grid-dev-<your-initials>` (e.g., `titan-grid-dev-dr`)
5. Click **"Verify email address"**
6. Check your email and copy the verification code
7. Paste the code and click **"Verify"**

### 1.2 Create Password
1. Set a strong password (min 8 characters, mix of upper/lower/numbers/symbols)
2. **Recommendation:** Use a password manager (Bitwarden, 1Password)

### 1.3 Contact Information
1. **Account type:** Select **Personal**
2. Fill in:
   - Full Name
   - Phone Number (with country code: +91 for India)
   - Country: India
   - Address, City, State, Postal Code
3. Check "I have read and agree to the AWS Customer Agreement"
4. Click **"Continue"**

### 1.4 Payment Information
1. **Credit/Debit Card:** Add your card details
   - **Important:** AWS will charge ₹2 for verification (refunded within 3-5 days)
   - This does NOT subscribe you to any paid services
2. Click **"Verify and Add"**

### 1.5 Identity Verification
1. Choose verification method:
   - **Option A:** Text message (SMS) - Recommended
   - **Option B:** Voice call
2. Enter the 4-digit verification code
3. Click **"Continue"**

### 1.6 Select Support Plan
1. Choose **"Basic support - Free"**
2. Click **"Complete sign up"**

🎉 **Account Created!** You'll receive a confirmation email.

---

## Step 2: Create S3 Bucket

### 2.1 Sign In to AWS Console
1. Go to: https://console.aws.amazon.com/
2. **Email:** Root user email
3. **Password:** Your account password
4. Click **"Sign in"**

### 2.2 Navigate to S3
1. In the top search bar, type **"S3"**
2. Click **"S3"** under Services

### 2.3 Create Bucket
1. Click **"Create bucket"** (orange button)

2. **Bucket Configuration:**
   ```
   Bucket name: titan-grid-storage-<your-initials>
   Example: titan-grid-storage-dr
   ```
   - **Important:** Bucket names must be globally unique
   - Use lowercase letters, numbers, and hyphens only
   - Must be between 3-63 characters

3. **AWS Region:** Select **Asia Pacific (Mumbai) ap-south-1**
   - This gives you lowest latency from India
   - All S3 operations will be in this region

4. **Object Ownership:**
   - Leave default: **"ACLs disabled (recommended)"**

5. **Block Public Access settings:**
   - ✅ **Keep ALL four checkboxes CHECKED**
   - This ensures your files are private by default
   - Never uncheck these unless absolutely necessary

6. **Bucket Versioning:**
   - Select **"Disable"** (to save costs)

7. **Default encryption:**
   - Select **"Enable"**
   - Encryption type: **"Server-side encryption with Amazon S3 managed keys (SSE-S3)"**
   - This is FREE and automatic

8. Click **"Create bucket"**

✅ **Bucket Created!** You should see it in your S3 bucket list.

---

## Step 3: Create IAM User (For Programmatic Access)

### 3.1 Navigate to IAM
1. In the top search bar, type **"IAM"**
2. Click **"IAM"** under Services

### 3.2 Create User
1. Click **"Users"** in the left sidebar
2. Click **"Create user"** (orange button)

3. **User details:**
   ```
   User name: titan-grid-service
   ```
   - No need to check "Provide user access to AWS Management Console"
4. Click **"Next"**

### 3.3 Set Permissions
1. Select **"Attach policies directly"**
2. In the search box, type: **"AmazonS3FullAccess"**
3. Check the box next to **AmazonS3FullAccess**
4. Click **"Next"**
5. Click **"Create user"**

✅ **User Created!**

### 3.4 Generate Access Keys
1. Click on the newly created user **"titan-grid-service"**
2. Click the **"Security credentials"** tab
3. Scroll down to **"Access keys"**
4. Click **"Create access key"**

5. **Use case:**
   - Select **"Application running outside AWS"**
   - Click **"Next"**

6. **Description tag (optional):**
   - Enter: `Titan Grid Cryptex Module`
   - Click **"Create access key"**

7. **🚨 CRITICAL - Save These Credentials:**
   ```
   Access key ID: AKIA.....................
   Secret access key: wJalrX.............................................
   ```
   - Click **"Download .csv file"** and save it securely
   - **WARNING:** You'll never be able to see the secret key again!

8. Click **"Done"**

---

## Step 4: Configure Application

### 4.1 Update `.env` File
Open `c:\PlayStation\assets\titan-grid\.env` and update:

```env
# AWS Credentials
AWS_ACCESS_KEY_ID=AKIA.....................
AWS_SECRET_ACCESS_KEY=wJalrX.............................................
AWS_REGION=ap-south-1
S3_BUCKET_NAME=titan-grid-storage-dr
```

### 4.2 Test Connection (Optional)
Install AWS CLI:
```powershell
winget install Amazon.AWSCLI
```

Verify access:
```powershell
aws configure set aws_access_key_id YOUR_ACCESS_KEY
aws configure set aws_secret_access_key YOUR_SECRET_KEY
aws configure set region ap-south-1

# Test S3 access
aws s3 ls
```

You should see your bucket listed.

---

## Security Best Practices

### ✅ DO:
- Rotate access keys every 90 days
- Use environment variables, never hardcode credentials
- Enable MFA (Multi-Factor Authentication) on root account
- Use least privilege (only grant necessary permissions)

### ❌ DON'T:
- Commit `.env` file to Git (it's in `.gitignore`)
- Share access keys via email/Slack
- Use root account credentials in application code
- Disable public access blocks unless absolutely necessary

---

## Troubleshooting

### Issue: "Bucket name already exists"
- **Solution:** Try a different suffix: `titan-grid-storage-dr-2026`

### Issue: "Access Denied" when testing
- **Verify:**
  - Access keys are correct
  - User has `AmazonS3FullAccess` policy
  - Bucket name matches exactly (case-sensitive)
  - Region is set to `ap-south-1`

### Issue: "Invalid security credentials"
- **Solution:** Regenerate access keys (delete old, create new)

---

## ✅ Verification Checklist

- [ ] AWS account created
- [ ] S3 bucket created: `titan-grid-storage-<initials>`
- [ ] IAM user created: `titan-grid-service`
- [ ] Access keys generated and saved securely
- [ ] `.env` file updated with credentials
- [ ] (Optional) AWS CLI tested successfully

---

**Next:** [Azure AD Setup](./AZURE_SETUP.md)
