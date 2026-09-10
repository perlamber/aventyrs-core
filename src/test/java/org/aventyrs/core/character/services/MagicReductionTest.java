package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.GorgonaFeat;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.DefensiveMasterpiece;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemMasterpiece;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Gorgona;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RM — Resistência à Magias, the magic-damage twin of RD. Each instance is worth {@code
 * DamageService#DEFAULT_DAMAGE_REDUCTION}, and it reaches a hit only when the caller classified
 * that hit as {@code DamageType#MAGICO}.
 *
 * <p>Note RD is still type-blind, so a MAGICO hit is reduced by RD <em>and</em> RM — every
 * expectation below that mixes the two says so explicitly rather than hiding it behind a total.
 */
class MagicReductionTest {

    private final DamageService damageService = new DamageServiceImpl();
    private final FeatService featService = new FeatServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    /** A Górgona qualified for Proteção da Rainha das Fadas — Carisma 4. */
    private static Character gorgonaWithCharisma() {
        return character().race(new Gorgona())
                .attributes(CharacterAttributes.builder()
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(4).build())
                        .build())
                .build();
    }

    private static CharacterSheet fundedSheet(final Character character) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        return sheet;
    }

    private static Item armorWith(final DefensiveMasterpiece masterpiece) {
        AbstractItem item = AbstractItem.builder().name("Armadura de teste").category(ItemCategory.ARMOR).build();
        item.setMasterpiece(ItemMasterpiece.of(masterpiece));
        return item;
    }

    /** Dyospiros may only be fitted to a Escudo Médio ou Pesado — its own Aprimoramento clause. */
    private static Item dyospirosShield() {
        AbstractItem shield = AbstractItem.builder().name("Escudo de teste")
                .category(ItemCategory.SHIELD).weightClass(ItemWeightClass.MEDIUM).build();
        shield.setMasterpiece(ItemMasterpiece.of(DefensiveMasterpiece.DYOSPIROS));
        return shield;
    }

    @Test
    void aCombatantWithNoSourceHasNoMagicReduction() {
        assertEquals(0, damageService.getTotalMagicReduction(CharacterSheet.of(character().build(), new Player())));
    }

    /**
     * {@code GorgonaFeat#PROTECAO_DA_RAINHA_DAS_FADAS} — "Você recebe RDS e RM", both
     * unconditional, both numberless, so one instance each.
     */
    @Test
    void protecaoDaRainhaDasFadasGrantsRdAndRmTogether() throws IllegalOperationException {
        Character gorgona = gorgonaWithCharisma();
        CharacterSheet sheet = fundedSheet(gorgona);

        featService.grantFeat(gorgona, sheet, GorgonaFeat.PROTECAO_DA_RAINHA_DAS_FADAS);

        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, damageService.getTotalMagicReduction(sheet));
        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, damageService.getTotalDamageReduction(gorgona));
    }

    /**
     * The two Materiais Especiais whose text reads "Concede RM" — {@code DYOSPIROS} on its Favor,
     * {@code MITRAL} on its Aprimoramento — both gated on their own Requisitos, like every other
     * enhancement bonus.
     */
    @Test
    void dyospirosAndMitralGrantRmOnlyWhenTheirRequirementsAreMet() {
        Character unqualified = character().equipment(List.of(dyospirosShield())).build();
        Character qualified = character()
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(3).build())
                        .build())
                .equipment(List.of(dyospirosShield()))
                .build();
        Character mitral = character()
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .build())
                .equipment(List.of(armorWith(DefensiveMasterpiece.MITRAL)))
                .build();

        assertEquals(0, damageService.getTotalMagicReduction(CharacterSheet.of(unqualified, new Player())));
        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION,
                damageService.getTotalMagicReduction(CharacterSheet.of(qualified, new Player())));
        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION,
                damageService.getTotalMagicReduction(CharacterSheet.of(mitral, new Player())));
    }

    /** A round-scoped grant, the same {@code TemporaryBonus} vehicle RD already has. */
    @Test
    void aRoundScopedMagicReductionBonusSumsWithTheStandingOnes() throws IllegalOperationException {
        Character gorgona = gorgonaWithCharisma();
        CharacterSheet sheet = fundedSheet(gorgona);
        featService.grantFeat(gorgona, sheet, GorgonaFeat.PROTECAO_DA_RAINHA_DAS_FADAS);

        sheet.grantTemporaryBonus(ModifierType.MAGIC_REDUCTION, DamageService.DEFAULT_DAMAGE_REDUCTION, 2);

        assertEquals(2 * DamageService.DEFAULT_DAMAGE_REDUCTION, damageService.getTotalMagicReduction(sheet));
    }

    /**
     * The point of the whole mechanism: RM reaches the mitigation total for a hit typed {@code
     * MAGICO} and for nothing else — not a {@code FISICO} one, and not an unclassified one, where
     * {@code null} means "caller didn't say", never "this was magic".
     */
    @Test
    void magicReductionAppliesOnlyToDamageTypedMagico() throws IllegalOperationException {
        Character gorgona = gorgonaWithCharisma();
        CharacterSheet sheet = fundedSheet(gorgona);
        featService.grantFeat(gorgona, sheet, GorgonaFeat.PROTECAO_DA_RAINHA_DAS_FADAS);
        int rd = DamageService.DEFAULT_DAMAGE_REDUCTION;

        // RD alone on a physical hit and on an unclassified one; RD *and* RM on a magic one,
        // since RD is still type-blind.
        assertEquals(10 - rd,
                damageService.calculateFinalDamage(sheet, null, DamageType.FISICO, null, 10, false));
        assertEquals(10 - rd,
                damageService.calculateFinalDamage(sheet, null, (DamageType) null, null, 10, false));
        assertEquals(10 - rd - DamageService.DEFAULT_DAMAGE_REDUCTION,
                damageService.calculateFinalDamage(sheet, null, DamageType.MAGICO, null, 10, false));
    }

    /** An attack that bypasses mitigation bypasses RM with RD — see the note in {@code computeFinalDamage}. */
    @Test
    void ignoringDamageReductionIgnoresMagicReductionToo() throws IllegalOperationException {
        Character gorgona = gorgonaWithCharisma();
        CharacterSheet sheet = fundedSheet(gorgona);
        featService.grantFeat(gorgona, sheet, GorgonaFeat.PROTECAO_DA_RAINHA_DAS_FADAS);

        assertEquals(10, damageService.calculateFinalDamage(sheet, null, DamageType.MAGICO, null, 10, true));
    }
}
