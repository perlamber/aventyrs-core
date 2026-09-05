package org.aventyrs.core.combat;

import lombok.NonNull;
import org.aventyrs.core.character.services.AttackTargetingService;
import org.aventyrs.core.character.services.AttackTargetingServiceImpl;
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
import static org.aventyrs.core.util.TranslatableMessages.TOO_MANY_ATTACK_TARGETS;

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
 * <h2>More than one target</h2>
 *
 * A Talento can widen an attack past its one target ({@code
 * ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA}). {@link
 * DeliveredAttack#getAdditionalTargets()} carries them, and {@link #resolve} refuses more than
 * {@code AttackTargetingService#getMaximumTargets} allows — it enforces <b>how many</b>, never
 * <b>which</b>: the adjacency those clauses require is geometry between two combatants who are
 * both not the roller, so picking the targets is the caller's step.
 *
 * <p>The roll still happens once. The three questions above are then asked again per additional
 * target against <em>that</em> defender's own Defesa, and answered on a {@link
 * DeliveredAttackTargetResult} each; the primary target keeps the flat fields on {@link
 * DeliveredAttackResult}, so a single-target caller sees no change at all. Every additional
 * target's chain head is marked {@code DamageInteraction#halvingDamage()} — one attack makes one
 * dano roll, and the Meio-Dano is applied inside it.
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
 * {@code AssassinoFeat#SAQUE_RELAMPAGO}'s "-1 nível" is the first authored clause blocked here —
 * it joins {@code unappliedDifficultyReduction} on this path and applies for real on the direct
 * skill-roll path and via {@link AttackReceiver}.
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
    private final AttackTargetingService attackTargetingService;

    public AttackDelivery() {
        this(new EffectChainServiceImpl(), new DamageServiceImpl());
    }

    public AttackDelivery(final EffectChainService effectChainService, final DamageService damageService) {
        this(effectChainService, damageService, new AttackTargetingServiceImpl());
    }

    public AttackDelivery(final EffectChainService effectChainService, final DamageService damageService,
                          final AttackTargetingService attackTargetingService) {
        this.effectChainService = effectChainService;
        this.damageService = damageService;
        this.attackTargetingService = attackTargetingService;
    }

    /**
     * Resolves attack, reporting the whole exchange without applying any of it.
     *
     * <p>Rolls the attacker's Perícia <b>exactly once</b> — that call can grant a temporary Ego
     * point on a critical success (the only state it changes; the first-roll-of-Turn check it
     * also runs is non-mutating now), so rolling twice for one attack would double-grant. It goes
     * through the longest {@code applyTo}, so both a target-conditioned ability ({@code FRIEZA}'s
     * proximity damage bonus, {@code ABATEDORES_DE_GIGANTES}' bonus against a larger foe) and a
     * delivery-conditioned one ({@code ARREMESSO_PODEROSO}'s substituted Attribute, from {@link
     * DeliveredAttack#getAttackSource()}) resolve against the real attack rather than a generic
     * fact about the encounter. The API is expected to call {@code
     * attacker.recordAction(...)} after {@code resolve} returns, building the {@code
     * CombatantAction} from {@code getAttackResult().getGoverningAttributeDomain()} and the
     * {@code AttackSource}/{@code ActionCost} it supplied.
     *
     * <p>With no {@code attackRoll} supplied, the comparison and the chain are skipped: every
     * outcome stays {@code null}, while {@code attackTotal} (the bonuses alone) and {@code
     * requiredTotal} are still reported — for every target, so a caller can show a player what
     * they need to roll against each of them.
     *
     * @throws IllegalOperationException if {@code attackSkill} isn't a Perícia de Ataque, or if
     *         the attack names more targets than the attacker's Talentos entitle them to
     */
    public DeliveredAttackResult resolve(@NonNull final DeliveredAttack attack) {
        if (!attack.getAttackSkill().isAttackSkill()) {
            throw new IllegalOperationException(NOT_AN_ATTACK_SKILL);
        }
        List<AttackTarget> additionalTargets = attack.getAdditionalTargets();
        int declaredTargets = 1 + additionalTargets.size();
        if (declaredTargets > attackTargetingService.getMaximumTargets(
                attack.getAttacker().getCharacter(), attack.getAttackSkill())) {
            throw new IllegalOperationException(TOO_MANY_ATTACK_TARGETS);
        }
        CombatantSheet defender = attack.getDefender();
        SkillRoll attackRoll = attack.getAttackRoll();

        List<CombatantSheet> extraTargets = additionalTargets.stream().map(AttackTarget::defender).toList();
        InteractionResult attackResult = SkillInteractionFactory.create(attack.getAttackSkill())
                .applyTo(attack.getAttacker(), attack.getSceneContext(), attackRoll, defender,
                        attack.getAttackSource(), extraTargets);

        int requiredTotal = attack.getDefenseValue();
        int attackTotal = attackResult.getSkillRollBonus()
                + (attackRoll == null ? 0 : attackRoll.getTotal());

        DeliveredAttackResult.DeliveredAttackResultBuilder result = DeliveredAttackResult.builder()
                .attackTotal(attackTotal)
                .requiredTotal(requiredTotal)
                .unappliedDifficultyReduction(attackResult.getDifficultyReduction());

        if (attackRoll == null) {
            additionalTargets.forEach(target -> result.additionalTargetResult(DeliveredAttackTargetResult.builder()
                    .defender(target.defender())
                    .requiredTotal(target.defenseValue())
                    .build()));
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
                    .nextInteraction(buildChain(attack, defender, criticalResult, criticalEffectTriggered,
                            effectChainTriggered, false))
                    .build();
        }

        for (AttackTarget target : additionalTargets) {
            result.additionalTargetResult(resolveAdditionalTarget(attack, target, attackTotal, criticalResult));
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
     * The same comparison the primary target got, against one additional target's own Defesa, with
     * the <b>one already-rolled</b> {@code attackTotal} and the one {@code criticalResult} —
     * neither is re-derived, because the attack is one roll. What genuinely differs per target is
     * the margin, whether it landed, whether it cleared <em>that</em> defender's Corrente
     * threshold, and the chain built for them.
     *
     * <p>Its chain head is marked {@code halvingDamage()} — "os danos no alvo adicional são
     * reduzidos à metade". The Efeitos Críticos are filtered against this defender's own anatomy,
     * so an immunity of theirs applies to them alone.
     */
    private DeliveredAttackTargetResult resolveAdditionalTarget(final DeliveredAttack attack, final AttackTarget target,
                                                                 final int attackTotal, final CriticalResult criticalResult) {
        CombatantSheet defender = target.defender();
        int margin = attackTotal - target.defenseValue();
        boolean hit = margin >= 0;
        boolean criticalEffectTriggered = hit && criticalResult != null && criticalResult.isCriticalSuccess();
        boolean effectChainTriggered = hit
                && margin >= effectChainService.getRequiredMargin(defender.getCharacter());

        return DeliveredAttackTargetResult.builder()
                .defender(defender)
                .requiredTotal(target.defenseValue())
                .margin(margin)
                .hit(hit)
                .criticalEffectTriggered(criticalEffectTriggered)
                .effectChainTriggered(effectChainTriggered)
                .nextInteraction(hit
                        ? buildChain(attack, defender, criticalResult, criticalEffectTriggered, effectChainTriggered, true)
                        : null)
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
     * the filter is shared between both directions rather than written here twice. defender is a
     * parameter rather than read off attack, because a multi-target attack builds one chain per
     * target and each is filtered against its <em>own</em> anatomy.
     *
     * <p>halfDamage marks the head {@code DamageInteraction} as dealing Meio-Dano — set for an
     * additional target and never for the primary one. The stages behind it are unaffected: the
     * halving belongs to the damage, not to the Efeitos it triggers.
     */
    private Interaction<CombatantSheet> buildChain(final DeliveredAttack attack,
                                                    final CombatantSheet defender,
                                                    final CriticalResult criticalResult,
                                                    final boolean criticalEffectTriggered,
                                                    final boolean effectChainTriggered,
                                                    final boolean halfDamage) {
        List<Effect> stages = new ArrayList<>();
        if (effectChainTriggered) {
            stages.addAll(attack.getEffectChains());
        }
        if (criticalEffectTriggered) {
            stages.addAll(CriticalEffect.applicableTo(defender, allCriticalEffects(attack, criticalResult)));
        }

        Interaction<CombatantSheet> next = null;
        for (int i = stages.size() - 1; i >= 0; i--) {
            next = stages.get(i).chainInto(next);
        }
        DamageInteraction head = new DamageInteraction(damageService);
        return (halfDamage ? head.halvingDamage() : head).chainInto(next);
    }

    /**
     * The caller-supplied Efeitos Críticos plus every one the attacker's Talentos add for this
     * kind of hit — {@code AssassinoFeat#ABRIR_FERIDAS}'s "'Sangramento' como Efeito Crítico
     * adicional". Talentos are outside every {@code ModifierResolver} scan, so they get an
     * explicit pass, the same shape {@code AbstractSkillInteraction} uses for its own {@code
     * Feat} hooks.
     */
    private List<CriticalEffect> allCriticalEffects(final DeliveredAttack attack, final CriticalResult criticalResult) {
        List<CriticalEffect> effects = new ArrayList<>(attack.getCriticalEffects());
        attack.getAttacker().getCharacter().getFeats().forEach(feat ->
                effects.addAll(feat.resolveExtraCriticalEffects(attack.getAttacker().getCharacter(),
                        attack.getAttackSkill(), attack.getAttackSource(), criticalResult)));
        return effects;
    }
}
