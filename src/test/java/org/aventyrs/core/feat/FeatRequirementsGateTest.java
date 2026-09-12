package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import java.math.BigDecimal;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Alignment;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.race.Anao;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Elfo;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.TitleArchetype;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four prerequisite clauses added for the Talento catalog — the Aventyr-tier title count,
 * its optional {@link TitleArchetype} narrowing, the Race restriction, and the
 * count-of-category clause.
 *
 * <p>Exercised through {@link AbstractFeat} rather than a catalog constant on purpose: these
 * pin {@link Feat#isEligible}'s own gate arithmetic, which is shared by every Talento, the same
 * layer {@code MetamagicoFeatTest} occupies for formulas. What a specific Talento *does* once
 * acquired is tested against its consuming service instead — see the {@code testing-a-feat}
 * skill and {@link MetamagicoFeatIntegrationTest}.
 */
class FeatRequirementsGateTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** A Título Aventyr of an archetype other than Santo's, so the archetype filter has two sides. */
    private record BrutoTitle() implements AventyrTitle {
        @Override public String getName() { return "Bruto de Teste"; }
        @Override public TitleArchetype getArchetype() { return TitleArchetype.BRUTO; }
        @Override public String getBaseEffectDescription() { return ""; }
        @Override public List<AventyrTitleSpecialization> getSpecializations() { return List.of(); }
        @Override public List<AventyrTitleAbility> getAbilities() { return List.of(); }
        @Override public void grantAbility(final AventyrTitleAbility ability) { }
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    private static Feat featRequiring(final FeatRequirements requirements) {
        return new AbstractFeat(FeatCategory.DESTINO, "test", requirements);
    }

    private static Feat ofCategory(final FeatCategory category) {
        return new AbstractFeat(category, "test", FeatRequirements.builder().build());
    }

    // ---- requiredAwakenedTitles -------------------------------------------------------------

    @Test
    void aTalentoNamingNoAventyrTierIsEligibleWithNoTitulosHeld() {
        assertTrue(featRequiring(FeatRequirements.builder().build()).isEligible(character().build()));
    }

    @Test
    void oneAwakenedTituloIsNotEnoughForATalentoDemandingTwo() {
        Character character = character().build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        Feat feat = featRequiring(FeatRequirements.builder().requiredAwakenedTitles(2).build());

        assertFalse(feat.isEligible(character));

        character.grantTitle(new BrutoTitle(), TitleSlot.SECONDARY);

        assertTrue(feat.isEligible(character));
    }

    @Test
    void aTituloIsDespertoSimplyByFillingASlotWithNoSpecializationsOrSupremas() {
        Character character = character().build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertTrue(featRequiring(FeatRequirements.builder().requiredAwakenedTitles(1).build())
                .isEligible(character));
    }

    // ---- requiredTitleArchetype -------------------------------------------------------------

    @Test
    void anArchetypeGateCountsOnlyTitulosOfThatArchetype() {
        Character character = character().build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        Feat centelhaBruta = featRequiring(FeatRequirements.builder()
                .requiredAwakenedTitles(1)
                .requiredTitleArchetype(TitleArchetype.BRUTO)
                .build());

        assertFalse(centelhaBruta.isEligible(character), "Santo is ABENCOADO, not BRUTO");

        character.grantTitle(new BrutoTitle(), TitleSlot.SECONDARY);

        assertTrue(centelhaBruta.isEligible(character));
    }

    @Test
    void santoSatisfiesAnAbencoadoArchetypeGate() {
        Character character = character().build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertTrue(featRequiring(FeatRequirements.builder()
                .requiredAwakenedTitles(1)
                .requiredTitleArchetype(TitleArchetype.ABENCOADO)
                .build()).isEligible(character));
    }

    // ---- requiredRace ------------------------------------------------------------------------

    @Test
    void aRaceGateRefusesAnyOtherRace() {
        Feat anaoOnly = featRequiring(FeatRequirements.builder().requiredRace(Anao.class).build());

        assertTrue(anaoOnly.isEligible(character().race(new Anao()).build()));
        assertFalse(anaoOnly.isEligible(character().race(new Human()).build()));
    }

    // ---- requiredFeatCategory / requiredFeatCategoryCount -------------------------------------

    @Test
    void aCountOfCategoryGateCountsOnlyTalentosOfThatCategory() {
        Character character = character().build();
        character.grantFeat(ofCategory(FeatCategory.DESTINO));
        character.grantFeat(ofCategory(FeatCategory.ARTILHARIA));

        Feat needsTwoDestino = featRequiring(FeatRequirements.builder()
                .requiredFeatCategory(FeatCategory.DESTINO)
                .requiredFeatCategoryCount(2)
                .build());

        assertFalse(needsTwoDestino.isEligible(character), "only one held Talento is DESTINO");

        character.grantFeat(ofCategory(FeatCategory.DESTINO));

        assertTrue(needsTwoDestino.isEligible(character));
    }

    // ---- combination -------------------------------------------------------------------------

    @Test
    void everySetClauseMustHoldAtOnce() {
        Feat feat = featRequiring(FeatRequirements.builder()
                .requiredAwakenedTitles(1)
                .requiredRace(Anao.class)
                .build());

        Character anaoWithoutTitulo = character().race(new Anao()).build();
        assertFalse(feat.isEligible(anaoWithoutTitulo));

        Character humanWithTitulo = character().race(new Human()).build();
        humanWithTitulo.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        assertFalse(feat.isEligible(humanWithTitulo));

        Character anaoWithTitulo = character().race(new Anao()).build();
        anaoWithTitulo.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        assertTrue(feat.isEligible(anaoWithTitulo));
    }

    @Test
    void almaFeericaAddsFeericoOnlyToTheHoldersPrerequisiteTypes() {
        Character elf = character().race(new Elfo()).build();
        Feat feericoOnly = featRequiring(FeatRequirements.builder()
                .requiredCreatureType(CreatureType.FEERICO)
                .build());

        assertFalse(feericoOnly.isEligible(elf));

        elf.grantFeat(ElficoFeat.ALMA_FEERICA);

        assertTrue(feericoOnly.isEligible(elf));
        assertEquals(CreatureType.HUMANOIDE, elf.getRace().getCreatureType());
    }

    @Test
    void corruptorRequiresANeutralOrEvilAlignment() {
        Character goodElf = character().race(new Elfo()).alignment(Alignment.GOOD).build();
        goodElf.grantFeat(ElficoFeat.GUARDIAO_DOS_BOSQUES);
        Character neutralElf = character().race(new Elfo()).alignment(Alignment.NEUTRAL).build();
        neutralElf.grantFeat(ElficoFeat.GUARDIAO_DOS_BOSQUES);

        assertFalse(ElficoFeat.CORRUPTOR_SOMBRIO.isEligible(goodElf));
        assertTrue(ElficoFeat.CORRUPTOR_SOMBRIO.isEligible(neutralElf));
    }

    // ---------- The clause shapes added for the disjunctions/exclusions batch ----------

    /**
     * {@code anyOf} is a disjunction hung off the AND group: every clause on the outer record
     * still has to hold, and then <em>at least one</em> branch must too. Clauses common to both
     * branches stay outside rather than being repeated in each.
     */
    @Test
    void anyOfNeedsOneBranchOnTopOfEveryOuterClause() {
        Feat feat = featRequiring(FeatRequirements.builder()
                .requiredAwakenedTitles(1)
                .alternative(FeatRequirements.builder()
                        .attributeDomain(AttributeDomain.GNOSE).requiredAttributeValue(3).build())
                .alternative(FeatRequirements.builder()
                        .attributeDomain(AttributeDomain.FOCUS).requiredAttributeValue(3).build())
                .build());

        assertFalse(feat.isEligible(withAttribute(AttributeDomain.GNOSE, 3)), "outer clause unmet");
        assertFalse(feat.isEligible(titled(withAttribute(AttributeDomain.VIGOR, 5))), "no branch met");
        assertTrue(feat.isEligible(titled(withAttribute(AttributeDomain.GNOSE, 3))));
        assertTrue(feat.isEligible(titled(withAttribute(AttributeDomain.FOCUS, 3))), "either branch does");
    }

    /** An empty {@code anyOf} never blocks — the default every Talento without a disjunction keeps. */
    @Test
    void noDisjunctionMeansNoExtraGate() {
        assertTrue(featRequiring(FeatRequirements.builder().build()).isEligible(character().build()));
    }

    /** A maximum is its own clause, so a Talento can name a floor on one Atributo and a ceiling on another. */
    @Test
    void anAttributeMaximumCapsRatherThanFloors() {
        Feat feat = featRequiring(FeatRequirements.builder()
                .maximumAttributeDomain(AttributeDomain.STRENGTH)
                .maximumAttributeValue(2)
                .build());

        assertTrue(feat.isEligible(withAttribute(AttributeDomain.STRENGTH, 2)), "inclusive");
        assertFalse(feat.isEligible(withAttribute(AttributeDomain.STRENGTH, 3)));
    }

    /** Iniciativa is an Ego, not an Atributo, so its ceiling reads {@code EgoValue#getBase()}. */
    @Test
    void anEgoMaximumCapsTheInvestedBase() {
        Feat feat = featRequiring(FeatRequirements.builder()
                .maximumEgoDomain(EgoDomain.INICIATIVA)
                .maximumEgoValue(2)
                .build());

        assertTrue(feat.isEligible(withEgo(EgoDomain.INICIATIVA, 2)));
        assertFalse(feat.isEligible(withEgo(EgoDomain.INICIATIVA, 3)));
    }

    /** "Atributo 3 ou Superior" names no domain, so any one reaching it opens the gate. */
    @Test
    void anyAttributeAtAValueAcceptsWhicheverDomainReachesIt() {
        Feat feat = featRequiring(FeatRequirements.builder().requiredAnyAttributeValue(3).build());

        assertFalse(feat.isEligible(character().build()));
        assertTrue(feat.isEligible(withAttribute(AttributeDomain.CHARISMA, 3)));
        assertTrue(feat.isEligible(withAttribute(AttributeDomain.VIGOR, 3)));
    }

    /**
     * The narrower form additionally demands the Atributo actually carry a Bônus Racial — a Base
     * of 5 with no racial bonus does not satisfy it.
     */
    @Test
    void anyRacialAttributeAtAValueAlsoDemandsTheRacialBonus() {
        Feat feat = featRequiring(FeatRequirements.builder().requiredAnyRacialAttributeValue(5).build());

        assertFalse(feat.isEligible(withAttribute(AttributeDomain.STRENGTH, 5)));
        assertTrue(feat.isEligible(character()
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder()
                                .domain(AttributeDomain.STRENGTH).base(5).fixedRacialBonus(1).build())
                        .build())
                .build()));
    }

    /** The mirror of {@code requiredRace}, {@code isInstance} and all. */
    @Test
    void aForbiddenRaceRefusesThatRaceAndNobodyElse() {
        Feat feat = featRequiring(FeatRequirements.builder().forbiddenRace(Human.class).build());

        assertFalse(feat.isEligible(character().race(new Human()).build()));
        assertTrue(feat.isEligible(character().race(new Elfo()).build()));
    }

    /** An exclusion is possession-only, and compares through {@code catalogEntry()} like its positive twin. */
    @Test
    void aForbiddenFeatShutsTheGateOnceItIsHeld() {
        Feat feat = featRequiring(FeatRequirements.builder()
                .forbiddenFeat(ElficoFeat.GUARDIAO_DOS_BOSQUES)
                .build());
        Character elf = character().race(new Elfo()).build();

        assertTrue(feat.isEligible(elf));

        elf.grantFeat(ElficoFeat.GUARDIAO_DOS_BOSQUES);

        assertFalse(feat.isEligible(elf));
    }

    /**
     * The two {@code CharacterSheet}-side clauses are <b>skipped</b> without a sheet, not failed:
     * a sheet-less preview stays looser than the real gate, never stricter.
     */
    @Test
    void sheetSideClausesAreSkippedWithoutASheetAndEnforcedWithOne() {
        Feat feat = featRequiring(FeatRequirements.builder()
                .requiredFame(15)
                .requiredTotalExperience(BigDecimal.valueOf(30))
                .build());
        Character character = character().build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertTrue(feat.isEligible(character), "no sheet: skipped");
        assertFalse(feat.isEligible(character, sheet));

        sheet.increaseFamaPositiva(15);
        assertFalse(feat.isEligible(character, sheet), "EXP still short");

        sheet.accumulateExperience(BigDecimal.valueOf(30));
        assertTrue(feat.isEligible(character, sheet));
    }

    /** Fama reads either column — the rules say plain "Fama", this core splits it in two. */
    @Test
    void fameIsSatisfiedByEitherRenownOrNotoriety() {
        Feat feat = featRequiring(FeatRequirements.builder().requiredFame(15).build());
        Character character = character().build();
        CharacterSheet infamous = CharacterSheet.of(character, new Player());
        infamous.increaseFamaNegativa(15);

        assertTrue(feat.isEligible(character, infamous));
    }

    private static Character withAttribute(final AttributeDomain domain, final int base) {
        return character()
                .attributes(CharacterAttributes.of(java.util.Map.of(domain, base)))
                .build();
    }

    private static Character withEgo(final EgoDomain domain, final int base) {
        CharacterEgos.CharacterEgosBuilder egos = CharacterEgos.builder();
        if (domain == EgoDomain.INICIATIVA) {
            egos.iniciativa(EgoValue.builder().base(base).build());
        }
        return character().egos(egos.build()).build();
    }

    private static Character titled(final Character character) {
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        return character;
    }
}
