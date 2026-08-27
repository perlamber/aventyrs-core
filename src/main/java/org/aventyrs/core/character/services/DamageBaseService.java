package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.skill.SkillType;

/**
 * Resolves the Dano Base an attack starts from — the wielded {@link Weapon}'s own {@link
 * DamageBase}, advanced by every "+N Dano Base" scale-up the wielding {@link Character}
 * brings to it.
 *
 * <h2>Two overloads, two inputs — not a cascading pair</h2>
 *
 * An attack is either made <em>with a weapon</em> or <em>with bare hands</em>, and each form
 * takes exactly what that case knows: {@link #getDamageBase(Character, Weapon)} reads both the
 * starting row and the attacking Perícia off the weapon itself ({@link Weapon#getSkillType()}),
 * while {@link #getDamageBase(Character, SkillType)} starts at {@link DamageBase#UNARMED} and
 * needs the Perícia named because there is no weapon to name it. Neither delegates to the
 * other, so this is <b>not</b> the cascading-overload convention (which is for genuinely
 * optional inputs) — it's the same "two different questions" split as {@code
 * ActionPointsService}'s {@code Character}/{@code CombatantSheet} pair.
 *
 * <p>The Perícia is no longer a caller-supplied argument alongside a weapon: pairing a machado
 * with Ataque à Distância was expressible and meaningless, and the weapon already knows. A
 * weapon usable with either Perícia would need a second column on {@code Weapon} and a
 * per-swing choice; no catalog entry needs one yet.
 *
 * <h2>This is the first of two stages, and it never sees the second</h2>
 *
 * Dano Base and dano <em>bonuses</em> are different mechanics that are resolved separately and
 * combined only by whoever finally assembles the hit. A scale-up buys a row of {@link
 * DamageBase}'s table (which may be worth a whole extra die); a {@code
 * org.aventyrs.core.character.DamageBonus} is a flat number added to an already-rolled total.
 * That's why a character can attack at a Dano Base of 1d6+0 and still land for 1d6+20 — see
 * {@link DamageBase}'s own javadoc. Nothing here sums the two, and nothing here rolls the dice
 * (same boundary as everywhere else in this core).
 *
 * <h2>Where the scale-ups come from</h2>
 *
 * Three sources, summed:
 * <ul>
 *   <li>every held {@code Feat}'s {@code Feat#resolveDamageBaseIncrease(Character)} (e.g.
 *   {@code ArtesMarciaisFeat#ARTISTA_MARCIAL});</li>
 *   <li>every held Habilidade de Competência's {@code
 *   SkillCompetencyAbility#resolveDamageBaseIncrease(SkillType, Character)} — scanned via
 *   {@code SkillCompetencyAbility#allFor} so racial abilities count, and deliberately
 *   <b>not</b> pre-filtered by the ability's own {@code getSkillType()} (see that hook's
 *   javadoc);</li>
 *   <li>the {@code SkillExcellency} tiers unlocked by <b>the attacking Perícia's own</b>
 *   Graduação (e.g. {@code AtaqueADistanciaExcellency#FOCADO}).</li>
 * </ul>
 *
 * <p>That last one is a deliberate departure from this codebase's usual three-source scan,
 * which walks every trained Perícia's tiers: a Dano Base increase granted by Ataque à
 * Distância's Excelência must not raise the Dano Base of a Corpo-a-Corpo swing. {@code
 * AttributeAbility}/{@code EgoAdvantage} are not scanned at all — no constant on either grants
 * Dano Base today, and neither carries the hook.
 */
public interface DamageBaseService {

    /**
     * The Dano Base of an Ataque Desarmado made with attackingSkill: the scale starts at
     * {@link DamageBase#UNARMED} (1d6+0) and every scale-up applies from there.
     *
     * <p>This is the one case where the Perícia has to be named, since there's no weapon to
     * read it off — an Ataque Desarmado is normally Corpo a Corpo, but {@code
     * ArtesMarciaisFeat#ARTISTA_MARCIAL}-style grants and Armas Naturais mean this core doesn't
     * assume which.
     */
    DamageBase getDamageBase(Character character, SkillType attackingSkill);

    /**
     * The Dano Base character deals swinging weapon: {@code weapon.getDamageBase()} advanced by
     * the summed scale-ups described on this interface, scoped to {@code
     * weapon.getSkillType()} — the Perícia that weapon is used with.
     *
     * <p>The parameter is a {@link Weapon}, not an {@link org.aventyrs.core.item.Item} — asking
     * a helmet what it hits for isn't a question this service declines, it's one the compiler
     * refuses to let a caller pose. See {@code Weapon}'s own javadoc. It is {@code @NonNull}:
     * an unarmed attack is the other overload, not a {@code null} here.
     *
     * <p>The weapon is passed explicitly rather than looked up in {@code
     * Character#getEquipment()}: a character may carry several, and only the caller knows which
     * one is swinging. Nothing checks that it's actually equipped — the same "possession is the
     * caller's business" restraint {@code Item#grantsFavorTo} already applies.
     *
     * <p>Never below {@link DamageBase#UNARMED}: the scale clamps at 0, so a hypothetical
     * negative grant can't take a character below their own bare hands.
     */
    DamageBase getDamageBase(Character character, Weapon weapon);
}
