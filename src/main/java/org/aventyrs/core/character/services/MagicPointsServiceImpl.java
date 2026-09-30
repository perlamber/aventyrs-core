package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ResourceType;

public class MagicPointsServiceImpl implements MagicPointsService {

    private final ModifierResolver modifierResolver;

    /** Only consulted for a {@link org.aventyrs.core.character.ResourceFormula#MONSTER} creature. */
    private final HitPointsService hitPointsService;

    public MagicPointsServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public MagicPointsServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
        this.hitPointsService = new HitPointsServiceImpl(modifierResolver);
    }

    @Override
    public int getManaMultiplier(final Character character) {
        int bonus = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.MANA_MULTIPLIER);
        int featBonus = character.getFeats().stream()
                .mapToInt(feat -> feat.resolveManaMultiplierIncrease(character))
                .sum();
        return Feat.fixedMultiplier(ResourceType.MAGIC_POINTS, character, character.getManaMultiplier() + bonus + featBonus);
    }

    @Override
    public int getMaxMagicPoints(final Character character) {
        return basePoints(character, null)
                + character.getEffectiveAttributeTotal(AttributeDomain.FOCUS) * getManaMultiplier(character);
    }

    @Override
    public int getManaMultiplier(final Character character, final CombatantSheet sheet) {
        if (sheet == null) {
            return getManaMultiplier(character);
        }
        return Feat.fixedMultiplier(ResourceType.MAGIC_POINTS, character,
                Math.max(1, getManaMultiplier(character) + sheet.getTemporaryBonus(ModifierType.MANA_MULTIPLIER)));
    }

    @Override
    public int getMaxMagicPoints(final Character character, final CombatantSheet sheet) {
        return basePoints(character, sheet)
                + character.getEffectiveAttributeTotal(AttributeDomain.FOCUS) * getManaMultiplier(character, sheet);
    }

    @Override
    public int getCurrentMagicPoints(final Character character, final CombatantSheet characterSheet) {
        return Math.max(0, getMaxMagicPoints(character, characterSheet) - characterSheet.getManaSpent());
    }

    /**
     * The flat part of the pool — {@value #BASE_MAGIC_POINTS} for a character, "Metade dos PV"
     * for a monster (see {@link org.aventyrs.core.character.ResourceFormula}).
     */
    private int basePoints(final Character character, final CombatantSheet sheet) {
        if (character.getResourceFormula().derivesLesserPoolsFromHitPoints()) {
            return hitPointsService.getMaxHitPoints(character, sheet) / 2;
        }
        return BASE_MAGIC_POINTS;
    }
}
