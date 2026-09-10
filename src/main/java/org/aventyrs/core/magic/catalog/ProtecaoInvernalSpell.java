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
 * PROTEÇÃO INVERNAL (Encantamento/Elemental: Gelo) — seven Magias, diverging at Muda into a
 * warding line and a binding line, and converging at Florescente.
 *
 * <p>Its recurring shape is a Defesas bonus plus Resistência Elemental: Gelo, then immunity to it.
 * The Defesas half is real ({@code DefenseService} reads {@code ModifierType#DEFESAS} for a
 * granted bonus); the elemental half is not, since RD/RA are resolved with no notion of damage
 * type and no immunity mechanism exists at all.
 *
 * <p>Three of its entries carry a Pontos de Ação reduction "até o mínimo de 2PA" — a floor of 2
 * rather than the 0 every {@code <Stat>Service} clamps at, the same shape Fúria de Tesla's Nova
 * Chocante uses.
 */
public enum ProtecaoInvernalSpell implements AuthoredSpell {

    CORPO_FRIO(SpellData.builder()
            .name("Corpo Frio")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .description("Alvo se torna resistente ao Frio, natural e mágico.")
            .primaryEffectDescription("Enquanto sob efeito desta magia, o personagem tocado se torna resistente ao "
                    + "frio, não sofrendo os efeitos comuns das baixas temperaturas. "
                    + "O alvo também recebe RE: Gelo.")
            .criticalEffectType(CriticalEffectType.IMUNIZAR)
            .duration(SpellDuration.rodadas(2))
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /** "Todo dano Elemental: Gelo … é reduzido à metade" is {@code ModifierType#HALF_DAMAGE}, which is real — but scoped to one damage type, which is not. */
    CORPO_GELIDO(SpellData.builder()
            .name("Corpo Gélido")
            .branchLevel(BranchLevel.BROTO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .description("Um personagem se torna extremamente resistente aos efeitos do frio.")
            .primaryEffectDescription("A pele do alvo é coberta por uma veste mágica feita de gelo e neve, "
                    + "concedendo Bônus de +1 nas Defesas e o permitindo ignorar parcialmente efeitos do frio "
                    + "extremo. Todo dano Elemental: Gelo, físico ou mágico, sofrido pelo alvo é reduzido à metade.")
            .effectChainDescription("Abraço Gélido: Como efeito adicional a veste é coberta de pequenos espinhos "
                    + "gélidos que congelam aqueles que o tocam. Personagens agarrados pelo alvo, ou que o estejam "
                    + "agarrando, sofrem Redutor de -2PA por 1 Rodada (até o mínimo de 2PA).")
            .criticalEffectType(CriticalEffectType.IMUNIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    CORACAO_DO_INVERNO(SpellData.builder()
            .name("Coração do Inverno")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.PROTECAO_INVERNAL_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("O alvo se torna imune aos efeitos do frio e causa danos elementais aqueles ao seu redor.")
            .primaryEffectDescription("O alvo é coberto por uma armadura mágica feita de gelo verdadeiro que lhe "
                    + "concede Bônus de +2 nas Defesas e o torna imune aos efeitos Elementais: Gelo.")
            .effectChainDescription("Geada: Personagens adjacentes ao alvo sofrem 2 pontos de Dano Mágico "
                    + "Elemental: Gelo a cada Rodada.")
            .criticalEffectType(CriticalEffectType.IMUNIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    GRILHOES_DO_INVERNO(SpellData.builder()
            .name("Grilhões do Inverno")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.PROTECAO_INVERNAL_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Grilhões de gelo prendem o alvo, reduzindo sua mobilidade.")
            .primaryEffectDescription("O alvo tocado é preso por algemas de gelo verdadeiro, personagens presos "
                    + "desta forma sofrem redutor de -1PA (até o mínimo de 2PA) e Desvantagem e suas rolagens de "
                    + "Perícia.")
            .effectChainDescription("Queimaduras de Gelo: Como efeito adicional o alvo sofre 1d6 pontos de Dano "
                    + "Elemental: Gelo a cada Rodada.")
            .criticalEffectType(CriticalEffectType.ATORDOANTE)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /** A wall whose length and height are both derived from the caster's Foco — geometry this core never does, so both stay prose. */
    MURALHA_DE_GELO(SpellData.builder()
            .name("Muralha de Gelo")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.PROTECAO_INVERNAL_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Cria uma muralha de indestrutível de gelo verdadeiro a sua frente.")
            .primaryEffectDescription("Você pode criar uma muralha reta de 2+Metade do Foco UD à sua Frente, a "
                    + "altura da muralha é igual a Metade do Foco UD.")
            .effectChainDescription("Nevasca Menor: Personagens adjacentes a muralha sofrem 2d6 pontos de Dano "
                    + "Mágico Elemental: Gelo a cada Rodada.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    /**
     * "seu Movimento Base é reduzido à metade" is the halving {@code MovementService} has no stage
     * for — it sums {@code ModifierType#MOVEMENT} additively with no multiplicative step, which is
     * why there is deliberately no {@code MOVEMENT_HALVED} constant to reach for.
     */
    PRISAO_INVERNAL(SpellData.builder()
            .name("Prisão Invernal")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.PROTECAO_INVERNAL_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Prende o alvo com diversas correntes de gelo, prejudicando gravemente sua mobilidade.")
            .primaryEffectDescription("O alvo é enredado por diversas correntes de gelo verdadeiro, fazendo sofrer "
                    + "Desvantagem em rolagens de Perícia e Dano. "
                    + "O personagem alvo também sofre Redutor em PA igual a metade do seu Foco (até o mínimo de 2PA) "
                    + "e seu Movimento Base é reduzido à metade.")
            .effectChainDescription("Queimaduras de Gelo: Como efeito adicional o alvo sofre 1d6 pontos de Dano "
                    + "Elemental: Gelo a cada Rodada.")
            .criticalEffectType(CriticalEffectType.ATORDOANTE)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /** The convergence rung. Its Efeito Alternativo turns the same dome into a prison around one hostile character — the two ramificações meeting in one Magia. */
    REDOMA_INVERNAL(SpellData.builder()
            .name("Redoma Invernal")
            .branchLevel(BranchLevel.FLORESCENTE)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Cria uma redoma indestrutível feita de gelo verdadeira ao seu redor.")
            .primaryEffectDescription("Similar a muralha de gelo, esta magia cria uma muralha de gelo ao seu redor e "
                    + "com altura suficiente para sempre cobrir a você e seu aliados. "
                    + "As paredes da redoma são indestrutíveis, isolando completamente os personagens em seu "
                    + "interior do mundo exterior.")
            .effectChainDescription("Nevasca Maior: Personagens adjacentes à redoma sofrem 3d6 pontos de Dano Mágico "
                    + "Elemental: Gelo a cada Rodada.")
            .secondaryEffectDescription("Exílio Invernal: a Redoma Invernal é criada ao redor de um único "
                    + "personagem, se este personagem for um hostil a Duração é reduzido à metade.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_MEDIA)))
            .build());

    private final SpellData data;

    ProtecaoInvernalSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.PROTECAO_INVERNAL;
    }
}
