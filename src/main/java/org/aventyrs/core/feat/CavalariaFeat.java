package org.aventyrs.core.feat;

import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.dirigirecavalgar.DirigirECavalgarCompetencyAbility;

/**
 * Talentos de Cavalaria — everything a character does while montado ou dirigindo.
 *
 * <p>All five are catalog entries with real Pré-requisitos and no mechanical effect, blocked on
 * one shared gap: <b>this core models no montaria or veículo</b>. A mount is a separate
 * combatant with its own Pontos de Ação and Movimento, and nothing anywhere expresses "the
 * character is currently riding one" — so neither the state these Talentos condition on nor the
 * resources they spend exist. That is a genuinely missing system, not a missing reader, so none
 * of these grants a partial effect.
 */
public enum CavalariaFeat implements Feat {

    /**
     * "Você não sofre Desvantagens em rolagens de Perícias baseadas em Força ou Destreza
     * enquanto estiver montado ou dirigindo em função da Habilidade Ginete, ao invés disso
     * recebe Vantagem nestas rolagens."
     *
     * <p>The Vantagem half is an ordinary {@code Skill#ADVANTAGE_BONUS}, but it is scoped two
     * ways this core cannot express at once — to rolls of a whole {@code AttributeDomain} rather
     * than a named Perícia, and only while mounted.
     */
    // TODO: needs a montaria/veículo concept before "enquanto estiver montado" can be tested.
    // TODO: a bonus scoped to "Perícias baseadas em Força ou Destreza" has no hook — every roll
    //  bonus here is either broad (SKILL_ROLL_BONUS) or one named Perícia's own rollBonusType;
    //  neither expresses "whichever Perícias that Attribute currently governs", which is itself
    //  live data once a substitution ability is held.
    // TODO: cancelling a Desvantagem is not modelled either — Skill#DISADVANTAGE_MALUS is
    //  applied by a caller, and nothing can suppress one.
    GRANDE_GINETE(
            "Você não sofre Desvantagens em rolagens de Perícias baseadas em Força ou Destreza "
                    + "enquanto estiver montado ou dirigindo em função da Habilidade Ginete, ao "
                    + "invés disso recebe Vantagem nestas rolagens de Perícias.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.DIRIGIR_E_CAVALGAR)
                    .requiredSkillGraduation(4)
                    .requiredSkillCompetencyAbility(DirigirECavalgarCompetencyAbility.GINETE)
                    .build()),

    /**
     * "Você pode interromper o movimento de sua Montaria ou Veículo, realizar outras ações,
     * então continuar o movimento."
     */
    // TODO: needs a montaria/veículo concept, and a notion of a movement that can be split
    //  around another action — MovementService returns a figure per Ponto de Ação and tracks
    //  no movement in progress.
    DIRECAO_CAOTICA(
            "Você pode interromper o movimento de sua Montaria ou Veículo, realizar outras ações, "
                    + "então continuar o movimento. Apenas uma ação pode ser feita desta forma, "
                    + "independente do seu Tempo de Ação, e apenas se a Ação puder ser concluída "
                    + "no mesmo Turno.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.DIRIGIR_E_CAVALGAR)
                    .requiredSkillGraduation(4)
                    .requiredSkillCompetencyAbility(DirigirECavalgarCompetencyAbility.GINETE)
                    .build()),

    /**
     * "Enquanto estiver montado ou dirigindo você pode comparar o resultado de suas Rolagens de
     * Ataque Corpo-a-Corpo com as Defesas de um alvo adicional." One dano roll covers both, each
     * taking Meio-Dano.
     *
     * <p>Note the rules text names "Combate Corpo-a-Corpo" where the Perícia is Ataque
     * Corpo-a-Corpo; read as the same Perícia.
     */
    // TODO: needs a montaria/veículo concept — "enquanto estiver montado ou dirigindo" is the
    //  only thing still blocking this constant. Multi-target attack resolution is built now
    //  (Feat#resolveAdditionalTargets, AttackTargetingService, DeliveredAttack#additionalTargets),
    //  and it already does everything this Talento's own mechanics need: one roll compared
    //  against each target's Defesa, one dano roll, Meio-Dano on the extra target. What it can't
    //  yet be gated on is the mount. Note the two differences from ARTE_FLUIDA when this lands:
    //  the Meio-Dano applies to *every* target including the primary ("em cada alvo"), which
    //  DeliveredAttackTargetResult's per-target halving does not currently express for the
    //  primary; and "apenas ataques físicos" needs the attack's DamageType, which the delivery
    //  path never carries.
    ATAQUE_EM_ARCO(
            "Enquanto estiver montado ou dirigindo você pode comparar o resultado de suas Rolagens "
                    + "de Ataque Corpo-a-Corpo com as Defesas de um alvo adicional e que estejam "
                    + "adjacentes entre si, você deve fazer uma única rolagem de danos para todos "
                    + "os alvos, o valor dos danos causados em cada alvo é igual a metade do valor "
                    + "rolado (Efeito de Meio-Dano) independentemente do número de alvos "
                    + "atingidos. Apenas ataques físicos podem ser beneficiados por este Talento.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(4)
                    .requiredFeat(GRANDE_GINETE)
                    .build()),

    /**
     * "Você pode Montar ou Desmontar de sua montaria, ou entrar e sair de um veículo, como Ação
     * Livre. Você também pode Desmontar de sua Montaria como Reação."
     */
    // TODO: needs a montaria/veículo concept. Note this changes what an action *costs*, not how
    //  many Ações Livres the character has — FreeActionsService is the wrong hook for it.
    // TODO: "apenas uma vez a cada Rodada" needs a per-Rodada activation counter; CharacterSheet
    //  tracks Round-scoped TemporaryEffects, not activation counts.
    MONTAR_E_DESMONTAR_INSTANTANEO(
            "Você pode Montar ou Desmontar de sua montaria, ou entrar e sair de um veículo, como "
                    + "Ação Livre. Você também pode Desmontar de sua Montaria como Reação. Este "
                    + "Talento pode ser utilizado apenas uma vez a cada Rodada.",
            FeatRequirements.builder()
                    .requiredFeat(GRANDE_GINETE)
                    .build()),

    /**
     * "Os movimentos com sua montaria consomem apenas 2PA delas, permitindo a elas efetuarem
     * outras ações com os PA restante." The mount's action allowance scales with the holder's
     * own Títulos Aventyr Despertos.
     */
    // TODO: needs a montaria/veículo concept — this spends the *mount's* Pontos de Ação, and a
    //  mount has no sheet to spend from.
    MONTARIA_DE_COMBATE(
            "Os movimentos com sua montaria consomem apenas 2PA delas, permitindo a elas "
                    + "efetuarem outras ações (que não sejam de movimento) com os PA restante, se "
                    + "possuírem. O máximo de ações que uma montaria pode realizar em seu Turno é "
                    + "igual à quantidade de Títulos Aventyr Despertos que você possuir, mas "
                    + "sempre limitados a quantidade de Pontos de Ação da montaria.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.DIRIGIR_E_CAVALGAR)
                    .requiredSkillGraduation(4)
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    CavalariaFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.CAVALARIA;
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
