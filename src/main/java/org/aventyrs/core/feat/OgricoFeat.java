package org.aventyrs.core.feat;

import org.aventyrs.core.race.Ogro;

/**
 * Talentos Ôgricos — every one of them an extension of <b>Bocarra</b>, the Ogro's swallow-whole
 * Característica Racial.
 *
 * <p>All four are catalog entries with enforced Pré-requisitos and no mechanical effect, blocked
 * on the single densest gap of any race in this core, already recorded on {@code Ogro} itself:
 * Devorar Inteiro needs a Corrente de Efeitos this core does not have, a containment relation
 * ("a creature is inside another creature") nothing models, a per-Rodada damage effect owned by
 * the <i>swallower</i> rather than the victim, and a Vigor-denominated stomach capacity with
 * nowhere to live.
 *
 * <p>That makes this tree unusually coherent in its uselessness: three of the four adjust
 * capacity, digestion time or Categoria de Tamanho limits — all figures of the missing system —
 * and the fourth attaches Roubo de Vida to attacks that cannot be made. None grants a partial
 * effect, because every partial would be a number applied to nothing.
 */
public enum OgricoFeat implements Feat {

    /**
     * "Você pode engolir personagens de até uma Categoria de Tamanho superior à sua." Raises the
     * Bocarra size ceiling, and restates the Vigor cost of each victim.
     */
    // TODO: Bocarra itself does not exist — see Ogro's own javadoc. This Talento only widens a
    //  limit ("apenas personagens de Categorias de Tamanho Inferior ao Ogro podem ser alvo") of
    //  a Corrente de Efeitos that is entirely unbuilt.
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
    // TODO: Roubo de Vida is real (LifeStealService), and the amount is computable — but it is
    //  scoped to "ataques com a Bocarra" and to damage dealt by Devorar Inteiro, neither of
    //  which exists. Granting it unscoped would attach Roubo de Vida to every attack the Ogro
    //  makes, which the clause plainly does not say.
    // TODO: "personagens com zero ou menos PV em seu interior morrem instantaneamente" is an
    //  outright kill conditioned on being swallowed — the CriticalEffectType#EXECUCAO_REAL
    //  machinery exists, but nothing can express the containment condition.
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
    // TODO: a Vigor uplift scoped to one calculation that does not exist. Note this must NOT be
    //  modelled as an Atributo bonus of any kind — it is explicitly *only* for stomach capacity,
    //  and a real +2 Vigor would inflate PV, Determinação and every Vigor-governed roll.
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
    // TODO: both halves adjust figures of the unbuilt Devorar Inteiro — stomach capacity and
    //  digestion time. Digestion time additionally has no clock: this core tracks Rodadas within
    //  a Scene, and the rules text denominates digestion in *hours*.
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
