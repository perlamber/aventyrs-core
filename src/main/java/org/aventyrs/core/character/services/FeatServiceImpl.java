package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.math.BigDecimal;

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
}
