package org.aventyrs.core.feat;

import org.aventyrs.core.race.Goblin;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos Goblins — both about improvised engineering, and both blocked on the same two gaps:
 * <b>Dureza has no consumer</b> (an {@code Item}'s column is real, exact data, but no damage or
 * repair mechanic reads it) and the GD reduction they grant is <b>purpose-scoped</b>.
 *
 * <p>That second point is worth reading alongside {@code GnomoFeat#FAVORITOS_DE_TESLA}, which
 * grants a −1 nível on the same Perícia and <i>is</i> real. The difference is the whole reason
 * {@link Feat#resolveDifficultyReduction}'s javadoc restricts itself: Favoritos de Tesla eases
 * every Profissão roll, while these two ease only rolls "para criar equipamento" — and this core
 * deliberately does not track what a roll is <i>for</i>. Granting them unscoped would ease a
 * Goblin's every Profissão roll, which the clause does not say.
 */
public enum GoblinFeat implements Feat {

    /**
     * "O GD de rolagens de Profissão para criar equipamento é reduzida em -1 Nível. Itens criados
     * utilizando efeitos deste Talento tem a Dureza reduzida à metade."
     */
    // TODO: the GD reduction is scoped to a narrative purpose ("para criar equipamento"), which
    //  this core does not track — see the class javadoc for why this disqualifies it from
    //  Feat#resolveDifficultyReduction where FAVORITOS_DE_TESLA qualifies.
    // TODO: the Dureza penalty needs both a consumer for Item#getHardness (there is none — no
    //  damage or repair mechanic) and the missing owned-item copy, since it is a property of the
    //  particular item produced rather than of the catalog entry.
    ENGENHEIRO_DE_IMPROVISOS(
            "O GD de rolagens de Profissão para criar equipamento é reduzida em -1 Nível. Itens "
                    + "criados utilizando efeitos deste Talento tem a Dureza reduzida à metade.",
            FeatRequirements.builder()
                    .requiredRace(Goblin.class)
                    .requiredSkillType(SkillType.CONHECIMENTOS)
                    .requiredSkillGraduation(5)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você pode substituir os Engenheiro de Improvisos e reduzir a GD da rolagem de Profissão em
     * -2 Níveis… Itens tecnológicos criados desta forma explodem quando sua Dureza chega à zero."
     */
    // TODO: same purpose-scoped GD gap and same missing Dureza consumer as its prerequisite.
    // TODO: the explosion needs three further things — an item whose Dureza can reach zero
    //  (missing owned-item copy), Área de Efeito – Explosão resolution (nothing turns a footprint
    //  into a set of targets), and a die roll this core never makes. Note "itens tecnológicos" is
    //  also a classification Item does not carry: ItemCategory names shape, not technology.
    KABUM(
            "Você pode substituir os Engenheiro de Improvisos e reduzir a GD da rolagem de "
                    + "Profissão em -2 Níveis (ao invés de -1 Nível), se o fizer a Dureza do "
                    + "Equipamento é reduzida para 5PV (ou 1/3, o que for menor). Itens "
                    + "tecnológicos criados desta forma explodem quando sua Dureza chega à zero, "
                    + "causando 1d6 Pontos de Dano Físico Elemental: Fogo para cada Título "
                    + "Aventyr que você possuir, Área de Efeito – Explosão.",
            FeatRequirements.builder()
                    .requiredFeat(ENGENHEIRO_DE_IMPROVISOS)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    GoblinFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.GOBLIN;
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
