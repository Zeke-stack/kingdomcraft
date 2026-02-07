const { Rcon } = require('rcon-client');

class RconManager {
    constructor() {
        this.rcon = null;
        this.host = process.env.RCON_HOST || 'localhost';
        this.port = parseInt(process.env.RCON_PORT || '25575');
        this.password = process.env.RCON_PASSWORD || 'kc-rcon-2025';
        this.connected = false;
        this.retryTimer = null;
    }

    async connect() {
        // Clean up old connection
        if (this.rcon) {
            try { this.rcon.end(); } catch {}
            this.rcon = null;
        }
        this.connected = false;

        console.log(`[RCON] Connecting to ${this.host}:${this.port}...`);

        if (!this.host || this.host === 'localhost') {
            console.log('[RCON] No RCON_HOST set, skipping connection. Set RCON_HOST env var.');
            return;
        }

        try {
            this.rcon = await Rcon.connect({
                host: this.host,
                port: this.port,
                password: this.password,
                timeout: 10000
            });
            this.connected = true;
            console.log(`[RCON] ✅ Connected to ${this.host}:${this.port}`);

            this.rcon.on('end', () => {
                this.connected = false;
                console.log('[RCON] Disconnected, retrying in 15s...');
                this.scheduleRetry(15000);
            });

            this.rcon.on('error', (err) => {
                console.error('[RCON] Socket error:', err.message);
                this.connected = false;
                // Don't rethrow — handled
            });
        } catch (err) {
            this.connected = false;
            console.error(`[RCON] ❌ Failed to connect to ${this.host}:${this.port}`);
            console.error(`[RCON] Error name: ${err.name}, code: ${err.code}, message: ${err.message}`);
            console.error('[RCON] Full error:', err);
            this.scheduleRetry(20000);
        }
    }

    scheduleRetry(ms) {
        if (this.retryTimer) clearTimeout(this.retryTimer);
        this.retryTimer = setTimeout(() => this.connect().catch(() => {}), ms);
    }

    async send(command) {
        if (!this.connected || !this.rcon) {
            throw new Error('Not connected to server');
        }
        try {
            const response = await this.rcon.send(command);
            return response;
        } catch (err) {
            this.connected = false;
            this.scheduleRetry(10000);
            throw err;
        }
    }

    async sendChat(playerName, message) {
        const cmd = `tellraw @a ["",{"text":"[Discord] ","color":"blue"},{"text":"${playerName}","color":"aqua"},{"text":": ${message.replace(/"/g, '\\"')}","color":"white"}]`;
        return this.send(cmd);
    }

    async getPlayerList() {
        return this.send('list');
    }

    async sendCommand(command) {
        return this.send(command);
    }

    isConnected() {
        return this.connected;
    }
}

module.exports = RconManager;
