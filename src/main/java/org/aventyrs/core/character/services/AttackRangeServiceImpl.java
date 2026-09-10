package org.aventyrs.core.character.services;

import java.util.Optional;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
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
        int reach = getEffectiveRangeInUnidadesDeDistancia(character, weapon);
        Range band = reach == UNBOUNDED_RANGE
                ? Range.AO_ALCANCE_DOS_OLHOS
                : Range.fromUnidadesDeDistancia(reach);
        return band.increasedBy(sumFeatSteps(character, weapon));
    }

    /**
     * The authored Alcance widened by the attacker's Categoria de Tamanho — see the interface for
     * why {@link Range#ADJACENTE} substitutes the size's own reach while every other band has the
     * modifier added to it.
     *
     * <p>The branch is on what the weapon <em>states</em>, not on which Perícia swings it, because
     * the question being asked is "does this weapon have a reach of its own?" and only its Alcance
     * answers that. One consequence worth knowing: a destroyed weapon reports {@link
     * Range#ADJACENTE} through {@code getEffectiveRange()}, so a shattered bow takes the melee
     * substitution — correct, since swinging the wreck is what is left to do with it.
     */
    @Override
    public int getEffectiveRangeInUnidadesDeDistancia(final Character character, @NonNull final Weapon weapon) {
        SizeCategory size = characterSizeService.getEffectiveSizeCategory(character);
        Range authored = weapon.getEffectiveRange();
        if (authored == Range.ADJACENTE) {
            return size.getRange();
        }
        Integer authoredUnidades = authored.getMaxUnidadesDeDistancia();
        if (authoredUnidades == null) {
            return UNBOUNDED_RANGE;
        }
        return Math.max(SizeCategory.MINIMUM_MELEE_RANGE, authoredUnidades + size.getRangeModifier());
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
