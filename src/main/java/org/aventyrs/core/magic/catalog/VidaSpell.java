package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.SpellAlternateEffect;
import org.aventyrs.core.magic.SpellData;
import org.aventyrs.core.magic.SpellDuration;
import org.aventyrs.core.magic.SpellHealing;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

import java.util.Set;

/**
 * VIDA (Natural/Divina) — nine Magias, the catalog's joint-largest tree, diverging at Broto into a
 * healing line and a cleansing line that never converge.
 *
 * <p>Eight of its nine are {@code Pessoal ou Toque} and eight are Instantânea, which makes it the
 * most uniform tree in the catalog after Regeneração.
 *
 * <h2>The first tree whose effects actually apply</h2>
 *
 * Both ramificações are wired, each through one parameterized {@code
 * org.aventyrs.core.effect.SpellEffect} that deepens rung by rung rather than a class per Magia —
 * see {@code SpellHealingEffect} and {@code ConditionCleansingEffect}. A caller gets one back on
 * {@code SpellCastingResult#getSpellEffect()} and runs it when it judges the cast landed.
 *
 * <p><b>Recovery is expressed as a Descanso, which is a mechanism this core has.</b> Five of these
 * Magias heal by naming a rest tier — "como se passasse por um Descanso Longo", "Descanso Mínimo",
 * "Descanso Total" — rather than a number of PV, and {@link org.aventyrs.core.rest.RestType} has
 * exactly those tiers. {@code SpellHealing} is the authored column; the effect reads {@code
 * RestService#getRecoveredHitPoints} against the <em>target's</em> Vigor and applies it with a bare
 * {@code CombatantSheet#heal}, never {@code RestService#applyRest} — "Este é um efeito similar a
 * Descanso e não substitui Descansos reais", and a real Rest would also restore PM/PD and settle
 * pending Ego recoveries.
 *
 * <p><b>The cleansing branch removes {@code ConditionType}s.</b> Doença, Veneno and Maldição were
 * already authored; {@link org.aventyrs.core.sheet.ConditionType#POSSESSAO} was added for
 * {@link #EXORCIZAR}, from the only rules text that describes it. What stays missing is narrower
 * than "no Malefício classification": nothing can make a target <em>immune</em> to a future one,
 * which is the second half of {@link #CORPO_FECHADO}.
 */
public enum VidaSpell implements AuthoredSpell {

    /**
     * The tree's trunk, and the one Magia here whose recovery is <em>conditional</em>: the
     * bleeding is stopped, or — "ao invés disso" — the target is healed, never both. Carried by
     * {@code HealingCondition#ONLY_IF_NOT_BLEEDING} and applied through {@code
     * CombatantSheet#stopBleeding()}, which interrupts a {@code Sangramento} without healing;
     * {@code heal} clears one too, but only as a consequence of PV coming back, which is the
     * opposite of what this clause says.
     */
    // TODO: "só afeta o alvo 1 vez, voltando a afetá-lo somente após ele passar por um Descanso
    //  Longo" is per-target effect history, which no sheet records — the same limit {@code
    //  RegeneracaoSpell#REGENERACAO} states and is blocked on.
    /**
     * <b>Its Efeito Alternativo is the catalog's only Reação-cast second version</b>, and it
     * overrides three of its parent's columns at once — Tempo de Conjuração 2PA → Reação, Alcance
     * {@code Pessoal ou Toque} → um aliado em Distância Curta, plus a Corrente of its own. That
     * combination is why {@code SpellAlternateEffect} exists rather than a prose string.
     *
     * <p><i>Procrastinar Ferimento</i> is real: {@code DamageInteraction#postponing()} hands the
     * already-mitigated figure to {@code CombatantSheet#schedulePostponedDamage}, and the Rodada
     * boundary applies it. The hit still landed — only the PV loss moved.
     *
     * <p><i>Estancar</i> needs no mechanism, and deliberately gets none: {@code
     * AttackDelivery#resolve} assembles a hit's chain but applies none of it, and every {@code
     * CriticalEffect} reports its own {@code getType()}, so a caller honouring Estancar simply
     * does not run the {@code SANGRAMENTO} stage — the same division of labour {@code
     * CriticalEffect#applicableTo} already follows for a defender's immunities.
     */
    // TODO: nothing suppresses that Sangramento *automatically*. A per-hit (rather than
    //  per-target) filter would need a new parameter threaded through AttackDelivery#buildChain,
    //  and no second clause asks for one yet.
    // TODO: the Reação it costs is reported, never spent — nothing in this core tracks a
    //  combatant's remaining Reações (see MovementReactionService's own documented limit).
    ALIVIAR_A_DOR(SpellData.builder()
            .name("Aliviar a Dor")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .description("Você pode fechar feridas e fazer parar qualquer tipo de dor ao toque.")
            .primaryEffectDescription("Interrompe qualquer efeito de Sangramento sofrido pelo alvo, se ele não "
                    + "estiver sob sangramento, ao invés disso essa magia cura o alvo, como se ele passasse por um "
                    + "Descanso Mínimo. "
                    + "Este é um efeito similar a Descanso e não substitui Descansos reais. "
                    + "O efeito de cura desta magia só afeta o alvo 1 vez, voltando a afetá-lo somente após ele "
                    + "passar por um Descanso Longo.")
            .healing(SpellHealing.restEquivalent(RestType.MINIMO).onlyIfNotBleeding())
            .secondaryEffectDescription("Procrastinar Ferimento: Como uma Reação você pode fazer com os PV que você, "
                    + "ou um aliado em Distância Curta, perderia em decorrência de um ataque sejam perdidos apenas "
                    + "no Rodada seguinte. Corrente de Efeitos - Estancar: Se o dano sofrido fosse causar efeitos de "
                    + "Sangramento ele não causará este efeito.")
            .alternateEffect(SpellAlternateEffect.builder()
                    .name("Procrastinar Ferimento")
                    .activationTime(ActivationTime.REACAO)
                    .targeting(SpellTargeting.distancia(Range.DISTANCIA_CURTA))
                    .effectChainDescription("Estancar: Se o dano sofrido fosse causar efeitos de Sangramento ele "
                            + "não causará este efeito.")
                    .build())
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /** Its Corrente <i>Sobrecura</i> is written inside the Efeito rather than on its own line, and is named again by three deeper Magias of this branch. */
    REVIGORAR(SpellData.builder()
            .name("Revigorar")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.VIDA_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Seu toque devolve a vitalidade dos seus aliados.")
            .primaryEffectDescription("Você pode fazer com que um alvo recupere PV como se passasse por um Descanso "
                    + "Longo. "
                    + "Este é um efeito similar a Descanso e não substitui Descansos reais.")
            .healing(SpellHealing.restEquivalent(RestType.LONGO))
            .effectChainDescription("Sobrecura: O alvo desta magia adicionalmente recupera +1d6+Metade do Foco PV.")
            .secondaryEffectDescription("Benção Bifurcada: Você pode curar até 2 alvos ao mesmo tempo. O GD da "
                    + "Conjuração muda pra Médio, se bem-sucedido ambos os alvos recuperam PV como se passassem por "
                    + "um Descanso Mínimo. Pode aplicar a Corrente de Efeitos – Sobrecura.")
            // "O GD da Conjuração muda pra Médio" restates the parent's own tier, minus its
            // "ou DM do alvo (Maior)" floor — so the override that matters is the floor, not the tier.
            .alternateEffect(SpellAlternateEffect.builder()
                    .name("Benção Bifurcada")
                    .castingDifficultyLevel(DifficultyLevel.MEDIUM)
                    .castingDifficultyFlooredByTargetMagicDefense(false)
                    .healing(SpellHealing.restEquivalent(RestType.MINIMO))
                    .build())
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * One of <b>three</b> Magias whose GD is a table over the rung of the effect being undone
     * rather than a fixed tier — "Efeitos mundanos ou de magias Sementes: Fácil, Brotos: Médio,
     * Mudas: Difícil, Emergentes: Muito Difícil, Florescentes: Improvável". Its siblings are
     * {@link #REMOVER_MALDICAO} and {@code AnulacaoSpell#IDENTIFICACAO}; {@code
     * getCastingDifficultyLevel()} is {@code null} for all three and {@code
     * getCastingDifficultyAgainst} answers instead.
     *
     * <p>The cleansing branch's first rung, lifting {@link ConditionType#DOENTE} and {@link
     * ConditionType#ENVENENADO} for real.
     */
    // TODO: "Apenas doenças de origem mundana, não mágica" is unenforced — a Condição records
    //  what it is and who applied it, never whether its origin was magical, so this currently
    //  lifts a magical Doença too.
    // TODO: its Corrente takes the Malefício onto the caster instead of dispelling it, then moves
    //  it on. Transfer needs a condition to be lifted from one sheet and applied to another
    //  carrying its remaining duration, and nothing copies a Condição between sheets.
    TOQUE_CURATIVO(SpellData.builder()
            .name("Toque Curativo")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.VIDA_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyScaledToTargetLevel(true)
            .description("Você poder curar doenças e remover venenos ao toque.")
            .primaryEffectDescription("Com seu toque você pode remover os Malefícios Doença e Venenos de um alvo. "
                    + "Apenas doenças de origem mundana, não mágica, pode ser removida desta forma.")
            .cleansedConditions(Set.of(ConditionType.DOENTE, ConditionType.ENVENENADO))
            .effectChainDescription("Transferir Doenças e Venenos: Quando for bem-sucedido em usar Toque Curativo, "
                    + "você pode escolher tomar para si o Malefício, ao invés de dissipá-lo, e então transferir para "
                    + "um novo alvo. "
                    + "Você sofre os efeitos negativos do Maleficio enquanto estiver com ele, mas pode transferi-lo "
                    + "a qualquer momento para um novo alvo, esta ação exige uma rolagem de perícia 'Ataque "
                    + "Corpo-a-Corpo', realizada contra DM do alvo. "
                    + "Caso falhe em alguma tentativa de transferir os efeitos, este persistirá no corpo do "
                    + "conjurador até seu próximo Descanso Longo.")
            .criticalEffectType(CriticalEffectType.IMUNIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /** Its Efeito names the Corrente <i>Sobrecura</i> with no body, relying on Revigorar's own text for it; transcribed as printed. */
    REVIGORAR_MAIOR(SpellData.builder()
            .name("Revigorar Maior")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.VIDA_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Com um toque você consegue recuperar grandes quantidades energia vital de seus aliados.")
            .primaryEffectDescription("Você pode fazer com que uma criatura tocada recupere PV como se passasse por "
                    + "um Descanso Total. "
                    + "Este é um efeito similar a Descanso e não substitui Descansos reais.")
            .healing(SpellHealing.restEquivalent(RestType.TOTAL))
            .effectChainDescription("Sobrecura.")
            .secondaryEffectDescription("Cura em Massa: Ao invés de afetar um único alvo você pode fazer com que "
                    + "você e todos os outros personagens à até 2m de você recuperem PV como se passassem por "
                    + "Descanso Mínimo. Pode aplicar a Corrente de Efeitos – Sobrecura.")
            // TODO: "à até 2m de você" is a reach in metres, which no Range band expresses — the
            //  targeting override is left unset and the caller picks who is within it.
            .alternateEffect(SpellAlternateEffect.builder()
                    .name("Cura em Massa")
                    .healing(SpellHealing.restEquivalent(RestType.MINIMO))
                    .build())
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The second of the three rung-scaled GDs — and the only one carrying an <b>extra clause the
     * table cannot hold</b>: "Maldições provenientes de Habilidades Monstruosas ou Aventyrs são de
     * Grau Muito Difícil", a fixed tier for curses that have no {@code BranchLevel} at all because
     * they are not Magias. {@code getCastingDifficultyAgainst} answers only the rung question; that
     * clause stays in the prose.
     */
    REMOVER_MALDICAO(SpellData.builder()
            .name("Remover Maldição")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.VIDA_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyScaledToTargetLevel(true)
            .description("Você pode remover o Malefício Maldição com seu toque.")
            .primaryEffectDescription("Conforme descrição, seu toque com esta magia é capaz de encerrar os efeitos "
                    + "de maldições. "
                    + "Maldições provenientes de Habilidades Monstruosas ou Aventyrs são de Grau Muito Difícil.")
            .cleansedConditions(Set.of(ConditionType.AMALDICOADO))
            .criticalEffectType(CriticalEffectType.IMUNIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The one Magia of this tree that is not {@code Pessoal ou Toque} — an area centred on the
     * caster, healing friend and foe alike at different rates. The halving is real ({@code
     * SpellHealing#halvedForHostiles()}), but <em>which</em> targets are hostile is the caller's:
     * a Magia of this shape needs one effect per combatant in the footprint.
     */
    // TODO: nothing resolves the Área de Efeito into a set of combatants — see CLAUDE.md's
    //  "Area de Efeito" gap. The caller picks the targets and builds an effect for each via
    //  {@code SpellCastingService#resolveEffect(spell, hostileTarget)}.
    // TODO: the Efeito Alternativo recovers PM and PD too, at a different rest tier per group,
    //  and exempts the caster. {@code SpellHealing} holds one tier and PV only.
    NOVA_REJUVENESCEDORA(SpellData.builder()
            .name("Nova Rejuvenescedora")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.VIDA_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Você pode criar um anel de energia curativa que expande rapidamente, recuperando as forças "
                    + "de todas as criaturas ao redor.")
            .primaryEffectDescription("Tendo o conjurador como centro, um pulso de energia positiva corre o cenário "
                    + "e recupera PV de todas as criaturas no alcance da magia como se passassem por um Descanso "
                    + "Longo. "
                    + "Inimigos do conjurador recuperam apenas metade desta quantidade de PV.")
            .healing(SpellHealing.restEquivalent(RestType.LONGO).halvingForHostiles())
            .effectChainDescription("Sobrecura.")
            .secondaryEffectDescription("Fonte da Juventude: Sua onda de energia curativa recupera PV, PM e PD de "
                    + "todas as criaturas afetadas, mas não recupera o conjurador, como se passassem por um descanso "
                    + "curto. "
                    + "Criaturas hostis ao conjurador recuperam pontos como se passassem por um descanso mínimo.")
            // TODO: its recovery is deliberately left unauthored rather than partly authored.
            //  Three things block it, and authoring the PV third alone would read as done:
            //  it restores PM and PD as well as PV, which SpellHealing does not carry; it gives
            //  hostis a *different tier* (Mínimo vs Curto) where halvedForHostiles only halves;
            //  and it exempts the caster, which no column expresses. Note the halving happens to
            //  coincide with Mínimo at today's multipliers (1.0/2 == 0.5) — do not lean on that,
            //  it is arithmetic coincidence, not the rule the text states.
            .alternateEffect(SpellAlternateEffect.named("Fonte da Juventude"))
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_CURTA)))
            .build()),

    /**
     * The Magia that {@link ConditionType#POSSESSAO} was authored for: it is the only clause in
     * the ruleset that names Maldição, Doença and Possessão as three Malefícios of one kind, and
     * the first two already had constants. All three are lifted for real.
     *
     * <p>Note its Descrição promises more than its Efeito — "doenças mundanas <em>ou mágicas</em>",
     * where {@link #TOQUE_CURATIVO} is limited to mundane ones. Since neither a Condição's origin
     * nor that limit is modelled, the widening is currently invisible either way.
     */
    // TODO: its Corrente turns an expelled summon against whoever called it — CLAUDE.md's "Forced
    //  attack targeting / interception" gap, and it additionally needs a summon to remember which
    //  Conjurador raised it, which SummonedMonsterTemplate does not record.
    EXORCIZAR(SpellData.builder()
            .name("Exorcizar")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.VIDA_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Ao toque você pode remover maldições, doenças mundanas ou mágicas, e findar possessões.")
            .primaryEffectDescription("Tocar uma criatura com esta magia o permite remover todos os Malefícios "
                    + "Maldição, Doença e Possessão de seu alvo.")
            .cleansedConditions(Set.of(ConditionType.AMALDICOADO, ConditionType.DOENTE,
                    ConditionType.POSSESSAO))
            .effectChainDescription("Barganha Negra: Ao invés de apenas expulsar o possuidor você pode fazer com que "
                    + "a criatura expulsa, apenas no caso de ter sido invocada por outro conjurador, tente possuir "
                    + "aquele que a invocou.")
            .criticalEffectType(CriticalEffectType.IMUNIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * "recupera todos os PV perdidos" is a full heal, which {@code CombatantSheet#heal} expresses
     * exactly — the branch's top rung, and the one recovery here that names no Descanso tier.
     */
    // TODO: the Efeito Alternativo's extra targets are gated on adjacency to the caster *or to a
    //  creature already healed by this cast* — pairwise geometry between two combatants who are
    //  both not the roller, which a SceneContext cannot answer. Its +3PM per extra target has no
    //  mechanism either: nothing spends PM per target.
    BENCAO_DA_LUZ(SpellData.builder()
            .name("Benção da Luz")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.VIDA_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Você pode recuperar totalmente uma criatura ferida.")
            .primaryEffectDescription("Seu toque recupera todos os PV perdidos da criatura tocada.")
            .healing(SpellHealing.FULL_RECOVERY)
            .secondaryEffectDescription("Corrente Abençoada: Você pode afetar criaturas adicionais com esta magia, "
                    + "desde que elas estejam adjacentes a você ou a uma criatura curada por esta magia. Para cada "
                    + "criatura adicional é necessário o uso de +3PM.")
            // Its recovery is its parent's, unchanged — the alternate only widens who is reached,
            // which is the half that cannot be expressed (see this constant's own TODOs).
            .alternateEffect(SpellAlternateEffect.builder()
                    .name("Corrente Abençoada")
                    .healing(SpellHealing.FULL_RECOVERY)
                    .build())
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The second of only two self-only Concentração Magias in the catalog — but only when cast at
     * {@code Pessoal}; its other reach is Toque, in which case it sustains an effect on somebody
     * else's sheet like the other seventeen. The other is {@code PiromanciaSpell#LUZ_DE_VELA}.
     *
     * <p>The only clause in the ruleset that names the whole category rather than its members,
     * which is what {@link ConditionType#maleficios()} exists for — every Condição but Escondido,
     * and a new constant joins it with no change here.
     */
    // TODO: only the first half applies. "Se torna imune à Malefícios enquanto estiver sob efeito
    //  de Corpo Fechado" needs per-condition immunity — nothing can refuse an applyCondition — and
    //  it would have to last this Magia's Concentração + 2 Rodadas rather than happening at once.
    //  Removal is real; refusing a future Malefício is not. See CLAUDE.md's Malefício-
    //  classification gap, item (c).
    CORPO_FECHADO(SpellData.builder()
            .name("Corpo Fechado")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.VIDA_ALTERNATIVO)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Encerra malefícios e impede o alvo de ser afetado por novos malefícios.")
            .primaryEffectDescription("O alvo desta magia tem todos os Malefícios removidos, este personagem também "
                    + "se torna imune à Malefícios enquanto estiver sofre efeito de Corpo Fechado.")
            .cleansedConditions(ConditionType.maleficios())
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.concentracaoMais(2))
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build());

    private final SpellData data;

    VidaSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.VIDA;
    }
}
