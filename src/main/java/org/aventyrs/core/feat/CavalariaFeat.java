package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Riding;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.dirigirecavalgar.DirigirECavalgarCompetencyAbility;

/**
 * Talentos de Cavalaria — everything a character does while montado ou dirigindo.
 *
 * <p>All five are real (0.0.69) on one shared state: {@link Riding} on the rider's sheet,
 * set by {@code MountService#mount}. While riding, every Perícia but Dirigir e Cavalgar is refused
 * unless the rider holds Ginete ({@code CombatantSheet#isSkillUsePrevented}); the Talentos read the
 * state themselves, or are read by {@code MountService}, which prices montar/desmontar and the mount's
 * Pontos de Ação.
 */
public enum CavalariaFeat implements Feat {

    /**
     * "Você não sofre Desvantagens em rolagens de Perícias baseadas em Força ou Destreza
     * enquanto estiver montado ou dirigindo em função da Habilidade Ginete, ao invés disso
     * recebe Vantagem nestas rolagens."
     *
     * <p><b>Real</b>, and resolved by the Desvantagem's own source: {@code
     * DirigirECavalgarCompetencyAbility#GINETE} reads this Talento and grants Vantagem instead of its
     * Desvantagem, on rolls governed by Força or Destreza while riding
     * ({@code SkillCompetencyAbility#resolveGoverningAttributeRollBonus}).
     */
    GRANDE_GINETE(
            "Você não sofre Desvantagens em rolagens de Perícias baseadas em Força ou Destreza "
                    + "enquanto estiver montado ou dirigindo em função da Habilidade Ginete, ao "
                    + "invés disso recebe Vantagem nestas rolagens de Perícias.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.DIRIGIR_E_CAVALGAR)
                    .requiredSkillGraduation(4)
                    .requiredSkillTrait(DirigirECavalgarCompetencyAbility.GINETE)
                    .build()),

    /**
     * "Você pode interromper o movimento de sua Montaria ou Veículo, realizar outras ações,
     * então continuar o movimento."
     */
    // Real as a permission: MountService#mayInterruptMountMovement. Splitting the movement around
    // the one action is the caller's, since movement in progress is not tracked here.
    DIRECAO_CAOTICA(
            "Você pode interromper o movimento de sua Montaria ou Veículo, realizar outras ações, "
                    + "então continuar o movimento. Apenas uma ação pode ser feita desta forma, "
                    + "independente do seu Tempo de Ação, e apenas se a Ação puder ser concluída "
                    + "no mesmo Turno.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.DIRIGIR_E_CAVALGAR)
                    .requiredSkillGraduation(4)
                    .requiredSkillTrait(DirigirECavalgarCompetencyAbility.GINETE)
                    .build()),

    /**
     * "Enquanto estiver montado ou dirigindo você pode comparar o resultado de suas Rolagens de
     * Ataque Corpo-a-Corpo com as Defesas de um alvo adicional." One dano roll covers both, each
     * taking Meio-Dano.
     *
     * <p>Note the rules text names "Combate Corpo-a-Corpo" where the Perícia is Ataque
     * Corpo-a-Corpo; read as the same Perícia.
     */
    // Real. While riding, an Ataque Corpo-a-Corpo made with anything but a Magia ("apenas ataques
    // físicos", read off the AttackSource) may name one more target, and once it does every target —
    // the primary included — takes Meio-Dano (halvesEveryTargetDamage, applied by AttackDelivery).
    // Adjacency between the targets is the caller's, as for every multi-target attack.
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
                    .build()) {
        @Override
        public int resolveAdditionalTargets(final SkillType attackingSkillType, final Character character,
                                            final CombatantSheet attacker, final AttackSource attackSource) {
            return appliesArco(attackingSkillType, attacker, attackSource) ? 1 : 0;
        }

        @Override
        public boolean halvesEveryTargetDamage(final SkillType attackingSkillType, final AttackSource attackSource,
                                               final CombatantSheet attacker, final int additionalTargets) {
            return additionalTargets > 0 && appliesArco(attackingSkillType, attacker, attackSource);
        }
    },

    /**
     * "Você pode Montar ou Desmontar de sua montaria, ou entrar e sair de um veículo, como Ação
     * Livre. Você também pode Desmontar de sua Montaria como Reação."
     */
    // Real: MountService#mount/#dismount price it as an Ação Livre, or a dismount as a Reação, once
    // per Turn (⚠️ "uma vez a cada Rodada" counted per Turn); otherwise MountService#DEFAULT_MOUNT_COST.
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
    // Real: MountService#getMountMovementActionPointCost (2PA of the mount) and
    // #getMountActionAllowance (min(Títulos, the mount's PA left)), read off Riding#steed's sheet.
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

    /** Ataque em Arco's conditions: Ataque Corpo-a-Corpo, riding, and not a Magia. */
    private static boolean appliesArco(final SkillType skill, final CombatantSheet attacker, final AttackSource source) {
        return skill == SkillType.ATAQUE_CORPO_A_CORPO && attacker != null && attacker.isRiding()
                && !(source instanceof Spell);
    }

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
