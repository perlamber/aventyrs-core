package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

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
    public Set<SpellTree> getKnownTrees(final Character character) {
        return character.getSpells().stream()
                .map(Spell::getTree)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public BigDecimal getAcquisitionCost(final Character character, final Spell spell) {
        boolean waived = character.getFeats().stream()
                .anyMatch(feat -> feat.grantsFreeSpellAcquisition(character, spell));
        if (waived) {
            return BigDecimal.ZERO;
        }

        BigDecimal base = ACQUISITION_EXPERIENCE_COST.getOrDefault(spell.getBranchLevel(), BigDecimal.ZERO);
        BigDecimal reduction = character.getFeats().stream()
                .map(feat -> feat.resolveSpellAcquisitionCostReduction(character, spell))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(character.getRace().resolveSpellAcquisitionCostReduction(character, spell));

        return base.subtract(reduction).max(BigDecimal.ZERO);
    }

    @Override
    public Spell grantSpell(final Character character, final CharacterSheet characterSheet,
                            final Spell spell) throws IllegalOperationException {
        if (!spell.isEligible(character, getMaxBranchLevel(character))) {
            throw new IllegalOperationException(SPELL_PREREQUISITE_NOT_MET);
        }

        characterSheet.useExperience(getAcquisitionCost(character, spell));

        character.grantSpell(spell);
        return spell;
    }
}
