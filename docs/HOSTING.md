# Minimum-Cost Hosting Options for 1Guntha

This document suggests low-cost or free hosting providers suitable for the 1Guntha stack: **Angular** (frontend), **Spring Boot** (backend), and **PostgreSQL**.

---

## Summary

| Option | Cost | Best for |
|--------|------|----------|
| **Render** | Free tier (with limits) | Easiest free start; backend + DB + static frontend |
| **Fly.io** | Free tier (with limits) | Free Postgres + app; good for small projects |
| **Oracle Cloud Always Free** | $0 (always free) | Lowest long-term cost; you manage VMs |
| **Railway** | Trial credits, then pay-as-you-go | Simple deploy; good DX |
| **DigitalOcean** | ~$6–12/month | Predictable, simple paid option |

---

## 1. Free / Near-Free Options

### Render

- **Cost:** Free tier for web service + PostgreSQL (1 GB DB).
- **Stack:** Deploy Spring Boot as a Web Service, PostgreSQL as a database, Angular as a Static Site (build from repo).
- **Limits:** Free web service spins down after ~15 min inactivity; cold start ~1 min. Free PostgreSQL: 1 GB, no expiration (as of current docs).
- **Pros:** One platform for frontend, backend, and DB; no credit card for free tier; custom domain + TLS.
- **Cons:** Cold starts; free tier not intended for production traffic.
- **Docs:** [render.com](https://render.com), [Deploy for Free](https://docs.render.com/free)

**Fit:** Good for demos, side projects, and testing with minimal cost.

---

### Fly.io

- **Cost:** Free tier includes compute and a free Postgres offering (e.g. 3 GB storage for small projects).
- **Stack:** Deploy Spring Boot as an app; add a Postgres cluster; serve Angular as static assets from the same app or a separate small machine.
- **Pros:** Free Postgres; global regions; no spin-down like Render.
- **Cons:** Credit card required; free resources are limited; you need to configure Docker/build.
- **Docs:** [fly.io](https://fly.io), [Free Postgres](https://fly.io/blog/free-postgres/), [Pricing](https://fly.io/docs/about/pricing/)

**Fit:** Good if you want always-on free tier without Render’s sleep behavior.

---

### Oracle Cloud Always Free

- **Cost:** $0 for Always Free resources (no time limit).
- **Includes:** 2 AMD VMs (1/8 OCPU, 1 GB RAM each), Always Free PostgreSQL option, storage, and networking.
- **Stack:** Use one VM for Spring Boot + Angular (e.g. Nginx for Angular + Java for backend), or split; use the other VM or managed Postgres for PostgreSQL.
- **Pros:** Truly free long-term; full control; good for learning and light production.
- **Cons:** You manage OS, Java, Nginx, Postgres; OCI UI has a learning curve; card required for signup (no charge if you stay in Always Free).
- **Docs:** [Oracle Cloud Free Tier](https://www.oracle.com/cloud/free/), [Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)

**Fit:** Best for minimum long-term cost when you’re okay with self-management.

---

### Railway

- **Cost:** New accounts get a one-time trial credit (e.g. $5, 30 days); after that, pay-as-you-go.
- **Stack:** Deploy backend from GitHub; add PostgreSQL plugin; host Angular as a static site or separate service.
- **Pros:** Simple GitHub deploy; Postgres with `DATABASE_URL`; good developer experience.
- **Cons:** No permanent free tier; after trial, even light usage can be a few dollars per month.
- **Docs:** [railway.app](https://railway.app), [PostgreSQL on Railway](https://docs.railway.com/guides/postgresql)

**Fit:** Good for quick demos and early development; then move to a fixed-cost provider if you want predictability.

---

## 2. Lowest-Cost Paid Options (~$5–12/month)

### DigitalOcean

- **Cost:** Droplets from **$4–6/month** (0.5–1 GB RAM); **App Platform** can run app + static site but is typically **$5–12+/month** depending on services.
- **Stack:** Option A: One Droplet ($6–12/month) — install JDK 17, Postgres, Nginx; run Spring Boot + serve Angular. Option B: App Platform (managed) for app + static site + managed DB (higher cost).
- **Pros:** Predictable pricing; clear docs; managed DB available.
- **Cons:** $4 droplet is tight for Spring Boot + Postgres; $6 (1 GB) is a practical minimum.

**Fit:** Best “first paid” option when you want a simple, predictable bill.

---

### Other Budget VPS / PaaS

- **Vultr / Linode:** Similar to DigitalOcean (~$5–6/month for small VPS); you manage everything.
- **Supabase:** Free tier for Postgres + APIs; you still need a host for Spring Boot (e.g. Render, Fly.io, or a VPS).
- **Hostinger / Cloudzy:** Sometimes cheaper VPS or managed DB; check current pricing and regions.

---

## 3. Recommended Approach by Goal

| Goal | Suggestion |
|------|------------|
| **Zero cost, minimal setup** | **Render** (free web + free Postgres + static site). Accept cold starts. |
| **Zero cost, no spin-down** | **Fly.io** (free app + free Postgres) or **Oracle Always Free** (VMs + Postgres). |
| **Lowest long-term cost** | **Oracle Cloud Always Free** (self-managed on 1–2 VMs + Postgres). |
| **Simple and cheap paid** | **DigitalOcean** Droplet ($6–12/month) or **Render** paid tier when you need no cold starts. |
| **Quick demo / trial** | **Railway** (trial credit) or **Render** (free). |

---

## 4. Deployment Outline for This Project

1. **Backend (Spring Boot)**  
   - Build: `mvn -DskipTests package` → `jar` in `target/`.  
   - Run with `java -jar …` or use a Dockerfile (as in this repo).  
   - Set env: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `FRONTEND_URL`.

2. **Database**  
   - Use a managed Postgres (Render, Fly.io, Railway, DigitalOcean) or install Postgres on a VPS (Oracle, DigitalOcean).  
   - Run Flyway migrations on first deploy (or ensure DB is created and migrations run from the app).

3. **Frontend (Angular)**  
   - Build: `npm run build` (output in `dist/`).  
   - Serve the static output via:  
     - **Render / Netlify / Vercel:** Static site linked to the same repo or `dist/` artifact.  
     - **VPS / Fly.io:** Nginx (or the same server as the backend) serving the built files.  
   - Set `apiUrl` in environment to your backend URL (e.g. `https://your-backend.onrender.com/api`).

4. **CORS**  
   - In Spring Boot, set `FRONTEND_URL` (or allowed origins) to your frontend URL (e.g. `https://your-app.onrender.com` or custom domain).

---

## 5. Quick Links

- [Render](https://render.com) — Free tier: web service + Postgres + static site  
- [Fly.io](https://fly.io) — Free tier: app + Postgres  
- [Oracle Cloud Free Tier](https://www.oracle.com/cloud/free/) — Always Free VMs + Postgres  
- [Railway](https://railway.app) — Trial then pay-as-you-go  
- [DigitalOcean](https://www.digitalocean.com/pricing) — Droplets and App Platform  

For run instructions (local and Docker), see [RUNNING.md](RUNNING.md).
