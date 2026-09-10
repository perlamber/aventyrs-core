package org.aventyrs.core.title;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.ElficoFeat;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ACQUISITION_PREVENTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TitleAcquisitionServiceImplTest {

    private final TitleAcquisitionService service = new TitleAcquisitionServiceImpl();

    private record BruxoTitle() implements AventyrTitle {
        @Override public String getName() { return "Bruxo"; }
        @Override public Optional<TitleIdentity> getIdentity() { return Optional.of(TitleIdentity.BRUXO); }
        @Override public TitleArchetype getArchetype() { return TitleArchetype.ARCANO; }
        @Override public String getBaseEffectDescription() { return ""; }
        @Override public List<AventyrTitleSpecialization> getSpecializations() { return List.of(); }
        @Override public List<AventyrTitleAbility> getAbilities() { return List.of(); }
        @Override public void grantAbility(final AventyrTitleAbility ability) { }
    }

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void guardianPreventsAcquiringBruxo() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build();
        character.grantFeat(ElficoFeat.GUARDIAO_DOS_BOSQUES);

        IllegalOperationException exception = assertThrows(IllegalOperationException.class,
                () -> service.grantTitle(character, new BruxoTitle(), TitleSlot.PRIMARY));

        assertEquals(TITLE_ACQUISITION_PREVENTED, exception.getMessage());
    }

    @Test
    void corruptorExplicitlyLiftsAGuardiansBruxoProhibition() throws IllegalOperationException {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build();
        character.grantFeat(ElficoFeat.GUARDIAO_DOS_BOSQUES);
        character.grantFeat(ElficoFeat.CORRUPTOR_SOMBRIO);
        BruxoTitle bruxo = new BruxoTitle();

        service.grantTitle(character, bruxo, TitleSlot.PRIMARY);

        assertEquals(bruxo, character.getPrimaryTitle());
    }
}
