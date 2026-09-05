package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.race.HomemFera;

/**
 * Talentos Ferais — the Homem-Fera's tree, split cleanly in two: three Talentos that graft a
 * physical trait on, and three Aventyr-tier ones that reshape <b>Forma Híbrida</b>'s economy.
 *
 * <p>No constant carries a mechanical effect, and the two halves fail for different reasons. The
 * first three each open with "+1 Bônus Racial em &lt;Atributo&gt;", which a Talento cannot grant
 * — see {@code BestialFeat}'s class javadoc for why that clause is systemically blocked. The last
 * three adjust the Custo, Tempo de Ativação and Duração of Forma Híbrida, which {@code HomemFera}
 * records as unbuilt: there is no form state, and no "spend a resource to enter a timed state"
 * transaction for these Talentos to make cheaper or longer.
 *
 * <p>The Pré-requisito ladder <i>is</i> real throughout, and is this tree's most interesting
 * structural feature: {@link #TRANSFORMACAO_RAPIDA} and {@link #TRANSFORMACAO_DURADOURA} count
 * two and three held Talentos Ferais respectively through {@code
 * FeatRequirements#requiredFeatCategory}, and {@link #ASPECTO_DAS_FERAS} names the latter
 * directly.
 *
 * <p><b>The mutual exclusion between the two Transformação Talentos is not enforced</b> — "não
 * pode ser usado em conjunto" is an exclusion clause, and {@code FeatRequirements} carries only
 * thresholds that must be met, never one that must not. Same shape as {@code HumanoFeat}'s own
 * pair, and recorded in {@code docs/rules/talentos-index.md}.
 */
public enum FeralFeat implements Feat {

    /**
     * "Você recebe Bônus Racial de +1 em Destreza e pode usar suas Presas Longas para empunhar
     * armas e manipular objetos como se fosse uma de suas mãos."
     */
    // TODO: a Talento cannot grant an Atributo bonus — see BestialFeat's class javadoc.
    // TODO: using Presas Longas as a hand needs both the Arma Natural concept and the limb
    //  concept Aviano's Braços Alados/Pés Hábeis pair is blocked on. With neither modelled there
    //  is no manual limitation for this to lift.
    PRESAS_COM_DESTREZA_MANUAL(
            "Você recebe Bônus Racial de +1 em Destreza e pode usar suas Presas Longas para "
                    + "empunhar armas e manipular objetos como se fosse uma de suas mãos.",
            FeatRequirements.builder()
                    .requiredRace(HomemFera.class)
                    .attributeDomain(AttributeDomain.DEXTERITY)
                    .requiredAttributeValue(3)
                    .build()),

    /**
     * "Você recebe Bônus Racial de +1 em Vigor e adquire a Habilidade Racial Regeneração Reativa
     * (Trolls)."
     */
    // TODO: a Talento cannot grant an Atributo bonus — see BestialFeat's class javadoc.
    // TODO: Regeneração Reativa is unbuilt — nothing triggers off taking damage, which is the
    //  healing counterpart of CLAUDE.md's "Reactive/retaliation damage" row. Note this Talento
    //  grants the Troll version at 1PV per Rodada rather than the 2PV Troll's own Característica
    //  states; the weaker figure is transcribed as written.
    BENCAO_DE_MAPINGUARI(
            "Você recebe Bônus Racial de +1 em Vigor e adquire a Habilidade Racial Regeneração "
                    + "Reativa (Trolls): após sofrer danos de fontes inimigas recupera 1PV por "
                    + "Rodada (em seus Turnos) por uma quantidade de Rodadas igual ao próprio "
                    + "Vigor. A quantidade de PV recuperados desta forma não pode superar os danos "
                    + "sofridos (Efeito não cumulativo).",
            FeatRequirements.builder()
                    .requiredRace(HomemFera.class)
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(3)
                    .build()),

    /**
     * "Você recebe Bônus Racial de +1 em Força, o Tipo de Dano base de suas Armas Naturais mudam
     * para Físico Elemental: Natural… e recebem a Corrente de Efeitos – Ferida Infecciosa."
     */
    // TODO: a Talento cannot grant an Atributo bonus — see BestialFeat's class javadoc.
    // TODO: re-typing an attack's dano is not expressible — DamageType is a classification a
    //  caller supplies per hit, and nothing lets a held trait override what an attack deals.
    //  Same gap OrquicoFeat#PALADINO_DE_EPONA cites, here compounded by there being no Arma
    //  Natural to re-type in the first place.
    // TODO: Corrente de Efeitos is an unbuilt system, and "Ferida Infecciosa" is not among the
    //  shared 13-entry catalog EffectChainService resolves.
    DESPREZO_NATURAL(
            "Você recebe Bônus Racial de +1 em Força, o Tipo de Dano base de suas Armas Naturais "
                    + "mudam para Físico Elemental: Natural em substituição aos seus tipos e "
                    + "recebem a Corrente de Efeitos – Ferida Infecciosa.",
            FeatRequirements.builder()
                    .requiredRace(HomemFera.class)
                    .attributeDomain(AttributeDomain.STRENGTH)
                    .requiredAttributeValue(3)
                    .build()),

    /**
     * "O Custo e Tempo de Ativação de sua Habilidade Racial Forma Híbrida é reduzido em -1
     * unidade, mas a Duração é reduzida em -2 Rodadas."
     */
    // TODO: Forma Híbrida is unbuilt — see HomemFera's own javadoc. All three figures this
    //  Talento adjusts (3PM Custo, 3PA Tempo de Ativação, the Instinto-scaled Duração) are exact
    //  authored data on a Característica with no mechanism behind it.
    TRANSFORMACAO_RAPIDA(
            "O Custo e Tempo de Ativação de sua Habilidade Racial Forma Híbrida é reduzido em -1 "
                    + "unidade, mas a Duração é reduzida em -2 Rodadas. Este Talento não pode ser "
                    + "usado em conjunto com Transformação Duradoura.",
            FeatRequirements.builder()
                    .requiredFeatCategory(FeatCategory.FERAL)
                    .requiredFeatCategoryCount(2)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você pode aumentar o Custo de suas Transformações em +2PM para estender a Duração até o
     * final da Cena."
     */
    // TODO: Forma Híbrida is unbuilt. This one additionally needs a Cena boundary — this core
    //  tracks Rodadas within a Scene and has no "until the end of the Cena" duration — and a
    //  time-of-day concept for the "durante a noite" clause, the same one HomemFera's own
    //  Duração multipliers wait on.
    TRANSFORMACAO_DURADOURA(
            "Você pode aumentar o Custo de suas Transformações em +2PM para estender a Duração "
                    + "até o final da Cena. Ativar este efeito durante a noite faz com que a "
                    + "Duração mude para Quantidade de Títulos Desperto Horas. Este Talento não "
                    + "pode ser usado em conjunto com Transformação Rápida.",
            FeatRequirements.builder()
                    .requiredFeatCategory(FeatCategory.FERAL)
                    .requiredFeatCategoryCount(3)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Suas transformações não possuem mais Duração, mas mantê-las ativas em local iluminado pelo
     * sol exige um Custo de 2PM a cada Instinto Horas."
     */
    // TODO: Forma Híbrida is unbuilt, so removing its Duração removes nothing.
    // TODO: the upkeep clause needs three things this core lacks — an hours-denominated clock
    //  (Rodadas are the only unit), a daylight/time-of-day concept, and a recurring cost that
    //  drains PM outside anyone's Turn.
    ASPECTO_DAS_FERAS(
            "Suas transformações não possuem mais Duração, mas mantê-las ativas em local "
                    + "iluminado pelo sol exige um Custo de 2PM a cada Instinto Horas.",
            FeatRequirements.builder()
                    .requiredFeat(TRANSFORMACAO_DURADOURA)
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    FeralFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.FERAL;
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
