package org.aventyrs.core.scene;

import lombok.NonNull;
import org.aventyrs.core.modifier.ModifierType;

/** One open-ended figure a {@link MonsterAura} grants whoever stands inside it — Aura do Paradoxo's "-2PA". */
public record AuraBonus(@NonNull ModifierType type, int value) {
}
