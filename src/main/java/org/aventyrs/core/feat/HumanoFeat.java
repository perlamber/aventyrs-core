package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.race.Human;

import java.util.List;
import java.util.function.Supplier;

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
 * #LIMIAR_DA_EVOLUCAO} is enforced.</b> "Um mesmo personagem não pode adquirir" both is an
 * exclusion clause, which {@code FeatRequirements#forbiddenFeats} now carries — each half naming
 * its twin. That mutual naming is why this enum holds its requirements as a {@link Supplier}
 * (see that field), and why the forward half goes through {@link #limiarDaEvolucao()}.
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
            () -> FeatRequirements.builder()
                    .requiredRace(Human.class)
                    .forbiddenFeat(limiarDaEvolucao())
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
            () -> FeatRequirements.builder()
                    .requiredRace(Human.class)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Escolha um Atributo, você recebe Bônus Racial de +1 no Atributo Escolhido. Ao Despertar
     * seu segundo Título Aventyr você recebe uma Habilidade do Atributo escolhido."
     */
    // The Atributo half is real, through AtributoRacialEscolhidoFeat (any Atributo).
    // TODO: the free Habilidade on Despertar a second Título. Withholding it until then is
    //  derivable (Character#getAllTitles().size() >= 2 inside getGrantedAttributeAbilities), but
    //  *which* Habilidade is a second pick made at that moment, not at acquisition, and nothing
    //  records a later pick on an already-granted Talento. Feat#resolveSessionEndAcquisitions is
    //  the closest shape, but it owes a fixed acquisition, not a choice.
    LIMIAR_DA_EVOLUCAO(
            "Juntamente ao seu Título Aventyr você desperta o sangue dos Primeiros Homens em seu "
                    + "corpo. Escolha uma Atributo, você recebe Bônus Racial de +1 no Atributo "
                    + "Escolhido. Ao Despertar seu segundo Título Aventyr você recebe uma "
                    + "Habilidade do Atributo escolhido. Um mesmo personagem não pode adquirir "
                    + "Aprendizado Rápido e Contínuo e Limiar da Evolução.",
            () -> FeatRequirements.builder()
                    .requiredRace(Human.class)
                    .forbiddenFeat(APRENDIZADO_RAPIDO_E_CONTINUO)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(FeatChoice.ofOne(AttributeDomain.class, List.of(AttributeDomain.values())));
        }
    };

    /**
     * {@link #LIMIAR_DA_EVOLUCAO}, reached through a method rather than named directly: Java
     * forbids referencing a <em>later</em> enum constant from an earlier constant's constructor
     * arguments, and a {@link Supplier} does not lift that — the restriction is on the reference,
     * not on when it is evaluated. A static method body is not an initializer, so the forward
     * reference is legal here. Only the forward half of the pair needs one.
     */
    private static Feat limiarDaEvolucao() {
        return LIMIAR_DA_EVOLUCAO;
    }

    private final String description;
    /**
     * Held as a {@link Supplier} rather than a plain field because this tree's mutually-exclusive
     * Talentos name each <em>other</em> as a {@code forbiddenFeat}, and Java forbids referencing
     * an enum constant from another constant's constructor arguments. Deferring construction to
     * the first {@link #getFeatRequirements()} call sidesteps that, the same way {@code
     * MetamagicoFeat} already does for its own sibling {@code requiredFeat} chain.
     */
    private final Supplier<FeatRequirements> featRequirements;

    HumanoFeat(final String description, final Supplier<FeatRequirements> featRequirements) {
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
        return featRequirements.get();
    }
}
