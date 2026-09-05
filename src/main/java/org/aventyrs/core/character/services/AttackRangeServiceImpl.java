package org.aventyrs.core.character.services;

import java.util.Optional;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.AttackSource;

public class AttackRangeServiceImpl implements AttackRangeService {

    private final CharacterSizeService characterSizeService;

    public AttackRangeServiceImpl() {
        this(new CharacterSizeServiceImpl());
    }

    public AttackRangeServiceImpl(final CharacterSizeService characterSizeService) {
        this.characterSizeService = characterSizeService;
    }

    @Override
    public Range getEffectiveRange(final Character character, @NonNull final Weapon weapon) {
        return sizeAdjustedMeleeBase(character, weapon.getEffectiveRange())
                .increasedBy(sumFeatSteps(character, weapon));
    }

    /**
     * A corpo-a-corpo weapon's {@link Range#ADJACENTE} base widened by the attacking {@link
     * Character}'s own {@link org.aventyrs.core.character.SizeCategory#getRange()} — a maior
     * creature's reach, in UD, converted back to a band via {@link
     * Range#fromUnidadesDeDistancia}. A weapon whose Alcance is already something other than
     * ADJACENTE (Ataque à Distância, Arremesso) is untouched: Size only widens the reach of an
     * attack that starts adjacent, not a ranged weapon's own authored band.
     */
    private Range sizeAdjustedMeleeBase(final Character character, final Range base) {
        if (base != Range.ADJACENTE) {
            return base;
        }
        int meleeReach = characterSizeService.getEffectiveSizeCategory(character).getRange();
        return Range.fromUnidadesDeDistancia(meleeReach);
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
