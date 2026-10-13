package org.aventyrs.core.character;

/**
 * The resistances of {@code docs/rules/defesas-e-resistencias.txt} / {@code acoes-de-combate.txt} — the single place
 * their per-instance figures live (table ruling, 2026-10-13).
 *
 * <p><b>Every resistance is a count of instances.</b> A clause naming "RD" (or "RD 1") grants one instance, "RDS 5" or
 * "RDS igual a 1 + …" that many; a source writes {@link #instances(int)} and never a raw point figure, so a changed
 * per-instance value reaches every source at once. An item Favor saying only "Dano … sofrido é reduzido em -N" grants N
 * {@link #RDS} instances (table ruling, 2026-10-13).
 *
 * <p>The services still sum <em>points</em> ({@code ModifierType#DAMAGE_REDUCTION} and kin hold the reduction a hit
 * loses); this enum is how a source turns its instances into them.
 */
public enum Resistance {

    /** Resistência à Danos — "Cada instância reduz o Dano Físico não-PRIMORDIAL e não-ELEMENTAL em -2". */
    RD(2),

    /** Resistência à Magias — "Cada instância reduz o Dano Mágico não-PRIMORDIAL em -2". */
    RM(2),

    /** Resistência Elemental — "Cada instância reduz Danos Elementais e Mágicos Elementais em -3" (V19 20261012). */
    RE(3),

    /** Resistência Absoluta — "Cada instância reduz os Danos Físicos, Mágicos e Elementais … em -2". */
    RA(2),

    /**
     * Redução de Danos Sofridos — "Cada instância reduz Danos de ordens Físicas e Mágicas em -1, não aplicável à danos
     * Primordiais, Profanos, Sagrados e Umbrais".
     */
    RDS(1),

    /**
     * Resistência à Críticos — "A Margem Crítica Menor dos ataques não-PRIMORDIAIS sofridos é reduzida em -2"; the Maior
     * by {@link #RC_MAJOR_MARGIN}.
     */
    RC(2);

    /** RC: "A Margem Crítica Maior dos ataques sofridos não-PRIMORDIAIS é reduzida em -1", per instance. */
    public static final int RC_MAJOR_MARGIN = 1;

    /** RA: Margem Crítica Menor and Maior each "reduzida em -1", not cumulative between instances. */
    public static final int RA_CRITICAL_MARGIN = 1;

    /** RD/RM/RE/RA: "Reduz a Duração de Efeitos … contínuos em -1 Unidade", not cumulative between instances. */
    public static final int DURATION_REDUCTION = 1;

    /**
     * Resistência à Corrente de Efeitos — "O valor padrão é 5": a Corrente fires only once the attack beats the
     * target's Defesas by this much. Increases and reductions are always enumerated (+N / −N), never instances.
     */
    public static final int EFFECT_CHAIN_BASE = 5;

    private final int perInstance;

    Resistance(final int perInstance) {
        this.perInstance = perInstance;
    }

    /** What one instance reduces (a Margem Crítica Menor, for {@link #RC}). */
    public int getPerInstance() {
        return perInstance;
    }

    /** The points count instances of this resistance are worth. */
    public int instances(final int count) {
        return count * perInstance;
    }

    /** One instance — what a numberless "RD" / "recebe RDS" grants. */
    public int instance() {
        return perInstance;
    }
}
