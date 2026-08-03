package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Habilidades de Competência available to characters trained in Atletismo.
 */
@Getter
@AllArgsConstructor
public enum AtletismoCompetencyAbility implements SkillCompetencyAbility {

    // TODO: grants Movimento Base Vertical — no movement/terrain system exists yet.
    ALPINISTA_VELOZ("Você recebe Movimento Base Vertical."),

    // TODO: once per Cena, ignore Terreno Difícil, gaining an additional use at the 5th and
    // 10th Graduação — needs a Terreno Difícil/terrain system, a Cena-scoped usage-limiting
    // mechanism, and a graduation-crossing-a-threshold trigger for the extra uses (same gap
    // as ArtesExcellency.FOCADO/LENDA's Fama trigger), none of which exist yet.
    SALTO_PODEROSO("Uma vez por Cena você pode ignorar Terreno Difícil, novos usos desta " +
            "Habilidade são adquiridos ao alcançar a 5ª e 10ª Graduação."),

    // TODO: grants Movimento Base de Natação — no movement/terrain system exists yet.
    ANFIBIO("Você recebe Movimento Base de Natação."),

    // TODO: lets this Perícia use Destreza instead of its normal base Attribute (Força),
    // unconditionally — the substitution mechanism itself now exists (see
    // SkillCompetencyAbility.getSubstituteAttributeDomain() / AtaqueCorpoACorpoCompetencyAbility
    // .ACUIDADE), this constant just doesn't override it yet, and AtletismoInteraction
    // doesn't yet resolve/pass it into CharacterSkillService.getValueForRoll's
    // substituteAttributeDomain overload (same remaining wiring gap as
    // AttentionCompetencyAbility.ALMA_DE_SHERLOCK). GnoseAbility.PERITO_TEORICO is a
    // different shape entirely (an acquisition-time choice of *which* Perícia, not a fixed
    // one) and needs its own AcquiredChoice-driven mechanism.
    ACROBATA("Você pode substituir o Atributo Base desta perícia por Destreza."),

    // TODO: +2UD to Movimento Base — no movement/distance system exists yet (same gap as
    // AtaqueCorpoACorpoExcellency.FOCADO, which grants the identical bonus).
    PASSO_LARGO("Movimento Base aumenta em +2UD.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATLETISMO;
    }
}
