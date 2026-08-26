package org.aventyrs.core.combat;

import lombok.NonNull;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.effect.CriticalEffect;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.effect.Effect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.EffectChainService;
import org.aventyrs.core.effect.EffectChainServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
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
 * exception is the defense roll itself: {@code applyTo} consumes {@code
 * CombatantSheet#consumeFirstRollThisTurn} and may grant a temporary Ego point on a critical
 * success. That's the roll genuinely happening, not an outcome being applied — which is also why
 * {@link #resolve} calls the Interaction <b>exactly once</b>, never twice for one attack.
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
     *   bonus, and compares — a tie is a successful defense.</li>
     *   <li>On a hit, works out which Efeitos the margin and the critical result each trigger,
     *   and assembles them into one chain behind a {@link DamageInteraction}.</li>
     * </ol>
     *
     * <p>With no {@code defenseRoll} supplied, steps 3–4 are skipped and every outcome field
     * stays {@code null} — but {@code defenseTotal} (the bonuses alone) and {@code requiredTotal}
     * are still reported, so a caller can show a player what they need to roll without
     * committing to an outcome.
     */
    public IncomingAttackResult resolve(@NonNull final IncomingAttack attack) {
        CombatantSheet defender = attack.getDefender();
        SkillRoll defenseRoll = attack.getDefenseRoll();

        InteractionResult defenseResult = esquivaEApararInteraction.applyTo(
                defender, attack.getSceneContext(), defenseRoll, attack.getDefenseType());

        DifficultyLevel effectiveDifficultyLevel =
                attack.getDifficultyLevel().easier(defenseResult.getDifficultyReduction());
        int requiredTotal = effectiveDifficultyLevel.getBaseValue() + attack.getAttackBonus();
        int defenseTotal = defenseResult.getSkillRollBonus()
                + (defenseRoll == null ? 0 : defenseRoll.getTotal());

        IncomingAttackResult.IncomingAttackResultBuilder result = IncomingAttackResult.builder()
                .defenseTotal(defenseTotal)
                .requiredTotal(requiredTotal)
                .effectiveDifficultyLevel(effectiveDifficultyLevel);

        if (defenseRoll == null) {
            return result.defenseResult(defenseResult).build();
        }

        int margin = requiredTotal - defenseTotal;
        boolean defended = margin <= 0;
        CriticalResult criticalResult = defenseResult.getCriticalResult();
        boolean criticalEffectTriggered = !defended && criticalResult != null && criticalResult.isCriticalFailure();
        boolean effectChainTriggered = !defended
                && margin >= effectChainService.getRequiredMargin(defender.getCharacter());

        if (!defended) {
            defenseResult = defenseResult.toBuilder()
                    .nextInteraction(buildChain(attack, criticalEffectTriggered, effectChainTriggered))
                    .build();
        }

        return result.defenseResult(defenseResult)
                .defended(defended)
                .margin(margin)
                .criticalResult(criticalResult)
                .criticalEffectTriggered(criticalEffectTriggered)
                .effectChainTriggered(effectChainTriggered)
                .build();
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
     * the filter is shared between both directions rather than written here twice.
     */
    private Interaction<CombatantSheet> buildChain(final IncomingAttack attack,
                                                    final boolean criticalEffectTriggered,
                                                    final boolean effectChainTriggered) {
        List<Effect> stages = new ArrayList<>();
        if (effectChainTriggered) {
            stages.addAll(attack.getEffectChains());
        }
        if (criticalEffectTriggered) {
            stages.addAll(CriticalEffect.applicableTo(attack.getDefender(), attack.getCriticalEffects()));
        }

        Interaction<CombatantSheet> next = null;
        for (int i = stages.size() - 1; i >= 0; i--) {
            next = stages.get(i).chainInto(next);
        }
        return new DamageInteraction(damageService).chainInto(next);
    }
}
