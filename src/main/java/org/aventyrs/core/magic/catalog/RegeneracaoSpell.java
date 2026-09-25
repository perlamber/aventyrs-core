package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.DurationUnit;
import org.aventyrs.core.magic.SpellAlternateEffect;
import org.aventyrs.core.magic.SpellData;
import org.aventyrs.core.magic.SpellDuration;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * REGENERAÇÃO (Encantamento/Elemental: Água) — six Magias, one of only two trees to diverge as
 * late as Emergente, converging again at Florescente.
 *
 * <p><b>Every one of the six shares a Duração, an Alcance and an Efeito Crítico</b>: {@code ‘Vigor
 * do Alvo’ Rodadas}, Toque, Potencializar. That makes it the whole of {@code
 * DurationKind#TARGET_ATTRIBUTE}'s catalog — all six Attribute-derived durations in the game are
 * here, and every one reads the <em>target's</em> Vigor rather than the caster's. Nothing resolves
 * it; which Attribute is authored data, and a caller with the target's sheet would multiply.
 *
 * <p>Healing itself is real ({@code CombatantSheet#heal}), and so is the per-Rodada drip these six
 * all describe: {@code org.aventyrs.core.sheet.Regeneration} is a {@code TemporaryBonus} that
 * heals on each of its holder's Turns, bounded by a Rodada count and an optional total budget.
 * This tree is therefore <b>unblocked but unwired</b> — unlike {@code VidaSpell}, no constant here
 * authors a {@code SpellHealing} yet, so nothing resolves one into a {@code
 * org.aventyrs.core.effect.HealingEffect}.
 *
 * <p>Two things a wiring pass would have to add, since {@code SpellHealing} holds neither: the
 * <b>recurring</b> figure beside the instant one (every rung here is "recupera X PV, depois disso
 * recupera +N PV por Rodada"), and the {@code DurationKind#TARGET_ATTRIBUTE} duration above as the
 * Regeneration's Rodada count.
 */
public enum RegeneracaoSpell implements AuthoredSpell {

    /** TODO "não pode afetar o mesmo alvo até que ele passe por um Descanso Longo" is per-target effect history, which no sheet records. */
    REGENERACAO(SpellData.builder()
            .name("Regeneração")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .description("Alvo recupera lentamente seus pontos de vida.")
            .primaryEffectDescription("Alvo recupera Metade do Foco PV, depois disso recupera +1PV por Rodada. "
                    + "Este efeito não pode afetar o mesmo alvo até que ele passe por um Descanso Longo.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.targetAttribute(AttributeDomain.VIGOR, DurationUnit.RODADA))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    REGENERACAO_MAIOR(SpellData.builder()
            .name("Regeneração Maior")
            .branchLevel(BranchLevel.BROTO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .description("Cura o alvo tocado, após isso ele lentamente recupera pontos de vida a cada rodada.")
            .primaryEffectDescription("O personagem tocado recupera 1d6PV+Metade do Foco, depois disso recupera "
                    + "+1PV por Rodada.")
            .secondaryEffectDescription("Regeneração em Massa: Até 2 personagens adjacentes recuperam Metade do Foco "
                    + "PV, depois disso recuperam +2PV por Rodada.")
            .alternateEffect(SpellAlternateEffect.named("Regeneração em Massa"))
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.targetAttribute(AttributeDomain.VIGOR, DurationUnit.RODADA))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The last Magia before the divergence, and the one both ramificações are named after: its own
     * title honours Undine and its Efeito Alternativo <i>Vingança de Haloi</i> the other deity. See
     * {@link MagicBranch#REGENERACAO_PRINCIPAL}.
     */
    LAGRIMA_DE_UNDINE(SpellData.builder()
            .name("Lágrima de Undine")
            .branchLevel(BranchLevel.MUDA)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Cura um personagem ou recupera um membro amputado.")
            .primaryEffectDescription("O personagem tocado recupera 2d6PV+Metade do Foco, ou regenera um membro "
                    + "amputado em seu próximo turno. "
                    + "Adicionalmente, nas Rodadas posteriores, o alvo recupera +3PV por Rodada.")
            .secondaryEffectDescription("Vingança de Haloi: A Perícia Chave para Conjuração muda para Ataque "
                    + "Corpo-a-Corpo com GD igual à DM do alvo, o personagem tocado sofre 2d6 pontos de dano e você "
                    + "recupera 2PV por uma quantidade de Rodadas igual ao dano causado ou regenera um membro "
                    + "amputado. Esta magia possui a Corrente de Efeitos – Sangue Sugas: Esta magia recebe Oferenda "
                    + "Maldita como Efeito Crítico adicional.")
            // TODO: its Corrente grants "Oferenda Maldita como Efeito Crítico *adicional*" — an
            //  addition, where criticalEffectType is a single replaceable column. Left unauthored
            //  rather than overwriting Potencializar with it.
            .alternateEffect(SpellAlternateEffect.builder()
                    .name("Vingança de Haloi")
                    .attackSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .castingDifficultyFlooredByTargetMagicDefense(true)
                    .effectChainDescription("Sangue Sugas: Esta magia recebe Oferenda Maldita como Efeito Crítico "
                            + "adicional.")
                    .build())
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.targetAttribute(AttributeDomain.VIGOR, DurationUnit.RODADA))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * TODO resurrection — reversing DEAD is real now ({@code CombatantSheet#heal(int, HealingSource)} lets a
     * permitted heal lift a dead target back above negative max PV), and "mortos nas últimas N Rodadas" is
     * readable through {@code CombatantSheet#getRoundsSinceDeath()}. Still missing: this tree authors no
     * {@code SpellHealing}; "retorna à vida com Metade do Foco PV" <i>sets</i> PV to a value rather than
     * healing an amount, which needs a revive-to-PV mutator; the permission is the Magia's own, and {@code
     * HealingSource} carries no intrinsic revival permission (only a healer's Título can grant one today);
     * and "voluntários" is narrative. Regeneração is Encantamento/Elemental, so Curar os Mortos does not
     * reach it either.
     */
    RESSURREICAO(SpellData.builder()
            .name("Ressurreição")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.REGENERACAO_PRINCIPAL)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Ressuscita um personagem morto recentemente.")
            .primaryEffectDescription("Ressuscita um personagem morto a até ‘Domínio do Mana’ Rodadas. "
                    + "O alvo retorna a vida com Metade do Foco Pontos de Vida, então recupera +1d6PV por Rodada. "
                    + "Apenas personagens voluntários, mortos nas últimas Metade do Foco Rodadas e que queiram "
                    + "retornar a vida, podem ser ressuscitados com esta magia.")
            .secondaryEffectDescription("Marionete de Undine: O alvo desta magia retorna a vida com metade dos seus "
                    + "PV e recupera 3PV por Rodada, mas deve fazer um favor a deusa Undine (acordado entre o "
                    + "narrador e o jogador cujo personagem foi ressuscitado).")
            .alternateEffect(SpellAlternateEffect.named("Marionete de Undine"))
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.targetAttribute(AttributeDomain.VIGOR, DurationUnit.RODADA))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * "Se um ataque fosse reduzir os PV do alvo para zero ou menos, ao invés disso ele permanecerá
     * com 1PV" is a floor interposed on damage resolution. TODO {@code
     * DamageService#calculateFinalDamage} has three mitigation stages and none of them is a floor
     * on the resulting Hit Points; its Efeito Alternativo goes further still and <em>inverts</em>
     * the sign of the target's remaining PV, which nothing in this core does.
     */
    ESCARNIO_DE_HALOI(SpellData.builder()
            .name("Escarnio de Haloi")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.REGENERACAO_ALTERNATIVO)
            .activationTime(ActivationTime.REACAO)
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("O alvo não pode morrer neste turno e recupera Pontos de Vida.")
            .primaryEffectDescription("Se um ataque fosse reduzir os PV do alvo para zero ou menos, ao invés disso "
                    + "ele permanecerá com 1PV. "
                    + "Nas rodadas posteriores adicionalmente o alvo recupera +1d6PV.")
            .secondaryEffectDescription("Paradoxo do Afogado: O Tempo de Conjuração de Retorno dos Afogados é de 3PA "
                    + "e apenas personagens mortos nas últimas 2 Rodadas podem ser alvos de seus efeitos, o alvo "
                    + "retorna a vida com todos os seus PV, mas perde 1d6PV a cada Rodada e não tem negado qual "
                    + "efeito de cura. Enquanto revividos recebem Vantagem em suas rolagens de Perícias de Ataque e "
                    + "Dano. Ao fim da Duração desta magia a vida do alvo é invertida, valores de PV positivos "
                    + "remanescentes se tornam negativos, valores negativos se tornam positivos.")
            // The second Reação-to-PA conversion in the catalog; its parent is cast as a Reação.
            // ⚠️ Its own sentence names "Retorno dos Afogados", a Magia that appears nowhere as a
            // rung in this document — transcribed as printed, and treated as naming this very
            // alternate. See the tree's javadoc.
            .alternateEffect(SpellAlternateEffect.builder()
                    .name("Paradoxo do Afogado")
                    .activationTime(ActivationTime.pa(3))
                    .build())
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.targetAttribute(AttributeDomain.VIGOR, DurationUnit.RODADA))
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The convergence rung — resurrection with no time limit at all, where Ressurreição has one.
     * TODO blocked on the same pieces as {@link #RESSURREICAO} (no authored heal, no revive-to-PV
     * mutator, no intrinsic revival permission); its Ressurreição em Massa additionally needs an Área
     * de Efeito footprint resolved.
     */
    RENASCIMENTO_NAS_AGUAS(SpellData.builder()
            .name("Renascimento nas Águas")
            .branchLevel(BranchLevel.FLORESCENTE)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Ressuscita um personagem morto.")
            .primaryEffectDescription("Ressuscita um personagem morto que queira retornar a vida. "
                    + "O alvo é ressuscitado com Metade de seus Pontos de Vida, depois disso recupera 1d6+Metade do "
                    + "Foco PV a cada Rodada, personagens ressuscitados desta forma recebem Vantagem em suas "
                    + "Rolagens de Perícia, Dano e Efeitos de Magia enquanto estiverem sob os efeitos curativos da "
                    + "magia.")
            .secondaryEffectDescription("Ressurreição em Massa: Você pode fazer com que todos os personagens mortos "
                    + "em Distância Curta retornam a vida com Metade do Foco PV.")
            .alternateEffect(SpellAlternateEffect.named("Ressurreição em Massa"))
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.targetAttribute(AttributeDomain.VIGOR, DurationUnit.RODADA))
            .targeting(SpellTargeting.TOQUE)
            .build());

    private final SpellData data;

    RegeneracaoSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.REGENERACAO;
    }
}
