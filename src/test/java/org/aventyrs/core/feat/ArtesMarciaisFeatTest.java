package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageBaseService;
import org.aventyrs.core.character.services.DamageBaseServiceImpl;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ARTISTA_MARCIAL judged as a player meets it — a character who satisfies its Pré-requisito,
 * pays its XP through {@link FeatService#grantFeat}, and swings for more afterwards. The
 * objective is the {@link DamageBase} that {@code DamageBaseService} reports for that character,
 * never the hook's own return value; see the {@code testing-a-feat} skill for why.
 */
class ArtesMarciaisFeatTest {

    private final FeatService featService = new FeatServiceImpl();
    private final DamageBaseService damageBaseService = new DamageBaseServiceImpl();

    /** A 2d6+0 Corpo-a-Corpo weapon — four rows up the scale, so a scale-up is visible either way. */
    private static final Weapon ESPADA = AbstractWeapon.builder()
            .name("Espada Longa")
            .category(ItemCategory.HEAVY_BLADE)
            .damageBase(DamageBase.of(2, 0))
            .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .build();

    /** The control weapon: same starting row, swung with the other Perícia de Ataque. */
    private static final Weapon ARCO = AbstractWeapon.builder()
            .name("Arco Longo")
            .category(ItemCategory.BOW)
            .damageBase(DamageBase.of(2, 0))
            .skillType(SkillType.ATAQUE_A_DISTANCIA)
            .build();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    private static CharacterSkill meleeAt(final int graduationValue) {
        return CharacterSkill.builder()
                .skill(new AtaqueCorpoACorpo())
                .graduation(SkillGraduation.builder().graduationValue(graduationValue).build())
                .build();
    }

    private static CharacterAttributes strength(final int base) {
        return CharacterAttributes.builder()
                .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(base).build())
                .build();
    }

    /**
     * Satisfies ARTISTA_MARCIAL's own "Força 2 e 2 Graduações em Ataque Corpo-a-Corpo" —
     * <em>exactly</em>, never generously, so a typo in the constant's own requiredAttributeValue
     * or requiredSkillGraduation would show up as a rejected grant here.
     */
    private static Character.CharacterBuilder martialArtist() {
        return character()
                .attributes(strength(2))
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, meleeAt(2));
    }

    private static CharacterSheet sheetWith(final Character character, final BigDecimal experience) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(experience);
        return sheet;
    }

    // ---------- catalog shape ----------

    @Test
    void everyConstantBelongsToTheArteMarcialTree() {
        for (ArtesMarciaisFeat feat : ArtesMarciaisFeat.values()) {
            assertEquals(FeatCategory.ARTE_MARCIAL, feat.getFeatCategory(), feat.name());
        }
    }

    @Test
    void everyConstantHasADescriptionAndRequirements() {
        for (ArtesMarciaisFeat feat : ArtesMarciaisFeat.values()) {
            assertFalse(feat.getDescription().isBlank(), feat.name());
            assertNotNull(feat.getFeatRequirements(), feat.name());
        }
    }

    @Test
    void theTreeHasOneTalento() {
        assertEquals(1, ArtesMarciaisFeat.values().length);
    }

    // ---------- can a character reach it? ----------

    @Test
    void aCharacterShortOfForcaCannotAcquireIt() {
        Character character = character()
                .attributes(strength(1))
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, meleeAt(2))
                .build();
        CharacterSheet sheet = sheetWith(character, BigDecimal.TEN);

        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(character, sheet, ArtesMarciaisFeat.ARTISTA_MARCIAL));
        assertEquals(BigDecimal.TEN, sheet.getUnUsedExperience(), "a rejected grant spends nothing");
    }

    @Test
    void aCharacterShortOfMeleeGraduacoesCannotAcquireIt() {
        Character character = character()
                .attributes(strength(2))
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, meleeAt(1))
                .build();
        CharacterSheet sheet = sheetWith(character, BigDecimal.TEN);

        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(character, sheet, ArtesMarciaisFeat.ARTISTA_MARCIAL));
        assertEquals(BigDecimal.TEN, sheet.getUnUsedExperience(), "a rejected grant spends nothing");
    }

    /** An untrained Perícia reads as Graduação 0 — the Talento is out of reach, not defaulted in. */
    @Test
    void anUntrainedMeleeCharacterCannotAcquireIt() {
        Character character = character().attributes(strength(2)).build();

        assertFalse(ArtesMarciaisFeat.ARTISTA_MARCIAL.isEligible(character));
    }

    @Test
    void meetingBothHalvesExactlyIsEnoughToAcquireIt() throws IllegalOperationException {
        Character character = martialArtist().build();
        CharacterSheet sheet = sheetWith(character, BigDecimal.TEN);

        featService.grantFeat(character, sheet, ArtesMarciaisFeat.ARTISTA_MARCIAL);

        assertEquals(List.of(ArtesMarciaisFeat.ARTISTA_MARCIAL), character.getFeats());
    }

    @Test
    void acquiringItSpendsExactlyItsRaceCost() throws IllegalOperationException {
        Character character = martialArtist().build();
        CharacterSheet sheet = sheetWith(character, BigDecimal.TEN);
        int cost = character.getRace().getNewFeatCost(FeatCategory.ARTE_MARCIAL);

        featService.grantFeat(character, sheet, ArtesMarciaisFeat.ARTISTA_MARCIAL);

        assertEquals(BigDecimal.valueOf(10 - cost), sheet.getUnUsedExperience());
    }

    // ---------- what changes once they hold it? ----------

    @Test
    void acquiringItRaisesTheDanoBaseTheCharacterSwingsWith() throws IllegalOperationException {
        Character character = martialArtist().build();
        DamageBase before = damageBaseService.getDamageBase(character, ESPADA);

        featService.grantFeat(character, sheetWith(character, BigDecimal.TEN), ArtesMarciaisFeat.ARTISTA_MARCIAL);

        assertEquals(before.scaledUp(1), damageBaseService.getDamageBase(character, ESPADA));
    }

    /** The same grant reaches an Ataque Desarmado, which starts at the bottom of the scale. */
    @Test
    void acquiringItRaisesAnUnarmedStrikeToo() throws IllegalOperationException {
        Character character = martialArtist().build();

        featService.grantFeat(character, sheetWith(character, BigDecimal.TEN), ArtesMarciaisFeat.ARTISTA_MARCIAL);

        assertEquals(DamageBase.UNARMED.scaledUp(1),
                damageBaseService.getDamageBase(character, SkillType.ATAQUE_CORPO_A_CORPO));
    }

    /**
     * The scaling term at two points: "+1, cumulativamente +1 para cada Título Aventyr Desperto".
     * One point alone couldn't tell a scaling term from a flat constant.
     */
    @Test
    void itScalesWithEachTituloAventyrDesperto() throws IllegalOperationException {
        Character titleless = martialArtist().build();
        Character oneTitle = martialArtist().primaryTitle(new Santo(List.of(), List.of())).build();
        DamageBase before = damageBaseService.getDamageBase(titleless, ESPADA);

        featService.grantFeat(titleless, sheetWith(titleless, BigDecimal.TEN), ArtesMarciaisFeat.ARTISTA_MARCIAL);
        featService.grantFeat(oneTitle, sheetWith(oneTitle, BigDecimal.TEN), ArtesMarciaisFeat.ARTISTA_MARCIAL);

        assertEquals(before.scaledUp(1), damageBaseService.getDamageBase(titleless, ESPADA));
        assertEquals(before.scaledUp(2), damageBaseService.getDamageBase(oneTitle, ESPADA));
    }

    @Test
    void twoDespertoTitlesScaleItTwice() throws IllegalOperationException {
        Character twoTitles = martialArtist()
                .primaryTitle(new Santo(List.of(), List.of()))
                .secondaryTitle(new Santo(List.of(), List.of()))
                .build();
        DamageBase before = damageBaseService.getDamageBase(twoTitles, ESPADA);

        featService.grantFeat(twoTitles, sheetWith(twoTitles, BigDecimal.TEN), ArtesMarciaisFeat.ARTISTA_MARCIAL);

        assertEquals(before.scaledUp(3), damageBaseService.getDamageBase(twoTitles, ESPADA));
    }

    // ---------- what must not change? ----------

    /**
     * A character who never met the Pré-requisito swings for exactly what they did before —
     * the Talento's effect is unreachable, not merely unapplied.
     */
    @Test
    void aCharacterWhoCannotAcquireItSwingsForTheSameDanoBase() {
        Character character = character().attributes(strength(1)).build();
        DamageBase before = damageBaseService.getDamageBase(character, ESPADA);
        CharacterSheet sheet = sheetWith(character, BigDecimal.TEN);

        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(character, sheet, ArtesMarciaisFeat.ARTISTA_MARCIAL));
        assertEquals(before, damageBaseService.getDamageBase(character, ESPADA));
    }

    /**
     * The known over-application, pinned deliberately: the clause names Ataques Desarmados e
     * Armas Naturais, but nothing classifies an attack as either (see CLAUDE.md's gap catalog),
     * so the grant currently reaches a wielded Arco too. This assertion is what should change
     * the day that classification lands — it is not a claim that the ranged swing *should*
     * benefit.
     */
    @Test
    void itCurrentlyReachesEveryAttackForLackOfAnUnarmedMarker() throws IllegalOperationException {
        Character character = martialArtist().build();
        DamageBase before = damageBaseService.getDamageBase(character, ARCO);

        featService.grantFeat(character, sheetWith(character, BigDecimal.TEN), ArtesMarciaisFeat.ARTISTA_MARCIAL);

        assertEquals(before.scaledUp(1), damageBaseService.getDamageBase(character, ARCO));
    }

    @Test
    void aCharacterHoldingNoTalentoIsUnaffected() {
        Character character = martialArtist().build();

        assertEquals(DamageBase.of(2, 0), damageBaseService.getDamageBase(character, ESPADA));
        assertEquals(DamageBase.UNARMED,
                damageBaseService.getDamageBase(character, SkillType.ATAQUE_CORPO_A_CORPO));
    }

    @Test
    void itIsOfferedToAQualifyingCharacterAndWithheldFromAnUnqualifiedOne() {
        assertTrue(featService.getAvailableFeats(martialArtist().build())
                .contains(ArtesMarciaisFeat.ARTISTA_MARCIAL));
        assertFalse(featService.getAvailableFeats(character().build())
                .contains(ArtesMarciaisFeat.ARTISTA_MARCIAL));
    }
}
