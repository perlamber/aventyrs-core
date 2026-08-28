package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.SPELL_PREREQUISITE_NOT_MET;

public class SpellServiceImpl implements SpellService {

    @Override
    public BranchLevel getMaxBranchLevel(final Character character) {
        int steps = character.getFeats().stream()
                .mapToInt(feat -> feat.resolveBranchLevelIncrease(character))
                .sum();
        return BASE_BRANCH_LEVEL.advancedBy(steps);
    }

    @Override
    public Spell grantSpell(final Character character, final CharacterSheet characterSheet,
                            final Spell spell) throws IllegalOperationException {
        if (!spell.isEligible(character, getMaxBranchLevel(character))) {
            throw new IllegalOperationException(SPELL_PREREQUISITE_NOT_MET);
        }

        character.grantSpell(spell);
        return spell;
    }
}
