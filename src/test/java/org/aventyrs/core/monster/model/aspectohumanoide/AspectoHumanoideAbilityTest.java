package org.aventyrs.core.monster.model.aspectohumanoide;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.InitiativeServiceImpl;
import org.aventyrs.core.feat.DestinoFeat;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterBlueprint;
import org.aventyrs.core.monster.MonsterRules;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.MonstrousAbilitySelection;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.monster.model.MonsterTestKit.blueprint;
import static org.aventyrs.core.monster.model.MonsterTestKit.held;
import static org.aventyrs.core.monster.model.MonsterTestKit.spawn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AspectoHumanoideAbilityTest {

    private static MonstrousAbilitySelection corpo(final String... skills) {
        return MonstrousAbilitySelection.of(AspectoHumanoideAbility.CORPO_HUMANOIDE,
                Map.of(AspectoHumanoideAbility.SKILLS, List.of(skills)));
    }

    @Test
    void corpoHumanoideAddsATalentoSlotAndMimicsItsPericiasOneNivelUp() {
        MonsterBlueprint presa = blueprint(0, corpo("PERSUASAO")).build();
        assertEquals(1, MonsterRules.featBudget(presa));

        MonsterSheet sheet = presa.spawn(new org.aventyrs.core.sheet.Player());
        DifficultyLevel before = sheet.getSkillDifficulty(SkillType.PERSUASAO).level();
        new ActiveAbilityServiceImpl().activate(sheet.getCharacter(), sheet, held(sheet, "Mimetizar Competência"), 1);
        assertEquals(before.harder(1), sheet.getSkillDifficulty(SkillType.PERSUASAO).level());
        assertEquals(before, sheet.getSkillDifficulty(SkillType.ATLETISMO).level());
    }

    @Test
    void corpoHumanoideAsksForMorePericiasAsItGrows() {
        assertEquals(1, AspectoHumanoideAbility.CORPO_HUMANOIDE.getChoiceSpecs(org.aventyrs.core.monster.MonsterCategory.PRESA).get(0).count());
        assertEquals(2, AspectoHumanoideAbility.CORPO_HUMANOIDE.getChoiceSpecs(org.aventyrs.core.monster.MonsterCategory.DEVIANTE).get(0).count());
        assertEquals(3, AspectoHumanoideAbility.CORPO_HUMANOIDE.getChoiceSpecs(org.aventyrs.core.monster.MonsterCategory.APEX).get(0).count());
    }

    @Test
    void mascaraSocialGrantsCarismaAparenciaInofensivaAndLaterBonuses() {
        MonstrousAbilitySelection mascara = MonstrousAbilitySelection.of(AspectoHumanoideAbility.MASCARA_SOCIAL);
        MonsterSheet presa = spawn(0, mascara);
        assertEquals(3, presa.getCharacter().getEffectiveAttributeTotal(AttributeDomain.CHARISMA));
        assertTrue(presa.getCharacter().getFeats().contains(DestinoFeat.APARENCIA_INOFENSIVA));

        int presaPersuasao = MonsterRules.skillDifficulty(blueprint(0, mascara).build(), SkillType.PERSUASAO).bonus();
        assertEquals(presaPersuasao + 2, MonsterRules.skillDifficulty(blueprint(12, mascara).build(), SkillType.PERSUASAO).bonus());
        assertEquals(new InitiativeServiceImpl().getTotalInitiative(spawn(12, mascara).getCharacter()) + 3,
                new InitiativeServiceImpl().getTotalInitiative(spawn(26, mascara).getCharacter()));
        assertEquals(DifficultyLevel.VERY_EASY.harder(1),
                MonsterRules.skillDifficulty(blueprint(46, mascara).build(), SkillType.PERSUASAO).level());
    }

    @Test
    void mimetizarRacaOpensTheChosenRacesTalentosFromPredador() {
        MonstrousAbilitySelection mimic = MonstrousAbilitySelection.of(AspectoHumanoideAbility.MIMETIZAR_RACA, FeatCategory.TROLL.name());
        assertFalse(MonsterRules.allowedFeatCategories(blueprint(12, mimic).build()).contains(FeatCategory.TROLL));
        MonsterBlueprint predador = blueprint(26, mimic).build();
        assertTrue(MonsterRules.allowedFeatCategories(predador).contains(FeatCategory.TROLL));
        assertEquals(26 / 5 + 1, MonsterRules.featBudget(predador));
    }

    @Test
    void mimetizarCentelhaRaisesThePdMultiplier() {
        assertEquals(1, AspectoHumanoideAbility.MIMETIZAR_CENTELHA.resolveModifier(ModifierType.DETERMINATION_MULTIPLIER,
                org.aventyrs.core.monster.model.AbilityContext.of(org.aventyrs.core.monster.MonsterCategory.DEVIANTE)));
    }

    @Test
    void mimicoStepsFurtividadeAndPersuasao() {
        MonsterBlueprint blueprint = blueprint(12, MonstrousAbilitySelection.of(AspectoHumanoideAbility.MIMICO)).build();
        assertEquals(DifficultyLevel.EASY, MonsterRules.skillDifficulty(blueprint, SkillType.FURTIVIDADE).level());
        assertEquals(DifficultyLevel.EASY, MonsterRules.skillDifficulty(blueprint, SkillType.PERSUASAO).level());
        assertEquals(DifficultyLevel.VERY_EASY, MonsterRules.skillDifficulty(blueprint, SkillType.ATTENTION).level());
    }

    @Test
    void formaVerdadeiraHoldsItsBonusesUntilTheFormIsLeft() {
        MonsterSheet sheet = spawn(61, MonstrousAbilitySelection.of(AspectoHumanoideAbility.FORMA_VERDADEIRA));
        int dex = sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.DEXTERITY, sheet);
        DifficultyLevel attack = sheet.getSkillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO).level();
        new ActiveAbilityServiceImpl().activate(sheet.getCharacter(), sheet, held(sheet, "Forma Verdadeira"), 1);

        assertEquals(dex + 2, sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.DEXTERITY, sheet));
        assertEquals(2, sheet.getTemporaryBonus(ModifierType.DAMAGE_REDUCTION));
        assertEquals(2, sheet.getTotalCriticalResistance(null));
        assertEquals(attack.harder(1), sheet.getSkillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO).level());
        sheet.tickTemporaryEffects();
        assertEquals(2, sheet.getTemporaryBonus(ModifierType.DAMAGE_REDUCTION), "the true form is open-ended");

        sheet.removeEffectsFrom(AspectoHumanoideAbility.FORMA_VERDADEIRA_SOURCE);
        assertEquals(dex, sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.DEXTERITY, sheet));
        assertEquals(attack, sheet.getSkillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO).level());
    }
}
