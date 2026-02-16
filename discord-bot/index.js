require('dotenv').config();
const { Client, GatewayIntentBits, EmbedBuilder, PermissionFlagsBits, SlashCommandBuilder, REST, Routes, ActivityType } = require('discord.js');
const express = require('express');
const crypto = require('crypto');
const path = require('path');

// ─── Config ───
const TOKEN = process.env.DISCORD_TOKEN;
const GUILD_ID = process.env.GUILD_ID;
const CHAT_CHANNEL_ID = process.env.CHAT_CHANNEL_ID;
const EVENTS_CHANNEL_ID = process.env.EVENTS_CHANNEL_ID;
const LOG_CHANNEL_ID = process.env.LOG_CHANNEL_ID;
const STATUS_CHANNEL_ID = process.env.STATUS_CHANNEL_ID;
const PORT = process.env.PORT || 3000;
const PANEL_PASSWORD = process.env.PANEL_PASSWORD || 'kc-panel-2025';
const MC_API_KEY = process.env.MC_API_KEY || 'kc-bridge-2025';
const panelTokens = new Set();

if (!TOKEN) {
    console.error('ERROR: DISCORD_TOKEN environment variable is required');
    process.exit(1);
}

// ─── Discord Client ───
const client = new Client({
    intents: [
        GatewayIntentBits.Guilds,
        GatewayIntentBits.GuildMessages,
        GatewayIntentBits.MessageContent,
    ]
});

// ─── Command Queue (replaces RCON) ───
const commandQueue = [];
let commandIdCounter = 1;
const commandResults = new Map();
let onlinePlayers = [];
let lastHeartbeat = 0;

function queueCommand(command) {
    return new Promise((resolve) => {
        const id = commandIdCounter++;
        const timeout = setTimeout(() => {
            commandResults.delete(id);
            resolve('Command timed out (server may be offline)');
        }, 15000);
        commandResults.set(id, { resolve, timeout });
        commandQueue.push({ id, command });
    });
}

function isServerOnline() {
    return (Date.now() - lastHeartbeat) < 60000;
}

let serverOnline = false;
let playerCount = 0;
let serverLocked = false;
let statusMessageId = null;

// ─── Logging Helper ───
async function log(title, description, color = 0x808080) {
    if (!LOG_CHANNEL_ID) return;
    try {
        const channel = await client.channels.fetch(LOG_CHANNEL_ID);
        if (channel) {
            const embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp();
            await channel.send({ embeds: [embed] });
        }
    } catch (err) {
        console.error('[Log] Failed to send log:', err.message);
    }
}

// ─── Status Channel Updater ───
async function cleanStatusChannel(channel) {
    try {
        const messages = await channel.messages.fetch({ limit: 20 });
        const botMessages = messages.filter(m => m.author.id === client.user.id);
        for (const [, msg] of botMessages) {
            try { await msg.delete(); } catch {}
        }
    } catch (err) {
        console.error('[Status] Failed to clean old messages:', err.message);
    }
    statusMessageId = null;
}

async function updateStatusEmbed() {
    if (!STATUS_CHANNEL_ID) return;
    try {
        const channel = await client.channels.fetch(STATUS_CHANNEL_ID);
        if (!channel) return;

        const status = serverOnline
            ? (serverLocked ? 'Locked' : 'Online')
            : 'Offline';
        const statusDot = serverOnline ? '🟢' : '🔴';

        const embed = new EmbedBuilder()
            .setTitle('KingdomCraft Server')
            .setColor(serverOnline ? 0x00ff00 : 0xff0000)
            .setDescription(
                `${statusDot} **${status}**\n\n` +
                `**Players:** ${playerCount}\n` +
                `**IP:** \`continents.cc\`\n` +
                `**Version:** Paper 1.21.1`
            )
            .setFooter({ text: 'Last updated' })
            .setTimestamp();

        if (statusMessageId) {
            try {
                const msg = await channel.messages.fetch(statusMessageId);
                await msg.edit({ embeds: [embed] });
                return;
            } catch {
                statusMessageId = null;
            }
        }

        await cleanStatusChannel(channel);
        const sent = await channel.send({ embeds: [embed] });
        statusMessageId = sent.id;
    } catch (err) {
        console.error('[Status] Failed to update:', err.message);
    }
}

// ─── Slash Commands ───
const commands = [
    new SlashCommandBuilder().setName('status').setDescription('Check Minecraft server status'),
    new SlashCommandBuilder().setName('players').setDescription('List online players'),
    new SlashCommandBuilder().setName('say').setDescription('Send a message to the Minecraft server')
        .addStringOption(opt => opt.setName('message').setDescription('Message to send').setRequired(true)),
    new SlashCommandBuilder().setName('cmd').setDescription('Run a server command (staff only)')
        .addStringOption(opt => opt.setName('command').setDescription('Command to run').setRequired(true)),
    new SlashCommandBuilder().setName('kingdoms').setDescription('List all kingdoms on the server'),
    new SlashCommandBuilder().setName('whitelist').setDescription('Manage whitelist (staff only)')
        .addStringOption(opt => opt.setName('action').setDescription('add/remove').setRequired(true)
            .addChoices({ name: 'add', value: 'add' }, { name: 'remove', value: 'remove' }))
        .addStringOption(opt => opt.setName('player').setDescription('Player name').setRequired(true)),
    new SlashCommandBuilder().setName('serverip').setDescription('Get the server IP address'),
    new SlashCommandBuilder().setName('lock').setDescription('Lock/unlock server - only whitelisted/OP players can join'),
].map(cmd => cmd.toJSON());

// ─── Deploy Commands ───
async function deployCommands() {
    try {
        const rest = new REST().setToken(TOKEN);
        if (GUILD_ID) {
            await rest.put(Routes.applicationGuildCommands(client.user.id, GUILD_ID), { body: commands });
        } else {
            await rest.put(Routes.applicationCommands(client.user.id), { body: commands });
        }
        console.log('[Bot] Slash commands registered');
    } catch (err) {
        console.error('[Bot] Failed to register commands:', err.message);
    }
}

// ─── Bot Ready ───
client.once('clientReady', async () => {
    console.log(`[Bot] Logged in as ${client.user.tag}`);

    try {
        await deployCommands();
    } catch (err) {
        console.error('[Bot] Command deploy error:', err.message);
    }

    console.log('[Bot] Using HTTP bridge (no RCON). MC plugin will poll for commands.');

    client.user.setPresence({
        activities: [{ name: 'Starting up...', type: ActivityType.Watching }],
        status: 'idle'
    });
    await updateStatusEmbed();
    await log('Bot Started', 'Discord bot is now online and connected.', 0x00ff00);

    // Update status every 30 seconds using heartbeat data
    setInterval(async () => {
        serverOnline = isServerOnline();
        if (!serverOnline) {
            playerCount = 0;
            onlinePlayers = [];
        }

        client.user.setPresence({
            activities: [{
                name: serverOnline
                    ? (serverLocked
                        ? `LOCKED | ${playerCount} online`
                        : `${playerCount} player${playerCount !== 1 ? 's' : ''} online`)
                    : 'Server Offline',
                type: ActivityType.Watching
            }],
            status: serverOnline ? 'online' : 'dnd'
        });

        await updateStatusEmbed();
    }, 30000);
});

// ─── Chat Sync: Discord → Minecraft ───
client.on('messageCreate', async (message) => {
    if (message.author.bot) return;
    if (!message.guild || message.guild.id !== GUILD_ID) return;
    if (message.channel.id !== CHAT_CHANNEL_ID) return;
    if (message.content.startsWith('/') || message.content.startsWith('!')) return;

    try {
        if (isServerOnline()) {
            const clean = message.content.replace(/\\/g, '').replace(/"/g, "'").replace(/\n/g, ' ').substring(0, 200);
            const name = message.member?.displayName || message.author.username;
            queueCommand(`tellraw @a ["",{"text":"[Discord] ","color":"blue"},{"text":"${name}","color":"aqua"},{"text":": ${clean}","color":"white"}]`);
            await log('Discord > MC', `**${message.author.tag}**: ${clean}`, 0x5865F2);
        }
    } catch (err) {
        console.error('[Chat] Failed to send to MC:', err.message);
    }
});

// ─── Slash Command Handler ───
client.on('interactionCreate', async (interaction) => {
    if (!interaction.isChatInputCommand()) return;
    if (!interaction.guild || interaction.guild.id !== GUILD_ID) {
        await interaction.reply({ content: '❌ This bot only works in the KingdomCraft server.', ephemeral: true });
        return;
    }

    const { commandName } = interaction;
    const user = interaction.user.tag;

    await log('Command Used', `**${user}** used \`/${commandName}\` ${interaction.options.data.map(o => `${o.name}: \`${o.value}\``).join(' ')}`, 0x5865F2);

    switch (commandName) {
        case 'status': {
            const embed = new EmbedBuilder()
                .setTitle('KingdomCraft Server Status')
                .setColor(serverOnline ? (serverLocked ? 0xff8800 : 0x00ff00) : 0xff0000)
                .addFields(
                    { name: 'Status', value: serverOnline ? (serverLocked ? 'Locked' : 'Online') : 'Offline', inline: true },
                    { name: 'Players', value: `${playerCount}`, inline: true },
                    { name: 'IP', value: '`continents.cc`', inline: true },
                    { name: 'Version', value: 'Paper 1.21.1', inline: true },
                    { name: 'Bridge', value: isServerOnline() ? 'Connected' : 'Disconnected', inline: true },
                    { name: 'Lock', value: serverLocked ? 'Locked' : 'Open', inline: true },
                )
                .setTimestamp()
                .setFooter({ text: 'KingdomCraft' });
            await interaction.reply({ embeds: [embed] });
            break;
        }

        case 'players': {
            if (!isServerOnline()) {
                await interaction.reply({ content: '❌ Server is offline', ephemeral: true });
                return;
            }
            const playerList = onlinePlayers.length > 0 ? onlinePlayers.join(', ') : 'No players online';
            const embed = new EmbedBuilder()
                .setTitle('Online Players')
                .setColor(0x00aaff)
                .setDescription(`There are ${onlinePlayers.length} players online: ${playerList}`)
                .setTimestamp();
            await interaction.reply({ embeds: [embed] });
            break;
        }

        case 'say': {
            const msg = interaction.options.getString('message');
            if (!isServerOnline()) {
                await interaction.reply({ content: '❌ Server is offline', ephemeral: true });
                return;
            }
            try {
                const clean = msg.replace(/\\/g, '').replace(/"/g, "'").substring(0, 200);
                const name = interaction.member?.displayName || interaction.user.username;
                queueCommand(`tellraw @a ["",{"text":"[Discord] ","color":"blue"},{"text":"${name}","color":"aqua"},{"text":": ${clean}","color":"white"}]`);
                await interaction.reply({ content: '✅ Message sent to server' });
                await log('Say', `**${user}** said: ${clean}`, 0x00aaff);
            } catch {
                await interaction.reply({ content: '❌ Failed to send message', ephemeral: true });
            }
            break;
        }

        case 'cmd': {
            if (!interaction.member.permissions.has(PermissionFlagsBits.Administrator)) {
                await interaction.reply({ content: '❌ You need Administrator permissions to use this.', ephemeral: true });
                return;
            }
            const cmd = interaction.options.getString('command');
            if (!isServerOnline()) {
                await interaction.reply({ content: '❌ Server is offline', ephemeral: true });
                return;
            }
            await interaction.deferReply();
            try {
                const response = await queueCommand(cmd);
                const embed = new EmbedBuilder()
                    .setTitle('Command Executed')
                    .setColor(0xffaa00)
                    .addFields(
                        { name: 'Command', value: `\`${cmd}\`` },
                        { name: 'Response', value: response || 'No output' }
                    )
                    .setTimestamp();
                await interaction.editReply({ embeds: [embed] });
                await log('Command', `**${user}** ran: \`${cmd}\`\nResponse: ${response || 'No output'}`, 0xffaa00);
            } catch {
                await interaction.editReply({ content: '❌ Failed to run command' });
            }
            break;
        }

        case 'kingdoms': {
            const embed = new EmbedBuilder()
                .setTitle('Kingdoms')
                .setColor(0xffcc00)
                .setDescription('Use the server to view kingdom details.\nKingdom data is managed in-game by event staff.')
                .addFields(
                    { name: 'Create Kingdom', value: '`/createkingdom <name> <leader>` (Event Staff)', inline: false },
                    { name: 'Join Kingdom', value: '`/joinkingdom <kingdom>` (All Players)', inline: false },
                    { name: 'Kingdom List', value: '`/kingdomlist` (Kingdom Leaders)', inline: false },
                )
                .setTimestamp();
            await interaction.reply({ embeds: [embed] });
            break;
        }

        case 'whitelist': {
            if (!interaction.member.permissions.has(PermissionFlagsBits.Administrator)) {
                await interaction.reply({ content: '❌ You need Administrator permissions.', ephemeral: true });
                return;
            }
            const action = interaction.options.getString('action');
            const player = interaction.options.getString('player');
            if (!isServerOnline()) {
                await interaction.reply({ content: '❌ Server is offline', ephemeral: true });
                return;
            }
            await interaction.deferReply();
            try {
                const response = await queueCommand(`whitelist ${action} ${player}`);
                await interaction.editReply({ content: `✅ Whitelist ${action}: **${player}**\n${response}` });
                await log('Whitelist', `**${user}** ${action === 'add' ? 'added' : 'removed'} **${player}** from whitelist`, 0x00aaff);
            } catch {
                await interaction.editReply({ content: '❌ Failed to update whitelist' });
            }
            break;
        }

        case 'serverip': {
            const embed = new EmbedBuilder()
                .setTitle('KingdomCraft Server')
                .setColor(0x00ff88)
                .addFields(
                    { name: 'Server IP', value: '`continents.cc`', inline: true },
                    { name: 'Version', value: '`1.21.1`', inline: true },
                    { name: 'Java Edition', value: 'Yes', inline: true }
                )
                .setDescription('Connect using Minecraft Java Edition 1.21.1')
                .setTimestamp()
                .setFooter({ text: 'KingdomCraft' });
            await interaction.reply({ embeds: [embed] });
            break;
        }

        case 'lock': {
            if (!interaction.member.permissions.has(PermissionFlagsBits.Administrator)) {
                await interaction.reply({ content: '❌ You need Administrator permissions to use this.', ephemeral: true });
                return;
            }
            if (!isServerOnline()) {
                await interaction.reply({ content: '❌ Server is offline', ephemeral: true });
                return;
            }

            await interaction.deferReply();
            try {
                if (!serverLocked) {
                    serverLocked = true;
                    queueCommand('whitelist on');
                    queueCommand('say Server has been LOCKED. Only whitelisted/OP players may join.');

                    const embed = new EmbedBuilder()
                        .setTitle('Server Locked')
                        .setColor(0xff0000)
                        .setDescription('Server is now **locked**. Only whitelisted/OP players can join.')
                        .addFields({ name: 'Locked By', value: user })
                        .setTimestamp();
                    await interaction.editReply({ embeds: [embed] });
                    await log('Server Locked', `**${user}** locked the server.`, 0xff0000);
                } else {
                    serverLocked = false;
                    queueCommand('whitelist off');
                    queueCommand('say Server has been UNLOCKED. All players may join.');

                    const embed = new EmbedBuilder()
                        .setTitle('Server Unlocked')
                        .setColor(0x00ff00)
                        .setDescription('Server is now **unlocked**. All players can join again.')
                        .addFields({ name: 'Unlocked By', value: user })
                        .setTimestamp();
                    await interaction.editReply({ embeds: [embed] });
                    await log('Server Unlocked', `**${user}** unlocked the server.`, 0x00ff00);
                }
                await updateStatusEmbed();
            } catch (err) {
                await interaction.editReply({ content: `❌ Lock failed: ${err.message}` });
            }
            break;
        }
    }
});

// ─── Express Server ───
const app = express();
app.use(express.json());

// Health check
app.get('/', (req, res) => {
    res.json({ status: 'ok', serverOnline, playerCount, serverLocked });
});

// ─── Bridge Endpoints (MC Plugin <-> Bot) ───

function bridgeAuth(req, res, next) {
    const key = req.headers['x-api-key'];
    if (key !== MC_API_KEY) return res.status(401).json({ error: 'Invalid API key' });
    next();
}

// MC plugin polls this for pending commands
app.post('/bridge/poll', bridgeAuth, (req, res) => {
    const pending = commandQueue.splice(0, commandQueue.length);
    res.json({ commands: pending });
});

// MC plugin sends heartbeat with player data
app.post('/bridge/heartbeat', bridgeAuth, (req, res) => {
    const { players, playerCount: count } = req.body;
    lastHeartbeat = Date.now();
    serverOnline = true;
    if (Array.isArray(players)) onlinePlayers = players;
    if (typeof count === 'number') playerCount = count;
    res.json({ ok: true, queueSize: commandQueue.length });
});

// MC plugin sends command execution results
app.post('/bridge/result', bridgeAuth, (req, res) => {
    const { id, result } = req.body;
    const entry = commandResults.get(id);
    if (entry) {
        clearTimeout(entry.timeout);
        entry.resolve(result || '');
        commandResults.delete(id);
    }
    res.json({ ok: true });
});

// ─── MC Webhook Endpoints (from KingdomCraft plugin) ───

app.post('/mc/chat', async (req, res) => {
    const { player, message } = req.body;
    try {
        if (CHAT_CHANNEL_ID) {
            const channel = await client.channels.fetch(CHAT_CHANNEL_ID);
            if (channel) await channel.send(`**${player}**: ${message}`);
        }
        await log('MC Chat', `**${player}**: ${message}`, 0x55ff55);
    } catch (err) {
        console.error('[Webhook] Chat error:', err.message);
    }
    res.sendStatus(200);
});

app.post('/mc/join', async (req, res) => {
    const { player, action } = req.body;
    try {
        if (CHAT_CHANNEL_ID) {
            const channel = await client.channels.fetch(CHAT_CHANNEL_ID);
            if (channel) {
                const emoji = action === 'join' ? '🟢' : '🔴';
                await channel.send(`${emoji} **${player}** ${action === 'join' ? 'joined' : 'left'} the server`);
            }
        }
        const color = action === 'join' ? 0x00ff00 : 0xff4444;
        await log(action === 'join' ? 'Player Join' : 'Player Leave', `**${player}** ${action === 'join' ? 'joined' : 'left'} the server`, color);
    } catch (err) {
        console.error('[Webhook] Join error:', err.message);
    }
    res.sendStatus(200);
});

app.post('/mc/death', async (req, res) => {
    const { player, message, isKing } = req.body;
    try {
        if (CHAT_CHANNEL_ID) {
            const channel = await client.channels.fetch(CHAT_CHANNEL_ID);
            if (channel) {
                if (isKing) {
                    const embed = new EmbedBuilder()
                        .setTitle('The King Has Died!')
                        .setColor(0xff0000)
                        .setDescription(`**${player}** has fallen.\nEvent staff will determine the next leader.`)
                        .setTimestamp();
                    await channel.send({ embeds: [embed] });
                } else {
                    await channel.send(`**${player}** has died. ${message || ''}`);
                }
            }
        }
        if (EVENTS_CHANNEL_ID) {
            const evChannel = await client.channels.fetch(EVENTS_CHANNEL_ID);
            if (evChannel) {
                const embed = new EmbedBuilder()
                    .setTitle(isKing ? 'KING DEATH' : 'Player Death')
                    .setColor(isKing ? 0xff0000 : 0x888888)
                    .setDescription(`**${player}**\n${message || 'Unknown cause'}`)
                    .setTimestamp();
                await evChannel.send({ embeds: [embed] });
            }
        }
        await log('Death', `**${player}**: ${message || 'Unknown cause'}${isKing ? ' **[KING]**' : ''}`, 0xff0000);
    } catch (err) {
        console.error('[Webhook] Death error:', err.message);
    }
    res.sendStatus(200);
});

app.post('/mc/kingdom', async (req, res) => {
    const { action, kingdom, player, message } = req.body;
    try {
        const embed = new EmbedBuilder()
            .setColor(0xffcc00)
            .setTimestamp();

        switch (action) {
            case 'created':
                embed.setTitle('New Kingdom Established!')
                    .setDescription(`**${kingdom}** has been founded by **${player}**!\n3 days of protection active.`);
                break;
            case 'destroyed':
                embed.setTitle('Kingdom Has Fallen')
                    .setColor(0xff0000)
                    .setDescription(`The kingdom of **${kingdom}** has been destroyed.`);
                break;
            case 'leadership_transferred':
                embed.setTitle('Leadership Transfer')
                    .setDescription(`**${player}** is now the leader of **${kingdom}**.`);
                break;
            case 'member_joined':
                embed.setTitle('New Kingdom Member')
                    .setDescription(`**${player}** joined **${kingdom}**.`);
                break;
            case 'member_left':
                embed.setTitle('Kingdom Member Left')
                    .setDescription(`**${player}** left **${kingdom}**.`);
                break;
            case 'member_kicked':
                embed.setTitle('Member Kicked')
                    .setColor(0xff4444)
                    .setDescription(`**${player}** was kicked from **${kingdom}**.`);
                break;
            case 'renamed':
                embed.setTitle('Kingdom Renamed')
                    .setDescription(`**${kingdom}**`);
                break;
            default:
                embed.setTitle('Kingdom Event')
                    .setDescription(message || `${action} - ${kingdom} - ${player}`);
        }

        if (CHAT_CHANNEL_ID) {
            const channel = await client.channels.fetch(CHAT_CHANNEL_ID);
            if (channel) await channel.send({ embeds: [embed] });
        }
        if (EVENTS_CHANNEL_ID) {
            const evChannel = await client.channels.fetch(EVENTS_CHANNEL_ID);
            if (evChannel) await evChannel.send({ embeds: [embed] });
        }
        await log('Kingdom', `**${action}** | Kingdom: **${kingdom}** | Player: **${player || 'N/A'}**`, 0xffcc00);
    } catch (err) {
        console.error('[Webhook] Kingdom error:', err.message);
    }
    res.sendStatus(200);
});

app.post('/mc/server', async (req, res) => {
    const { action } = req.body;
    try {
        const embed = new EmbedBuilder().setTimestamp();

        if (action === 'start') {
            serverOnline = true;
            embed.setTitle('Server Online')
                .setColor(0x00ff00)
                .setDescription('KingdomCraft server is now online!\nConnect: `continents.cc`');
        } else if (action === 'stop') {
            serverOnline = false;
            playerCount = 0;
            serverLocked = false;
            embed.setTitle('Server Offline')
                .setColor(0xff0000)
                .setDescription('KingdomCraft server has gone offline.');
        }

        if (CHAT_CHANNEL_ID) {
            const channel = await client.channels.fetch(CHAT_CHANNEL_ID);
            if (channel) await channel.send({ embeds: [embed] });
        }

        await log(action === 'start' ? 'Server Start' : 'Server Stop', `Server ${action === 'start' ? 'started' : 'stopped'}`, action === 'start' ? 0x00ff00 : 0xff0000);
        await updateStatusEmbed();
    } catch (err) {
        console.error('[Webhook] Server error:', err.message);
    }
    res.sendStatus(200);
});

// ─── Control Panel ───
app.use('/panel-assets', express.static(path.join(__dirname, 'public')));

app.get('/panel', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'panel.html'));
});

function panelAuth(req, res, next) {
    const auth = req.headers.authorization;
    if (!auth || !auth.startsWith('Bearer ')) return res.status(401).json({ error: 'Unauthorized' });
    const t = auth.slice(7);
    if (!panelTokens.has(t)) return res.status(401).json({ error: 'Invalid token' });
    next();
}

app.post('/panel/login', (req, res) => {
    const { password } = req.body;
    if (password === PANEL_PASSWORD) {
        const t = crypto.randomBytes(32).toString('hex');
        panelTokens.add(t);
        if (panelTokens.size > 50) {
            const arr = [...panelTokens];
            arr.slice(0, arr.length - 20).forEach(tok => panelTokens.delete(tok));
        }
        return res.json({ success: true, token: t });
    }
    res.json({ success: false });
});

app.get('/panel/status', panelAuth, (req, res) => {
    res.json({
        serverOnline,
        playerCount,
        serverLocked,
        rconConnected: isServerOnline()
    });
});

app.get('/panel/players', panelAuth, (req, res) => {
    res.json({ players: onlinePlayers });
});

app.post('/panel/command', panelAuth, async (req, res) => {
    const { command } = req.body;
    if (!command) return res.json({ response: 'No command provided' });
    if (!isServerOnline()) return res.json({ response: 'Server not connected' });
    try {
        const response = await queueCommand(command);
        await log('Panel Command', `\`${command}\`\nResponse: ${response || 'No output'}`, 0xffaa00);
        res.json({ response: response || 'No output' });
    } catch (err) {
        res.json({ response: 'Error: ' + err.message });
    }
});

app.post('/panel/action', panelAuth, async (req, res) => {
    const { action } = req.body;
    if (!isServerOnline()) {
        return res.json({ message: 'Server not connected' });
    }
    try {
        switch (action) {
            case 'save':
                queueCommand('save-all');
                res.json({ message: 'World save queued' });
                break;
            case 'weather-clear':
                queueCommand('weather clear');
                res.json({ message: 'Weather clear queued' });
                break;
            case 'time-day':
                queueCommand('time set day');
                res.json({ message: 'Time set to day queued' });
                break;
            case 'time-night':
                queueCommand('time set night');
                res.json({ message: 'Time set to night queued' });
                break;
            case 'whitelist-on':
                queueCommand('whitelist on');
                res.json({ message: 'Whitelist enable queued' });
                break;
            case 'whitelist-off':
                queueCommand('whitelist off');
                res.json({ message: 'Whitelist disable queued' });
                break;
            case 'toggle-lock':
                if (!serverLocked) {
                    serverLocked = true;
                    queueCommand('whitelist on');
                    queueCommand('say Server has been LOCKED. Only whitelisted/OP players may join.');
                    await updateStatusEmbed();
                    res.json({ message: 'Server locked' });
                } else {
                    serverLocked = false;
                    queueCommand('whitelist off');
                    queueCommand('say Server has been UNLOCKED. All players may join.');
                    await updateStatusEmbed();
                    res.json({ message: 'Server unlocked' });
                }
                break;
            case 'stop':
                queueCommand('stop');
                res.json({ message: 'Server stop queued' });
                break;
            default:
                res.json({ message: 'Unknown action' });
        }
        await log('Panel Action', `Action: \`${action}\``, 0x4a6cf7);
    } catch (err) {
        res.json({ message: 'Error: ' + err.message });
    }
});

// ─── Start ───
app.listen(PORT, '0.0.0.0', () => {
    console.log(`[API] Listening on port ${PORT}`);
});

console.log('[Bot] Logging in...');
console.log('[Bot] Token present:', !!TOKEN);
console.log('[Bot] Guild ID:', GUILD_ID || 'NOT SET');

client.login(TOKEN).catch(err => {
    console.error('[Bot] FATAL: Failed to login:', err.message);
});

process.on('unhandledRejection', (err) => {
    console.error('[Bot] Unhandled rejection:', err);
});
process.on('uncaughtException', (err) => {
    console.error('[Bot] Uncaught exception:', err);
});
