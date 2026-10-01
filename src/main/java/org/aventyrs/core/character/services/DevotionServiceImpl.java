package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DevotionTier;
import org.aventyrs.core.feat.DevotionPick;
import org.aventyrs.core.feat.DevotoFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatChoice;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.ArrayList;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.DEVOTION_PICK_NOT_OWED;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_DEVOTION_PICK;

public class DevotionServiceImpl implements DevotionService {

    @Override
    public void setTier(@NonNull final Character character, final DevotionTier tier) {
        character.setDevotionTier(tier);
    }

    @Override
    public List<OwedPicks> owedPicks(@NonNull final Character character) {
        List<OwedPicks> owed = new ArrayList<>();
        for (DevotoFeat talento : heldDevotoFeats(character)) {
            for (DevotionTier rung : DevotionTier.values()) {
                List<FeatChoice<?>> choices = talento.resolveRungChoices(rung, character);
                if (!choices.isEmpty() && character.isDevotedAtLeast(rung)
                        && character.getDevotionPicks(talento, rung).isEmpty()) {
                    owed.add(new OwedPicks(talento, rung, choices));
                }
            }
        }
        return owed;
    }

    @Override
    public void recordPicks(@NonNull final Character character, @NonNull final DevotoFeat talento,
                            @NonNull final DevotionTier rung, @NonNull final List<Object> picks) {
        if (!heldDevotoFeats(character).contains(talento) || !character.isDevotedAtLeast(rung)) {
            throw new IllegalOperationException(DEVOTION_PICK_NOT_OWED);
        }
        List<FeatChoice<?>> choices = talento.resolveRungChoices(rung, character);
        int expected = choices.stream().mapToInt(FeatChoice::picks).sum();
        if (choices.isEmpty() || picks.size() != expected) {
            throw new IllegalOperationException(INVALID_DEVOTION_PICK);
        }
        int index = 0;
        List<DevotionPick> recorded = new ArrayList<>();
        for (FeatChoice<?> choice : choices) {
            List<Object> taken = new ArrayList<>();
            for (int i = 0; i < choice.picks(); i++) {
                Object value = picks.get(index++);
                if (!choice.options().contains(value) || taken.contains(value)) {
                    throw new IllegalOperationException(INVALID_DEVOTION_PICK);
                }
                taken.add(value);
                recorded.add(new DevotionPick(talento, rung, value));
            }
        }
        character.replaceDevotionPicks(talento, rung, recorded);
    }

    private static List<DevotoFeat> heldDevotoFeats(final Character character) {
        return character.getFeats().stream()
                .map(Feat::catalogEntry)
                .filter(DevotoFeat.class::isInstance)
                .map(DevotoFeat.class::cast)
                .distinct()
                .toList();
    }
}
