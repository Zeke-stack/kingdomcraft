const { Client, GatewayIntentBits, EmbedBuilder, PermissionFlagsBits, SlashCommandBuilder, REST, Routes, ActivityType } = require('discord.js');
const express = require('express');
const RconManager = require('./rcon');

// ─── Config ───
const TOKEN = process.env.DISCORD_TOKEN;
const GUILD_ID = process.env.DISCORD_GUILD_ID;
const CHAT_CHANNEL_ID = process.env.DISCORD_CHAT_CHANNEL;
const LOG_CHANNEL_ID = process.env.DISCORD_LOG_CHANNEL;
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
    await deployCommands();
    await rcon.connect();

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
                name: serverOnline ? `${playerCount} player${playerCount !== 1 ? 's' : ''} online | continents.cc` : 'Server Offline',
                type: ActivityType.Watching
            }],
            status: serverOnline ? 'online' : 'dnd'
        });
    }, 30000);

    // Initial status
    client.user.setPresence({
        activities: [{ name: 'Starting up...', type: ActivityType.Watching }],
        status: 'idle'
    });
});

// ─── Chat Sync: Discord → Minecraft ───
client.on('messageCreate', async (message) => {
    if (message.author.bot) return;
    if (message.channel.id !== CHAT_CHANNEL_ID) return;

    // Don't forward commands
    if (message.content.startsWith('/') || message.content.startsWith('!')) return;

    try {
        if (rcon.isConnected()) {
            // Sanitize message
            const clean = message.content
                .replace(/\\/g, '')
                .replace(/"/g, "'")
                .replace(/\n/g, ' ')
                .substring(0, 200);
            await rcon.sendChat(message.member?.displayName || message.author.username, clean);
        }
    } catch (err) {
        console.error('[Chat] Failed to send to MC:', err.message);
    }
});

// ─── Slash Command Handler ───
client.on('interactionCreate', async (interaction) => {
    if (!interaction.isChatInputCommand()) return;

    const { commandName } = interaction;

    switch (commandName) {
        case 'status': {
            const embed = new EmbedBuilder()
                .setTitle('⚔️ KingdomCraft Server Status')
                .setColor(serverOnline ? 0x00ff00 : 0xff0000)
                .addFields(
                    { name: 'Status', value: serverOnline ? '🟢 Online' : '🔴 Offline', inline: true },
                    { name: 'Players', value: `${playerCount}`, inline: true },
                    { name: 'IP', value: '`continents.cc`', inline: true },
                    { name: 'Version', value: 'Paper 1.21.1', inline: true },
                    { name: 'RCON', value: rcon.isConnected() ? '✅ Connected' : '❌ Disconnected', inline: true },
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
            // Staff only
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
    }
});

// ─── Express Server: Receive Webhooks from MC Plugin ───
const app = express();
app.use(express.json());

// Health check
app.get('/', (req, res) => {
    res.json({ status: 'ok', serverOnline, playerCount });
});

// Receive chat messages from Minecraft
app.post('/mc/chat', async (req, res) => {
    const { player, message } = req.body;
    if (!CHAT_CHANNEL_ID) return res.sendStatus(200);
    try {
        const channel = await client.channels.fetch(CHAT_CHANNEL_ID);
        if (channel) {
            await channel.send(`**${player}**: ${message}`);
        }
    } catch (err) {
        console.error('[Webhook] Chat error:', err.message);
    }
    res.sendStatus(200);
});

// Receive join/leave events
app.post('/mc/join', async (req, res) => {
    const { player, action } = req.body;
    if (!CHAT_CHANNEL_ID) return res.sendStatus(200);
    try {
        const channel = await client.channels.fetch(CHAT_CHANNEL_ID);
        if (channel) {
            const emoji = action === 'join' ? '🟢' : '🔴';
            await channel.send(`${emoji} **${player}** ${action === 'join' ? 'joined' : 'left'} the server`);
        }
    } catch (err) {
        console.error('[Webhook] Join error:', err.message);
    }
    res.sendStatus(200);
});

// Receive death events
app.post('/mc/death', async (req, res) => {
    const { player, message, isKing } = req.body;
    try {
        // Send to chat channel
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
        // Send to log channel
        if (LOG_CHANNEL_ID) {
            const logChannel = await client.channels.fetch(LOG_CHANNEL_ID);
            if (logChannel) {
                await logChannel.send(`[Death] ${player}: ${message || 'Unknown cause'}`);
            }
        }
    } catch (err) {
        console.error('[Webhook] Death error:', err.message);
    }
    res.sendStatus(200);
});

// Receive kingdom events
app.post('/mc/kingdom', async (req, res) => {
    const { action, kingdom, player, message } = req.body;
    if (!CHAT_CHANNEL_ID) return res.sendStatus(200);
    try {
        const channel = await client.channels.fetch(CHAT_CHANNEL_ID);
        if (channel) {
            const embed = new EmbedBuilder()
                .setColor(0xffcc00)
                .setTimestamp();

            switch (action) {
                case 'created':
                    embed.setTitle('🏰 New Kingdom Established!')
                        .setDescription(`**${kingdom}** has been founded by **${player}**!\n🛡️ 3 days of protection active.`);
                    break;
                case 'deleted':
                    embed.setTitle('💀 Kingdom Has Fallen')
                        .setColor(0xff0000)
                        .setDescription(`The kingdom of **${kingdom}** has been destroyed.`);
                    break;
                case 'transfer':
                    embed.setTitle('👑 Leadership Transfer')
                        .setDescription(`**${player}** is now the leader of **${kingdom}**.`);
                    break;
                case 'join':
                    embed.setTitle('📥 New Kingdom Member')
                        .setDescription(`**${player}** joined **${kingdom}**.`);
                    break;
                case 'leave':
                    embed.setTitle('📤 Kingdom Member Left')
                        .setDescription(`**${player}** left **${kingdom}**.`);
                    break;
                default:
                    embed.setTitle('🏰 Kingdom Event')
                        .setDescription(message || `${action} - ${kingdom} - ${player}`);
            }
            await channel.send({ embeds: [embed] });
        }
    } catch (err) {
        console.error('[Webhook] Kingdom error:', err.message);
    }
    res.sendStatus(200);
});

// Receive server start/stop
app.post('/mc/server', async (req, res) => {
    const { action } = req.body;
    if (!CHAT_CHANNEL_ID) return res.sendStatus(200);
    try {
        const channel = await client.channels.fetch(CHAT_CHANNEL_ID);
        if (channel) {
            if (action === 'start') {
                const embed = new EmbedBuilder()
                    .setTitle('🟢 Server Online')
                    .setColor(0x00ff00)
                    .setDescription('KingdomCraft server is now online!\nConnect: `continents.cc`')
                    .setTimestamp();
                await channel.send({ embeds: [embed] });
            } else if (action === 'stop') {
                const embed = new EmbedBuilder()
                    .setTitle('🔴 Server Offline')
                    .setColor(0xff0000)
                    .setDescription('KingdomCraft server has gone offline.')
                    .setTimestamp();
                await channel.send({ embeds: [embed] });
            }
        }
    } catch (err) {
        console.error('[Webhook] Server error:', err.message);
    }
    res.sendStatus(200);
});

// ─── Start ───
app.listen(PORT, '0.0.0.0', () => {
    console.log(`[API] Listening on port ${PORT}`);
});

client.login(TOKEN);
