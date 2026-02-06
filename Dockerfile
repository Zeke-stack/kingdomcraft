# Build stage for custom plugins
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy plugin source code
COPY plugins/AdvancedInvViewer ./plugins/AdvancedInvViewer
COPY plugins/StaffCommands ./plugins/StaffCommands
COPY plugins/KingdomCraft ./plugins/KingdomCraft

# Build plugins
RUN cd plugins/AdvancedInvViewer && mvn clean package -q && \
    cd ../StaffCommands && mvn clean package -q && \
    cd ../KingdomCraft && mvn clean package -q

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /server

# Copy server files
COPY . .

# Copy built plugins from builder stage
COPY --from=builder /build/plugins/AdvancedInvViewer/target/AdvancedInvViewer.jar ./plugins/AdvancedInvViewer.jar
COPY --from=builder /build/plugins/StaffCommands/target/StaffCommands.jar ./plugins/StaffCommands.jar
COPY --from=builder /build/plugins/KingdomCraft/target/KingdomCraft.jar ./plugins/KingdomCraft.jar

# Make start script executable
RUN chmod +x start.sh

# Expose Minecraft server port
EXPOSE 25565

# Expose voice chat port if needed
EXPOSE 24454/udp

# Run the start script
CMD ["./start.sh"]
