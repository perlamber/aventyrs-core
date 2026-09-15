package org.aventyrs.core.magic;

import lombok.Builder;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

import java.util.Set;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SPELL_ALTERNATE_EFFECT;

/**
 * A Magia's {@code Efeito Alternativo} as a <b>delta over its parent's descriptor columns</b> —
 * the authored half of {@link AlternateSpellVersion}.
 *
 * <p><b>It is a second version of the same Magia, never a second Magia.</b> The ruleset is
 * explicit: <i>"Certas magias possuem Efeitos Alternativos, um personagem que aprenda a versão
 * base automaticamente aprende sua segunda versão"</i> ({@code docs/rules/magias.txt:29}). So it
 * is a column here rather than its own catalog entry — a constant of its own would cost
 * experience in {@code SpellService#grantSpell}, count as a foothold for the climb gate, and
 * change {@code SpellCatalog}'s count, none of which may happen for a version granted free.
 *
 * <p>It is also not a footnote: the same preamble says one ramificação <i>"foca na evolução dos
 * Efeitos Alternativos"</i> (L30), which is what {@code BranchRole#ALTERNATIVO} names. 64 of the
 * 145 authored Magias carry one, and <b>40 of those override at least one parent column</b> —
 * eight override three or more. A prose string cannot represent that, which is why this exists.
 *
 * <h2>Every field is an override; {@code null} means "inherited"</h2>
 *
 * That is the whole contract, and it is what keeps this a delta rather than a second full
 * descriptor block. Three shapes the catalog forced:
 *
 * <ul>
 *   <li><b>{@link #suppressesCriticalEffect} is a separate flag</b> from {@link
 *   #criticalEffectType}, because a {@code null} type already means "inherited" and cannot also
 *   mean "this version has none" — <i>Portal de Fuga</i>'s "não recebe os benefícios de Efeitos
 *   Críticos" needs the third state.</li>
 *   <li><b>{@link #castingDifficultyFlooredByTargetMagicDefense} is a {@code Boolean}</b>, boxed
 *   for the same tri-state reason: an alternate may switch the floor on <em>or</em> off.</li>
 *   <li><b>{@link #attackSkillType} and {@link #castingDifficultyLevel} co-vary</b> — in four of
 *   the five Perícia overrides the GD flips to {@code DM do Alvo} in the same sentence. They stay
 *   separate fields, because each maps 1:1 onto a parent column and that mapping is what makes
 *   the decorator mechanical; the co-variance is an authoring fact, not a structural one.</li>
 * </ul>
 *
 * <p><b>The effect prose is deliberately not here.</b> It already lives on the parent as {@code
 * SpellData#secondaryEffectDescription} for all 64, and {@link AlternateSpellVersion} reads it
 * from there as its own {@code Efeito:} line. Carrying it twice would be two sources of truth for
 * one transcription. Only the alternate's {@link #name} is here, because the prose embeds it.
 *
 * <p><b>Author an override only where it maps to a real type.</b> A reach stated in metres or as
 * a dice formula ("até 4m", "cone de até 6 metros", "2d6*100 UD") has no {@code Range} band, so
 * {@link #targeting} stays {@code null} and the clause stays prose with a TODO — the same
 * restraint every unmodellable clause in this catalog gets.
 *
 * @param name             the alternate's own name, e.g. {@code "Procrastinar Ferimento"}
 * @param activationTime   a different {@code Tempo de Conjuração} — Reação, Ação Livre or NPA.
 *                         Runs <b>both directions</b>: two alternates convert a Reação back to PA
 * @param targeting        a different {@code Alcance}, where it maps to a {@link SpellTargeting}
 * @param duration         a different {@code Duração}
 * @param castingDifficultyLevel a different {@code GD da Conjuração}
 * @param attackSkillType  a different {@code Perícia Chave} — Domínio do Mana to an attack Perícia
 * @param castingDifficultyFlooredByTargetMagicDefense the {@code ou DM do Alvo (Maior)} floor
 * @param criticalEffectType a different {@code Efeito Crítico}
 * @param suppressesCriticalEffect whether this version receives no Efeito Crítico at all
 * @param manaCost         a different PM cost; {@code 0} for "dispensa o uso de PM"
 * @param effectChainDescription its own {@code Corrente de Efeitos}, for the ten that declare one
 * @param healing          the recovery this version performs — see {@link SpellHealing}
 * @param cleansedConditions the Malefícios this version lifts
 * @param primaryDamage    the damage this version deals — see {@link SpellDamage}
 */
@Builder
public record SpellAlternateEffect(
        String name,
        ActivationTime activationTime,
        SpellTargeting targeting,
        SpellDuration duration,
        DifficultyLevel castingDifficultyLevel,
        SkillType attackSkillType,
        Boolean castingDifficultyFlooredByTargetMagicDefense,
        CriticalEffectType criticalEffectType,
        boolean suppressesCriticalEffect,
        Integer manaCost,
        String effectChainDescription,
        SpellHealing healing,
        Set<ConditionType> cleansedConditions,
        SpellDamage primaryDamage) {

    public SpellAlternateEffect {
        if (name == null || name.isBlank()) {
            throw new IllegalOperationException(INVALID_SPELL_ALTERNATE_EFFECT);
        }
        // Naming an Efeito Crítico and suppressing every Efeito Crítico are contradictory
        // authorings, not a value — the same cross-field validation SpellDamage/SpellHealing apply.
        if (suppressesCriticalEffect && criticalEffectType != null) {
            throw new IllegalOperationException(INVALID_SPELL_ALTERNATE_EFFECT);
        }
        if (manaCost != null && manaCost < 0) {
            throw new IllegalOperationException(INVALID_SPELL_ALTERNATE_EFFECT);
        }
        cleansedConditions = cleansedConditions == null ? Set.of() : Set.copyOf(cleansedConditions);
    }

    /** The commonest shape by far: a named alternate that overrides no descriptor column. */
    public static SpellAlternateEffect named(final String name) {
        return SpellAlternateEffect.builder().name(name).build();
    }
}
