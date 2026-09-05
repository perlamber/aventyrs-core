package org.aventyrs.core.combat;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.effect.CriticalEffect;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

import java.util.List;

/**
 * One attack the player is making, bundled — the request half of {@link AttackDelivery#resolve},
 * and the mirror image of {@link IncomingAttack}.
 *
 * <p>The two are mirrored because the rule behind them is: <b>the player always rolls</b>. When
 * the player is attacked, the foe contributes a fixed GD and the player rolls Esquiva e Aparar
 * ({@link IncomingAttack}). When the player attacks, the foe contributes a fixed Defesa and the
 * player rolls an Ataque Perícia (this class). Neither request reaches into the other side for
 * its number — both take it already resolved, and {@link #from} fills it off a {@link
 * MonsterSheet} when there is one.
 *
 * <p>Damage is absent here for the same reason it's absent from {@link IncomingAttack}: turning
 * a roll into a raw damage figure needs a weapon/dano-roll concept this core doesn't have. What
 * {@link AttackDelivery} produces instead is a pre-wired chain with a {@link DamageInteraction}
 * at its head for the caller to supply that figure to.
 */
@Getter
@Builder
public class DeliveredAttack {

    /** The character making the attack — the one who rolls. */
    @NonNull
    private final CombatantSheet attacker;

    /** Who is being attacked. Also the combatant every triggered Efeito is applied to. */
    @NonNull
    private final CombatantSheet defender;

    /**
     * Which Perícia de Ataque is being rolled. Only {@code SkillType#isAttackSkill()} constants
     * are meaningful; {@link AttackDelivery} rejects anything else rather than quietly resolving
     * an Atletismo roll as an attack.
     */
    @NonNull
    private final SkillType attackSkill;

    /**
     * The attacker's already-rolled 3d6, or {@code null} when the caller wants only the bonuses
     * and threshold reported and hasn't rolled yet. With {@code null}, every outcome on {@link
     * DeliveredAttackResult} stays {@code null} — undetermined, not a miss.
     */
    private final SkillRoll attackRoll;

    /**
     * The defender's Defesa as a target number — its DF or DM. Authored on the foe's stat block
     * rather than derived, since a foe never rolls; see {@code MonsterTemplate}.
     */
    private final int defenseValue;

    /** Which Defesa {@link #defenseValue} is, for reporting and for scoped effects. */
    @NonNull
    private final DefenseType defenseType;

    /** Nearby allies/enemies and their ranges, or {@code null} outside an encounter. */
    private final SceneContext sceneContext;

    /**
     * What this attack is being delivered with — pass the {@link org.aventyrs.core.item.Weapon}
     * or the {@link org.aventyrs.core.magic.Spell} itself, since both <i>are</i> {@link
     * AttackSource}s. {@code null} when the caller didn't say: optional like {@link
     * #sceneContext}/{@link #attackRoll}, and read as "no scope matched" rather than as an error,
     * so every existing caller is unaffected.
     *
     * <p>It reaches the Perícia roll itself, where a delivery-scoped ability such as {@code
     * AtaqueADistanciaCompetencyAbility#ARREMESSO_PODEROSO} narrows on it by type — including on
     * the {@code attackRoll == null} preview path, since which Attribute governs the roll is part
     * of the bonus figure a caller shows the player before they roll.
     *
     * <p>It deliberately does <b>not</b> fill in {@link #attackSkill}, even though {@link
     * AttackSource#getAttackSkillType()} could answer: a caller passing both would then have one
     * of them silently overrule the other. Naming the Perícia stays the caller's call, and the
     * two disagreeing is a caller bug this class doesn't hide. Nothing validates that they
     * agree either — the usual builders-aren't-gatekeepers restraint.
     */
    private final AttackSource attackSource;

    /**
     * Targets this attack affects <b>beyond</b> {@link #defender}, each with their own Defesa —
     * empty for every ordinary attack. What {@code
     * ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA}'s "seus ataques afetam um alvo
     * adicional" produces, and what {@link AttackDelivery#resolve} compares the one attack total
     * against in turn, reporting each on {@link DeliveredAttackResult#getAdditionalTargetResults()}.
     *
     * <p><b>The caller picks them.</b> The rules require an additional target to be adjacent to
     * the primary one, which is pairwise geometry between two combatants who are both not the
     * roller — something a {@code SceneContext} cannot answer and this core never computes. What
     * {@link AttackDelivery} does enforce is the <em>count</em>, against {@code
     * AttackTargetingService#getMaximumTargets}: a caller cannot name more targets than the
     * attacker's Talentos entitle them to.
     */
    @Singular
    private final List<AttackTarget> additionalTargets;

    /**
     * The Efeitos Críticos this attack inflicts if the attack roll comes up an Acerto Crítico.
     * Caller-constructed, because this core has no way to know which one a given weapon or Magia
     * carries.
     *
     * <p>Unlike the defensive direction, these need no translation: {@code
     * CriticalEffect#validateCriticalHit} demands an Acerto Crítico, which is exactly what the
     * attacker rolling a critical produces. This is the direction that validation was written for.
     */
    @Singular
    private final List<CriticalEffect> criticalEffects;

    /**
     * The Correntes de Efeitos this attack inflicts if it clears the Defesa by the required
     * margin — see {@link AttackDelivery}. Independent of {@link #criticalEffects}.
     */
    @Singular
    private final List<EffectChain> effectChains;

    /**
     * A {@link DeliveredAttackBuilder} with defender and {@link #defenseValue} already filled from
     * foe's own stat block — the convenience for the common case of attacking a monster, so a
     * caller doesn't hand-copy a number that lives on the sheet it already holds. The caller
     * still supplies the attacker, the Perícia and the roll.
     */
    public static DeliveredAttackBuilder from(@NonNull final MonsterSheet foe, @NonNull final DefenseType defenseType) {
        return builder()
                .defender(foe)
                .defenseType(defenseType)
                .defenseValue(foe.getDefense(defenseType));
    }
}
