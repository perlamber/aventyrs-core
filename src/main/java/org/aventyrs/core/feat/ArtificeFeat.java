package org.aventyrs.core.feat;

import org.aventyrs.core.skill.SkillType;

/**
 * Talentos de Artífice — the Regalia-crafting ladder. Every constant here is a catalog entry
 * with real, enforced Pré-requisitos and no mechanical effect, and that is not an oversight:
 * a Regalia is an item tier this core has no concept of, and crafting one needs the whole
 * production pipeline the gap catalog's "Owned/produced item copy" row describes.
 *
 * <p>Each constant's TODO names what specifically blocks it. They share three blockers —
 * no Regalia item tier, no crafting/production mechanic, and no Centelha sacrifice — so the
 * per-constant TODOs name only what is additional to those.
 */
public enum ArtificeFeat implements Feat {

    /**
     * "Após estudar incansavelmente a Regalia em sua posse, você agora pode criar Regalias
     * Menores." The roll it demands is a Profissão check at GD Inimaginável which no Habilidade,
     * Vantagem or Efeito may reduce except an Artífice one.
     */
    // TODO: needs a Regalia item tier and a production mechanic (gap catalog, "Owned/produced
    //  item copy") before any of this can apply.
    // TODO: "posse de uma Regalia" is an inventory precondition FeatRequirements cannot express
    //  — it names an Item, and the record models Attribute/Perícia/Talento/Título/Race only.
    // TODO: "não é possível reduzir GD desta rolagem, exceto Habilidades de Artífice" needs a
    //  per-roll suppression of getDifficultyReduction() scoped by the reducer's own source;
    //  AbstractSkillInteraction sums every source with no notion of which granted it.
    ARTESAO_DE_REGALIAS_MENOR(
            "Após estudar incansavelmente a Regalia em sua posse, você agora pode criar Regalias "
                    + "Menores. Criar uma Regalia Menor exige uma rolagem de Profissão "
                    + "(Especialização conforme o tipo de Regalia) na GD Inimaginável, não é "
                    + "possível reduzir GD desta rolagem com Habilidades, Vantagens ou Efeitos, "
                    + "exceto Habilidades de Artífice. Como custo adicional é necessário que um "
                    + "personagem sacrifique voluntariamente uma de suas Centelhas, despejando seu "
                    + "sangue sobre o equipamento. A Criação de uma Regalia Menor leva até 90 dias "
                    + "de trabalho.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.PROFISSAO)
                    .requiredSkillGraduation(7)
                    .build()),

    /**
     * "Você se tornou capaz de criar Regalias Superiores." GD Milagre, and the Centelha cost
     * rises from one to all of the donor's.
     */
    // TODO: same three blockers as ARTESAO_DE_REGALIAS_MENOR.
    // TODO: "ter sido bem-sucedido na criação de 3 ou mais Regalias Menores" is a count of past
    //  successful crafts — this core records no history of anything a character has done.
    ARTESAO_DE_REGALIAS_SUPERIORES(
            "Após estudar Regalias Superiores e ter sido bem-sucedido na criação de 3 ou mais "
                    + "Regalias Menores, você se tornou capaz de criar Regalias Superiores. Criar "
                    + "uma Regalia Superior exige uma rolagem de Profissão na GD Milagre. Como "
                    + "custo adicional é necessário que um personagem sacrifique voluntariamente "
                    + "todas as suas Centelhas. A Criação leva até 145 dias de trabalho.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.PROFISSAO)
                    .requiredSkillGraduation(10)
                    .requiredFeat(ARTESAO_DE_REGALIAS_MENOR)
                    .build()),

    /**
     * "Você dominou a arte da criação de Regalias e agora pode criar Regalias Divinas." The
     * donor must be a Dragão, Elemental, Abissal or Celestial, and the roll must land an Acerto
     * Crítico outright.
     */
    // TODO: same three blockers as ARTESAO_DE_REGALIAS_MENOR, plus the craft-history count.
    // TODO: the donor clause needs creature classifications (Dragão/Abissal/Celestial) that
    //  CreatureType does not carry — it has only HUMANOIDE/FEERICO/MONSTRUOSO.
    // TODO: "deve ser feita exclusivamente em uma Forja do Olho de Deus" is a location
    //  precondition; this core models no places.
    ARTESAO_DE_REGALIAS_DIVINAS(
            "Você dominou a arte da criação de Regalias e agora pode criar Regalias Divinas. "
                    + "Criar uma Regalia Divina exige uma rolagem de Profissão na GD Milagre que "
                    + "tenha por resultado obrigatório um Acerto Crítico. Como custo adicional é "
                    + "necessário que um Dragão, Elemental, Abissal ou Celestial sacrifique "
                    + "voluntariamente suas Centelhas. A Criação leva até 180 dias de trabalho e "
                    + "deve ser feita exclusivamente em uma Forja do Olho de Deus.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.PROFISSAO)
                    .requiredSkillGraduation(10)
                    .requiredFeat(ARTESAO_DE_REGALIAS_SUPERIORES)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    ArtificeFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ARTIFICE;
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
