package org.aventyrs.core.character.services;

import java.util.Optional;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.AttackSource;

public class AttackRangeServiceImpl implements AttackRangeService {

    @Override
    public Range getEffectiveRange(final Character character, @NonNull final Weapon weapon) {
        return weapon.getEffectiveRange().increasedBy(sumFeatSteps(character, weapon));
    }

    @Override
    public Optional<Range> getEffectiveRange(final Character character, @NonNull final Spell spell) {
        Range baseReach = spell.getTargeting().range();
        return Optional.ofNullable(baseReach)
                .map(reach -> reach.increasedBy(sumFeatSteps(character, spell)));
    }

    /**
     * Every "+N níveis de distância" step character's held Talentos grant for an attack made with
     * attackSource — see {@link AttackRangeService}'s javadoc for why this is the only source
     * scanned today and why it is an explicit pass rather than a {@code ModifierResolver} one.
     */
    private int sumFeatSteps(final Character character, final AttackSource attackSource) {
        return character.getFeats().stream()
                .mapToInt(feat -> feat.resolveAttackRangeIncrease(character, attackSource))
                .sum();
    }
}
