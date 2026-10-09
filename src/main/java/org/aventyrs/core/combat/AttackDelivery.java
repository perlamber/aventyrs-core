package org.aventyrs.core.combat;

import org.aventyrs.core.ego.SorteEffect;
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
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.ExplosaoCataclismica;
import org.aventyrs.core.effect.GolpeTrovejante;
import org.aventyrs.core.effect.RepeatedEffect;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.ShieldAttack;
import org.aventyrs.core.sheet.ActionOutcome;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillInteractionFactory;
import org.aventyrs.core.skill.SkillRoll;

import java.util.ArrayList;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.NOT_AN_ATTACK_SKILL;
import static org.aventyrs.core.util.TranslatableMessages.SUMMON_CANNOT_FIGHT;
import static org.aventyrs.core.util.TranslatableMessages.AREA_OF_EFFECT_NOT_GRANTED;
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
     * fact about the encounter. {@code resolve} bundles the roll as a ready {@code CombatantAction}
     * on {@link DeliveredAttackResult#getRecordedAction()} — it does <b>not</b> record it (still
     * report-only) — so the caller files the exchange with one {@code
     * Scene#recordAction(attacker, result.getRecordedAction())} (or {@code
     * attacker.recordAction(...)} with no live Scene), instead of re-assembling the action from
     * {@code getGoverningAttributeDomain()} and the source/cost it supplied.
     *
     * <p>With no {@code attackRoll} supplied, the comparison and the chain are skipped: every
     * outcome stays {@code null}, while {@code attackTotal} (the bonuses alone) and {@code
     * requiredTotal} are still reported — for every target, so a caller can show a player what
     * they need to roll against each of them.
     *
     * <p>A provoking Aura in {@code scene} is judged against the primary {@code defender}: a bound
     * attacker owing its first attack to the Aura's holder is refused, and one that already paid
     * it takes {@code Skill#DISADVANTAGE_MALUS} on {@code attackTotal} against anyone else.
     *
     * @throws IllegalOperationException if {@code attackSkill} isn't a Perícia de Ataque, if
     *         the attack names more targets than the attacker's Talentos entitle them to, or
     *         ({@code FORCED_ATTACK_TARGET_REQUIRED}) if an Aura requires it to target the
     *         holder first
     */
    public DeliveredAttackResult resolve(@NonNull final DeliveredAttack given) {
        if (!given.getAttackSkill().isAttackSkill()) {
            throw new IllegalOperationException(NOT_AN_ATTACK_SKILL);
        }
        // A Familiar Maior "não é capaz de lutar".
        if (given.getAttacker() instanceof org.aventyrs.core.monster.MonsterSheet monster && monster.isNonCombatant()) {
            throw new IllegalOperationException(SUMMON_CANNOT_FIGHT);
        }
        // Devorado: "Não pode ser afetado por efeitos externos" — only its devourer reaches it.
        if (given.getDefender().isShieldedFrom(given.getAttacker())
                || given.getAdditionalTargets().stream().anyMatch(extra -> extra.defender().isShieldedFrom(given.getAttacker()))) {
            throw new IllegalOperationException(org.aventyrs.core.util.TranslatableMessages.TARGET_INSIDE_DEVOURER);
        }
        // An Ataque com Escudo adds the shield's bonus for the Defesa it is rolled against — so it is
        // aimed at this attack's own DefenseType, whatever the caller built it with.
        DeliveredAttack redirected = againstOverriddenDefense(given);
        DeliveredAttack attack = redirected.getAttackSource() instanceof ShieldAttack shield
                && shield.getTargetDefense() != redirected.getDefenseType()
                ? redirected.toBuilder().attackSource(shield.against(redirected.getDefenseType())).build()
                : redirected;
        List<AttackTarget> additionalTargets = attack.getAdditionalTargets();
        if (attack.getAreaOfEffect() != null) {
            // An area attack names whoever stands in its footprint — no count to enforce — but the
            // area itself must be one the attacker's Talentos grant for this attack.
            boolean granted = attackTargetingService.resolveAttackArea(attack.getAttacker().getCharacter(),
                            attack.getAttackSkill(), attack.getAttackSource(), attack.getAttackRoll())
                    .filter(attack.getAreaOfEffect()::equals)
                    .isPresent();
            if (!granted) {
                throw new IllegalOperationException(AREA_OF_EFFECT_NOT_GRANTED);
            }
        } else {
            int declaredTargets = 1 + additionalTargets.size();
            if (declaredTargets > attackTargetingService.getMaximumTargets(
                    attack.getAttacker(), attack.getAttackSkill(), attack.getAttackSource())) {
                throw new IllegalOperationException(TOO_MANY_ATTACK_TARGETS);
            }
        }
        CombatantSheet defender = attack.getDefender();
        boolean auraHalvesDamage = AuraTargeting.resolveHalvesDamage(attack.getScene(), attack.getAttacker(),
                defender, attack.isForcedTargetUnavailable());
        SkillRoll attackRoll = attack.getAttackRoll();
        // Ataque em Arco: "o valor dos danos causados em cada alvo é igual a metade" — the primary too.
        // Ataque Repentino: "Os danos causados por este ataque são reduzidos à metade", area or not.
        boolean everyTargetHalved = (attack.getAreaOfEffect() == null && attackTargetingService.halvesEveryTarget(
                attack.getAttacker(), attack.getAttackSkill(), attack.getAttackSource(), additionalTargets.size()))
                || halvesAttackDamage(attack, attackRoll);
        Retaliation retaliation = RetaliationResolver.resolve(defender, attack.getAttackSkill());

        List<CombatantSheet> extraTargets = additionalTargets.stream().map(AttackTarget::defender).toList();
        InteractionResult attackResult = SkillInteractionFactory.create(attack.getAttackSkill())
                .applyTo(attack.getAttacker(), attack.getSceneContext(), attackRoll, defender,
                        attack.getAttackSource(), extraTargets);

        // The Aptidões Mágicas' DM against a Magia the defender can cast rides on the flat Defesa the
        // caller supplied, which cannot see what is being resisted.
        int requiredTotal = attack.getDefenseValue()
                + SpellResistance.defenseBonus(defender, attack.getDefenseType(), attack.getAttackSource())
                + defenceFavour(attack, defender);
        int attackTotal = attackResult.getSkillRollBonus()
                + (attackRoll == null ? 0 : attackRoll.getTotal());

        DeliveredAttackResult.DeliveredAttackResultBuilder result = DeliveredAttackResult.builder()
                .attackTotal(attackTotal)
                .requiredTotal(requiredTotal)
                .auraHalvesDamage(auraHalvesDamage)
                .everyTargetHalved(everyTargetHalved)
                .retaliation(retaliation)
                .unappliedDifficultyReduction(attackResult.getDifficultyReduction())
                .unappliedSpellResistanceReduction(SpellResistance.difficultyReduction(defender, attack.getAttackSource()));

        if (attackRoll == null) {
            additionalTargets.forEach(target -> result.additionalTargetResult(DeliveredAttackTargetResult.builder()
                    .defender(target.defender())
                    .requiredTotal(target.defenseValue()
                            + SpellResistance.defenseBonus(target.defender(), attack.getDefenseType(),
                                    attack.getAttackSource())
                            + defenceFavour(attack, target.defender()))
                    .build()));
            return result.attackResult(attackResult).build();
        }

        int margin = attackTotal - requiredTotal;
        // A Cego attacker's failed 1d6 misses whatever the total.
        boolean blindMiss = Boolean.TRUE.equals(attackResult.getBlindCheckFailed());
        // Aptidão Mágica Dracônica: a Magia the defender can cast never lands on them.
        // Sorte's chosen success lands "independente do resultado dos dados" — never through an immunity.
        boolean hit = (margin >= 0 || forcedSuccess(attackRoll)) && !blindMiss
                && !SpellResistance.immune(defender, attack.getAttackSource());
        CriticalResult rolledCritical = attackResult.getCriticalResult();
        // Sorte's permanent point: on a hit, the Efeitos Críticos apply as Maior — see SorteEffect.
        CriticalResult criticalResult = unleashed(attackRoll) && hit ? CriticalResult.ACERTO_CRITICO_MAIOR : rolledCritical;
        // Frenesi Assustador: a fear-struck attacker "se tornam incapazes de desferir Efeitos Críticos
        // Menores e não podem desencadear Correntes de Efeitos" while the Gigante who cast it is down.
        boolean suppressed = attack.getAttacker().isMinorCriticalAndChainSuppressed();
        boolean criticalEffectTriggered = hit && criticalResult != null && criticalResult.isCriticalSuccess()
                && !(suppressed && criticalResult.isMinor());
        boolean effectChainTriggered = hit && !suppressed && !superficial(attack)
                && (unleashed(attackRoll) || margin >= effectChainService.getRequiredMargin(attack.getAttacker(),
                        AttackReceiver.positionOf(attack.getScene(), attack.getAttacker(), attack.getSceneContext()),
                        defender, AttackReceiver.positionOf(attack.getScene(), defender, null)));

        if (hit) {
            attackResult = attackResult.toBuilder()
                    .nextInteraction(buildChain(attack, defender, criticalResult, criticalEffectTriggered,
                            effectChainTriggered, auraHalvesDamage || everyTargetHalved))
                    .build();
        }

        for (AttackTarget target : additionalTargets) {
            result.additionalTargetResult(resolveAdditionalTarget(attack, target, attackTotal, rolledCritical,
                    blindMiss));
        }

        if (hit) {
            result.onHitRetaliations(RetaliationResolver.resolveOnHit(defender, attack.getAttacker(),
                    attack.getAttackSkill(), attack.getAttackSource(),
                    rolledCritical != null && rolledCritical.isCriticalSuccess()));
            result.unappliedCriticalEffects(CriticalEffectResolver.resolve(attack.getAttacker(),
                    attack.getAttackSource(), attack.getAttackSkill(), criticalEffectTriggered ? criticalResult : null,
                    true, additionalCriticalEffectTypes(attack, criticalResult,
                            effectChainTriggered && carriesCataclysmicExplosion(attack, criticalResult)),
                    attack.getDiceRoller(), false,
                    effectChainTriggered ? thunderousApplications(attack, criticalResult) : 0).unapplied());
            // Força Excessiva: "se o fizer e for bem-sucedido você sofre 2 pontos de Dano Físico Primordial".
            result.lockedSelfDamage(attack.getAttacker().getCharacter().getFeats().stream()
                    .mapToInt(feat -> feat.resolveLockedSelfDamageOnHit(attack.getAttackSkill(),
                            attack.getAttackSource(), attack.getAttacker(), attackRoll))
                    .sum());
        }

        return result.attackResult(attackResult)
                .margin(margin)
                .hit(hit)
                // The roll's own result, reported as rolled; the Sorte-raised severity only drives the effects.
                .criticalResult(rolledCritical)
                .criticalEffectTriggered(criticalEffectTriggered)
                .effectChainTriggered(effectChainTriggered)
                .recordedAction(recordedAction(attack, attackResult, hit, margin, rolledCritical))
                .build();
    }

    /**
     * Bundles the attacker's roll as a ready-to-file {@link CombatantAction} — the caller records
     * it via {@code scene.recordAction(attacker, action)} rather than re-deriving it from {@code
     * getAttackResult().getGoverningAttributeDomain()} and the source/cost it supplied. The verdict
     * comes from {@link AttackDelivery}'s own comparison against the flat Defesa (the Perícia roll
     * itself was made against nothing stated), and {@code turnNumber} from {@link
     * DeliveredAttack#getScene()} when one is present. Never recorded here — {@link #resolve} stays
     * report-only.
     */
    private CombatantAction recordedAction(final DeliveredAttack attack, final InteractionResult attackResult,
                                            final boolean hit, final int margin, final CriticalResult criticalResult) {
        return new CombatantAction(
                attack.getAttackSkill(),
                attackResult.getGoverningAttributeDomain(),
                attack.getAttackSource(),
                attack.getAttackRoll().getActionCost(),
                attack.getScene() == null ? 0 : attack.getScene().getCurrentRound(),
                new ActionOutcome(hit, margin, criticalResult, null),
                attack.getDefender().getId(),
                attack.getAttackRoll().getActivatedFeats());
    }

    /** Whether a held Talento halves this whole attack because of how it was made — Ataque Repentino. */
    private static boolean halvesAttackDamage(final DeliveredAttack attack, final SkillRoll attackRoll) {
        return attack.getAttacker().getCharacter().getFeats().stream()
                .anyMatch(feat -> feat.halvesAttackDamage(attack.getAttackSkill(), attack.getAttackSource(),
                        attack.getAttacker(), attackRoll));
    }

    /** Sorte a Zero's Superficialidade: "incapaz de … desencadear Correntes de Efeitos" (core 0.0.82). */
    private static boolean superficial(final DeliveredAttack attack) {
        return attack.getAttacker() != null
                && attack.getAttacker().hasEgoSetback(org.aventyrs.core.ego.EgoSetback.SUPERFICIALIDADE);
    }

    /** Sorte's chosen success — see {@link SorteEffect#FORCED_SUCCESS}. */
    private static boolean forcedSuccess(final SkillRoll attackRoll) {
        return attackRoll != null && attackRoll.hasSorte(SorteEffect.FORCED_SUCCESS);
    }

    /** Sorte's unleashed Correntes and Efeitos Críticos Maiores — see {@link SorteEffect#UNLEASHED_CRITICALS}. */
    private static boolean unleashed(final SkillRoll attackRoll) {
        return attackRoll != null && attackRoll.hasSorte(SorteEffect.UNLEASHED_CRITICALS);
    }

    /**
     * The same comparison the primary target got, against one additional target's own Defesa, with
     * the <b>one already-rolled</b> {@code attackTotal} and the one {@code criticalResult} —
     * neither is re-derived, because the attack is one roll. What genuinely differs per target is
     * the margin, whether it landed, whether it cleared <em>that</em> defender's Corrente
     * threshold, and the chain built for them.
     *
     * <p>Its chain head is marked {@code halvingDamage()} — "os danos no alvo adicional são
     * reduzidos à metade" — unconditionally, so a provoking Aura's own Meio-Dano adds nothing here
     * and is not passed in: Meio-Dano is a flag, and halving twice is still halving once. The
     * Efeitos Críticos are filtered against this defender's own anatomy, so an immunity of theirs
     * applies to them alone.
     */
    private DeliveredAttackTargetResult resolveAdditionalTarget(final DeliveredAttack attack, final AttackTarget target,
                                                                 final int attackTotal, final CriticalResult criticalResult,
                                                                 final boolean blindMiss) {
        CombatantSheet defender = target.defender();
        int requiredTotal = target.defenseValue()
                + SpellResistance.defenseBonus(defender, attack.getDefenseType(), attack.getAttackSource())
                + defenceFavour(attack, defender);
        int margin = attackTotal - requiredTotal;
        SkillRoll attackRoll = attack.getAttackRoll();
        boolean hit = (margin >= 0 || forcedSuccess(attackRoll)) && !blindMiss
                && !SpellResistance.immune(defender, attack.getAttackSource());
        CriticalResult effectCritical = unleashed(attackRoll) && hit ? CriticalResult.ACERTO_CRITICO_MAIOR : criticalResult;
        boolean suppressed = attack.getAttacker().isMinorCriticalAndChainSuppressed();
        boolean criticalEffectTriggered = hit && effectCritical != null && effectCritical.isCriticalSuccess()
                && !(suppressed && effectCritical.isMinor());
        boolean effectChainTriggered = hit && !suppressed && !superficial(attack)
                && (unleashed(attackRoll) || margin >= effectChainService.getRequiredMargin(attack.getAttacker(),
                        AttackReceiver.positionOf(attack.getScene(), attack.getAttacker(), attack.getSceneContext()),
                        defender, AttackReceiver.positionOf(attack.getScene(), defender, null)));

        return DeliveredAttackTargetResult.builder()
                .defender(defender)
                .requiredTotal(requiredTotal)
                .margin(margin)
                .hit(hit)
                .criticalEffectTriggered(criticalEffectTriggered)
                .effectChainTriggered(effectChainTriggered)
                .nextInteraction(hit
                        ? buildChain(attack, defender, effectCritical, criticalEffectTriggered, effectChainTriggered,
                                // An additional target of a multi-target attack takes Meio-Dano; one
                                // caught in an Área de Efeito takes the hit in full.
                                attack.getAreaOfEffect() == null)
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
     * <p>halfDamage marks the head {@code DamageInteraction} as dealing Meio-Dano — set for every
     * additional target, and for <em>any</em> target when a provoking Aura binds the attacker
     * elsewhere ({@code AbencoadoPelaLuzAbility#ORGULHO_ELDURIANO}). Read as a flag, so the two
     * sources coinciding still halve exactly once rather than quartering. The stages behind it are
     * unaffected: the halving belongs to the damage, not to the Efeitos it triggers.
     */
    private Interaction<CombatantSheet> buildChain(final DeliveredAttack attack,
                                                    final CombatantSheet defender,
                                                    final CriticalResult criticalResult,
                                                    final boolean criticalEffectTriggered,
                                                    final boolean effectChainTriggered,
                                                    final boolean halfDamage) {
        List<Effect> chains = new ArrayList<>();
        if (effectChainTriggered) {
            chains.addAll(attack.getEffectChains());
            chains.addAll(effectChainsGrantedByFeats(attack, criticalResult));
        }
        if (criticalEffectTriggered) {
            // Arte do Escudo Atacante: "seus Acertos Críticos recebem a Corrente de Efeitos – Rugido",
            // whether or not the Corrente threshold was cleared.
            attack.getAttacker().getCharacter().getFeats().forEach(feat -> chains.addAll(
                    feat.resolveCriticalHitEffectChains(attack.getAttacker().getCharacter(), attack.getAttackSkill(),
                            attack.getAttackSource(), attack.getAttacker(), attack.getAttackRoll())));
            // Abrir Defesas: "Após um acerto crítico seu alvo recebe o Malefício Desprevenido por 1 Rodada".
            SkillCompetencyAbility.allFor(attack.getAttacker().getCharacter(), attack.getAttacker()).forEach(ability ->
                    chains.addAll(ability.resolveCriticalHitEffects(attack.getAttackSkill(), attack.getAttacker())));
        }
        List<Effect> criticals = new ArrayList<>();
        if (criticalEffectTriggered) {
            criticals.addAll(CriticalEffect.applicableTo(defender,
                    allCriticalEffects(attack, criticalResult,
                            effectChainTriggered ? thunderousApplications(attack, criticalResult) : 0,
                            effectChainTriggered && carriesCataclysmicExplosion(attack, criticalResult)),
                    criticalResult, attack.getSceneContext()));
        } else {
            // Finalização: a hit that is not critical still applies the Arma Natural's own Efeito
            // Crítico Menor — filtered at Menor, so an anatomy immune to Menores shrugs it off.
            criticals.addAll(CriticalEffect.applicableTo(defender,
                    typedCriticalEffects(attack, criticalResult, false).effects(),
                    CriticalResult.ACERTO_CRITICO_MENOR, attack.getSceneContext()));
        }
        // Tiro Duplo/Múltiplo: "Correntes de Efeito e Efeitos Críticos aplicam seus efeitos duas
        // vezes" — each group once more per extra projectile, right behind the original.
        int repetitions = effectRepetitions(attack);
        List<Effect> stages = new ArrayList<>();
        // Roubo de Vida: the attacker's standing figure plus any against this target, right behind the damage.
        int lifeSteal = lifeStealAgainst(attack, defender);
        if (lifeSteal > 0) {
            stages.add(new org.aventyrs.core.effect.RouboDeVida(attack.getAttacker(), lifeSteal));
        }
        stages.addAll(repeated(chains, repetitions));
        stages.addAll(repeated(criticals, repetitions));

        Interaction<CombatantSheet> next = null;
        for (int i = stages.size() - 1; i >= 0; i--) {
            next = stages.get(i).chainInto(next);
        }
        DamageInteraction head = new DamageInteraction(damageService)
                .fromSpell(SpellResistance.spellOf(attack.getAttackSource()))
                .withSanctity(sanctityOf(attack, chains))
                .withDiceRoller(attack.getDiceRoller());
        if (criticalResult != null && criticalResult.isCriticalSuccess()) {
            // Feridas Ardentes: the critical's Metade da Gnose heals only with a Descanso Verdadeiro
            // or Roubo de Vida.
            head.lockingDamage(attack.getAttacker().getCharacter().getFeats().stream()
                    .mapToInt(feat -> feat.resolveUnhealableCriticalDamage(attack.getAttackSkill(),
                            attack.getAttackSource(), attack.getAttacker().getCharacter(), criticalResult))
                    .sum());
        }
        return (halfDamage ? head.halvingDamage() : head).chainInto(next);
    }

    /**
     * given, rolled against the Defesa a held Talento redirects it to — "contra a DM do alvo, ao invés da DF" ({@code
     * Feat#resolveTargetDefenseOverride}, core 0.0.88) — with every target's Defesa re-read for that type: a foe's off
     * its stat block, anyone else's through {@code DefenseService}. given itself when nothing redirects it.
     */
    private DeliveredAttack againstOverriddenDefense(final DeliveredAttack given) {
        org.aventyrs.core.character.DefenseType override = given.getAttacker().getCharacter().getFeats().stream()
                .map(feat -> feat.resolveTargetDefenseOverride(given.getAttackSkill(), given.getAttackSource(),
                        given.getAttacker(), given.getAttackRoll()))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (override == null || override == given.getDefenseType()) {
            return given;
        }
        return given.toBuilder()
                .defenseType(override)
                .defenseValue(defenseOf(given.getDefender(), override))
                .clearAdditionalTargets()
                .additionalTargets(given.getAdditionalTargets().stream()
                        .map(target -> new AttackTarget(target.defender(), defenseOf(target.defender(), override)))
                        .toList())
                .build();
    }

    private static int defenseOf(final CombatantSheet defender, final org.aventyrs.core.character.DefenseType type) {
        return defender instanceof org.aventyrs.core.monster.MonsterSheet foe
                ? foe.getDefense(type)
                : new org.aventyrs.core.character.services.DefenseServiceImpl().getTotalDefense(defender, type);
    }

    /**
     * The Roubo de Vida this hit carries against defender — {@code LifeStealService#getTotalLifeSteal} for the
     * attacker plus each held Talento's {@code Feat#resolveTargetedLifeSteal} (core 0.0.86).
     */
    private static int lifeStealAgainst(final DeliveredAttack attack, final CombatantSheet defender) {
        CombatantSheet attacker = attack.getAttacker();
        int total = new org.aventyrs.core.character.services.LifeStealServiceImpl()
                .getTotalLifeSteal(attacker.getCharacter(), attacker);
        total += attacker.getCharacter().getFeats().stream()
                .mapToInt(feat -> feat.resolveTargetedLifeSteal(attacker.getCharacter(), attacker, defender))
                .sum();
        // A Bispo's "Roubo de Vida 1 aos seus ataques e magias" (core 0.0.92).
        total += org.aventyrs.core.subordinate.SubordinateBenefit.LIFE_STEAL * org.aventyrs.core.subordinate.SubordinateBenefits.count(attacker, attack.getSceneContext(), org.aventyrs.core.subordinate.SubordinateBenefit.BISPO_LIFE_STEAL);
        return Math.max(0, total);
    }

    /**
     * The sacred or profane nature of this hit (core 0.0.89): Profano when a triggered Corrente is a Toque Sombrio
     * ("Profano em substituição aos seus tipos"), otherwise the nature the attacking weapon's socketed Pedra gives in
     * its Efeito Ofensivo ("em adição aos seus tipos"), otherwise none.
     */
    private static org.aventyrs.core.character.DamageSanctity sanctityOf(final DeliveredAttack attack,
                                                                         final List<Effect> chains) {
        if (chains.stream().anyMatch(org.aventyrs.core.effect.ToqueSombrio.class::isInstance)) {
            return org.aventyrs.core.character.DamageSanctity.PROFANO;
        }
        if (attack.getAttackSource() instanceof org.aventyrs.core.item.Item weapon && weapon.getPowerStone() != null
                && !weapon.isDestroyed() && weapon.getType() == org.aventyrs.core.item.ItemType.OFFENSIVE) {
            return weapon.getPowerStone().getType().getOffensiveSanctity();
        }
        return null;
    }

    /** Every held Talento's extra applications of this attack's Correntes and Efeitos Críticos. */
    private static int effectRepetitions(final DeliveredAttack attack) {
        return attack.getAttacker().getCharacter().getFeats().stream()
                .mapToInt(feat -> feat.resolveAttackEffectRepetitions(attack.getAttackSkill(), attack.getAttackSource(),
                        attack.getAttacker(), attack.getAttackRoll()))
                .sum();
    }

    /** effects, then each of them again repetitions more times as a {@link RepeatedEffect}. */
    private static List<Effect> repeated(final List<Effect> effects, final int repetitions) {
        List<Effect> all = new ArrayList<>(effects);
        for (int i = 0; i < repetitions; i++) {
            effects.forEach(effect -> all.add(new RepeatedEffect(effect)));
        }
        return all;
    }

    /**
     * How many more times a triggered Corrente makes the natural Efeito Crítico apply on a critical —
     * one per {@link GolpeTrovejante} among the attack's own and its Talentos' Correntes ({@code
     * DuelistaFeat#MAESTRIA_EM_ARMA}). Only meaningful when the Corrente threshold was cleared.
     */
    private int thunderousApplications(final DeliveredAttack attack, final CriticalResult criticalResult) {
        List<Effect> chains = new ArrayList<>(attack.getEffectChains());
        chains.addAll(effectChainsGrantedByFeats(attack, criticalResult));
        return chains.stream()
                .filter(GolpeTrovejante.class::isInstance)
                .map(GolpeTrovejante.class::cast)
                .mapToInt(GolpeTrovejante::extraNaturalCriticalEffectApplications)
                .sum();
    }

    /**
     * The caller-supplied Efeitos Críticos plus every one the attacker's Talentos add for this
     * kind of hit — {@code AssassinoFeat#ABRIR_FERIDAS}'s "'Sangramento' como Efeito Crítico
     * adicional". Talentos are outside every {@code ModifierResolver} scan, so they get an
     * explicit pass, the same shape {@code AbstractSkillInteraction} uses for its own {@code
     * Feat} hooks.
     */
    private List<CriticalEffect> allCriticalEffects(final DeliveredAttack attack, final CriticalResult criticalResult,
                                                    final int thunderousApplications,
                                                    final boolean cataclysmicExplosion) {
        List<CriticalEffect> effects = new ArrayList<>(attack.getCriticalEffects());
        attack.getAttacker().getCharacter().getFeats().forEach(feat ->
                effects.addAll(feat.resolveExtraCriticalEffects(attack.getAttacker().getCharacter(),
                        attack.getAttackSkill(), attack.getAttackSource(), criticalResult)));
        effects.addAll(CriticalEffectResolver.resolve(attack.getAttacker(), attack.getAttackSource(),
                attack.getAttackSkill(), criticalResult, true,
                additionalCriticalEffectTypes(attack, criticalResult, cataclysmicExplosion),
                attack.getDiceRoller(), true, thunderousApplications).effects());
        return effects;
    }

    /**
     * The request's additional Efeitos Críticos, plus Cataclismo when a triggered Corrente is an Explosão
     * Cataclísmica — "… e Cataclismo como um Efeito Crítico adicional".
     */
    private static List<CriticalEffectType> additionalCriticalEffectTypes(final DeliveredAttack attack,
                                                                          final CriticalResult criticalResult,
                                                                          final boolean cataclysmicExplosion) {
        List<CriticalEffectType> additional = new ArrayList<>(attack.getAdditionalCriticalEffectTypes());
        if (cataclysmicExplosion) {
            additional.add(ExplosaoCataclismica.ADDITIONAL_CRITICAL_EFFECT);
        }
        return additional;
    }

    /** Whether a triggered Corrente of this attack is an {@link ExplosaoCataclismica} — its own or a Talento's. */
    private boolean carriesCataclysmicExplosion(final DeliveredAttack attack, final CriticalResult criticalResult) {
        return attack.getEffectChains().stream().anyMatch(ExplosaoCataclismica.class::isInstance)
                || effectChainsGrantedByFeats(attack, criticalResult).stream().anyMatch(ExplosaoCataclismica.class::isInstance);
    }

    /**
     * The Efeitos Críticos this attack carries by identity — its source's own, the attacker's
     * Títulos', the request's — built through {@link CriticalEffectResolver}. critical selects
     * whether this is the critical hit's set or a plain hit's (Finalização's Menor repetitions).
     */
    private CriticalEffectResolver.Resolved typedCriticalEffects(final DeliveredAttack attack,
                                                                 final CriticalResult criticalResult,
                                                                 final boolean critical) {
        return CriticalEffectResolver.resolve(attack.getAttacker(), attack.getAttackSource(), attack.getAttackSkill(),
                critical ? criticalResult : null, true, attack.getAdditionalCriticalEffectTypes(),
                attack.getDiceRoller());
    }

    private List<EffectChain> effectChainsGrantedByFeats(final DeliveredAttack attack, final CriticalResult criticalResult) {
        List<EffectChain> chains = new java.util.ArrayList<>(attack.getAttacker().getCharacter().getFeats().stream()
                .flatMap(feat -> feat.resolveEffectChains(attack.getAttacker().getCharacter(),
                        attack.getAttackSkill(), attack.getAttackSource(), attack.getAttacker(),
                        attack.getSceneContext(), criticalResult).stream())
                .toList());
        // A foe's own stat block — an invoked Lacerto creature's Inocular Veneno or Devorar Inteiro (core 0.0.92).
        if (attack.getAttacker() instanceof org.aventyrs.core.monster.MonsterSheet foe) {
            chains.addAll(foe.getAttackEffectChains());
        }
        return chains;
    }

    /**
     * Caído's and Cego's outward Favorecido em Esquiva e Aparar: a defender who attacked the attacker
     * while it was Caído or Cego defends against it with Vantagem. A foe rolls no defence, so the
     * Vantagem lands on its flat Defesa.
     */
    private static int defenceFavour(final DeliveredAttack attack, final CombatantSheet defender) {
        return attack.getAttacker().favoursDefenceBy(defender, attack.getSceneContext())
                ? org.aventyrs.core.skill.Skill.ADVANTAGE_BONUS
                : 0;
    }
}
