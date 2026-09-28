package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.AbstractFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.feat.FeatRequirements;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.FreeSpellPick;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.magic.catalog.PiromanciaSpell;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.magic.TestSpellBranch;
import org.aventyrs.core.magic.TestSpellTree;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private static final Spell LINEAR_SEMENTE =
            TestSpell.at(TestSpellTree.LINEAR, BranchLevel.SEMENTE, null);

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

    /** A sheet with enough XP that a gate test never fails on the price instead. */
    private static CharacterSheet sheetFor(final Character character) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        return sheet;
    }

    private static CharacterSheet brokeSheetFor(final Character character) {
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

    // ---------- getKnownTrees ----------

    @Test
    void getKnownTreesIsEmptyWithNoSpells() {
        assertEquals(Set.of(), spellService.getKnownTrees(character(List.of())));
    }

    @Test
    void getKnownTreesListsEachDistinctTreeOnce() {
        Character character = character(List.of(), TRUNK_SEMENTE, TRUNK_BROTO, LINEAR_SEMENTE);

        assertEquals(Set.of(TestSpellTree.DIVERGING, TestSpellTree.LINEAR),
                spellService.getKnownTrees(character));
    }

    // ---------- getAcquisitionCost ----------

    @Test
    void getAcquisitionCostFollowsTheRungLadder() {
        Character character = character(List.of());

        assertEquals(0, BigDecimal.ZERO.compareTo(spellService.getAcquisitionCost(character, TRUNK_SEMENTE)));
        assertEquals(0, BigDecimal.ONE.compareTo(spellService.getAcquisitionCost(character, TRUNK_BROTO)));
        assertEquals(0, BigDecimal.valueOf(2).compareTo(spellService.getAcquisitionCost(character, MUDA_A)));
    }

    @Test
    void aFeatThatGrantsTheSpellWaivesItsCost() {
        Character character = character(List.of(freeBrotoFeat()), TRUNK_SEMENTE);

        assertEquals(0, BigDecimal.ZERO.compareTo(spellService.getAcquisitionCost(character, TRUNK_BROTO)));
    }

    @Test
    void aFeatDiscountIsSubtractedFromTheRungCost() {
        Character character = character(List.of(discountFeat("0.5")));

        assertEquals(0, new BigDecimal("1.5").compareTo(spellService.getAcquisitionCost(character, MUDA_A)));
    }

    @Test
    void discountsFromEveryFeatStackAndFloorAtZero() {
        Character character = character(List.of(discountFeat("0.5"), discountFeat("5")));

        assertEquals(0, BigDecimal.ZERO.compareTo(spellService.getAcquisitionCost(character, TRUNK_BROTO)));
    }

    /** A homebrew Talento that only knows how to discount spell acquisition. */
    private static Feat discountFeat(final String amount) {
        return new AbstractFeat(FeatCategory.METAMAGICO, "Spell discount of " + amount + ".",
                FeatRequirements.builder().build()) {
            @Override
            public BigDecimal resolveSpellAcquisitionCostReduction(final Character character, final Spell spell) {
                return new BigDecimal(amount);
            }
        };
    }

    // ---------- grantSpell ----------

    @Test
    void grantSpellAddsTheSpellWhenEveryGatePasses() {
        Character character = character(List.of(treeSlotsFeat(1)));

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

    // ---------- grantSpell — the XP price ----------

    @Test
    void aSementeCostsNothing() {
        Character character = character(List.of(treeSlotsFeat(1)));
        CharacterSheet sheet = brokeSheetFor(character);

        spellService.grantSpell(character, sheet, TRUNK_SEMENTE);

        assertEquals(0, BigDecimal.ZERO.compareTo(sheet.getUnUsedExperience()));
    }

    @Test
    void grantSpellSpendsTheRungCost() {
        Character character = character(List.of(capRaisingFeat(1)), TRUNK_SEMENTE);
        CharacterSheet sheet = sheetFor(character);

        spellService.grantSpell(character, sheet, TRUNK_BROTO);

        assertEquals(0, BigDecimal.valueOf(99).compareTo(sheet.getUnUsedExperience()));
    }

    @Test
    void grantSpellSpendsTheDiscountedCost() {
        Character character = character(List.of(capRaisingFeat(2), discountFeat("1")), TRUNK_SEMENTE, TRUNK_BROTO);
        CharacterSheet sheet = sheetFor(character);

        spellService.grantSpell(character, sheet, MUDA_A); // rung cost 2, minus the 1 discount

        assertEquals(0, BigDecimal.valueOf(99).compareTo(sheet.getUnUsedExperience()));
    }

    /**
     * "Um personagem que aprenda a versão base automaticamente aprende sua segunda versão" — so an
     * Efeito Alternativo is never acquired, and granting one would charge experience for a Magia
     * already known. It reports its parent's rung, so it would also satisfy the climb gate for the
     * next one.
     */
    @Test
    void grantSpellRefusesASecondVersionAndSpendsNothing() {
        Character character = character(List.of(capRaisingFeat(1)), VidaSpell.ALIVIAR_A_DOR);
        CharacterSheet sheet = sheetFor(character);
        Spell secondVersion = VidaSpell.ALIVIAR_A_DOR.getAlternateVersion().orElseThrow();

        assertThrows(IllegalOperationException.class,
                () -> spellService.grantSpell(character, sheet, secondVersion));

        assertEquals(List.of(VidaSpell.ALIVIAR_A_DOR), character.getSpells());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(sheet.getUnUsedExperience()));
    }

    @Test
    void grantSpellRejectsAndSpendsNothingWhenExperienceIsShort() {
        Character character = character(List.of(capRaisingFeat(1)), TRUNK_SEMENTE);
        CharacterSheet sheet = brokeSheetFor(character);

        assertThrows(IllegalOperationException.class,
                () -> spellService.grantSpell(character, sheet, TRUNK_BROTO));

        assertEquals(List.of(TRUNK_SEMENTE), character.getSpells());
        assertEquals(0, BigDecimal.ZERO.compareTo(sheet.getUnUsedExperience()));
    }

    @Test
    void aFeatGrantedSpellIsAcquiredWithoutSpendingExperience() {
        Character character = character(List.of(freeBrotoFeat()), TRUNK_SEMENTE);
        CharacterSheet sheet = brokeSheetFor(character);

        spellService.grantSpell(character, sheet, TRUNK_BROTO);

        assertTrue(character.getSpells().contains(TRUNK_BROTO));
        assertEquals(0, BigDecimal.ZERO.compareTo(sheet.getUnUsedExperience()));
    }

    /** A stand-in for ARCANISTA: raises the cap and hands out TRUNK_BROTO as its own benefit. */
    private static Feat freeBrotoFeat() {
        return new AbstractFeat(FeatCategory.METAMAGICO, "Grants the Broto free.",
                FeatRequirements.builder().build()) {
            @Override
            public int resolveBranchLevelIncrease(final Character character) {
                return 1;
            }

            @Override
            public boolean grantsFreeSpellAcquisition(final Character character, final Spell spell) {
                return spell == TRUNK_BROTO;
            }
        };
    }

    // ---------- Árvores conhecidas — the slot gate ----------

    /** A stand-in for ARCANISTA's "conhece N Árvores": capacity only. */
    private static Feat treeSlotsFeat(final int slots) {
        return new AbstractFeat(FeatCategory.METAMAGICO, "Knows " + slots + " trees.",
                FeatRequirements.builder().build()) {
            @Override
            public int resolveKnownTreeCapacity(final Character character) {
                return slots;
            }
        };
    }

    /** A stand-in for a ladder rung: raises the cap by rungs and owes picks free Magias at rung. */
    private static Feat freePicksFeat(final int rungs, final BranchLevel rung, final int picks) {
        return new AbstractFeat(FeatCategory.METAMAGICO, "Owes " + picks + " at " + rung + ".",
                FeatRequirements.builder().build()) {
            @Override
            public int resolveBranchLevelIncrease(final Character character) {
                return rungs;
            }

            @Override
            public List<FreeSpellPick> resolveFreeSpellPicks(final Character character) {
                return List.of(new FreeSpellPick(rung, picks));
            }
        };
    }

    @Test
    void withoutATreeSlotNoSpellCanBeLearned() {
        Character character = character(List.of());
        CharacterSheet sheet = sheetFor(character);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> spellService.grantSpell(character, sheet, TRUNK_SEMENTE));

        assertEquals("SPELL_TREE_CAPACITY_REACHED", refused.getMessage());
        assertEquals(List.of(), character.getSpells());
    }

    @Test
    void aFullSetOfTreesRefusesANewTreeButNotAKnownOne() {
        Character character = character(List.of(treeSlotsFeat(1), capRaisingFeat(1)), TRUNK_SEMENTE);

        assertThrows(IllegalOperationException.class,
                () -> spellService.grantSpell(character, sheetFor(character), LINEAR_SEMENTE));
        spellService.grantSpell(character, sheetFor(character), TRUNK_BROTO);

        assertEquals(List.of(TRUNK_SEMENTE, TRUNK_BROTO), character.getSpells());
    }

    @Test
    void openTreeSlotsAreTheCapacityLessTheKnownTrees() {
        Character character = character(List.of(treeSlotsFeat(2), treeSlotsFeat(1)), TRUNK_SEMENTE);

        assertEquals(3, spellService.getKnownTreeCapacity(character));
        assertEquals(2, spellService.getOpenTreeSlots(character));
    }

    @Test
    void openTreeSlotsNeverGoNegative() {
        Character character = character(List.of(treeSlotsFeat(1)), TRUNK_SEMENTE, LINEAR_SEMENTE);

        assertEquals(0, spellService.getOpenTreeSlots(character));
        assertEquals(List.of(), spellService.getLearnableTrees(character));
    }

    @Test
    void learnableTreesAreEveryCatalogTreeNotYetKnown() {
        Character character = character(List.of(treeSlotsFeat(2)), VidaSpell.ALIVIAR_A_DOR);

        List<?> learnable = spellService.getLearnableTrees(character);

        assertEquals(MagicTree.values().length - 1, learnable.size());
        assertFalse(learnable.contains(MagicTree.VIDA));
    }

    @Test
    void learnTreeGrantsItsSementeFree() {
        Character character = character(List.of(treeSlotsFeat(1)));
        CharacterSheet sheet = brokeSheetFor(character);

        List<Spell> learned = spellService.learnTree(character, sheet, MagicTree.VIDA);

        assertEquals(1, learned.size());
        assertEquals(BranchLevel.SEMENTE, learned.get(0).getBranchLevel());
        assertEquals(learned, character.getSpells());
    }

    @Test
    void learnTreeRefusesWithNoSlotAndChangesNothing() {
        Character character = character(List.of());

        assertThrows(IllegalOperationException.class,
                () -> spellService.learnTree(character, sheetFor(character), MagicTree.VIDA));

        assertEquals(List.of(), character.getSpells());
    }

    @Test
    void learnTreeRefusesATreeAlreadyKnown() {
        Character character = character(List.of(treeSlotsFeat(3)), VidaSpell.ALIVIAR_A_DOR);

        assertThrows(IllegalOperationException.class,
                () -> spellService.learnTree(character, sheetFor(character), MagicTree.VIDA));
    }

    // ---------- the free rung picks ----------

    @Test
    void nothingIsOwedWithoutAFreePickTalento() {
        assertEquals(Map.of(), spellService.getOwedFreeSpells(character(List.of(capRaisingFeat(1)))));
    }

    @Test
    void owedPicksShrinkAsDistinctTreesHoldTheRung() {
        Character none = character(List.of(freePicksFeat(1, BranchLevel.BROTO, 2)), TRUNK_SEMENTE);
        Character one = character(List.of(freePicksFeat(1, BranchLevel.BROTO, 2)), TRUNK_SEMENTE, TRUNK_BROTO);

        assertEquals(Map.of(BranchLevel.BROTO, 2), spellService.getOwedFreeSpells(none));
        assertEquals(Map.of(BranchLevel.BROTO, 1), spellService.getOwedFreeSpells(one));
    }

    @Test
    void theFirstBrotoOfATreeIsFreeWhilePicksAreOwed() {
        Character character = character(List.of(freePicksFeat(1, BranchLevel.BROTO, 2)), TRUNK_SEMENTE);
        CharacterSheet sheet = brokeSheetFor(character);

        spellService.grantSpell(character, sheet, TRUNK_BROTO);

        assertTrue(character.getSpells().contains(TRUNK_BROTO));
        assertEquals(0, BigDecimal.ZERO.compareTo(sheet.getUnUsedExperience()));
    }

    @Test
    void onceThePicksAreSpentTheRungCostsItsPrice() {
        Character character = character(List.of(freePicksFeat(1, BranchLevel.BROTO, 1)),
                TRUNK_SEMENTE, TRUNK_BROTO, VidaSpell.ALIVIAR_A_DOR);

        assertEquals(Map.of(), spellService.getOwedFreeSpells(character));
        assertEquals(0, BigDecimal.ONE.compareTo(spellService.getAcquisitionCost(character, VidaSpell.REVIGORAR)));
    }

    @Test
    void aSecondBrotoInTheSameTreeIsNeverAFreePick() {
        Character character = character(List.of(freePicksFeat(1, BranchLevel.BROTO, 2)),
                TRUNK_SEMENTE, TRUNK_BROTO);

        assertEquals(Map.of(BranchLevel.BROTO, 1), spellService.getOwedFreeSpells(character));
        assertEquals(0, BigDecimal.ONE.compareTo(spellService.getAcquisitionCost(character,
                TestSpell.at(TestSpellTree.DIVERGING, BranchLevel.BROTO, null))));
    }

    @Test
    void aDivergingTreeOffersBothRamificacoesAsFreeOptions() {
        Character character = character(List.of(freePicksFeat(1, BranchLevel.BROTO, 2)), PiromanciaSpell.LUZ_DE_VELA);

        List<Spell> options = spellService.getFreeSpellOptions(character, BranchLevel.BROTO);

        assertEquals(2, options.size());
        assertEquals(2, options.stream().map(Spell::getBranch).distinct().count());
    }

    @Test
    void takingOneRamificacaoRemovesTheTreeFromTheOptions() {
        Character character = character(List.of(freePicksFeat(1, BranchLevel.BROTO, 2)), PiromanciaSpell.LUZ_DE_VELA);
        Spell eldur = spellService.getFreeSpellOptions(character, BranchLevel.BROTO).get(0);

        spellService.grantSpell(character, brokeSheetFor(character), eldur);

        assertEquals(List.of(), spellService.getFreeSpellOptions(character, BranchLevel.BROTO));
    }

    @Test
    void noOptionsAreOfferedForARungNothingIsOwedAt() {
        Character character = character(List.of(capRaisingFeat(1)), PiromanciaSpell.LUZ_DE_VELA);

        assertEquals(List.of(), spellService.getFreeSpellOptions(character, BranchLevel.BROTO));
    }
}
