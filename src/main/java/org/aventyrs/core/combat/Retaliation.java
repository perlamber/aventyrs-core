package org.aventyrs.core.combat;

import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;

/**
 * Damage an attacker takes back for having attacked, reported on {@link
 * DeliveredAttackResult#getRetaliation()} / {@link IncomingAttackResult#getRetaliation()} —
 * {@code AbracadoPelaEscuridaoAbility#ESPINHOS_VENENOS_DE_GAEA} is the one source today.
 *
 * <p><b>Reported by the attack, dealt by the caller</b> — {@link #dealTo} does the whole of it in
 * one call (an ordinary typed {@code DamageService#applyDamage} against the attacker's own sheet,
 * then the Malefício if the damage landed). The attack paths never deal it themselves: they stay
 * report-only, the same division of labour {@code Teleportation} and {@code Scene#refreshAura} use.
 *
 * <p>The attack's own outcome is not this record's business: the thorns answer "atacarem", so they
 * are reported whether or not the attack landed. The {@link #conditionOnDamage} half is the one
 * that is conditional — "Personagem que lhe infligirem danos <i>adicionalmente</i> perdem…" — so
 * a caller applies it only when the attack actually dealt damage.
 *
 * @param damage           how much the attacker takes
 * @param descriptor       what kind of damage it is, so the attacker's own resistances can judge it
 * @param conditionOnDamage a Malefício the attacker additionally takes <b>if their attack dealt
 *                          damage</b>, or {@code null} when the retaliation inflicts none
 * @param conditionRounds  how long {@link #conditionOnDamage} lasts, in Rodadas; 0 when there is none
 * @param conditionEffects the inflicting source's own magnitudes for {@link #conditionOnDamage}
 *                         ("perdem -1 Multiplicador de Pontos de Vida"), carried on the Condição
 */
public record Retaliation(int damage,
                          DamageDescriptor descriptor,
                          ConditionType conditionOnDamage,
                          int conditionRounds,
                          java.util.List<ConditionType.ConditionEffect> conditionEffects) {

    public Retaliation {
        conditionEffects = conditionEffects == null ? java.util.List.of() : java.util.List.copyOf(conditionEffects);
    }

    /** A retaliation whose Malefício carries no magnitudes of its own. */
    public Retaliation(final int damage, final DamageDescriptor descriptor, final ConditionType conditionOnDamage,
                       final int conditionRounds) {
        this(damage, descriptor, conditionOnDamage, conditionRounds, java.util.List.of());
    }

    /**
     * Deals this retaliation to attacker — the one step the report leaves to its caller, written
     * once. The damage goes through the ordinary {@code DamageService#applyDamage} path, typed by
     * {@link #descriptor}, so the attacker's own RD/RDS/RE, immunities and Meio-Dano judge it like
     * any other hit; {@code source} is the defender whose thorns these are, when the caller has it.
     * {@link #conditionOnDamage} is applied only when that damage actually landed.
     *
     * @return the PV the attacker actually lost (shield points absorbed are not PV lost)
     */
    public int dealTo(final CombatantSheet attacker, final CombatantSheet source, final SceneContext sceneContext,
                      final DamageService damageService) {
        int before = attacker.getDamageTaken();
        damageService.applyDamage(attacker, sceneContext, descriptor, source, damage, false);
        int dealt = attacker.getDamageTaken() - before;
        if (dealt > 0 && conditionOnDamage != null) {
            attacker.applyCondition(new Condition(conditionOnDamage, conditionRounds, source, conditionEffects));
        }
        return dealt;
    }
}
