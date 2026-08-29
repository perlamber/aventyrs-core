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
 * VIDA (Natural/Divina) — nine Magias, the catalog's joint-largest tree, diverging at Broto into a
 * healing line and a cleansing line that never converge.
 *
 * <p>Eight of its nine are {@code Pessoal ou Toque} and eight are Instantânea, which makes it the
 * most uniform tree in the catalog after Regeneração.
 *
 * <h2>Recovery is expressed as a Descanso, which is a mechanism this core has</h2>
 *
 * Five of these Magias heal by naming a rest tier — "como se passasse por um Descanso Longo",
 * "Descanso Mínimo", "Descanso Total" — rather than a number of PV. {@code RestService#applyRest}
 * and {@code RestType} are real, so the amounts these describe are already computable; what is
 * missing is a way for a Magia to <em>invoke</em> a rest on a target without the rest actually
 * happening ("Este é um efeito similar a Descanso e não substitui Descansos reais").
 *
 * <p>The cleansing branch is blocked further back: every rung of it removes a Malefício by kind
 * (Doença, Veneno, Maldição, Possessão), and no Malefício classification exists at all.
 */
public enum VidaSpell implements AuthoredSpell {

    /**
     * "Interrompe qualquer efeito de Sangramento" is the one clause in this tree that is fully
     * expressible: {@code Sangramento} is a real {@code CriticalEffect} and {@code
     * CombatantSheet#heal} already clears it. TODO the rest is not — the once-per-Descanso-Longo
     * limit is per-target effect history, and the Efeito Alternativo defers damage to the
     * following Rodada, which nothing in {@code DamageService} can postpone.
     */
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
            .secondaryEffectDescription("Procrastinar Ferimento: Como uma Reação você pode fazer com os PV que você, "
                    + "ou um aliado em Distância Curta, perderia em decorrência de um ataque sejam perdidos apenas "
                    + "no Rodada seguinte. Corrente de Efeitos - Estancar: Se o dano sofrido fosse causar efeitos de "
                    + "Sangramento ele não causará este efeito.")
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
            .effectChainDescription("Sobrecura: O alvo desta magia adicionalmente recupera +1d6+Metade do Foco PV.")
            .secondaryEffectDescription("Benção Bifurcada: Você pode curar até 2 alvos ao mesmo tempo. O GD da "
                    + "Conjuração muda pra Médio, se bem-sucedido ambos os alvos recuperam PV como se passassem por "
                    + "um Descanso Mínimo. Pode aplicar a Corrente de Efeitos – Sobrecura.")
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
     */
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
            .effectChainDescription("Sobrecura.")
            .secondaryEffectDescription("Cura em Massa: Ao invés de afetar um único alvo você pode fazer com que "
                    + "você e todos os outros personagens à até 2m de você recuperem PV como se passassem por "
                    + "Descanso Mínimo. Pode aplicar a Corrente de Efeitos – Sobrecura.")
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
            .criticalEffectType(CriticalEffectType.IMUNIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /** The one Magia of this tree that is not {@code Pessoal ou Toque} — an area centred on the caster, healing friend and foe alike at different rates. */
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
            .effectChainDescription("Sobrecura.")
            .secondaryEffectDescription("Fonte da Juventude: Sua onda de energia curativa recupera PV, PM e PD de "
                    + "todas as criaturas afetadas, mas não recupera o conjurador, como se passassem por um descanso "
                    + "curto. "
                    + "Criaturas hostis ao conjurador recuperam pontos como se passassem por um descanso mínimo.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_CURTA)))
            .build()),

    /** Its Corrente turns an expelled summon against whoever called it — the forced-targeting gap, plus the Possessão classification that does not exist. */
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
            .effectChainDescription("Barganha Negra: Ao invés de apenas expulsar o possuidor você pode fazer com que "
                    + "a criatura expulsa, apenas no caso de ter sido invocada por outro conjurador, tente possuir "
                    + "aquele que a invocou.")
            .criticalEffectType(CriticalEffectType.IMUNIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /** "recupera todos os PV perdidos" is a full heal, which {@code CombatantSheet#heal} can already express exactly — one of the few effects in this catalog that needs no new mechanism. */
    BENCAO_DA_LUZ(SpellData.builder()
            .name("Benção da Luz")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.VIDA_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Você pode recuperar totalmente uma criatura ferida.")
            .primaryEffectDescription("Seu toque recupera todos os PV perdidos da criatura tocada.")
            .secondaryEffectDescription("Corrente Abençoada: Você pode afetar criaturas adicionais com esta magia, "
                    + "desde que elas estejam adjacentes a você ou a uma criatura curada por esta magia. Para cada "
                    + "criatura adicional é necessário o uso de +3PM.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The second of only two self-only Concentração Magias in the catalog — but only when cast at
     * {@code Pessoal}; its other reach is Toque, in which case it sustains an effect on somebody
     * else's sheet like the other seventeen. The other is {@code PiromanciaSpell#LUZ_DE_VELA}.
     */
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
