package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ResourceType;

public class DeterminationPointsServiceImpl implements DeterminationPointsService {

    private final ModifierResolver modifierResolver;

    /** Only consulted for a {@link org.aventyrs.core.character.ResourceFormula#MONSTER} creature. */
    private final HitPointsService hitPointsService;

    public DeterminationPointsServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public DeterminationPointsServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
        this.hitPointsService = new HitPointsServiceImpl(modifierResolver);
    }

    @Override
    public int getDeterminationMultiplier(final Character character) {
        return getDeterminationMultiplier(character, null);
    }

    @Override
    public int getMaxDeterminationPoints(final Character character) {
        return getMaxDeterminationPoints(character, null);
    }

    @Override
    public int getDeterminationMultiplier(final Character character, final CombatantSheet sheet) {
        int bonus = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.DETERMINATION_MULTIPLIER);
        // Talentos are outside every ModifierResolver scan, so they get an explicit pass.
        for (Feat feat : character.getFeats()) {
            bonus += feat.resolveDeterminationMultiplierIncrease(character, sheet);
        }
        return ResourcePoolFormula.multiplier(ResourceType.DETERMINATION_POINTS, character, sheet, bonus);
    }

    /** {@code base + Instinto × Multiplicador}, both read through the sheet when one is given — see {@link ResourcePoolFormula}. */
    @Override
    public int getMaxDeterminationPoints(final Character character, final CombatantSheet sheet) {
        return basePoints(character, sheet)
                + ResourcePoolFormula.attribute(ResourceType.DETERMINATION_POINTS, character, sheet)
                        * getDeterminationMultiplier(character, sheet);
    }

    @Override
    public int getCurrentDeterminationPoints(final Character character, final CombatantSheet characterSheet) {
        return Math.max(0, getMaxDeterminationPoints(character, characterSheet) - characterSheet.getDeterminationSpent());
    }

    /**
     * The flat part of the pool — {@value #BASE_DETERMINATION_POINTS} for a character, "Metade dos
     * PV" for a monster (see {@link org.aventyrs.core.character.ResourceFormula}).
     */
    private int basePoints(final Character character, final CombatantSheet sheet) {
        if (character.getResourceFormula().derivesLesserPoolsFromHitPoints()) {
            return hitPointsService.getMaxHitPoints(character, sheet) / 2;
        }
        return BASE_DETERMINATION_POINTS;
    }
}
