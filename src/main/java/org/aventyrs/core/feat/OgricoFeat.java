package org.aventyrs.core.feat;

import org.aventyrs.core.race.Ogro;

/**
 * Talentos Ôgricos — every one of them an extension of <b>Bocarra</b>, the Ogro's swallow-whole
 * Característica Racial.
 *
 * <p>All four are real through {@code character.services.DevourService}, which reads them by
 * catalog entry — each adjusts a figure only Bocarra has, so none needs a {@code Feat} hook of its
 * own.
 */
public enum OgricoFeat implements Feat {

    /**
     * "Você pode engolir personagens de até uma Categoria de Tamanho superior à sua." Raises the
     * Bocarra size ceiling, and restates the Vigor cost of each victim.
     */
    // Real: DevourService#getVigorOccupied lets the holder swallow its own Categoria (3 Vigor) and
    // one above (4 Vigor).
    MANDIBULA_DESARTICULADA(
            "Você pode engolir personagens de até uma Categoria de Tamanho superior à sua. "
                    + "Personagens da sua Categoria de Tamanho ocupam 3 Pontos de Vigor em seu "
                    + "estômago, personagens até uma Categoria superior ocupam 4 Pontos de Vigor.",
            FeatRequirements.builder()
                    .requiredRace(Ogro.class)
                    .build()),

    /**
     * "Seus ataques com a Bocarra recebem Roubo de Vida… Seu Roubo de Vida é igual a sua
     * quantidade de Título Aventyrs Despertos."
     */
    // Real: the Roubo de Vida (one per Título Desperto) is reported on DevourService's BocarraBite
    // for the bite, and recovered by DevourService#digest from each victim damaged. A victim at 0 PV
    // or below dies there, and the holder recovers PV equal to its Vigor.
    DEVORATRIZ(
            "Seus ataques com a Bocarra recebem Roubo de Vida, o Roubo de Vida também se aplica a "
                    + "alvos sofrendo danos pelos efeitos de Devorar Inteiro. Personagens com zero "
                    + "ou menos PV em seu interior morrem instantaneamente e você recupera uma "
                    + "quantidade de PV igual ao Vigor do alvo. Seu Roubo de Vida é igual a sua "
                    + "quantidade de Título Aventyrs Despertos.",
            FeatRequirements.builder()
                    .requiredRace(Ogro.class)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você é considerado como tendo Vigor +2 para calcular a quantidade de alvos que você pode
     * Engolir Inteiro."
     */
    // Real: +2 to DevourService#getStomachCapacity only — never an Atributo bonus, which would
    // inflate PV, Determinação and every Vigor-governed roll.
    DOIS_ESTOMAGOS(
            "Você é considerado como tendo Vigor +2 para calcular a quantidade de alvos que você "
                    + "pode Engolir Inteiro.",
            FeatRequirements.builder()
                    .requiredRace(Ogro.class)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Alvos engolidos ocupam 1 Ponto de Vigor a menos (mínimo 1 Ponto de Vigor), o tempo de sua
     * digestão é reduzido à metade."
     */
    // Real: DevourService#getVigorOccupied takes 1 off (minimum 1), and #getDigestionHours halves.
    // The hours are reported only — this core tracks Rodadas, not hours.
    PODEROSO_GLUTAO(
            "Alvos engolidos ocupam 1 Ponto de Vigor a menos (mínimo 1 Ponto de Vigor), o tempo "
                    + "de sua digestão é reduzido à metade.",
            FeatRequirements.builder()
                    .requiredRace(Ogro.class)
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    OgricoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.OGRICO;
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
