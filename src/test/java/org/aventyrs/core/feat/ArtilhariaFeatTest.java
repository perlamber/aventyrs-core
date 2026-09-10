package org.aventyrs.core.feat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.character.services.AttackRangeService;
import org.aventyrs.core.character.services.AttackRangeServiceImpl;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TIRO_LONGO judged as a player meets it — a character with 3 Graduações em Ataque à Distância
 * who pays its XP through {@link FeatService#grantFeat} and reaches one band further afterwards.
 * The objective is the {@link Range} that {@code AttackRangeService} reports, never the hook's
 * own return value; see the {@code testing-a-feat} skill.
 */
class ArtilhariaFeatTest {

    private final FeatService featService = new FeatServiceImpl();
    private final AttackRangeService attackRangeService = new AttackRangeServiceImpl();

    private static final Weapon LONGBOW = AbstractWeapon.builder()
            .name("Arco Longo")
            .category(ItemCategory.BOW)
            .damageBase(DamageBase.of(1, 3))
            .skillType(SkillType.ATAQUE_A_DISTANCIA)
            .range(Range.DISTANCIA_LONGA)
            .build();

    /** The control weapon: a corpo-a-corpo swing, out of "ataques à Distância" scope. */
    private static final Weapon DAGGER = AbstractWeapon.builder()
            .name("Adaga")
            .category(ItemCategory.LIGHT_BLADE)
            .damageBase(DamageBase.of(1, 2))
            .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .build();

    private static final Spell RANGED_MAGIA = new TestSpell() {
        @Override
        public SpellTargeting getTargeting() {
            return SpellTargeting.distancia(Range.DISTANCIA_MEDIA);
        }
    };

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    private static CharacterSkill rangedAt(final int graduationValue) {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_A_DISTANCIA_1).build();
        skill.increaseGraduation(graduationValue);
        return skill;
    }

    /** Satisfies TIRO_LONGO's own "3 Graduações em Ataque à Distância" — exactly, not generously. */
    private static Character.CharacterBuilder marksman() {
        return character().skill(SkillType.ATAQUE_A_DISTANCIA, rangedAt(3));
    }

    private static CharacterSheet sheetWith(final Character character, final BigDecimal experience) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(experience);
        return sheet;
    }

    // ---------- catalog shape ----------

    @Test
    void everyConstantBelongsToTheArtilhariaTree() {
        for (ArtilhariaFeat feat : ArtilhariaFeat.values()) {
            assertEquals(FeatCategory.ARTILHARIA, feat.getFeatCategory(), feat.name());
        }
    }

    @Test
    void everyConstantHasADescriptionAndRequirements() {
        for (ArtilhariaFeat feat : ArtilhariaFeat.values()) {
            assertFalse(feat.getDescription().isBlank(), feat.name());
            assertNotNull(feat.getFeatRequirements(), feat.name());
        }
    }

    @Test
    void theTreeHasEveryTalentoTheCatalogAuthors() {
        assertEquals(10, ArtilhariaFeat.values().length);
    }

    // ---------- can a character reach it? ----------

    @Test
    void threeGraduacoesEmAtaqueADistanciaIsEnoughToAcquireIt() throws IllegalOperationException {
        Character character = marksman().build();
        CharacterSheet sheet = sheetWith(character, BigDecimal.TEN);
        int cost = character.getRace().getNewFeatCost(FeatCategory.ARTILHARIA);

        featService.grantFeat(character, sheet, ArtilhariaFeat.TIRO_LONGO);

        assertEquals(BigDecimal.valueOf(10 - cost), sheet.getUnUsedExperience());
    }

    @Test
    void twoGraduacoesIsNotEnoughAndTheRejectedGrantSpendsNothing() {
        Character character = character().skill(SkillType.ATAQUE_A_DISTANCIA, rangedAt(2)).build();
        CharacterSheet sheet = sheetWith(character, BigDecimal.TEN);

        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(character, sheet, ArtilhariaFeat.TIRO_LONGO));
        assertEquals(BigDecimal.TEN, sheet.getUnUsedExperience());
    }

    // ---------- what changes once they hold it? ----------

    @Test
    void acquiringItWidensARangedWeaponsMaximumDistanceByOneBand() throws IllegalOperationException {
        Character character = marksman().build();
        Range before = attackRangeService.getEffectiveRange(character, LONGBOW);

        featService.grantFeat(character, sheetWith(character, BigDecimal.TEN), ArtilhariaFeat.TIRO_LONGO);

        assertEquals(before.increasedBy(1), attackRangeService.getEffectiveRange(character, LONGBOW));
    }

    /** "físicos e Mágicos" — a ranged Magia's reach widens the same way. */
    @Test
    void acquiringItWidensARangedMagiasReachToo() throws IllegalOperationException {
        Character character = marksman().build();

        featService.grantFeat(character, sheetWith(character, BigDecimal.TEN), ArtilhariaFeat.TIRO_LONGO);

        assertEquals(Optional.of(Range.DISTANCIA_LONGA),
                attackRangeService.getEffectiveRange(character, RANGED_MAGIA));
    }

    // ---------- what must not change? ----------

    /** "ataques à Distância" — a corpo-a-corpo swing is out of scope and stays at Adjacente. */
    @Test
    void itLeavesACorpoACorpoWeaponUntouched() throws IllegalOperationException {
        Character character = marksman().build();

        featService.grantFeat(character, sheetWith(character, BigDecimal.TEN), ArtilhariaFeat.TIRO_LONGO);

        assertEquals(Range.ADJACENTE, attackRangeService.getEffectiveRange(character, DAGGER));
    }

    @Test
    void aMarksmanHoldingNoTalentoReachesExactlyTheWeaponsOwnAlcance() {
        Character character = marksman().build();

        assertEquals(Range.DISTANCIA_LONGA, attackRangeService.getEffectiveRange(character, LONGBOW));
    }

    /** No other constant in the tree grants a range step — a copy-pasted override would fail here. */
    @Test
    void everyOtherConstantGrantsNoRangeStep() {
        Character character = marksman().build();
        for (ArtilhariaFeat feat : ArtilhariaFeat.values()) {
            if (feat == ArtilhariaFeat.TIRO_LONGO) {
                continue;
            }
            assertEquals(0, feat.resolveAttackRangeIncrease(character, LONGBOW), feat.name());
        }
    }
}
