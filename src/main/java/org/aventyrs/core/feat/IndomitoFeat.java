package org.aventyrs.core.feat;

import org.aventyrs.core.race.Indomito;

/**
 * Talentos Indômitos — a tree of exactly one.
 *
 * <p><b>Reconhecer suas Presas is deliberately not here.</b> Its header reads {@code
 * (Aventyr/Indômito/Gigante Enfurecido)}, but its Pré-requisito is an <b>or</b> — "o Título
 * Aventyr Gigante Enfurecido <i>ou</i> raça Indômito" — so the Indômito tag restricts nothing
 * and cannot be the tree. It is an {@code AVENTYR} Talento, and that tree is deferred. See
 * {@code docs/rules/talentos-index.md}'s scope decisions.
 */
public enum IndomitoFeat implements Feat {

    /**
     * "Você não entra na Ferocidade de Lacerto enquanto tiver aliados vivos e conscientes, com 1
     * ou mais PV, em Distância Curta."
     */
    // TODO: Ferocidade de Lacerto is unbuilt — Indomito's own javadoc records why it is withheld
    //  rather than approximated (it is a state that can be *declined*, and nothing tracks whether
    //  an Indômito is currently ferocious). A Talento that suppresses it has nothing to suppress.
    // TODO: the condition itself is very nearly expressible, which is worth recording for
    //  whoever builds the state: SceneContext#getAlliesWithin(Range.DISTANCIA_CURTA) gives the
    //  allies, and HitPointsService#getStatus would give "vivos e conscientes" — but that is a
    //  service an enum constant cannot reach, the same obstacle GoblinsRacialAbility
    //  #AUTODESCONFIANCA_EM_COMBATE hits for its own "ou inconscientes" clause.
    RENEGAR_A_LACERTO(
            "Você não entra na Ferocidade de Lacerto enquanto tiver aliados vivos e conscientes, "
                    + "com 1 ou mais PV, em Distância Curta.",
            FeatRequirements.builder()
                    .requiredRace(Indomito.class)
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    IndomitoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.INDOMITO;
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
