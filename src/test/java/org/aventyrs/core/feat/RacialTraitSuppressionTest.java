package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.CharacterSizeService;
import org.aventyrs.core.character.services.CharacterSizeServiceImpl;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.race.Guampo;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.RacialTraitSuppression;
import org.aventyrs.core.race.Troll;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Abandonando seus traços raciais" — {@link RacialTraitSuppression}, the ladder, and which of a
 * {@code Race}'s traits each rung actually silences.
 *
 * <p><b>Why a cross-race matrix rather than per-Talento tests.</b> Three clauses ask for
 * suppression at three different strengths, and what needs guarding is not any one of them but the
 * <em>table</em>: that each rung reaches exactly the traits it should, that a stronger rung
 * contains a weaker one, and above all that suppression removes only the <b>racial</b> term of a
 * stat whose other contributors must survive. A per-Talento test would pass while the table was
 * wrong.
 *
 * <p>The Formas are entered through the bare {@code enterForm} mutator and the Talentos granted
 * through {@code Character#grantFeat}, both of which bypass validation by design — the usual
 * builders-aren't-gatekeepers convention, and what lets one fixture stand in for races the
 * Pré-requisitos would otherwise gate apart. {@code MimetizarFormaHumanaTest}-style acquisition
 * coverage belongs with the constants; this file is about the mechanism.
 */
class RacialTraitSuppressionTest {

    private final CharacterSizeService characterSizeService = new CharacterSizeServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /**
     * A Guampo: Categoria de Tamanho +1, a fixed Gnose/Instinto bonus, and Vigor de Épona — a
     * racial Habilidade that is a real {@code @Modifier}, so its loss shows up as a number off
     * {@code DamageService} rather than only as a missing list entry. One race carrying a trait
     * from three different rungs of the ladder.
     *
     * <p>The size and the Atributo bonus are set by hand because {@code CharacterFixture.blank}
     * bypasses both {@code Race#generateEmptyCharacter} and {@code CharacterCreationService}:
     * this restates what those two would have produced for a Guampo, which is what the
     * suppression is supposed to take away. Setting only the race would leave nothing to suppress
     * and the assertions would pass vacuously.
     */
    private static Character guampo() {
        Guampo race = new Guampo();
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .race(race)
                .feats(new ArrayList<>())
                .sizeCategory(race.getBaseSizeCategory())
                .attributes(CharacterAttributes.builder()
                        .gnose(AttributeValue.builder().domain(AttributeDomain.GNOSE)
                                .base(2)
                                .fixedRacialBonus(race.getFixedAttributeBonuses()
                                        .getOrDefault(AttributeDomain.GNOSE, 0))
                                .build())
                        .build())
                .build();
    }

    /** A Troll: the anatomy pair — one instance of RC and three Efeito Crítico immunities. */
    private static Character troll() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new Troll()).feats(new ArrayList<>()).build();
    }

    private static CharacterSheet sheetOf(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    /** Puts sheet into a shape suppressing at the given rung, using the real catalog Talentos. */
    private static CharacterSheet inFormSuppressing(final Character character,
                                                    final RacialTraitSuppression rung) {
        CharacterSheet sheet = sheetOf(character);
        switch (rung) {
            case NONE -> { }
            case NATURAL_WEAPONS_ONLY -> {
                character.grantFeat(new MetamorfoseDraculeaFeat(
                        java.util.EnumSet.of(FormaMetamorfica.SERPENTE_ESPINHOSA,
                                FormaMetamorfica.CAVALO_DE_CHIFRES)));
                sheet.enterForm(FormType.SERPENTE_ESPINHOSA);
            }
            case PHYSICAL -> {
                character.grantFeat(MonstruosoFeat.MIMETIZAR_FORMA_HUMANA);
                sheet.enterForm(FormType.HUMANA);
            }
            case ALL -> {
                character.grantFeat(FeericoFeat.ANCIENTEFORME);
                sheet.enterForm(FormType.ANCIENTE);
            }
        }
        return sheet;
    }

    // ---------- The ladder itself ----------

    @Test
    void theRungsAreOrderedAndEachContainsTheOneBeforeIt() {
        assertFalse(RacialTraitSuppression.NONE.suppressesNaturalWeapons());
        assertFalse(RacialTraitSuppression.NONE.suppressesPhysicalTraits());
        assertFalse(RacialTraitSuppression.NONE.suppressesInnateTraits());

        assertTrue(RacialTraitSuppression.NATURAL_WEAPONS_ONLY.suppressesNaturalWeapons());
        assertFalse(RacialTraitSuppression.NATURAL_WEAPONS_ONLY.suppressesPhysicalTraits());

        assertTrue(RacialTraitSuppression.PHYSICAL.suppressesNaturalWeapons());
        assertTrue(RacialTraitSuppression.PHYSICAL.suppressesPhysicalTraits());
        assertFalse(RacialTraitSuppression.PHYSICAL.suppressesInnateTraits(),
                "a Gnomo passing for human is no less clever for it");

        assertTrue(RacialTraitSuppression.ALL.suppressesNaturalWeapons());
        assertTrue(RacialTraitSuppression.ALL.suppressesPhysicalTraits());
        assertTrue(RacialTraitSuppression.ALL.suppressesInnateTraits());
    }

    /** Two Talentos disagreeing, the stronger wins — and a {@code null} never drags a total down. */
    @Test
    void strongestFoldsSeveralOpinionsAndTreatsNullAsNone() {
        assertEquals(RacialTraitSuppression.ALL, RacialTraitSuppression.strongest(
                RacialTraitSuppression.PHYSICAL, RacialTraitSuppression.ALL));
        assertEquals(RacialTraitSuppression.ALL, RacialTraitSuppression.strongest(
                RacialTraitSuppression.ALL, RacialTraitSuppression.NATURAL_WEAPONS_ONLY));
        assertEquals(RacialTraitSuppression.PHYSICAL, RacialTraitSuppression.strongest(
                null, RacialTraitSuppression.PHYSICAL));
        assertEquals(RacialTraitSuppression.NONE, RacialTraitSuppression.strongest(null, null));
    }

    /** Out of any Forma, every Talento in the catalog suppresses nothing. */
    @Test
    void nothingIsSuppressedOutsideAForma() {
        Character character = guampo();
        character.grantFeat(FeericoFeat.ANCIENTEFORME);
        character.grantFeat(MonstruosoFeat.MIMETIZAR_FORMA_HUMANA);

        assertEquals(RacialTraitSuppression.NONE, sheetOf(character).getRacialTraitSuppression());
    }

    @Test
    void everyOtherTalentoInTheCatalogSuppressesNothing() {
        Character character = guampo();
        CharacterSheet sheet = sheetOf(character);
        sheet.enterForm(FormType.MONSTRUOSA);

        for (Feat feat : FeatCatalog.all()) {
            assertEquals(RacialTraitSuppression.NONE,
                    feat.resolveRacialTraitSuppression(character, sheet),
                    feat + " unexpectedly suppresses racial traits in an unrelated Forma");
        }
    }

    // ---------- The matrix, trait by trait ----------

    /** Armas Naturais go on every rung — the one trait even the narrowest clause reaches. */
    @Test
    void armasNaturaisGoOnEveryRung() {
        for (RacialTraitSuppression rung : RacialTraitSuppression.values()) {
            Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                    .race(new org.aventyrs.core.race.Vampiro(
                            org.aventyrs.core.race.Vampiro.VampiroLineage.NOSFERATU, new Human()))
                    .feats(new ArrayList<>()).build();
            CharacterSheet sheet = inFormSuppressing(character, rung);

            boolean keepsFangs = sheet.getNaturalWeapons().contains(NaturalWeapon.PRESAS_LONGAS);
            assertEquals(rung == RacialTraitSuppression.NONE, keepsFangs,
                    rung + " got the racial Armas Naturais wrong");
        }
    }

    /**
     * The anatomy pair — RC and Efeito Crítico immunities — survives
     * {@link RacialTraitSuppression#NATURAL_WEAPONS_ONLY} and goes from {@code PHYSICAL} up.
     * That boundary is the whole reason the narrow rung exists.
     */
    @Test
    void theAnatomyPairSurvivesTheNarrowRungAndGoesFromPhysicalUp() {
        assertAnatomy(RacialTraitSuppression.NONE, true);
        assertAnatomy(RacialTraitSuppression.NATURAL_WEAPONS_ONLY, true);
        assertAnatomy(RacialTraitSuppression.PHYSICAL, false);
        assertAnatomy(RacialTraitSuppression.ALL, false);
    }

    private void assertAnatomy(final RacialTraitSuppression rung, final boolean kept) {
        CharacterSheet sheet = inFormSuppressing(troll(), rung);

        assertEquals(kept ? CombatantSheet.CRITICAL_RESISTANCE_INSTANCE : 0,
                sheet.getTotalCriticalResistance(null), rung + " got the racial RC wrong");
        assertEquals(kept,
                sheet.getCriticalEffectImmunities().contains(CriticalEffectType.SANGRAMENTO),
                rung + " got the Efeito Crítico immunities wrong");
    }

    /**
     * Base Categoria de Tamanho, from {@code PHYSICAL} up. ⚠️ An <b>inference</b>: the clause
     * enumerates appendages and never stature — see {@link RacialTraitSuppression#PHYSICAL}. This
     * is where a rules clarification would land first.
     */
    @Test
    void theBaseSizeCategoryBecomesTheHumanBaselineFromPhysicalUp() {
        assertEquals(SizeCategory.PLUS_ONE,
                characterSizeService.getEffectiveSizeCategory(
                        inFormSuppressing(guampo(), RacialTraitSuppression.NATURAL_WEAPONS_ONLY)),
                "a snake-shaped Guampo is still a big Guampo");
        assertEquals(SizeCategory.ZERO,
                characterSizeService.getEffectiveSizeCategory(
                        inFormSuppressing(guampo(), RacialTraitSuppression.PHYSICAL)),
                "a human shape is human-sized");
    }

    /**
     * Racial Habilidades, {@code ALL} alone — read off {@code DamageService} rather than off the
     * ability list, so what is asserted is that the Habilidade stopped <em>working</em>. Vigor de
     * Épona's "reduzem em -1 todo dano sofrido" is a real {@code @Modifier}.
     */
    @Test
    void racialHabilidadesGoOnlyUnderAll() {
        assertEquals(1, damageService.getTotalDamageReduction(
                        inFormSuppressing(guampo(), RacialTraitSuppression.PHYSICAL),
                        (DamageType) null, null),
                "Vigor de Épona is not a body part");
        assertEquals(0, damageService.getTotalDamageReduction(
                        inFormSuppressing(guampo(), RacialTraitSuppression.ALL),
                        (DamageType) null, null),
                "the whole race falls silent");
    }

    /** And they leave the ability list too, which is the chokepoint every other consumer reads. */
    @Test
    void aSuppressedRacialHabilidadeLeavesTheAbilityList() {
        Character character = guampo();
        CharacterSheet suppressed = inFormSuppressing(character, RacialTraitSuppression.ALL);

        assertFalse(SkillCompetencyAbility.allFor(character, suppressed)
                        .contains(org.aventyrs.core.race.GuamposRacialAbility.VIGOR_DE_EPONA));
        assertTrue(SkillCompetencyAbility.allFor(character, null)
                        .contains(org.aventyrs.core.race.GuamposRacialAbility.VIGOR_DE_EPONA),
                "the sheet-less form cannot see a Forma and so never suppresses");
    }

    /** Racial Atributo bonuses, {@code ALL} alone, and only on a path that holds a sheet. */
    @Test
    void racialAttributeBonusesGoOnlyUnderAllAndOnlyWhereASheetIsInHand() {
        Character character = guampo();
        CharacterSheet suppressed = inFormSuppressing(character, RacialTraitSuppression.ALL);

        int withSheet = character.getEffectiveAttributeTotal(AttributeDomain.GNOSE, suppressed);
        int withoutSheet = character.getEffectiveAttributeTotal(AttributeDomain.GNOSE);

        assertEquals(withoutSheet - 1, withSheet, "the Guampo's racial +1 Gnose is gone");
        assertEquals(character.getAttributes().getAttribute(AttributeDomain.GNOSE).getTotal(),
                withoutSheet,
                "the sheet-less overload keeps it — PV/PM/Conjuração read this one, by design");
    }

    // ---------- What must never be suppressed ----------

    /**
     * Creature type, on every rung. {@code MIMETIZAR_FORMA_HUMANA}'s own Pré-requisito is
     * {@code requiredCreatureType(MONSTRUOSO|FEERICO)}, so suppressing it would make the Talento
     * retroactively ineligible for its own holder — looking human is not being human.
     */
    @Test
    void creatureTypeIsNeverSuppressed() {
        for (RacialTraitSuppression rung : RacialTraitSuppression.values()) {
            Character character = guampo();
            inFormSuppressing(character, rung);

            assertEquals(org.aventyrs.core.race.CreatureType.MONSTRUOSO,
                    character.getRace().getCreatureType(), rung + " must not touch creature type");
            assertTrue(character.getPrerequisiteCreatureTypes()
                            .contains(org.aventyrs.core.race.CreatureType.MONSTRUOSO),
                    rung + " must leave the Talento's own gate satisfiable");
        }
    }

    /**
     * A non-racial contributor to a suppressed stat survives even {@code ALL} — the likeliest bug
     * in the whole mechanism, since the obvious implementation zeroes the total rather than the
     * racial term. {@code AnaoFeat#VIGOR_DO_INVERNO} grants RC at combat start.
     */
    @Test
    void aTalentoGrantedContributionSurvivesFullSuppression() {
        Character character = troll();
        character.grantFeat(MonstruosoFeat.ANATOMIA_INCOMUM);
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet sheet = inFormSuppressing(character, RacialTraitSuppression.ALL);

        assertEquals(CombatantSheet.CRITICAL_RESISTANCE_INSTANCE,
                sheet.getTotalCriticalResistance(null),
                "the Talento's instance stays; only the Troll's own is silenced");
    }

    /** A Talento-granted Arma Natural is not racial either, and survives the same way. */
    @Test
    void aTalentoGrantedArmaNaturalSurvivesFullSuppression() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new org.aventyrs.core.race.NascidoDoDragao(
                        new Human(), org.aventyrs.core.magic.ElementalType.FOGO))
                .feats(new ArrayList<>()).build();
        character.grantFeat(new ArmamentoDraconicoFeat(java.util.EnumSet.of(
                NaturalWeapon.GARRAS_AFIADAS, NaturalWeapon.PRESAS_LONGAS)));
        CharacterSheet sheet = inFormSuppressing(character, RacialTraitSuppression.ALL);

        assertTrue(sheet.getNaturalWeapons().contains(NaturalWeapon.GARRAS_AFIADAS),
                "the claws came from a Talento, not from being a Nascido do Dragão");
    }
}
