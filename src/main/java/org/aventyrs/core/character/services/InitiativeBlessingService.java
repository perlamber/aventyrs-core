package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.Blessing;

import java.util.List;

public interface InitiativeBlessingService {

    /**
     * Every {@link Blessing} character's own held traits grant the moment it wins
     * initiative for its group — concatenates {@code character.getEgoAdvantages().values()},
     * {@code character.getAttributeAbilities()}, and {@code
     * org.aventyrs.core.skill.SkillCompetencyAbility#allFor(character)} (acquired <b>and</b>
     * racial), flat-mapping each through its own {@code resolveInitiativeBlessings()} — the
     * exact three sources named "Advantages, Abilities and Competency." Deliberately does
     * <b>not</b> also scan unlocked {@code SkillExcellency} tiers the way {@link
     * ReactionsService}/{@link MovementService}'s flat {@code @Modifier} aggregation does —
     * this is a narrower, three-source scan by design, not an oversight.
     *
     * <p>A pure function over character's own data — this doesn't grant, revoke, or otherwise
     * mutate anything; {@code org.aventyrs.core.scene.Scene#applyInitiativeBlessings} is the
     * caller that actually applies what this resolves, as {@code TemporaryBonus}es on the
     * intended recipients' {@code CharacterSheet}s.
     */
    List<Blessing> resolveBlessings(Character character);
}
