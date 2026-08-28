package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos de Duelista — weapon mastery, two-weapon fighting, and trading accuracy for damage.
 *
 * <p>Three blockers recur and are not repeated on every constant.
 *
 * <ul>
 *   <li><b>Trading Vantagem for Desvantagem across two rolls.</b> Half this tree lets a
 *       character accept a malus on one roll to gain a bonus on another. Both figures exist
 *       ({@code Skill#ADVANTAGE_BONUS}/{@code Skill#DISADVANTAGE_MALUS}), but a roll and its dano
 *       roll are resolved separately with nothing linking a choice made on one to the other.</li>
 *   <li><b>Rerolling the lowest die.</b> This core never rolls dice; a {@code SkillRoll} arrives
 *       already resolved, so a reroll is the caller's own step.</li>
 *   <li><b>What is being wielded.</b> {@code Character#equipment} lists Items but nothing marks
 *       a weapon as held in the off-hand, as light, or as an Arma Natural.</li>
 * </ul>
 */
public enum DuelistaFeat implements Feat {

    /**
     * "Escolha entre um tipo de arma, armas naturais ou magias ofensivas. Receba vantagem nas
     * rolagens de ataque com a arma escolhida."
     */
    // TODO: carries an acquisition-time choice; and no Feat hook reaches a Perícia roll —
    //  AbstractSkillInteraction never scans character.getFeats(). See PeritoFeat's javadoc for
    //  the choice-representation question, which this constant shares.
    ESPECIALISTA_EM_ARMA(
            "Escolha entre um tipo de arma, armas naturais ou magias ofensivas. Receba vantagem "
                    + "nas rolagens de ataque com a arma escolhida, ou com suas magias ofensivas "
                    + "(que exijam uma rolagem de perícia de Ataque), conforme escolhido.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(2)
                    .build()),

    /**
     * "Você pode aumentar o Tempo de Ação de um Ataque Corpo-a-Corpo em +1PA, se o fizer poderá
     * rolar novamente o dado de menor valor em sua rolagem."
     */
    // TODO: reroll — see the enum's own javadoc.
    LUTADOR_NATO(
            "Você pode aumentar o Tempo de Ação de um Ataque Corpo-a-Corpo em +1PA, se o fizer "
                    + "poderá rolar novamente o dado de menor valor em sua rolagem. O novo "
                    + "resultado será utilizado, mesmo que seja inferior ao anterior.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(1)
                    .build()),

    /**
     * "Você pode optar por receber Desvantagem em rolagens de 'Ataque Corpo-a-Corpo' para receber
     * vantagem em sua rolagem de dano."
     */
    // TODO: the accuracy-for-damage trade — see the enum's own javadoc.
    ATAQUE_CONCENTRADO(
            "Você pode optar por receber Desvantagem em rolagens de ‘Ataque Corpo-a-Corpo’ para "
                    + "receber vantagem em sua rolagem de dano. Se utilizar este Talento ao mesmo "
                    + "tempo que Lutador Nato, ao invés da Vantagem na rolagem de Dano, seu dano "
                    + "aumenta em +1d6.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.STRENGTH)
                    .requiredAttributeValue(4)
                    .requiredFeat(LUTADOR_NATO)
                    .build()),

    /**
     * "Você recebe Vantagem em suas rolagens de Ataque Corpo-a-Corpo contra alvos adjacentes."
     */
    // TODO: the range condition is expressible (Range#ADJACENTE) but no Feat hook receives a
    //  SceneContext or an attack target; reroll for the second half.
    // TODO: disjunctive Pré-requisito (Talento Lutador Nato *ou* 4 Graduações); modelled as the
    //  Talento branch, so the pure-Graduação route is wrongly refused.
    LUTAR_ENGAJADO(
            "Você recebe Vantagem em suas rolagens de Ataque Corpo-a-Corpo contra alvos "
                    + "adjacentes. Sempre que usar o talento Lutador Nato contra um alvo adjacente "
                    + "poderá também rolar novamente o dado de menor valor em suas rolagens de "
                    + "Dano.",
            FeatRequirements.builder()
                    .requiredFeat(LUTADOR_NATO)
                    .build()),

    /**
     * "Enquanto empunhar 2 armas simultaneamente você poderá realizar dois ataques, um com cada
     * uma de suas armas, ao custo de 3PA."
     */
    // TODO: granting an extra attack is not expressible, and off-hand/light weapon state is
    //  missing — see the enum's own javadoc.
    COMBATER_COM_2_ARMAS(
            "Enquanto empunhar 2 armas simultaneamente você poderá realizar dois ataques, um com "
                    + "cada uma de suas armas, ao custo de 3PA. Rolagens de Perícias de Ataque "
                    + "feitas desta forma sofrem Desvantagem. Se a arma na mão inábil não for uma "
                    + "arma leve adicionalmente você sofre Desvantagem nas rolagens de Danos.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(3)
                    .build()),

    /**
     * "Você recebe um bônus de +1 em DF enquanto estiver empunhando 2 armas", scaling to +3/+5 by
     * Graduação em Ataque Corpo-a-Corpo.
     *
     * <p>The ladder is plain arithmetic {@link Feat#resolveDefenseBonus} could compute, and it is
     * correctly scoped to {@code DefenseType#PHYSICAL}; only the wielding condition blocks it.
     */
    // TODO: nothing marks two weapons as simultaneously wielded — see the enum's own javadoc.
    //  Granting unconditionally would hand a Defesa bonus to a character carrying nothing.
    // TODO: the reduction-to-zero clause needs attacks made this Turn to be tracked.
    DEFESA_COM_2_ARMAS(
            "Você recebe um bônus de +1 em DF enquanto estiver empunhando 2 armas, e bônus "
                    + "cumulativo de +2 em DF (para um total de +3) se não utilizar nenhuma de "
                    + "suas armas para atacar. Este Bônus aumenta para +2/+4 se tiver 4 ou mais "
                    + "Graduações em Ataque Corpo-a-Corpo, e para +3/+5 se tiver 7 ou mais "
                    + "Graduações. Estes bônus em DF são reduzidos à zero por 1 Rodada se ambas as "
                    + "armas foram utilizadas para atacar em seu Turno, a menos que você tenha 10 "
                    + "Graduações.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(3)
                    .requiredFeat(COMBATER_COM_2_ARMAS)
                    .build()),

    /** "Você recebe Vantagem em todas as suas rolagens de ataque feitas com qualquer arma ou se estiver desarmado." */
    // TODO: no Feat hook reaches a Perícia roll. Unusually, this half is *unconditional* across
    //  both Perícias de Ataque, so a Feat roll-bonus hook would make it real outright.
    // TODO: "apenas personagens que não escolheram Magias Ofensivas" is an exclusion on the
    //  prerequisite Talento's own recorded choice, which is not represented.
    DOMINAR_ARMAS(
            "Você recebe Vantagem em todas as suas rolagens de ataque feitas com qualquer arma ou "
                    + "se estiver desarmado. Adicionalmente o primeiro ataque que fizer a cada "
                    + "Rodada, com a arma escolhida no talento ‘Especialista em Arma’, tem seu "
                    + "Tempo de Ação reduzido em -1PA (mínimo 1PA). Apenas personagens que não "
                    + "escolheram Magias Ofensivas podem adquirir este Talento.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(6)
                    .requiredFeat(ESPECIALISTA_EM_ARMA)
                    .build()),

    /** "Seus ataques possuem a Margem Crítica Menor aumentada em +1 e recebem a Corrente de Efeitos – Golpe Trovejante." */
    // TODO: Margem Crítica hook missing on Feat, and this names the Menor tier specifically (see
    //  AssassinoFeat's javadoc); granting a Corrente de Efeitos has no hook on the attack path.
    MAESTRIA_EM_ARMA(
            "Enquanto estiver utilizando o método escolhido para atacar no Talento ‘Especialista "
                    + "em Arma’, seus ataques possuem a Margem Crítica Menor aumentada em +1 e "
                    + "recebem a Corrente de Efeitos – Golpe Trovejante.",
            FeatRequirements.builder()
                    .requiredFeat(DOMINAR_ARMAS)
                    .build()),

    /** "Você pode escolher receber Desvantagem em sua Rolagem de Danos para adquirir Vantagem em sua Rolagem de Perícia de Ataque." */
    // TODO: the damage-for-accuracy trade — see the enum's own javadoc. This is ATAQUE_CONCENTRADO
    //  inverted, and blocked identically.
    ATAQUE_RAPIDO(
            "Ao desferir um ataque você pode escolher receber Desvantagem em sua Rolagem de Danos "
                    + "para adquirir Vantagem em sua Rolagem de Perícia de Ataque.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(2)
                    .build()),

    /** "Uma vez por Turno você pode fazer uma rolagem de Perícia de Ataque ao tempo de 1PA." */
    // TODO: what an attack costs is the caller's; and Meio-Dano exists as a DamageService flag
    //  but nothing lets a Talento set it for one attack.
    ATAQUE_REPENTINO(
            "Você é capaz de fazer um ataque de grande velocidade, mas que causa poucos danos. Uma "
                    + "vez por Turno você pode fazer uma rolagem de Perícia de Ataque ao tempo de "
                    + "1PA. Os danos causados por este ataque são reduzidos à metade, este é um "
                    + "efeito de Meio-Dano.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(4)
                    .requiredFeat(ATAQUE_RAPIDO)
                    .build()),

    /** "Enquanto estiver cego ou privado de seus sentidos visuais você não fica Desprevenido em função destas condições." */
    // TODO: needs blindness and the Desprevenido condition; this core has no status-condition
    //  system, so there is nothing to be exempt from.
    COMBATER_AS_CEGAS(
            "Enquanto estiver cego ou privado de seus sentidos visuais você não fica Desprevenido "
                    + "em função destas condições e não precisa efetuar rolagens de 1d6 para "
                    + "utilizar efeitos pessoais e Ataques Corpo-a-Corpo. Para receber os "
                    + "benefícios deste Talento, rolagens de Perícias feitas às cegas recebem "
                    + "Desvantagem.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.INSTINCT)
                    .requiredAttributeValue(3)
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(4)
                    .build()),

    /**
     * "Você pode adicionar metade do seu valor de Vigor à sua rolagem de danos físicos
     * corpo-a-corpo, se o fizer e for bem-sucedido você sofre 2 pontos de Dano Físico Primordial."
     */
    // TODO: the bonus is computable (half Vigor) but Feat has no dano-bonus hook, and the
    //  self-damage is a Dano Primordial that ignores mitigation and resists healing — neither an
    //  unmitigable damage type nor a healing restriction exists.
    FORCA_EXCESSIVA(
            "Você pode adicionar metade do seu valor de Vigor à sua rolagem de danos físicos "
                    + "corpo-a-corpo, se o fizer e for bem-sucedido você sofre 2 pontos de Dano "
                    + "Físico Primordial. Danos sofridos desta forma não podem ser reduzidos e são "
                    + "recuperados apenas com Descansos Verdadeiros.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.STRENGTH)
                    .requiredAttributeValue(3)
                    .build()),

    /** "Ao realizar um ataque com uma Arma ou Ataque Desarmado seu tipo de ataque muda para Área de Efeito – Explosão." */
    // TODO: needs Área de Efeito footprint resolution (gap catalog, "Area de Efeito"), and a
    //  per-Rodada activation counter.
    ATAQUE_GIRATORIO(
            "Apenas uma vez por Rodada, ao realizar um ataque com uma Arma ou Ataque Desarmado seu "
                    + "tipo de ataque muda para Área de Efeito – Explosão.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .requiredFeatCategory(FeatCategory.DUELISTA)
                    .requiredFeatCategoryCount(4)
                    .build()),

    /** "Após ser bem-sucedido em um Ataque Rápido você pode fazer imediatamente um Ataque Concentrado ao custo de 1PA." */
    // TODO: chains two Talentos that are both unbuilt, and granting an extra attack is not
    //  expressible.
    // TODO: names *two* required Talentos; requiredFeat is singular, so only ATAQUE_CONCENTRADO
    //  is recorded and ATAQUE_RAPIDO goes unenforced.
    UM_DOIS(
            "Após ser bem-sucedido em um Ataque Rápido você pode fazer imediatamente um Ataque "
                    + "Concentrado ao custo de 1PA.",
            FeatRequirements.builder()
                    .requiredFeat(ATAQUE_CONCENTRADO)
                    .requiredAwakenedTitles(1)
                    .build()),

    /** "Seu segundo ataque contra um mesmo alvo na mesma Rodada tem a Margem Crítica Menor aumentada em +2 números." */
    // TODO: Margem Crítica hook missing on Feat, Menor tier only; and "segundo ataque contra um
    //  mesmo alvo" needs per-target attack counting within a Rodada, which nothing tracks.
    // TODO: the critical dano half (Metade da Gnose) is computable but Feat has no dano hook.
    EXPLORAR_PONTOS_FRACOS(
            "Seu segundo ataque contra um mesmo alvo na mesma Rodada tem a Margem Crítica Menor "
                    + "aumentada em +2 números. Seus Acertos Críticos causam Metade da Gnose como "
                    + "dano adicional.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(4)
                    .requiredAwakenedTitles(1)
                    .build()),

    /** "O dano adicional de Metade da Gnose não pode ser curado, exceto por Descansos Verdadeiros e Roubo de Vida." */
    // TODO: builds on EXPLORAR_PONTOS_FRACOS, and needs damage that resists healing — CombatantSheet
    //  #heal has no hook to refuse a recovery (the same gap Zumbi's own clause records).
    FERIDAS_ARDENTES(
            "O dano adicional de Metade da Gnose, causado em seus Danos Críticos, não pode ser "
                    + "curado, exceto por Descansos Verdadeiros e efeitos de Roubo de Vida.",
            FeatRequirements.builder()
                    .requiredFeat(EXPLORAR_PONTOS_FRACOS)
                    .build()),

    /** "Você pode substituir sua rolagem de Defesa Física por uma Rolagem de Perícia de Ataque." */
    // TODO: AttackReceiver rolls the defender's Esquiva e Aparar with no hook to swap in another
    //  Perícia, and damaging the attacker's weapon needs per-copy item state (Dureza).
    DEFENDER_SE_ATACANDO(
            "Apenas uma vez por Rodada, você pode substituir sua rolagem de Defesa Física por uma "
                    + "Rolagem de Perícia de Ataque. Se for bem-sucedido você evita o ataque que "
                    + "lhe seria causado e inflige Força pontos de danos à arma ou projétil "
                    + "utilizado, se for malsucedido o atacante recebe Vantagem na rolagem de "
                    + "danos deste ataque.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .requiredFeatCategory(FeatCategory.DUELISTA)
                    .requiredFeatCategoryCount(3)
                    .build()),

    /** "Você pode usar o Talento Defender-se Atacando também em substituição à Defesa Mágica." */
    // TODO: builds on DEFENDER_SE_ATACANDO, and its exception needs Malefício classification —
    //  no Encantamento/Maldição tag exists (gap catalog, "Malefício classification").
    DEFENDER_SE_ATACANDO_SUPERIOR(
            "Você pode usar o Talento Defender-se Atacando também em substituição à Defesa Mágica, "
                    + "exceto para evitar Encantamentos e Maldições.",
            FeatRequirements.builder()
                    .requiredFeat(DEFENDER_SE_ATACANDO)
                    .build()),

    /**
     * "Você recebe Resistência à Críticos. Sempre que um atacante Corpo-a-Corpo lhe infligir danos
     * físicos ele também sofre 1 ponto de Dano Físico."
     *
     * <p>One of the five Talentos with no Pré-requisito line; "Vigor 5" is printed as its
     * Pré-requisito and is modelled as such.
     */
    // TODO: retaliation damage does not exist — DamageService only computes damage *to* a target
    //  *from* an attacker, never the reverse (gap catalog, "Reactive/retaliation damage").
    // TODO: "Resistência à Críticos" is not a stat; CriticalEffect immunity is per named
    //  CriticalEffectType, not a general resistance.
    CORACAO_DE_FERRO(
            "Você recebe Resistência à Críticos. Sempre que um atacante Corpo-a-Corpo lhe infligir "
                    + "danos físicos ele também sofre 1 ponto de Dano Físico, se o ataque for um "
                    + "Acerto Crítico ao invés disso ele sofre 3 pontos de Danos Físicos.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(5)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    DuelistaFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.DUELISTA;
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
