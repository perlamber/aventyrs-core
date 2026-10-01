package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DevotionTier;
import org.aventyrs.core.feat.DevotoFeat;
import org.aventyrs.core.feat.FeatChoice;

import java.util.List;

/**
 * A character's devotion — the tier ({@link DevotionTier}) and the picks each Talento de Devoção's rungs ask for (core
 * 0.0.86). Table rulings (2026-10-01): the player picks the tier at creation, the Narrador raises or lowers it on
 * role-play alone, and a rung's pick is made the first time the tier reaches it and kept while it is lowered.
 */
public interface DevotionService {

    /**
     * Sets character's tier — the player at creation, the Narrador afterwards. Validates nothing and costs nothing: a
     * tier is not earned. {@code null} is no devotion. Picks already made are kept whatever the tier.
     */
    void setTier(Character character, DevotionTier tier);

    /** One rung still waiting for its pick: the Talento, the rung, and what to choose, in order. */
    record OwedPicks(DevotoFeat talento, DevotionTier rung, List<FeatChoice<?>> choices) {
    }

    /**
     * Every rung of a held Talento de Devoção that character's tier reaches and that asks for a pick not yet made —
     * what a client prompts for at creation and whenever the Narrador raises the tier.
     */
    List<OwedPicks> owedPicks(Character character);

    /**
     * Records the picks for talento's rung — one value per pick, in the order {@code
     * DevotoFeat#resolveRungChoices} declares the choices. Replaces any earlier picks for that rung.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code DEVOTION_PICK_NOT_OWED} when talento isn't
     *         held or the tier doesn't reach rung; {@code INVALID_DEVOTION_PICK} when a value isn't among its
     *         choice's options or the count is wrong
     */
    void recordPicks(Character character, DevotoFeat talento, DevotionTier rung, List<Object> picks);
}
