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
 * <h2>An attack's reach starts with what it was made with, and the body wielding it</h2>
 *
 * A weapon states its Alcance ({@link Weapon#getRange()} — {@link Range#ADJACENTE} for the
 * corpo-a-corpo default), and a Magia states its reach ({@code spell.getTargeting().range()},
 * non-null only for a {@code DISTANCIA} or a placed-centre {@code AREA_DE_EFEITO}). A Talento such
 * as {@code ArtilhariaFeat#TIRO_LONGO} only <em>shifts</em> that authored band; it is meaningless
 * without a source to shift.
 *
 * <p><b>The attacker's Categoria de Tamanho is the one character-level contribution</b>, per the
 * Alcance column of {@code docs/rules/categorias-de-tamanho.txt}: a larger body reaches further
 * with whatever it holds. See {@link #getEffectiveRangeInUnidadesDeDistancia} for how it composes
 * with the weapon's own Alcance, and why {@link Range#ADJACENTE} is the case that substitutes
 * rather than adds. Magias take no such widening — a Conjurador's reach is not their own body's.
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
     * The {@link Range} band character's attack with weapon falls into — {@link
     * #getEffectiveRangeInUnidadesDeDistancia} resolved to a band, then advanced by the summed
     * Talento steps described on this interface. Never past {@link Range#AO_ALCANCE_DOS_OLHOS}.
     *
     * <p><b>This is a rounded-<em>up</em> summary, not the reach.</b> {@link Range} is a geometric
     * ladder (1/2/4/8/16/24 UD) while a size modifier is linear, so most reaches are not
     * expressible as a band: an adaga wielded at Categoria +4 reaches 3 UD — Muito Curta plus one —
     * and {@link Range#fromUnidadesDeDistancia} can only answer {@link Range#DISTANCIA_CURTA},
     * which is worth 4.
     *
     * <p>That is exactly right for the question a band asks. {@link Range#isWithin} still answers
     * correctly, because the band returned is the smallest one that <em>covers</em> the reach: 3 UD
     * maps to CURTA, and {@code CURTA.isWithin(DISTANCIA_MUITO_CURTA)} is properly {@code false}.
     * It becomes wrong only if a caller reads {@link Range#getMaxUnidadesDeDistancia()} off the
     * result and treats it as a distance — <b>a caller measuring anything wants {@link
     * #getEffectiveRangeInUnidadesDeDistancia} instead</b>.
     */
    Range getEffectiveRange(Character character, Weapon weapon);

    /**
     * How far character actually reaches with weapon, in Unidades de Distância — the figure a
     * caller measuring against a position needs, and the one {@link #getEffectiveRange} rounds up
     * into a band.
     *
     * <p>Two cases, because {@link Range#ADJACENTE} is not a distance. It means there is no space
     * between the combatants at all, so nothing is added to it:
     * <ul>
     *   <li>a weapon that states <b>no reach of its own</b> ({@code getEffectiveRange() ==
     *   ADJACENTE} — a fist, a dagger, a sword) reaches the attacker's own {@code
     *   SizeCategory#getRange()}, the Alcance column of the Categorias de Tamanho table. A bigger
     *   creature reaches further because of its body, not because adjacency grew;</li>
     *   <li>a weapon that <b>does</b> state one (a Lança at Muito Curta, an arco at Longa) has
     *   {@code SizeCategory#getRangeModifier()} added to that stated distance, floored at {@code
     *   SizeCategory#MINIMUM_MELEE_RANGE}. This applies to <b>every</b> such weapon — Arremesso and
     *   Ataque à Distância included; a giant draws a longbow further than a goblin does.</li>
     * </ul>
     *
     * <p>The split is what keeps a giant's lança out-reaching its own fist. Modelling the first
     * case as a substitution rather than as "add to 1 UD" costs nothing numerically today — the
     * Alcance column <em>is</em> {@code max(1, 1 + modifier)} — and stays correct if the authored
     * column ever stops following that formula.
     *
     * <p><b>Talento steps are not included here</b>, and cannot be: {@code
     * Feat#resolveAttackRangeIncrease} returns whole band steps ("+1 nível de distância"), never a
     * UD count, so they can only be applied to the band form. This figure is the authored Alcance
     * plus Size, nothing else. Adding a UD-valued Talento hook needs a rules source that states
     * one.
     *
     * <p>{@link #UNBOUNDED_RANGE} for a weapon whose Alcance is {@link
     * Range#AO_ALCANCE_DOS_OLHOS}, which names no fixed distance to add to.
     */
    int getEffectiveRangeInUnidadesDeDistancia(Character character, Weapon weapon);

    /** What {@link #getEffectiveRangeInUnidadesDeDistancia} reports for a reach limited only by
     * sight — {@link Range#AO_ALCANCE_DOS_OLHOS} has no {@code maxUnidadesDeDistancia}, so there is
     * no number to state and none to add a modifier to. */
    int UNBOUNDED_RANGE = Integer.MAX_VALUE;

    /**
     * The maximum {@link Range} character reaches casting spell at a target — the Magia's own
     * {@code getTargeting().range()} advanced by the same summed Talento steps — or {@link
     * Optional#empty()} for a reach that names no placed distance ({@code PESSOAL}, {@code
     * TOQUE}, {@code PLANAR}, or an {@code AREA_DE_EFEITO} centred on the Conjurador).
     */
    Optional<Range> getEffectiveRange(Character character, Spell spell);
}
