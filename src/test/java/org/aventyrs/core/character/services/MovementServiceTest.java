package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.ability.DexterityAbility;
import org.aventyrs.core.feat.MobilidadeFeat;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementServiceTest {

    private final MovementService movementService = new MovementServiceImpl();

    private static class MovementBonusAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.DEXTERITY;
        }

        @Override
        public String getDescription() {
            return "Test-only +1 Movimento Base bonus source.";
        }

        @Modifier(ModifierType.MOVEMENT)
        public int bonus() {
            return 1;
        }
    }

    /**
     * Grants Pontos de Acao, never Movimento — the source under test in {@code
     * extraActionPointsDoNotWidenMovementBase}.
     */
    private static class ActionPointsBonusAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.DEXTERITY;
        }

        @Override
        public String getDescription() {
            return "Test-only +2 Pontos de Acao bonus source.";
        }

        @Modifier(ModifierType.ACTION_POINTS)
        public int bonus() {
            return 2;
        }
    }

    private static class MovementMalusAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.DEXTERITY;
        }

        @Override
        public String getDescription() {
            return "Test-only -100 Movimento Base malus source.";
        }

        @Modifier(ModifierType.MOVEMENT)
        public int malus() {
            return -100;
        }
    }

    private static class MovementBonusSkillCompetencyAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATLETISMO;
        }

        @Override
        public String getDescription() {
            return "Test-only +1 Movimento Base bonus source.";
        }

        @Modifier(ModifierType.MOVEMENT)
        public int bonus() {
            return 1;
        }
    }

    private static class RaceWithMovementBonus extends Human {
        @Override
        public List<SkillCompetencyAbility> getRacialAbilities() {
            return List.of(new MovementBonusSkillCompetencyAbility());
        }
    }

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    @Test
    void baseMovementIsTheSizeCategoryMovementPerActionPoint() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        // SizeCategory.ZERO's 4UD per Ponto de Acao, not multiplied by how many the character has.
        assertEquals(4, movementService.getMovementBase(character));
    }

    @Test
    void largerSizeCategoriesMoveFurtherPerActionPoint() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .sizeCategory(SizeCategory.PLUS_ONE)
                .build();
        // SizeCategory.PLUS_ONE's 5UD per Ponto de Acao.
        assertEquals(5, movementService.getMovementBase(character));
    }

    @Test
    void attributeAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new MovementBonusAbility())
                .build();
        assertEquals(5, movementService.getMovementBase(character));
    }

    @Test
    void skillCompetencyAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new MovementBonusSkillCompetencyAbility())
                .build();
        assertEquals(5, movementService.getMovementBase(character));
    }

    @Test
    void racialAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new RaceWithMovementBonus())
                .build();
        assertEquals(5, movementService.getMovementBase(character));
    }

    @Test
    void unlockedSkillExcellencyModifierIsAdded() {
        CharacterSkill ataqueCorpoACorpoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        ataqueCorpoACorpoSkill.increaseGraduation(5); // unlocks AtaqueCorpoACorpoExcellency.FOCADO (+2UD).
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, ataqueCorpoACorpoSkill)
                .build();
        assertEquals(6, movementService.getMovementBase(character));
    }

    /**
     * Movimento Base is what a single Ponto de Acao buys, so having more of them buys more
     * total distance without widening the per-point figure — the player decides how many of
     * their Pontos de Acao that Turn go to moving and how many to everything else.
     */
    @Test
    void extraActionPointsDoNotWidenMovementBase() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new ActionPointsBonusAbility())
                .build();
        assertEquals(4, movementService.getMovementBase(character));
    }

    @Test
    void neverNegative() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new MovementMalusAbility())
                .build();
        assertEquals(0, movementService.getMovementBase(character));
    }
    // ---------- The per-movement axis: getMovementBase(CombatantSheet[, movementIndex]) ----------

    private static CharacterSheet sheetOf(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    /**
     * PASSOS_LONGOS is both halves at once: a permanent +1UD and +2UD on the Rodada's first
     * movement. Both are per Ponto de Acao, so the first movement reads 4+1+2 = 7 and every
     * later one 4+1 = 5.
     */
    @Test
    void passosLongosWidensOnlyTheFirstMovementOfTheRodada() {
        CharacterSheet sheet = sheetOf(CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(DexterityAbility.PASSOS_LONGOS)
                .build());

        assertEquals(7, movementService.getMovementBase(sheet, 0));
        assertEquals(5, movementService.getMovementBase(sheet, 1));
        assertEquals(5, movementService.getMovementBase(sheet, 2));
    }

    /** The permanent half alone is what a Character-only caller can see — no first-movement +2. */
    @Test
    void passosLongosPermanentHalfIsAllTheCharacterOverloadSees() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(DexterityAbility.PASSOS_LONGOS)
                .build();

        assertEquals(5, movementService.getMovementBase(character));
    }

    /**
     * The no-index overload resolves against the sheet's own counter, so it answers "the movement
     * I would make next" — and consuming a movement advances that answer.
     */
    @Test
    void theSheetOverloadTracksWhichMovementComesNext() {
        CharacterSheet sheet = sheetOf(CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(DexterityAbility.PASSOS_LONGOS)
                .build());

        assertEquals(7, movementService.getMovementBase(sheet));
        // Reading it again must not consume the first-movement position.
        assertEquals(7, movementService.getMovementBase(sheet));

        assertEquals(0, sheet.consumeMovementThisRound());
        assertEquals(5, movementService.getMovementBase(sheet));
        assertEquals(1, sheet.consumeMovementThisRound());
        assertEquals(5, movementService.getMovementBase(sheet));
    }

    /** startTurn resets the counter, which is what makes the bonus per-Rodada rather than once. */
    @Test
    void startingANewTurnMakesTheNextMovementFirstAgain() {
        CharacterSheet sheet = sheetOf(CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(DexterityAbility.PASSOS_LONGOS)
                .build());
        sheet.consumeMovementThisRound();
        assertEquals(5, movementService.getMovementBase(sheet));

        sheet.startTurn(1);

        assertEquals(0, sheet.getMovementsTakenThisRound());
        assertEquals(7, movementService.getMovementBase(sheet));
    }

    /**
     * VELOCISTA is the Feat-side consumer and the reason movementIndex is a count rather than an
     * "is first" flag: "+1UD para cada outro movimento feito no mesmo Turno" grows without bound.
     * 4 base + 1 permanent + movementIndex.
     */
    @Test
    void velocistaGrowsCumulativelyWithEachMovementAlreadyMade() throws IllegalOperationException {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .build();
        CharacterSheet sheet = sheetOf(character);
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        new FeatServiceImpl().grantFeat(character, sheet, MobilidadeFeat.MOVIMENTO_RAPIDO);
        new FeatServiceImpl().grantFeat(character, sheet, MobilidadeFeat.VELOCISTA);

        // MOVIMENTO_RAPIDO's +2 and VELOCISTA's +1 are both permanent: 4+2+1 = 7 on the first.
        assertEquals(7, movementService.getMovementBase(sheet, 0));
        assertEquals(8, movementService.getMovementBase(sheet, 1));
        assertEquals(9, movementService.getMovementBase(sheet, 2));
    }

    /** A Round-scoped grant from someone else's action stacks with the per-movement axis. */
    @Test
    void theSheetsTemporaryMovementBonusIsAddedOnTopOfBoth() {
        CharacterSheet sheet = sheetOf(CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(DexterityAbility.PASSOS_LONGOS)
                .build());
        sheet.grantTemporaryBonus(ModifierType.MOVEMENT, 2, 1);

        // 4 base + 1 permanent + 2 first-movement + 2 temporary.
        assertEquals(9, movementService.getMovementBase(sheet, 0));
        assertEquals(7, movementService.getMovementBase(sheet, 1));
    }

    /** A character holding nothing scoped to a movement reads the same on every one of them. */
    @Test
    void aCharacterWithNoPerMovementClauseReadsFlatAcrossTheRodada() {
        CharacterSheet sheet = sheetOf(CharacterFixture.blank(CharacterFixture.BLANK).build());

        assertEquals(4, movementService.getMovementBase(sheet, 0));
        assertEquals(4, movementService.getMovementBase(sheet, 3));
    }

    /**
     * The permanent figure is floored first — it is a stat, and a stat cannot be negative — so a
     * character carried below 0 by a malus reads 0 permanently, and the first-movement bonus then
     * applies on top of that valid base rather than being swallowed by the deficit.
     */
    @Test
    void aFlooredPermanentFigureStillTakesThePerMovementBonusOnTop() {
        CharacterSheet sheet = sheetOf(CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new MovementMalusAbility())
                .attributeAbility(DexterityAbility.PASSOS_LONGOS)
                .build());

        assertEquals(2, movementService.getMovementBase(sheet, 0));
        assertEquals(0, movementService.getMovementBase(sheet, 1));
    }

    /** With nothing to lift it, the sheet overload floors at 0 like the permanent one. */
    @Test
    void theSheetOverloadIsAlsoNeverNegative() {
        CharacterSheet sheet = sheetOf(CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new MovementMalusAbility())
                .build());

        assertEquals(0, movementService.getMovementBase(sheet, 0));
    }
}
