package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.SpellData;
import org.aventyrs.core.magic.SpellDuration;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * FÚRIA DE TESLA (Encantamento/Elemental: Eletricidade) — seven Magias, one of only two trees to
 * diverge as late as Emergente, and one of eight that never converge.
 *
 * <p>Every rung past Muda is gated on Abraço de Tesla being active, and three of them read its
 * remaining Duração as a damage multiplier or spend it outright. That makes it the tree where
 * {@code SpellDuration.sameAs} matters most — Aura Chocante's Duração <em>is</em> Abraço de
 * Tesla's, live, and its Efeito Crítico is annotated "(afeta Duração do Abraço de Tesla)" rather
 * than its own.
 *
 * <p>Its three attack Perícias are the widest spread of any tree: Domínio do Mana at four rungs,
 * Ataque Corpo-a-Corpo at two, Ataque à Distância at one.
 */
public enum FuriaDeTeslaSpell implements AuthoredSpell {

    MAGNETIZAR(SpellData.builder()
            .name("Magnetizar")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Faz com que um equipamento adquira propriedades magnéticas")
            .primaryEffectDescription("Você pode tocar um equipamento metálico e magnetizá-lo, equipamentos "
                    + "magnéticos podem atrair ou serem atraídos por outros de metal. "
                    + "Uma arma ou armadura tocada será atraída por armaduras e escudos, facilitando acertar o alvo, "
                    + "concedendo Vantagem as rolagens de ataque de seu portador. "
                    + "Armaduras e Escudos tocados atraem ataques de armas metálicas, dificultando a esquiva, "
                    + "concedendo Redutor de -2 nas Defesas do alvo para evitar ataques sofridos por armas "
                    + "metálicas.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(2))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * "Se o alvo desta magia estiver em posse de qualquer equipamento de metal você poderá tocá-lo
     * a distância" — a Magia de Toque whose reach conditionally becomes Distância Curta. That is
     * not a second {@code SpellTargeting} (which is the dual-reach {@code Pessoal ou Toque} shape,
     * a free choice); it is conditional on the target's equipment, so it stays prose.
     */
    TOQUE_DE_TESLA(SpellData.builder()
            .name("Toque de Tesla")
            .branchLevel(BranchLevel.BROTO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Você pode encantar sua mão com energia elétrica, causando danos com seu toque.")
            .primaryEffectDescription("O alvo tocado sofre 1d6+Metade do Foco pontos de Dano Mágico Elemental. "
                    + "Se o alvo desta magia estiver em posse de qualquer equipamento de metal você poderá tocá-lo a "
                    + "distância, afetando-o mesmo que esteja em Distância Curta.")
            .criticalEffectType(CriticalEffectType.ATORDOANTE)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The state every deeper rung requires, and whose Duração three of them read. Its Corrente is
     * written inside its Efeito rather than on its own {@code Corrente de Efeitos –} line, and is
     * granted on the caster's own Ataque or Esquiva e Aparar rolls rather than on this Magia's
     * delivery roll — so it is transcribed in the prose and left off {@code
     * getEffectChainDescription()}, which is the "superar a DM do alvo em 5" descriptor.
     */
    ABRACO_DE_TESLA(SpellData.builder()
            .name("Abraço de Tesla")
            .branchLevel(BranchLevel.MUDA)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Você encanta todo o seu corpo com energia elétrica, amplificando seus ataques e "
                    + "aprimorando suas defesas.")
            .primaryEffectDescription("Você recebe Bônus em Defesas igual à metade do seu Foco, adicionalmente você "
                    + "recebe Vantagem em suas rolagens de Ataque e Dano. "
                    + "Enquanto sob efeito do Abraço de Tesla, sempre que você efetuar uma rolagem de Perícia de "
                    + "Ataque ou Esquiva e Aparar, você recebe a Corrente de Efeitos - Brilhantismo de Tesla: Seu "
                    + "alvo ou atacante sofre 2 pontos de Dano Mágico Elemental: Eletricidade, se ele estiver em "
                    + "posse de equipamentos de metal, ao invés disso, o dano muda para 1d6.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /**
     * Its {@code Efeito Crítico:} line reads "Potencializar (afeta Duração do Abraço de Tesla)" —
     * the type is Potencializar, and the parenthesis says which Duração the +2d6 unidades land on,
     * which is not this Magia's own. Authored as the type; the redirection has nowhere to live,
     * and the {@code sameAs} reference is what makes it come out right anyway.
     */
    AURA_CHOCANTE(SpellData.builder()
            .name("Aura Chocante")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.FURIA_DE_TESLA_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("seu encantamento libera uma aura mágica elétrica que causa danos a todos os personagens "
                    + "próximos")
            .primaryEffectDescription("Esta magia pode ser conjurada apenas se você estiver sob efeito de Abraço de "
                    + "Tesla. "
                    + "No início de cada um de seus turnos todos os personagens em Distância Curta sofrem Metade do "
                    + "Foco pontos de Dano Mágico Elemental: Eletricidade.")
            .effectChainDescription("Sagacidade de Tesla: Apenas personagens inimigos sofrem danos por esta magia.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.sameAs(() -> ABRACO_DE_TESLA.getDuration()))
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /**
     * "+3PA" is a real {@code ModifierType#ACTION_POINTS} {@code TemporaryBonus}, which the
     * {@code CombatantSheet}-taking {@code ActionPointsService#getMaxActionPoints} overload reads
     * for real. TODO "nesta Rodada suas ações não provocam Reações" is the movement-triggers-Reação
     * gap — a clause exempting actions from provoking Reações is currently exempt from nothing.
     * TODO "apenas uma vez a cada Rodada" is a within-Round activation counter.
     */
    PASSO_RELAMPEJANTE(SpellData.builder()
            .name("Passo Relampejante")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.FURIA_DE_TESLA_ALTERNATIVO)
            .activationTime(ActivationTime.ACAO_LIVRE)
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Você se torna extremamente veloz por um breve momento.")
            .primaryEffectDescription("Esta magia pode ser conjurada apenas se você estiver sob efeito de Abraço de "
                    + "Tesla e apenas uma vez a cada Rodada. "
                    + "Você recebe temporariamente +3PA e nesta Rodada suas ações não provocam Reações.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /** "perdem -2PA por 2 Rodadas (até o mínimo de 2PA)" is a negative ACTION_POINTS bonus with a floor of 2, not the 0 every service clamps at. */
    NOVA_CHOCANTE(SpellData.builder()
            .name("Nova Chocante")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.FURIA_DE_TESLA_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Uma poderosa explosão de energia em forma de anel capaz de causar grandes danos e aturdir "
                    + "seus inimigos.")
            .primaryEffectDescription("Esta magia pode ser conjurada apenas se você estiver sob efeito de Abraço de "
                    + "Tesla. "
                    + "Todos os personagens afetados sofrem 2d6+Metade do Foco pontos de Dano Mágico Elemental: "
                    + "Eletricidade e perdem -2PA por 2 Rodadas (até o mínimo de 2PA). "
                    + "O dano causado aumenta em +1 para cada Rodada restante na Duração do Abraço de Tesla, ao fim "
                    + "da conjuração a Duração do Abraço de Tesla é reduzida à zero.")
            .effectChainDescription("Sagacidade de Tesla: Apenas personagens inimigos sofrem danos por esta magia.")
            .criticalEffectType(CriticalEffectType.ATORDOANTE)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_CURTA)))
            .build()),

    /**
     * Its {@code Alcance:} is Toque, but its Efeito begins by moving the caster to a target at
     * Distância Longa — the touch is what happens on arrival. Authored as Toque, which is what the
     * descriptor says; the approach is forced movement and this core never does geometry.
     */
    IMPETO_TROVEJANTE(SpellData.builder()
            .name("Ímpeto Trovejante")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.FURIA_DE_TESLA_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Você se move em velocidade ofuscante até o alvo, se chocando contra ele causando grande de "
                    + "dano.")
            .primaryEffectDescription("Esta magia pode ser conjurada apenas se você estiver sob efeito de Abraço de "
                    + "Tesla. "
                    + "Escolha um alvo em Distância Longa, você se move rapidamente até o personagem escolhido e se "
                    + "choca com ele, esta ação não permite Reações. O alvo sofre 3d6+Metade do Foco pontos de Dano. "
                    + "O dano causado aumenta em +2 para cada Rodada restante na Duração do Abraço de Tesla, ao fim "
                    + "da conjuração a Duração do Abraço de Tesla é reduzida à zero.")
            .effectChainDescription("Precisão de Tesla: Sua Margem Crítica Menor para este ataque é aumentada em +5 "
                    + "números.")
            .criticalEffectType(CriticalEffectType.TOQUE_DO_AETHER)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.TOQUE)
            .build());

    private final SpellData data;

    FuriaDeTeslaSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.FURIA_DE_TESLA;
    }
}
