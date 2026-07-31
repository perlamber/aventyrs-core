package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Habilidades de Competência available to characters trained in Ataque à Distância. Every
 * one of these needs a system this core doesn't have yet (attribute substitution, a
 * graduation cap tied to another attribute, damage/critical-damage rolls, range/targeting, or
 * dice rolling this core deliberately never does — see the {@code skill} package-info's
 * "What this library computes" section) so none are expressible for real today; see each
 * constant's TODO.
 */
@Getter
@AllArgsConstructor
public enum AtaqueADistanciaCompetencyAbility implements SkillCompetencyAbility {

    // TODO: lets this Perícia use Força instead of its normal base Attribute (Destreza), but
    // only for attacks made with arremesso (throwing) weapons — no Perícia base-Attribute
    // substitution mechanism exists yet (same gap as AtletismoCompetencyAbility.ACROBATA /
    // GnoseAbility.PERITO_TEORICO / AttentionCompetencyAbility.ALMA_DE_SHERLOCK), and this
    // codebase also doesn't track what weapon/method a specific roll is being made with.
    ARREMESSO_PODEROSO("Você pode substituir o Atributo Base desta perícia por Força, mas " +
            "apenas para rolagens de ataques com armas de arremessos."),

    // TODO: caps this Perícia's graduation at the character's Foco attribute value instead
    // of whatever normally caps graduation, without changing its base Attribute (still
    // Destreza) — no graduation-cap-by-another-attribute mechanism exists yet.
    DISPARO_ARCANO("Seu limite de graduações nesta perícia passa a ser limitada pelo " +
            "atributo Foco, esta Habilidade não altera o Atributo Base da Perícia."),

    // TODO: Vantagem on damage rolls (not the Perícia roll itself) against targets at
    // Distância Curta or closer — no damage-roll concept or target-range/distance system
    // exists yet.
    FRIEZA("Vantagem nas rolagens de dano de Ataques à Distância realizados contra alvos " +
            "em Distância Curta ou inferior."),

    // TODO: a successful hit lets the character pick a new target at Distância Muito Curta
    // from the original and roll this Perícia again, dealing 1d6 damage on success — no
    // "Corrente de Efeitos" (chain-effect) or targeting/range system exists yet, and this
    // core deliberately never rolls dice (1d6) — see the skill package-info.
    DISPARO_RICOCHETE("Seus Ataques à Distância recebem a Corrente de Efeitos – Ricochete: " +
            "Você pode escolher um novo alvo em Distância Muito Curta de seu alvo inicial e " +
            "efetuar uma nova rolagem nesta Perícia, se for bem-sucedido o alvo adicional " +
            "sofre 1d6 pontos de dano."),

    // TODO: Vantagem on Critical Damage rolls — no critical-damage-roll concept exists yet.
    MIRAR_NA_CABECA("Vantagem nas rolagens de Danos Críticos.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_A_DISTANCIA;
    }
}
