package org.aventyrs.core.feat;

import org.aventyrs.core.ability.StrengthAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConselheiroDeGuerraYmirianoFeatTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character characterWithStrengthBase(final int strengthBase) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(strengthBase).build())
                        .gnose(AttributeValue.builder().domain(AttributeDomain.GNOSE).base(5).build())
                        .build())
                .feats(new ArrayList<>())
                .build();
    }

    @Test
    void delegatesIdentityToTheCatalogConstant() {
        ConselheiroDeGuerraYmirianoFeat feat =
                new ConselheiroDeGuerraYmirianoFeat(StrengthAbility.DESTRUIDOR_DE_MUROS);

        assertSame(AnaoFeat.CONSELHEIRO_DE_GUERRA_YMIRIANO, feat.catalogEntry());
        assertEquals(FeatCategory.ANAO, feat.getFeatCategory());
        assertEquals(AnaoFeat.CONSELHEIRO_DE_GUERRA_YMIRIANO.getDescription(), feat.getDescription());
        assertEquals(AnaoFeat.CONSELHEIRO_DE_GUERRA_YMIRIANO.getFeatRequirements(), feat.getFeatRequirements());
    }

    @Test
    void requiresAChosenAbility() {
        assertThrows(NullPointerException.class,
                () -> new ConselheiroDeGuerraYmirianoFeat(null));
    }

    @Test
    void grantsBonusRacialDeMaisUmEmGnoseAndNothingElse() {
        ConselheiroDeGuerraYmirianoFeat feat =
                new ConselheiroDeGuerraYmirianoFeat(StrengthAbility.DESTRUIDOR_DE_MUROS);

        assertEquals(1, feat.resolveAttributeBonus(AttributeDomain.GNOSE, null));
        for (AttributeDomain domain : AttributeDomain.values()) {
            if (domain != AttributeDomain.GNOSE) {
                assertEquals(0, feat.resolveAttributeBonus(domain, null), domain + " must be untouched");
            }
        }
    }

    @Test
    void grantsTheChosenHabilidadeDeForca() {
        ConselheiroDeGuerraYmirianoFeat feat =
                new ConselheiroDeGuerraYmirianoFeat(StrengthAbility.SUBJUGAR);

        assertEquals(List.of(StrengthAbility.SUBJUGAR), feat.getGrantedAttributeAbilities(null));
    }

    @Test
    void chosenByFindsThePickOnAHolder() {
        Character holder = characterWithStrengthBase(3);
        holder.grantFeat(new ConselheiroDeGuerraYmirianoFeat(StrengthAbility.ESTILHACADOR));

        assertEquals(StrengthAbility.ESTILHACADOR,
                ConselheiroDeGuerraYmirianoFeat.chosenBy(holder).orElseThrow());
        assertTrue(ConselheiroDeGuerraYmirianoFeat.chosenBy(characterWithStrengthBase(3)).isEmpty());
    }

    @Test
    void validatedFactoryRejectsAHolderWithNoForcaAbilitySlot() {
        Character noSlot = characterWithStrengthBase(2);

        assertThrows(IllegalOperationException.class,
                () -> ConselheiroDeGuerraYmirianoFeat.of(noSlot, StrengthAbility.DESTRUIDOR_DE_MUROS));
    }

    @Test
    void validatedFactoryAcceptsAHolderWithAForcaAbilitySlot() {
        Character withSlot = characterWithStrengthBase(3);

        ConselheiroDeGuerraYmirianoFeat feat =
                ConselheiroDeGuerraYmirianoFeat.of(withSlot, StrengthAbility.DESTRUIDOR_DE_MUROS);

        assertEquals(StrengthAbility.DESTRUIDOR_DE_MUROS, feat.getChosenAbility());
    }
}
