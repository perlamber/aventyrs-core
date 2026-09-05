package org.aventyrs.core.item;

import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillType;

/**
 * An {@link Item} that can actually be swung — the only kind that has a Dano Base at all, and
 * (alongside {@code org.aventyrs.core.magic.Spell}) one of the two things an attack can be made
 * with, hence {@link AttackSource}.
 *
 * <p>This exists so {@link #getDamageBase()} lives where it's meaningful instead of on every
 * {@code Item}. A pauldron has no Dano Base column, and a signature typed to {@code Item}
 * could only answer that question with a stand-in (1d6+0, indistinguishable from a real
 * dagger's). Typed to {@code Weapon}, the question can't be asked of a pauldron in the first
 * place: {@code DamageBaseService#getDamageBase} takes this type, so <b>the compiler refuses a
 * non-weapon</b> rather than a runtime guard rejecting one. Same enforcement-by-type discipline
 * as {@code CharacterSheet} vs {@code MonsterSheet} — there is deliberately no {@code
 * isWeapon()} flag and no check anywhere.
 *
 * <p>Everything else about a weapon is an ordinary {@code Item}: its Preço, Dureza, Raridade,
 * {@link ItemFavor} and even its DF/DM all behave identically and need no override. Only these
 * two columns are new, and both are declared abstract rather than defaulted — a type whose
 * whole reason to exist is having a Dano Base should have to say what it is, and what it takes
 * to swing.
 *
 * <p>Nothing validates that an implementation's {@link Item#getCategory()} is actually an
 * {@link ItemType#OFFENSIVE} one. That's the usual restraint: catalogs and builders in this
 * codebase are data holders, not gatekeepers.
 */
public interface Weapon extends Item, AttackSource {

    /**
     * The Dano Base this weapon deals — the starting row of {@link DamageBase}'s scale that its
     * wielder's own "+N Dano Base" grants are applied on top of, by {@code
     * org.aventyrs.core.character.services.DamageBaseService}.
     */
    DamageBase getDamageBase();

    /**
     * The Dano Base this weapon <em>currently</em> delivers — {@link #getDamageBase()} until it is
     * destroyed, and {@link DamageBase#UNARMED} from then on. A shattered sword grants nothing, so
     * swinging the wreck is an Ataque Desarmado: the bottom rung of the scale, and literally what
     * a bare fist deals. This is the form {@code DamageBaseService} reads; {@link
     * #getDamageBase()} stays the authored column, so a repair mechanism would restore it
     * untouched.
     */
    default DamageBase getEffectiveDamageBase() {
        return isDestroyed() ? DamageBase.UNARMED : getDamageBase();
    }

    /**
     * The Perícia that governs this weapon's use — Ataque Corpo a Corpo for a machado, Ataque à
     * Distância for an arco. This is a column of the weapon itself, not a per-swing decision:
     * it's what {@code DamageBaseService#getDamageBase(Character, Weapon)} scans by, selecting
     * which {@link org.aventyrs.core.skill.SkillExcellency} tiers (and which Perícia-scoped
     * Habilidade de Competência grants) can raise this weapon's Dano Base.
     *
     * <p>One weapon, one Perícia. A weapon whose rules text lets it be used with either — a
     * lança thrown rather than thrust — can't say so here; that would need a second column and
     * a per-swing choice, and no catalog entry needs it yet.
     */
    SkillType getSkillType();

    /**
     * A weapon <em>is</em> an {@link AttackSource}, and the Perícia it presents there is the same
     * {@link #getSkillType()} column above — so this delegates rather than adding a second,
     * separately-authorable answer that could disagree with the first. Don't override it.
     */
    @Override
    default SkillType getAttackSkillType() {
        return getSkillType();
    }

    /**
     * This weapon's authored Alcance — the maximum {@link Range} it reaches before any
     * range-extending Talento or ability. <b>Defaults to {@link Range#ADJACENTE}</b>: a
     * corpo-a-corpo weapon, which is most of the catalog, and the one band a weapon that never
     * says otherwise should present. A weapon de Ataque à Distância or de Arremesso authors its
     * own — {@code AbstractWeapon} carries it as a {@code @Builder.Default} column.
     *
     * <p>Unlike {@link #getDamageBase()}/{@link #getSkillType()} this is <em>not</em> abstract:
     * it has a sane universal default and the great majority of call sites don't consult it, so
     * forcing every builder to state it would be noise. {@code
     * org.aventyrs.core.character.services.AttackRangeService} is what reads it.
     */
    default Range getRange() {
        return Range.ADJACENTE;
    }

    /**
     * Whether this weapon can be knocked out of its wielder's hands — true unless a fitted
     * Obra-Prima or Aprimoramento says otherwise ("Não pode ser desarmado"). Consulted by {@code
     * CombatantSheet#disarm(Weapon)}, which refuses rather than silently dropping the weapon.
     *
     * <p>On {@code Weapon} rather than {@code Item} because the question is only meaningful for
     * something held to fight with: a helmet is not disarmable, it is simply worn, and asking
     * would invite a stand-in answer. Same enforcement-by-type reasoning as {@link
     * #getDamageBase()}.
     *
     * <p>Not gated on {@link Item#isDestroyed()} — a shattered sword is still gripped, and
     * knocking the wreck away is exactly as possible as knocking away a whole one.
     */
    default boolean isDisarmable() {
        boolean masterpiecePrevents = getMasterpiece() != null && getMasterpiece().preventsDisarming();
        boolean improvementPrevents = getImprovements().stream().anyMatch(Improvement::preventsDisarming);
        return !masterpiecePrevents && !improvementPrevents;
    }

    /**
     * The Alcance this weapon <em>currently</em> reaches — {@link #getRange()} until it is
     * destroyed, and {@link Range#ADJACENTE} from then on. A shattered bow carries no further
     * than a bare fist, exactly as {@link #getEffectiveDamageBase()} drops such a weapon to
     * {@link DamageBase#UNARMED}. This is the form {@code AttackRangeService} reads; {@link
     * #getRange()} stays the authored column, so a repair mechanism would restore it untouched.
     */
    default Range getEffectiveRange() {
        return isDestroyed() ? Range.ADJACENTE : getRange();
    }
}
