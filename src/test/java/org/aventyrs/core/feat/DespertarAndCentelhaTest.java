package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.PDCost;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ACTIVATION_OPT_IN_EXHAUSTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase H: the Despertar and Centelha Talentos of the Destino tree. */
class DespertarAndCentelhaTest {

    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheetWith(final int totalExperience, final Feat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>(List.of(feats)))
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(totalExperience));
        return sheet;
    }

    private static void awaken(final CharacterSheet sheet, final TitleSlot slot) {
        sheet.getCharacter().grantTitle(new Santo(List.of(), List.of()), slot);
    }

    private static int figure(final CharacterSheet sheet) {
        return DestinoFeat.ATRASAR_DESPERTAR.resolveSkillRollBonus(SkillType.ATTENTION, null, null,
                sheet.getCharacter(), null, sheet);
    }

    // ---------- Atrasar Despertar / Abdicador ----------

    @Test
    void atrasarDespertarMustBeTakenBeforeAnyTituloAwakens() {
        CharacterSheet sheet = sheetWith(0);
        assertTrue(DestinoFeat.ATRASAR_DESPERTAR.isEligible(sheet.getCharacter(), sheet));

        awaken(sheet, TitleSlot.PRIMARY);
        assertFalse(DestinoFeat.ATRASAR_DESPERTAR.isEligible(sheet.getCharacter(), sheet));
    }

    @Test
    void itsFigureGrowsWithExperienceAndEachUnawakenedTitulo() {
        CharacterSheet sheet = sheetWith(30, DestinoFeat.ATRASAR_DESPERTAR);
        assertEquals(3 * 3, figure(sheet));

        awaken(sheet, TitleSlot.PRIMARY);
        assertEquals(3 * 2, figure(sheet));
        assertEquals(6, DestinoFeat.ATRASAR_DESPERTAR.resolveDamageTakenReduction(sheet.getCharacter(), sheet));
        assertEquals(6, DestinoFeat.ATRASAR_DESPERTAR.resolveCriticalMarginIncrease(SkillType.ATTENTION, null,
                sheet.getCharacter(), null, sheet));
        assertEquals(6, DestinoFeat.ATRASAR_DESPERTAR.resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, null, null,
                sheet.getCharacter(), null, 1, sheet).orElseThrow().getValue());
    }

    @Test
    void withoutASheetItGrantsNothing() {
        CharacterSheet sheet = sheetWith(30, DestinoFeat.ATRASAR_DESPERTAR);
        assertEquals(0, DestinoFeat.ATRASAR_DESPERTAR.resolveSkillRollBonus(SkillType.ATTENTION, null, null,
                sheet.getCharacter(), null, null));
    }

    @Test
    void abdicadorDoublesIt() {
        CharacterSheet sheet = sheetWith(0, DestinoFeat.ATRASAR_DESPERTAR, DestinoFeat.ABDICADOR);
        assertEquals(2 * 3, figure(sheet));
    }

    // ---------- Fragmento da Encarnação de Gilgamesh ----------

    @Test
    void gilgameshCountsCentelhasLostAndThoseWithheldByAtrasar() {
        CharacterSheet plain = sheetWith(0, DestinoFeat.FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH);
        Character character = plain.getCharacter();
        assertEquals(0, DestinoFeat.FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH.resolveAttributeBonus(AttributeDomain.VIGOR, character));

        character.sacrificeCentelhas(1);
        assertEquals(2, DestinoFeat.FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH.resolveAttributeBonus(AttributeDomain.VIGOR, character));

        CharacterSheet delaying = sheetWith(0, DestinoFeat.ATRASAR_DESPERTAR, DestinoFeat.FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH);
        awaken(delaying, TitleSlot.PRIMARY);
        assertEquals(2 * 2, DestinoFeat.FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH
                .resolveAttributeBonus(AttributeDomain.FOCUS, delaying.getCharacter()));
    }

    @Test
    void gilgameshNeedsAtrasarOrAMissingCentelha() {
        CharacterSheet sheet = sheetWith(0);
        Character character = sheet.getCharacter();
        assertFalse(DestinoFeat.FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH.isEligible(character, sheet));

        character.sacrificeCentelhas(1);
        assertTrue(DestinoFeat.FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH.isEligible(character, sheet));

        CharacterSheet delaying = sheetWith(0, DestinoFeat.ATRASAR_DESPERTAR);
        assertTrue(DestinoFeat.FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH.isEligible(delaying.getCharacter(), delaying));
    }

    // ---------- Centelha Gran-Aventyr Antecipada ----------

    @Test
    void theSecundarioAwakensAtTwentyThreeExperience() throws IllegalOperationException {
        CharacterSheet sheet = sheetWith(20);
        awaken(sheet, TitleSlot.PRIMARY);
        Character character = sheet.getCharacter();
        var chosen = CentelhaGranAventyrAntecipadaFeat.optionsFor(character).get(0);
        character.grantFeat(CentelhaGranAventyrAntecipadaFeat.of(character, chosen));

        assertTrue(sheet.applySessionEndAcquisitions().isEmpty());
        assertNull(character.getSecondaryTitle());

        sheet.accumulateExperience(BigDecimal.valueOf(3));
        assertEquals(1, sheet.applySessionEndAcquisitions().size());
        assertEquals(chosen.getClass(), character.getSecondaryTitle().getClass());
        assertTrue(sheet.applySessionEndAcquisitions().isEmpty());
    }

    // ---------- Acelerar Habilidade / Centelha Duradoura ----------

    private static final class TestInteraction extends AbstractTitleAbilityInteraction {
        TestInteraction() {
            super(new AventyrTitleAbility() {
                @Override
                public String getDescription() {
                    return "test";
                }

                @Override
                public PDCost getPDCost() {
                    return PDCost.fixed(1);
                }

                @Override
                public org.aventyrs.core.sheet.ActionCost getActionPointCost() {
                    return org.aventyrs.core.sheet.ActionCost.ofActionPoints(3);
                }

                @Override
                public Optional<Class<? extends Interaction>> getInteractionClass() {
                    return Optional.empty();
                }
            });
        }

        @Override
        protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
            return InteractionResult.builder().build();
        }
    }

    private int currentPd(final CharacterSheet sheet) {
        return determinationPointsService.getCurrentDeterminationPoints(sheet.getCharacter(), sheet);
    }

    @Test
    void acelerarHabilidadeTradesAPaForTwoPdOncePerTurn() {
        CharacterSheet sheet = sheetWith(0, DestinoFeat.ACELERAR_HABILIDADE);
        TestInteraction interaction = new TestInteraction();
        int before = currentPd(sheet);

        InteractionResult result = interaction.activate(TitleAbilityActivationRequest.builder()
                .activator(sheet).activatedFeat(DestinoFeat.ACELERAR_HABILIDADE).build());

        assertEquals(before - 3, currentPd(sheet));
        assertEquals(2, result.getActionPointCost().actionPoints());
        IllegalOperationException again = assertThrows(IllegalOperationException.class, () -> interaction.activate(
                TitleAbilityActivationRequest.builder().activator(sheet).activatedFeat(DestinoFeat.ACELERAR_HABILIDADE).build()));
        assertEquals(TITLE_ACTIVATION_OPT_IN_EXHAUSTED, again.getMessage());
    }

    @Test
    void centelhaDuradouraReportsTwoMoreUnidades() {
        CharacterSheet sheet = sheetWith(0, DestinoFeat.CENTELHA_DURADOURA);
        int before = currentPd(sheet);

        InteractionResult result = new TestInteraction().activate(TitleAbilityActivationRequest.builder()
                .activator(sheet).activatedFeat(DestinoFeat.CENTELHA_DURADOURA).build());

        assertEquals(before - 3, currentPd(sheet));
        assertEquals(2, result.getTitleAbilityDurationIncrease());
    }

    @Test
    void aTalentoNotOptedIntoChangesNothing() {
        CharacterSheet sheet = sheetWith(0, DestinoFeat.CENTELHA_DURADOURA);
        int before = currentPd(sheet);

        InteractionResult result = new TestInteraction().activate(TitleAbilityActivationRequest.builder().activator(sheet).build());

        assertEquals(before - 1, currentPd(sheet));
        assertNull(result.getTitleAbilityDurationIncrease());
    }
}
