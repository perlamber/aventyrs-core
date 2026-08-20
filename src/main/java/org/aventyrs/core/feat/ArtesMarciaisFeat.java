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
     * #getDanoBaseBonus(Character)} — but this codebase has no Item/Equipamento entity (see
     * CLAUDE.md's "Item/Equipamento" gap citation) and therefore no way to classify whether a
     * given attack is actually an Ataque Desarmado/Arma Natural versus a weapon attack, so
     * there's nothing yet to gate {@code getDanoBaseBonus} on beyond the character simply
     * holding this Feat — no attack-resolution engine exists to call it automatically either
     * way, the same "no caller yet" shape as {@code ArtesAprimorarComArteAbility
     * #getBaseDamageBonus(SkillType)}.
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
        //TODO filter for Ataques Desarmados e Armas Naturais
                        @Override
        public int getDanoBaseBonus(final Character character) {
            return BASE_DANO_BONUS + character.getAllTitles().size();
        }
    };

    /** ARTISTA_MARCIAL's own flat "+1" before any Título Aventyr Desperto is counted. */
    private static final int BASE_DANO_BONUS = 1;

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

    /**
     * How much this Feat adds to Dano Base right now — zero by default; only overridden on a
     * constant whose rules text grants one. See {@link #ARTISTA_MARCIAL}'s own javadoc for why
     * this can be computed for real even though nothing calls it yet.
     */
    public int getDanoBaseBonus(final Character character) {
        return 0;
    }
}
