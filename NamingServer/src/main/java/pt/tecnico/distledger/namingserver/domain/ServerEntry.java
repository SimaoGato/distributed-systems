package pt.tecnico.distledger.namingserver.domain;

import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger;

public class ServerEntry {

    private String target;

    private String qualifier;

    public ServerEntry(String target, String qualifier) {
        this.target = target;
        this.qualifier = qualifier;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public NamingServerDistLedger.ServerEntry proto() {
        return NamingServerDistLedger.ServerEntry.newBuilder()
                .setQualifier(this.qualifier).setTarget(this.target)
                .build();
    }

    @Override
    public String toString() {
        return "ServerEntry{" +
                "target='" + target + '\'' +
                ", qualifier='" + qualifier + '\'' +
                '}';
    }
}