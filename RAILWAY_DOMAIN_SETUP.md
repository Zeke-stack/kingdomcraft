# Railway Domain Configuration Guide

## Custom Domain Setup for KingdomCraft Server

### Issue: Custom domain doesn't work on the server

If your custom domain isn't connecting to the Minecraft server, follow these steps:

## 🔧 Railway Configuration

### 1. Check Port Mapping in Railway Dashboard

1. Go to your Railway project
2. Click on the **KingdomCraft** service
3. Go to **Settings**
4. Under "Networking" section:
   - Port **25565** should be exposed (TCP)
   - Port **24454** should be exposed (UDP) for voice chat

### 2. Set Custom Domain

1. In Railway dashboard, select your service
2. Go to **Networking** tab
3. Look for **Public URL** section
4. Click "Add Public URL"
5. Select port **25565** (Minecraft)
6. Click on the domain to connect your custom domain

### 3. Configure DNS Records

Add these DNS records to your domain provider:

**Option A: Direct Connection (Recommended)**
```
Type:  CNAME
Name:  minecraft (or whatever subdomain you want)
Value: <railway-public-url>
TTL:   3600 or Auto
```

**Option B: Using A Record**
```
Type:  A
Name:  minecraft
Value: <railway-ip-address>
TTL:   3600 or Auto
```

### 4. Verify DNS Propagation

```bash
# Check if DNS is working
nslookup minecraft.yourdomain.com
# or
dig minecraft.yourdomain.com
```

## 🎮 Minecraft Connection

### From Minecraft Launcher:

1. Click "Add Server"
2. Server Name: `KingdomCraft`
3. Server Address: `minecraft.yourdomain.com:25565`
4. Done!

### Testing Connection:

```bash
# Test from command line (Windows)
ping minecraft.yourdomain.com

# Test port connectivity (Windows)
Test-NetConnection -ComputerName minecraft.yourdomain.com -Port 25565

# Linux
nc -zv minecraft.yourdomain.com 25565
```

## 📋 Troubleshooting Checklist

- [ ] DNS records are added to domain provider
- [ ] DNS has propagated (check with nslookup)
- [ ] Railway service is running (check deployment logs)
- [ ] Port 25565 is exposed in Railway
- [ ] server-ip=0.0.0.0 in server.properties
- [ ] server-port=25565 in server.properties
- [ ] Firewall allows port 25565 (Railway handles this)
- [ ] Custom domain is connected in Railway dashboard

## 🌐 Railway Public URLs

If you don't have a custom domain, Railway provides a free public URL:

1. Go to Railway dashboard
2. Select KingdomCraft service
3. Look for "Public URL" in Networking section
4. Use that URL directly in Minecraft launcher
5. Format: `your-project-xxx.railway.app:25565`

## 🔄 Common Issues & Solutions

### "Cannot reach server" / "Connection timed out"
- Check Railway deployment status (ensure service is running)
- Verify port 25565 is exposed in Railway settings
- Wait 5-10 minutes after adding domain (DNS propagation)
- Try using Railway's direct public URL first

### "Connection refused"
- Check server is running (`railway logs` in CLI)
- Verify server.properties has correct port
- Make sure /data volume is mounted correctly

### "Cannot resolve hostname"
- DNS hasn't propagated yet (wait 5-30 minutes)
- Check DNS records in domain provider
- Use `nslookup` to verify DNS is working
- Try Railway's direct public URL as temporary solution

### Domain works for HTTP but not Minecraft
- This is normal! Minecraft needs TCP port 25565, not HTTP (port 80)
- Make sure you're exposing port 25565, not port 80
- DNS CNAME should point to Railway app, not web domain

## 🔐 SSL/TLS Notes

Minecraft Java Edition does NOT use SSL/TLS for the protocol. Do not configure HTTPS redirects for the Minecraft port. This will break connections.

Only configure HTTPS if you have a separate web portal on a different port.

## 📞 Getting Help

**If domain still doesn't work:**

1. Check Railway logs: `railway logs`
2. Verify server.properties: `server-ip=0.0.0.0` and `server-port=25565`
3. Test with Railway's direct public URL first
4. Check DNS with: `nslookup minecraft.yourdomain.com`
5. Contact Railway support if deployment has issues

---

**Made for KingdomCraft Server** ⚔️
