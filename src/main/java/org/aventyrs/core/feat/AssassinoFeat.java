package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos de Assassino — critical hits, opening strikes, and finishing a wounded target.
 *
 * <p>One gap dominates this tree and is not repeated on every constant: <b>{@code
 * resolveCriticalMarginIncrease} does not exist on {@link Feat}</b>. It lives on {@code
 * EgoAdvantage}/{@code AttributeAbility}/{@code SkillCompetencyAbility}, and {@code
 * AbstractSkillInteraction#sumCriticalMarginIncrease} scans those three sources only — {@code
 * character.getFeats()} is not among them. Six constants here turn on that one hook.
 *
 * <p>Promoting it would not by itself finish them, which is why none is half-wired: every clause
 * below widens the Margem Crítica <b>Menor</b> specifically, while {@code
 * SkillRoll#getCriticalResult(int)} takes one margin that widens the Menor and Maior tiers
 * together. Granting through the existing hook would widen both, over-granting on every one of
 * them.
 */
public enum AssassinoFeat implements Feat {

    /**
     * "Escolha entre um Tipo de Arma ou Conjuração de Magias. Sua Margem Crítica Menor com o tipo
     * de arma escolhida, ou das magias que você conjurar, é aumentada em +1."
     *
     * <p>The root of the tree, and the one Talento in this catalog with <b>no Pré-requisito at
     * all</b> — a genuinely unrestricted acquisition, not an omission in the source.
     */
    // TODO: needs resolveCriticalMarginIncrease on Feat, and a Menor-only margin — see the
    //  enum's own javadoc for both.
    // TODO: carries an acquisition-time choice (a Tipo de Arma, or Conjuração de Magias) with no
    //  representation. The choice space is not small and fixed — "Tipo de Arma" is an open set —
    //  so this is CLAUDE.md's AcquiredChoice pattern, which still has no consuming mechanism.
    ACERTO_CRITICO_APRIMORADO(
            "Escolha entre um Tipo de Arma ou Conjuração de Magias. Sua Margem Crítica Menor com o "
                    + "tipo de arma escolhida, ou das magias que você conjurar, é aumentada em +1.",
            FeatRequirements.builder().build()),

    /**
     * "Você pode sacar uma arma como Ação Livre, a primeira rolagem de Perícia de Ataque que
     * realizar neste mesmo turno será feita em Desvantagem."
     */
    // TODO: needs a notion of drawing a weapon — Character#equipment is a flat list with no
    //  sheathed/drawn state, so "sacar" has nothing to change.
    // TODO: applying Skill#DISADVANTAGE_MALUS to only the Turn's first Ataque roll needs a
    //  per-Turn roll counter scoped to a Perícia group; consumeFirstRollThisTurn is per
    //  AttributeDomain and already spoken for.
    SAQUE_RAPIDO(
            "Você pode sacar uma arma como Ação Livre, a primeira rolagem de Perícia de Ataque que "
                    + "realizar neste mesmo turno será feita em Desvantagem.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.DEXTERITY)
                    .requiredAttributeValue(2)
                    .build()),

    /**
     * "A cada Rodada, a primeira vez que fizer um ataque utilizando apenas 1PA ou Ação Livre a GD
     * da Perícia de Ataque é reduzida em -1 nível."
     */
    // TODO: needs the attack to know what it cost — neither SkillRoll nor AttackDelivery carries
    //  the Pontos de Ação spent or the action type used.
    // TODO: a Feat cannot reduce a roll's GD — getDifficultyReduction() is a
    //  SkillCompetencyAbility/SkillExcellency hook, and AbstractSkillInteraction does not scan
    //  Talentos for it.
    // TODO: its Pré-requisito is a disjunction ("Destreza 3 e Saque Rápido, *ou* Foco 5") which
    //  FeatRequirements cannot express — every set clause is combined with and. Modelled here as
    //  the Destreza branch only, so the Foco 5 route is wrongly refused.
    // TODO: carries an acquisition-time choice (Armas ou Magias) — see ACERTO_CRITICO_APRIMORADO.
    SAQUE_RELAMPAGO(
            "Escolha entre Armas ou Magias. A cada Rodada, a primeira vez que fizer um ataque "
                    + "utilizando apenas 1PA ou Ação Livre a GD da Perícia de Ataque é reduzida em "
                    + "-1 nível. Se este ataque for realizado imediatamente após sacar sua primeira "
                    + "arma, ou for a primeira magia conjurada, na Cena de Combate, você também "
                    + "recebe Vantagem nesta Rolagem.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.DEXTERITY)
                    .requiredAttributeValue(3)
                    .requiredFeat(SAQUE_RAPIDO)
                    .build()),

    /**
     * "Você pode guardar sua arma atual como uma Reação."
     */
    // TODO: needs sheathed/drawn weapon state — see SAQUE_RAPIDO.
    // TODO: "apenas uma vez a cada Rodada" needs a per-Rodada activation counter.
    TROCA_DE_ARMA_VELOZ(
            "Você pode guardar sua arma atual como uma Reação. Este Talento pode ser usado apenas "
                    + "uma vez a cada Rodada. Não é possível usar o Talento Saque Rápido para "
                    + "sacar uma arma guardada usando este Talento.",
            FeatRequirements.builder()
                    .requiredFeat(SAQUE_RAPIDO)
                    .build()),

    /** "Seus ataques recebem 'Sangramento' como Efeito Crítico adicional." */
    // TODO: granting an Efeito Crítico needs a hook on the attack path — CriticalEffect instances
    //  are assembled by the caller and filtered by the victim's immunities; nothing consults the
    //  attacker's Talentos for extra ones.
    ABRIR_FERIDAS(
            "Seus ataques recebem ‘Sangramento’ como Efeito Crítico adicional.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.STRENGTH)
                    .requiredAttributeValue(3)
                    .requiredFeat(ACERTO_CRITICO_APRIMORADO)
                    .build()),

    /**
     * "A primeira magia conjurada a cada Rodada utilizando apenas 1PA ou Ação Livre tem a Margem
     * Crítica Menor aumentada em +1."
     */
    // TODO: Margem Crítica hook — see the enum's own javadoc.
    // TODO: needs the cast to know its Pontos de Ação cost — see SAQUE_RELAMPAGO.
    // TODO: its Pré-requisito names a *specific choice* of another Talento ("Acerto Crítico
    //  Aprimorado (Magias)"); FeatRequirements can require the Talento but not which branch of
    //  its acquisition-time choice was taken.
    ACERTO_CRITICO_ARCANO(
            "A primeira magia conjurada a cada Rodada utilizando apenas 1PA ou Ação Livre tem a "
                    + "Margem Crítica Menor aumentada em +1.",
            FeatRequirements.builder()
                    .requiredFeat(SAQUE_RELAMPAGO)
                    .build()),

    /**
     * "A Margem Crítica Menor aumenta em +2 em seu primeiro ataque de cada cena", dropping to +1
     * on each Rodada's first attack from the second Rodada on.
     */
    // TODO: Margem Crítica hook — see the enum's own javadoc.
    // TODO: "imediatamente após sacar Armas do tipo escolhida" needs both drawn-weapon state and
    //  the prerequisite Talento's own recorded choice — see ACERTO_CRITICO_ARCANO.
    ACERTO_CRITICO_RELAMPAGO(
            "A Margem Crítica Menor aumenta em +2 em seu primeiro ataque de cada cena. Este "
                    + "benefício é válido apenas se o ataque for realizado imediatamente após "
                    + "sacar Armas do tipo escolhida no talento ‘Acerto Crítico Aprimorado’. A "
                    + "partir da segunda Rodada do combate, seu primeiro ataque de cada Rodada com "
                    + "esta mesma arma tem a Margem Crítica Menor aumentada em +1.",
            FeatRequirements.builder()
                    .requiredFeat(SAQUE_RELAMPAGO)
                    .build()),

    /**
     * "Você recebe Vantagem em rolagens de Danos em alvos que já tenham perdido pelo menos a
     * metade de seus PV."
     */
    // TODO: a Vantagem on a *dano* roll has no hook — DamageBonus is a flat value with no
    //  advantage concept, and Feat has no dano-bonus hook at all.
    // TODO: the target's PV fraction is resolvable (HitPointsService), but no Feat hook receives
    //  an attack target to test it against.
    GOLPE_DE_FINALIZACAO(
            "Você recebe Vantagem em rolagens de Danos em alvos que já tenham perdido pelo menos a "
                    + "metade de seus PV. Se possuir 7 ou mais graduações na Perícia de Ataque "
                    + "utilizada, as Margens Críticas Menor e Maior de seus Golpes de Finalização "
                    + "aumentam em +1.",
            FeatRequirements.builder()
                    .requiredFeatCategory(FeatCategory.ASSASSINO)
                    .requiredFeatCategoryCount(3)
                    .build()),

    /**
     * "Você pode gastar 1PM, se o fizer sua rolagem de ataque será efetuada contra a DM do alvo,
     * ao invés da DF."
     */
    // TODO: needs a PM spend that modifies a single roll — the gap catalog's "One-time roll
    //  effects bought with a resource"; PV/PM/PD spends have no transaction or reaction path.
    // TODO: redirecting an attack from DF to DM is not expressible — AttackDelivery picks the
    //  Defesa from the attack itself, with no hook to override which DefenseType is compared.
    // TODO: granting a Corrente de Efeitos to a critical — see ABRIR_FERIDAS.
    GOLPE_SOBRENATURAL(
            "Sempre que realizar Golpes de Finalização você pode gastar 1PM, se o fizer sua "
                    + "rolagem de ataque será efetuada contra a DM do alvo, ao invés da DF, este "
                    + "ataque causa Danos Mágicos. Acertos Críticos de Golpes Sobrenaturais "
                    + "recebem a Corrente de Efeitos – Escancarar Defesas. Este Talento pode ser "
                    + "utilizado apenas 1 vez a cada Rodada.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.FOCUS)
                    .requiredAttributeValue(2)
                    .requiredFeat(GOLPE_DE_FINALIZACAO)
                    .build()),

    /**
     * "Você pode gastar +2PM, se o fizer poderá realizar um novo Golpe Sobrenatural contra o mesmo
     * alvo como uma Ação Livre."
     */
    // TODO: same PM-spend gap as GOLPE_SOBRENATURAL, plus granting an extra attack, which nothing
    //  in this core can express — an attack is initiated by a caller, never by a resolution.
    // TODO: Margem Crítica hook — see the enum's own javadoc.
    GOLPE_SOMBRA_SOBRENATURAL(
            "Após ser bem-sucedido em realizar um Golpe Sobrenatural você pode gastar +2PM, se o "
                    + "fizer poderá realizar um novo Golpe Sobrenatural contra o mesmo alvo como "
                    + "uma Ação Livre. Sempre que efetuar um segundo Golpe Sobrenatural contra um "
                    + "mesmo alvo na mesma Rodada, este segundo ataque tem a Margem Crítica Menor "
                    + "e Maior aumentadas em +1 número. Golpe Sombra Sobrenatural pode ser "
                    + "utilizado apenas em seu Turno e apenas uma vez por Rodada.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.FOCUS)
                    .requiredAttributeValue(3)
                    .requiredFeat(GOLPE_SOBRENATURAL)
                    .build()),

    /**
     * "Você recebe Bônus de +3 em suas Defesas e RDS enquanto estiver escondido ou invisível."
     */
    // TODO: needs an escondido/invisível condition — this core has no status-condition system,
    //  and Furtividade resolves a roll rather than setting a lasting state.
    // TODO: its Pré-requisito names the Talento 'Corrida Furtiva', which belongs to another tree
    //  and is wired once that tree is authored.
    ESCUDO_DE_SOMBRAS(
            "Você recebe Bônus de +3 em suas Defesas e RDS enquanto estiver escondido ou "
                    + "invisível.",
            FeatRequirements.builder().build()),

    /**
     * "Sempre que derrotar um inimigo, reduzindo seus PV à 0 ou menos, para este Turno você
     * recebe +1PA e então +1PA para cada Título Aventyr que possuir."
     */
    // TODO: needs a defeat trigger — nothing reports "this attack reduced a target to 0 PV" back
    //  to the attacker, and this codebase has no observer mechanism anywhere.
    // TODO: the grant itself would be a Blessing of ModifierType.ACTION_POINTS scoped to this
    //  Turn; the amount is computable, only the trigger is missing.
    SANGUE_QUENTE(
            "Sempre que derrotar um inimigo, reduzindo seus PV à 0 ou menos, para este Turno você "
                    + "recebe +1PA e então +1PA para cada Título Aventyr que possuir.",
            FeatRequirements.builder()
                    .requiredFeat(GOLPE_DE_FINALIZACAO)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você não recebe Vantagem em Danos em seus Acertos Críticos, ao invés disso recebe Bônus de
     * +1d6."
     */
    // TODO: needs the critical-dano Vantagem this replaces, which itself has no representation.
    // TODO: the Movimento half needs the defeat trigger (see SANGUE_QUENTE) and a movement scoped
    //  to approaching enemies, which this core cannot express — it never does geometry.
    VIOLENCIA_DESCOMUNAL(
            "Você não recebe Vantagem em Danos em seus Acertos Críticos, ao invés disso recebe "
                    + "Bônus de +1d6. Sempre que eliminar um alvo com um Acerto Crítico, seu "
                    + "Movimento Base aumenta em +dobro do Número de Títulos Brutos Despertos, mas "
                    + "apenas para se aproximar de personagens inimigos.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.STRENGTH)
                    .requiredAttributeValue(5)
                    .requiredAwakenedTitles(1)
                    .build()),

    /** "O Dano Base de suas Armas Tecnológicas aumenta em +1." */
    // TODO: needs an Arma Tecnológica classification on Item — ItemCategory carries no such
    //  value, so the Dano Base half would apply to every weapon rather than only those.
    // TODO: the Danos Críticos half has no hook — see GOLPE_DE_FINALIZACAO.
    // TODO: its Pré-requisito names a required Especialização (Profissão/Mecânica);
    //  FeatRequirements models a Habilidade de Competência but not a SkillSpecialization.
    ESPECIALISTA_TECNOLOGICO(
            "O Dano Base de suas Armas Tecnológicas aumenta em +1. Você recebe Bônus de +3 em "
                    + "rolagens de Danos Críticos de Armas Tecnológicas para cada Título Aventyr "
                    + "Especialista Desperto.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(4)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Suas Magias ofensivas têm a Margem Crítica Menor aumentada em 1 + o Número de Títulos
     * Arcanos Despertos."
     */
    // TODO: Margem Crítica hook — see the enum's own javadoc. The Título count is narrowed to
    //  TitleArchetype.ARCANO, which is expressible; the hook to spend it on is not.
    // TODO: "Bônus cumulativo de +1 em Conjuração até o fim da Cena" needs the defeat trigger
    //  (see SANGUE_QUENTE) and a Conjuração bonus reaching SpellCastingService, which the gap
    //  catalog's "Item numeric columns" row already records as unread.
    ARCANISMO_AVASSALADOR(
            "Suas Magias ofensivas têm a Margem Crítica Menor aumentada em 1 + Número de Títulos "
                    + "Arcanos Despertos. Sempre que eliminar um alvo com um Acerto Crítico você "
                    + "recebe Bônus cumulativo de +1 em Conjuração até o fim da Cena.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(7)
                    .requiredFeat(ACERTO_CRITICO_APRIMORADO)
                    .requiredAwakenedTitles(1)
                    .requiredTitleArchetype(org.aventyrs.core.title.TitleArchetype.ARCANO)
                    .build()),

    /**
     * "Seu primeiro Acerto Crítico em rolagem de Perícia de Ataque de cada Rodada recebe Roubo de
     * Vida 1."
     */
    // TODO: LifeStealService exists, but nothing scopes Roubo de Vida to one roll per Rodada, and
    //  no Feat hook grants it.
    // TODO: "Roubo de Bônus Base" is a third steal type; only Roubo de Vida exists (the gap
    //  catalog's "Roubo de Mana / de Determinação" row).
    // TODO: "Raça Renascida" has no Race class — requiredRace is left unset, so this is wrongly
    //  open to every race.
    BANQUETEAR_SE(
            "Seu primeiro Acerto Crítico em rolagem de Perícia de Ataque de cada Rodada recebe "
                    + "Roubo de Vida 1. Caso possua 2 Títulos Aventyrs Despertos ao invés de Roubo "
                    + "de Vida você recebe Roubo de Bônus Base 1.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    AssassinoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ASSASSINO;
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
