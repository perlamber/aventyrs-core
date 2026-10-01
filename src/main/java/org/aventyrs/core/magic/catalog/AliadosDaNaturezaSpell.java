package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.SpellAlternateEffect;
import org.aventyrs.core.magic.SpellData;
import org.aventyrs.core.magic.SpellDuration;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * ALIADOS DA NATUREZA (Natural/Invocação) — seven Magias, diverging at Muda and converging again
 * at Florescente.
 *
 * <p>Every Magia here invokes a creature, and since core 0.0.92 each one is real: the stat blocks are {@code
 * monster.summon.NatureSummon}s ({@code NatureSummonKind}), and what each Magia does once cast — the creature, its
 * Duração, what it may not coexist with, any extra PM — is {@code magic.invocation.NatureInvocationService}, which
 * places the creatures in the {@code Scene} as their caster's invocations ({@code Scene#addSummons}). A {@code Spell}
 * still has no column naming its creature: the service is the link. The {@code Atributos e Perícias} blocks are
 * summarised in each {@code Efeito:} as before.
 */
public enum AliadosDaNaturezaSpell implements AuthoredSpell {

    /**
     * The only Magia of this tree delivered by Ataque Corpo-a-Corpo, and one of two in the whole
     * catalog whose GD is a <b>floor</b> rather than a tier — "Fácil (13|14) ou DM do Alvo
     * (maior)".
     *
     * <p>The Subordinado it makes (Cavaleiro or Torre; Prodigioso for half the Duração with Falsa Matilha) and the
     * "uma segunda vez sem que antes passe por um Descanso" gate are {@code NatureInvocationService#captivate}.
     */
    CATIVAR_ANIMAL(SpellData.builder()
            .name("Cativar Animal")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Torna um animal amigável a você.")
            .primaryEffectDescription("Toca um animal, tornando-o amistoso a você e ao seu grupo. "
                    + "O animal tocado também lhe ajuda em combate, se tornando um Subordinado do tipo Cavaleiro "
                    + "ou Torre, desde que suas ações não sejam contrárias aos instintos do animal (como lutar "
                    + "contra seu próprio bando ou destruir seu habitat). "
                    + "Um mesmo animal não pode ser alvo deste efeito uma segunda vez sem que antes passe por um "
                    + "Descanso.")
            .effectChainDescription("Domar: O animal alvo se torna completamente obediente, realizando inclusive "
                    + "ações contrárias aos seus instintos. Forçar um animal a fazer algo contrário ao seu "
                    + "instinto natural pode fazer com que ele se volte contra você após a Duração da magia.")
            .secondaryEffectDescription("Falsa Matilha: A Duração da magia é reduzida à metade. O animal tocado se "
                    + "torna um Subordinado Prodigioso.")
            // TODO: "A Duração da magia é reduzida à metade" is relative to its parent; SpellDuration
            //  holds an absolute figure, not a modifier of the column it overrides.
            .alternateEffect(SpellAlternateEffect.named("Falsa Matilha"))
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The Magia both ramificações are traced back from — its Efeito Alternativo, Predador
     * Regional, is what {@link MagicBranch#ALIADOS_DA_NATUREZA_ALTERNATIVO} evolves.
     *
     * <p>Its animal is {@code NatureSummonKind#ALIADO_DA_NATUREZA} (Predador Regional: {@code #PREDADOR_REGIONAL}),
     * invoked by {@code NatureInvocationService#invokeAliado}.
     */
    ALIADOS_DA_NATUREZA(SpellData.builder()
            .name("Aliados da Natureza")
            .branchLevel(BranchLevel.BROTO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .description("Invoca um animal da região para ajudar o conjurador.")
            .primaryEffectDescription("Esta magia transporta um animal da região para o seu lado, o animal "
                    + "transportado se torna temporariamente obediente, ao fim da Duração da magia, ou se o animal "
                    + "fosse sofrer um dano letal, o animal desaparece, transportado em segurança de volta ao seu "
                    + "local de origem. "
                    + "Apenas 1 Aliado da Natureza pode ser invocado por vez, caso um novo Aliado da Natureza seja "
                    + "invocado o anterior desaparece ao final do Turno.")
            .secondaryEffectDescription("Predador Regional: Você pode aumentar o Custo de Conjuração em +1PM, se o "
                    + "fizer o animal invocado será um forte exemplar da sua espécie, recebendo +2 Graduações em "
                    + "suas Perícias e Vigor +1.")
            .alternateEffect(SpellAlternateEffect.named("Predador Regional"))
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    /** Restates "Como Aliado da Natureza" outright — the principal effect, at quantity. */
    CANCAO_DE_FLORA(SpellData.builder()
            .name("Canção de Flora")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.ALIADOS_DA_NATUREZA_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Esta magia imita uma cantiga mágica feérica, e invoca diversos animais para lhe auxiliar.")
            .primaryEffectDescription("Como Aliado da Natureza, mas invoca 1 Predador Regional e 1 Aliado da "
                    + "Natureza para lhe auxiliar. "
                    + "É possível invocar um número maior de animais com esta magia aumentando seu Custo de "
                    + "Conjuração em +2PM para cada animal adicional. "
                    + "Você pode usar esta magia em conjunto com Predador Regional, mas deverá utilizar +1PM para "
                    + "cada animal fortalecido. "
                    + "Não é possível manter invocações de Canção de Flora e Aliados da Natureza simultaneamente. "
                    + "Conjurar Canção de Flora enquanto houver algum animal invocado por conjurações anteriores de "
                    + "Canção de Flora dissipa a conjuração anterior.")
            .effectChainDescription("Fauna Flora: Como efeito adicional invoca 1 Aliado da Natureza para cada 3 "
                    + "Graduações em Conhecimento: Natureza que você possuir.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.concentracaoMais(2))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    /**
     * Spelled "Experimento de Larcerto" on its own identity line and "Experimento de Lacerto"
     * everywhere else in the document, including inside its own Efeito. The identity line is kept
     * as the authored {@code name}; do not "fix" it silently.
     *
     * <p>Its creature is {@code NatureSummonKind#EXPERIMENTO_DE_LACERTO}, its power ({@code LacertoPower}) rolled
     * at cast by {@code NatureInvocationService#invokeExperimento} (table ruling, 2026-10-01).
     */
    EXPERIMENTO_DE_LARCERTO(SpellData.builder()
            .name("Experimento de Larcerto")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.ALIADOS_DA_NATUREZA_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Invoca um animal monstruoso para lhe ajudar.")
            .primaryEffectDescription("Ao conjurar esta magia um animal monstruoso próximo é invocado para lhe "
                    + "ajudar, como uma quimera, um vorme, kraken dentre outros. A criatura invocada é obediente ao "
                    + "conjurador, e assim como Aliado da Natureza, a fera é devolvida ao seu local original ao fim "
                    + "da duração da magia, ou em caso de dano letal. "
                    + "Não é possível manter mais de uma criatura invocada por Experimento de Lacerto ao mesmo tempo.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    /**
     * Creates Predadores Regionais on a timer — the Broto's principal invocation, sustained. The timer is {@code
     * NatureInvocationService#raiseTotem} (a {@code scene.SummonSpawner}); the strengthening of animal allies in
     * Distância Longa is {@code #blessFromTotem}, the caller naming who is in reach.
     */
    TOTEM_DE_GAEA(SpellData.builder()
            .name("Totem de Gaea")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.ALIADOS_DA_NATUREZA_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Cria um totem que invoca Predadores Regionais e fortalece Animais aliados próximos.")
            .primaryEffectDescription("Ao conjurar esta magia um Totem com a forma de diversos animais entalhados "
                    + "surge a frente do conjurador, e em seguida um destes animais ganha vida, dando lugar a um "
                    + "novo entalhe animal. A cada Rodada um novo animal é criado desta forma. "
                    + "Animais criados pelo Totem de Gaea são Predadores Regionais criados pela magia Aliados da "
                    + "Natureza. Todos os animais aliados que estejam em Distância Longa do Totem recebem +1PA, "
                    + "Força +2, Vigor +2 e reduzem o GD de rolagens de Perícias em -1 nível. "
                    + "É possível combinar animais invocador com Totem de Gaea com os invocados por Canção de Flora "
                    + "ou Aliados da Natureza.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    /**
     * Its monster is {@code NatureSummonKind#ORGULHO_DE_LACERTO}, rolling <b>two</b> of the powers {@link
     * #EXPERIMENTO_DE_LARCERTO} rolls one from ({@code NatureInvocationService#invokeOrgulho}); Laboratório de
     * Lacerto is {@code #invokeLaboratorio}.
     */
    ORGULHO_DE_LACERTO(SpellData.builder()
            .name("Orgulho de Lacerto")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.ALIADOS_DA_NATUREZA_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Invoca um monstro verdadeiro para lutar ao seu lado.")
            .primaryEffectDescription("Ao conjurar esta magia um Monstro da região é invocado para lhe ajudar "
                    + "temporariamente. "
                    + "Não é possível manter mais de uma criatura invocada por Orgulho de Lacerto ao mesmo tempo.")
            .secondaryEffectDescription("Laboratório de Lacerto: Ao invés de invocar um monstro com as qualidades do "
                    + "Orgulho de Lacerto, invoca simultaneamente 2 Experimentos de Lacerto, este efeito não é "
                    + "cumulativo com a magia Orgulho de Lacerto.")
            .alternateEffect(SpellAlternateEffect.named("Laboratório de Lacerto"))
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.concentracaoMais(2))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    /**
     * The convergence rung — no ramificação, so it sits on every path, which is the whole
     * convergence mechanism. See {@link SpellTree}.
     *
     * <p>The Anciente is {@code NatureSummonKind#ANCIENTE} ({@code NatureInvocationService#awakenAnciente}), its
     * immunity to Efeitos Críticos Menores and harmful Encantamentos included. TODO Benção da Regeneração (a rolled
     * per-Rodada heal) and Benção Compartilhada (that heal to adjacent allies in even Rodadas) are the caller's.
     */
    DESPERTAR_ANCIENTE_DE_GAEA(SpellData.builder()
            .name("Despertar Anciente de Gaea")
            .branchLevel(BranchLevel.FLORESCENTE)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Invoca um Anciente, uma poderosa e gentil Árvore Humanoide, para te auxiliar.")
            .primaryEffectDescription("Como parte da conjuração desta magia é preciso tocar uma árvore, invocando "
                    + "sobre ela a marca do Chamado de Gaea, despertando-a como um Anciente. "
                    + "Ancientes são algumas das encarnações mais poderosas da Natureza e sua invocação é exaustiva, "
                    + "mas normalmente traz grandes benefícios, principalmente em um combate. Possuem Atributos 10, "
                    + "podem realizar rolagens de quaisquer Perícias com Bônus igual ao ‘Domínio do Mana’ de seu "
                    + "conjurador ou +20, o que for maior, e causam 3d6+7 de dano, possuem Categoria de Tamanho +3, "
                    + "3PA, 70PV (Multiplicador de PV x6), DF +20 e DM +25.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.concentracaoMais(2))
            .targeting(SpellTargeting.TOQUE)
            .build());

    private final SpellData data;

    AliadosDaNaturezaSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.ALIADOS_DA_NATUREZA;
    }
}
