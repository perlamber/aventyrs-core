package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.race.Indomito;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.function.Predicate;

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
    // Real: LacertoFerocityService refuses to enter the Ferocidade while any ally in Distância
    // Curta still has 1 or more PV — "vivos e conscientes" follows from that, since a character at
    // 0 PV or below is Caído, in Coma or dead. It only prevents entering: an Indômito already
    // ferocious is not pulled out when an ally arrives.
    RENEGAR_A_LACERTO(
            "Você não entra na Ferocidade de Lacerto enquanto tiver aliados vivos e conscientes, "
                    + "com 1 ou mais PV, em Distância Curta.",
            FeatRequirements.builder()
                    .requiredRace(Indomito.class)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public boolean preventsEnteringLacertoFerocity(final Character holder, final SceneContext holderContext,
                                                       final Predicate<CombatantSheet> standing) {
            return holderContext != null
                    && holderContext.getAlliesWithin(Range.DISTANCIA_CURTA).stream().anyMatch(standing);
        }
    };

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
