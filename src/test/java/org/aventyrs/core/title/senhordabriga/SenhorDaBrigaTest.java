package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.title.TitleArchetype;
import org.aventyrs.core.title.santo.SantoSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_LOCKED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_PREREQUISITE_NOT_MET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenhorDaBrigaTest {

    @BeforeEach
    void setup() {
        SenhorDaBrigaFixtures.loadTemplates();
    }

    @Test
    void identity() {
        SenhorDaBriga title = new SenhorDaBriga(List.of(), List.of());

        assertEquals("Senhor da Briga", title.getName());
        assertEquals(TitleArchetype.BRUTO, title.getArchetype());
        assertFalse(title.getBaseEffectDescription().isBlank());
        assertTrue(title.getPrimaryTitleBonusDescription().contains("Título Primário"));
    }

    @Test
    void grantedTraitsLandInTheirOwnLists() {
        SenhorDaBriga title = new SenhorDaBriga(List.of(), List.of());

        title.grantSpecialization(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL);
        title.grantAbility(PunhoInigualavelAbility.AGARRAR_E_DERRUBAR);

        assertEquals(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL), title.getSpecializations());
        assertEquals(List.of(PunhoInigualavelAbility.AGARRAR_E_DERRUBAR), title.getAbilities());
        assertTrue(title.holds(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL));
    }

    @Test
    void anotherTitlesEspecializacaoIsRefused() {
        SenhorDaBriga title = new SenhorDaBriga(List.of(), List.of());

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> title.grantSpecialization(SantoSpecialization.ABENCOADO_PELA_LUZ));
        assertEquals(TITLE_ABILITY_PREREQUISITE_NOT_MET, refused.getMessage());
    }

    @Test
    void theElementIsChosenOnceAndCannotChange() {
        SenhorDaBriga title = new SenhorDaBriga(List.of(), List.of());

        title.chooseImpactoElementalElement(ElementalType.FOGO);
        title.chooseImpactoElementalElement(ElementalType.FOGO);

        assertEquals(Optional.of(ElementalType.FOGO), title.getImpactoElementalElement());
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> title.chooseImpactoElementalElement(ElementalType.GELO));
        assertEquals(TITLE_ABILITY_CHOICE_LOCKED, refused.getMessage());
    }

    @Test
    void todosIsNotAnElementOnOffer() {
        SenhorDaBriga title = new SenhorDaBriga(List.of(), List.of());

        assertThrows(IllegalOperationException.class, () -> title.chooseImpactoElementalElement(ElementalType.TODOS));
        assertTrue(SenhorDaBriga.IMPACTO_ELEMENTAL_ELEMENTS.contains(ElementalType.VENTO));
    }

    @Test
    void aPersistedElementIsRestoredByTheConstructor() {
        SenhorDaBriga title = new SenhorDaBriga(List.of(), List.of(), ElementalType.ELETRICIDADE);

        assertEquals(Optional.of(ElementalType.ELETRICIDADE), title.getImpactoElementalElement());
    }

    @Test
    void impactoElementalLastsTwoPlusTheOtherPunhoInigualavelTraits() {
        SenhorDaBriga alone = new SenhorDaBriga(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL),
                List.of(PunhoInigualavelAbility.IMPACTO_ELEMENTAL, SenhorDaBrigaAbility.FINALIZACAO));
        SenhorDaBriga withSiblings = new SenhorDaBriga(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL),
                List.of(PunhoInigualavelAbility.IMPACTO_ELEMENTAL, PunhoInigualavelAbility.AGARRAR_E_DERRUBAR,
                        PunhoInigualavelAbility.GRANDE_MESTRE_DAS_BRIGAS));

        assertEquals(2, alone.resolveImpactoElementalAttacks());
        assertEquals(4, withSiblings.resolveImpactoElementalAttacks());
    }

    @Test
    void heldByFindsTheSheetsOwnTitle() {
        SenhorDaBriga title = new SenhorDaBriga(List.of(), List.of());

        assertEquals(Optional.of(title), SenhorDaBriga.heldBy(SenhorDaBrigaFixtures.holder(title)));
        assertEquals(Optional.empty(), SenhorDaBriga.heldBy(SenhorDaBrigaFixtures.combatant()));
    }
}
