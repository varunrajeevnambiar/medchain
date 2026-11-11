# Real Inbound/Outbound Email Integration Guide for MedChain

The website is wired with the official **EmailJS SDK** to dispatch real password reset emails into actual Gmail/Outlook/Yahoo inboxes.

---

## 2-Minute Free Activation Steps:

1. **Create Free Account**:
   - Go to [emailjs.com](https://www.emailjs.com/) and sign up (Free tier includes 200 real emails/month).

2. **Connect an Email Service**:
   - In the EmailJS dashboard, click **Email Services** > **Add New Service**.
   - Select **Gmail** (or Outlook/Yahoo) and click **Connect Account**.
   - Note down your **Service ID** (e.g. `service_medchain`).

3. **Create the Password Reset Template**:
   - Click **Email Templates** > **Create New Template**.
   - Set Subject: `Reset your MedChain Health password`
   - In the template body, type:
     ```text
     Hello,

     A request has been received to reset the password for your MedChain account ({{user_email}}).

     Please click the link below to set your new password:
     {{reset_link}}

     If you did not request this, please ignore this email.
     ```
   - Set the **To Email** field to: `{{to_email}}`
   - Click **Save** and note your **Template ID** (e.g. `template_pw_reset`).

4. **Get Your Public Key**:
   - Go to **Account** > **API Keys** and copy your **Public Key** (e.g. `user_abc123xyz`).

5. **Paste into `c:\Users\Dithin Mukkati\java1\dapp\app.js`**:
   At line 339 of `dapp/app.js`, paste your keys:
   ```javascript
   const EMAILJS_CONFIG = {
     publicKey: "YOUR_PUBLIC_KEY",      // paste your key here
     serviceId: "YOUR_SERVICE_ID",      // paste your service ID here
     templateId: "YOUR_TEMPLATE_ID"     // paste your template ID here
   };
   ```

Once pasted, clicking **"Send Reset Link"** sends an email directly to the recipient's inbox, while displaying the clean notification:
> **Password reset email sent to {email address}**
