package org.aventyrs.core.feat;

import org.aventyrs.core.race.Furia;

/**
 * Talentos das Fúrias — a tree of exactly one, the mirror of {@code
 * FadasFeat#ASPECTO_DA_BONDADE_NATURAL} and blocked on precisely the same missing piece.
 */
public enum FuriasFeat implements Feat {

    /**
     * "Conjurar Magias Profanas e Encantamentos em personagens inimigos ou neutros custam 1PM ou
     * 1PD à menos (mínimo 1 Unidade)."
     */
    // TODO: identical blockers to its Fada twin — no cost step in SpellCastingService to reduce,
    //  and no caster-to-target allegiance at cast time. Note "Profanas" is not a MagicType this
    //  core carries at all, unlike its twin's "Divinas e Naturais", so this one has an extra
    //  missing classification even once the cost step exists.
    ASPECTO_DA_DECOMPOSICAO_NATURAL(
            "Conjurar Magias Profanas e Encantamentos em personagens inimigos ou neutros custam "
                    + "1PM ou 1PD à menos (mínimo 1 Unidade).",
            FeatRequirements.builder()
                    .requiredRace(Furia.class)
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    FuriasFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.FURIAS;
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
