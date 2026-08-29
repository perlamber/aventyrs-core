package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
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
 * <p>Every Magia here invokes a creature, which is the tree's standing limitation: an
 * invocation's stat block is a {@code MonsterTemplate}, and none of these has one. The {@code
 * Atributos e Perícias}/{@code Características Especiais}/{@code Outras Informações} blocks are
 * summarised in each {@code Efeito:} rather than transcribed, because a {@code Spell} has nowhere
 * to put them — see the TODO on each constant that carries one.
 */
public enum AliadosDaNaturezaSpell implements AuthoredSpell {

    /**
     * The only Magia of this tree delivered by Ataque Corpo-a-Corpo, and one of two in the whole
     * catalog whose GD is a <b>floor</b> rather than a tier — "Fácil (13|14) ou DM do Alvo
     * (maior)".
     *
     * <p>TODO the Subordinado grade it grants (Cavaleiro/Torre, Prodigioso on the Efeito
     * Alternativo) has no shape in this core: nothing models a controlled ally's command grade.
     * TODO "não pode ser alvo deste efeito uma segunda vez sem que antes passe por um Descanso"
     * needs per-target effect history, which no sheet records.
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
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The Magia both ramificações are traced back from — its Efeito Alternativo, Predador
     * Regional, is what {@link MagicBranch#ALIADOS_DA_NATUREZA_ALTERNATIVO} evolves.
     *
     * <p>TODO the invoked animal is a full stat block (Força 4, Destreza 3, Vigor 1, Gnose 1,
     * Instinto 3, Foco 1, Carisma 2; Ataque Corpo-a-Corpo [Primal] +4, Atenção [Sentidos Apurados]
     * +2, Esquiva e Aparar [Guerreiro Natural] +2, Furtividade [Maestria da Ocultação] +2;
     * Tamanho +0, 3PA, 15PV at Multiplicador de PV x5, Defesas +3, Danos 1d6+4). That is a {@code
     * SummonedMonsterTemplate} parameterized by the Conjurador's Graduações em Domínio do Mana,
     * exactly the {@code Zumbi} shape — but {@code Spell} has no column pointing at one, so
     * nothing links the Magia to the creature it invokes.
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
     * <p>TODO the invoked creature is a stat block (Força 6, Destreza 4, Vigor 3, Gnose 1,
     * Instinto 4, Foco 1, Carisma 2; Tamanho +1, 3PA, 25PV at Multiplicador de PV x5, DF +12 /
     * DM +8, Danos 1d6+7) that additionally rolls one of six random powers — Inocular Veneno,
     * Sopro Elemental, Aura Elemental, Devorar Inteiro, Bruto, Membros Múltiplos. No {@code
     * MonsterTemplate} models a randomly-chosen power set.
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

    /** Creates Predadores Regionais on a timer — the Broto's principal invocation, sustained. */
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
     * TODO the invoked monster is a stat block (Força 8, Destreza 7, Vigor 5, Gnose 1, Instinto 6,
     * Foco 1, Carisma 3; Tamanho +2, 4PA, 35PV at Multiplicador de PV x5, DF +18 / DM +18, Danos
     * 2d6+5) carrying <b>two</b> randomly-chosen powers from the same six-entry list {@link
     * #EXPERIMENTO_DE_LARCERTO} rolls one from.
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
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.concentracaoMais(2))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    /**
     * The convergence rung — no ramificação, so it sits on every path, which is the whole
     * convergence mechanism. See {@link SpellTree}.
     *
     * <p>TODO the Anciente is a stat block (Atributos 10 per its Descrição, then Força 8, Destreza
     * 10, Vigor 10, Gnose 3, Instinto 8, Foco 4, Carisma 6 in its own table; Tamanho +3, 3PA, 70PV
     * at Multiplicador de PV x6, DF +18 / DM +21, Danos 2d6+7) with two clauses this core cannot
     * express either: "Imunidade a Efeitos Críticos Menores" is severity-scoped, and {@code
     * CriticalEffectType} identifies which effect, never its Maior/Menor tier; and Benção
     * Compartilhada heals adjacent allies on even Rodadas, a continuous cross-character grant of
     * healing rather than of a stat.
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
