package org.aventyrs.core.combat;

import org.aventyrs.core.effect.CriticalEffectType;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.CriticalResult;

import java.util.List;

/**
 * What {@link AttackDelivery#resolve} reports about one attack the player made — the mirror of
 * {@link IncomingAttackResult}. Purely a report: nothing here has been applied, and even the
 * chain is only assembled.
 *
 * <p>A {@code null} boxed field means "not applicable", which here means no {@code attackRoll}
 * was supplied, so the outcome is undetermined rather than a miss.
 */
@Getter
@Builder
public class DeliveredAttackResult {

    /**
     * The attacker's full Perícia de Ataque {@link InteractionResult} — its {@code
     * skillRollBonus} (including any target-conditioned bonus the newly-lifted 4-arg {@code
     * applyTo} resolved), {@code difficultyReduction}, {@code criticalResult}, {@code
     * damageBonus}, and — on a hit — {@link InteractionResult#getNextInteraction()} set to the
     * head of the assembled stage chain, aimed at the defender.
     */
    private final InteractionResult attackResult;

    /** The attacker's total: their roll bonus plus the dice. The bonus alone with no roll. */
    private final int attackTotal;

    /** What {@link #attackTotal} had to reach — the defender's Defesa. */
    private final int requiredTotal;

    /**
     * Whether a provoking Aura makes this attack deal Meio-Dano — {@code true} when a bound
     * attacker, having already attacked the Aura's holder this Rodada, attacks someone else
     * ({@code AbencoadoPelaLuzAbility#ORGULHO_ELDURIANO}).
     *
     * <p><b>Applied, not merely reported</b>: when true the primary target's chain head is marked
     * {@code DamageInteraction#halvingDamage()}, exactly as an additional target's always is. It is
     * surfaced here so a UI can show where the reduction came from. It does <em>not</em> touch
     * {@link #attackTotal} — V19 moved this penalty off the roll and onto the damage.
     */
    private final boolean auraHalvesDamage;

    /**
     * Whether a Talento halved the damage on every target, the primary included — {@code
     * CavalariaFeat#ATAQUE_EM_ARCO}. Applied, like {@link #auraHalvesDamage}, by marking the primary's
     * chain head {@code halvingDamage()}; the two are read as one flag, so they never quarter.
     */
    private final boolean everyTargetHalved;

    /**
     * What the defender's thorns deal back to the attacker, or {@code null} when nothing does —
     * see {@link Retaliation}, which carries the whole calculation. <b>Reported, never dealt</b>:
     * this core sends damage only one way, so the caller applies it against the attacker's sheet.
     */
    private final Retaliation retaliation;

    /**
     * What the primary defender's worn Espinhosa / Rigidez Refletora do back to this attack — hit or miss — or {@code
     * null} (core 0.1.6.11). Reported, never dealt: see {@link EquipmentRetaliation#applyTo}.
     */
    private final EquipmentRetaliation equipmentRetaliation;

    /**
     * How many of {@link #projectilesToConsume} a Rigidez Refletora destroyed — never recovered at combat end. Pass it
     * to {@code AmmunitionService#consume}'s five-argument form.
     */
    private final int projectilesDestroyed;

    /**
     * PV the <b>attacker</b> owes for this hit, only a Descanso Verdadeiro recovers — {@code
     * Feat#resolveLockedSelfDamageOnHit}, {@code DuelistaFeat#FORCA_EXCESSIVA}'s "se o fizer e for
     * bem-sucedido você sofre 2 pontos de Dano Físico Primordial". 0 on a miss. Reported, never
     * dealt: the caller pays it with {@code attacker.payWithVitality(lockedSelfDamage)} — no
     * mitigation ("não podem ser reduzidos"), then locked.
     */
    private final int lockedSelfDamage;

    /**
     * Raw damage this hit deals to the primary target's worn Escudo, else Armadura — {@code
     * AttributeAbility#resolveDefensiveEquipmentDamageOnHit}, {@code StrengthAbility#ESTILHACADOR}'s 2. 0 on a miss
     * or with nothing to deal. Reported, never dealt: the target's owner applies it with {@code
     * ShieldBreak#against(target, amount).apply()}.
     */
    private final int defensiveEquipmentDamage;

    /**
     * The projectile this shot fired ({@code DeliveredAttack#getProjectile()}), and how many of it — 1, plus one per
     * extra projectile a Tiro Duplo/Múltiplo fires. Reported whether or not the shot landed: an arrow loosed is spent.
     * The caller spends them with {@code AmmunitionService#consume}; {@code null}/0 when nothing was fired or on the
     * no-roll preview path.
     */
    private final org.aventyrs.core.item.ProjectileItem firedProjectile;

    private final int projectilesToConsume;

    /**
     * What the defender's Talentos deal back because this attack <b>landed</b> — {@code
     * Feat#resolveRetaliation}, one {@link Retaliation} per answering Talento. Empty on a miss, on
     * an Ataque à Distância, and without a roll. <b>Reported, never dealt</b>, like {@link
     * #retaliation}; skip it when mitigation left the hit dealing nothing ("lhe infligir danos").
     */
    @Singular
    private final List<Retaliation> onHitRetaliations;


    /**
     * By how much the attack beat the Defesa — {@code attackTotal - requiredTotal}, so zero or
     * positive on a hit. What {@link #effectChainTriggered} is judged against. {@code null}
     * without an {@code attackRoll}.
     */
    private final Integer margin;

    /** {@code true} when the attack landed, {@code null} without an {@code attackRoll}. */
    private final Boolean hit;

    /** The attack roll's own critical outcome, or {@code null} without an {@code attackRoll}. */
    private final CriticalResult criticalResult;

    /**
     * Whether this attack's Efeitos Críticos fired — it landed <i>and</i> the roll was an Acerto
     * Crítico. {@code null} without an {@code attackRoll}.
     */
    private final Boolean criticalEffectTriggered;

    /**
     * Whether this attack's Correntes de Efeitos fired — it landed <i>and</i> cleared the Defesa
     * by {@code EffectChainService}'s required margin. Independent of {@link
     * #criticalEffectTriggered}. {@code null} without an {@code attackRoll}.
     */
    private final Boolean effectChainTriggered;

    /**
     * The attacker's own {@code difficultyReduction} in níveis — <b>already applied</b>: every
     * target's {@code requiredTotal} is eased by it (see {@link AttackDelivery}'s javadoc). Reported
     * so a caller can show why the number rolled against moved.
     */
    private final int difficultyReduction;

    /**
     * Níveis the primary defender's Talentos take off the GD to resist this Magia — Artesão de
     * Barreiras, Aptidão Mágica Suprema ({@code Feat#resolveSpellResistanceDifficultyReduction}).
     * <b>Already applied</b>: {@link #requiredTotal} is that many níveis harder. 0 for anything but
     * a Magia.
     */
    private final int spellResistanceReduction;

    /**
     * The attacker's roll bundled as a {@link CombatantAction} ready to file — its Perícia,
     * the resolved {@code governingAttributeDomain}, the {@link DeliveredAttack#getAttackSource()}
     * and {@code ActionCost} the caller supplied, the {@code turnNumber} (from {@link
     * DeliveredAttack#getScene()}'s current Rodada, or 0 with no Scene) and the roll's verdict.
     * {@code null} on the {@code attackRoll == null} preview path — no action was taken.
     *
     * <p>{@link AttackDelivery#resolve} builds it but does <b>not</b> record it (it stays
     * report-only). The caller files it with {@code scene.recordAction(attacker, action)} (or
     * {@code attacker.recordAction(action)} with no Scene) — one call, no hand-assembly.
     */
    private final CombatantAction recordedAction;

    /**
     * One entry per {@link DeliveredAttack#getAdditionalTargets()}, in the order they were
     * supplied — empty for every ordinary single-target attack.
     *
     * <p>The primary target's own outcome stays on this class's flat fields rather than joining
     * the list: every attack has exactly one primary, every existing caller already reads it
     * there, and only it carries the {@link #attackResult} the whole attack shares.
     */
    @Singular
    private final List<DeliveredAttackTargetResult> additionalTargetResults;

    /**
     * Efeitos Críticos this attack should have applied by identity but could not be built — no
     * {@code DiceRoller} for a dice-bearing one, or no mechanism at all (Desmembrar). Empty on an
     * attack that triggered none. For the caller to adjudicate, never silently dropped.
     */
    @Singular("unappliedCriticalEffect")
    private final List<CriticalEffectType> unappliedCriticalEffects;
}
