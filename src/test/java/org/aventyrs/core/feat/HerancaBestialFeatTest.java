package org.aventyrs.core.feat;

import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ActiveAbilityService;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.AsphyxiaService;
import org.aventyrs.core.character.services.AsphyxiaServiceImpl;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.race.Bestial;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.attention.AttentionInteraction;
import org.aventyrs.core.title.santo.Santo;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Heranças Bestiais held the way a player holds them — as {@link HerancaBestialFeat}, carrying the
 * Perícia trait picked at acquisition (core 0.1.7.23).
 */
class HerancaBestialFeatTest {

    private static final List<Integer> DICE = List.of(3, 3, 3);

    private final FeatService featService = new FeatServiceImpl();
    private final AttentionInteraction attention = new AttentionInteraction();
    private final AsphyxiaService asphyxia = new AsphyxiaServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character bestial() {
        return CharacterFixture.blank(CharacterFixture.BLANK).race(new Bestial()).feats(new ArrayList<>()).build();
    }

    private static CharacterSheet funded(final Character character) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        return sheet;
    }

    /** heranca as a player acquires it: every choice answered with its first option. */
    private static HerancaBestialFeat acquired(final BestialFeat heranca, final Character holder) {
        return HerancaBestialFeat.of(heranca, heranca.resolveRequiredChoices(holder).stream()
                .map(choice -> (SkillTrait) choice.options().get(0))
                .toList());
    }

    private CharacterSheet holding(final BestialFeat heranca) throws IllegalOperationException {
        Character character = bestial();
        CharacterSheet sheet = funded(character);
        featService.grantFeat(character, sheet, acquired(heranca, character));
        return sheet;
    }

    private static List<BestialFeat> herancas() {
        return Arrays.stream(BestialFeat.values()).filter(feat -> feat.name().startsWith("HERANCA_")).toList();
    }

    @Nested
    class Acquisition {

        @Test
        void everyHerancaDeclaresItsTraitAndRefusesTheBareConstant() {
            for (BestialFeat heranca : herancas()) {
                Character character = bestial();
                assertFalse(heranca.resolveRequiredChoices(character).isEmpty(), heranca.name());
                IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                        () -> featService.grantFeat(character, funded(character), heranca), heranca.name());
                assertEquals(TranslatableMessages.FEAT_REQUIRES_CHOICE, refused.getMessage());
            }
        }

        @Test
        void eachChoiceOffersOnlyTheNamedPericiasTraitsOfTheNamedKind() {
            Character character = bestial();
            List<FeatChoice<?>> bovidea = BestialFeat.HERANCA_BOVIDEA.resolveRequiredChoices(character);
            assertEquals(1, bovidea.size());
            assertTrue(bovidea.get(0).options().stream().allMatch(option -> option instanceof SkillCompetencyAbility
                    && ((SkillTrait) option).getSkillType() == SkillType.ATLETISMO));

            List<FeatChoice<?>> reptiliana = BestialFeat.HERANCA_REPTILIANA.resolveRequiredChoices(character);
            assertTrue(reptiliana.get(0).options().stream().allMatch(option -> option instanceof SkillSpecialization
                    && ((SkillTrait) option).getSkillType() == SkillType.ATLETISMO));
        }

        @Test
        void caninaGrantsAnEspecializacaoAndACompetenciaDeAtencao() throws IllegalOperationException {
            CharacterSheet sheet = holding(BestialFeat.HERANCA_CANINA);
            Character character = sheet.getCharacter();
            HerancaBestialFeat held = (HerancaBestialFeat) character.getFeats().get(0);

            assertEquals(2, held.getChosenTraits().size());
            assertInstanceOf(SkillSpecialization.class, held.getChosenTraits().get(0));
            assertInstanceOf(SkillCompetencyAbility.class, held.getChosenTraits().get(1));
            assertTrue(character.getSpecializations(SkillType.ATTENTION).contains(held.getChosenTraits().get(0)));
            assertTrue(SkillCompetencyAbility.allFor(character).contains(held.getChosenTraits().get(1)));
        }

        @Test
        void aTraitAlreadyGrantedIsNotOfferedAgain() throws IllegalOperationException {
            CharacterSheet sheet = holding(BestialFeat.HERANCA_ANFIBIA);
            SkillTrait first = ((HerancaBestialFeat) sheet.getCharacter().getFeats().get(0)).getChosenTrait();

            // Felina names Furtividade's Habilidades de Competência too.
            assertFalse(BestialFeat.HERANCA_FELINA.resolveRequiredChoices(sheet.getCharacter()).get(0)
                    .options().contains(first));
        }

        @Test
        void theAtributoBonusAndArmaNaturalSurviveTheAcquiredForm() throws IllegalOperationException {
            CharacterSheet sheet = holding(BestialFeat.HERANCA_BOVIDEA);
            Character character = sheet.getCharacter();
            Character plain = bestial();

            assertEquals(plain.getEffectiveAttributeTotal(AttributeDomain.STRENGTH) + 1,
                    character.getEffectiveAttributeTotal(AttributeDomain.STRENGTH));
            assertTrue(character.getNaturalWeapons().contains(org.aventyrs.core.item.NaturalWeapon.CHIFRES_PODEROSOS));
        }
    }

    /**
     * The acquired form replaces the constant in {@code getFeats()}, so a clause it does not forward is silently lost —
     * Anfíbia's grapple Vantagem was, until 0.1.7.23. Every hook a Herança overrides must be overridden here too, bar
     * the two asked only of the activated constant a roll names and the choice itself.
     */
    @Test
    void everyHookAHerancaOverridesIsForwarded() {
        Set<String> askedOfTheConstant = Set.of("resolveRequiredChoices", "permitsActivation",
                "resolveActivationDeterminationCost");
        for (BestialFeat heranca : herancas()) {
            for (Method method : heranca.getClass().getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.isSynthetic()
                        || askedOfTheConstant.contains(method.getName())) {
                    continue;
                }
                try {
                    Method forwarded = HerancaBestialFeat.class.getMethod(method.getName(), method.getParameterTypes());
                    assertEquals(HerancaBestialFeat.class, forwarded.getDeclaringClass(),
                            heranca.name() + "." + method.getName() + " is not forwarded");
                } catch (NoSuchMethodException e) {
                    throw new AssertionError(heranca.name() + "." + method.getName() + " has no counterpart", e);
                }
            }
        }
    }

    @Nested
    class Clauses {

        @Test
        void anfibiasGrappleVantagemReachesTheAcquiredForm() throws IllegalOperationException {
            CharacterSheet sheet = holding(BestialFeat.HERANCA_ANFIBIA);
            Feat held = sheet.getCharacter().getFeats().get(0);
            SkillRoll escape = new SkillRoll(DICE, null, null, null, Manoeuvre.LIBERTAR_SE_DO_AGARRAO, Set.of());

            assertEquals(Skill.ADVANTAGE_BONUS, held.resolveSkillRollBonus(SkillType.ATLETISMO, null, null,
                    sheet.getCharacter(), null, sheet, escape));
            assertEquals(0, held.resolveSkillRollBonus(SkillType.ATLETISMO, null, null,
                    sheet.getCharacter(), null, sheet, new SkillRoll(DICE)));
        }

        @Test
        void faroApuradoIsAVantagemOnAtencao() throws IllegalOperationException {
            CharacterSheet canine = holding(BestialFeat.HERANCA_CANINA);
            CharacterSheet plain = CharacterSheet.of(bestial(), new Player());

            int with = attention.applyTo(canine, null, new SkillRoll(DICE)).getSkillRollBonus();
            int without = attention.applyTo(plain, null, new SkillRoll(DICE)).getSkillRollBonus();
            // The Canina's own Atenção Especialização/Competência may add more; the Vantagem is at least there.
            assertTrue(with - without >= Skill.ADVANTAGE_BONUS, with + " vs " + without);
        }

        @Test
        void ecolocalizacaoTakesOneNivelOffAnAtencaoRollForTwoPd() throws IllegalOperationException {
            CharacterSheet cetacean = holding(BestialFeat.HERANCA_CETACEA);
            SkillRoll echo = new SkillRoll(DICE, null, null, null, null, Set.of(BestialFeat.HERANCA_CETACEA));

            InteractionResult activated = attention.applyTo(cetacean, null, echo);
            InteractionResult plain = attention.applyTo(cetacean, null, new SkillRoll(DICE));

            assertEquals(plain.getDifficultyReduction() + 1, activated.getDifficultyReduction());
            assertEquals(2, activated.getActivationDeterminationCost());
            assertNull(plain.getActivationDeterminationCost());
        }

        @Test
        void ecolocalizacaoIsRefusedOnAnotherPericiaAndWithoutTheHeranca() throws IllegalOperationException {
            CharacterSheet cetacean = holding(BestialFeat.HERANCA_CETACEA);
            SkillRoll echo = new SkillRoll(DICE, null, null, null, null, Set.of(BestialFeat.HERANCA_CETACEA));

            IllegalOperationException elsewhere = assertThrows(IllegalOperationException.class,
                    () -> SkillType.FURTIVIDADE.newInteraction().applyTo(cetacean, null, echo));
            assertEquals(TranslatableMessages.FEAT_ACTIVATION_NOT_PERMITTED, elsewhere.getMessage());

            CharacterSheet stranger = CharacterSheet.of(bestial(), new Player());
            IllegalOperationException notHeld = assertThrows(IllegalOperationException.class,
                    () -> attention.applyTo(stranger, null, echo));
            assertEquals(TranslatableMessages.ACTIVATED_FEAT_NOT_HELD, notHeld.getMessage());
        }
    }

    @Nested
    class Breathing {

        @Test
        void anfibiaBreathesUnderWaterButNotInAGas() throws IllegalOperationException {
            CharacterSheet amphibian = holding(BestialFeat.HERANCA_ANFIBIA);

            asphyxia.stopBreathing(amphibian, true);
            assertFalse(asphyxia.isBreathless(amphibian));

            asphyxia.stopBreathing(amphibian);
            assertTrue(asphyxia.isBreathless(amphibian));
        }

        @Test
        void cetaceaHoldsItsBreathTwiceAsLongUnderWaterOnly() throws IllegalOperationException {
            CharacterSheet cetacean = holding(BestialFeat.HERANCA_CETACEA);
            int vigor = Math.max(1, cetacean.getAttributeTotal(AttributeDomain.VIGOR));

            asphyxia.stopBreathing(cetacean, true);
            assertEquals(2 * vigor, asphyxia.getRoundsPerInstance(cetacean));

            asphyxia.breathe(cetacean);
            asphyxia.stopBreathing(cetacean, false);
            assertEquals(vigor, asphyxia.getRoundsPerInstance(cetacean));
        }

        @Test
        void anyoneElseDrownsAsBefore() {
            CharacterSheet plain = CharacterSheet.of(bestial(), new Player());

            asphyxia.stopBreathing(plain, true);
            assertTrue(asphyxia.isBreathless(plain));
            assertEquals(Math.max(1, plain.getAttributeTotal(AttributeDomain.VIGOR)),
                    asphyxia.getRoundsPerInstance(plain));
        }
    }

    @Test
    void metamorfoseSelvagemIsRefusedWithoutAHeranca() {
        Character character = bestial();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        character.grantFeat(BestialFeat.METAMORFOSE_SELVAGEM);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        ActiveAbilityService abilities = new ActiveAbilityServiceImpl();

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> abilities.activate(character, sheet, character.getActiveAbilities().stream()
                        .filter(MetamorfoseSelvagemActiveAbility.class::isInstance)
                        .findFirst().orElseThrow(), 1));
        assertEquals(TranslatableMessages.ACTIVE_ABILITY_CONDITION_NOT_MET, refused.getMessage());
    }
}
