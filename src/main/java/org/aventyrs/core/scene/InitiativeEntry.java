package org.aventyrs.core.scene;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.CharacterSheet;

/**
 * A CharacterSheet paired with the Iniciativa value it rolled for a Scene — Iniciativa's Ego
 * total plus whatever dice roll the caller applied; this project doesn't compute that roll
 * itself, it's supplied already-resolved.
 */
@Getter
@AllArgsConstructor
public class InitiativeEntry {
    private final CharacterSheet characterSheet;
    private final int initiativeValue;
}
