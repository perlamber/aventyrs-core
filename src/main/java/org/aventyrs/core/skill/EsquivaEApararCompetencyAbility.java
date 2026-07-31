package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;

/**
 * The Habilidades de Competência available to characters trained in Esquiva e Aparar.
 */
@Getter
@AllArgsConstructor
public enum EsquivaEApararCompetencyAbility implements SkillCompetencyAbility {

    // TODO: lets the character keep adding Destreza in full while wearing Média armor (no
    // penalty), and half (instead of none) while wearing Pesada armor — this modifies the
    // armor-category-to-attribute-penalty rule described in EsquivaEApararInteraction's own
    // TODO, so it's blocked on the same missing piece: no Equipamento/Armadura tracking
    // exists on Character yet to know what's currently equipped or its Categoria.
    ENCOURACADO_E_VELOZ("Você pode adicionar seu valor integral de Destreza em suas " +
            "rolagens mesmo enquanto utilizando Equipamentos de Categoria Natural Média, e " +
            "metade quando equipados com Equipamentos Naturalmente Pesados."),

    // TODO: +3 to this Perícia's own roll bonus, scoped to resisting Área de Efeito attacks
    // specifically, then +5 once 7 Graduações are reached (a graduation-tiered scaling
    // bonus, not a flat one — same shape as DominioDoManaCompetencyAbility
    // .LETALIDADE_ARCANA) — the @Modifier mechanism only supports a fixed value per
    // constant, it can't read the holder's own graduation to pick +3 vs +5.
    EVASAO("Defesas +3 para resistir à ataques e efeitos com Área de Efeito, benefício muda " +
            "para +5 ao alcançar 7 Graduações."),

    // Note: scoped to rolls made specifically in response to a Reação-triggering effect, but
    // this codebase doesn't track what a roll is *for* (same simplification as
    // DirigirECavalgarCompetencyAbility.CONTROLAR_ANIMAIS), so it's implemented as an
    // unconditional flat bonus to every Esquiva e Aparar roll rather than silently narrowed.
    MOVIMENTO_DEFENSIVO("Contra efeitos de Reações você recebe bônus de +3 em suas " +
            "Defesas.") {
        @Modifier(ModifierType.SKILL_ROLL_BONUS)
        public int defesasBonus() {
            return 3;
        }
    },

    // TODO: an activated ability rolling Esquiva e Aparar instead of an Ataque Perícia
    // against GD Difícil; on success, reduces the GD to attack that specific target by -1
    // Nível for 2 Rodadas — needs a roll-resolution-vs-DifficultyLevel engine (to know
    // "success" against a fixed GD, same gap as ArtesCompetencyAbility.DOM_BARDICO), plus a
    // Rodada-scoped duration system and a way to apply a GD reduction to a specific
    // opponent's future attack rolls (a cross-character effect, not a self-buff) — none of
    // which exist yet.
    ESTUDAR_DEFESAS("Você pode efetuar rolagens de Esquiva e Aparar ao invés da Perícia de " +
            "Ataque contra GD Difícil para analisar o oponente, seus padrões de " +
            "movimentos, brechas na armadura etc. Se for bem-sucedido você reduz em o GD " +
            "para efetuar ataques contra o alvo em -1 Nível por 2 Rodadas."),

    // TODO: Requer 7 Graduações – after taking damage from an attack, spend a Reação to move
    // to an adjacent free space — needs a damage-triggers-a-Reação-opportunity mechanic and
    // a movement/positioning system, neither of which exist yet. Requires 7 Graduações to
    // acquire — also unenforced, same as AtletismoCompetencyAbility.ATLETA_VERSATIL, since no
    // eligibility-validation service exists for SkillCompetencyAbility.
    RECUO_RAPIDO("Requer 7 Graduações - Após sofrer danos de um ataque, como uma Reação, " +
            "você pode se mover para um espaço livre adjacente.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ESQUIVA_E_APARAR;
    }
}
