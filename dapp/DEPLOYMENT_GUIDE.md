# Live Production Deployment Guide for MedChain Health

Deploying your `dapp/` folder to a live public HTTPS URL allows hospital administrators, doctors, and patients to access the platform on their phones and external computers.

---

## 🚀 Option 1: 30-Second Instant Drag-and-Drop (Netlify Drop)
*No terminal, no command line, 100% free with automatic SSL.*

1. In your browser, open **[app.netlify.com/drop](https://app.netlify.com/drop)**.
2. Open Windows File Explorer to:
   ```
   C:\Users\Dithin Mukkati\java1
   ```
3. Drag the **`dapp`** folder and drop it into the upload circle on the Netlify webpage.
4. Netlify will deploy the site in ~10 seconds and generate a live URL:
   ```text
   https://medchain-health-ledger.netlify.app
   ```
5. You can now open this link on your phone or send it to anyone!

---

## ⚡ Option 2: Free GitHub Pages Deployment
*Permanent, free hosting hosted directly from your GitHub repository.*

1. Push this project to a GitHub repository:
   ```cmd
   git init
   git add dapp/
   git commit -m "Deploy MedChain Health DApp"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo-name>.git
   git push -u origin main
   ```
2. On GitHub, go to your repository **Settings** > **Pages**.
3. Under **Build and deployment**:
   - Source: **Deploy from a branch**
   - Branch: `main` / folder `/dapp`
4. Click **Save**.
5. Your live URL will be:
   ```text
   https://<your-username>.github.io/<your-repo-name>/
   ```

---

## 🌐 Option 3: Free Vercel Deployment

1. Go to **[vercel.com](https://vercel.com/)** and log in with GitHub.
2. Click **Add New Project** and select your repository.
3. Set the **Root Directory** to `dapp`.
4. Click **Deploy**. Vercel will assign a production URL:
   ```text
   https://medchain-health.vercel.app
   ```
Deployment setup updated
