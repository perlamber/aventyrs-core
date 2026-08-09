package org.aventyrs.core.scene;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.CharacterSheet;

import java.util.UUID;

/**
 * A CharacterSheet paired with the Iniciativa value it rolled for a Scene — Iniciativa's Ego
 * total plus modifiers (see {@link org.aventyrs.core.character.services.InitiativeService
 * #getTotalInitiative}) plus whatever dice roll the caller applied; this project doesn't
 * compute that roll itself, so the combined total is supplied already-resolved — plus the
 * sub-group it belongs to within the Scene, used to resolve who counts as an ally (see
 * {@link Scene#getAllies}).
 */
@Getter
@AllArgsConstructor
public class InitiativeEntry {
    private final CharacterSheet characterSheet;
    private final int initiativeValue;
    private final UUID group;
}
