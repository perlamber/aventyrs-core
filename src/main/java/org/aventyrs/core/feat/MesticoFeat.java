package org.aventyrs.core.feat;

/**
 * Talentos Mestiços — a tree of exactly one, widening the Mestiço's inherited Características.
 *
 * <p>Its Pré-requisito is the one clause in the racial catalog {@code FeatRequirements} cannot
 * express at all. "Apenas personagens Mestiços" is a question about {@code Race#isMestico()}, a
 * boolean — but {@code requiredRace} is a {@code Class<? extends Race>} tested with {@code
 * isInstance}, and there is no common Mestiço supertype to name: {@code AbstractMesticoRace}
 * covers the six Mestiços Elementais, while {@code MeioElfo} and {@code NascidoDoDragao}
 * implement {@code Race} directly because they share none of that base's structure. A {@code
 * requiredMestico} flag would fix it and is deliberately not added — one Talento in ~305 needs
 * it, which is short of this codebase's "wait for the second real consumer" bar. The gate is
 * therefore looser than the text, the same direction every other unexpressible clause errs in.
 */
public enum MesticoFeat implements Feat {

    /**
     * "Você recebe uma Característica Racial adicional em Mestiço Humanoide ou Mestiço Mortal.
     * Esta Característica extra pode ser escolhida por você, sem a necessidade de seleções
     * aleatórias."
     *
     * <p><b>Half of this Talento is already true.</b> "Sem a necessidade de seleções aleatórias"
     * describes exactly how this core already works: a Mestiço's constructor accepts
     * already-externally-resolved Características because this library never rolls dice, so
     * nothing distinguishes a chosen inheritance from a random one. What is missing is only the
     * other half — the count.
     */
    // TODO: the count is fixed at construction. Both Mestiço shapes validate
    //  inheritedRacialAbilities against a MAX_INHERITED_RACIAL_ABILITIES of 2 in their
    //  constructor, and a Race instance is immutable thereafter — so a Talento acquired later
    //  has nothing to widen. Closing this needs the cap to be resolved against the holder rather
    //  than checked once, which would invert the current design: the Race would have to consult
    //  the Character, and it is the Character that holds the Race.
    // TODO: "recém-criados" is not modelled either — nothing records when a Talento was
    //  acquired. Same simplification as DraconicoFeat's own two creation-only Talentos.
    CARACTERISTICA_RACIAL_ADICIONAL(
            "Você recebe uma Característica Racial adicional em Mestiço Humanoide ou Mestiço "
                    + "Mortal. Esta Característica extra pode ser escolhida por você, sem a "
                    + "necessidade de seleções aleatórias.",
            FeatRequirements.builder().build());

    private final String description;
    private final FeatRequirements featRequirements;

    MesticoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.MESTICO;
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
