package org.aventyrs.core.feat;

import org.aventyrs.core.race.Fada;

/**
 * Talentos das Fadas — a tree of exactly one, the Fada's half of a matched pair with {@code
 * FuriasFeat#ASPECTO_DA_DECOMPOSICAO_NATURAL}. The two are mirror images: one cheapens Magias
 * Divinas e Naturais cast on allies, the other Magias Profanas e Encantamentos cast on enemies.
 */
public enum FadasFeat implements Feat {

    /**
     * "Conjurar Magias Divinas e Naturais em personagens aliados ou neutros custam 1PM ou 1PD à
     * menos (mínimo 1 Unidade)."
     */
    // TODO: there is no cost step to reduce. SpellCastingService#castSpell rolls the delivery
    //  Perícia and Domínio do Mana and spends nothing at all — Spell#getManaCost is authored data
    //  with no consumer — so the discount, its floor and its "PM ou PD" choice all have nothing
    //  to apply to. The same missing cost step GuamposRacialAbility's Benção Divina and
    //  ElementalFeat#ARCANISMO_ELEMENTAL both cite.
    // TODO: scoping by the *target's* allegiance ("aliados ou neutros") needs a caster-to-target
    //  relation at cast time, which castSpell does not carry — and this core's only notion of
    //  allegiance is Scene's binary sub-group split, with no "neutro" third state.
    // TODO: "Divinas e Naturais" is a MagicType scope that does exist, so this half would follow
    //  for free once the cost step did.
    ASPECTO_DA_BONDADE_NATURAL(
            "Conjurar Magias Divinas e Naturais em personagens aliados ou neutros custam 1PM ou "
                    + "1PD à menos (mínimo 1 Unidade).",
            FeatRequirements.builder()
                    .requiredRace(Fada.class)
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    FadasFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.FADAS;
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
