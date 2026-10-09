package org.aventyrs.core.combat;

import org.aventyrs.core.ego.AutocontroleDefence;
import org.aventyrs.core.sheet.CenaImmunity;
import org.aventyrs.core.ego.SorteEffect;
import org.aventyrs.core.sheet.AttackerGuard;
import org.aventyrs.core.scene.InitiativePosition;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
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
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.DuelistaFeat;
import org.aventyrs.core.sheet.ActionOutcome;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillInteractionFactory;
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


import static org.aventyrs.core.util.TranslatableMessages.DEFENSE_SUBSTITUTION_NOT_PERMITTED;
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
        // Devorado: "Não pode ser afetado por efeitos externos" — only its devourer reaches it.
        if (attack.getAttacker() != null && defender.isShieldedFrom(attack.getAttacker())) {
            throw new IllegalOperationException(org.aventyrs.core.util.TranslatableMessages.TARGET_INSIDE_DEVOURER);
        }
        boolean auraHalvesDamage = AuraTargeting.resolveHalvesDamage(attack.getScene(), attack.getAttacker(),
                defender, attack.isForcedTargetUnavailable());
        SkillRoll defenseRoll = attack.getDefenseRoll();

        SkillType defenseSkill = substitutedDefenseSkill(attack);
        InteractionResult defenseResult = defenseSkill == null
                ? esquivaEApararInteraction.applyTo(defender, attack.getSceneContext(), defenseRoll,
                        attack.getDefenseType(), attack.getDamageDescriptor())
                // Defender-se Atacando: the Perícia de Ataque's roll stands where the whole defence
                // would have. Its activation (and the once-per-Rodada limit) is validated by the roll.
                : SkillInteractionFactory.create(defenseSkill).applyTo(defender, attack.getSceneContext(), defenseRoll);

        // Artesão de Barreiras / Aptidão Mágica Suprema: the GD to resist a Magia the defender can cast.
        DifficultyLevel effectiveDifficultyLevel = attack.getDifficultyLevel().easier(defenseResult.getDifficultyReduction()
                + SpellResistance.difficultyReduction(defender, attack.getAttackSource()));
        // Favorecido em Perícias de Ataque against a Caído/Cego/Flanqueado/… defender.
        int requiredTotal = effectiveDifficultyLevel.getBaseValue() + attack.getAttackBonus()
                + defender.getAttackerAttackRollBonus(attack.getSceneContext());
        int defenseTotal = defenseResult.getSkillRollBonus()
                // Caído/Cego attacker: whoever attacked it while it was so defends with Vantagem.
                + (attack.getAttacker() != null
                        && attack.getAttacker().favoursDefenceBy(defender, attack.getSceneContext())
                        ? org.aventyrs.core.skill.Skill.ADVANTAGE_BONUS : 0)
                + (defenseRoll == null ? 0 : defenseRoll.getTotal())
                + (attack.isAreaOfEffect() ? areaOfEffectDefenseBonus(defender) : 0)
                // The Aptidões Mágicas' DM against a Magia the defender can cast.
                + SpellResistance.defenseBonus(defender, attack.getDefenseType(), attack.getAttackSource());

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
        boolean immune = defender.getGuardsAgainst(attack.getAttacker()).stream().anyMatch(AttackerGuard::isImmune)
                // Aptidão Mágica Dracônica: immune to a Magia the defender can cast — defended outright.
                || SpellResistance.immune(defender, attack.getAttackSource());
        // Trava Mental: "não é capaz de se defender de ataques do tipo escolhido" — only an immunity holds.
        boolean undefendable = attack.getDamageDescriptor() != null && defender.getCharacter().getFeats().stream()
                .anyMatch(feat -> feat.preventsDefenseAgainst(attack.getDamageDescriptor(), defender.getCharacter()));
        // A Cego defender's failed 1d6 fails the defence whatever its total.
        // Sorte's chosen success defends "independente do resultado dos dados" — never a defence Trava Mental forbids.
        boolean forcedSuccess = defenseRoll.hasSorte(SorteEffect.FORCED_SUCCESS);
        // Imobilizado/Desacordado: may not act, but still defends — succeeding unless it is a Falha
        // Crítica (table ruling, 2026-10-07).
        boolean heldDefence = defender.defendsUnlessCriticalFailure(attack.getSceneContext())
                && (defenseResult.getCriticalResult() == null || !defenseResult.getCriticalResult().isCriticalFailure());
        boolean defended = ((margin <= 0 || forcedSuccess || heldDefence) && !undefendable
                && !Boolean.TRUE.equals(defenseResult.getBlindCheckFailed()))
                || immune;
        CriticalResult criticalResult = defenseResult.getCriticalResult();
        // Sorte a Zero's Azarão on the defence: failed by 5 or more, the attacker's critical effects land as a Menor's.
        if (!defended && margin >= org.aventyrs.core.skill.AbstractSkillInteraction.AZARAO_MARGIN
                && defender.hasEgoSetback(org.aventyrs.core.ego.EgoSetback.AZARAO)
                && (criticalResult == null || !criticalResult.isCriticalFailure())) {
            criticalResult = CriticalResult.FALHA_CRITICA_MENOR;
        }
        // The defender's Autocontrole (core 0.0.81): what it avoids does not land, and is not reported as landing.
        boolean chainAvoided = defenseRoll.hasAutocontrole(AutocontroleDefence.AVOID_CHAIN)
                || defenseRoll.hasAutocontrole(AutocontroleDefence.AVOID_CHAIN_WITH_IMMUNITY);
        boolean criticalAvoided = defenseRoll.hasAutocontrole(AutocontroleDefence.AVOID_CRITICAL_WITH_IMMUNITY)
                || defenseRoll.hasAutocontrole(AutocontroleDefence.IGNORE_MINOR_CRITICAL)
                        && criticalResult != null && criticalResult.isMinor();
        boolean criticalEffectTriggered = !defended && criticalResult != null && criticalResult.isCriticalFailure();
        boolean effectChainTriggered = !defended
                && margin >= effectChainService.getRequiredMargin(attack.getAttacker(),
                        positionOf(attack.getScene(), attack.getAttacker(), null), defender,
                        positionOf(attack.getScene(), defender, attack.getSceneContext()));
        if (effectChainTriggered && chainAvoided
                && defenseRoll.hasAutocontrole(AutocontroleDefence.AVOID_CHAIN_WITH_IMMUNITY)) {
            attack.getEffectChains().forEach(stage -> defender.grantCenaImmunity(CenaImmunity.kindOf(stage)));
        }
        if (criticalEffectTriggered && criticalAvoided
                && defenseRoll.hasAutocontrole(AutocontroleDefence.AVOID_CRITICAL_WITH_IMMUNITY)) {
            criticalEffectsOf(attack, criticalResult)
                    .forEach(effect -> defender.grantCenaImmunity(CenaImmunity.kindOf(effect)));
        }
        effectChainTriggered = effectChainTriggered && !chainAvoided;
        criticalEffectTriggered = criticalEffectTriggered && !criticalAvoided;

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
        } else if (!immune && (criticalResult != null && criticalResult.isCriticalSuccess()
                || defenseRoll.hasSorte(SorteEffect.UNLEASHED_CRITICALS))) {
            // "Efeitos Críticos Defensivos substituem as falhas críticas inimigas em caso de Sucesso
            // Crítico nas rolagens de Defesas" — built for the caller to apply.
            for (DefensiveCriticalEffectType type : DefensiveCriticalEffects.grantedTo(defender)) {
                // Sorte's permanent point: the defence's Efeitos Críticos apply as Maior — see SorteEffect.
                CriticalResult defensiveSeverity = defenseRoll.hasSorte(SorteEffect.UNLEASHED_CRITICALS)
                        ? CriticalResult.ACERTO_CRITICO_MAIOR : criticalResult;
                DefensiveCriticalEffect.of(type, defender, attack.getAttacker(), defensiveSeverity,
                                attack.getAttackSkill(), attack.getAttackSource(), attack.getDiceRoller())
                        .ifPresentOrElse(result::defensiveCriticalEffect,
                                () -> result.unappliedCriticalEffect(type));
            }
        }

        if (defenseSkill != null) {
            if (defended) {
                result.counterWeaponDamage(defender.getAttributeTotal(AttributeDomain.STRENGTH));
            } else {
                result.attackerDamageAdvantage(true);
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
        SkillType defenseSkill = attack.getDefenseSkill() == null ? SkillType.ESQUIVA_E_APARAR : attack.getDefenseSkill();
        return new CombatantAction(
                defenseSkill,
                defenseResult.getGoverningAttributeDomain(),
                null,
                attack.getDefenseRoll().getActionCost(),
                attack.getScene() == null ? 0 : attack.getScene().getCurrentRound(),
                new ActionOutcome(defended, defenderMargin, criticalResult, effectiveDifficultyLevel),
                null,
                attack.getDefenseRoll().getActivatedFeats());
    }

    /**
     * The Perícia de Ataque this defence is rolled with in place of Esquiva e Aparar, or {@code null}
     * for the ordinary defence — refusing ({@code DEFENSE_SUBSTITUTION_NOT_PERMITTED}) a substitution
     * that is not an attack Perícia, one no held Talento permits against this Defesa (Defender-se
     * Atacando for the Física, its Superior for the Mágica unless the attack is an Encantamento or a
     * Maldição), and a rolled one that does not activate Defender-se Atacando.
     */
    private static SkillType substitutedDefenseSkill(final IncomingAttack attack) {
        SkillType skill = attack.getDefenseSkill();
        if (skill == null || skill == SkillType.ESQUIVA_E_APARAR) {
            return null;
        }
        Character defender = attack.getDefender().getCharacter();
        boolean enchantmentOrCurse = attack.isEnchantmentOrCurseAttack();
        boolean permitted = skill.isAttackSkill() && defender.getFeats().stream()
                .anyMatch(feat -> feat.permitsDefenseSubstitution(attack.getDefenseType(), enchantmentOrCurse));
        boolean activated = attack.getDefenseRoll() == null
                || attack.getDefenseRoll().activated(DuelistaFeat.DEFENDER_SE_ATACANDO);
        if (!permitted || !activated) {
            throw new IllegalOperationException(DEFENSE_SUBSTITUTION_NOT_PERMITTED);
        }
        return skill;
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
            stages.addAll(CriticalEffect.applicableTo(attack.getDefender(), criticalEffectsOf(attack, criticalResult),
                    criticalResult, attack.getSceneContext()));
        } else {
            // Finalização on this side too: a non-critical hit applies the natural weapon's Menor.
            stages.addAll(CriticalEffect.applicableTo(attack.getDefender(),
                    CriticalEffectResolver.resolve(attack.getAttacker(), attack.getAttackSource(),
                            attack.getAttackSkill(), null, true, List.of(), attack.getDiceRoller()).effects(),
                    CriticalResult.FALHA_CRITICA_MENOR, attack.getSceneContext()));
        }

        // Autocontrole's Cena immunity: a kind the defender is immune to is left out.
        stages.removeIf(stage -> attack.getDefender().isCenaImmune(CenaImmunity.kindOf(stage)));
        Interaction<CombatantSheet> next = null;
        for (int i = stages.size() - 1; i >= 0; i--) {
            next = stages.get(i).chainInto(next);
        }
        DamageInteraction head = new DamageInteraction(damageService)
                .fromSpell(SpellResistance.spellOf(attack.getAttackSource()))
                .withDiceRoller(attack.getDiceRoller());
        return (halfDamage ? head.halvingDamage() : head).chainInto(next);
    }

    /** Every Efeito Crítico a critical of criticalResult brings: the request's own, the weapon's and the Títulos'. */
    private static List<CriticalEffect> criticalEffectsOf(final IncomingAttack attack, final CriticalResult criticalResult) {
        List<CriticalEffect> effects = new ArrayList<>(attack.getCriticalEffects());
        effects.addAll(CriticalEffectResolver.resolve(attack.getAttacker(), attack.getAttackSource(),
                attack.getAttackSkill(), criticalResult, true, attack.getAdditionalCriticalEffectTypes(),
                attack.getDiceRoller()).effects());
        return effects;
    }

    /** Every held Habilidade de Competência's Defesa against an Área de Efeito (Evasão). */
    private static int areaOfEffectDefenseBonus(final CombatantSheet defender) {
        return org.aventyrs.core.skill.SkillCompetencyAbility.allFor(defender.getCharacter(), defender).stream()
                .mapToInt(ability -> ability.resolveAreaOfEffectDefenseBonus(defender.getCharacter()))
                .sum();
    }

    /**
     * Where combatant stands in the order of play — read off its own context when that is the one in
     * hand, else off the live Scene, else {@code UNKNOWN}. What Grande Analista Tático's "Enquanto você
     * for o último a agir" reads on either side of a Corrente.
     */
    static InitiativePosition positionOf(final Scene scene, final CombatantSheet combatant,
                                         final SceneContext ownContext) {
        if (ownContext != null && ownContext.getInitiativePosition() != InitiativePosition.UNKNOWN) {
            return ownContext.getInitiativePosition();
        }
        if (scene == null || combatant == null) {
            return InitiativePosition.UNKNOWN;
        }
        return scene.initiativePositionOf(combatant);
    }
}
