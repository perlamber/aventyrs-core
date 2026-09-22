package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.REQUIRED_TITLE_TRAIT_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_NOT_ACTIVATABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** {@code AventyrTitle#activateAbility}, exercised through a real {@link Santo}. */
class TitleAbilityActivationEntryPointTest {

    private CharacterSheet actor;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        actor = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    private TitleAbilityActivationRequest selfRequest() {
        return TitleAbilityActivationRequest.builder().activator(actor).build();
    }

    @Test
    void anAbilityThatIsNotHeldIsRefused() {
        Santo santo = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of());

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> santo.activateAbility(AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO, selfRequest()));

        assertEquals(REQUIRED_TITLE_TRAIT_NOT_HELD, refused.getMessage());
    }

    @Test
    void aHeldAbilityWithNoActivationIsRefused() {
        Santo santo = new Santo(List.of(), List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS));

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> santo.activateAbility(SantoAbility.BASTIAO_DOS_NECESSITADOS, selfRequest()));

        assertEquals(TITLE_ABILITY_NOT_ACTIVATABLE, refused.getMessage());
    }

    @Test
    void aHeldHabilidadeRunsItsOwnInteraction() {
        Santo santo = new Santo(List.of(), List.of(AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO));

        InteractionResult result = santo.activateAbility(AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO, selfRequest());

        assertEquals(3, result.getBlessings().size());
        assertEquals(2, result.getDeterminationPointsSpent());
    }

    /**
     * The Especialização is PV-priced under V19, and its heal counts the toucher's own Habilidades
     * — so the Título has to be on the activator's Character, not merely in a local variable.
     */
    @Test
    void aHeldEspecializacaoRunsItsOwnInteraction() {
        Santo santo = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of());
        actor.getCharacter().grantTitle(santo, TitleSlot.PRIMARY);
        actor.applyDamage(8);

        InteractionResult result = santo.activateAbility(SantoSpecialization.ABENCOADO_PELA_LUZ,
                TitleAbilityActivationRequest.builder()
                        .activator(actor)
                        .choice(AbencoadoPelaLuzInteraction.Branch.HEAL)
                        .build());

        assertEquals(0, result.getDeterminationPointsSpent());
        assertEquals(AbencoadoPelaLuzInteraction.TOUCH_HIT_POINT_COST, result.getResourceLossValue());
        assertEquals(SantoSpecialization.BASE_TOUCH_HEAL, result.getResourceGainValue());
        assertEquals(8 + AbencoadoPelaLuzInteraction.TOUCH_HIT_POINT_COST - result.getResourceGainValue(),
                actor.getDamageTaken());
    }
}
