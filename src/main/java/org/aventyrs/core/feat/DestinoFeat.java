package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;

/**
 * Talentos de Destino — what a character is, rather than what they can do: how enemies read
 * them, which Habilidades they were born to, and when their Títulos Aventyr awaken.
 *
 * <p>Two blockers dominate — one of them now half-lifted. The first is <b>granting another
 * trait</b>: five constants here hand out a Habilidade de Atributo, a Vantagem de Ego, or another
 * Talento outright. The three that grant a <b>Habilidade de Atributo</b> are real now, through
 * {@link HabilidadeDeAtributoEscolhidaFeat}. The other two are not: a Vantagem de Ego is chosen
 * once at character creation and never awarded later, and "treat me as holding another Talento"
 * has no expression at all.
 *
 * <p>The second is the <b>Despertar timeline</b>. Half the Aventyr-tier constants here delay,
 * accelerate, or forgo awakening a Título, and trade on <i>when</i> one awakens. This core models
 * a Título as simply held or not (see {@code FeatRequirements#requiredAwakenedTitles}); there is
 * no EXP threshold at which one awakens, and no game session for "ao fim da primeira sessão" to
 * name.
 *
 * <p><b>Counting the un-awakened ones is <i>not</i> part of that gap</b>, and used to be filed
 * under it by mistake. A Character has exactly three {@link TitleSlot}s, so "cada Título ainda não
 * Desperto" is three minus the held ones — arithmetic available today, with no timeline involved.
 * That is what makes {@link #ABDICADOR}'s PV/PM multiplier real while the rest of its sentence
 * still waits.
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
     * <p>The rules text names this Talento "Coração de Ferro", the same as the unrelated {@code
     * DuelistaFeat#CORACAO_DE_FERRO} in another tree. The constant here is suffixed {@code
     * _DO_DESTINO} to keep the two apart, because a {@code Feat}'s {@code name()} <b>is</b> its
     * persisted identity — {@code FeatCatalog} indexes by it and aventyrs-api stores a held Talento
     * as that bare string, so two constants sharing one name are indistinguishable once written and
     * load back as whichever a reader indexes first. Display text comes from {@link
     * #getDescription()}, never from the constant name, so the suffix is invisible to a player.
     */
    // TODO: the Descanso recovery half needs a PD equivalent of resolveRestMagicPointsBonus —
    //  RestService recovers PD but scans no Talento hook for it. The figure itself is computable
    //  (2 + getAllTitles().size()).
    CORACAO_DE_FERRO_DO_DESTINO(
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
    // The "Força igual ou inferior à 2" maximum is enforced now —
    // FeatRequirements#maximumAttributeDomain is its own clause, separate from the minimum,
    // because a Talento naming both names two different Atributos.
    APARENCIA_INOFENSIVA(
            "A menos que você seja o único alvo disponível, ou já tenha realizado ações ofensivas "
                    + "contra seus inimigos, você nunca será alvo primário de ataques, Magias ou "
                    + "Habilidades inimigas nas duas primeiras Rodadas de um combate. Este Talento "
                    + "afeta apenas inimigos inteligentes.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.CHARISMA)
                    .requiredAttributeValue(3)
                    .maximumAttributeDomain(AttributeDomain.STRENGTH)
                    .maximumAttributeValue(2)
                    .build()),

    /** As {@link #APARENCIA_INOFENSIVA}, extended to the first four Rodadas. */
    // Same targeting blockers as APARENCIA_INOFENSIVA; its Pré-requisito, though, is now fully
    // enforced — "Carisma 5 e Força 1" is a minimum on one Atributo and an exact figure on
    // another, read as the maximum it also is (a Força of 1 is the floor every Atributo starts
    // at, so "Força 1" can only mean "no higher than 1").
    APARENCIA_VERDADEIRAMENTE_INOFENSIVA(
            "A menos que você seja o único alvo disponível, ou já tenha realizado ações ofensivas "
                    + "contra seus inimigos, você nunca será alvo primário de ataques, Magias ou "
                    + "Habilidades inimigas nas quatro primeiras Rodadas de um combate.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.CHARISMA)
                    .requiredAttributeValue(5)
                    .maximumAttributeDomain(AttributeDomain.STRENGTH)
                    .maximumAttributeValue(1)
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
    // Real, through HabilidadeDeAtributoEscolhidaFeat — the acquired form recording which
    // Habilidade the player picked, granted past the AttributeAbilityService slot economy by
    // Feat#getGrantedAttributeAbilities. Its "Atributo com valor Base 2 ou superior" is the same
    // floor that hook's own factory enforces, so the clause needs no second check.
    PRODIGIO(
            "Escolha um Atributo que você possua valor Base 2 ou superior. Você adquire uma "
                    + "Habilidade do Atributo escolhido, você ainda precisa preencher requisitos "
                    + "de Atributos Mínimos - se houver.",
            FeatRequirements.builder().build()),

    /** "Escolha um Atributo, você recebe uma das Habilidades do Atributo escolhido." */
    // Real, the same way as PRODIGIO — HabilidadeDeAtributoEscolhidaFeat serves all three of
    // these constants, since an AttributeAbility already reports its own Atributo.
    // "Atributo 3 ou Superior" is enforced now, through FeatRequirements#requiredAnyAttributeValue.
    GENIALIDADE(
            "Escolha um Atributo, você recebe uma das Habilidades do Atributo escolhido. Você "
                    + "precisa cumprir com Requisitos da Habilidade de Atributo escolhida para "
                    + "receber seus benefícios.",
            FeatRequirements.builder()
                    .requiredAnyAttributeValue(3)
                    .build()),

    /**
     * "Você recebe +1 de Bônus Racial no Atributo escolhido e uma de suas Habilidades do Atributo."
     *
     * <p>The chosen Atributo is recorded now, so the Bônus Racial lands on it — modelled as a
     * {@code Feat#resolveAttributeBonus} grant rather than a write to {@code
     * AttributeValue#racialBonus}, the same reading every other "recebe Bônus Racial de +1"
     * Talento takes.
     */
    // Both halves real, through HabilidadeDeAtributoEscolhidaFeat: the chosen Habilidade, and
    // the "+1 de Bônus Racial no Atributo escolhido" that only this constant of the three adds.
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
    // "Fama 15" is enforced now: Feat#isEligible has a CharacterSheet-taking overload, which
    // FeatService#grantFeat calls. Read as *either* Fama reaching 15 — the rules text says plain
    // "Fama" while this core splits it Positiva/Negativa, and Favoritismo is about being
    // recognised, which notoriety serves as well as renown. A sheet-less eligibility preview
    // skips the clause rather than failing it, so the listing stays looser than the real gate.
    FAVORITISMO_MAIOR(
            "Escolha uma Perícia, você recebe os benefícios de Favoritismo da Perícia quando "
                    + "efetuada em disputa contra um oponente que o reconheça ou visando um alvo "
                    + "que o reconheça, mesmo que não tenham pessoas neutras a cena assistindo.",
            FeatRequirements.builder()
                    .requiredFame(15)
                    .build()),

    /**
     * "Você pode escolher atrasar seu Despertar de Títulos, recebendo seus benefícios apenas
     * quando quiser e se quiser."
     */
    // The "Títulos ainda não Despertos" multiplicand is *not* a blocker — see ABDICADOR below and
    // this enum's own javadoc: it is three TitleSlots minus the held ones, countable today. What
    // this constant still needs is the other half of each figure, "+1 para cada 15EXP": EXP total
    // lives on CharacterSheet, and while resolveSkillRollBonus does have a sheet-taking overload
    // now, resolveDamageReduction/resolveCriticalMarginIncrease would each need the same reach for
    // the RDS and Margem Crítica figures — so the Talento would land in pieces rather than whole.
    // TODO: the four figures (Perícia roll, Danos, RDS, Margem Crítica Menor) therefore stay
    //  ungranted together, per this tree's grant-it-whole-or-not-at-all rule.
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

    /**
     * "Você não Desperta Títulos Aventyr, mantendo suas Centelhas inertes indefinidamente."
     *
     * <p><b>The multiplier sentence is real</b>: "+1 [de Multiplicador de PV e PM] para cada
     * Título ainda não Desperto", through {@link Feat#resolveLifeMultiplierIncrease} and {@link
     * Feat#resolveManaMultiplierIncrease}. A Título "ainda não Desperto" is an <b>unfilled {@link
     * TitleSlot}</b> — a Character has exactly three, so the multiplicand is simply the three
     * minus however many are held. That reading is what makes this the one clause of the Despertar
     * cluster that needs no timeline: it counts slots, not events.
     *
     * <p>It is the catalog's only multiplier that moves <em>inversely</em> with Títulos — every
     * other one grows as they awaken ({@code OrquicoFeat#TERRA_NAS_VEIAS}), which is exactly the
     * trade this Talento is written to make.
     */
    // TODO: "Os Benefícios de Atrasar Despertar são dobrados" is still blocked — ATRASAR_DESPERTAR
    //  itself is unbuilt (the Despertar timeline), so there are no benefits to double. Only the
    //  multiplier sentence lands; the two are separate clauses and should not be conflated again.
    ABDICADOR(
            "Você não Desperta Títulos Aventyr, mantendo suas Centelhas inertes indefinidamente. "
                    + "Os Benefícios de Atrasar Despertar são dobrados e seu Multiplicador de PV e "
                    + "PM aumentam em +1 para cada Título ainda não Desperto.",
            FeatRequirements.builder()
                    .requiredFeat(ATRASAR_DESPERTAR)
                    .build()) {
        @Override
        public int resolveLifeMultiplierIncrease(final Character character) {
            return unawakenedTitles(character);
        }

        @Override
        public int resolveManaMultiplierIncrease(final Character character) {
            return unawakenedTitles(character);
        }
    },

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

    /**
     * Títulos Aventyr this character has <b>not</b> awakened — the three {@link TitleSlot}s minus
     * the filled ones. What {@link #ABDICADOR}'s "para cada Título ainda não Desperto" multiplies
     * by; see that constant for why an empty slot is the right reading.
     */
    private static int unawakenedTitles(final Character character) {
        return TitleSlot.values().length - character.getAllTitles().size();
    }

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
