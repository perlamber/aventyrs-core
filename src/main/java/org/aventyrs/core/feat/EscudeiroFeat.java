package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.ShieldItem;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.SkillType;

import java.util.List;

/**
 * Talentos de Escudeiro — fighting with, and behind, a shield.
 *
 * <p><b>A Escudo is classifiable now</b>: {@code ItemCategory#SHIELD} and the {@link ShieldItem}
 * catalog carry the kind and the weight tier (Leve/Médio/Pesado — Escudo de Corpo is {@link
 * ShieldItem#ESCUDO_DE_CORPO}), and an equipped copy knows its catalog entry ({@code
 * AbstractItem#getTemplate()}). {@link #ESPECIALISTA_EM_ESCUDO} is real through that.
 *
 * <p>One blocker still runs through half the tree: <b>a Escudo is not a {@code Weapon}</b>, so
 * the "Ataque com Escudo" that {@link #ATACAR_COM_ESCUDOS} creates and five other constants build
 * on has no {@code DamageBase}, no attacking Perícia and nothing an {@code AttackSource} could
 * name. Per-constant TODOs name only what is <em>additional</em> to that.
 */
public enum EscudeiroFeat implements Feat {

    /**
     * "Sempre que iniciar um combate com um Escudo de Categoria Média ou Pesada em mãos, sua
     * iniciativa aumenta em +2."
     *
     * <p><b>Real</b>, through {@link Feat#resolveInitiativeBonus}: an equipped {@code
     * ItemCategory#SHIELD} whose authored weight is {@code ItemWeightClass#MEDIUM} or {@code
     * HEAVY}. Iniciativa is read when it is rolled, so "ao iniciar um combate" is judged then.
     */
    ESCUDO_VELOZ(
            "Sempre que iniciar um combate com um Escudo de Categoria Média ou Pesada em mãos, sua "
                    + "iniciativa aumenta em +2.",
            FeatRequirements.builder().build()) {
        @Override
        public int resolveInitiativeBonus(final Character character) {
            boolean heavyShield = character.getEquipment().stream()
                    .anyMatch(item -> item.getCategory() == ItemCategory.SHIELD
                            && (item.getWeightClass() == ItemWeightClass.MEDIUM
                            || item.getWeightClass() == ItemWeightClass.HEAVY));
            return heavyShield ? ESCUDO_VELOZ_INITIATIVE_BONUS : 0;
        }
    },

    /**
     * "Você recebe +1 em suas Defesas enquanto utilizar um item escolhido do tipo 'Escudo'",
     * rising to +2 at 4 Graduações em Esquiva e Aparar and +3 at 7.
     *
     * <p><b>Real</b>, through {@link EspecialistaEmEscudoFeat} — the acquired form recording the
     * chosen {@link ShieldItem}, granted in place of this constant. The bonus holds while a copy
     * of that Escudo is equipped.
     */
    ESPECIALISTA_EM_ESCUDO(
            "Você recebe +1 em suas Defesas enquanto utilizar um item escolhido do tipo ‘Escudo’. "
                    + "Escolha um item do tipo ‘Escudo’, se tiver 4 ou mais graduações em ‘Esquiva "
                    + "e Aparar’ enquanto estiver utilizando o item escolhido este Bônus aumenta "
                    + "para +2, se tiver 7 ou mais Graduações este Bônus aumenta para +3.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(1)
                    .build()) {
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(FeatChoice.ofOne(ShieldItem.class, List.of(ShieldItem.values())));
        }
    },

    /**
     * "Você pode usar seu escudo para atacar, se o fizer você perde metade dos bônus em Defesas
     * concedidos por ele." Shield attacks roll at half Esquiva e Aparar plus the shield's own
     * defensive bonuses, and deal 1d6+1/+2/+3 by weight.
     */
    // TODO: a shield is not a Weapon, so it has no DamageBase and no attacking SkillType — the
    //  three authored figures (Leve 1d6+1, Médio 1d6+2, Pesado 1d6+3) are exactly DamageBase.of
    //  values keyed by ShieldItem#getWeightClass, but nothing lets a ShieldItem be swung.
    // TODO: an attack roll built from half a Graduação plus the item's own Defesa bonuses is a
    //  bespoke formula no Interaction can express; AbstractSkillInteraction reads the Perícia's
    //  own governing Attribute and Graduação.
    // TODO: "Margem Crítica Menor 17" and "Sucesso Crítico: Atordoante" are Weapon columns
    //  (getLesserCriticalMargin/getCriticalEffect) — they land for free once a shield can be one.
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
    // TODO: both facts it reads exist now — CombatantSheet#getMovementsTakenThisRound /
    //  getRepositionsTakenThisRound (reset at startTurn), and "um escudo que você seja
    //  especialista" via EspecialistaEmEscudoFeat#isUsingChosenShield. What is missing is the
    //  moment: the +2 is earned when the Turn *ends* without movement and lasts 1 Rodada, and
    //  Feat has no end-of-Turn trigger to grant a Blessing from.
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
    // TODO: everything but the turn-order position is reachable now —
    //  Feat#resolveDefenseBonus(DefenseType, Character, SceneContext) takes a SceneContext (see
    //  AnaoFeat#VANTAGEM_DE_TAMANHO), the round window is SceneContext#isWithinFirstCombatRounds(2)
    //  and "ganhou a iniciativa" has a resolver. What blocks it is "não for o primeiro"/"o último
    //  a agir": turn-order position is not live on Scene, and the two branches grant different
    //  amounts (+3 vs +5), so neither can be picked.
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
    // TODO: Feat#resolveCriticalMarginIncrease takes the AttackSource, but an Ataque com Escudo
    //  has none — a Escudo is not a Weapon (see the enum javadoc).
    // TODO: Corrente de Efeitos – Rugido — Feat#resolveEffectChains is the hook, but Rugido is not
    //  an authored EffectChain, and the hook adds to every landed attack rather than to criticals.
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
    // TODO: Feat#resolveDifficultyReduction is real and scanned by
    //  AbstractSkillInteraction#sumFeatDifficultyReductions, but it is documented for an
    //  *unconditional* reduction on a named Perícia.
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
    // TODO: both conditions are readable now — Escudo de Corpo is ShieldItem#ESCUDO_DE_CORPO, and
    //  "não efetuar ataques" is CombatantSheet#hasActedOffensivelyThisRound. What is missing is
    //  the effect: reducing a hit to zero for a budget of 1 + Títulos attacks. That needs a
    //  per-hit consult in DamageServiceImpl like sheet.PeleDePedra's, not a stat.
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
    // The +2 Defesas is real, read off EnvironmentalState#flying on the holder's SceneContext —
    // a null context (no Scene to ask) grants nothing, per the "cannot tell" rule.
    // TODO: "suas Asas são consideradas itens do tipo Escudo" — every Escudo clause reads the
    //  equipment list, and Asas are no Item.
    ASAS_ADAMANTINAS(
            "Você recebe Bônus de +2 em suas Defesas enquanto não estiver utilizando suas asas "
                    + "para voar. Para efeitos diversos, como Talentos e Habilidades, suas Asas "
                    + "são consideradas itens do tipo Escudo enquanto você não estiver voando.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character,
                                        final SceneContext sceneContext) {
            return sceneContext != null && !sceneContext.getEnvironmentalState().flying()
                    ? ASAS_ADAMANTINAS_DEFENSE_BONUS : 0;
        }
    },

    /**
     * "Você não é beneficiado por RA, RD e RM, ao invés disso você recebe Bônus de +1 em Defesas
     * para cada um destes efeitos."
     */
    // TODO: converting mitigation into Defesa needs DamageService's RD/RA/RM to be suppressible
    //  per character; all three are real stats now (ModifierType.MAGIC_REDUCTION closed the RM
    //  half) but each is summed unconditionally with no opt-out. The exchange rate itself is
    //  trivial once suppression exists.
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

    /** ESCUDO_VELOZ's "sua iniciativa aumenta em +2". */
    private static final int ESCUDO_VELOZ_INITIATIVE_BONUS = 2;

    /** ASAS_ADAMANTINAS' "Bônus de +2 em suas Defesas" while not flying. */
    private static final int ASAS_ADAMANTINAS_DEFENSE_BONUS = 2;

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
