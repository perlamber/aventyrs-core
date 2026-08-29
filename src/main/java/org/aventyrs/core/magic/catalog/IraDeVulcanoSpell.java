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
 * IRA DE VULCANO (Elemental: Magma) — five Magias and the second of the two trees that never
 * diverge, so every rung sits on the trunk.
 *
 * <p>It is also the tree that needed {@code AreaShape#PENETRANTE}: its Semente and Broto are the
 * catalog's only two {@code Área de Efeito - Penetrante} entries. A Penetrante radiates from the
 * caster like a {@code LINHA}, so it is an emanation and may carry no placement {@code Range};
 * what its Broto's Corrente adds ("Permite atingir um terceiro alvo") is the piercing count, which
 * only means something once footprint resolution exists.
 *
 * <p>Every rung but Forma de Magma reads "ou DM do Alvo (Maior)", making this the tree with the
 * highest proportion of GD floors in the catalog.
 */
public enum IraDeVulcanoSpell implements AuthoredSpell {

    SOPRO_DE_MAGMA_MENOR(SpellData.builder()
            .name("Sopro de Magma Menor")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Sopra um jato de magma em alvos próximos")
            .primaryEffectDescription("Causa Metade do Foco pontos de Dano Mágico Elemental: Magma ao alvo.")
            .criticalEffectType(CriticalEffectType.INFLAMAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.penetrating(Range.DISTANCIA_MEDIA)))
            .build()),

    SOPRO_DE_MAGMA_MAIOR(SpellData.builder()
            .name("Sopro de Magma Maior")
            .branchLevel(BranchLevel.BROTO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Sopra uma grande quantidade de magma em seus alvos.")
            .primaryEffectDescription("Alvos atingidos sofrem 1d6+Metade do Foco pontos de Dano Mágico Elemental: "
                    + "Magma.")
            .effectChainDescription("Estilhaço Vulcânico: Permite atingir um terceiro alvo com o efeito Penetrante.")
            .criticalEffectType(CriticalEffectType.INFLAMAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.penetrating(Range.DISTANCIA_LONGA)))
            .build()),

    /** "dano reduzido em 2 para cada UD percorrido" is distance falloff; this core never does geometry, so it stays prose. */
    TORRENTE_VULCANICA(SpellData.builder()
            .name("Torrente Vulcânica")
            .branchLevel(BranchLevel.MUDA)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Sopra uma quantidade massiva de magma em seus alvos.")
            .primaryEffectDescription("Causa 2d6+Metade do Foco pontos de Dano Mágico Elemental: Magma, dano "
                    + "reduzido em 2 para cada UD percorrido.")
            .effectChainDescription("Folego Vulcano: Vantagem na rolagem de Dano, dano reduzido em 1 (ao invés de 2) "
                    + "para cada UD percorrido.")
            .criticalEffectType(CriticalEffectType.INFLAMAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.cone(Range.DISTANCIA_MEDIA)))
            .build()),

    /**
     * "sua Categoria de Tamanho aumenta em +2" is a real size shift, which {@code
     * CharacterSizeService#getEffectiveSizeCategory} resolves. TODO "pode ocupar o mesmo espaço
     * que outros personagens" and the damage to whoever starts a Turn sharing that space both need
     * positioning, which this core has none of.
     */
    FORMA_DE_MAGMA(SpellData.builder()
            .name("Forma de Magma")
            .branchLevel(BranchLevel.EMERGENTE)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Se transforma em uma poça de magma viva.")
            .primaryEffectDescription("Você se transforma numa poça de Magma, enquanto nesta forma sua Categoria de "
                    + "Tamanho aumenta em +2, você recebe RA, imunidade à Magma e pode ocupar o mesmo espaço que "
                    + "outros personagens. "
                    + "Personagens que iniciarem seus Turnos ocupando o mesmo espaço que você sofrem 1d6+Metade do "
                    + "Foco pontos de Dano Mágico Elemental: Magma.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /**
     * The catalog's only 5PA activation outside Piromancia, and the one Magia whose effect fires
     * twice on a delay — "2 Rodadas após este efeito uma chuva de Magma e detritos tomam o local".
     * Its Duração is Instantânea all the same, which is the document's own reading: the second
     * wave is part of the one resolution, not a sustained effect.
     */
    IRA_DE_VULCANO(SpellData.builder()
            .name("Ira de Vulcano")
            .branchLevel(BranchLevel.FLORESCENTE)
            .activationTime(ActivationTime.pa(5))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Invoca uma erupção Vulcânica centralizada em você, evocando jatos de magma do subsolo de "
                    + "Tellus.")
            .primaryEffectDescription("Causa 3d6+Metade do Foco pontos de Dano Mágico Elemental: Magma em todos os "
                    + "personagens dentro da área desta magia. 2 Rodadas após este efeito uma chuva de Magma e "
                    + "detritos tomam o local, causando 2d6 Pontos de Dano Mágico Elemental: Magma a todos que ainda "
                    + "estiverem na área. "
                    + "Apenas o dano primário desta magia pode ser beneficiado por efeitos críticos.")
            .criticalEffectType(CriticalEffectType.INFLAMAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_LONGA)))
            .build());

    private final SpellData data;

    IraDeVulcanoSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.IRA_DE_VULCANO;
    }
}
