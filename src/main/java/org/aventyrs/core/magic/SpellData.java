package org.aventyrs.core.magic;

import lombok.Builder;
import lombok.Getter;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

import java.util.Set;

/**
 * One Magia's authored rules-text block, in builder form — the shape the whole catalog is
 * written against. Every field is a descriptor line of {@code docs/rules/magias.txt}, named after
 * it, so a constant reads back as the block it was transcribed from:
 *
 * <pre>
 * SpellData.builder()
 *         .name("Cativar Animal")
 *         .branchLevel(BranchLevel.SEMENTE)
 *         .activationTime(ActivationTime.pa(2))
 *         .attackSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
 *         .castingDifficultyLevel(DifficultyLevel.EASY)
 *         .description("Torna um animal amigável a você.")
 *         .primaryEffectDescription("...")
 *         .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
 *         .duration(SpellDuration.rodadas(3))
 *         .targeting(SpellTargeting.TOQUE)
 *         .build()
 * </pre>
 *
 * <p>It is <b>data, not a {@link Spell}</b>: it carries no {@link SpellTree}, since every constant
 * of a tree's enum shares one and repeating it 9 times per file would be authored redundancy
 * that could disagree with itself. {@link AuthoredSpell} is what pairs the two.
 *
 * <p>Unset optional fields are exactly the document's absent lines — a {@code null} {@code
 * secondaryEffectDescription} is a Magia with no {@code Efeito Alternativo}, the same
 * not-applicable convention {@code ItemFavor} and every {@code InteractionResult} field follow.
 * The two {@code castingDifficulty*} escape hatches default to the ordinary fixed-tier case.
 */
@Getter
@Builder
public class SpellData {

    /** The identity line's name, e.g. {@code Semente – <b>Cativar Animal</b>}. */
    private final String name;

    /** The rung the identity line names. */
    private final BranchLevel branchLevel;

    /** Which ramificação, or {@code null} for a Magia on the trunk. */
    private final SpellBranch branch;

    /** {@code Tempo de Ativação:} */
    private final ActivationTime activationTime;

    /** {@code Perícia Chave para Conjuração:} — the delivery roll, see {@link Spell}. */
    private final SkillType attackSkillType;

    /** {@code GD da Conjuração:}, or {@code null} when the entry states no fixed tier. */
    private final DifficultyLevel castingDifficultyLevel;

    /** {@code GD da Conjuração: ‹tier› ou DM do Alvo (maior)} — the tier is a floor. */
    private final boolean castingDifficultyFlooredByTargetMagicDefense;

    /** {@code GD da Conjuração:} given as a table over the target effect's own rung. */
    private final boolean castingDifficultyScaledToTargetLevel;

    /** {@code Descrição:} */
    private final String description;

    /** {@code Efeito:} */
    private final String primaryEffectDescription;

    /**
     * The structured damage of {@link #primaryEffectDescription}, or {@code null} for a Magia
     * whose primary effect deals none (utility, healing, a buff) or whose damage cannot yet be
     * modelled (positional, delayed, falloff-only) — see {@link SpellDamage}.
     */
    private final SpellDamage primaryDamage;

    /**
     * The structured recovery of {@link #primaryEffectDescription}, or {@code null} for a Magia
     * that restores no Pontos de Vida — see {@link SpellHealing}. The healing twin of {@link
     * #primaryDamage}, and a Magia may author neither, but never both.
     */
    private final SpellHealing healing;

    /**
     * The Malefícios {@link #primaryEffectDescription} lifts — empty for a Magia that lifts none,
     * which is every one but {@code Vida}'s alternativo branch today. Defaulted rather than left
     * {@code null} because a caller iterates it, unlike the nullable descriptor lines around it.
     */
    @Builder.Default
    private final Set<ConditionType> cleansedConditions = Set.of();

    /** {@code Efeito Alternativo – ‹name›:}, or {@code null}. */
    private final String secondaryEffectDescription;

    /**
     * The structured half of {@link #secondaryEffectDescription} — the descriptor columns this
     * Magia's second version <em>overrides</em>, and the effect columns it carries. {@code null}
     * for a Magia with no {@code Efeito Alternativo} at all.
     *
     * <p>Paired with the prose above rather than replacing it: the prose stays the verbatim
     * transcription (and is what {@link AlternateSpellVersion} reports as the alternate's own
     * {@code Efeito:}), while this holds only what maps to a real type. See {@link
     * SpellAlternateEffect}.
     */
    private final SpellAlternateEffect alternateEffect;

    /** {@code Corrente de Efeitos – ‹name›:}, or {@code null}. */
    private final String effectChainDescription;

    /** {@code Efeito Crítico:}, or {@code null} for the document's two blanks. */
    private final CriticalEffectType criticalEffectType;

    /** {@code Duração:} */
    private final SpellDuration duration;

    /** {@code Alcance:} */
    private final SpellTargeting targeting;

    /** The second reach of an {@code Alcance: Pessoal ou Toque}, or {@code null}. */
    private final SpellTargeting alternateTargeting;
}
