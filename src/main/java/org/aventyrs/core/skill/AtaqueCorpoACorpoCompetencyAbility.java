package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Habilidades de Competência available to characters trained in Ataque Corpo-a-Corpo.
 * Every one of these needs a system this core doesn't have yet (attribute substitution, a
 * graduation cap tied to another attribute, weapon damage, critical margin, or
 * Malefício/status-effect tracking) so none are expressible for real today; see each
 * constant's TODO.
 */
@Getter
@AllArgsConstructor
public enum AtaqueCorpoACorpoCompetencyAbility implements SkillCompetencyAbility {

    // TODO: lets this Perícia use Destreza instead of its normal base Attribute (Força), but
    // only for attacks with weapons whose Categoria Natural is Leve or Média — no Perícia
    // base-Attribute substitution mechanism exists yet (same gap as
    // AtaqueADistanciaCompetencyAbility.ARREMESSO_PODEROSO / AtletismoCompetencyAbility
    // .ACROBATA / GnoseAbility.PERITO_TEORICO / AttentionCompetencyAbility
    // .ALMA_DE_SHERLOCK), and this codebase also doesn't track a weapon's category on a
    // specific roll.
    ACUIDADE("Você pode substituir o Atributo Base desta perícia por Destreza, mas apenas " +
            "para rolagens de ataques com armas cuja Categoria Natural seja Leve ou Média."),

    // TODO: +1 to the wielded melee weapon's Dano Base — no weapon-damage system exists yet
    // (same gap as AtaqueADistanciaExcellency.FOCADO).
    BRUTALIDADE("Dano Base das armas Corpo-a-Corpo que você utilizar aumentam em +1."),

    // TODO: caps this Perícia's graduation at the character's Foco attribute value instead
    // of whatever normally caps graduation, without changing its base Attribute (still
    // Força) — no graduation-cap-by-another-attribute mechanism exists yet. (Ataque à
    // Distância's DISPARO_ARCANO used to share this exact gap, but its rules text was
    // revised into a plain attribute substitution instead — this is now the only ability
    // with this specific graduation-cap shape.)
    SAGACIDADE_ARCANA("Seu limite de graduações nesta perícia passa a ser limitada pelo " +
            "atributo Foco, esta Habilidade não altera o Atributo Base da Perícia."),

    // TODO: +1 to this Perícia's Margem Crítica Menor — no Margem Crítica (critical-margin)
    // concept exists yet (same gap as DominioDoManaCompetencyAbility.LETALIDADE_ARCANA).
    ATAQUE_PRECISO("A margem crítica menor de seus Ataques Corpo-a-Corpo é aumentada em +1 " +
            "número."),

    // TODO: a critical hit inflicts the Malefício Desprevenido on the target for 1 Rodada —
    // no Malefício/status-effect system, critical-hit-trigger detection, or Rodada-scoped
    // duration tracking exists yet.
    ABRIR_DEFESAS("Após um acerto crítico seu alvo recebe o Malefício Desprevenido por 1 " +
            "Rodada.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }
}
