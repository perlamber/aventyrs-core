package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.SkillType;

public enum ArtesMarciaisFeat implements Feat {

    /**
     * "O Dano Base de seus Ataques Desarmados e Armas Naturais aumenta em +1, o Dano Base
     * cumulativamente aumenta em +1 para cada Título Aventyr Desperto que você possui." A
     * Título Aventyr is "Desperto" simply by being held — see {@code
     * InstinctAbility#SENTIR_A_INTENCAO}'s own confirmed reading of this exact phrase, {@link
     * Character#getAllTitles()} non-empty/its size is the count.
     *
     * <p>The Título-count half of this bonus is real, computable arithmetic — see {@link
     * #resolveDamageBaseIncrease(Character)} — and now genuinely consumed: {@code
     * DamageBaseService} sums it into the wielded item's own {@link
     * org.aventyrs.core.character.DamageBase}. What's still missing is the <em>gate</em>: this
     * core has no way to classify whether a given attack is an Ataque Desarmado/Arma Natural
     * versus a weapon attack (an {@code Item} catalog exists, but nothing marks an attack as
     * unarmed beyond a caller passing no weapon at all), so the bonus currently applies to any
     * attack its holder makes rather than only to those two.
     */
    ARTISTA_MARCIAL(
            "O Dano Base de seus Ataques Desarmados e Armas Naturais aumenta em +1, o Dano Base "
                    + "cumulativamente aumenta em +1 para cada Título Aventyr Desperto que você possui.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.STRENGTH)
                    .requiredAttributeValue(2)
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(2)
                    .build()) {
        // TODO: filter for Ataques Desarmados e Armas Naturais — DamageBaseService passes the
        // wielded Item (null for unarmed), but "Arma Natural" has no marker on Item at all, so
        // gating on weapon == null would silently drop the Armas Naturais half of the clause.
        @Override
        public int resolveDamageBaseIncrease(final Character character) {
            return BASE_DAMAGE_BASE_INCREASE + character.getAllTitles().size();
        }
    };

    /** ARTISTA_MARCIAL's own flat "+1" before any Título Aventyr Desperto is counted. */
    private static final int BASE_DAMAGE_BASE_INCREASE = 1;

    private final String description;
    private final FeatRequirements featRequirements;

    ArtesMarciaisFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ARTE_MARCIAL;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return featRequirements;
    }
}
