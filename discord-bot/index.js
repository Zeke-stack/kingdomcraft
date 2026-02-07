require('dotenv').config();
const { Client, GatewayIntentBits, EmbedBuilder, PermissionFlagsBits, SlashCommandBuilder, REST, Routes, ActivityType } = require('discord.js');
const express = require('express');
const RconManager = require('./rcon');

// ─── Config ───
const TOKEN = process.env.DISCORD_TOKEN;
const GUILD_ID = process.env.GUILD_ID;
const CHAT_CHANNEL_ID = process.env.CHAT_CHANNEL_ID;
const EVENTS_CHANNEL_ID = process.env.EVENTS_CHANNEL_ID;
const LOG_CHANNEL_ID = process.env.LOG_CHANNEL_ID;
const STATUS_CHANNEL_ID = process.env.STATUS_CHANNEL_ID;
const PORT = process.env.PORT || 3000;

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

const rcon = new RconManager();
let serverOnline = false;
let playerCount = 0;
let serverLocked = false;
let statusMessageId = null; // persistent status embed message

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
async function updateStatusEmbed() {
    if (!STATUS_CHANNEL_ID) return;
    try {
        const channel = await client.channels.fetch(STATUS_CHANNEL_ID);
        if (!channel) return;

        const embed = new EmbedBuilder()
            .setTitle('⚔️ KingdomCraft Server Status')
            .setColor(serverOnline ? (serverLocked ? 0xff8800 : 0x00ff00) : 0xff0000)
            .addFields(
                { name: '📡 Status', value: serverOnline ? (serverLocked ? '🔒 Locked (OP Only)' : '🟢 Online') : '🔴 Offline', inline: true },
                { name: '👥 Players', value: `${playerCount}`, inline: true },
                { name: '🌐 IP', value: '`continents.cc`', inline: true },
                { name: '📦 Version', value: 'Paper 1.21.1', inline: true },
                { name: '🔗 RCON', value: rcon.isConnected() ? '✅ Connected' : '❌ Disconnected', inline: true },
                { name: '🔒 Lock', value: serverLocked ? '🔴 Locked' : '🟢 Open', inline: true },
            )
            .setFooter({ text: 'KingdomCraft ⚔️ • Updates every 30s' })
            .setTimestamp();

        // Edit existing message or send new one
        if (statusMessageId) {
            try {
                const msg = await channel.messages.fetch(statusMessageId);
                await msg.edit({ embeds: [embed] });
                return;
            } catch {
                // Message deleted, send a new one
                statusMessageId = null;
            }
        }

        // Send new status message
        const sent = await channel.send({ embeds: [embed] });
        statusMessageId = sent.id;
    } catch (err) {
        console.error('[Status] Failed to update:', err.message);
    }
}

// ─── Slash Commands ───
const commands = [
    new SlashCommandBuilder()
        .setName('status')
        .setDescription('Check Minecraft server status'),
    new SlashCommandBuilder()
        .setName('players')
        .setDescription('List online players'),
    new SlashCommandBuilder()
        .setName('say')
        .setDescription('Send a message to the Minecraft server')
        .addStringOption(opt => opt.setName('message').setDescription('Message to send').setRequired(true)),
    new SlashCommandBuilder()
        .setName('cmd')
        .setDescription('Run a server command (staff only)')
        .addStringOption(opt => opt.setName('command').setDescription('Command to run').setRequired(true)),
    new SlashCommandBuilder()
        .setName('kingdoms')
        .setDescription('List all kingdoms on the server'),
    new SlashCommandBuilder()
        .setName('whitelist')
        .setDescription('Manage whitelist (staff only)')
        .addStringOption(opt => opt.setName('action').setDescription('add/remove').setRequired(true).addChoices({ name: 'add', value: 'add' }, { name: 'remove', value: 'remove' }))
        .addStringOption(opt => opt.setName('player').setDescription('Player name').setRequired(true)),
    new SlashCommandBuilder()
        .setName('serverip')
        .setDescription('Get the server IP address'),
    new SlashCommandBuilder()
        .setName('lock')
        .setDescription('Lock/unlock server - kicks non-OP players and only OPs can join'),
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
client.once('ready', async () => {
    console.log(`[Bot] Logged in as ${client.user.tag}`);
    
    try {
        await deployCommands();
    } catch (err) {
        console.error('[Bot] Command deploy error:', err.message);
    }
    
    // Connect RCON in background (don't block or crash)
    rcon.connect().catch(err => {
        console.error('[Bot] RCON initial connect failed (will retry):', err.message);
    });

    // Initial status
    client.user.setPresence({
        activities: [{ name: 'Starting up...', type: ActivityType.Watching }],
        status: 'idle'
    });
    await updateStatusEmbed();

    await log('🤖 Bot Started', 'Discord bot is now online and connected.', 0x00ff00);

    // Update status every 30 seconds
    setInterval(async () => {
        try {
            if (rcon.isConnected()) {
                const list = await rcon.getPlayerList();
                const match = list.match(/(\d+)/);
                playerCount = match ? parseInt(match[1]) : 0;
                serverOnline = true;
            } else {
                serverOnline = false;
                playerCount = 0;
            }
        } catch {
            serverOnline = false;
            playerCount = 0;
        }

        client.user.setPresence({
            activities: [{
                name: serverOnline
                    ? (serverLocked
                        ? `🔒 LOCKED | ${playerCount} online`
                        : `${playerCount} player${playerCount !== 1 ? 's' : ''} online | continents.cc`)
                    : 'Server Offline',
                type: ActivityType.Watching
            }],
            status: serverOnline ? (serverLocked ? 'dnd' : 'online') : 'dnd'
        });

        await updateStatusEmbed();
    }, 30000);
});

// ─── Chat Sync: Discord → Minecraft ───
client.on('messageCreate', async (message) => {
    if (message.author.bot) return;
    if (!message.guild || message.guild.id !== GUILD_ID) return;
    if (message.channel.id !== CHAT_CHANNEL_ID) return;

    // Don't forward commands
    if (message.content.startsWith('/') || message.content.startsWith('!')) return;

    try {
        if (rcon.isConnected()) {
            const clean = message.content
                .replace(/\\/g, '')
                .replace(/"/g, "'")
                .replace(/\n/g, ' ')
                .substring(0, 200);
            await rcon.sendChat(message.member?.displayName || message.author.username, clean);
            await log('💬 Discord → MC', `**${message.author.tag}**: ${clean}`, 0x5865F2);
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

    // Log every command usage
    await log('🔧 Command Used', `**${user}** used \`/${commandName}\` ${interaction.options.data.map(o => `${o.name}: \`${o.value}\``).join(' ')}`, 0x5865F2);

    switch (commandName) {
        case 'status': {
            const embed = new EmbedBuilder()
                .setTitle('⚔️ KingdomCraft Server Status')
                .setColor(serverOnline ? (serverLocked ? 0xff8800 : 0x00ff00) : 0xff0000)
                .addFields(
                    { name: 'Status', value: serverOnline ? (serverLocked ? '🔒 Locked' : '🟢 Online') : '🔴 Offline', inline: true },
                    { name: 'Players', value: `${playerCount}`, inline: true },
                    { name: 'IP', value: '`continents.cc`', inline: true },
                    { name: 'Version', value: 'Paper 1.21.1', inline: true },
                    { name: 'RCON', value: rcon.isConnected() ? '✅ Connected' : '❌ Disconnected', inline: true },
                    { name: 'Lock', value: serverLocked ? '🔴 Locked' : '🟢 Open', inline: true },
                )
                .setTimestamp()
                .setFooter({ text: 'KingdomCraft' });
            await interaction.reply({ embeds: [embed] });
            break;
        }

        case 'players': {
            if (!rcon.isConnected()) {
                await interaction.reply({ content: '❌ Server is offline', ephemeral: true });
                return;
            }
            try {
                const list = await rcon.getPlayerList();
                const embed = new EmbedBuilder()
                    .setTitle('👥 Online Players')
                    .setColor(0x00aaff)
                    .setDescription(list || 'No players online')
                    .setTimestamp();
                await interaction.reply({ embeds: [embed] });
            } catch {
                await interaction.reply({ content: '❌ Failed to get player list', ephemeral: true });
            }
            break;
        }

        case 'say': {
            const msg = interaction.options.getString('message');
            if (!rcon.isConnected()) {
                await interaction.reply({ content: '❌ Server is offline', ephemeral: true });
                return;
            }
            try {
                const clean = msg.replace(/\\/g, '').replace(/"/g, "'").substring(0, 200);
                await rcon.sendChat(interaction.member?.displayName || interaction.user.username, clean);
                await interaction.reply({ content: `✅ Sent: ${msg}` });
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
            if (!rcon.isConnected()) {
                await interaction.reply({ content: '❌ Server is offline', ephemeral: true });
                return;
            }
            try {
                const response = await rcon.sendCommand(cmd);
                const embed = new EmbedBuilder()
                    .setTitle('🔧 Command Executed')
                    .setColor(0xffaa00)
                    .addFields(
                        { name: 'Command', value: `\`${cmd}\`` },
                        { name: 'Response', value: response || 'No output' }
                    )
                    .setTimestamp();
                await interaction.reply({ embeds: [embed] });
                await log('⚡ RCON Command', `**${user}** ran: \`${cmd}\`\nResponse: ${response || 'No output'}`, 0xffaa00);
            } catch {
                await interaction.reply({ content: '❌ Failed to run command', ephemeral: true });
            }
            break;
        }

        case 'kingdoms': {
            if (!rcon.isConnected()) {
                await interaction.reply({ content: '❌ Server is offline', ephemeral: true });
                return;
            }
            const embed = new EmbedBuilder()
                .setTitle('🏰 Kingdoms')
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
            if (!rcon.isConnected()) {
                await interaction.reply({ content: '❌ Server is offline', ephemeral: true });
                return;
            }
            try {
                const response = await rcon.sendCommand(`whitelist ${action} ${player}`);
                await interaction.reply({ content: `✅ Whitelist ${action}: **${player}**\n${response}` });
                await log('📋 Whitelist', `**${user}** ${action === 'add' ? 'added' : 'removed'} **${player}** from whitelist`, 0x00aaff);
            } catch {
                await interaction.reply({ content: '❌ Failed to update whitelist', ephemeral: true });
            }
            break;
        }

        case 'serverip': {
            const embed = new EmbedBuilder()
                .setTitle('🌐 KingdomCraft Server')
                .setColor(0x00ff88)
                .addFields(
                    { name: 'Server IP', value: '`continents.cc`', inline: true },
                    { name: 'Version', value: '`1.21.1`', inline: true },
                    { name: 'Java Edition', value: '✅', inline: true }
                )
                .setDescription('Connect using Minecraft Java Edition 1.21.1')
                .setTimestamp()
                .setFooter({ text: 'KingdomCraft ⚔️' });
            await interaction.reply({ embeds: [embed] });
            break;
        }

        case 'lock': {
            // Admin only
            if (!interaction.member.permissions.has(PermissionFlagsBits.Administrator)) {
                await interaction.reply({ content: '❌ You need Administrator permissions to use this.', ephemeral: true });
                return;
            }
            if (!rcon.isConnected()) {
                await interaction.reply({ content: '❌ Server is offline', ephemeral: true });
                return;
            }

            await interaction.deferReply();

            try {
                if (!serverLocked) {
                    // ── LOCK THE SERVER ──
                    serverLocked = true;

                    // Enable whitelist (OPs bypass whitelist by default)
                    await rcon.sendCommand('whitelist on');

                    // Get online players and kick non-OPs
                    const listResponse = await rcon.getPlayerList();
                    let kickedPlayers = [];

                    // Parse player names from "There are X of a max of Y players online: Name1, Name2"
                    const nameMatch = listResponse.match(/:\s*(.+)/);
                    if (nameMatch && nameMatch[1].trim().length > 0) {
                        const playerNames = nameMatch[1].split(',').map(n => n.trim()).filter(n => n.length > 0);

                        for (const name of playerNames) {
                            // Check if player is OP
                            const opCheck = await rcon.sendCommand(`minecraft:op ${name}`);
                            // If player is already OP, the response contains "Nothing changed" or similar
                            // If they weren't OP, we need to de-op them back
                            const wasAlreadyOp = opCheck.includes('Nothing changed') || opCheck.includes('already');

                            if (!wasAlreadyOp) {
                                // They weren't OP - de-op them and kick
                                await rcon.sendCommand(`deop ${name}`);
                                await rcon.sendCommand(`kick ${name} §c§lServer Locked §r§7- Only OP players may be online.`);
                                kickedPlayers.push(name);
                            }
                        }
                    }

                    // Broadcast to server
                    await rcon.sendCommand('say §c§l🔒 SERVER LOCKED §r§7- Only OP players may remain online.');

                    const embed = new EmbedBuilder()
                        .setTitle('🔒 Server Locked')
                        .setColor(0xff0000)
                        .setDescription('Server is now **locked**. Only OP players can join or stay.')
                        .addFields(
                            { name: 'Kicked Players', value: kickedPlayers.length > 0 ? kickedPlayers.join(', ') : 'None (no non-OP players online)' },
                            { name: 'Locked By', value: user }
                        )
                        .setTimestamp();
                    await interaction.editReply({ embeds: [embed] });

                    await log('🔒 Server Locked', `**${user}** locked the server.\nKicked: ${kickedPlayers.length > 0 ? kickedPlayers.join(', ') : 'None'}`, 0xff0000);

                } else {
                    // ── UNLOCK THE SERVER ──
                    serverLocked = false;

                    // Disable whitelist
                    await rcon.sendCommand('whitelist off');
                    await rcon.sendCommand('say §a§l🔓 SERVER UNLOCKED §r§7- All players may now join!');

                    const embed = new EmbedBuilder()
                        .setTitle('🔓 Server Unlocked')
                        .setColor(0x00ff00)
                        .setDescription('Server is now **unlocked**. All players can join again.')
                        .addFields({ name: 'Unlocked By', value: user })
                        .setTimestamp();
                    await interaction.editReply({ embeds: [embed] });

                    await log('🔓 Server Unlocked', `**${user}** unlocked the server.`, 0x00ff00);
                }

                // Update status immediately
                await updateStatusEmbed();
            } catch (err) {
                await interaction.editReply({ content: `❌ Lock failed: ${err.message}` });
                await log('❌ Lock Error', `Failed to toggle lock: ${err.message}`, 0xff0000);
            }
            break;
        }
    }
});

// ─── Express Server: Receive Webhooks from MC Plugin ───
const app = express();
app.use(express.json());

// Health check
app.get('/', (req, res) => {
    res.json({ status: 'ok', serverOnline, playerCount, serverLocked });
});

// Receive chat messages from Minecraft
app.post('/mc/chat', async (req, res) => {
    const { player, message } = req.body;
    try {
        if (CHAT_CHANNEL_ID) {
            const channel = await client.channels.fetch(CHAT_CHANNEL_ID);
            if (channel) await channel.send(`**${player}**: ${message}`);
        }
        await log('💬 MC Chat', `**${player}**: ${message}`, 0x55ff55);
    } catch (err) {
        console.error('[Webhook] Chat error:', err.message);
    }
    res.sendStatus(200);
});

// Receive join/leave events
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
        await log(action === 'join' ? '📥 Player Join' : '📤 Player Leave', `**${player}** ${action === 'join' ? 'joined' : 'left'} the server`, color);
    } catch (err) {
        console.error('[Webhook] Join error:', err.message);
    }
    res.sendStatus(200);
});

// Receive death events
app.post('/mc/death', async (req, res) => {
    const { player, message, isKing } = req.body;
    try {
        if (CHAT_CHANNEL_ID) {
            const channel = await client.channels.fetch(CHAT_CHANNEL_ID);
            if (channel) {
                if (isKing) {
                    const embed = new EmbedBuilder()
                        .setTitle('👑 The King Has Died!')
                        .setColor(0xff0000)
                        .setDescription(`**${player}** has fallen.\nEvent staff will determine the next leader.`)
                        .setTimestamp();
                    await channel.send({ embeds: [embed] });
                } else {
                    await channel.send(`☠️ **${player}** has died. ${message || ''}`);
                }
            }
        }
        if (EVENTS_CHANNEL_ID) {
            const evChannel = await client.channels.fetch(EVENTS_CHANNEL_ID);
            if (evChannel) {
                const embed = new EmbedBuilder()
                    .setTitle(isKing ? '👑💀 KING DEATH' : '☠️ Player Death')
                    .setColor(isKing ? 0xff0000 : 0x888888)
                    .setDescription(`**${player}**\n${message || 'Unknown cause'}`)
                    .setTimestamp();
                await evChannel.send({ embeds: [embed] });
            }
        }
        await log('☠️ Death', `**${player}**: ${message || 'Unknown cause'}${isKing ? ' **[KING]**' : ''}`, 0xff0000);
    } catch (err) {
        console.error('[Webhook] Death error:', err.message);
    }
    res.sendStatus(200);
});

// Receive kingdom events
app.post('/mc/kingdom', async (req, res) => {
    const { action, kingdom, player, message } = req.body;
    try {
        const embed = new EmbedBuilder()
            .setColor(0xffcc00)
            .setTimestamp();

        switch (action) {
            case 'created':
                embed.setTitle('🏰 New Kingdom Established!')
                    .setDescription(`**${kingdom}** has been founded by **${player}**!\n🛡️ 3 days of protection active.`);
                break;
            case 'destroyed':
                embed.setTitle('💀 Kingdom Has Fallen')
                    .setColor(0xff0000)
                    .setDescription(`The kingdom of **${kingdom}** has been destroyed.`);
                break;
            case 'leadership_transferred':
                embed.setTitle('👑 Leadership Transfer')
                    .setDescription(`**${player}** is now the leader of **${kingdom}**.`);
                break;
            case 'member_joined':
                embed.setTitle('📥 New Kingdom Member')
                    .setDescription(`**${player}** joined **${kingdom}**.`);
                break;
            case 'member_left':
                embed.setTitle('📤 Kingdom Member Left')
                    .setDescription(`**${player}** left **${kingdom}**.`);
                break;
            case 'member_kicked':
                embed.setTitle('🦶 Member Kicked')
                    .setColor(0xff4444)
                    .setDescription(`**${player}** was kicked from **${kingdom}**.`);
                break;
            case 'renamed':
                embed.setTitle('✏️ Kingdom Renamed')
                    .setDescription(`**${kingdom}**`);
                break;
            default:
                embed.setTitle('🏰 Kingdom Event')
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
        await log('🏰 Kingdom', `**${action}** | Kingdom: **${kingdom}** | Player: **${player || 'N/A'}**`, 0xffcc00);
    } catch (err) {
        console.error('[Webhook] Kingdom error:', err.message);
    }
    res.sendStatus(200);
});

// Receive server start/stop
app.post('/mc/server', async (req, res) => {
    const { action } = req.body;
    try {
        const embed = new EmbedBuilder()
            .setTimestamp();

        if (action === 'start') {
            serverOnline = true;
            embed.setTitle('🟢 Server Online')
                .setColor(0x00ff00)
                .setDescription('KingdomCraft server is now online!\nConnect: `continents.cc`');
        } else if (action === 'stop') {
            serverOnline = false;
            playerCount = 0;
            serverLocked = false;
            embed.setTitle('🔴 Server Offline')
                .setColor(0xff0000)
                .setDescription('KingdomCraft server has gone offline.');
        }

        if (CHAT_CHANNEL_ID) {
            const channel = await client.channels.fetch(CHAT_CHANNEL_ID);
            if (channel) await channel.send({ embeds: [embed] });
        }

        await log(action === 'start' ? '🟢 Server Start' : '🔴 Server Stop', `Server ${action === 'start' ? 'started' : 'stopped'}`, action === 'start' ? 0x00ff00 : 0xff0000);
        await updateStatusEmbed();
    } catch (err) {
        console.error('[Webhook] Server error:', err.message);
    }
    res.sendStatus(200);
});

// ─── Start ───
// Start Express FIRST so Railway sees a healthy service
app.listen(PORT, '0.0.0.0', () => {
    console.log(`[API] Listening on port ${PORT}`);
});

// Then connect Discord
console.log('[Bot] Logging in...');
console.log('[Bot] Token present:', !!TOKEN);
console.log('[Bot] Guild ID:', GUILD_ID || 'NOT SET');

client.login(TOKEN).catch(err => {
    console.error('[Bot] FATAL: Failed to login:', err.message);
});

// Global error handlers so the process doesn't crash
process.on('unhandledRejection', (err) => {
    console.error('[Bot] Unhandled rejection:', err);
});
process.on('uncaughtException', (err) => {
    console.error('[Bot] Uncaught exception:', err);
});
