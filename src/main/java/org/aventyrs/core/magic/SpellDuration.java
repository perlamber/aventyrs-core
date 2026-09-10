package org.aventyrs.core.magic;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.OptionalInt;
import java.util.function.Supplier;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SPELL_DURATION;

/**
 * A Magia's {@code Duração:} descriptor in full — the authored count <b>and the unit it was
 * authored in</b>, plus whichever companion its particular shape needs.
 *
 * <h2>Why not a plain {@code int} of Rodadas</h2>
 *
 * Rodadas is the correct canonical unit, and {@link #inRodadas()} produces it. But converting at
 * authoring time and keeping only the number loses data that a real mechanic reads: {@code
 * CriticalEffectType#POTENCIALIZAR} — 57 of the 145 complete Magias, the commonest Efeito
 * Crítico in the game — adds "+2d6 <b>unidades</b>", in the Magia's own unit. See {@link
 * DurationUnit}.
 *
 * <h2>Which fields each {@link DurationKind} carries</h2>
 *
 * <pre>
 * kind                count  unit   scalingAttribute  reference
 * ------------------  -----  -----  ----------------  ---------
 * INSTANTANEA         0      —      —                 —
 * FIXED               ≥0     req.   —                 —
 * TARGET_ATTRIBUTE    0      req.   required          —
 * UNTIL_END_OF_TURN   0      —      —                 —
 * SAME_AS_REFERENCED  0      —      —                 required
 * </pre>
 *
 * Enforced by the canonical constructor, which throws {@link IllegalOperationException} ({@code
 * INVALID_SPELL_DURATION}) on any other combination — the same cross-field pairing check {@link
 * SpellTargeting} applies to its own reach/range/area triple, and for the same reason: the
 * combination is authored data, so a meaningless one is a mistake rather than a value.
 *
 * <h2>Concentração is two phases, and {@link #count()} is the <em>trailing</em> one</h2>
 *
 * {@link #concentration()} is an orthogonal flag, not a kind — 19 of the 145 Magias read {@code
 * Concentração + N Rodada(s)}, and one reads bare {@code Concentração}. While the caster stays
 * focused the effect is active with <b>no countdown at all</b>; concentration breaks when the
 * caster <em>casts another Magia or attacks</em>, and only then does the {@code N} begin. So
 * {@code N} is what remains after the break, never a total, and {@code
 * concentracao()} (count 0) is the limiting case that ends the instant focus does.
 *
 * <p>A naive {@code getDuration() == 2} is wrong in both directions at once: it starts the clock
 * immediately, and it caps at two Rodadas an effect that could legitimately run a whole Cena.
 *
 * <p><b>Phase one needs no new machinery</b> — {@code TemporaryEffect.remainingRounds} is a
 * nullable {@code Integer} whose {@code tick()} no-ops and whose {@code isExpired()} stays false
 * while it is {@code null}, which is already this core's encoding for "runs until something stops
 * it" ({@code Sangramento}/{@code ManaPurge} Maior use exactly it). <b>Two things are still
 * missing</b>, and neither is a duration type:
 *
 * <ul>
 *   <li><b>The transition.</b> {@code remainingRounds} is private with a getter and no mutator,
 *       so nothing can move an effect from {@code null} to {@code N} when focus breaks.</li>
 *   <li><b>A caster-to-sustained-effects link.</b> Only 2 of the 19 land on the caster; the other
 *       17 sit on a <em>target's</em> {@code CombatantSheet} while the concentration is the
 *       caster's own state, and neither {@code TemporaryEffect} nor {@code TemporaryBonus}
 *       records who granted it. {@code Scene.grantedBlessings} is the precedent for the shape.</li>
 * </ul>
 *
 * <p>The break trigger also has no single chokepoint: {@code SpellCastingService#castSpell} is
 * one and {@code AttackDelivery#resolve} the other, but an attack Perícia can be rolled straight
 * through {@code AbstractSkillInteraction#applyTo} without passing either. Note that <b>being
 * attacked must not break concentration</b> — only the caster's own attack does, so {@code
 * AttackReceiver} is deliberately not a trigger. "One Concentração at a time" needs no invariant
 * anywhere: casting a second Magia breaks the first by the rule itself.
 *
 * <p>Concentração is never <em>defined</em> in any of the three source documents, only used as a
 * Duração value; the rule above is carried-in ruleset knowledge. It is also not perfectly
 * uniform — four Magias attach their own clauses (Solo Profano forbids movement while
 * concentrating, Refúgio Invisível can be broken by a third party inside it, Festim dos Mortos
 * exempts its summons outright, Raio Antivida binds caster and target together) — so those live
 * in the Magia's own prose rather than being generalised into this type.
 */
public record SpellDuration(DurationKind kind,
                            int count,
                            DurationUnit unit,
                            boolean concentration,
                            AttributeDomain scalingAttribute,
                            Supplier<SpellDuration> reference) {

    /** {@code Instantânea} — the effect resolves and is over. */
    public static final SpellDuration INSTANTANEA =
            new SpellDuration(DurationKind.INSTANTANEA, 0, null, false, null, null);

    /** {@code Até o final do turno}. */
    public static final SpellDuration UNTIL_END_OF_TURN =
            new SpellDuration(DurationKind.UNTIL_END_OF_TURN, 0, null, false, null, null);

    /** {@code Duração: Concentração} — ends the instant focus breaks, with no trailing count. */
    public static final SpellDuration CONCENTRACAO =
            new SpellDuration(DurationKind.FIXED, 0, DurationUnit.RODADA, true, null, null);

    public SpellDuration {
        if (kind == null || count < 0 || !isLegalCombination(kind, count, unit, scalingAttribute, reference)) {
            throw new IllegalOperationException(INVALID_SPELL_DURATION);
        }
    }

    private static boolean isLegalCombination(final DurationKind kind, final int count,
                                              final DurationUnit unit, final AttributeDomain scalingAttribute,
                                              final Supplier<SpellDuration> reference) {
        return switch (kind) {
            case FIXED -> unit != null && scalingAttribute == null && reference == null;
            case TARGET_ATTRIBUTE ->
                    count == 0 && unit != null && scalingAttribute != null && reference == null;
            case SAME_AS_REFERENCED ->
                    count == 0 && unit == null && scalingAttribute == null && reference != null;
            case INSTANTANEA, UNTIL_END_OF_TURN ->
                    count == 0 && unit == null && scalingAttribute == null && reference == null;
        };
    }

    /** {@code N Rodadas}. */
    public static SpellDuration rodadas(final int count) {
        return fixed(count, DurationUnit.RODADA, false);
    }

    /** {@code N minuto(s)} — kept in minutes, see {@link DurationUnit}. */
    public static SpellDuration minutos(final int count) {
        return fixed(count, DurationUnit.MINUTO, false);
    }

    /** {@code Até N Hora(s)} — kept in hours, see {@link DurationUnit}. */
    public static SpellDuration horas(final int count) {
        return fixed(count, DurationUnit.HORA, false);
    }

    /** {@code Concentração + N Rodadas} — {@code count} is the <b>trailing</b> count, not a total. */
    public static SpellDuration concentracaoMais(final int trailingRodadas) {
        return concentracaoMais(trailingRodadas, DurationUnit.RODADA);
    }

    /**
     * {@code Concentração + até N minuto} — the same trailing count in a unit other than Rodadas.
     * One Magia is authored this way ({@code PiromanciaSpell#LUZ_DE_VELA}).
     */
    public static SpellDuration concentracaoMais(final int trailingCount, final DurationUnit unit) {
        return fixed(trailingCount, unit, true);
    }

    private static SpellDuration fixed(final int count, final DurationUnit unit, final boolean concentration) {
        return new SpellDuration(DurationKind.FIXED, count, unit, concentration, null, null);
    }

    /** {@code 'Vigor do Alvo' Rodadas} — a count read off the target's own Attribute. */
    public static SpellDuration targetAttribute(final AttributeDomain attribute, final DurationUnit unit) {
        return new SpellDuration(DurationKind.TARGET_ATTRIBUTE, 0, unit, false, attribute, null);
    }

    /**
     * {@code A mesma de ‹other Magia›}. A {@link Supplier} rather than a resolved value because
     * the referenced Magia is almost always a sibling constant of the same tree enum, and Java
     * forbids a forward reference in a constructor argument even when qualified — the same reason
     * {@code org.aventyrs.core.feat.MetamagicoFeat} holds its {@code FeatRequirements} as one.
     */
    public static SpellDuration sameAs(final Supplier<SpellDuration> reference) {
        return new SpellDuration(DurationKind.SAME_AS_REFERENCED, 0, null, false, null, reference);
    }

    /**
     * This duration as a plain count of Rodadas, or {@link OptionalInt#empty()} when it cannot be
     * resolved from the Magia alone — {@link DurationKind#TARGET_ATTRIBUTE} and {@link
     * DurationKind#TARGET_ATTRIBUTE} needs a sheet to read the Attribute off and {@link
     * DurationKind#UNTIL_END_OF_TURN} the Scene's turn boundary. {@link
     * DurationKind#SAME_AS_REFERENCED} resolves through to whatever it points at.
     *
     * <p>For a Concentração duration this is the <b>trailing</b> count only — phase one has no
     * length, so there is nothing to add to it.
     */
    public OptionalInt inRodadas() {
        return switch (kind) {
            case INSTANTANEA -> OptionalInt.of(0);
            case FIXED -> OptionalInt.of(count * unit.getRodadas());
            case SAME_AS_REFERENCED -> reference.get().inRodadas();
            case TARGET_ATTRIBUTE, UNTIL_END_OF_TURN -> OptionalInt.empty();
        };
    }
}
