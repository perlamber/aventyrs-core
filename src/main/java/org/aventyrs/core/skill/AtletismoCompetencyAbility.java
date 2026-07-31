package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Habilidades de Competência available to characters trained in Atletismo.
 */
@Getter
@AllArgsConstructor
public enum AtletismoCompetencyAbility implements SkillCompetencyAbility {

    // TODO: grants Movimento Base Vertical (except on perfectly smooth surfaces) — no
    // movement/terrain system exists yet.
    ALPINISTA_VELOZ("Você recebe Movimento Base Vertical, exceto em superfícies " +
            "perfeitamente lisas."),

    // TODO: adds the character's Força value as bonus UD to jump distance — no
    // movement/distance system exists yet.
    SALTO_PODEROSO("Você pode saltar grandes distâncias, acrescentando seu valor de Força " +
            "como UD adicional à distância percorrida."),

    // TODO: grants Movimento Base de Natação — no movement/terrain system exists yet.
    ANFIBIO("Você recebe Movimento Base de Natação."),

    // TODO: lets this Perícia use Destreza instead of its normal base Attribute (Força),
    // except in Armadura/Escudo Pesado or over carrying capacity — no Perícia
    // base-Attribute substitution mechanism or armor/carrying-capacity system exists yet
    // (same substitution gap as GnoseAbility.PERITO_TEORICO / AttentionCompetencyAbility.ALMA_DE_SHERLOCK).
    ACROBATA("Você pode substituir o Atributo Base desta perícia por Destreza. Não é " +
            "possível realizar rolagens desta forma utilizando Armaduras e Escudos " +
            "Pesados, ou quando o personagem estiver acima de sua capacidade de carga."),

    // Note: requires 7 graduations to acquire — this codebase doesn't validate
    // SkillCompetencyAbility acquisition prerequisites yet (unlike AttributeAbilityService
    // for AttributeAbility), so nothing currently enforces this; treat it as a rules note
    // until such a service exists. The -1 GD effect itself is real, though — see
    // getDifficultyReduction().
    ATLETA_VERSATIL("Requer 7 Graduações - O GD de suas rolagens com esta Perícia é " +
            "reduzido em 1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    };

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATLETISMO;
    }
}
