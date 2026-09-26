package org.aventyrs.core.monster;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.model.MonstrousAbility;

/**
 * A held {@link MonstrousAbility}, resolved at its holder's Categoria and dressed as an {@link
 * AttributeAbility} — the adapter that lets a Habilidade Monstruosa reach every aggregated stat
 * with no service knowing monsters exist.
 *
 * <p>{@code ActionPointsService}, {@code InitiativeService}, {@code MovementService}, {@code
 * ReactionsService}, {@code FreeActionsService}, {@code DefenseService} and the three pool services
 * all already sum {@code @Modifier}s across {@code Character#getAttributeAbilities()}. A {@code
 * @Modifier} method takes no arguments, so the Categoria and the choice are captured here, per
 * instance — the pattern {@code ZumbiAbility} uses for its tiers — and each method delegates to
 * {@link MonstrousAbility#resolveModifier}. {@code ModifierResolverImpl} indexes declared methods
 * only, which is why they live on this concrete class and not on an interface.
 *
 * <p>{@link #hitPoints()} also carries the flat "+2PV" every Habilidade Monstruosa grants its
 * holder ("Para cada Habilidade Monstruosa que possuir ele recebe Bônus de +2PV") — including the
 * catalog-only ones, whose effects are otherwise not applied.
 *
 * <p>A {@link ModifierType} missing from the list below is one no implemented Habilidade grants
 * yet; add its method when one does.
 */
@Getter
@EqualsAndHashCode
public class MonstrousAbilityGrant implements AttributeAbility {

    /** "Para cada Habilidade Monstruosa que possuir ele recebe Bônus de +2PV". */
    public static final int HIT_POINTS_PER_ABILITY = 2;

    @NonNull
    private final MonstrousAbility ability;

    @NonNull
    private final MonsterCategory category;

    private final String choice;

    public MonstrousAbilityGrant(@NonNull final MonstrousAbility ability, @NonNull final MonsterCategory category,
                                 final String choice) {
        this.ability = ability;
        this.category = category;
        this.choice = choice;
    }

    private int resolve(final ModifierType type) {
        return ability.resolveModifier(type, category, choice);
    }

    @Modifier(ModifierType.HIT_POINTS)
    int hitPoints() {
        return HIT_POINTS_PER_ABILITY + resolve(ModifierType.HIT_POINTS);
    }

    @Modifier(ModifierType.LIFE_MULTIPLIER)
    int lifeMultiplier() {
        return resolve(ModifierType.LIFE_MULTIPLIER);
    }

    @Modifier(ModifierType.MANA_MULTIPLIER)
    int manaMultiplier() {
        return resolve(ModifierType.MANA_MULTIPLIER);
    }

    @Modifier(ModifierType.DETERMINATION_MULTIPLIER)
    int determinationMultiplier() {
        return resolve(ModifierType.DETERMINATION_MULTIPLIER);
    }

    @Modifier(ModifierType.ACTION_POINTS)
    int actionPoints() {
        return resolve(ModifierType.ACTION_POINTS);
    }

    @Modifier(ModifierType.REACTIONS)
    int reactions() {
        return resolve(ModifierType.REACTIONS);
    }

    @Modifier(ModifierType.FREE_ACTIONS)
    int freeActions() {
        return resolve(ModifierType.FREE_ACTIONS);
    }

    @Modifier(ModifierType.INITIATIVE)
    int initiative() {
        return resolve(ModifierType.INITIATIVE);
    }

    @Modifier(ModifierType.MOVEMENT)
    int movement() {
        return resolve(ModifierType.MOVEMENT);
    }

    @Modifier(ModifierType.DEFESAS)
    int defenses() {
        return resolve(ModifierType.DEFESAS);
    }

    @Modifier(ModifierType.PHYSICAL_DEFENSE)
    int physicalDefense() {
        return resolve(ModifierType.PHYSICAL_DEFENSE);
    }

    @Modifier(ModifierType.MAGIC_DEFENSE)
    int magicDefense() {
        return resolve(ModifierType.MAGIC_DEFENSE);
    }

    @Modifier(ModifierType.DAMAGE_REDUCTION)
    int damageReduction() {
        return resolve(ModifierType.DAMAGE_REDUCTION);
    }

    @Modifier(ModifierType.MAGIC_REDUCTION)
    int magicReduction() {
        return resolve(ModifierType.MAGIC_REDUCTION);
    }

    @Modifier(ModifierType.CRITICAL_RESISTANCE)
    int criticalResistance() {
        return resolve(ModifierType.CRITICAL_RESISTANCE);
    }

    /**
     * {@code null}: a Habilidade Monstruosa belongs to no Atributo. The domain is read only where a
     * character <i>acquires</i> an Habilidade de Atributo (slot counting, {@code
     * AttributeAbilityService}), which a monster never goes through.
     */
    @Override
    public AttributeDomain getAttributeDomain() {
        return null;
    }

    @Override
    public String getDescription() {
        return ability.getDescription();
    }
}
