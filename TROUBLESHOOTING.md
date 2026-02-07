# Complete Troubleshooting Guide

## Step 1: Test Railway Server Directly (Bypass Cloudflare)

### Get Railway's Direct URL
1. Go to **Railway Dashboard**
2. Select **KingdomCraft** service
3. Click **Networking** tab
4. Copy the **Public URL** (format: `your-project-xxx.railway.app`)

### Test Direct Connection in Minecraft
1. Open Minecraft Launcher
2. **Add Server**
3. Server Name: `KingdomCraft Test`
4. Server Address: `your-project-xxx.railway.app:25565`
5. Try to connect

### Results:
- **✅ Works**: Server is fine, issue is with Cloudflare DNS
- **❌ Fails**: Server problem, check step 2

---

## Step 2: Check Railway Service Status

### In Railway Dashboard:
1. Go to your **KingdomCraft** service
2. Click **Deployments** tab
3. Check the latest deployment status:
   - 🟢 **Success** = Server running
   - 🔴 **Failed** = Server didn't start (check logs)
   - 🟡 **Building** = Still compiling

### View Server Logs:
1. Still in **KingdomCraft** service
2. Click **Logs** tab
3. Look for errors like:
   ```
   ERROR
   Exception
   Failed to bind port
   ```

### If There's an Error:
- Note the full error message
- Could be plugin compilation issue
- Check if plugins have syntax errors

---

## Step 3: Check Cloudflare DNS Records

### Verify DNS Setup:
1. Go to **Cloudflare Dashboard**
2. Select your **domain**
3. **DNS** tab
4. Look for your Minecraft record (e.g., `minecraft.yourdomain.com`)

### Should Show:
```
Type:    CNAME
Name:    minecraft
Content: your-project-xxx.railway.app
Status:  ✅ Active
Proxy:   DNS only (gray cloud)
TTL:     Auto
```

### Check for Issues:
- ❌ Red X = DNS is broken
- ❌ Orange cloud = Still proxied (should be gray)
- ❌ CNAME Content is wrong = Points to wrong server
- ✅ Gray cloud + correct content = Should work

---

## Step 4: Clear DNS Cache & Wait

If DNS looks correct but still not working:

1. **Clear Cloudflare Cache**:
   - Cloudflare Dashboard → Caching → Configuration
   - Click "Purge Everything"
   - Wait 2-5 minutes

2. **Local DNS Cache**:
   - Windows: `ipconfig /flushdns`
   - Mac: `sudo dscacheutil -flushcache`
   - Linux: `sudo systemctl restart systemd-resolved`

3. **Wait for Propagation**:
   - DNS changes take 5-30 minutes
   - If changed recently, wait at least 15 minutes

---

## Step 5: Test DNS Resolution

### Test if DNS is Working:

**Windows:**
```powershell
nslookup minecraft.yourdomain.com
# Should show Railway IP address

# Or test connection directly:
Test-NetConnection -ComputerName minecraft.yourdomain.com -Port 25565
```

**Mac/Linux:**
```bash
nslookup minecraft.yourdomain.com
dig minecraft.yourdomain.com
nc -zv minecraft.yourdomain.com 25565
```

### Expected Output:
```
Name:    minecraft.yourdomain.com
Address: xxx.xxx.xxx.xxx  (Railway IP)
```

### If Not Working:
- DNS hasn't propagated yet - **wait longer**
- Record is wrong - **check Cloudflare settings again**
- Cloudflare API issue - **try in 5-10 minutes**

---

## Step 6: Check for Cloudflare Issues

### Cloudflare API Error (502)
This specific error means Cloudflare's system is having issues. Try:

1. **Wait 5-10 minutes** - Temporary outage
2. **Check Cloudflare Status**: https://www.cloudflarestatus.com/
3. **Toggle DNS Record**:
   - Click the record in Cloudflare
   - Change proxy status back to Proxied, then back to DNS Only
   - Save and wait 2 minutes

---

## Quick Troubleshooting Checklist

### ✅ Before You Connect:
- [ ] Railway service is running (check Deployments → Success)
- [ ] Railway direct URL works in Minecraft
- [ ] Cloudflare DNS record uses "DNS Only" (gray cloud)
- [ ] CNAME content is correct
- [ ] DNS has propagated (checked with nslookup)
- [ ] Local DNS cache is cleared
- [ ] 15+ minutes have passed since DNS change

### If Still Not Working:
- [ ] Check Railway logs for errors
- [ ] Try Railway direct URL again
- [ ] Check Cloudflare status website
- [ ] Verify Cloudflare API isn't having issues
- [ ] Contact support with: `minecraft.yourdomain.com` resolution results

---

## Common Errors & Fixes

### "Cannot reach server" or "Connection timed out"
**Likely Cause**: DNS not propagated or server not running
**Fix**: 
1. Test with Railway direct URL
2. Wait 15+ minutes
3. Check Railway service is running

### "Connection refused"
**Likely Cause**: Server crashed or wrong port
**Fix**:
1. Check Railway logs for errors
2. Verify port 25565 is exposed
3. Restart deployment

### "Name or service not known"
**Likely Cause**: DNS record doesn't exist or is wrong
**Fix**:
1. Verify record in Cloudflare DNS tab
2. Check CNAME content is correct
3. Wait for propagation

### 502 Bad Gateway from Cloudflare API
**Likely Cause**: Cloudflare system issue or DNS configuration problem
**Fix**:
1. Wait 10 minutes
2. Check Cloudflare status
3. Toggle DNS record proxy status
4. Clear Cloudflare cache

---

## 🎯 Quick Start (TL;DR)

1. Test Railway direct URL first: `your-project-xxx.railway.app:25565`
2. If works → DNS issue, check Cloudflare
3. If fails → Server issue, check Railway logs
4. For Cloudflare: Must be "DNS Only" (gray cloud)
5. Wait 15 minutes after changes
6. Clear caches (browser, DNS, Cloudflare)

---

## 📞 Still Stuck?

Provide these details:
- [ ] Does Railway direct URL work? (yes/no)
- [ ] What does `nslookup minecraft.yourdomain.com` show?
- [ ] What's in Railway deployment logs?
- [ ] Cloudflare DNS record details (screenshot of the record)
- [ ] How long ago did you make the DNS change?

