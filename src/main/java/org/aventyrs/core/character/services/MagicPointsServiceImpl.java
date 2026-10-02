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
        return getManaMultiplier(character, null);
    }

    @Override
    public int getMaxMagicPoints(final Character character) {
        return getMaxMagicPoints(character, null);
    }

    @Override
    public int getManaMultiplier(final Character character, final CombatantSheet sheet) {
        int bonus = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.MANA_MULTIPLIER)
                + character.getFeats().stream()
                        .mapToInt(feat -> feat.resolveManaMultiplierIncrease(character, sheet))
                        .sum();
        return ResourcePoolFormula.multiplier(ResourceType.MAGIC_POINTS, character, sheet, bonus);
    }

    /** {@code base + Foco × Multiplicador}, both read through the sheet when one is given — see {@link ResourcePoolFormula}. */
    @Override
    public int getMaxMagicPoints(final Character character, final CombatantSheet sheet) {
        return basePoints(character, sheet)
                + ResourcePoolFormula.attribute(ResourceType.MAGIC_POINTS, character, sheet)
                        * getManaMultiplier(character, sheet);
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
