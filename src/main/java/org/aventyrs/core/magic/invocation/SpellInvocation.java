package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.monster.summon.NatureSummonKind;

import java.util.Set;

/**
 * What a Magia invokes (core 0.1.7.6) — the column a {@code SpellData}/{@code SpellAlternateEffect} names so the Magia
 * itself says what its cast brings into the Cena, read through {@code Spell#getInvocation()} and reported on {@code
 * SpellCastingResult#getInvocation()}. {@link SpellInvocationService#plan} turns one into an {@link InvocationOrder};
 * before this column a caller had to recognise each Magia by its constant and pick the {@link NatureInvocationService}
 * or {@link UndeadInvocationService} method itself.
 *
 * <p>One constant per <em>shape</em> a cast takes, so a Magia and its Efeito Alternativo name different ones when the
 * second version invokes something else (Predador Regional, Laboratório de Lacerto, Servo Cadavérico, Açougueiro,
 * Horda Faminta, Falsa Matilha). Each carries the static facts a caller asks <em>before</em> the cast — what it targets
 * and whether it takes room under the caster's {@link SummonLimit}.
 *
 * <p>Invocar Abantesma names none yet: casting it on its own is still open (the Abantesma is only ever a Festim roll).
 */
public enum SpellInvocation {

    /** Cativar Animal: the touched animal becomes the caster's Subordinado. */
    CATIVAR_ANIMAL(Shape.SUBORDINATE, Target.ANIMAL, false, Set.of()),
    /** Falsa Matilha: Cativar Animal's Efeito Alternativo. */
    FALSA_MATILHA(Shape.SUBORDINATE, Target.ANIMAL, false, Set.of()),
    /** Aliados da Natureza: an Aliado, replacing the caster's previous fauna. */
    ALIADO(Shape.SUMMON, Target.BESIDE_CASTER, true, Set.of(NatureSummonKind.ALIADO_DA_NATUREZA,
            NatureSummonKind.PREDADOR_REGIONAL)),
    /** Predador Regional: Aliados da Natureza's Efeito Alternativo, +1PM. */
    PREDADOR_REGIONAL(Shape.SUMMON, Target.BESIDE_CASTER, true, Set.of(NatureSummonKind.ALIADO_DA_NATUREZA,
            NatureSummonKind.PREDADOR_REGIONAL)),
    /** Canção de Flora: several animals, priced per animal; its Corrente is Fauna e Flora. */
    CANCAO_DE_FLORA(Shape.SUMMON, Target.BESIDE_CASTER, true, Set.of(NatureSummonKind.ALIADO_DA_NATUREZA,
            NatureSummonKind.PREDADOR_REGIONAL)),
    /** Experimento de Lacerto. */
    EXPERIMENTO(Shape.SUMMON, Target.BESIDE_CASTER, true, Set.of(NatureSummonKind.EXPERIMENTO_DE_LACERTO)),
    /** Totem de Gaea: a spawner of Predadores Regionais. */
    TOTEM_DE_GAEA(Shape.SPAWNER, Target.BESIDE_CASTER, true, Set.of()),
    /** Orgulho de Lacerto. */
    ORGULHO(Shape.SUMMON, Target.BESIDE_CASTER, true, Set.of(NatureSummonKind.ORGULHO_DE_LACERTO,
            NatureSummonKind.EXPERIMENTO_DE_LACERTO)),
    /** Laboratório de Lacerto: Orgulho de Lacerto's Efeito Alternativo. */
    LABORATORIO(Shape.SUMMON, Target.BESIDE_CASTER, true, Set.of(NatureSummonKind.ORGULHO_DE_LACERTO,
            NatureSummonKind.EXPERIMENTO_DE_LACERTO)),
    /** Despertar Anciente de Gaea. */
    ANCIENTE(Shape.SUMMON, Target.BESIDE_CASTER, true, Set.of()),
    /** Reanimar: a Zumbi raised from the touched body. */
    ZUMBI(Shape.SUMMON, Target.CORPSE, true, Set.of()),
    /** Servo Cadavérico: Reanimar's Efeito Alternativo — the body becomes a Subordinado, not an invocation. */
    SERVO_CADAVERICO(Shape.SUBORDINATE, Target.CORPSE, false, Set.of()),
    /** Criar Carniçal. */
    CARNICAL(Shape.SUMMON, Target.CORPSE, true, Set.of()),
    /** Açougueiro: Criar Carniçal's Efeito Alternativo, +5PM and no Duração. */
    ACOUGUEIRO(Shape.SUMMON, Target.CORPSE, true, Set.of()),
    /** Festim dos Mortos: the Sarcófago Negro, whose excess rises uncontrolled — so it is never refused at the limit. */
    FESTIM(Shape.SPAWNER, Target.BESIDE_CASTER, false, Set.of()),
    /** Horda Faminta: Festim dos Mortos' Efeito Alternativo, the Jazigo Profano. */
    HORDA_FAMINTA(Shape.SPAWNER, Target.BESIDE_CASTER, false, Set.of());

    /** What a cast leaves behind. */
    public enum Shape {
        /** Creatures, each its own participant ({@link InvocationOrder.Summons}). */
        SUMMON,
        /** A spawner invoking one creature at a time ({@link InvocationOrder.Spawner}). */
        SPAWNER,
        /** A touched body or animal made a Subordinado ({@link InvocationOrder.Subordination}). */
        SUBORDINATE
    }

    /** Who the cast is aimed at. */
    public enum Target {
        /** Nobody — the cast is aimed at its caster and the creature appears beside them. */
        BESIDE_CASTER,
        /** A body {@link RaiseDeadService} can raise. */
        CORPSE,
        /** A touched creature, judged an animal by {@link NatureInvocationService#captivate}. */
        ANIMAL
    }

    private final Shape shape;
    private final Target target;
    private final boolean countsTowardSummonLimit;
    private final Set<NatureSummonKind> replacedKinds;

    SpellInvocation(final Shape shape, final Target target, final boolean countsTowardSummonLimit,
                    final Set<NatureSummonKind> replacedKinds) {
        this.shape = shape;
        this.target = target;
        this.countsTowardSummonLimit = countsTowardSummonLimit;
        this.replacedKinds = replacedKinds;
    }

    public Shape shape() {
        return shape;
    }

    public Target target() {
        return target;
    }

    /**
     * Whether the cast is refused once the caster has no room left under their {@link SummonLimit} — every invocation
     * but a Subordinado and Festim dos Mortos (table ruling, 2026-10-14).
     */
    public boolean countsTowardSummonLimit() {
        return countsTowardSummonLimit;
    }

    /**
     * The caster's standing summons this cast sends away ("o anterior desaparece" — the exclusivity groups), which
     * therefore free their room before the limit is judged. Empty for the rest.
     */
    public Set<NatureSummonKind> replacedKinds() {
        return replacedKinds;
    }

    /**
     * Whether the caster's per-cast {@link InvocationOptions} (Bruxo's Invocação Maior/Dupla, the comet) reach this
     * invocation — the nature tree's creatures and Totem, which {@link NatureInvocationService#enhance} improves.
     */
    public boolean acceptsInvocationOptions() {
        return switch (this) {
            case ALIADO, PREDADOR_REGIONAL, CANCAO_DE_FLORA, EXPERIMENTO, TOTEM_DE_GAEA, ORGULHO, LABORATORIO,
                 ANCIENTE -> true;
            default -> false;
        };
    }
}
