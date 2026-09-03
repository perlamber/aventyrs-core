package org.aventyrs.core.skill.esquivaeaparar;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * The Habilidades de Competência available to characters trained in Esquiva e Aparar.
 */
@Getter
@AllArgsConstructor
public enum EsquivaEApararCompetencyAbility implements SkillCompetencyAbility {

    // Real: shifts the armor-Categoria Destreza penalty one bracket lighter — see
    // EsquivaEApararInteraction#armorCategoryPenalty, which reads Character#getEquipment() for
    // the heaviest equipped Defensive item's ItemWeightClass. This constant needs no @Modifier
    // of its own: the penalty isn't a flat bonus, it's a scaling deduction the Interaction
    // computes, and it checks for this constant by identity there.
    ENCOURACADO_E_VELOZ("Você pode adicionar seu valor integral de Destreza em suas " +
            "rolagens mesmo enquanto utilizando Equipamentos de Categoria Natural Média, e " +
            "metade quando equipados com Equipamentos Naturalmente Pesados."),

    // TODO: +3 Defesas scoped to resisting Área de Efeito attacks specifically, rising to +5
    // once 7 Graduações are reached. The Defesas half is no longer what blocks this —
    // ModifierType.PHYSICAL_DEFENSE/MAGIC_DEFENSE and DefenseService are real now, so a flat
    // Defesa bonus is expressible. Two blockers remain: (1) an Área de Efeito can now be
    // *described* (scene.AreaOfEffect, reachable from Spell#getTargeting()), but nothing marks
    // an *incoming* attack as an area one — AttackReceiver carries no such classification — so
    // there is still no flag for DefenseService to scope this bonus to; and (2) it's a
    // graduation-tiered scaling bonus (same shape as
    // DominioDoManaCompetencyAbility.LETALIDADE_ARCANA) — @Modifier supports only a fixed value
    // per constant and can't read the holder's own graduation to pick +3 vs +5.
    EVASAO("Defesas +3 para resistir à ataques e efeitos com Área de Efeito, benefício muda " +
            "para +5 ao alcançar 7 Graduações."),

    // Note: scoped to rolls made specifically in response to a Reação-triggering effect, but
    // this codebase doesn't track what a roll is *for* (same simplification as
    // DirigirECavalgarCompetencyAbility.CONTROLAR_ANIMAIS), so it's implemented as an
    // unconditional flat bonus to every Esquiva e Aparar roll rather than silently narrowed.
    // Deliberately still SKILL_ROLL_BONUS rather than ModifierType.DEFESAS now that the latter
    // has a reader: its rules text says "em suas Defesas", but in this ruleset an Esquiva e
    // Aparar roll *is* the Defesa (see DefenseType), and this bonus applies whether the roll is
    // resisting with DF, with DM, or with neither — retyping it to DEFESAS would narrow it to
    // the DF/DM-typed rolls alone. Same reasoning keeps EsquivaEApararExcellency's own
    // "Defesas +1/+3" tiers on SKILL_ROLL_BONUS.
    MOVIMENTO_DEFENSIVO("Contra efeitos de Reações você recebe bônus de +3 em suas " +
            "Defesas.") {
        @Modifier(ModifierType.SKILL_ROLL_BONUS)
        public int defesasBonus() {
            return 3;
        }
    },

    // TODO: an activated ability rolling Esquiva e Aparar instead of an Ataque Perícia
    // against GD Difícil; on success, reduces the GD to attack that specific target by -1
    // Nível for 2 Rodadas. "Success against a fixed GD" is answerable now (a SkillRoll carries
    // its own targetValue and InteractionResult reports succeeded), and a Rodada-scoped grant is
    // an ordinary Blessing via resolveSuccessBlessings — but the grant here is a GD *reduction*
    // applied to this roller's future attacks against one specific opponent, and a Blessing
    // carries a ModifierType, not a GD step scoped to a named target. That cross-character,
    // per-opponent shape is what is still missing.
    ESTUDAR_DEFESAS("Você pode efetuar rolagens de Esquiva e Aparar ao invés da Perícia de " +
            "Ataque contra GD Difícil para analisar o oponente, seus padrões de " +
            "movimentos, brechas na armadura etc. Se for bem-sucedido você reduz em o GD " +
            "para efetuar ataques contra o alvo em -1 Nível por 2 Rodadas."),

    // TODO: once per Rodada, after taking damage from an enemy attack, spend a Reação to
    // perform a "Reposicionar" action — needs a damage-triggers-a-Reação-opportunity
    // mechanic, a once-per-Rodada usage-limiting mechanism, and a defined "Reposicionar"
    // action type (this codebase only tracks generic PA/Ação Livre/Reação counters, not a
    // catalog of distinct named actions), none of which exist yet.
    RECUO_RAPIDO("Apenas uma vez por Rodada e após sofrer danos de um ataque inimigo, como " +
            "Reação você pode usar uma ação de Reposicionar.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ESQUIVA_E_APARAR;
    }
}
