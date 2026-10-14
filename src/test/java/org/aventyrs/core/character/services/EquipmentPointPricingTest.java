package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.ego.EquipmentPointExpense;
import org.aventyrs.core.ego.ResourcesAdvantage;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.persuasao.PersuasaoCompetencyAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The three-source PE discount (core 0.1.6.5): Barganhista, Cambalacho and Profissão's Excelências, floored at 1PE. */
class EquipmentPointPricingTest {

    private static final int PRICE = 20;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static Character withProfissao(final int graduation) {
        CharacterSkill profissao = CharacterSkillFixture.blank(CharacterSkillFixture.PROFISSAO_1).build();
        profissao.increaseGraduation(graduation);
        return CharacterFixture.blank(CharacterFixture.BLANK).skill(SkillType.PROFISSAO, profissao).build();
    }

    private static int priced(final Character holder, final EquipmentPointExpense kind) {
        return EquipmentPointPricing.discounted(PRICE, holder, kind);
    }

    /** Focado (Graduação 3) "-1PE"; Lenda (10) "muda para -3PE" — the total, Prodígio adding nothing. */
    @Test
    void profissaoExcellenciesTakeOneAtFocadoAndThreeAtLenda() {
        assertEquals(PRICE, priced(withProfissao(2), EquipmentPointExpense.PURCHASE));
        assertEquals(PRICE - 1, priced(withProfissao(3), EquipmentPointExpense.PURCHASE));
        assertEquals(PRICE - 1, priced(withProfissao(7), EquipmentPointExpense.PRODUCTION));
        assertEquals(PRICE - 3, priced(withProfissao(10), EquipmentPointExpense.PRODUCTION));
    }

    /** "Custos de criação e compras" / "compra e produção" — never an Aprimoramento install. */
    @Test
    void neitherReachesAnAprimoramentoInstall() {
        Character haggler = withProfissao(10).toBuilder()
                .skillCompetencyAbility(PersuasaoCompetencyAbility.CAMBALACHO).build();

        assertEquals(PRICE, priced(haggler, EquipmentPointExpense.UPGRADE));
    }

    @Test
    void cambalachoTakesOneOffBuyingAndProducing() {
        Character trickster = withProfissao(0).toBuilder()
                .skillCompetencyAbility(PersuasaoCompetencyAbility.CAMBALACHO).build();

        assertEquals(PRICE - 1, priced(trickster, EquipmentPointExpense.PURCHASE));
        assertEquals(PRICE - 1, priced(trickster, EquipmentPointExpense.PRODUCTION));
    }

    /** All three sum — Barganhista 2 + Cambalacho 1 + Lenda 3 — and the price still never drops below 1PE. */
    @Test
    void everySourceSumsAndTheFloorHolds() {
        Character everything = withProfissao(10).toBuilder()
                .skillCompetencyAbility(PersuasaoCompetencyAbility.CAMBALACHO)
                .egoAdvantage(EgoDomain.RECURSOS, ResourcesAdvantage.BARGANHISTA).build();

        assertEquals(PRICE - 6, priced(everything, EquipmentPointExpense.PURCHASE));
        assertEquals(1, EquipmentPointPricing.discounted(4, everything, EquipmentPointExpense.PRODUCTION));
    }
}
