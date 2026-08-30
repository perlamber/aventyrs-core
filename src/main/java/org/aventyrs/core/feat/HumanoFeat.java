package org.aventyrs.core.feat;

import org.aventyrs.core.race.Human;

/**
 * Talentos Humanos — all three about the Humano's defining Característica, <b>Aprendizado
 * Rápido</b>, and all three blocked on it.
 *
 * <p>{@code Human}'s own javadoc records why it is unbuilt, and nothing here changes that:
 * {@code SkillGraduationService#getUpgradeCost} takes no {@code Race} and has no notion of a
 * per-race discount, and nothing records <i>which</i> Perícias a character chose at creation for
 * the discount to scope itself to. Two of these Talentos extend how far that discount reaches
 * and the third replaces it outright, so none has anything to act on.
 *
 * <p><b>The mutual exclusion between {@link #APRENDIZADO_RAPIDO_E_CONTINUO} and {@link
 * #LIMIAR_DA_EVOLUCAO} is not enforced.</b> "Um mesmo personagem não pode adquirir" both is an
 * exclusion clause, and {@code FeatRequirements} has no negative form — every clause it carries
 * is a threshold that must be met, never one that must not. Seven Talentos across the catalog
 * share this, and it is recorded in {@code docs/rules/talentos-index.md} rather than worked
 * around; the gate is looser than the text, never stricter.
 */
public enum HumanoFeat implements Feat {

    /**
     * "Sua Habilidade Aprendizado Rápido te beneficia até a quinta Graduação em Perícia, após
     * Despertar um Título Aventyr você estende os benefícios até a sétima."
     */
    // TODO: Aprendizado Rápido is unbuilt — see the class javadoc. This Talento only widens the
    //  Graduação range the discount covers, so there is nothing to widen.
    // TODO: the second-Título clause grants an Especialização or Habilidade de Competência per
    //  benefited Perícia — the "grant an extra acquisition slot" gap, here in bulk and with a
    //  per-Perícia choice between two kinds of slot.
    APRENDIZADO_RAPIDO_E_CONTINUO(
            "Um Exemplar entre os Humanos, sua Habilidade Aprendizado Rápido te beneficia até a "
                    + "quinta Graduação em Perícia, após Despertar um Título Aventyr você estende "
                    + "os benefícios de Aprendizado até a sétima Graduação. Após Despertar seu "
                    + "segundo Título Aventyr você recebe uma Especialização ou Habilidade de "
                    + "Competência de cada Perícia beneficiada por Aprendizado Rápido (os "
                    + "benefícios podem ser escolhidos separadamente para cada Perícia). Um mesmo "
                    + "personagem não pode adquirir Aprendizado Rápido e Contínuo e Limiar da "
                    + "Evolução.",
            FeatRequirements.builder()
                    .requiredRace(Human.class)
                    .build()),

    /**
     * "Você adquire novas Habilidades de Competência ou Especializações de Perícias com 3, 5, 7 e
     * 10 Graduações, ao invés de 4, 7 e 10 Graduações."
     */
    // TODO: this rewrites the *schedule* on which acquisition slots unlock, which is a stronger
    //  ask than the usual "grant an extra slot" gap — SkillExcellency#unlockedBy resolves tiers
    //  from fixed Graduação thresholds declared on each <Skill>Excellency constant, and nothing
    //  can shift those per character. Note it also adds a fourth rung (3/5/7/10 against 4/7/10),
    //  so it is not a pure re-indexing.
    ENTENDER_OS_FUNDAMENTOS(
            "Você adquire novas Habilidades de Competência ou Especializações de Perícias com 3, "
                    + "5, 7 e 10 Graduações, ao invés de 4, 7 e 10 Graduações.",
            FeatRequirements.builder()
                    .requiredRace(Human.class)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Escolha um Atributo, você recebe Bônus Racial de +1 no Atributo Escolhido. Ao Despertar
     * seu segundo Título Aventyr você recebe uma Habilidade do Atributo escolhido."
     */
    // TODO: a Talento cannot grant an Atributo bonus — same gap as
    //  AnaoFeat#CONSELHEIRO_DE_GUERRA_YMIRIANO and TrollFeat#VIGOR_TROLLICO. This one is harder
    //  still: *which* Atributo is a per-acquisition choice, and a Feat is a flat catalog constant
    //  carrying no such data. The two shapes that could hold it both exist — an instance-based
    //  class (ArtesAprimorarComArteAbility's pattern) or one constant per AttributeDomain
    //  (PeritoTeoricoAbility's) — but neither fits an enum whose whole tree is one enum class.
    // TODO: the second-Título clause is the "grant an extra acquisition slot" gap, restricted to
    //  the chosen Atributo.
    LIMIAR_DA_EVOLUCAO(
            "Juntamente ao seu Título Aventyr você desperta o sangue dos Primeiros Homens em seu "
                    + "corpo. Escolha uma Atributo, você recebe Bônus Racial de +1 no Atributo "
                    + "Escolhido. Ao Despertar seu segundo Título Aventyr você recebe uma "
                    + "Habilidade do Atributo escolhido. Um mesmo personagem não pode adquirir "
                    + "Aprendizado Rápido e Contínuo e Limiar da Evolução.",
            FeatRequirements.builder()
                    .requiredRace(Human.class)
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    HumanoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.HUMANO;
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
