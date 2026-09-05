package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;

/**
 * Talentos de Destino — what a character is, rather than what they can do: how enemies read
 * them, which Habilidades they were born to, and when their Títulos Aventyr awaken.
 *
 * <p>Two blockers dominate. The first is <b>granting another trait</b>: five constants here hand
 * out a Habilidade de Atributo, a Vantagem de Ego, or another Talento outright. Those are all
 * acquisition-slot grants, which the gap catalog records as having no shape — and a Vantagem de
 * Ego in particular is chosen once at character creation and never awarded later.
 *
 * <p>The second is the <b>Despertar timeline</b>. Half the Aventyr-tier constants here delay,
 * accelerate, or forgo awakening a Título, and trade on how many remain un-awakened. This core
 * models a Título as simply held or not (see {@code FeatRequirements#requiredAwakenedTitles});
 * there is no un-awakened Título to count, no EXP threshold at which one awakens, and no game
 * session for "ao fim da primeira sessão" to name.
 */
public enum DestinoFeat implements Feat {

    /**
     * "Sua força de vontade permitiu desenvolver-se mais que a maioria, seu multiplicador de PD
     * aumenta em +1."
     *
     * <p><b>The multiplier half is real</b>, through {@link
     * Feat#resolveDeterminationMultiplierIncrease} — an unconditional, permanent uplift consumed
     * by {@code DeterminationPointsService}. That hook was added for this constant.
     *
     * <p>Not to be confused with {@code DuelistaFeat#CORACAO_DE_FERRO}: the catalog authors two
     * unrelated Talentos under this same name, in different trees.
     */
    // TODO: the Descanso recovery half needs a PD equivalent of resolveRestMagicPointsBonus —
    //  RestService recovers PD but scans no Talento hook for it. The figure itself is computable
    //  (2 + getAllTitles().size()).
    CORACAO_DE_FERRO(
            "Sua força de vontade permitiu desenvolver-se mais que a maioria, seu multiplicador de "
                    + "PD aumenta em +1. Sua recuperação de PD também aumenta, a cada Descanso "
                    + "você recupera +2PD, e então +1PD para cada Título Aventyr que tenha "
                    + "Desperto.",
            FeatRequirements.builder().build()) {
        @Override
        public int resolveDeterminationMultiplierIncrease(final Character character) {
            return 1;
        }
    },

    /**
     * "A menos que você seja o único alvo disponível você nunca será alvo primário de ataques nas
     * duas primeiras Rodadas de um combate."
     */
    // TODO: nothing chooses an attack's target — a caller does, and this core has no targeting
    //  step to refuse one (the same direction the gap catalog's "Forced attack targeting /
    //  interception" row records, from the other side).
    // TODO: "inimigos inteligentes" is a creature classification CreatureType does not carry.
    // TODO: "Força igual ou inferior à 2" is a *maximum*; FeatRequirements expresses only
    //  Attribute minimums, so only the Carisma half is enforced.
    APARENCIA_INOFENSIVA(
            "A menos que você seja o único alvo disponível, ou já tenha realizado ações ofensivas "
                    + "contra seus inimigos, você nunca será alvo primário de ataques, Magias ou "
                    + "Habilidades inimigas nas duas primeiras Rodadas de um combate. Este Talento "
                    + "afeta apenas inimigos inteligentes.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.CHARISMA)
                    .requiredAttributeValue(3)
                    .build()),

    /** As {@link #APARENCIA_INOFENSIVA}, extended to the first four Rodadas. */
    // TODO: same blockers as APARENCIA_INOFENSIVA, including the unenforceable Força maximum.
    APARENCIA_VERDADEIRAMENTE_INOFENSIVA(
            "A menos que você seja o único alvo disponível, ou já tenha realizado ações ofensivas "
                    + "contra seus inimigos, você nunca será alvo primário de ataques, Magias ou "
                    + "Habilidades inimigas nas quatro primeiras Rodadas de um combate.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.CHARISMA)
                    .requiredAttributeValue(5)
                    .requiredFeat(APARENCIA_INOFENSIVA)
                    .build()),

    /**
     * "Escolha um Ego que você possua valor 2 ou superior. Você adquire uma Vantagem do Ego
     * escolhido."
     */
    // TODO: a Vantagem de Ego is chosen once at character creation (CharacterCreationService,
    //  gated on EGO_ADVANTAGE_MIN_BASE) and there is no path to award one afterwards. Note this
    //  Talento would also bypass that threshold of 3, granting at 2.
    AUTOCONHECIMENTO(
            "Escolha um Ego que você possua valor 2 ou superior. Você adquire uma Vantagem do Ego "
                    + "escolhido.",
            FeatRequirements.builder().build()),

    /**
     * "Escolha um Atributo que você possua valor Base 2 ou superior. Você adquire uma Habilidade
     * do Atributo escolhido."
     */
    // TODO: an acquisition-slot grant — the gap catalog records that such traits have no shape.
    //  Character#grantAttributeAbility exists; the chosen Atributo/Habilidade could be recorded
    //  (a choice-carrying AbstractFeat subclass, see FocoEmPericiaFeat), but the *granting* of a
    //  free ability slot is the blocker.
    PRODIGIO(
            "Escolha um Atributo que você possua valor Base 2 ou superior. Você adquire uma "
                    + "Habilidade do Atributo escolhido, você ainda precisa preencher requisitos "
                    + "de Atributos Mínimos - se houver.",
            FeatRequirements.builder().build()),

    /** "Escolha um Atributo, você recebe uma das Habilidades do Atributo escolhido." */
    // TODO: same acquisition-slot grant as PRODIGIO.
    // TODO: "Atributo 3 ou Superior" names no particular Attribute — FeatRequirements tests one
    //  named AttributeDomain, not "any", so this is left unset.
    GENIALIDADE(
            "Escolha um Atributo, você recebe uma das Habilidades do Atributo escolhido. Você "
                    + "precisa cumprir com Requisitos da Habilidade de Atributo escolhida para "
                    + "receber seus benefícios.",
            FeatRequirements.builder().build()),

    /**
     * "Você recebe +1 de Bônus Racial no Atributo escolhido e uma de suas Habilidades do Atributo."
     *
     * <p>The Bônus Racial half is a real, writable field ({@code AttributeValue#racialBonus}) —
     * only which Attribute receives it is unrecorded.
     */
    // TODO: same acquisition-slot grant and unrepresented choice as PRODIGIO.
    GENIALIDADE_DESPERTA(
            "Escolha um Atributo, você recebe +1 de Bônus Racial no Atributo escolhido e uma de "
                    + "suas Habilidades do Atributo. Você precisa cumprir com Requisitos da "
                    + "Habilidade de Atributo escolhida para receber seus benefícios.",
            FeatRequirements.builder()
                    .requiredFeat(GENIALIDADE)
                    .requiredAwakenedTitles(2)
                    .build()),

    /** "Escolha um Talento Racial que você cumpra todos os demais requisitos, além da Raça." */
    // TODO: grants another Talento's benefits without granting the Talento — nothing expresses
    //  "treat me as holding X", and the racial trees are not yet authored.
    EXCEPCIONALIDADE(
            "Escolha um Talento Racial que você cumpra todos os demais requisitos, além da Raça. "
                    + "Você recebe os benefícios do Talento escolhido. Este Talento só pode ser "
                    + "adquirido por personagens que passem longos períodos de convivência com, ou "
                    + "que tenha sido criado por, membros da raça de referência.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .requiredFeatCategory(FeatCategory.DESTINO)
                    .requiredFeatCategoryCount(2)
                    .build()),

    /**
     * "A GD para Conjurar suas Magias Naturais é reduzida em -1 Nível."
     */
    // TODO: Feat#resolveDifficultyReduction is real, but it is summed by
    //  AbstractSkillInteraction on a Perícia roll — a Conjuração GD is a different question, and
    //  SpellCastingService does not resolve either roll's target GD at all yet.
    // TODO: scoped to Magias of one MagicType — note the enum's NATURAL constant is itself in
    //  question (see CLAUDE.md's MagiaAlternativaAbility warning).
    // TODO: its Pré-requisito names 'Escolhido de Gaea', a Talento de Devoção, which is excluded
    //  from this catalog — so it is left unset and this is wrongly open.
    ARCANISMO_DRUIDICO(
            "A GD para Conjurar suas Magias Naturais é reduzida em -1 Nível. Se você tiver pelo "
                    + "menos 1 Título Aventyr Desperto, o Tempo de Conjuração da primeira Magia "
                    + "Natural que conjurar em Rodadas Ímpares é reduzido em -1PA.",
            FeatRequirements.builder().build()),

    /**
     * "Escolha uma Perícia, você recebe os benefícios de Favoritismo da Perícia quando efetuada em
     * disputa contra um oponente que o reconheça."
     */
    // TODO: Favoritismo is an unmodelled mechanic, and "um oponente que o reconheça" needs
    //  recognition between characters, which nothing tracks.
    // TODO: "Fama 15" is a CharacterSheet value; FeatRequirements reads only Character-side data,
    //  and Feat#isEligible takes no sheet — the same split MoralHerdadaAbility#applyStartingFama
    //  works around by taking both.
    FAVORITISMO_MAIOR(
            "Escolha uma Perícia, você recebe os benefícios de Favoritismo da Perícia quando "
                    + "efetuada em disputa contra um oponente que o reconheça ou visando um alvo "
                    + "que o reconheça, mesmo que não tenham pessoas neutras a cena assistindo.",
            FeatRequirements.builder().build()),

    /**
     * "Você pode escolher atrasar seu Despertar de Títulos, recebendo seus benefícios apenas
     * quando quiser e se quiser."
     */
    // TODO: the whole Despertar timeline is missing — see this enum's own javadoc. Every figure
    //  here multiplies by "Títulos ainda não Despertos", a count that cannot exist while a
    //  Título is either held or absent.
    // TODO: "deve ser adquirido antes de Despertar seus Títulos" is an ordering constraint
    //  FeatRequirements cannot express.
    ATRASAR_DESPERTAR(
            "Você pode escolher atrasar seu Despertar de Títulos, recebendo seus benefícios apenas "
                    + "quando quiser e se quiser. Você recebe Bônus de +1 em Rolagens de Perícias e "
                    + "Danos, além de Redução de Danos Sofridos e aumento de Margem Crítica Menor "
                    + "em rolagens de Perícias, estes benefícios aumentam em +1 para cada 15EXP e "
                    + "são multiplicados pelo número de Títulos ainda não Despertos. Este Talento "
                    + "deve ser adquirido antes de Despertar seus Títulos.",
            FeatRequirements.builder().build()),

    /** "Você não Desperta Títulos Aventyr, mantendo suas Centelhas inertes indefinidamente." */
    // TODO: same missing Despertar timeline as ATRASAR_DESPERTAR. The PV/PM multiplier uplift is
    //  otherwise expressible (ModifierType#LIFE_MULTIPLIER/MANA_MULTIPLIER), but its multiplicand
    //  is the un-awakened Título count.
    ABDICADOR(
            "Você não Desperta Títulos Aventyr, mantendo suas Centelhas inertes indefinidamente. "
                    + "Os Benefícios de Atrasar Despertar são dobrados e seu Multiplicador de PV e "
                    + "PM aumentam em +1 para cada Título ainda não Desperto.",
            FeatRequirements.builder()
                    .requiredFeat(ATRASAR_DESPERTAR)
                    .build()),

    /**
     * "Você recebe Bônus Racial de +2 em todos os Atributos para cada Centelha que você não possua
     * ou voluntariamente não Despertar."
     */
    // TODO: a Centelha is not modelled — nothing represents a character possessing or lacking
    //  one (its *sacrifice* is now a caller assertion, RegaliaDonation, which is enough for the
    //  ArtificeFeat tree but not for this "para cada Centelha que você não possua" count).
    // TODO: disjunctive Pré-requisito (the Talento *ou* lacking a Centelha).
    FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH(
            "Você recebe Bônus Racial de +2 em todos os Atributos para cada Centelha que você não "
                    + "possua ou voluntariamente não Despertar.",
            FeatRequirements.builder()
                    .requiredFeat(ATRASAR_DESPERTAR)
                    .build()),

    /** "Você pode reduzir o Tempo de Ativação de suas Habilidades de Título em -1PA." */
    // TODO: a Habilidade de Título's activation cost is not modelled — AventyrTitleAbility
    //  reports whether it is passive, not what activating it costs.
    ACELERAR_HABILIDADE(
            "Você pode reduzir o Tempo de Ativação de suas Habilidades de Título em -1PA, acelerar "
                    + "Habilidades aumenta o Custo de Ativação da Habilidade em +2PD. Este efeito "
                    + "pode ser ativado apenas uma vez a cada Rodada e apenas em seu Turno.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .build()),

    /** "Durante a Ativação de uma Habilidade você pode aumentar seu Custo em +2PD, se o fizer a Duração da Habilidade é aumentada em +2 Unidades." */
    // TODO: same missing activation cost as ACELERAR_HABILIDADE, and a Habilidade's Duração is
    //  likewise not a modelled value.
    CENTELHA_DURADOURA(
            "Durante a Ativação de uma Habilidade você pode aumentar seu Custo em +2PD, se o fizer "
                    + "a Duração da Habilidade é aumentada em +2 Unidades.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .build()),

    /** "Seu personagem desperta seu Título Primário ao fim da primeira sessão de Jogo." */
    // TODO: needs both the Despertar timeline and a game-session concept — the gap catalog
    //  records that no session state exists, only a consumer-triggered recovery call.
    // TODO: "Apenas personagens recém-criados" is a creation-time-only restriction with no
    //  representation.
    DESPERTAR_ANTECIPADO(
            "Seu personagem desperta seu Título Primário ao fim da primeira sessão de Jogo.",
            FeatRequirements.builder().build()),

    /** "Você desperta seu Título Secundário ao atingir a marca de 23EXP." */
    // TODO: same missing Despertar timeline; and the EXP threshold is a CharacterSheet value
    //  Feat#isEligible cannot reach — see FAVORITISMO_MAIOR.
    CENTELHA_GRAN_AVENTYR_ANTECIPADA(
            "Você desperta seu Título Secundário ao atingir a marca de 23EXP.",
            FeatRequirements.builder()
                    .requiredFeat(DESPERTAR_ANTECIPADO)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    DestinoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.DESTINO;
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
