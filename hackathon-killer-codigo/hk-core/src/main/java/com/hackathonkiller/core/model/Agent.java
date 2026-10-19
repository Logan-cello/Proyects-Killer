package com.hackathonkiller.core.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Agente registrado en el coordinador (uno por worker local). */
public class Agent {

    private String id;
    private String name;
    private AgentRole role;
    private AdapterType adapterType;
    private List<String> capabilities = new ArrayList<>();
    private AgentStatus status = AgentStatus.ONLINE;
    private Instant registeredAt = Instant.now();
    private Instant lastHeartbeat = Instant.now();

    public Agent() {
        // requerido por Jackson
    }

    public Agent(String id, String name, AgentRole role, AdapterType adapterType, List<String> capabilities) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.adapterType = adapterType;
        if (capabilities != null) {
            this.capabilities = new ArrayList<>(capabilities);
        }
    }

    public void heartbeat() {
        this.lastHeartbeat = Instant.now();
        if (this.status == AgentStatus.OFFLINE) {
            this.status = AgentStatus.ONLINE;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AgentRole getRole() { return role; }
    public void setRole(AgentRole role) { this.role = role; }
    public AdapterType getAdapterType() { return adapterType; }
    public void setAdapterType(AdapterType adapterType) { this.adapterType = adapterType; }
    public List<String> getCapabilities() { return capabilities; }
    public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }
    public AgentStatus getStatus() { return status; }
    public void setStatus(AgentStatus status) { this.status = status; }
    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
}
