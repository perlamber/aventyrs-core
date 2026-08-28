package org.aventyrs.core.feat;

import org.aventyrs.core.skill.SkillType;

/**
 * Talentos de Escudeiro — fighting with, and behind, a shield.
 *
 * <p>One blocker runs through the entire tree and is not repeated on every constant: <b>nothing
 * classifies an {@code Item} as a Escudo</b>. {@code ItemCategory} has no such value, and {@code
 * Weapon} is the only Item subtype, so "enquanto estiver utilizando um item do tipo Escudo" —
 * the condition almost every constant here turns on — cannot be tested. The weight tiers these
 * Talentos distinguish (Leve/Médio/Pesado/de Corpo) would additionally need {@code
 * ItemWeightClass} to reach a shield, and the Bônus Defensivos they halve or forgo need per-copy
 * item state the gap catalog's "Owned/produced item copy" row records as missing.
 *
 * <p>Per-constant TODOs name only what is <em>additional</em> to that.
 */
public enum EscudeiroFeat implements Feat {

    /**
     * "Sempre que iniciar um combate com um Escudo de Categoria Média ou Pesada em mãos, sua
     * iniciativa aumenta em +2."
     *
     * <p>The one constant here whose <em>effect</em> is already expressible — {@code
     * ModifierType.INITIATIVE} is summed by {@code InitiativeService} — but {@code Feat} is not
     * among the three sources it scans, and the shield condition is untestable regardless.
     */
    // TODO: InitiativeService scans attributeAbilities/skillCompetencyAbilities/excellencies, not
    //  character.getFeats(); a Talento cannot grant Iniciativa today.
    ESCUDO_VELOZ(
            "Sempre que iniciar um combate com um Escudo de Categoria Média ou Pesada em mãos, sua "
                    + "iniciativa aumenta em +2.",
            FeatRequirements.builder().build()),

    /**
     * "Você recebe +1 em suas Defesas enquanto utilizar um item escolhido do tipo 'Escudo'",
     * rising to +2 at 4 Graduações em Esquiva e Aparar and +3 at 7.
     *
     * <p>The graduation ladder itself is plain arithmetic a {@code resolveDefenseBonus} override
     * could compute; only the shield condition blocks it, and granting unconditionally would
     * hand the bonus to a character carrying nothing.
     */
    // TODO: carries an acquisition-time choice (which Escudo item) — CLAUDE.md's AcquiredChoice
    //  pattern, still without a consuming mechanism.
    ESPECIALISTA_EM_ESCUDO(
            "Você recebe +1 em suas Defesas enquanto utilizar um item escolhido do tipo ‘Escudo’. "
                    + "Escolha um item do tipo ‘Escudo’, se tiver 4 ou mais graduações em ‘Esquiva "
                    + "e Aparar’ enquanto estiver utilizando o item escolhido este Bônus aumenta "
                    + "para +2, se tiver 7 ou mais Graduações este Bônus aumenta para +3.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(1)
                    .build()),

    /**
     * "Você pode usar seu escudo para atacar, se o fizer você perde metade dos bônus em Defesas
     * concedidos por ele." Shield attacks roll at half Esquiva e Aparar plus the shield's own
     * defensive bonuses, and deal 1d6+1/+2/+3 by weight.
     */
    // TODO: a shield is not a Weapon, so it has no DamageBase and no attacking SkillType — the
    //  three authored figures (Leve 1d6+1, Médio 1d6+2, Pesado 1d6+3) are exactly DamageBase.of
    //  values, but there is no shield item to hang them on.
    // TODO: an attack roll built from half a Graduação plus the item's own Defesa bonuses is a
    //  bespoke formula no Interaction can express; AbstractSkillInteraction reads the Perícia's
    //  own governing Attribute and Graduação.
    // TODO: "Margem Crítica Menor 17" is an absolute margin, not an increase — SkillRoll widens
    //  from the die faces and has no notion of a threshold number.
    ATACAR_COM_ESCUDOS(
            "Você pode usar seu escudo para atacar, se o fizer você perde metade dos bônus em "
                    + "Defesas concedidos por ele até o início de seu próximo turno. Rolagens de "
                    + "Ataque Corpo-a-Corpo efetuadas com escudos recebem Metade das suas "
                    + "Graduações em Esquiva e Aparar + Bônus Defensivos do Escudo, o dano causado "
                    + "varia conforme o tipo de escudo: Escudos Leves 1d6+1, Médios 1d6+2, Pesados "
                    + "1d6+3. Escudos possuem Margem Crítica Menor 17 e Sucesso Crítico: "
                    + "Atordoante.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(2)
                    .build()),

    /**
     * "Se você não se mover em seu Turno e estiver empunhando um escudo que você seja
     * especialista, você recebe um Bônus de +2 em suas Defesas por 1 Rodada."
     */
    // TODO: "se você não se mover em seu Turno" needs movement actually spent this Turn to be
    //  tracked; MovementService computes an allowance per Ponto de Ação and records no spend.
    // TODO: the Gigante/Anão Reposicionar exception needs a forced-movement/positioning concept
    //  (gap catalog, "Forced movement / positioning").
    DEFESA_TARTARUGA(
            "Se você não se mover em seu Turno e estiver empunhando um escudo que você seja "
                    + "especialista, você recebe um Bônus de +2 em suas Defesas por 1 Rodada. "
                    + "Personagens das raças Gigante e Anão não perdem os bônus concedidos por este "
                    + "Talento se o único movimento feito por eles na Rodada for ‘Reposicionar’.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(4)
                    .requiredFeat(ESPECIALISTA_EM_ESCUDO)
                    .build()),

    /**
     * "Após uma rolagem de Iniciativa, se você não for o primeiro a agir, nas duas primeiras
     * Rodadas do combate você recebe bônus de +3 em suas Defesas", or +5 if last to act.
     */
    // TODO: the round window is expressible (SceneContext#isWithinFirstCombatRounds(2)) and
    //  "ganhou a iniciativa" has a resolver, but "not first"/"last to act" is turn-order position
    //  which Scene exposes for nobody, and no Feat hook receives a SceneContext at all.
    // TODO: "você ignora efeitos que reduzem Defesas" needs suppression of a malus by source;
    //  DefenseService sums every contribution with no notion of which granted it.
    INICIO_DEFENSIVO(
            "Após uma rolagem de Iniciativa, se você não for o primeiro a agir, nas duas primeiras "
                    + "Rodadas do combate você recebe bônus de +3 em suas Defesas. Se você for o "
                    + "último a agir este bônus aumenta para +5. Durante estas Rodadas iniciais "
                    + "você ignora efeitos que reduzem Defesas.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(2)
                    .build()),

    /**
     * "Após realizar uma rolagem de Perícia de Ataque, você pode realizar um Ataque com Escudo em
     * Desvantagem com Tempo de Ação reduzido em -1PA."
     */
    // TODO: granting an extra attack is not expressible — an attack is initiated by a caller,
    //  never by a resolution.
    ESPARTANO(
            "Após realizar uma rolagem de Perícia de Ataque, você pode realizar um Ataque com "
                    + "Escudo em Desvantagem com Tempo de Ação reduzido em -1PA.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(3)
                    .requiredFeat(ATACAR_COM_ESCUDOS)
                    .build()),

    /**
     * "Você mantém metade dos Bônus Defensivos de seu Escudo ao realizar mais de um Ataque com
     * Escudos na mesma Rodada."
     */
    // TODO: modifies a penalty ATACAR_COM_ESCUDOS imposes, which is itself unbuilt.
    ATAQUE_MULTIPLO_COM_ESCUDOS(
            "Você mantém metade dos Bônus Defensivos de seu Escudo ao realizar mais de um Ataque "
                    + "com Escudos na mesma Rodada.",
            FeatRequirements.builder()
                    .requiredFeat(ATACAR_COM_ESCUDOS)
                    .build()),

    /**
     * "Você não perde Bônus Defensivo ao atacar com Escudos", plus +2 Margem Crítica Menor and a
     * Corrente de Efeitos on criticals.
     */
    // TODO: Margem Crítica — resolveCriticalMarginIncrease is not on Feat, and this names the
    //  Menor tier specifically where SkillRoll widens both together (see AssassinoFeat).
    // TODO: granting a Corrente de Efeitos to a critical has no hook on the attack path.
    ARTE_DO_ESCUDO_ATACANTE(
            "Você não perde Bônus Defensivo ao atacar com Escudos. Seus ataques com Escudos feitos "
                    + "em seus Turnos tem a Margem Crítica Menor aumentada em +2 números e seus "
                    + "Acertos Críticos recebem a Corrente de Efeitos – Rugido.",
            FeatRequirements.builder()
                    .requiredFeat(ATAQUE_MULTIPLO_COM_ESCUDOS)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "A GD de seu primeiro Ataque com Escudo de cada um de seus Turnos é reduzida em -1 nível."
     */
    // TODO: a Feat cannot reduce a roll's GD — getDifficultyReduction() is a
    //  SkillCompetencyAbility/SkillExcellency hook that no Interaction scans Talentos for.
    // TODO: "Empurrão Violento" is a manoeuvre with no representation, and "Alcance Estendido
    //  como Aprimoramento" needs per-copy item state (gap catalog, "Owned/produced item copy").
    DOMINIO_DA_ARTE_DO_ESCUDO_ATACANTE(
            "A GD de seu primeiro Ataque com Escudo de cada um de seus Turnos é reduzida em -1 "
                    + "nível. Após ser bem-sucedido em realizar um Empurrão Violento seu Escudo "
                    + "recebe Alcance Estendido como Aprimoramento até o final do Turno.",
            FeatRequirements.builder()
                    .requiredFeat(ARTE_DO_ESCUDO_ATACANTE)
                    .build()),

    /** "Você pode fazer Reações mesmo quando o efeito impedir Reações." */
    // TODO: nothing prevents Reações in the first place — there is no suppression mechanism to
    //  be exempt from (the gap catalog's "Movement-triggered Reações" row records the same for
    //  its own half). "Defender o Perímetro" is likewise an unmodelled Reação type.
    MESTRE_ESCUDEIRO(
            "Você pode fazer Reações mesmo quando o efeito impedir Reações. Enquanto estiver "
                    + "utilizando um item do tipo Escudo você pode fazer Reações do tipo Defender "
                    + "o Perímetro mesmo quando for alvo de investidas.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .requiredFeatCategory(FeatCategory.ESCUDEIRO)
                    .requiredFeatCategoryCount(2)
                    .build()),

    /**
     * "Enquanto portar um Escudo de Corpo e não efetuar ataques você reduz danos sofridos à zero",
     * for 1 + the holder's Título count many attacks.
     */
    // TODO: nullifying damage outright is a further stage than RD/RA — the gap catalog's
    //  "Damage-type-scoped mitigation, and damage-type immunity" row records that no immunity
    //  mechanism of any kind exists.
    // TODO: "não efetuar ataques" needs attacks made this Turn to be tracked.
    CRIAR_REFUGIO(
            "Enquanto portar um Escudo de Corpo e não efetuar ataques você reduz danos sofridos à "
                    + "zero. O número de ataques que podem ser reduzidos à zero desta forma é "
                    + "igual à 1 + número de Títulos que você possuir.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .requiredFeatCategory(FeatCategory.ESCUDEIRO)
                    .requiredFeatCategoryCount(2)
                    .build()),

    /**
     * "Você recebe Bônus de +2 em suas Defesas enquanto não estiver utilizando suas asas para
     * voar", and those Asas count as a Escudo meanwhile.
     */
    // TODO: "possuir Asas" is a Pré-requisito naming another Talento/racial trait not yet
    //  authored, so it is left unset and this is wrongly open to wingless characters.
    // TODO: flight has no representation, so "não estiver voando" cannot be tested.
    ASAS_ADAMANTINAS(
            "Você recebe Bônus de +2 em suas Defesas enquanto não estiver utilizando suas asas "
                    + "para voar. Para efeitos diversos, como Talentos e Habilidades, suas Asas "
                    + "são consideradas itens do tipo Escudo enquanto você não estiver voando.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você não é beneficiado por RA, RD e RM, ao invés disso você recebe Bônus de +1 em Defesas
     * para cada um destes efeitos."
     */
    // TODO: converting mitigation into Defesa needs DamageService's RD/RA to be suppressible per
    //  character; both are summed unconditionally with no opt-out, and "RM" has no constant at
    //  all. The exchange rate itself is trivial once suppression exists.
    BASTIAO_DE_VIDRO(
            "Você não é beneficiado por efeitos de Redução de Danos Sofridos, ao invés disso você "
                    + "recebe Bônus em Defesa igual ao valor que você receberia de Redução de "
                    + "Danos Sofridos. Você não é beneficiado por RA, RD e RM, ao invés disso você "
                    + "recebe Bônus de +1 em Defesas para cada um destes efeitos.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .requiredFeatCategory(FeatCategory.ESCUDEIRO)
                    .requiredFeatCategoryCount(3)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    EscudeiroFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ESCUDEIRO;
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
