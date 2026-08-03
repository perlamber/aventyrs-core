package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;

import java.util.Optional;

/**
 * The Habilidades de Competência available to characters trained in Ataque Corpo-a-Corpo.
 * Most of these need a system this core doesn't have yet (damage rolls, weapon damage,
 * graduation-threshold triggers, critical margin, or Malefício/status-effect tracking) so
 * they aren't expressible for real today; see each constant's TODO. ACUIDADE's unconditional
 * Attribute substitution is the exception — see {@link SkillCompetencyAbility
 * #getSubstituteAttributeDomain()}.
 */
@Getter
@AllArgsConstructor
public enum AtaqueCorpoACorpoCompetencyAbility implements SkillCompetencyAbility {

    // The Desvantagem-on-Damage-rolls-with-a-Categoria-Pesada-weapon half of this ability is
    // still TODO: this codebase has no damage-roll concept to apply Desvantagem to (same gap
    // as AtaqueADistanciaCompetencyAbility.FRIEZA) or a way to track a weapon's category on a
    // specific roll. The substitution half is real — see getSubstituteAttributeDomain() below.
    ACUIDADE("Você pode substituir o Atributo Base desta perícia por Destreza, se arma for " +
            "de Categoria Pesada você sofre Desvantagem nas rolagens de Danos.") {
        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.DEXTERITY);
        }
    },

    // TODO: starts as a +1 Damage-roll bonus (same missing damage-roll concept as FRIEZA),
    // then at 5 Graduações converts into a Dano Base increase instead (a qualitative shift,
    // not just a bigger number — no weapon-damage system exists yet either, same gap as
    // AtaqueADistanciaExcellency.FOCADO), then at 10 Graduações that Dano Base increase
    // becomes +2. Also needs a graduation-crossing-a-threshold trigger to switch mechanics
    // automatically (same gap as ArtesExcellency.FOCADO/LENDA's Fama trigger).
    BRUTALIDADE("Você recebe Bônus de +1 em rolagens de Danos de Ataques Corpo-a-Corpo, com " +
            "5 Graduações este Bônus é convertido em Dano Base, com 10 Graduações o " +
            "aumento no Dano Base muda para +2."),

    // TODO: lets this Perícia use Foco instead of its normal base Attribute (Força),
    // unconditionally — no Perícia base-Attribute substitution mechanism exists yet (same
    // gap as AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO, EmpatiaSelvagemCompetencyAbility
    // .ACADEMICO_SELVAGEM/INSTINTO_ANIMAL, FurtividadeCompetencyAbility.LADINO_TEORICO).
    SAGACIDADE_ARCANA("Você pode substituir o Atributo Base desta perícia por Foco."),

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
