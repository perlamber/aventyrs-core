package org.aventyrs.core.feat;

import org.aventyrs.core.race.Goblin;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos Goblins — both about improvised engineering, and both blocked on the same two gaps:
 * <b>nothing models a produced copy</b> (Dureza is now a real, spendable pool — {@code
 * Item#applyDamage}/{@code getCurrentHardness} — but these clauses set it at <i>creation</i>, and
 * no production mechanic exists to set it at) and the GD reduction they grant is
 * <b>purpose-scoped</b>.
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
    //  Feat#resolveDifficultyReduction where FAVORITOS_DE_TESLA qualifies. EquipmentCraftingService
    //  now has a fabrication GD to reduce, but no Feat hook feeds a step into it.
    // TODO: the Dureza penalty needs a per-forge hook. Fabrication produces a copy now
    //  (EquipmentCraftingService#forge, which already scales Dureza via
    //  SkillCompetencyAbility#resolveProducedHardnessMultiplier for AUMENTAR_A_DUREZA), but there
    //  is no Feat-side equivalent, and forge() takes no "this Talento was used" marker to gate one.
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
    // TODO: same purpose-scoped GD gap and same missing production mechanic as its prerequisite.
    // TODO: the explosion needs three further things — a trigger fired when an item is destroyed
    //  (Item#isDestroyed is a derived state nothing observes; this codebase has no observer
    //  mechanism anywhere), Área de Efeito – Explosão resolution (nothing turns a footprint into a
    //  set of targets), and a die roll this core never makes. Note "itens tecnológicos" is also a
    //  classification Item does not carry: ItemCategory names shape, not technology.
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
