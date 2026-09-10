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
 * TEMPO (Encantamento/Temporal) — seven Magias, diverging at Broto into a hastening line and a
 * slowing line, converging at Emergente. One of the two trees that required {@link
 * org.aventyrs.core.magic.MagicType#TEMPORAL} to exist.
 *
 * <p><b>The cleanest branch trace in the catalog</b>: its Semente grants +2UD Movimento and its
 * Efeito Alternativo takes 2UD away, and the two ramificações are exactly those two clauses
 * carried forward. Both halves of L30 point the same way with nothing left over.
 *
 * <p>Mechanically it is also the tree closest to working. Pontos de Ação are real, and both the
 * grants and the reductions are {@code ModifierType#ACTION_POINTS} {@code TemporaryBonus}es the
 * {@code CombatantSheet}-taking {@code ActionPointsService#getMaxActionPoints} overload already
 * reads. TODO what is missing is the <b>bounds</b>: these Magias variously cap at 5PA, at 6PA,
 * "quaisquer limites" (Parar o Tempo removes the cap outright), and floor at 2PA — and the service
 * clamps at 0 with no ceiling at all.
 *
 * <p>It also carries three of the source document's blank descriptors, more than any other tree.
 */
public enum TempoSpell implements AuthoredSpell {

    /**
     * <b>The most damaged entry in the source document.</b> Three separate problems, none of them
     * silently repaired:
     *
     * <ul>
     *   <li>Its {@code Perícia Chave para Conjuração:} line is blank, so {@code
     *       getAttackSkillType()} is {@code null} — the only Magia in the catalog with no delivery
     *       Perícia at all. Note that makes it an {@code AttackSource} answering {@code null},
     *       which every {@code resolve*} hook keyed on a Perícia will simply not match.</li>
     *   <li>Its {@code GD da Conjuração:} line is blank too.</li>
     *   <li>Its {@code Descrição:} says "reduz a velocidade de movimento de seu alvo" while its
     *       {@code Efeito:} says "Movimento Base aumentado em +2UD" — the description belongs to
     *       its Efeito Alternativo, <i>Reduzir Passos</i>. Both are transcribed as printed.</li>
     * </ul>
     *
     * <p>Its {@code Tempo de Ativação:} is also the catalog's one conditional activation — "2 PA,
     * pode ser conjurado como Reação por personagens com Domínio do Mana 5 ou superior ao custo de
     * 3PM". Authored as its base 2PA; see {@code ActivationTime}'s own note on why the alternative
     * is not modelled.
     *
     * <p>The +2UD half is otherwise real and lands exactly: a permanent-shaped {@code
     * ModifierType#MOVEMENT} change is per Ponto de Ação spent moving, which is what "Movimento
     * Base aumentado" means.
     */
    AUMENTAR_PASSOS(SpellData.builder()
            .name("Aumentar Passos")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(2))
            .description("Esta magia reduz a velocidade de movimento de seu alvo.")
            .primaryEffectDescription("O personagem alvo tem seu Movimento Base aumentado em +2UD. "
                    + "Esta magia não afeta objetos e construtos. Um mesmo personagem não pode ser afetado por esta "
                    + "magia em turnos consecutivos.")
            .effectChainDescription("Enredar: Em substituição aos efeitos anteriores, o Movimento Base do alvo é "
                    + "reduzido em -4UD, até o mínimo de 1UD.")
            .secondaryEffectDescription("Reduzir Passos: A perícia chave para conjuração desta magia muda para "
                    + "Ataque à Distância. "
                    + "Personagem afetados por Reduzir Passos tem seu Movimento Base reduzido em -2UD. Esta magia "
                    + "não afeta objetos e construtos, e nunca reduz o Movimento Base de um personagem a menos que "
                    + "2UD.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(1))
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_CURTA))
            .build()),

    /** "não permite ao alvo superar o limite de 5PA" — a ceiling {@code ActionPointsService} does not have. */
    ASAS_NOS_PES(SpellData.builder()
            .name("Asas nos Pés")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.TEMPO_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .description("O alvo desta magia se torna mais veloz.")
            .primaryEffectDescription("Personagens tocados por esta magia adquirem +1PA adicional. "
                    + "Esta magia não permite ao alvo superar o limite de 5PA e anula os efeitos de Raízes nos Pés.")
            .secondaryEffectDescription("Aprimorar Condução: O movimento de um veículo ou animal de montaria é "
                    + "aumentado em 50%.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /** Its "anula os efeitos de Asas nos Pés" and that Magia's mirror clause are mutual cancellation; nothing lets one effect remove a named other. */
    RAIZES_NOS_PES(SpellData.builder()
            .name("Raízes nos Pés")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.TEMPO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Esta magia reduz a velocidade de seu alvo.")
            .primaryEffectDescription("Personagens afetados por esta magia sofrem redutor de -1PA. "
                    + "Esta magia nunca reduz o total de PA do alvo a menos que 2PA e anula os efeitos de Asas nos "
                    + "Pés.")
            .secondaryEffectDescription("Retardar Condução: O Alcance desta magia muda para Alvos Distantes – Cone "
                    + "Curto e a Duração é reduzida para 1 Rodada. Veículos e Montarias afetadas tem seu "
                    + "deslocamento reduzido à metade.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_MEDIA))
            .build()),

    ACELERAR(SpellData.builder()
            .name("Acelerar")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.TEMPO_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Esta magia torna seus alvos extremamente velozes.")
            .primaryEffectDescription("Alvos desta magia adquirem temporariamente +2PA, até um máximo de 6PA.")
            .secondaryEffectDescription("Aceleração em Massa: O alcance desta magia muda para Pessoal e Adjacente. O "
                    + "conjurador e aliados adjacentes adquirem +1PA. Conjurar a magia desta forma exige o uso de +1 "
                    + "PM para cada criatura adjacente afetada.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * <b>Its {@code Perícia Chave para Conjuração:} line is blank</b> — the second of the two such
     * blanks, both in this tree. {@code getAttackSkillType()} is {@code null}.
     */
    LENTIDAO(SpellData.builder()
            .name("Lentidão")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.TEMPO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Reduz drasticamente a velocidade de um ou mais personagens.")
            .primaryEffectDescription("O alvo desta magia tem sofre redutor de -2PA. "
                    + "Esta magia nunca reduz o total de PA do alvo a menos que 2PA.")
            .secondaryEffectDescription("Lentidão em Massa: O Alcance desta magia muda para Alvo Distante - cone de "
                    + "até 6 metros, criaturas afetadas sofrem redutor de -1PA.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_CURTA))
            .build()),

    /**
     * The convergence rung, and it does both branches' jobs at once — allies gain a Ponto de Ação
     * inside the field and enemies lose one, which is the two ramificações folded back together.
     *
     * <p>The second of the catalog's two placed-centre areas: "Ponto Central em Distância Média,
     * Área de Efeito – Área Circular Curta" is a Círculo whose centre may be put up to Distância
     * Média away, which only a non-emanation shape may be.
     */
    PARADOXO_TEMPORAL(SpellData.builder()
            .name("Paradoxo Temporal")
            .branchLevel(BranchLevel.EMERGENTE)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Um campo que acelera os aliados e desacelera os inimigos.")
            .primaryEffectDescription("Cria uma área que, dentro dela, o conjurador e seus aliados adquirem "
                    + "temporariamente +1PA, e inimigos sofrem redutor de -1PA. "
                    + "Esta magia não reduz o total de PA dos inimigos para menos que 2, personagens que deixem a "
                    + "área imediatamente deixam de receber seus efeitos.")
            .secondaryEffectDescription("Entrelaçamento Temporal: A Duração da Conjuração muda para Concentração +1 "
                    + "Rodada. Você e seus aliados recebem temporariamente +2PA.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(2))
            .targeting(SpellTargeting.areaDeEfeito(Range.DISTANCIA_MEDIA,
                    AreaOfEffect.circle(Range.DISTANCIA_CURTA)))
            .build()),

    /**
     * Its {@code Efeito Crítico:} line reads "Amenizar*" with a trailing asterisk and no footnote
     * anywhere in the document. Authored as Amenizar; the asterisk is noted here rather than
     * silently dropped or guessed at.
     *
     * <p>"tem seu total de PA dobrados … te permite superar quaisquer limites de PA total" is a
     * multiplicative stage plus a cap removal, and {@code ActionPointsService} sums additively with
     * no ceiling to remove.
     */
    PARAR_O_TEMPO(SpellData.builder()
            .name("Parar o Tempo")
            .branchLevel(BranchLevel.FLORESCENTE)
            .activationTime(ActivationTime.ACAO_LIVRE)
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("O conjurador afeta as matrizes do universo, interrompendo brevemente o fluxo do tempo.")
            .primaryEffectDescription("Ao conjurar essa magia, o conjurador tem seu total de PA dobrados. "
                    + "Este efeito é cumulativo com outras magias desta mesma Árvore de Magias e te permite superar "
                    + "quaisquer limites de PA total em sua Rodada.")
            .secondaryEffectDescription("Prisão Temporal: O Alcance desta magia é alterada para toque, a criatura "
                    + "tocada é removida brevemente do tempo, perdendo o seu próximo turno. "
                    + "Enquanto fora do tempo o alvo não pode ser afetado por nenhum outro efeito e não sofre danos "
                    + "proveniente de ataques. Efeitos que seriam desencadeados no turno do alvo deixam de fazê-lo, "
                    + "e são ativados apenas na rodada seguinte. Efeitos que afetem o alvo e que possuam uma duração "
                    + "não tem seu tempo reduzido enquanto o alvo está fora do tempo. "
                    + "Você não pode afetar um mesmo personagem com Prisão Temporal até que você passe por um "
                    + "Descanso Longo.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .build());

    private final SpellData data;

    TempoSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.TEMPO;
    }
}
