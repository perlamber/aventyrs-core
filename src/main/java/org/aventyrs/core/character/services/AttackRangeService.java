package org.aventyrs.core.character.services;

import java.util.Optional;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.scene.Range;

/**
 * Resolves the maximum {@link Range} an attack can reach — the {@link Weapon}'s or {@link Spell}'s
 * own authored Alcance, advanced up the band ladder by every "+N níveis de distância" the
 * attacking {@link Character} brings to it.
 *
 * <h2>An attack's reach belongs to what it was made with, not to the character</h2>
 *
 * There is no character-level "range" stat and this service never invents one: a weapon states
 * its Alcance ({@link Weapon#getRange()} — {@link Range#ADJACENTE} for the corpo-a-corpo default),
 * and a Magia states its reach ({@code spell.getTargeting().range()}, non-null only for a {@code
 * DISTANCIA} or a placed-centre {@code AREA_DE_EFEITO}). A Talento such as {@code
 * ArtilhariaFeat#TIRO_LONGO} only <em>shifts</em> that authored band; it is meaningless without a
 * source to shift.
 *
 * <h2>Two overloads, two questions — not a cascading pair</h2>
 *
 * {@link #getEffectiveRange(Character, Weapon)} and {@link #getEffectiveRange(Character, Spell)}
 * take genuinely different inputs and neither delegates to the other — the same "two different
 * questions" split as {@code DamageBaseService}'s {@code Weapon}/{@code SkillType} pair, not the
 * cascading-overloads convention. The {@link Spell} form returns an {@link Optional} because a
 * {@code PESSOAL}/{@code TOQUE}/{@code PLANAR} or caster-centred Magia has no placed maximum range
 * to widen — nothing extends those, so the answer is "not applicable" rather than a band.
 *
 * <h2>Where the widening comes from — a Talento source, plus Size for melee</h2>
 *
 * Every held {@code Feat}'s {@code Feat#resolveAttackRangeIncrease(Character, AttackSource)}, passed
 * the weapon or Magia as the {@code AttackSource} so a clause can scope itself to how the attack is
 * delivered ({@code TIRO_LONGO} grants only to {@code ATAQUE_A_DISTANCIA}). Talentos sit outside
 * every {@code ModifierResolver} scan, so this is an explicit pass over {@code Character#getFeats()},
 * the same shape {@code MovementServiceImpl}/{@code DamageBaseServiceImpl} use.
 *
 * <p>A weapon whose authored Alcance is {@link Range#ADJACENTE} — corpo-a-corpo — is additionally
 * widened by the attacker's own {@code CharacterSizeService#getEffectiveSizeCategory}
 * ({@code SizeCategory#getRange()}, converted back to a band via
 * {@link Range#fromUnidadesDeDistancia}), the same size source {@code MovementServiceImpl} reads
 * for Movimento per Ponto de Ação — a maior creature reaches further with an unarmed or melee
 * strike. A weapon whose Alcance is already something other than ADJACENTE is left alone: this
 * models reach, not a general size-scaled bonus to every attack. Magias have no such widening —
 * a Conjurador's reach isn't their own body's.
 *
 * <p><b>No ability or equipment source yet</b>, each deliberately absent rather than forgotten:
 * <ul>
 *   <li>no {@code SkillCompetencyAbility}/{@code AttributeAbility} range hook — no constant on
 *   either states an unconditional "+N níveis de distância" clause today (the closest, {@code
 *   GorgonaFeat#MARCA_DA_MALDICAO}'s Olhar de Lacerto reach, is blocked on the ability itself
 *   being unbuilt), so the hook is added with its first real consumer;</li>
 *   <li>no equipment scan — the offensive Obra-Prima/Aprimoramento catalog doesn't exist, so
 *   "Alcance Estendido" has nothing to sum, and Arco Longo's "Alcance Base muda para Distância
 *   Muito Longa" Favor is a <em>replacement</em> with no {@code ModifierType} to carry it. When
 *   that lands this service grows an {@code Character#getEquipment()} pass, exactly as {@code
 *   DamageBaseServiceImpl} already has one for {@code resolveEnhancementDamageBaseIncrease}.</li>
 * </ul>
 *
 * <p>This core still never checks that an attack's target is actually within the range this
 * returns — {@code AttackDelivery} and {@code SpellCastingService#validateRequest} both leave
 * that to the caller. This service answers "how far can they reach", not "did this attack connect".
 */
public interface AttackRangeService {

    /**
     * The maximum {@link Range} character reaches swinging, firing or throwing weapon —
     * {@code weapon.getEffectiveRange()} (its authored Alcance, or {@link Range#ADJACENTE} once
     * the weapon is destroyed), widened for a corpo-a-corpo weapon by character's own
     * effective {@code SizeCategory}, then advanced by the summed Talento steps described on
     * this interface. Never past {@link Range#AO_ALCANCE_DOS_OLHOS}.
     */
    Range getEffectiveRange(Character character, Weapon weapon);

    /**
     * The maximum {@link Range} character reaches casting spell at a target — the Magia's own
     * {@code getTargeting().range()} advanced by the same summed Talento steps — or {@link
     * Optional#empty()} for a reach that names no placed distance ({@code PESSOAL}, {@code
     * TOQUE}, {@code PLANAR}, or an {@code AREA_DE_EFEITO} centred on the Conjurador).
     */
    Optional<Range> getEffectiveRange(Character character, Spell spell);
}
