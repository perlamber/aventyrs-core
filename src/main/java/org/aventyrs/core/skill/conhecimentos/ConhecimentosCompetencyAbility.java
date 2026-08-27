package org.aventyrs.core.skill.conhecimentos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.List;

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

    // Mirrors GnoseAbility#DOMINIO_DO_CONHECIMENTO's own resolvePendingSpecializationChoices/
    // resolvePendingSkillTraitChoices shape — every currently trained Perícia is a candidate,
    // no domain filter ("à sua escolha," the player's own pick). Each entry is resolved via
    // org.aventyrs.core.character.services.AttributeAbilityService#grantSpecializationChoice,
    // up to 2 times total per this ability's own "2 Especializações... entre até 2 Perícias"
    // text (both on the same Perícia, or split across two) — that cap isn't enforced here,
    // same unenforced-prerequisite restraint already applied elsewhere. Unlike
    // DOMINIO_DO_CONHECIMENTO, there's still no acquisition-time service to report this
    // constant's own pending entries through automatically (no SkillCompetencyAbility
    // equivalent of AttributeAbilityService#grantAttributeAbility exists) — see this method's
    // own javadoc on SkillCompetencyAbility.
    GENERALISTA("Você adquire 2 Especializações divididas entre até 2 Perícias à sua " +
            "escolha.") {
        @Override
        public List<SkillType> resolvePendingSpecializationChoices(final Character character) {
            return character.getSkills().values().stream()
                    .map(CharacterSkill::getSkill)
                    .map(Skill::getSkillType)
                    .toList();
        }
    },

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
