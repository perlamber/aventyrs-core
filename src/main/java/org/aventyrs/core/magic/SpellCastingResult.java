package org.aventyrs.core.magic;

import lombok.Builder;
import lombok.Getter;
import org.aventyrs.core.effect.SpellEffect;
import org.aventyrs.core.scene.ActiveAreaSpellEffect;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.AreaDamage;
import org.aventyrs.core.sheet.InteractionResult;

import java.util.List;

/**
 * The outcome of {@link SpellCastingService#castSpell} — the two rolls a Magia's casting
 * involves: whichever Perícia delivered it (e.g. Ataque à Distância for a ranged spell) and
 * the follow-up Domínio do Mana roll.
 */
@Getter
@Builder
public class SpellCastingResult {
    InteractionResult deliveryResult;
    InteractionResult dominioDoManaResult;

    /**
     * How many níveis the caster's Talentos take off this version's GD da Conjuração — see {@link
     * SpellCastingService#resolveCastingDifficultyReduction}. 0 on the legacy overload.
     */
    int castingDifficultyReduction;

    /**
     * The GD da Conjuração the Domínio do Mana roll is made against: the authored {@code
     * Spell#getCastingDifficultyLevel()} eased by {@link #castingDifficultyReduction}. {@code null}
     * when the Magia states no fixed tier. Report-only — nothing compares a roll against it, and
     * an "ou DM do Alvo (maior)" floor is still the caller's to apply on top.
     */
    org.aventyrs.core.skill.DifficultyLevel castingDifficultyLevel;

    /**
     * This version's Tempo de Ativação as the caster pays it on this cast — the authored figure
     * after any Talento reduction, see {@link SpellCastingService#resolveActivationTime}. Reported,
     * not spent. {@code null} on the legacy overload.
     */
    ActivationTime activationTime;

    Integer durationInRounds;
    ActiveAreaSpellEffect areaSpellEffect;

    /**
     * The Magia's primary damage resolved against the caster — {@code null} when the Magia
     * authors no {@link SpellDamage}. The {@code deterministicAmount} is ready; the caller still
     * rolls the {@code diceCount} d6, adds them, and runs its own {@code DamageInteraction} with
     * the type/element for mitigation. See {@link SpellCastingService#resolvePrimaryDamage}.
     */
    ResolvedSpellDamage primaryDamage;

    /**
     * The Magia's {@code Efeito:} line as a ready-to-apply {@link SpellEffect} — {@code null} when
     * the Magia authors none this core can express yet. See {@link
     * SpellCastingService#resolveEffect}.
     *
     * <p><b>Report-only, like {@link #primaryDamage} and {@link #recordedAction}.</b> The effect
     * mutates a sheet when run, but {@code castSpell} never runs it: this core resolves no target
     * GD, so it cannot tell whether the cast landed. The caller decides, then drives it:
     *
     * <pre>{@code
     * InteractionResult r = target.receiveInteraction(result.getSpellEffect());
     * while (r.getNextInteraction() != null) {
     *     r = target.receiveInteraction(r.getNextInteraction());
     * }
     * }</pre>
     *
     * <p>A Corrente de Efeitos the caller judged triggered is chained on first, with {@code
     * AbstractEffect#chainInto} — {@code Sobrecura} is the one this tree needs.
     */
    SpellEffect spellEffect;

    /**
     * The cast bundled as a ready-to-file {@link CombatantAction} — the delivering Perícia, the
     * {@link Spell} as its {@code attackSource} (which is what {@code
     * AttributeAbility#upgradesFirstSpellOfRoundFocusScaling}'s "primeira Magia da Rodada" reads
     * back), and the {@code turnNumber} from the request's Scene. {@code null} on the legacy
     * {@code castSpell(CombatantSheet, Interaction)} overload, which has no Scene.
     *
     * <p>Partial by nature: {@code castSpell} computes the two rolls' bonuses but the caller rolls
     * the dice, so {@code governingAttributeDomain} and the {@code ActionOutcome} verdict are not
     * filled — unlike {@code org.aventyrs.core.combat.DeliveredAttackResult#getRecordedAction()},
     * where the roll is supplied. {@link SpellCastingService#castSpell} does <b>not</b> record it;
     * the caller files it with {@code scene.recordAction(caster, action)}.
     */
    CombatantAction recordedAction;

    /**
     * Whether a Magia that would otherwise simply land must first overcome the target's DM — Fanático
     * de Cyt's "Magias Conjuradas por terceiros sempre contam como magias hostis, mesmo as magias
     * benéficas … devem superar sua DM para lhe afetar". {@code null}/false for an ordinary cast.
     */
    Boolean mustOvercomeMagicDefense;

    /** The Frenesi Arcano option this cast spent, or {@code null}. */
    SpellEmpowerment empowerment;
    /**
     * The PM this cast costs — the Magia's own figure times every held Título's multiplier (Benção
     * de Boros' "o dobro de PM"). Reported, never spent, like the PA; 0 when paid in PV instead.
     */
    int manaCost;
    /**
     * PV the caster pays in place of {@link #manaCost} (Transferir Vitalidade), for the caller to
     * apply through {@code CombatantSheet#payWithVitality}. 0 unless the request asked for it.
     */
    int vitalityCost;

    /**
     * Extra damage this cast deals around its caster — Cataclismo Elemental's "suas Magias de
     * Duração instantânea … causam, como efeito adicional, dano Mágico Elemental" — reported for the
     * caller to apply. {@code null} when there is none.
     */
    List<AreaDamage> areaDamage;

    /**
     * How many Rodadas after the cast the Magia's effect begins — "Você pode fazer com que suas
     * magias iniciem seu efeito 1 Rodada após a conjuração" ({@code
     * MetamagicoFeat#PROCRASTINAR_CONJURACAO}, opted into on {@code SpellCastRequest}). 0 for an
     * ordinary cast. <b>Reported, not scheduled</b>: the caller holds the effect back.
     */
    private final int effectDelayRounds;

    /**
     * PV this cast's healing gains or loses — every held Talento's {@code Feat#resolveSpellHealingBonus}
     * (Conjuração Rápida's Desvantagem "nas rolagens de … Cura mágica", Arcanismo Elemental's +2). Already
     * folded into {@link #spellEffect}; reported for a caller building its own per-target effects
     * ({@code SpellEffectContext#withHealingBonus}). 0 for an ordinary cast.
     */
    private final int healingBonus;

    /**
     * Whether the named target is immune to this Magia — Aptidão Mágica Dracônica's "imune a Magias
     * que você é capaz de conjurar" (table ruling, 2026-09-27: no damage and no effects). The caller
     * skips {@link #spellEffect} and {@link #primaryDamage} for them.
     */
    private final boolean targetImmune;
}
