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
 * OCULTAÇÃO (Encantamento) — eight Magias, diverging at Muda into an invisibility line and a
 * shadow line, and never converging.
 *
 * <p>Nothing in this tree can be applied. Being unseen is not a state this core models at all:
 * there is no visibility, no detection, no line of sight, and no notion of a target being unaware.
 * Every constant's effect is therefore prose in full, and the columns that <em>are</em> real —
 * rung, GD, Duração, Alcance, Efeito Crítico — are what the tree contributes today.
 *
 * <p>Its rungs are printed out of order in the source document: the Principal branch runs Muda →
 * Emergente → Florescente, then the Alternativo branch's Emergente and Florescente follow. They
 * are declared here in rung order instead, which is the order {@code SpellTree#getSpells()}
 * promises.
 */
public enum OcultacaoSpell implements AuthoredSpell {

    /**
     * The second of the two entries whose GD is bare "{@code DM do Alvo}" with no tier — authored
     * as a {@code null} tier with the floor flag, see {@code
     * Spell#isCastingDifficultyFlooredByTargetMagicDefense()}.
     *
     * <p>Its descriptor block prints {@code Alcance:} before {@code Duração:}, reversing the
     * catalog's usual order; both are transcribed the same way regardless.
     */
    CRIAR_DISTRACAO(SpellData.builder()
            .name("Criar Distração")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(1))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Você cria uma ilusão dinâmica, o alvo enxerga nela algo que chama sua atenção por um breve "
                    + "segundo, e então ela desaparece.")
            .primaryEffectDescription("Você pode fazer rolagens de 'Ladinice' ou de 'Furtividade' contra o alvo, "
                    + "mesmo que ele esteja ciente de sua presença. "
                    + "O Alcance muda para Área de Efeito – Cone Curto se tiver Foco 5 ou superior.")
            .secondaryEffectDescription("Visão/Som Fantasma: O alvo escuta sons ou enxerga vultos vindo de espaços "
                    + "vazios, as distâncias são limitadas ao alcance da magia.")
            .criticalEffectType(CriticalEffectType.PREVENIR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_CURTA))
            .build()),

    /**
     * The last Magia before the divergence. Its Efeito Alternativo <i>Ocultação em Massa</i> is
     * the clause L30 would have the Alternativo branch evolve — and the branch that actually
     * evolves it is the Principal one (Campo de Invisibilidade). See {@link
     * MagicBranch#OCULTACAO_PRINCIPAL}.
     */
    OCULTAR_SE_NAS_SOMBRAS(SpellData.builder()
            .name("Ocultar-se nas Sombras")
            .branchLevel(BranchLevel.BROTO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .description("Você se torna invisível enquanto estiver sobre a cobertura de uma sombra.")
            .primaryEffectDescription("Enquanto estiver em uma sombra grande o suficiente para cobri-lo você não "
                    + "pode ser visto ou detectado por meios visuais. "
                    + "Realizar qualquer ação que afete terceiros não voluntários, ou sair da cobertura da sombra, "
                    + "cancela os efeitos dessa magia.")
            .secondaryEffectDescription("Ocultação em Massa: Você pode ocultar criaturas além de você se a sombra "
                    + "for grande o bastante, se o fizer o Custo de Conjuração aumenta em +2PM para cada outro "
                    + "personagem além de você e a GD muda para Difícil.")
            .criticalEffectType(CriticalEffectType.GUILHOTINA)
            .duration(SpellDuration.minutos(2))
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    LUDIBRIAR_OS_OLHOS(SpellData.builder()
            .name("Ludibriar os Olhos")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.OCULTACAO_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Você se torna invisível, mas apenas aos olhos dos alvos afetados por esta magia.")
            .primaryEffectDescription("Um cone de energia é lançado à frente do conjurador, todos os alvos atingidos "
                    + "têm sua mente nublada, e os olhos deles não mais conseguem enxergar o conjurador. "
                    + "Este é um efeito hipnótico, de Encantamento, e realizar uma ação ofensiva (que afete "
                    + "terceiros) cancela esta magia, tornando você novamente visível a todos os personagens antes "
                    + "afetados.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.cone(Range.DISTANCIA_MEDIA)))
            .build()),

    /**
     * Its {@code Alcance:} reads "Área de Efeito – Sombras em Distância Curta" — the area is
     * whatever shadows lie within that band, not a footprint of any {@code AreaShape}. Authored as
     * the nearest thing that <em>is</em> a footprint, a Círculo at Distância Curta, which is the
     * region the shadows are picked from; the "shadows only" filter has nowhere to live and stays
     * in the prose.
     */
    DOBRAR_SOMBRAS(SpellData.builder()
            .name("Dobrar Sombras")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.OCULTACAO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Você pode aumentar ou diminuir o tamanho de uma sombra.")
            .primaryEffectDescription("você pode moldar, aumentar ou reduzir o tamanho e forma de uma ou mais "
                    + "sombras em seu alcance. O total de modificações de tamanho por conjuração é de 1UD para cada "
                    + "2 Graduações em ' Domínio do Mana '.")
            .secondaryEffectDescription("Purgar Sombra: Você pode fazer desaparecer por completo a sombra de uma "
                    + "criatura ou objeto.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.minutos(1))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_CURTA)))
            .build()),

    /**
     * Its Corrente <i>Invisibilidade Persistente</i> is written inside the Efeito and a second
     * one, <i>Presença Nula</i>, on its own {@code Corrente de Efeitos –} line. Only the latter is
     * the "superar a DM do alvo em 5" descriptor, so it is what {@code
     * getEffectChainDescription()} carries; the former is transcribed in the prose where the
     * document puts it.
     */
    INVISIBILIDADE_VERDADEIRA(SpellData.builder()
            .name("Invisibilidade Verdadeira")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.OCULTACAO_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Você se torna realmente invisível.")
            .primaryEffectDescription("Um encantamento poderoso e que pode ser conjurada apenas sobre si mesmo. "
                    + "Quando bem-sucedido na conjuração você se torna invisível, e nenhum Personagem é capaz de "
                    + "detectá-lo utilizando da visão por meios não mágicos, mas ainda é possível percebê-lo "
                    + "utilizando outros sentidos. "
                    + "Efetuar ações ofensivas cancelam esta magia, a primeira ação ofensiva que realizar enquanto "
                    + "invisível recebe a Corrente de Efeitos – Invisibilidade Persistente: No final deste turno sua "
                    + "Invisibilidade Verdadeira é reativada, a Duração não é renovada, permanecendo a mesma "
                    + "restante da conjuração inicial.")
            .effectChainDescription("Presença Nula: Sua presença não pode ser percebida através da audição e "
                    + "olfato, tornando impossível detectá-lo com estes sentidos por meios não mágicos.")
            .secondaryEffectDescription("Purgar Invisibilidade: A Perícia Chave para Conjuração muda para Ataque à "
                    + "Distância e o GD se torna igual à DM do alvo. Você lança um cone de energia com alcance de 4m "
                    + "para sua frente, personagens invisíveis dentro da área, por qualquer tipo de efeito de "
                    + "invisibilidade, se torna visível. Corrente de Efeitos – Roubar Invisibilidade: Você se torna "
                    + "invisível por uma quantidade de Rodadas igual ao número de alvos afetados")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /**
     * TODO its Efeito Alternativo <i>Refúgio Invisível</i> is the catalog's only caster-scaled
     * Duração — "Concentração + Foco Horas", derived from an Attribute <em>and</em> denominated in
     * hours — and also the only Concentração a <em>third party</em> can break ("se qualquer
     * personagem em seu interior realizar ações ofensivas … a magia é encerrada imediatamente").
     * An Efeito Alternativo is not a separate {@code Spell} and {@code SpellData} holds one
     * Duração, so neither is representable; see {@code DurationKind}'s own note.
     */
    CAMPO_DE_INVISIBILIDADE(SpellData.builder()
            .name("Campo de Invisibilidade")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.OCULTACAO_PRINCIPAL)
            .activationTime(ActivationTime.pa(1))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Você pode tornar invisível múltiplos personagens e objetos simultaneamente.")
            .primaryEffectDescription("Você pode tornar invisível diversos Personagens inteligentes, ou objetos, "
                    + "próximos a você. Os alvos são afetados por Invisibilidade Verdadeira, mas suas ações não "
                    + "recebem a Corrente de Efeitos – Invisibilidade Persistente. "
                    + "Para cada alvo o Tempo de Conjuração desta magia aumenta em +1PA.")
            .secondaryEffectDescription("Refúgio Invisível: Você pode mudar Alcance desta magia para toque e afetar "
                    + "uma Construção. A Construção, com tudo e todos que estiverem dentro dela, não pode ser vista "
                    + "por quem estiver do lado de fora. A Duração da Magia muda para Concentração + Foco Horas, se "
                    + "qualquer personagem em seu interior realizar ações ofensivas contra personagens no exterior a "
                    + "magia é encerrada imediatamente.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.minutos(2))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_CURTA)))
            .build()),

    /** "se torna imune a Dano Físico, mas ainda pode sofrer danos mágicos" is damage-type immunity, of which this core has no mechanism at all. */
    RASTEJAR_NAS_SOMBRAS(SpellData.builder()
            .name("Rastejar nas Sombras")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.OCULTACAO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Você pode entrar nas sombras de criaturas e objetos.")
            .primaryEffectDescription("Você consegue entrar nas sombras de uma criatura ou objeto, enquanto estiver "
                    + "dentro das sombras você não pode ser afetado ou detectado por meios comuns e se torna imune a "
                    + "Dano Físico, mas ainda pode sofrer danos mágicos. "
                    + "Enquanto estiver dentro de uma sombra você não pode afetar o espaço exterior.")
            .secondaryEffectDescription("Ataque Sombrio: Você pode conjurar esta versão da magia apenas enquanto "
                    + "estiver sob efeito de ‘Ocultar-se nas Sombras’ ou ‘Rastejar nas Sombras’. Você pode realizar "
                    + "uma rolagem de Perícia de Ataque contra a DM de um alvo à até 4m. Alvos Corpo à Corpo sofrem "
                    + "3d6+Metade do Foco pontos de Dano Mágico Profano. Alvos à Distância sofrem 2d6+Metade do "
                    + "Foco. Efeito Crítico muda para Oferenda Sombria. "
                    + "Ataques realizados desta forma recebem a Corrente de Efeitos – Maldição Sombria: O alvo deste "
                    + "ataque tem sua Margem para Falhas Críticas Menores aumenta em 2 números (rolagens 6 ou menos, "
                    + "efeito não cumulativo), por 2 Rodadas. "
                    + "Realizar um Ataque Sombrio não cancela efeitos de Invisibilidade ou de Rastejar nas Sombras, "
                    + "mas reduz a Duração destes efeitos em 1 Rodada, exceto quando o alvo estiver Amaldiçoado.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /**
     * Names a Magia the catalog does not contain: "pode conjurar 'Rastejar nas Sombras' e 'Moldar
     * Sombras' sem gastar PM". There is no <i>Moldar Sombras</i> in this tree or any other — Dobrar
     * Sombras' own Efeito says "você pode <b>moldar</b>, aumentar ou reduzir", so the two are
     * almost certainly the same Magia under two names. Transcribed as written; do not silently
     * rename either.
     */
    ASPECTO_SOMBRIO(SpellData.builder()
            .name("Aspecto Sombrio")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.OCULTACAO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Você se torna temporariamente um Aspecto Sombrio, uma criatura da Umbra Profunda composta "
                    + "apenas de sombras.")
            .primaryEffectDescription("Enquanto na forma de Aspecto Sombrio você não pode realizar ataques físicos e "
                    + "não é afetado por eles, sendo imune a efeitos não mágicos. "
                    + "Nesta forma, adicionalmente, você também pode conjurar 'Rastejar nas Sombras' e 'Moldar "
                    + "Sombras' sem gastar PM, e está sempre sob o efeito de 'Ocultar-se nas Sombras'. Enquanto "
                    + "estiver sob a cobertura de uma sombra suas Magias recebem a Corrente de Efeitos – Vampirismo "
                    + "Umbral: Esta magia adquire Roubo de Vida 2 como um efeito adicional.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .build());

    private final SpellData data;

    OcultacaoSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.OCULTACAO;
    }
}
