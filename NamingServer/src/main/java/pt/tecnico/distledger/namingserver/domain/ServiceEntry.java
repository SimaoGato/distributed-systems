package pt.tecnico.distledger.namingserver.domain;

import java.util.HashSet;
import java.util.Set;

public class ServiceEntry {

    private String service;

    private Set<ServerEntry> serverEntries;

    public ServiceEntry(String service) {
        this.service = service;
        this.serverEntries  = new HashSet<ServerEntry>();
    }

    public ServiceEntry(String service, Set<ServerEntry> serverEntries) {
        this.service = service;
        this.serverEntries = serverEntries;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Set<ServerEntry> getServerEntries() {
        return serverEntries;
    }

    public void setServerEntries(Set<ServerEntry> serverEntries) {
        this.serverEntries = serverEntries;
    }

    public void addServerEntry(ServerEntry serverEntry) {
        this.serverEntries.add(serverEntry);
    }

    @Override
    public String toString() {
        return "ServiceEntry{" +
                "service='" + service + '\'' +
                ", serverEntries=" + serverEntries +
                '}';
    }
}
