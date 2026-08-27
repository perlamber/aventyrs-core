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
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillInteractionFactory;
import org.aventyrs.core.skill.SkillRoll;

import java.util.ArrayList;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.NOT_AN_ATTACK_SKILL;

/**
 * The entry point for "this character is attacking someone" — the mirror of {@link
 * AttackReceiver}, and the half of a combat exchange this core was missing.
 *
 * <p>Both halves exist because <b>the player always rolls</b>. A foe never touches dice, so it
 * contributes a fixed number in either direction: a Grau de Dificuldade when it attacks (which
 * {@link AttackReceiver} compares the player's Esquiva e Aparar roll against), and a Defesa when
 * it is attacked (which this class compares the player's Ataque roll against). The two are
 * separate entry points, not two halves of one call — neither ever invokes the other.
 *
 * <p>Like its mirror, it orchestrates rather than computes: it rolls the attacker's Perícia
 * through whichever {@code <Skill>Interaction} {@code attackSkill} names, compares the total
 * against the defender's Defesa, and answers three independent questions:
 *
 * <ul>
 *   <li><b>Did it land?</b> The attack total reached the Defesa.</li>
 *   <li><b>Was it a critical?</b> The attack roll was an Acerto Crítico — the ordinary
 *   direction, unlike {@link AttackReceiver}, where the trigger is the defender's Falha Crítica.
 *   This is the direction {@code CriticalEffect#validateCriticalHit} was written for, so no
 *   translation is needed here.</li>
 *   <li><b>Did it clear the Corrente de Efeitos threshold?</b> It beat the Defesa by at least
 *   {@link EffectChainService#getRequiredMargin} — 5 normally, 7 against a defender holding
 *   {@code AutocontroleAdvantage#RESOLUTO}.</li>
 * </ul>
 *
 * <p>The last two are independent: an attack can clear the Corrente threshold without critting,
 * and crit without clearing it.
 *
 * <h2>Open question: the attacker's own {@code difficultyReduction}</h2>
 *
 * A Perícia's GD reduction is denominated in <i>níveis</i>, which is exactly what {@code
 * DifficultyLevel#easier} takes — and that's how {@link AttackReceiver} applies it. Here the
 * target number is a flat integer authored on the foe's stat block, and <b>there is no defined
 * conversion from níveis to points</b>. Rather than invent a rate, {@link #resolve} computes the
 * reduction and reports it on {@link DeliveredAttackResult#getUnappliedDifficultyReduction()}
 * without applying it — real, exact data whose application is blocked, the same discipline this
 * codebase applies elsewhere.
 *
 * <p>TODO: apply the attacker's difficultyReduction once the rules define what one nível is
 * worth against a flat Defesa (or once a foe's Defesa is authored as a tier rather than a number).
 *
 * <h2>What it produces</h2>
 *
 * On a hit, {@link DeliveredAttackResult#getAttackResult()}'s {@code nextInteraction} is the head
 * of a chain running Damage → every triggered Corrente → every triggered Efeito Crítico, all
 * aimed at the <b>defender</b>. Report-only otherwise: nothing is applied, and the caller drains
 * the chain with the loop {@code org.aventyrs.core.effect}'s package-info documents, supplying
 * the damage figure at the head.
 */
public class AttackDelivery {

    private final EffectChainService effectChainService;
    private final DamageService damageService;

    public AttackDelivery() {
        this(new EffectChainServiceImpl(), new DamageServiceImpl());
    }

    public AttackDelivery(final EffectChainService effectChainService, final DamageService damageService) {
        this.effectChainService = effectChainService;
        this.damageService = damageService;
    }

    /**
     * Resolves attack, reporting the whole exchange without applying any of it.
     *
     * <p>Rolls the attacker's Perícia <b>exactly once</b> — that call consumes {@code
     * CombatantSheet#consumeFirstRollThisTurn} and can grant a temporary Ego point on a critical
     * success, so rolling twice for one attack would double-consume the Turn's state. It goes
     * through the longest {@code applyTo}, so both a target-conditioned ability ({@code FRIEZA}'s
     * proximity damage bonus, {@code ABATEDORES_DE_GIGANTES}' bonus against a larger foe) and a
     * delivery-conditioned one ({@code ARREMESSO_PODEROSO}'s substituted Attribute, from {@link
     * DeliveredAttack#getAttackSource()}) resolve against the real attack rather than a generic
     * fact about the encounter.
     *
     * <p>With no {@code attackRoll} supplied, the comparison and the chain are skipped: every
     * outcome stays {@code null}, while {@code attackTotal} (the bonuses alone) and {@code
     * requiredTotal} are still reported, so a caller can show a player what they need to roll.
     *
     * @throws IllegalOperationException if {@code attackSkill} isn't a Perícia de Ataque
     */
    public DeliveredAttackResult resolve(@NonNull final DeliveredAttack attack) {
        if (!attack.getAttackSkill().isAttackSkill()) {
            throw new IllegalOperationException(NOT_AN_ATTACK_SKILL);
        }
        CombatantSheet defender = attack.getDefender();
        SkillRoll attackRoll = attack.getAttackRoll();

        InteractionResult attackResult = SkillInteractionFactory.create(attack.getAttackSkill())
                .applyTo(attack.getAttacker(), attack.getSceneContext(), attackRoll, defender, attack.getAttackSource());

        int requiredTotal = attack.getDefenseValue();
        int attackTotal = attackResult.getSkillRollBonus()
                + (attackRoll == null ? 0 : attackRoll.getTotal());

        DeliveredAttackResult.DeliveredAttackResultBuilder result = DeliveredAttackResult.builder()
                .attackTotal(attackTotal)
                .requiredTotal(requiredTotal)
                .unappliedDifficultyReduction(attackResult.getDifficultyReduction());

        if (attackRoll == null) {
            return result.attackResult(attackResult).build();
        }

        int margin = attackTotal - requiredTotal;
        boolean hit = margin >= 0;
        CriticalResult criticalResult = attackResult.getCriticalResult();
        boolean criticalEffectTriggered = hit && criticalResult != null && criticalResult.isCriticalSuccess();
        boolean effectChainTriggered = hit
                && margin >= effectChainService.getRequiredMargin(defender.getCharacter());

        if (hit) {
            attackResult = attackResult.toBuilder()
                    .nextInteraction(buildChain(attack, criticalEffectTriggered, effectChainTriggered))
                    .build();
        }

        return result.attackResult(attackResult)
                .margin(margin)
                .hit(hit)
                .criticalResult(criticalResult)
                .criticalEffectTriggered(criticalEffectTriggered)
                .effectChainTriggered(effectChainTriggered)
                .build();
    }

    /**
     * Assembles the stages a landed attack triggers into one chain, back to front, returning its
     * head — always a {@link DamageInteraction}, since a hit deals damage even when it triggers
     * no Efeito. Byte-for-byte the same order {@link AttackReceiver} uses, because it's the same
     * pipeline: Damage, then every triggered {@link EffectChain}, then every triggered {@link
     * CriticalEffect}. {@code DamageInteraction} still only forwards to its successor when the
     * hit actually dealt damage, so a blow fully absorbed by RD/RA ends the chain there.
     *
     * <p>The Efeitos Críticos are filtered through {@link CriticalEffect#applicableTo} first, so
     * one the defender's anatomy is immune to never reaches the chain — see that method for why
     * the filter is shared between both directions rather than written here twice.
     */
    private Interaction<CombatantSheet> buildChain(final DeliveredAttack attack,
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
