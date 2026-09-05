package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatCatalog;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.math.BigDecimal;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.FEAT_PREREQUISITE_NOT_MET;

public class FeatServiceImpl implements FeatService {

    @Override
    public Feat grantFeat(final Character character, final CharacterSheet characterSheet, final Feat feat) throws IllegalOperationException {
        if (!feat.isEligible(character)) {
            throw new IllegalOperationException(FEAT_PREREQUISITE_NOT_MET);
        }

        int cost = character.getRace().getNewFeatCost(feat.getFeatCategory());
        characterSheet.useExperience(BigDecimal.valueOf(cost));

        character.grantFeat(feat);
        return feat;
    }

    @Override
    public List<Feat> getAvailableFeats(final Character character) {
        return FeatCatalog.availableFor(character);
    }

    @Override
    public List<Feat> getAffordableFeats(final Character character, final CharacterSheet characterSheet) {
        return getAvailableFeats(character).stream()
                .filter(feat -> canAfford(character, characterSheet, feat))
                .toList();
    }

    private boolean canAfford(final Character character, final CharacterSheet characterSheet, final Feat feat) {
        BigDecimal cost = BigDecimal.valueOf(character.getRace().getNewFeatCost(feat.getFeatCategory()));
        return characterSheet.getUnUsedExperience().compareTo(cost) >= 0;
    }
}
