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
 * ANULAÇÃO (Primordial) — five Magias, one of the two trees that never diverges, so every rung is
 * on the trunk and its branch gate can refuse nothing.
 *
 * <h2>Two authoring conventions this tree is the reference for</h2>
 *
 * <ul>
 *   <li><b>An area with no stated placement distance is centred on the Conjurador.</b> Only
 *       Detectar Magia spells it out ("centralizada em você"); the other three name a size and
 *       nothing else. They are authored with no {@code Range}, which is exactly {@code
 *       SpellTargeting#isCenteredOnCaster()}, rather than inventing a placement distance the
 *       document does not give.</li>
 *   <li><b>A Foco-scaled footprint is authored at its base size.</b> Three of these five grow
 *       their area at Foco 5 or above; {@code AreaOfEffect} holds one fixed length, and the area
 *       depends on the caster's <em>live</em> Foco, so the growth clause stays in the Magia's own
 *       prose. Modelling it needs a footprint that can be resolved against a sheet, which is the
 *       missing Área de Efeito resolution, not a missing column.</li>
 * </ul>
 *
 * <p>Its {@code Tempo de Ativação:} lines are all headed {@code Tempo de Conjuração:} instead —
 * one of the source document's spelling variants, and the same descriptor.
 */
public enum AnulacaoSpell implements AuthoredSpell {

    /**
     * One of only two Magias in the catalog whose GD is a <b>table over the target effect's own
     * rung</b> rather than a fixed tier — "Variável (Sementes: Fácil, Brotos: Médio, Mudas:
     * Difícil, Emergentes: Muito Difícil, Florescentes: Improvável)". So {@code
     * getCastingDifficultyLevel()} is {@code null} here and {@code getCastingDifficultyAgainst}
     * answers instead. The other is {@code VidaSpell#REMOVER_MALDICAO}.
     */
    IDENTIFICACAO(SpellData.builder()
            .name("Identificação")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyScaledToTargetLevel(true)
            .description("Permite a identificação dos efeitos de uma Magia ou de um Item.")
            .primaryEffectDescription("O conjurador descobre os efeitos de uma magia ativa, item encantado, Regalia "
                    + "ou item tecnológico.")
            .secondaryEffectDescription("Anulação Arcana: Após identificar uma magia ativa, se for um encantamento, "
                    + "caso o conjurador seja capaz de lançar a magia identificada, ele poderá fazê-lo. Se fizer, ao "
                    + "invés de ativar os efeitos da magia as duas se anularão. Se a magia em questão for "
                    + "permanente, ao invés disso ela ficará inerte por uma quantidade de rodadas igual ao ‘Domínio "
                    + "do Mana’ do conjurador.")
            .criticalEffectType(CriticalEffectType.PREVENIR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * "Esta área aumenta em +2UD para cada PA utilizado em sua conjuração" — a footprint that
     * grows with Pontos de Ação spent beyond the stated cost. {@code ActivationTime} holds one
     * cost and {@code AreaOfEffect} one length, so the base pair is authored and the scaling stays
     * prose; nothing in this core lets a caster overspend PA on an activation to begin with.
     */
    DETECTAR_MAGIA(SpellData.builder()
            .name("Detectar Magia")
            .branchLevel(BranchLevel.BROTO)
            .activationTime(ActivationTime.pa(1))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .description("Esta magia torna visível a aura mágica de magias, itens encantados, Regalias Verdadeiras, "
                    + "e conjuradores.")
            .primaryEffectDescription("Ao conjurar esta magia seu conjurador consegue enxergar a aura mágica de "
                    + "todos os equipamentos afetados por magias. Esta área aumenta em +2UD para cada PA utilizado "
                    + "em sua conjuração.")
            .effectChainDescription("Detectar Conjuradores: Conjuradores de magia dentro da área de efeito, que "
                    + "possuam valores em ‘Domínio do Mana’ menor que o do conjurador, também são revelados por esta "
                    + "magia.")
            .criticalEffectType(CriticalEffectType.PREVENIR)
            .duration(SpellDuration.concentracaoMais(1))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_MEDIA)))
            .build()),

    /**
     * TODO its whole effect is a per-use d6 table applied to <em>other</em> Magias cast inside the
     * area (1-2 fail, 3-4 misbehave, 5-6 work). Nothing in this core interposes on someone else's
     * casting, and this core never rolls dice, so both halves are missing.
     */
    CAMPO_DE_MAGIA_CAOTICA(SpellData.builder()
            .name("Campo de Magia Caótica")
            .branchLevel(BranchLevel.MUDA)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Capaz de perturbar o Aether e o Mana da área, dificultando a conjuração de magias e "
                    + "ativações de itens encantados em seu interior.")
            .primaryEffectDescription("Magias não primordiais e itens encantados na área de efeito podem parar de "
                    + "funcionar ou apresentar comportamentos instáveis e inesperados. Sempre que um item ou uma "
                    + "magia for utilizada dentro da área de efeito desta magia um dado deverá ser rolado. "
                    + "Resultados 1 e 2 indicam falha na conjuração ou que a magia imbuída no item foi "
                    + "temporariamente dissipada; 3 e 4 indicam que as magias apresentam comportamentos inesperados, "
                    + "como mudanças de alvo, elementos e efeitos; 5 e 6 indicam perfeito funcionamento. "
                    + "A Área Circular Média aumenta em 1 Categoria se tiver Foco 5 ou superior.")
            .effectChainDescription("Área de Magia Morta: Se o conjurador tiver 7 ou mais Graduações em ‘Domínio do "
                    + "Mana’, todas as magias não-Primordiais em seu interior são desativadas.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.minutos(1))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_MEDIA)))
            .build()),

    /**
     * Authored in minutes, not the 12 Rodadas it converts to — see {@code DurationUnit}. This is
     * one of the six Magias {@code POTENCIALIZAR} would misapply by 12× if the unit were dropped,
     * and it carries that Efeito Crítico itself.
     */
    AREA_DE_MAGIA_VIVA(SpellData.builder()
            .name("Área de Magia Viva")
            .branchLevel(BranchLevel.EMERGENTE)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Cria uma área dentro de Campo de Magia Caótica ou de uma Área de Magia Morta que, dentro "
                    + "dela, permite que conjuradores utilizem magias normalmente.")
            .primaryEffectDescription("Cria uma pequena área dentro de um Campo de Magia Caótica ou de uma Área de "
                    + "Magia Morta que, enquanto dentro dela, magias funcionam normalmente, ignorando efeitos "
                    + "limitadores existentes no terreno. "
                    + "A Área Circular Curta aumenta para Área Circular Média se tiver Foco 5 ou superior.")
            .effectChainDescription("Campo de Magia Persistente: Se 10 Graduações em Domínio do Mana, suas Magias "
                    + "conjuradas dentro de sua Área de Magia Viva não podem ser anuladas por nenhum efeito.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.minutos(1))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_CURTA)))
            .build()),

    /**
     * TODO "seus danos não são considerados nem físicos e nem mágicos, por isso seus danos não
     * podem ser reduzidos ou evitados por efeitos" is unmitigable damage, which {@code
     * DamageService} cannot express: {@code ignoreDamageReduction} skips RD but never RA, and
     * nothing skips both plus half-damage. TODO it also leaves behind a Terreno Difícil, and
     * {@code TerrainType} describes a whole Scene rather than a patch of one.
     */
    BOMBA_DE_AETHER_MENOR(SpellData.builder()
            .name("Bomba de AEther Menor")
            .branchLevel(BranchLevel.FLORESCENTE)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Uma das magias mais poderosas conhecidas e seu uso é considerado um tabu. O conjurador "
                    + "cria em suas mãos uma esfera de energia primordial massiva, uma imitação menor da arma mágica "
                    + "usada contra o reino de Jully, pelo Olho de Deus.")
            .primaryEffectDescription("Esta magia cria uma esfera de energia translúcida em suas mãos e ela detonará "
                    + "após 3 Rodadas. "
                    + "Após ser conjurada pode ser arremessada em até Distância Longa ao Tempo de 2PA, ou "
                    + "simplesmente depositada em algum lugar. "
                    + "Ao explodir todas as Magias ativas na área são dissipadas. Criaturas vivas na área sofrem 2d6 "
                    + "pontos de Dano Primordial, então perdem 1d6PD e 1d6PM, objetos e mortos-vivos sofrem o dobro "
                    + "de danos. "
                    + "Após a explosão a área afetada se tornará uma Área de Magia Caótica, adicionalmente causa 2 "
                    + "pontos de danos a todos os personagens vivos na área (danos dobrados à Mortos-Vivos e "
                    + "Objetos) e se torna um Terreno Difícil em função de uma distorção na física local. "
                    + "A Bomba de AEther afeta a essência de tudo em seu interior e causa danos a nível molecular, "
                    + "seus danos não são considerados nem físicos e nem mágicos, por isso seus danos não podem ser "
                    + "reduzidos ou evitados por efeitos. "
                    + "O Alcance dobra se tiver Foco 5 ou superior.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(10))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_LONGA)))
            .build());

    private final SpellData data;

    AnulacaoSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.ANULACAO;
    }
}
