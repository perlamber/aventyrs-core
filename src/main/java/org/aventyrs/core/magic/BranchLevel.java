package org.aventyrs.core.magic;

public enum BranchLevel {
    SEMENTE(0), BROTO(1), MUDA(3), EMERGENTE(5), FLORESCENTE(7);

    int manaCost;
    public int getManaCost() {
        return manaCost;
    }
    BranchLevel(int manaCost) {
        this.manaCost = manaCost;
    }
}
