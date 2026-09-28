package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.magic.catalog.SpellCatalog;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.aventyrs.core.util.TranslatableMessages.SPELL_ALTERNATE_VERSION_NOT_GRANTABLE;
import static org.aventyrs.core.util.TranslatableMessages.SPELL_PREREQUISITE_NOT_MET;
import static org.aventyrs.core.util.TranslatableMessages.SPELL_TREE_CAPACITY_REACHED;

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
    public int getKnownTreeCapacity(final Character character) {
        return character.getFeats().stream()
                .mapToInt(feat -> feat.resolveKnownTreeCapacity(character))
                .sum();
    }

    @Override
    public int getOpenTreeSlots(final Character character) {
        return Math.max(0, getKnownTreeCapacity(character) - getKnownTrees(character).size());
    }

    @Override
    public List<SpellTree> getLearnableTrees(final Character character) {
        if (getOpenTreeSlots(character) == 0) {
            return List.of();
        }
        Set<SpellTree> known = getKnownTrees(character);
        return Arrays.stream(MagicTree.values())
                .filter(tree -> !known.contains(tree))
                .map(SpellTree.class::cast)
                .toList();
    }

    @Override
    public List<Spell> learnTree(final Character character, final CharacterSheet characterSheet,
                                 final SpellTree tree) throws IllegalOperationException {
        if (getKnownTrees(character).contains(tree)) {
            throw new IllegalOperationException(SPELL_PREREQUISITE_NOT_MET);
        }
        if (getOpenTreeSlots(character) == 0) {
            throw new IllegalOperationException(SPELL_TREE_CAPACITY_REACHED);
        }

        List<Spell> learned = new ArrayList<>();
        for (Spell spell : tree.getSpells()) {
            if (spell.getBranchLevel() == BranchLevel.SEMENTE && !spell.isAlternateVersion()) {
                learned.add(grantSpell(character, characterSheet, spell));
            }
        }
        return List.copyOf(learned);
    }

    @Override
    public Map<BranchLevel, Integer> getOwedFreeSpells(final Character character) {
        Map<BranchLevel, Integer> granted = new EnumMap<>(BranchLevel.class);
        character.getFeats().stream()
                .flatMap(feat -> feat.resolveFreeSpellPicks(character).stream())
                .forEach(pick -> granted.merge(pick.rung(), pick.picks(), Integer::sum));

        Map<BranchLevel, Integer> owed = new EnumMap<>(BranchLevel.class);
        granted.forEach((rung, picks) -> {
            int remaining = picks - treesHoldingRung(character, rung).size();
            if (remaining > 0) {
                owed.put(rung, remaining);
            }
        });
        return owed;
    }

    @Override
    public List<Spell> getFreeSpellOptions(final Character character, final BranchLevel rung) {
        if (!getOwedFreeSpells(character).containsKey(rung)) {
            return List.of();
        }
        Set<SpellTree> taken = treesHoldingRung(character, rung);
        BranchLevel cap = getMaxBranchLevel(character);
        return SpellCatalog.at(rung).stream()
                .filter(spell -> !spell.isAlternateVersion())
                .filter(spell -> !taken.contains(spell.getTree()))
                .filter(spell -> spell.isEligible(character, cap))
                .toList();
    }

    @Override
    public BigDecimal getAcquisitionCost(final Character character, final Spell spell) {
        boolean waived = character.getFeats().stream()
                .anyMatch(feat -> feat.grantsFreeSpellAcquisition(character, spell));
        if (waived || spendsFreePick(character, spell)) {
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
        // An Efeito Alternativo comes free with its parent — "um personagem que aprenda a versão
        // base automaticamente aprende sua segunda versão". Granting one would spend experience on
        // a Magia already known and, since it reports its parent's rung, would also satisfy the
        // climb gate for the next one.
        if (spell.isAlternateVersion()) {
            throw new IllegalOperationException(SPELL_ALTERNATE_VERSION_NOT_GRANTABLE);
        }
        if (!spell.isEligible(character, getMaxBranchLevel(character))) {
            throw new IllegalOperationException(SPELL_PREREQUISITE_NOT_MET);
        }
        // The climb gate already makes a Magia of an unknown Árvore its Semente; this is the
        // Árvore-count half of ARCANISTA's "conhece N Árvores".
        if (!getKnownTrees(character).contains(spell.getTree()) && getOpenTreeSlots(character) == 0) {
            throw new IllegalOperationException(SPELL_TREE_CAPACITY_REACHED);
        }

        characterSheet.useExperience(getAcquisitionCost(character, spell));

        character.grantSpell(spell);
        return spell;
    }

    /** A free pick is spent by the first Magia of its rung in each further Árvore. */
    private boolean spendsFreePick(final Character character, final Spell spell) {
        return getOwedFreeSpells(character).containsKey(spell.getBranchLevel())
                && !treesHoldingRung(character, spell.getBranchLevel()).contains(spell.getTree());
    }

    private static Set<SpellTree> treesHoldingRung(final Character character, final BranchLevel rung) {
        return character.getSpells().stream()
                .filter(held -> held.getBranchLevel() == rung)
                .map(Spell::getTree)
                .collect(Collectors.toUnmodifiableSet());
    }
}
