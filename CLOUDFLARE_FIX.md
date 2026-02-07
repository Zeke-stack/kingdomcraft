# Cloudflare DNS Fix for Minecraft Server

## Issue: 502 Bad Gateway from Cloudflare API

If you're seeing this error: `API Request Failed: GET /api/v4/zones/.../load_balancers (502)`

**Root Cause**: Cloudflare's proxy is incompatible with Minecraft's protocol.

## ✅ Solution: Disable Cloudflare Proxy

### In Cloudflare Dashboard:

1. Log into **Cloudflare.com**
2. Select your **domain**
3. Go to **DNS** tab
4. Find your Minecraft record (e.g., `minecraft.yourdomain.com`)
5. Look for the **Proxy Status** column
6. Click the icon - should show:
   - 🟠 **Proxied** (currently - WRONG for Minecraft)
   - 🟡 **DNS Only** (what we want)
7. Change it to **DNS Only** (gray cloud icon)
8. Save changes

### What This Does:

- **Proxied** (orange): Routes through Cloudflare - breaks Minecraft protocol
- **DNS Only** (gray): Direct connection to Railway - works for Minecraft ✅

## 🔄 DNS Records to Set

In Cloudflare DNS settings:

```
Type:    CNAME
Name:    minecraft (or your subdomain)
Content: your-project-xxx.railway.app
TTL:     Auto
Proxy:   DNS Only (gray cloud)
Status:  Active
```

## ⏱️ Wait for Changes

After changing to "DNS Only":
1. Wait 5-10 minutes for changes to propagate
2. Try connecting in Minecraft
3. Use this to test: `nslookup minecraft.yourdomain.com`

## 🚀 Alternative: Use Railway's Domain

If you don't want to deal with Cloudflare:

1. **Railway Dashboard** → Your KingdomCraft service
2. **Networking** tab
3. Use the free Railway URL directly: `your-project-xxx.railway.app`
4. Connect to: `your-project-xxx.railway.app:25565` in Minecraft

No Cloudflare needed!

## 🔐 Why Cloudflare Proxying Doesn't Work

Cloudflare's proxy is designed for HTTP/HTTPS web traffic. Minecraft uses:
- Raw TCP protocol (not HTTP)
- Custom binary data format
- Direct socket connection

Proxying through Cloudflare breaks this connection. That's why we use "DNS Only" - it just resolves the domain name but doesn't proxy the actual connection.

## ✅ Checklist

- [ ] Cloudflare record set to "DNS Only" (gray cloud)
- [ ] CNAME points to Railway URL
- [ ] Waited 5+ minutes for propagation
- [ ] Try connecting with custom domain
- [ ] If still issues, use Railway's direct domain

## 📞 Still Having Issues?

If it still doesn't work after setting to "DNS Only":

1. Test with Railway's direct URL first: `your-project-xxx.railway.app:25565`
2. If that works - it's a DNS propagation issue, wait longer
3. If that fails - check Railway service is running (deployment logs)

---

**Key Takeaway**: Always use "DNS Only" for Minecraft servers through Cloudflare! 🎮
