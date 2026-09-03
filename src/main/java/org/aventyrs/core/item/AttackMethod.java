package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.skill.AttackSource;

/**
 * What an attack is delivered <em>with</em>, at the granularity the rules text names when a
 * Talento says "escolha um tipo de arma" — the {@link ItemCategory} constants that are actually
 * weapons, plus {@link #NATURAL_WEAPON} and {@link #OFFENSIVE_MAGIC}. Existing catalogs already
 * carry every distinction the rules draw here (Arco/Arremesso/Balestra/Chicote/Clava/Lâmina
 * Leve/Lâmina Pesada/Lança/Projéteis, from {@code docs/rules/equipamentos-index.md}); this is
 * that same list read as "what was this attack made with" rather than "what kind of Item is
 * this".
 *
 * <p>The acquisition-time-choice Talentos that name a "tipo de arma, armas naturais ou magias
 * ofensivas" — {@code DuelistaFeat#ESPECIALISTA_EM_ARMA}, {@code ArtilhariaFeat#ATIRADOR_PERFEITO},
 * {@code AssassinoFeat#ACERTO_CRITICO_APRIMORADO} — record their choice as one constant here, the
 * same way {@code PeritoFeat#FOCO_EM_PERICIA} records a {@link org.aventyrs.core.skill.SkillType}.
 */
public enum AttackMethod {

    BOW(ItemCategory.BOW),
    THROWABLE(ItemCategory.THROWABLE),
    CROSSBOW(ItemCategory.CROSSBOW),
    WHIP(ItemCategory.WHIP),
    CLUB(ItemCategory.CLUB),
    LIGHT_BLADE(ItemCategory.LIGHT_BLADE),
    HEAVY_BLADE(ItemCategory.HEAVY_BLADE),
    SPEAR(ItemCategory.SPEAR),
    PROJECTILE(ItemCategory.PROJECTILE),

    /** Armas Naturais — matched via {@link Character#treatsAsNaturalWeapon}, not a raw category
     * test, so a Talento that reclassifies a weapon as natural (e.g. {@code
     * ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA}) is seen here too. */
    NATURAL_WEAPON(ItemCategory.NATURAL_WEAPON),

    /** Magias Ofensivas — any {@link Spell}, since this core has no offensive/utility split on
     * Magias yet (see the magic-system skill). */
    OFFENSIVE_MAGIC(null);

    private final ItemCategory category;

    AttackMethod(final ItemCategory category) {
        this.category = category;
    }

    /**
     * Whether source (a wielded {@link Weapon} or cast {@link Spell}, or {@code null} when the
     * caller didn't say) was delivered by this method. {@code null} always reads as "no match" —
     * the same convention {@code AttackSource}-narrowing hooks use throughout this core.
     */
    public boolean matches(final AttackSource source, final Character character) {
        if (this == OFFENSIVE_MAGIC) {
            return source instanceof Spell;
        }
        if (!(source instanceof Weapon weapon)) {
            return false;
        }
        return this == NATURAL_WEAPON
                ? character != null && character.treatsAsNaturalWeapon(weapon)
                : weapon.getCategory() == category;
    }
}
