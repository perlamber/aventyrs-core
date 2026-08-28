package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.AbstractFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.feat.FeatRequirements;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.magic.TestSpellBranch;
import org.aventyrs.core.magic.TestSpellTree;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellServiceImplTest {

    private final SpellService spellService = new SpellServiceImpl();

    private static final Spell TRUNK_SEMENTE =
            TestSpell.at(TestSpellTree.DIVERGING, BranchLevel.SEMENTE, null);
    private static final Spell TRUNK_BROTO =
            TestSpell.at(TestSpellTree.DIVERGING, BranchLevel.BROTO, null);
    private static final Spell MUDA_A =
            TestSpell.at(TestSpellTree.DIVERGING, BranchLevel.MUDA, TestSpellBranch.BRANCH_A);
    private static final Spell MUDA_B =
            TestSpell.at(TestSpellTree.DIVERGING, BranchLevel.MUDA, TestSpellBranch.BRANCH_B);

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** A Talento raising the general Árvore de Magia cap by rungs — a stand-in Metamágico. */
    private static Feat capRaisingFeat(final int rungs) {
        // Extends AbstractFeat, the extension point for the sealed Feat hierarchy — a homebrew
        // Talento with real behaviour is written exactly this way.
        return new AbstractFeat(FeatCategory.METAMAGICO,
                "Raises the Árvore de Magia cap by " + rungs + ".",
                FeatRequirements.builder().build()) {
            @Override
            public int resolveBranchLevelIncrease(final Character character) {
                return rungs;
            }
        };
    }

    private static Character character(final List<Feat> feats, final Spell... spells) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>(feats))
                .spells(new ArrayList<>(Arrays.asList(spells)))
                .build();
    }

    private static CharacterSheet sheetFor(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    // ---------- getMaxBranchLevel ----------

    @Test
    void theCapStartsAtSementeWithNoFeats() {
        assertEquals(BranchLevel.SEMENTE, spellService.getMaxBranchLevel(character(List.of())));
    }

    @Test
    void everyCapRaisingFeatAdvancesTheCap() {
        Character character = character(List.of(capRaisingFeat(1), capRaisingFeat(2)));

        assertEquals(BranchLevel.EMERGENTE, spellService.getMaxBranchLevel(character));
    }

    @Test
    void theCapClampsAtFlorescente() {
        Character character = character(List.of(capRaisingFeat(99)));

        assertEquals(BranchLevel.FLORESCENTE, spellService.getMaxBranchLevel(character));
    }

    @Test
    void aFeatThatDoesNotRaiseTheCapLeavesItAlone() {
        Character character = character(List.of(capRaisingFeat(0)));

        assertEquals(BranchLevel.SEMENTE, spellService.getMaxBranchLevel(character));
    }

    // ---------- grantSpell ----------

    @Test
    void grantSpellAddsTheSpellWhenEveryGatePasses() {
        Character character = character(List.of());

        spellService.grantSpell(character, sheetFor(character), TRUNK_SEMENTE);

        assertEquals(List.of(TRUNK_SEMENTE), character.getSpells());
    }

    @Test
    void grantSpellRejectsASpellDeeperThanTheCap() {
        Character character = character(List.of(), TRUNK_SEMENTE);

        assertThrows(IllegalOperationException.class,
                () -> spellService.grantSpell(character, sheetFor(character), TRUNK_BROTO));
    }

    @Test
    void grantSpellRejectsASpellWithNoFootholdBelowIt() {
        Character character = character(List.of(capRaisingFeat(4)));

        assertThrows(IllegalOperationException.class,
                () -> spellService.grantSpell(character, sheetFor(character), MUDA_A));
    }

    @Test
    void grantSpellRejectsTheOppositeRamificacao() {
        Character character = character(List.of(capRaisingFeat(4)), TRUNK_SEMENTE, TRUNK_BROTO, MUDA_A);

        assertThrows(IllegalOperationException.class,
                () -> spellService.grantSpell(character, sheetFor(character), MUDA_B));
    }

    @Test
    void aRejectedGrantLeavesTheKnownSpellsUntouched() {
        Character character = character(List.of(), TRUNK_SEMENTE);

        assertThrows(IllegalOperationException.class,
                () -> spellService.grantSpell(character, sheetFor(character), TRUNK_BROTO));

        assertEquals(List.of(TRUNK_SEMENTE), character.getSpells());
    }

    @Test
    void aRaisedCapUnblocksAGrantThatWasRefusedBefore() {
        Character blocked = character(List.of(), TRUNK_SEMENTE);
        assertThrows(IllegalOperationException.class,
                () -> spellService.grantSpell(blocked, sheetFor(blocked), TRUNK_BROTO));

        Character raised = character(List.of(capRaisingFeat(1)), TRUNK_SEMENTE);
        spellService.grantSpell(raised, sheetFor(raised), TRUNK_BROTO);

        assertTrue(raised.getSpells().contains(TRUNK_BROTO));
    }

    @Test
    void grantSpellSpendsNoExperience() {
        Character character = character(List.of());
        CharacterSheet sheet = sheetFor(character);
        sheet.accumulateExperience(java.math.BigDecimal.TEN);

        spellService.grantSpell(character, sheet, TRUNK_SEMENTE);

        assertEquals(0, java.math.BigDecimal.TEN.compareTo(sheet.getUnUsedExperience()));
    }
}
