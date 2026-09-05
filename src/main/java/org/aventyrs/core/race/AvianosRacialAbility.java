package org.aventyrs.core.race;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * Habilidades Raciais granted to every Aviano — see {@link Race#getRacialAbilities()} for why
 * these are modeled as ordinary {@link SkillCompetencyAbility} instances rather than a separate
 * type. Only one of the race's four Características Raciais fits the shape; the other three are
 * catalogued as gaps on {@link Aviano} itself.
 */
@Getter
@AllArgsConstructor
public enum AvianosRacialAbility implements SkillCompetencyAbility {

    // Granted the same flat-Vantagem way as ElfosRacialAbility.SENTIDOS_ABSOLUTOS — a @Modifier
    // method AbstractSkillInteraction picks up for every Character whose Race grants it.
    //
    // One deliberate over-grant, flagged rather than silently narrowed: the rules text scopes
    // this Vantagem to "percepções visuais", and this core doesn't track what a roll is *for*
    // (see CLAUDE.md's "Vantagem is a flat +2 bonus" section). Granting it on every Atenção roll
    // is the same simplification DirigirECavalgarCompetencyAbility#CONTROLAR_ANIMAIS already
    // makes for its own "rolagens envolvendo animais" scope, and is closer to the clause than
    // granting nothing. The Visão no Escuro half is a separate gap — no vision/senses concept
    // exists in this core.
    VISAO_ALEM_DO_ALCANCE("Vantagem em rolagens de Atenção para percepções visuais; também " +
            "possuem Visão no Escuro, enxergando de forma monocromática na ausência de luz.") {
        @Modifier(ModifierType.ATTENTION_ROLL_BONUS)
        public int advantageBonus() {
            return Skill.ADVANTAGE_BONUS;
        }
    };

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATTENTION;
    }
}
