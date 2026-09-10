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
 * MORTE (Profana) — eight Magias, diverging at Broto and converging at Florescente. The catalog's
 * joint-largest tree.
 *
 * <p>Every Magia here applies the Malefício <b>Amaldiçoado</b>, and several read whether the
 * target was <em>already</em> cursed. TODO none of that is expressible: there is no
 * Encantamento/Maldição/Doença classification anywhere in this core, so a Malefício cannot be
 * tagged as a curse, and nothing lets an effect ask what other effects its target is under.
 *
 * <p>It is also the tree where the caster pays in PV — four of the eight cost "metade do dano que
 * causar". That is self-damage tied to the amount dealt, which {@code DamageService} never
 * computes in reverse.
 */
public enum MorteSpell implements AuthoredSpell {

    /**
     * <b>Its {@code GD da Conjuração:} is "DM do alvo" with no tier at all</b> — the one entry in
     * the catalog written that way, as against the two that name a tier "ou DM do Alvo (maior)".
     * Authored as a {@code null} tier with the floor flag set, which reads exactly as intended: a
     * floor of nothing is always beaten by the target's Defesa Mágica.
     *
     * <p>Its principal effect — denying the use of Ego — is what {@link
     * MagicBranch#MORTE_PRINCIPAL} is traced to. {@code EgoPointsService} is real and both pools
     * are live, so TODO the missing piece is narrow: nothing suppresses a spend, and {@code
     * CombatantSheet#spendEgoPoints} has no gate to refuse one.
     */
    IMPOR_ARREPSIA(SpellData.builder()
            .name("Impor Arrepsia")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.REACAO)
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Uma maldição que impede outros personagens de se beneficiarem do ‘Ego’.")
            .primaryEffectDescription("O alvo desta magia tem seu uso de ‘Ego’ negado e não pode fazer novos usos de "
                    + "‘Ego’ enquanto sob efeito desta magia. O ponto de ‘Ego’ que seria usado não é perdido. "
                    + "Impor Arrepsia é uma Maldição e aplica o Malefício Amaldiçoado no alvo enquanto os efeitos da "
                    + "magia estiverem ativos. "
                    + "Os PdN que tentarem utilizar efeitos de ‘Ego’ podem ser afetados por Traumatizar, se isto "
                    + "ocorrer o efeito solicitado pelo Narrador é perdido e os jogadores recebem os pontos "
                    + "temporários de ‘Ego’ normalmente.")
            .effectChainDescription("Traumatizar: Em adição ao efeito anterior, o alvo desta magia perde o ponto de "
                    + "Ego que tentou utilizar e sofre Desvantagem em sua próxima rolagem de perícia. Traumatizar "
                    + "não afeta um mesmo personagem duas vezes no mesmo dia.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(1))
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_CURTA))
            .build()),

    /**
     * <b>Carries the source document's run-on Corrente name.</b> Its Efeito Alternativo reads
     * "…aplica a Corrente de Efeitos – Veneno Sombrio mesmo que não supere a DM do alvo em 5,
     * também recebe adicionalmente a Corrente de Efeitos – Perdição Arcana: …" — two Correntes and
     * a condition on one line. Transcribed whole, so neither name is lost the way a
     * name-extracting parse would lose <i>Perdição Arcana</i>.
     *
     * <p>"você então perde uma quantidade de Pontos de Vida igual à metade do dano que causar" is
     * Roubo de Vida run backwards; {@code LifeStealService} only ever gives the attacker PV.
     * "Mortos-Vivos são imunes aos efeitos desta magia" is real data on the other side — {@code
     * MonsterTemplate#isUndead()} exists — but there is no hook for a Magia to exempt a target.
     */
    TOQUE_ANTIVIDA(SpellData.builder()
            .name("Toque Antivida")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.MORTE_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Esta magia drena a força vital do conjurador e de seu alvo, entregando-as a Escuridão, ou "
                    + "outras criaturas Profanas.")
            .primaryEffectDescription("Como parte da conjuração desta magia um outro personagem deve ser tocado. "
                    + "Durante esta ação as mãos do conjurador ficam coberta por uma fumaça negra e fétida de "
                    + "energia Profana. "
                    + "O personagem tocado pelo conjurador sofre 3d6+Metade do Foco pontos de danos Mágico Profano, "
                    + "você então perde uma quantidade de Pontos de Vida igual à metade do dano que causar. "
                    + "Esta magia é uma Maldição e aplica a Condição ‘Amaldiçoado’ em seu alvo e no conjurador "
                    + "durante 1 rodada. Mortos-Vivos são imunes aos efeitos desta magia.")
            .effectChainDescription("Veneno Sombrio: Quando conjurado sobre um personagem previamente amaldiçoado, "
                    + "no início de seu próximo turno você e o alvo sofrem 1d6 pontos de Dano Profano e são ambos "
                    + "amaldiçoados por +1 Rodada.")
            .secondaryEffectDescription("Estigma: O dano no alvo é reduzido para 1d6+Metade do Foco, você ainda "
                    + "sofre metade do dano que causar. Este efeito aplica a Corrente de Efeitos – Veneno Sombrio "
                    + "mesmo que não supere a DM do alvo em 5, também recebe adicionalmente a Corrente de Efeitos – "
                    + "Perdição Arcana: Esta magia recebe Roubo de Mana 1, e o alvo perde 1PM.")
            .criticalEffectType(CriticalEffectType.OFERENDA_MALDITA)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * <b>Its {@code Efeito Crítico:} line is missing entirely</b> — not blank like Necropotência's
     * and Reanimar's, simply absent, the descriptor list running straight from its Corrente to its
     * Duração. {@code getCriticalEffectType()} is {@code null}, the same answer a blank line gets.
     */
    IMBUIR_FADIGA(SpellData.builder()
            .name("Imbuir Fadiga")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.MORTE_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Uma maldição que atrapalha a concentração de seus alvos.")
            .primaryEffectDescription("O alvo desta magia recebe o Malefício Amaldiçoado e sofre Desvantagem em suas "
                    + "rolagens de perícia enquanto a maldição perdurar. "
                    + "Se o alvo desta magia estiver previamente amaldiçoado adicionalmente ele sofre Desvantagem em "
                    + "suas rolagens de Dano e Efeitos de magia.")
            .effectChainDescription("Corrente da Fadiga: Você deve escolher um personagem adjacente ao alvo desta "
                    + "magia que não seja você mesmo, se a DM do personagem escolhido for inferior ao resultado de "
                    + "sua rolagem de Ataque Corpo-a-Corpo ele também sofrerá os efeitos desta magia.")
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * One of the four Magias whose Concentração carries its own extra clause: caster and target
     * are bound together for as long as focus holds ("enquanto o conjurador se mantiver
     * concentrado"), so breaking it ends a condition on <em>someone else's</em> sheet. That is
     * precisely the caster-to-sustained-effects link {@code SpellDuration} documents as missing.
     */
    RAIO_ANTIVIDA(SpellData.builder()
            .name("Raio Antivida")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.MORTE_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Uma maldição que conecta o conjurador e seu alvo, roubando vida a vida de ambos.")
            .primaryEffectDescription("Lança um raio de energia escura contra um alvo distante, se bem-sucedido o "
                    + "conjurador e o alvo são conectados e ambos recebem a Condição ‘Amaldiçoado’ enquanto o "
                    + "conjurador se mantiver concentrado. "
                    + "O alvo sofre 1d6+Metade do Foco pontos de dano e, em cada Rodada próxima, no turno do "
                    + "conjurador, sofre 1d6 pontos de dano adicionais automaticamente. "
                    + "Personagens conjuradores desta magia perdem uma quantidade de Pontos de Vida iguais à metade "
                    + "de todo o dano que causarem com ela.")
            .effectChainDescription("Conexão Mortis: O alvo desta magia tem seu Multiplicador de PV reduzidos em 1, "
                    + "e o conjurador tem seu multiplicador aumentado em 1, enquanto ambos estiverem amaldiçoados "
                    + "por esta magia. Se o alvo da magia estiver previamente Amaldiçoado, o dano inicial desta "
                    + "magia é aumentado em +2d6. "
                    + "Os PV ganhos pelo conjurador devido ao aumento do multiplicador são os últimos a serem "
                    + "perdidos.")
            .criticalEffectType(CriticalEffectType.OFERENDA_MALDITA)
            .duration(SpellDuration.concentracaoMais(1))
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_MEDIA))
            .build()),

    /**
     * TODO its whole effect is scoped by <em>which Attribute governs</em> the target's rolls, and
     * it resolves differently for a Personagem Jogador than for a PdN. Neither is expressible:
     * nothing scopes a modifier by governing Attribute, and this core draws no player/PdN
     * distinction — a foe is an ordinary {@code Character} behind a {@code MonsterSheet}.
     */
    ONDA_DA_EXAUSTAO(SpellData.builder()
            .name("Onda da Exaustão")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.MORTE_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Um cone de energia que drena as forças e energias de seus alvos.")
            .primaryEffectDescription("Escolha 1 Atributo. "
                    + "Se o alvo de Onda de Exaustão for um Personagem Jogador todas as rolagens de Perícia "
                    + "efetuadas por este Personagem que tenham o Atributo Base escolhido tem a GD aumenta em 1 "
                    + "Grau. "
                    + "Rolagens destas perícias efetuadas por um Personagem Jogador afetado contra outro Personagem "
                    + "Jogador, ao invés do efeito acima, são feitas em Desvantagem. "
                    + "Por outro lado, se o alvo desta magia for um PdN, a GD das perícias dele (baseadas no "
                    + "atributo escolhido) são reduzidas em 1 Grau. "
                    + "Personagens afetados por Onda de Exaustão são considerados Amaldiçoados enquanto estiverem "
                    + "sob efeito desta magia. "
                    + "O Cone Curto aumenta para Cone Médio se tiver Foco 5 ou superior.")
            .effectChainDescription("Estresse Intenso: Você pode escolher um segundo atributo para afetar aos alvos "
                    + "desta magia. Como efeito adicional os Personagens afetados não pode ativar efeitos de "
                    + "Autocontrole.")
            .criticalEffectType(CriticalEffectType.TOQUE_DO_AETHER)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.cone(Range.DISTANCIA_CURTA)))
            .build()),

    /**
     * "se tornam automaticamente Amaldiçoados … mesmo que sua DM não seja superada" splits one
     * cast into an unavoidable half and a rolled half. Nothing models a Magia landing partially,
     * and this core does not track a Magia's target set at all.
     */
    GRITO_DA_BANSHEE(SpellData.builder()
            .name("Grito da Banshee")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.MORTE_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Um poderoso grito amaldiçoa e causa grande dano aos seus alvos.")
            .primaryEffectDescription("Como parte de sua conjuração o conjurador desta magia solta um grito "
                    + "poderoso, magicamente amplificado, e que pode ser ouvido a grandes distancias. "
                    + "Você e todas as criaturas que ouvirem este grito, que estejam em sua Área de Efeito, se "
                    + "tornam automaticamente Amaldiçoados por 2 Rodadas, mesmo que sua DM não seja superada. "
                    + "O grito do conjurador causa efeito devastador naqueles que estão próximos, outros personagens "
                    + "dentro de sua área de efeito e que tenham a DM superadas sofrem 2d6+Metade do Foco pontos de "
                    + "dano e ficam surdos enquanto estiverem amaldiçoados por esta magia. "
                    + "O conjurador perde uma quantidade de pontos de vida igual à metade do dano que rolar.")
            .effectChainDescription("Maldição da Banshee: Efeitos de Cura em Personagens que tenham sofrido danos "
                    + "por esta magia são reduzidos à metade, os alvos ficam surdos permanentemente. "
                    + "Efeitos que removem maldições curam a surdez.")
            .criticalEffectType(CriticalEffectType.OFERENDA_MALDITA)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_MEDIA)))
            .build()),

    /**
     * The rung that traces this tree's PRINCIPAL branch: "perde temporariamente 3 pontos de todos
     * os seus Egos (exceto Recursos), e não pode utilizar efeitos de Egos" is Impor Arrepsia's own
     * principal effect, deepened.
     *
     * <p>The temporary Ego loss is <b>exactly</b> {@code TemporaryEgoPenalty} — a dedicated {@code
     * TemporaryEffect} lowering the temporary ceiling rather than a spend, which is the case that
     * type was built for. TODO what is still missing is the denial half and the "exceto Recursos"
     * scoping, plus its Corrente's permanent, stacking recovery block.
     *
     * <p>The catalog's only {@code Até 1 Hora} Duração — 720 Rodadas, kept in hours.
     */
    RUINA(SpellData.builder()
            .name("Ruína")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.MORTE_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Esta maldição poderosa, capaz de trazer toda forma de infortúnios ao seu alvo.")
            .primaryEffectDescription("Um personagem tocado com esta magia perde temporariamente 3 pontos de todos "
                    + "os seus Egos (exceto Recursos), e não pode utilizar efeitos de Egos enquanto a duração desta "
                    + "magia estiver ativa. "
                    + "O personagem recebe o Malefício Amaldiçoado enquanto Ruína estiver ativa. "
                    + "Outras rolagens de Perícia efetuadas contra o alvo recebem a Corrente de Efeitos – Marca da "
                    + "Queda: Se esta rolagem não for um Sucesso Crítico Maior ela recebe os benefícios de Sucesso "
                    + "Crítico Menor, independente dos números rolados. Sucessos Críticos Maiores aumentam a duração "
                    + "desta maldição em +1 Rodada. "
                    + "Pontos de Ego perdidos desta forma são recuperados um de cada ego a cada Descanso Total.")
            .effectChainDescription("Macular a Alma: O alvo desta magia sofre uma maldição adicional, de duração "
                    + "permanentemente até ser removida. Esta maldição secundária faz com que o personagem afetado "
                    + "não recupere um dos pontos de Sorte ou Autocontrole perdidos em ‘Ruína’. Este efeito é "
                    + "cumulativo.")
            .criticalEffectType(CriticalEffectType.TOQUE_DO_AETHER)
            .duration(SpellDuration.horas(1))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    /**
     * The convergence rung. Its text follows the Alternativo branch openly ("De Conjuração similar
     * ao Grito da Banshee") while its Ego drain follows the Principal one — which is exactly what
     * a trunk Magia after a convergence should look like.
     *
     * <p>TODO killing a target and enslaving it as a Subordinado needs both the Subordinado grades
     * (Cavaleiro/Torre/Bispo/Rei/Rainha/Peão) that nothing models, and a summon acting on its
     * summoner's roll, which {@code AttackDelivery} has no notion of.
     */
    ESCRAVIZAR_OS_CAIDOS(SpellData.builder()
            .name("Escravizar os Caídos")
            .branchLevel(BranchLevel.FLORESCENTE)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Um Grito capaz de levar a morte aqueles que ouvem e que amaldiçoa os que sobrevivem.")
            .primaryEffectDescription("De Conjuração similar ao Grito da Banshee, Escravizar o Caído tem como parte "
                    + "de seu componente um poderoso grito, afetando com energia profana todos aqueles em sua Área "
                    + "de Efeito. "
                    + "Você e todas as criaturas que ouvirem este grito, que estejam em sua Área de Efeito, se "
                    + "tornam automaticamente Amaldiçoados por 3 Rodadas, mesmo que sua DM não seja superada. "
                    + "Personagens amaldiçoados por essa magia perdem temporariamente 1 ponto de Ego, personagens "
                    + "adjacentes a você perdem o dobro, e não podem usar efeitos de Ego enquanto sob efeito da "
                    + "maldição - Efeitos de perda de Egos não cumulativos. "
                    + "Outros personagens na Área de Efeito sofrem 3d6+ Metade do Foco pontos de Dano, você perde "
                    + "uma quantidade de pontos de vida igual à metade do dano rolado desta forma. "
                    + "O real poder desta magia está além da maldição e dos danos causados, personagens que cheguem "
                    + "à zero ou menos PV em decorrência dos danos sofridos morrem imediatamente e tem sua excessiva "
                    + "vital absorvida e escravizada, se tornando um Subordinados. "
                    + "Personagens combatentes se tornam Cavaleiros ou Torres, conjuradores podem se tornar "
                    + "Cavaleiros, Bispos, Reis ou Rainhas, personagens sem especializações claras se tornam Peões. "
                    + "Você recupera 2PV, 1PD e 1PM para cada essência vital que devorar desta forma, estes são "
                    + "considerados efeitos de Roubo de Bônus Base.")
            .effectChainDescription("Chamado do Rei dos Mortos: Os alvos se tornaram Subordinados Prodigiosos e a "
                    + "quantidade de Pontos de Vida recuperados por devorar essência vital é dobrada.")
            .criticalEffectType(CriticalEffectType.OFERENDA_MALDITA)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_MEDIA)))
            .build());

    private final SpellData data;

    MorteSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.MORTE;
    }
}
