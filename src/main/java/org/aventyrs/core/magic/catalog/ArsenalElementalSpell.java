package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.SpellData;
import org.aventyrs.core.magic.SpellDuration;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * ARSENAL ELEMENTAL (Encantamento/Elemental: Todos) — seven Magias, diverging at Muda into an
 * offensive and a defensive line and converging again at Florescente.
 *
 * <p>The tree is the reason {@code ElementalType#TODOS} exists: every Magia here lets the caster
 * pick an element per cast, so the tree is typed Elemental without being typed to one element.
 * <b>Which elements are on offer differs between the Magias and this core models none of them</b>
 * — the Semente offers four (Fogo, Terra, Água, Vento), Arma Elemental nine (adding Magma, Gelo,
 * Som, Eletricidade, Selvagem). {@code DamageType} has no elemental breakdown at all, so the lists
 * stay in the prose.
 *
 * <p>It also holds both of the catalog's non-PA activations that this tree contributes: a {@code
 * Reação} at Semente and an {@code Ação Livre} at Florescente.
 */
public enum ArsenalElementalSpell implements AuthoredSpell {

    /**
     * One of five Magias in the catalog cast as a {@code Reação} rather than for Pontos de Ação.
     *
     * <p>Its Efeito Alternativo, Proteção Elemental — take the Defesas bonus instead of dealing
     * the damage — is the clause both ramificações are traced from: {@link
     * MagicBranch#ARSENAL_ELEMENTAL_ALTERNATIVO} is the line that keeps turning the elements
     * defensive.
     *
     * <p>TODO "A cada Cena esta magia pode ser Conjurada apenas uma vez para cada Elemento" is a
     * per-Scene, per-element use counter; {@code CharacterSheet} tracks Round-scoped effects, not
     * Scene-scoped activation counts. TODO "não podem ser potencializados por Equipamentos, mas
     * ainda pode ser afetado por Habilidades de Foco, Talentos e Habilidades de Títulos Aventyr"
     * needs a bonus scoped by <em>which source</em> granted it, which no {@code ModifierType} scan
     * distinguishes.
     */
    RETALIACAO_ELEMENTAL(SpellData.builder()
            .name("Retaliação Elemental")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.REACAO)
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .description("Personagens que te atacarem são punidas por forças Elementais.")
            .primaryEffectDescription("Esta magia pode ser conjurada apenas quando estiver sendo atacado por um "
                    + "inimigo adjacente. "
                    + "Escolha um elemento entre: Fogo, Terra, Água ou Vento, o personagem que estiver te atacando "
                    + "sofre Metade do Foco pontos de dano Mágico Elemental. "
                    + "A cada Cena esta magia pode ser Conjurada apenas uma vez para cada Elemento. "
                    + "Os efeitos desta magia não podem ser potencializados por Equipamentos, mas ainda pode ser "
                    + "afetado por Habilidades de Foco, Talentos e Habilidades de Títulos Aventyr.")
            .secondaryEffectDescription("Proteção Elemental: Ao invés de causar dano você pode receber Bônus "
                    + "Elemental de +Metade do Foco em suas Defesas.")
            .criticalEffectType(CriticalEffectType.CATACLISMO)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /**
     * Its Efeito Alternativo is headed {@code Efeito Alternativo: Distribuição - …} rather than
     * the catalog's usual {@code Efeito Alternativo – Distribuição: …}; same descriptor, and the
     * name is transcribed the same way as everywhere else.
     */
    BENCAO_ELEMENTAL(SpellData.builder()
            .name("Benção Elemental")
            .branchLevel(BranchLevel.BROTO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .description("Você pode clamar pelo apoio dos Elementais para que eles fortifiquem seus equipamentos.")
            .primaryEffectDescription("O equipamento tocado pelo conjurador adquire propriedades mágicas adicionais. "
                    + "Armas encantadas desta forma fornecem Vantagem nas rolagens de Ataque ou Dano, Armaduras ou "
                    + "Escudos fornecem Bônus de +2 em DF ou DM. "
                    + "A escolha das propriedades do encanto deve ser efetuada no momento da conjuração.")
            .secondaryEffectDescription("Distribuição: Você pode trocar a vantagem recebida em ataque ou dano de "
                    + "Armas por um bônus de +1 em rolagens de ataque e dano, Armaduras e Escudos podem dividir os "
                    + "bônus defensivos, concedendo Bônus de +1 às Defesas.")
            .criticalEffectType(CriticalEffectType.FORTALECER)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The one Magia in the catalog whose {@code Efeito Crítico:} line is a sentence rather than a
     * tier — "Armas afetadas por esta magia adquirem Cataclismo como Efeito Crítico Adicional".
     * The type is authored as {@link CriticalEffectType#CATACLISMO}, since that is the effect
     * named; what the sentence adds is that it lands on the <em>enchanted weapon</em> rather than
     * on this Magia's own casting roll, which is a per-copy item property and so unmodelable —
     * see the "owned/produced item copy" gap.
     */
    ARMA_ELEMENTAL(SpellData.builder()
            .name("Arma Elemental")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.ARSENAL_ELEMENTAL_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("O ímpeto dos Elementais encanta suas armas, concedendo-a dando mágico adicional.")
            .primaryEffectDescription("A arma tocada concede vantagem nas rolagens de ataque e dano ao seu usuário. "
                    + "Escolha um elemento entre: Fogo, Magma, Terra, Água, Gelo, Som ou Vento, Eletricidade e "
                    + "Selvagem. Ataques realizados com esta arma são rolados contra DM e metade de seu dano passa a "
                    + "ser do Elemento escolhido. Armas de ataque a distância imbuem o poder em suas munições.")
            .secondaryEffectDescription("Natureza Impetuosa: Você pode afetar um Personagem ao invés de uma arma, e "
                    + "todas as armas que ela utilizar receberão os benefícios de Arma Elemental, assim como seus "
                    + "ataques desarmados. Afetar Personagens ao invés de Armas é menos eficiente, reduzindo a "
                    + "duração da magia pela metade.")
            .criticalEffectType(CriticalEffectType.CATACLISMO)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /** The defensive line's first rung — a shield rather than a weapon. */
    BASTIAO_ELEMENTAL(SpellData.builder()
            .name("Bastião Elemental")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.ARSENAL_ELEMENTAL_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Você clama pela proteção dos elementos, e eles te ouvem, e te protegem.")
            .primaryEffectDescription("Você deve tocar um item do tipo ‘Escudo’, como um escudo ou uma braçadeira. O "
                    + "item tocado concede RD ao seu usuário. "
                    + "Escolha um elemento entre: Fogo, Magma, Terra, Água, Gelo, Som ou Vento, Eletricidade e "
                    + "Selvagem. O usuário do item também recebe Resistência Elemental ao elemento escolhido.")
            .effectChainDescription("Escudo Cromático: O escudo tocado adicionalmente fornece Resistência Elemental: "
                    + "Todos os Elementos ao seu usuário.")
            .criticalEffectType(CriticalEffectType.FORTALECER)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /** TODO reactive/retaliation damage — {@code DamageService} only ever computes damage to a target from an attacker. */
    FURIA_ELEMENTAL(SpellData.builder()
            .name("Fúria Elemental")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.ARSENAL_ELEMENTAL_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Os espíritos Elementais o cercam, e atacam seus oponentes com força e fúria.")
            .primaryEffectDescription("Escolha um elemento disponível em Arma Elemental. Inimigos adjacentes sofrem "
                    + "2 pontos de dano no início de cada turno do conjurador. Inimigos adjacentes que atacarem o "
                    + "conjurador sofrem 2+’Metade do Foco’ pontos de Dano Mágico Elemental.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * TODO "Você se torna imune ao elemento escolhido" is damage-type immunity, of which this core
     * has no mechanism of any kind, and "Resistência Elemental a todos os outros Elementos" is
     * damage-type-scoped mitigation, which RD/RA resolve without any notion of damage type.
     */
    ARMADURA_ELEMENTAL(SpellData.builder()
            .name("Armadura Elemental")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.ARSENAL_ELEMENTAL_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Os espíritos Elementais o cercam, ou aos seus aliados, e o protegem das ameaças externas.")
            .primaryEffectDescription("Você recebe Bônus de +4 em suas Defesas e RM. "
                    + "Escolha um elemento disponível em Bastião Elemental. Você se torna imune ao elemento escolhido "
                    + "e recebe Resistência Elemental a todos os outros Elementos.")
            .criticalEffectType(CriticalEffectType.FORTALECER)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The convergence rung, and it says so in its own text: it grants Natureza Impetuosa and Fúria
     * Elemental from one ramificação <em>and</em> Escudo Cromático and Armadura Elemental from the
     * other. Sitting on the trunk is what makes that reachable from either path.
     *
     * <p>Its {@code Tempo de Ativação: Livre} is an {@code Ação Livre}, one of four in the catalog.
     */
    CAMPEAO_ELEMENTAL(SpellData.builder()
            .name("Campeão Elemental")
            .branchLevel(BranchLevel.FLORESCENTE)
            .activationTime(ActivationTime.ACAO_LIVRE)
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("O conjurador se transforma em um poderoso Elemental, e adquire diversas de suas "
                    + "habilidades.")
            .primaryEffectDescription("Escolha um elemento, você se transforma em uma Criatura Elemental do tipo "
                    + "escolhido. Sua aparência pouco muda, sua pele se transfora no elemento escolhido e passa a "
                    + "expelir grandes constantemente farpas ou fagulhas elementais. "
                    + "Enquanto nessa forma você recebe Bônus de +2 em Foco, adquire os Benefícios de Natureza "
                    + "Impetuosa, Fúria Elemental, Escudo Cromático e Armadura Elemental (mesmo que não utilize um "
                    + "item do tipo Armadura ou Escudo) e não pode conjurar magias.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build());

    private final SpellData data;

    ArsenalElementalSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.ARSENAL_ELEMENTAL;
    }
}
