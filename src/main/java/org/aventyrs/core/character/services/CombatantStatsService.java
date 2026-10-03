package org.aventyrs.core.character.services;

import java.util.EnumMap;
import java.util.Map;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Reads a {@link CombatantStats} off a live sheet (core 0.1.0) — every figure through the same sheet-aware services
 * the rules use, so the snapshot is exactly what the next PV/PM/PD/size read would answer.
 */
public class CombatantStatsService {

    private final HitPointsService hitPointsService;
    private final MagicPointsService magicPointsService;
    private final DeterminationPointsService determinationPointsService;
    private final CharacterSizeService characterSizeService;

    public CombatantStatsService() {
        this(new HitPointsServiceImpl(), new MagicPointsServiceImpl(), new DeterminationPointsServiceImpl(),
                new CharacterSizeServiceImpl());
    }

    public CombatantStatsService(final HitPointsService hitPointsService, final MagicPointsService magicPointsService,
                                 final DeterminationPointsService determinationPointsService,
                                 final CharacterSizeService characterSizeService) {
        this.hitPointsService = hitPointsService;
        this.magicPointsService = magicPointsService;
        this.determinationPointsService = determinationPointsService;
        this.characterSizeService = characterSizeService;
    }

    /** What sheet's derived stats are right now. */
    public CombatantStats snapshot(final CombatantSheet sheet) {
        Character character = sheet.getCharacter();
        Map<AttributeDomain, Integer> attributes = new EnumMap<>(AttributeDomain.class);
        for (AttributeDomain domain : AttributeDomain.values()) {
            attributes.put(domain, sheet.getAttributeTotal(domain));
        }
        return new CombatantStats(
                hitPointsService.getMaxHitPoints(character, sheet),
                hitPointsService.getCurrentHitPoints(character, sheet),
                hitPointsService.getLifeMultiplier(character, sheet),
                magicPointsService.getMaxMagicPoints(character, sheet),
                magicPointsService.getCurrentMagicPoints(character, sheet),
                magicPointsService.getManaMultiplier(character, sheet),
                determinationPointsService.getMaxDeterminationPoints(character, sheet),
                determinationPointsService.getCurrentDeterminationPoints(character, sheet),
                determinationPointsService.getDeterminationMultiplier(character, sheet),
                hitPointsService.getStatus(sheet),
                characterSizeService.getEffectiveSizeCategory(sheet),
                attributes);
    }
}
