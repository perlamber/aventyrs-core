package org.aventyrs.core.combat;

import org.aventyrs.core.sheet.AttackerGuard;
import org.aventyrs.core.effect.DefensiveCriticalEffects;
import org.aventyrs.core.effect.DefensiveCriticalEffectType;
import org.aventyrs.core.effect.DefensiveCriticalEffect;
import lombok.NonNull;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.effect.CriticalEffect;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.effect.Effect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.EffectChainService;
import org.aventyrs.core.effect.EffectChainServiceImpl;
import org.aventyrs.core.sheet.ActionOutcome;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararInteraction;

import java.util.ArrayList;
import java.util.List;

/**
 * The entry point for "this character is being attacked" — the target-side half both {@code
 * AtaqueADistanciaInteraction} and {@code AtaqueCorpoACorpoInteraction} document as missing
 * ("the rules text compares this roll against a target's DF or DM rather than a fixed GD, but
 * that target-side lookup/conversion is left to a layer above this core").
 *
 * <p>It orchestrates rather than computes. An attack against a character is resolved as that
 * character's own Esquiva e Aparar roll (this game's dice are always rolled by the player)
 * against the Grau de Dificuldade the attack presents, resisting with DF or DM. So this class
 * rolls the defense through {@link EsquivaEApararInteraction}, compares the total against the
 * attack's threshold, and answers three independent questions about what landed:
 *
 * <ul>
 *   <li><b>Did it hit?</b> The defense total fell short of the threshold.</li>
 *   <li><b>Was it a critical?</b> The defense roll came up a Falha Crítica — inverted, because
 *   it's the <i>defender</i> rolling, so their critical failure is the attacker's critical hit.</li>
 *   <li><b>Did it clear the Corrente de Efeitos threshold?</b> The attack beat the defense by at
 *   least {@link EffectChainService#getRequiredMargin} — 5 normally, 7 against a defender holding
 *   {@code AutocontroleAdvantage#RESOLUTO}.</li>
 * </ul>
 *
 * <p>The last two are genuinely independent: an attack can clear the Corrente threshold without
 * critting, and crit without clearing it.
 *
 * <p><b>Inference worth flagging:</b> {@code EffectChainService}'s margin math was written for
 * the ordinary direction, where the <i>triggering</i> roll must surpass a challenge number. Here
 * the roll belongs to the defender, so the same margin is applied to the inverted comparison —
 * by how much the attack beat the defense. The margin constants and RESOLUTO's effect on them
 * are the service's own, confirmed rules text; applying them to a defender-rolled attack is a
 * reading of that text, not something it states.
 *
 * <h2>What it produces: a pre-wired chain, not damage</h2>
 *
 * Damage is deliberately absent. Turning a roll into a raw damage figure needs a weapon/dano-roll
 * concept this core doesn't have — the still-manual "Skill -&gt; Damage handoff" {@code
 * org.aventyrs.core.effect}'s package-info names. What this class can do is decide <i>which
 * stages an attack triggers</i>, and assemble them: on a hit, {@link IncomingAttackResult
 * #getDefenseResult()}'s {@code nextInteraction} is the head of a chain running
 * Damage → every triggered Corrente de Efeitos → every triggered Efeito Crítico, matching the
 * pipeline order that package-info documents. The caller drains it with the loop it already
 * documents, supplying the damage figure at the head:
 *
 * <pre>{@code
 * IncomingAttackResult attack = attackReceiver.resolve(incoming);
 * Interaction<CombatantSheet> stage = attack.getDefenseResult().getNextInteraction();
 * if (stage instanceof DamageInteraction damage) {
 *     InteractionResult result = damage.applyTo(defender, rawDamage, false);
 *     while (result.getNextInteraction() != null) {
 *         result = defender.receiveInteraction(result.getNextInteraction());
 *     }
 * }
 * }</pre>
 *
 * <p><b>Report-only otherwise.</b> {@link #resolve} assembles the chain but applies none of it,
 * and touches no resource on the defender — the same restraint {@code
 * GritoDeGuerraVulcanoInteraction} applies to the Blessings it reports. The one unavoidable
 * exception is the defense roll itself: {@code applyTo} may grant a temporary Ego point on a
 * critical success (the first-roll-of-Turn check it also runs is non-mutating now). That's the
 * roll genuinely happening, not an outcome being applied — which is also why {@link #resolve}
 * calls the Interaction <b>exactly once</b>, never twice for one attack. {@link #resolve} bundles
 * the defence roll as a ready {@code CombatantAction} on {@link
 * IncomingAttackResult#getRecordedAction()} without recording it, so the caller files the exchange
 * with one {@code Scene#recordAction(defender, result.getRecordedAction())} — or {@code
 * defender.recordAction(...)} with no live Scene.
 */
public class AttackReceiver {

    private final EsquivaEApararInteraction esquivaEApararInteraction;
    private final EffectChainService effectChainService;
    private final DamageService damageService;

    public AttackReceiver() {
        this(new EsquivaEApararInteraction(), new EffectChainServiceImpl(), new DamageServiceImpl());
    }

    public AttackReceiver(final EsquivaEApararInteraction esquivaEApararInteraction,
                          final EffectChainService effectChainService,
                          final DamageService damageService) {
        this.esquivaEApararInteraction = esquivaEApararInteraction;
        this.effectChainService = effectChainService;
        this.damageService = damageService;
    }

    /**
     * Resolves attack against its defender, reporting the whole exchange without applying any of
     * it. In order:
     *
     * <ol>
     *   <li>Rolls the defender's Esquiva e Aparar (once), typed by {@code defenseType} so the
     *   right Defesa feeds it.</li>
     *   <li>Makes the attack's Grau de Dificuldade easier by the defender's own {@code
     *   difficultyReduction} — that value is denominated in <i>níveis</i>, which is exactly what
     *   {@link DifficultyLevel#easier} takes.</li>
     *   <li>Derives the threshold from that tier's {@code baseValue} plus the attack's flat
     *   bonus, and compares — a tie is a successful defense. A provoking Aura's malus lowers
     *   that threshold when a bound attacker turns on someone other than the Aura's holder.</li>
     *   <li>On a hit, works out which Efeitos the margin and the critical result each trigger,
     *   and assembles them into one chain behind a {@link DamageInteraction}.</li>
     * </ol>
     *
     * <p>With no {@code defenseRoll} supplied, steps 3–4 are skipped and every outcome field
     * stays {@code null} — but {@code defenseTotal} (the bonuses alone) and {@code requiredTotal}
     * are still reported, so a caller can show a player what they need to roll without
     * committing to an outcome.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException ({@code
     *         FORCED_ATTACK_TARGET_REQUIRED}) if a provoking Aura in {@code scene} requires {@code
     *         attacker} to attack its holder first, and this attack targets someone else
     */
    public IncomingAttackResult resolve(@NonNull final IncomingAttack attack) {
        CombatantSheet defender = attack.getDefender();
        boolean auraHalvesDamage = AuraTargeting.resolveHalvesDamage(attack.getScene(), attack.getAttacker(),
                defender, attack.isForcedTargetUnavailable());
        SkillRoll defenseRoll = attack.getDefenseRoll();

        InteractionResult defenseResult = esquivaEApararInteraction.applyTo(
                defender, attack.getSceneContext(), defenseRoll, attack.getDefenseType(),
                attack.getDamageDescriptor());

        DifficultyLevel effectiveDifficultyLevel =
                attack.getDifficultyLevel().easier(defenseResult.getDifficultyReduction());
        int requiredTotal = effectiveDifficultyLevel.getBaseValue() + attack.getAttackBonus();
        int defenseTotal = defenseResult.getSkillRollBonus()
                + (defenseRoll == null ? 0 : defenseRoll.getTotal());

        IncomingAttackResult.IncomingAttackResultBuilder result = IncomingAttackResult.builder()
                .defenseTotal(defenseTotal)
                .requiredTotal(requiredTotal)
                .auraHalvesDamage(auraHalvesDamage)
                .retaliation(RetaliationResolver.resolve(defender, SkillType.ATAQUE_CORPO_A_CORPO))
                .effectiveDifficultyLevel(effectiveDifficultyLevel);

        if (defenseRoll == null) {
            return result.defenseResult(defenseResult).build();
        }

        int margin = requiredTotal - defenseTotal;
        // Ímpeto Defensivo Maior: "se torna imune aos ataques dele por 1 Rodada".
        boolean immune = defender.getGuardsAgainst(attack.getAttacker()).stream().anyMatch(AttackerGuard::isImmune);
        boolean defended = margin <= 0 || immune;
        CriticalResult criticalResult = defenseResult.getCriticalResult();
        boolean criticalEffectTriggered = !defended && criticalResult != null && criticalResult.isCriticalFailure();
        boolean effectChainTriggered = !defended
                && margin >= effectChainService.getRequiredMargin(defender.getCharacter());

        if (!defended) {
            defenseResult = defenseResult.toBuilder()
                    .nextInteraction(buildChain(attack, criticalEffectTriggered, effectChainTriggered,
                            criticalResult, auraHalvesDamage))
                    .build();
            // An unstated Perícia is read as melee here, the same default the thorns above take.
            result.onHitRetaliations(RetaliationResolver.resolveOnHit(defender, attack.getAttacker(),
                    attack.getAttackSkill() == null ? SkillType.ATAQUE_CORPO_A_CORPO : attack.getAttackSkill(),
                    attack.getAttackSource(), criticalResult != null && criticalResult.isCriticalFailure()));
            result.unappliedCriticalEffects(CriticalEffectResolver.resolve(attack.getAttacker(),
                    attack.getAttackSource(), attack.getAttackSkill(), criticalEffectTriggered ? criticalResult : null,
                    true, attack.getAdditionalCriticalEffectTypes(), attack.getDiceRoller(), false).unapplied());
        } else if (criticalResult != null && criticalResult.isCriticalSuccess() && !immune) {
            // "Efeitos Críticos Defensivos substituem as falhas críticas inimigas em caso de Sucesso
            // Crítico nas rolagens de Defesas" — built for the caller to apply.
            for (DefensiveCriticalEffectType type : DefensiveCriticalEffects.grantedTo(defender)) {
                DefensiveCriticalEffect.of(type, defender, attack.getAttacker(), criticalResult,
                                attack.getAttackSkill(), attack.getAttackSource(), attack.getDiceRoller())
                        .ifPresentOrElse(result::defensiveCriticalEffect,
                                () -> result.unappliedCriticalEffect(type));
            }
        }

        return result.defenseResult(defenseResult)
                .defended(defended)
                .margin(margin)
                .criticalResult(criticalResult)
                .criticalEffectTriggered(criticalEffectTriggered)
                .effectChainTriggered(effectChainTriggered)
                .recordedAction(recordedAction(attack, defenseResult, defended, defenseTotal - requiredTotal,
                        criticalResult, effectiveDifficultyLevel))
                .build();
    }

    /**
     * Bundles the defender's Esquiva e Aparar roll as a ready-to-file {@link CombatantAction} —
     * the caller records it via {@code scene.recordAction(defender, action)}. The verdict is from
     * the <em>defender's</em> side: {@code succeeded} is whether the defence held, {@code margin}
     * is {@code defenseTotal - requiredTotal} (positive when it held), and {@code turnNumber}
     * comes from {@link IncomingAttack#getScene()} when one is present. Never recorded here —
     * {@link #resolve} stays report-only.
     */
    private CombatantAction recordedAction(final IncomingAttack attack, final InteractionResult defenseResult,
                                            final boolean defended, final int defenderMargin,
                                            final CriticalResult criticalResult,
                                            final DifficultyLevel effectiveDifficultyLevel) {
        return new CombatantAction(
                SkillType.ESQUIVA_E_APARAR,
                defenseResult.getGoverningAttributeDomain(),
                null,
                attack.getDefenseRoll().getActionCost(),
                attack.getScene() == null ? 0 : attack.getScene().getCurrentRound(),
                new ActionOutcome(defended, defenderMargin, criticalResult, effectiveDifficultyLevel));
    }

    /**
     * Assembles the stages a landed attack triggers into one chain, back to front, and returns
     * its head — always a {@link DamageInteraction}, since a hit always deals damage even when it
     * triggers no Efeito at all.
     *
     * <p>Order matches the pipeline {@code org.aventyrs.core.effect}'s package-info documents:
     * Damage, then every triggered {@link EffectChain}, then every triggered {@link
     * CriticalEffect}. The two groups are gated independently, so an attack may carry both, one,
     * or neither. Note that {@code DamageInteraction} only forwards to its successor when the hit
     * actually dealt damage — a hit fully absorbed by RD/RA ends the chain there, which is that
     * class's own long-standing rule, not something added here.
     *
     * <p>The Efeitos Críticos are filtered through {@link CriticalEffect#applicableTo} first, so
     * one the defender's anatomy is immune to never reaches the chain — see that method for why
     * the filter is shared between both directions rather than written here twice. criticalResult
     * goes with them: on this path an Efeito Crítico fires off the defender's own Falha Crítica,
     * so a {@code FALHA_CRITICA_MENOR} is what "Efeito Crítico Menor" means here, and a severity-
     * keyed immunity needs to see it.
     */
    private Interaction<CombatantSheet> buildChain(final IncomingAttack attack,
                                                    final boolean criticalEffectTriggered,
                                                    final boolean effectChainTriggered,
                                                    final CriticalResult criticalResult,
                                                    final boolean halfDamage) {
        List<Effect> stages = new ArrayList<>();
        if (effectChainTriggered) {
            stages.addAll(attack.getEffectChains());
        }
        if (criticalEffectTriggered) {
            List<CriticalEffect> effects = new ArrayList<>(attack.getCriticalEffects());
            effects.addAll(CriticalEffectResolver.resolve(attack.getAttacker(), attack.getAttackSource(),
                    attack.getAttackSkill(), criticalResult, true, attack.getAdditionalCriticalEffectTypes(),
                    attack.getDiceRoller()).effects());
            stages.addAll(CriticalEffect.applicableTo(attack.getDefender(), effects,
                    criticalResult, attack.getSceneContext()));
        } else {
            // Finalização on this side too: a non-critical hit applies the natural weapon's Menor.
            stages.addAll(CriticalEffect.applicableTo(attack.getDefender(),
                    CriticalEffectResolver.resolve(attack.getAttacker(), attack.getAttackSource(),
                            attack.getAttackSkill(), null, true, List.of(), attack.getDiceRoller()).effects(),
                    CriticalResult.FALHA_CRITICA_MENOR, attack.getSceneContext()));
        }

        Interaction<CombatantSheet> next = null;
        for (int i = stages.size() - 1; i >= 0; i--) {
            next = stages.get(i).chainInto(next);
        }
        DamageInteraction head = new DamageInteraction(damageService);
        return (halfDamage ? head.halvingDamage() : head).chainInto(next);
    }
}
