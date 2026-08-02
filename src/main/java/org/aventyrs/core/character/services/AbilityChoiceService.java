package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;

import java.util.Optional;

/**
 * Looks up the value a character chose when they acquired an ability whose rules require
 * picking one — see {@code org.aventyrs.core.ability.AcquiredChoice}.
 */
public interface AbilityChoiceService {
    /**
     * The value chosen for the given ability instance, if the character has one recorded —
     * e.g. which {@code SkillType} they picked for
     * {@code ArtesCompetencyAbility.APRIMORAR_COM_ARTE}. {@code ability} is matched by
     * identity/equality against {@code AcquiredChoice.getAbility()}, so pass the same enum
     * constant the character was granted (enum constants are singletons, so this is exact
     * reference equality in practice).
     */
    <C> Optional<C> getChoiceFor(Character character, Object ability);
}
