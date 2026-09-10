package org.aventyrs.core.combat;

import lombok.NonNull;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * One combatant an attack is being made against, paired with the Defesa the attack total has to
 * reach for it — the unit {@link DeliveredAttack#getAdditionalTargets()} is a list of.
 *
 * <p>The primary target stays spelled out as {@code defender}/{@code defenseValue} fields on
 * {@link DeliveredAttack} rather than being folded in here: every attack has exactly one, it is
 * the target every target-conditioned ability resolves against, and it is what every existing
 * caller already supplies. Only the <em>extra</em> targets are a list, and only a Talento such as
 * {@code ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA} ever produces one.
 *
 * <p>A Defesa per target and not one shared number, because it is each defender's own stat — but
 * the {@link DefenseType} <em>is</em> shared, and stays on {@link DeliveredAttack}: one blow is
 * resisted the same way by everyone it reaches.
 *
 * @param defender     who is being hit
 * @param defenseValue their Defesa as a target number, of {@link DeliveredAttack#getDefenseType()}
 */
public record AttackTarget(@NonNull CombatantSheet defender, int defenseValue) {

    /**
     * An {@link AttackTarget} with the Defesa already read off the foe's own stat block — the
     * mirror of {@link DeliveredAttack#from(MonsterSheet, DefenseType)}, for the same reason: a
     * caller holding the sheet shouldn't have to hand-copy a number that lives on it.
     */
    public static AttackTarget of(@NonNull final MonsterSheet foe, @NonNull final DefenseType defenseType) {
        return new AttackTarget(foe, foe.getDefense(defenseType));
    }
}
