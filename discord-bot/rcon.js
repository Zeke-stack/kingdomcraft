const { Rcon } = require('rcon-client');

class RconManager {
    constructor() {
        this.rcon = null;
        this.host = process.env.RCON_HOST || 'localhost';
        this.port = parseInt(process.env.RCON_PORT || '25575');
        this.password = process.env.RCON_PASSWORD || 'kc-rcon-2025';
        this.connected = false;
    }

    async connect() {
        try {
            this.rcon = await Rcon.connect({
                host: this.host,
                port: this.port,
                password: this.password,
                timeout: 5000
            });
            this.connected = true;
            console.log('[RCON] Connected to Minecraft server');

            this.rcon.on('end', () => {
                this.connected = false;
                console.log('[RCON] Disconnected, will retry...');
                setTimeout(() => this.connect(), 10000);
            });

            this.rcon.on('error', (err) => {
                console.error('[RCON] Error:', err.message);
                this.connected = false;
            });
        } catch (err) {
            this.connected = false;
            console.error('[RCON] Failed to connect:', err.message);
            setTimeout(() => this.connect(), 15000);
        }
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
