package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;

/**
 * The Habilidades de Competência available to characters trained in Conhecimentos.
 */
@Getter
@AllArgsConstructor
public enum ConhecimentosCompetencyAbility implements SkillCompetencyAbility {

    // Note: flavor-only permission to attempt Conhecimentos rolls about a specific topic
    // (physiology of creatures from other Planos) — this core never gates what topic a
    // Conhecimentos roll is about, so there's no numeric effect or missing system to
    // implement here; any character could already attempt such a roll.
    VIAJANTE_PLANAR("Você pode fazer rolagens para entender sobre a física, natureza e " +
            "fisiologia de seres de outros Planos."),

    // Note: same as VIAJANTE_PLANAR — flavor-only topic permission (Deuses/Elementais
    // lore), no numeric effect to implement.
    TEOGONIA("Você pode aplicar seus conhecimentos para descobrir sobre Deuses, " +
            "Elementais e suas linhagens, criações, poderes, heráldica."),

    // Note: a passive narrative trait (perfect recall), not a roll bonus — nothing for this
    // core to compute.
    MEMORIA_EIDETICA("Você nunca se esquece de algo que já tenha visto ou lido."),

    // TODO: grants 2 additional Especializações split across up to 2 Perícias of the
    // player's choice — CharacterSkill.specialization is a single String field, not a list,
    // so a character can't hold more than one specialization per trained skill yet, and
    // there's no mechanism for one skill's ability to grant a specialization onto a
    // *different* skill's CharacterSkill either.
    GENERALISTA("Você adquire 2 Especializações divididas entre até 2 Perícias à sua " +
            "escolha."),

    // Note: scoped to rolls within the character's own Especializações specifically, but
    // this codebase doesn't track what a roll is *for* (same simplification as
    // DirigirECavalgarCompetencyAbility.CONTROLAR_ANIMAIS), so it's implemented as an
    // unconditional flat bonus to every Conhecimentos roll rather than silently narrowed.
    O_PROFESSOR("Você recebe Vantagem em suas rolagens de Conhecimentos, mas apenas de " +
            "suas Especializações.") {
        @Modifier(ModifierType.SKILL_ROLL_BONUS)
        public int advantageBonus() {
            return Skill.ADVANTAGE_BONUS;
        }
    };

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.CONHECIMENTOS;
    }
}
