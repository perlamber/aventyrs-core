package org.aventyrs.core.feat;

import org.aventyrs.core.race.Bestial;

/**
 * Talentos Bestiais — seven <b>Heranças</b>, each grafting one animal lineage onto the holder,
 * plus two Aventyr-tier Talentos that draw on however many Heranças are held.
 *
 * <p><b>No constant in this tree carries a mechanical effect</b>, and that is not seven separate
 * accidents: every Herança is built from the same four clauses, and all four are blocked.
 *
 * <ul>
 *   <li><b>"+1 de bônus racial em &lt;Atributo&gt;"</b> — a Talento cannot grant an Atributo
 *   bonus. {@code Race#getFixedAttributeBonuses()} is the only racial-bonus hook and it belongs
 *   to the Race; nothing reads a {@code Feat} for an {@code AttributeValue}, and there is no
 *   single service that resolves an Attribute total for a hook to plug into — 27 call sites read
 *   {@code getTotal()} directly. Six of the seven Heranças open with this clause.</li>
 *   <li><b>An Arma Natural</b> (Chifres Poderosos, Presas Longas, Garras Afiadas) — no weapon
 *   catalog is authored, and nothing marks a weapon as natural. CLAUDE.md's "Classifying an
 *   attack as Desarmado/Arma Natural" row.</li>
 *   <li><b>A Movimento Base de Natação / Voo / Vertical</b> — these are a <i>different sub-stat</i>
 *   from ordinary Movimento Base, deliberately not wired into {@code ModifierType#MOVEMENT} (see
 *   that type's own note, and {@code AtletismoCompetencyAbility#ALPINISTA_VELOZ}/{@code ANFIBIO}).
 *   Routing them there would raise the holder's ground movement, which no Herança says.</li>
 *   <li><b>"Recebem uma Habilidade de Competência de &lt;Perícia&gt;"</b> — the "grant an extra
 *   acquisition slot" gap, the single most-cited blocker of the racial catalog.</li>
 * </ul>
 *
 * <p>What the tree <i>does</i> deliver is a fully enforced prerequisite ladder: {@link
 * #ACEITAR_A_LACERTO} counts three held Heranças through {@code
 * FeatRequirements#requiredFeatCategory}, so the tree's own internal progression is real even
 * where its effects are not.
 */
public enum BestialFeat implements Feat {

    /**
     * "+1 de bônus racial em Vigor e Movimento Base de Natação… Vantagem para resistir a ataques
     * de agarrar, se livrar de cordas e outros objetos, ou lugares apertados."
     */
    // TODO: Atributo bonus, swim movement and the free Habilidade de Competência are all blocked
    //  — see the class javadoc for each.
    // TODO: the Vantagem is scoped to resisting grapples, ropes and confinement — none of which
    //  is a manoeuvre this core represents, so there is no roll to apply it to. Distinct from a
    //  merely purpose-scoped Vantagem: here the *action* is missing, not just its classification.
    // TODO: breathing underwater has no state to toggle — nothing tracks breathing (CLAUDE.md's
    //  "Fadiga/asfixia" row).
    HERANCA_ANFIBIA(
            "Receba +1 de bônus racial em Vigor e Movimento Base de Natação. Personagens com este "
                    + "talento podem respirar dentro e fora d'agua normalmente e possuem pele "
                    + "escorregadia, sem pelos, que lhes garantem Vantagem para resistir a ataques "
                    + "de agarrar, se livrar de cordas e outros objetos, ou lugares apertados, que "
                    + "te prendam. Bestiais Anfíbios recebem uma Habilidade de Competência de "
                    + "Furtividade.",
            FeatRequirements.builder()
                    .requiredRace(Bestial.class)
                    .build()),

    /**
     * "+1 de bônus racial em Foco. Você agora tem asas e possui Movimento Base de Voo."
     */
    // TODO: Atributo bonus and the free Habilidade de Competência are blocked — see the class
    //  javadoc. Flight additionally needs the flight state Aviano's own Braços Alados records,
    //  plus a "spend a resource to enter a timed state" transaction for its 2PA+3PD cost.
    // TODO: "+1 Rodada para cada Talento de Herança que possuir" is countable today (feats of
    //  this category), but there is no Duração to extend.
    HERANCA_AVIANA(
            "Receba +1 de bônus racial em Foco. Você agora tem asas e possui Movimento Base de "
                    + "Voo. Em Cenas de Combate voar exige o uso de 2PA e 3PD, a capacidade de voo "
                    + "poderá ser usada por 1d6 Rodadas, +1 Rodada para cada Talento de Herança "
                    + "que possuir. Encerrada a duração retornam ao solo lentamente, de forma "
                    + "segura. Bestiais Avianos recebem uma Habilidade de Competência de Persuasão.",
            FeatRequirements.builder()
                    .requiredRace(Bestial.class)
                    .build()),

    /** "+1 de bônus racial no atributo Força e Arma Natural Chifres Poderosos." */
    // TODO: Atributo bonus, Arma Natural and the free Habilidade de Competência are all blocked
    //  — see the class javadoc.
    HERANCA_BOVIDEA(
            "Você recebe +1 de bônus racial no atributo Força e Arma Natural Chifres Poderosos. "
                    + "Bestiais Bovídeos recebem uma Habilidade de Competência de Atletismo.",
            FeatRequirements.builder()
                    .requiredRace(Bestial.class)
                    .build()),

    /**
     * "+1 de bônus racial em Instinto, Arma Natural Presas Longas e Faro Apurado – Vantagem em
     * rolagens de Atenção para perceber ameaças a partir do olfato."
     */
    // TODO: Atributo bonus, Arma Natural and the free Especialização/Habilidade are all blocked
    //  — see the class javadoc.
    // TODO: Faro Apurado is a Vantagem on Atenção scoped to a narrative purpose ("a partir do
    //  olfato"), which CLAUDE.md records as unmodellable — this core never tracks what a roll
    //  is for, so Feat#resolveSkillRollBonus (which is real) has nothing to test. Granting it on
    //  every Atenção roll would be wider than the clause. Every flat roll bonus in this core is
    //  a @Modifier method on an ability, and Talentos are outside every ModifierResolver scan.
    //  Note the scoping alone would not have blocked it: AvianosRacialAbility
    //  #VISAO_ALEM_DO_ALCANCE grants its own purpose-scoped Atenção Vantagem broadly with the
    //  simplification documented, following DirigirECavalgarCompetencyAbility#CONTROLAR_ANIMAIS.
    //  It is the missing hook that blocks this one.
    HERANCA_CANINA(
            "Você recebe +1 de bônus racial em Instinto, Arma Natural Presas Longas e Faro "
                    + "Apurado – Vantagem em rolagens de Atenção para perceber ameaças a partir do "
                    + "olfato. Bestiais Caninos recebem uma Especialização e uma Habilidade de "
                    + "Competência de 'Atenção'.",
            FeatRequirements.builder()
                    .requiredRace(Bestial.class)
                    .build()),

    /**
     * "+1 de bônus racial no atributo Gnose e a habilidade Ecolocalização – Você pode gastar 2PD
     * para reduzir a GD de testes de Atenção em -1 nível."
     */
    // TODO: Atributo bonus and the free Especialização are blocked — see the class javadoc.
    // TODO: Ecolocalização's GD reduction is *bought with a resource*, which is exactly the shape
    //  Feat#resolveDifficultyReduction excludes — that hook is for an unconditional reduction, and
    //  nothing converts a PD spend into a per-roll effect. Contrast GnomoFeat#FAVORITOS_DE_TESLA,
    //  which is unconditional and therefore real.
    // TODO: mapping the terrain in Distância Média needs geometry this core never does.
    HERANCA_CETACEA(
            "Você recebe +1 de bônus racial no atributo Gnose e a habilidade Ecolocalização – "
                    + "Você pode gastar 2PD para reduzir a GD de testes de Atenção em -1 nível, "
                    + "sempre que o fizer você mapeia todo o terreno (sem precisão de detalhes) em "
                    + "Distância Média. Bestiais com este talento recebem uma Especialização "
                    + "adicional de 'Persuasão' e podem permanecer submersos, sem respirar, pelo "
                    + "dobro do tempo.",
            FeatRequirements.builder()
                    .requiredRace(Bestial.class)
                    .build()),

    /** "+1 de bônus racial em Destreza, Visão no Escuro e Arma Natural Garras Afiadas." */
    // TODO: Atributo bonus, Arma Natural and the free Habilidade de Competência are all blocked
    //  — see the class javadoc. Visão no Escuro additionally needs a vision/senses concept.
    HERANCA_FELINA(
            "Receba +1 de bônus racial em Destreza, Visão no Escuro e Arma Natural Garras "
                    + "Afiadas. Bestiais Felinos recebem uma Habilidade de Competência adicional "
                    + "de 'Furtividade'.",
            FeatRequirements.builder()
                    .requiredRace(Bestial.class)
                    .build()),

    /** "+1 de bônus racial em Carisma e Movimento Base Vertical." */
    // TODO: Atributo bonus, vertical movement and the free Especialização are all blocked — see
    //  the class javadoc.
    HERANCA_REPTILIANA(
            "Receba +1 de bônus racial em Carisma e Movimento Base Vertical. Bestiais Reptilianos "
                    + "recebem uma Especialização adicional de 'Atletismo'.",
            FeatRequirements.builder()
                    .requiredRace(Bestial.class)
                    .build()),

    /**
     * "A partir da terceira Rodada de Cenas de Combate… você pode gastar temporariamente 1 ponto
     * de Autocontrole e Mimetizar os Efeitos da Ferocidade de Lacerto por Instinto Rodadas."
     *
     * <p>Its Pré-requisito — three or more Talentos Bestiais — is real and enforced, so the tree's
     * own ladder works even though nothing it climbs to does.
     */
    // TODO: Ferocidade de Lacerto is unbuilt — Indomito's own javadoc records why it is withheld
    //  rather than approximated, and this Talento mimics it, so it inherits every blocker.
    // TODO: the Autocontrole spend is expressible (EgoPointsService#useEgoPointsForEffect), but
    //  there is no effect for it to buy, and no per-Cena activation counter for "apenas uma vez a
    //  cada Cena de Combate" — CharacterSheet counts Rodadas via TemporaryEffect, not activations.
    // TODO: "+1d6 pontos de dano adicionais" to Armas Naturais needs both the Arma Natural
    //  concept and a die, neither of which exists here.
    ACEITAR_A_LACERTO(
            "A partir da terceira Rodada de Cenas de Combate, como uma Ação Livre, você pode "
                    + "gastar temporariamente 1 ponto de Autocontrole e Mimetizar os Efeitos da "
                    + "Ferocidade de Lacerto (Característica da Raça Indômito) por Instinto "
                    + "Rodadas. Enquanto estiver Mimetizando a Ferocidade de Lacerto suas Armas "
                    + "Naturais causam +1d6 pontos de dano adicionais. Este efeito pode ser "
                    + "ativado apenas uma vez a cada Cena de Combate.",
            FeatRequirements.builder()
                    .requiredFeatCategory(FeatCategory.BESTIAL)
                    .requiredFeatCategoryCount(3)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você pode gastar 2PD para se transformar em um animal que pertence a uma categoria de
     * Talento Bestial que você possua."
     */
    // TODO: needs a form state — the same missing piece HomemFera's Forma Híbrida and
    //  DraconicoFeat#DRACONATO are blocked on. Note both of this Talento's numeric grants (+2UD
    //  Movimento, +2 Defesas) would be expressible unconditionally through existing hooks; they
    //  are withheld because they apply *only while transformed*, and granting them flat would
    //  hand a permanent bonus to a character who never transforms.
    // TODO: "é impossível usar equipamentos enquanto ativo" needs an equipment *restriction*
    //  mechanism — Character#equip validates nothing. Same gap DraconicoFeat#ASAS_DE_DRAGAO's
    //  Capa clause cites.
    METAMORFOSE_SELVAGEM(
            "Você pode gastar 2PD para se transformar em um animal que pertence a uma categoria "
                    + "de Talento Bestial que você possua. Enquanto transformado em animal seu "
                    + "movimento Base aumenta em +2UD, você recebe Bônus de +2 nas Defesas e "
                    + "Vantagem em suas rolagens Ataque e Dano com armas naturais. É impossível "
                    + "usar equipamentos enquanto este Talento estiver ativo.",
            FeatRequirements.builder()
                    .requiredRace(Bestial.class)
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    BestialFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.BESTIAL;
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
