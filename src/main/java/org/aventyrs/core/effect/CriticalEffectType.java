package org.aventyrs.core.effect;

/**
 * Which Efeito Crítico an {@link CriticalEffect} <i>is</i> — the identity a rules text names when
 * it refers to one without applying it.
 *
 * <h2>Why an enum rather than the implementing class</h2>
 *
 * Everywhere else in this core an effect is identified by its own class: {@code
 * AbstractCombatantSheet#applyEffect} de-duplicates non-cumulative effects with {@code
 * existing.getClass() == effect.getClass()}, and {@code heal} clears {@code Bleeding} with an
 * {@code instanceof}. That works because those call sites always hold a real instance.
 *
 * <p>A creature's immunity list doesn't. A stat block names the Efeitos Críticos its anatomy
 * shrugs off, and it names <b>all</b> of them — including ones this core has not built. Keyed on
 * {@code Class<? extends CriticalEffect>}, an immunity to Dilacerar would be inexpressible until
 * the day a {@code Dilacerar} class exists, and the stat block would have to lie by omission in
 * the meantime. Keyed on this enum, the immunity is real, exact, authored data from the start,
 * and the day the effect is implemented it is <i>already</i> being resisted correctly — the same
 * "can't apply it yet doesn't mean can't compute it yet" discipline an unread {@code ItemBonus}
 * column follows.
 *
 * <h2>Four of these have no class behind them</h2>
 *
 * <ul>
 *   <li><b>Implemented</b> — {@link #SANGRAMENTO} ({@link Sangramento}), {@link #PURGA_DE_MANA}
 *   ({@link ManaPurge}), {@link #PRIMOR} ({@link Primor}), {@link #SABOTAGEM} ({@link Sabotage}),
 *   {@link #EXECUCAO_REAL} ({@link RealExecution}).</li>
 *   <li><b>Named only</b> — {@link #AMALDICOAR}, {@link #DILACERAR}, {@link #EXCRUCIANTE},
 *   {@link #FERIDA_PROFUNDA}. Nothing produces these yet; they exist here so an immunity to them
 *   can be authored, and so that whoever builds one has the constant waiting. A caller cannot
 *   currently construct an effect reporting one of these types, which is why filtering on them
 *   is a no-op today rather than an error.</li>
 * </ul>
 *
 * <p>This is <b>not</b> a catalog of every Efeito Crítico the game has — only the ones some
 * piece of authored data in this core needs to name. Add a constant when something names it,
 * not for completeness.
 */
public enum CriticalEffectType {

    /** Bleeding — immediate PV loss plus a per-Rodada drain. See {@link Sangramento}. */
    SANGRAMENTO,

    /** Mana burn. See {@link ManaPurge}. */
    PURGA_DE_MANA,

    /** The attacker's flourish, spending a temporary Ego point. See {@link Primor}. */
    PRIMOR,

    /** Targets the victim's equipment. See {@link Sabotage}. */
    SABOTAGEM,

    /** An outright kill. See {@link RealExecution}. */
    EXECUCAO_REAL,

    /** Named by stat blocks; no implementation yet. */
    AMALDICOAR,

    /** Named by stat blocks; no implementation yet. */
    DILACERAR,

    /** Named by stat blocks; no implementation yet. */
    EXCRUCIANTE,

    /** Named by stat blocks; no implementation yet. */
    FERIDA_PROFUNDA
}
