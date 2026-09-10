package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatCatalog;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.math.BigDecimal;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.FEAT_PREREQUISITE_NOT_MET;
import static org.aventyrs.core.util.TranslatableMessages.FEAT_REQUIRES_ACTIVE_ABILITY_CHOICE;

public class FeatServiceImpl implements FeatService {

    @Override
    public Feat grantFeat(final Character character, final CharacterSheet characterSheet, final Feat feat) throws IllegalOperationException {
        if (!feat.isEligible(character, characterSheet)) {
            throw new IllegalOperationException(FEAT_PREREQUISITE_NOT_MET);
        }
        // A Talento whose rules make the player choose between ActiveAbilities cannot be granted
        // as the bare catalog constant — that would hand over a Metamorfose with no Formas. The
        // acquired, choice-carrying form reports the constant through catalogEntry(), so "is this
        // still the plain constant" is exactly feat == feat.catalogEntry().
        if (feat == feat.catalogEntry() && feat.resolveActiveAbilityChoice(character) != null) {
            throw new IllegalOperationException(FEAT_REQUIRES_ACTIVE_ABILITY_CHOICE);
        }

        int cost = character.getRace().getNewFeatCost(feat.getFeatCategory());
        characterSheet.useExperience(BigDecimal.valueOf(cost));

        character.grantFeat(feat);
        feat.getGrantedMimetizedSpells(character).forEach(character::grantMimetizedSpell);
        return feat;
    }

    @Override
    public List<Feat> getAvailableFeats(final Character character) {
        return FeatCatalog.availableFor(character);
    }

    @Override
    public List<Feat> getAvailableFeats(final Character character, final CharacterSheet characterSheet) {
        return FeatCatalog.availableFor(character, characterSheet);
    }

    @Override
    public List<Feat> getAffordableFeats(final Character character, final CharacterSheet characterSheet) {
        return getAvailableFeats(character, characterSheet).stream()
                .filter(feat -> canAfford(character, characterSheet, feat))
                .toList();
    }

    private boolean canAfford(final Character character, final CharacterSheet characterSheet, final Feat feat) {
        BigDecimal cost = BigDecimal.valueOf(character.getRace().getNewFeatCost(feat.getFeatCategory()));
        return characterSheet.getUnUsedExperience().compareTo(cost) >= 0;
    }
}
