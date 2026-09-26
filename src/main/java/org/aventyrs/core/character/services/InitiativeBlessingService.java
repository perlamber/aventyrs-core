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
     * racial), and {@code character.getFeats()} ({@code MobilidadeFeat#LIDERAR_O_AVANCO}),
     * flat-mapping each through its own {@code resolveInitiativeBlessings()} — the three sources
     * named "Advantages, Abilities and Competency", plus Talentos. Deliberately does
     * <b>not</b> also scan unlocked {@code SkillExcellency} tiers the way {@link
     * ReactionsService}/{@link MovementService}'s flat {@code @Modifier} aggregation does —
     * this is a narrower scan by design, not an oversight.
     *
     * <p>A pure function over character's own data — this doesn't grant, revoke, or otherwise
     * mutate anything; {@code org.aventyrs.core.scene.Scene#applyInitiativeBlessings} is the
     * caller that actually applies what this resolves, as {@code TemporaryBonus}es on the
     * intended recipients' {@code CombatantSheet}s.
     */
    List<Blessing> resolveBlessings(Character character);
}
