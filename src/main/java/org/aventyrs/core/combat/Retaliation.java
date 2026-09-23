package org.aventyrs.core.combat;

import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.sheet.ConditionType;

/**
 * Damage an attacker takes back for having attacked, reported on {@link
 * DeliveredAttackResult#getRetaliation()} / {@link IncomingAttackResult#getRetaliation()} —
 * {@code AbracadoPelaEscuridaoAbility#ESPINHOS_VENENOS_DE_GAEA} is the one source today.
 *
 * <p><b>Reported, never dealt.</b> This core computes damage only ever <em>to</em> a target
 * <em>from</em> an attacker; nothing sends it the other way, which is exactly the gap the
 * "Reactive/retaliation damage" catalogue row names. What is real here is the whole calculation —
 * who takes it, how much, of what type, and what Malefício rides along — leaving the caller to
 * apply it with an ordinary {@code DamageService#applyDamage} against the attacker's own sheet.
 * That is the same division of labour {@code Teleportation} and {@code Scene#refreshAura} use.
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
 */
public record Retaliation(int damage,
                          DamageDescriptor descriptor,
                          ConditionType conditionOnDamage,
                          int conditionRounds) {
}
