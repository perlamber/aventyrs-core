package org.aventyrs.core.feat;

import org.aventyrs.core.ability.StrengthAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
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

    // ---------- what the two halves are worth, read off a roll ----------

    /**
     * The fixed half: "Bônus Racial de +1 em Gnose" reaches a Gnose-governed Perícia roll —
     * Conhecimentos — as one more point of Atributo, and leaves a Força-governed one alone.
     */
    @Test
    void theGnoseBonusReachesAGnoseGovernedPericiaRoll() {
        Character before = characterWithStrengthBase(3);
        Character after = characterWithStrengthBase(3);
        after.grantFeat(new ConselheiroDeGuerraYmirianoFeat(StrengthAbility.DESTRUIDOR_DE_MUROS));

        assertEquals(rollMargin(before, SkillType.CONHECIMENTOS) + 1, rollMargin(after, SkillType.CONHECIMENTOS));
        assertEquals(rollMargin(before, SkillType.ATLETISMO), rollMargin(after, SkillType.ATLETISMO),
                "a Força-governed Perícia is untouched");
    }

    /**
     * The chosen half, read off the effect the pick actually has: {@code DESTRUIDOR_DE_MUROS}
     * makes the Rodada's first attack scale dano by the <em>whole</em> Força rather than half it,
     * so the melee dano bonus differs from a holder who picked something else. This is the
     * assertion that would fail if the granted Habilidade never reached {@code
     * Character#getAttributeAbilities()}.
     */
    @Test
    void theChosenHabilidadeIsInEffectOnARoll() {
        Character destruidor = characterWithStrengthBase(4);
        destruidor.grantFeat(new ConselheiroDeGuerraYmirianoFeat(StrengthAbility.DESTRUIDOR_DE_MUROS));
        Character subjugar = characterWithStrengthBase(4);
        subjugar.grantFeat(new ConselheiroDeGuerraYmirianoFeat(StrengthAbility.SUBJUGAR));

        int forca = destruidor.getEffectiveAttributeTotal(AttributeDomain.STRENGTH);

        assertEquals(forca - forca / 2, meleeDanoBonus(destruidor) - meleeDanoBonus(subjugar),
                "the whole Força instead of half it, for the Rodada's first attack");
    }

    /** The margin against a stated GD — one more point of the governing Atributo moves it by one. */
    private static int rollMargin(final Character character, final SkillType skillType) {
        return skillType.newInteraction()
                .applyTo(CharacterSheet.of(character, new Player()), null,
                        SkillRoll.against(List.of(3, 3, 3), DifficultyLevel.MEDIUM))
                .getMargin();
    }

    private static int meleeDanoBonus(final Character character) {
        DamageBonus danoBonus = SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(CharacterSheet.of(character, new Player()), null, null).getDamageBonus();
        return danoBonus == null ? 0 : danoBonus.getValue();
    }
}
