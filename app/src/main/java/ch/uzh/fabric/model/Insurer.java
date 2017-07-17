package ch.uzh.fabric.model;

import java.util.List;

public class Insurer {
    private String name;
    private List<InsureProposal> proposals;

    public Insurer(String name, List<InsureProposal> proposals) {
        this.name = name;
        this.proposals = proposals;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<InsureProposal> getProposals() {
        return proposals;
    }

    public void setProposals(List<InsureProposal> proposals) {
        this.proposals = proposals;
    }
}
