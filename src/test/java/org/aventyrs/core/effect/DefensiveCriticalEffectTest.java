package org.aventyrs.core.effect;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.MagicPointsService;
import org.aventyrs.core.character.services.MagicPointsServiceImpl;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.magic.catalog.SpellCatalog;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Efeitos Críticos Defensivos not already driven end to end through {@code AttackReceiver}. */
class DefensiveCriticalEffectTest {

    private static final CriticalResult MAIOR = CriticalResult.ACERTO_CRITICO_MAIOR;
    private static final CriticalResult MENOR = CriticalResult.ACERTO_CRITICO_MENOR;

    private CharacterSheet defender;
    private CharacterSheet attacker;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        defender = sheet();
        attacker = sheet();
    }

    private static CharacterSheet sheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .equipment(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(3).build())
                        .build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private DefensiveCriticalOutcome apply(final DefensiveCriticalEffectType type, final CriticalResult result,
                                           final Object source, final Integer... dice) {
        return DefensiveCriticalEffect.of(type, defender, attacker, result, SkillType.ATAQUE_CORPO_A_CORPO,
                        (org.aventyrs.core.skill.AttackSource) source,
                        dice.length == 0 ? null : DiceRoller.fixed(dice))
                .orElseThrow().apply();
    }

    @Test
    void choqueDeAetherMenorSwingsTwoPm() {
        defender.spendMagicPoints(5);

        apply(DefensiveCriticalEffectType.CHOQUE_DE_AETHER, MENOR, null);

        assertEquals(3, defender.getManaSpent());
        assertEquals(2, attacker.getManaSpent());
    }

    @Test
    void choqueDeAetherMaiorShiftsBothManaMultipliers() {
        MagicPointsService magic = new MagicPointsServiceImpl();
        int defenderBefore = magic.getManaMultiplier(defender.getCharacter(), defender);
        int attackerBefore = magic.getManaMultiplier(attacker.getCharacter(), attacker);

        apply(DefensiveCriticalEffectType.CHOQUE_DE_AETHER, MAIOR, null);

        assertEquals(defenderBefore + 1, magic.getManaMultiplier(defender.getCharacter(), defender));
        assertEquals(attackerBefore - 1, magic.getManaMultiplier(attacker.getCharacter(), attacker));
        attacker.clearRestCooldowns(RestType.MINIMO);
        assertEquals(attackerBefore, magic.getManaMultiplier(attacker.getCharacter(), attacker));
    }

    @Test
    void faiscaDeDeterminacaoMenorSwingsTwoPd() {
        defender.spendDeterminationPoints(5);

        apply(DefensiveCriticalEffectType.FAISCA_DE_DETERMINACAO, MENOR, null);

        assertEquals(3, defender.getDeterminationSpent());
        assertEquals(2, attacker.getDeterminationSpent());
    }

    @Test
    void impetoDefensivoMenorPushesAndGuardsAgainstThatAttacker() {
        DefenseService defenseService = new DefenseServiceImpl();
        int before = defenseService.getTotalDefense(defender, DefenseType.PHYSICAL);
        SceneContext againstAttacker = new SceneContext(List.of(), List.of(attacker), Map.of(), null, true, 0,
                false, attacker);

        DefensiveCriticalOutcome outcome = apply(DefensiveCriticalEffectType.IMPETO_DEFENSIVO, MENOR, null);

        assertEquals(1, outcome.attackerPushedUd());
        assertEquals(before + 2, defenseService.getTotalDefense(defender, DefenseType.PHYSICAL, againstAttacker));
        assertEquals(before, defenseService.getTotalDefense(defender, DefenseType.PHYSICAL));
    }

    @Test
    void repelirDamagesTheAttackersWeapon() {
        AbstractItem weapon = org.aventyrs.core.item.AbstractWeapon.builder().name("Espada")
                .category(ItemCategory.HEAVY_BLADE).hardness(20)
                .damageBase(org.aventyrs.core.character.DamageBase.of(2, 0))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();

        apply(DefensiveCriticalEffectType.REPELIR_E_SUPRIMIR, MAIOR, weapon, 3, 4);

        assertEquals(7, weapon.getDamageTaken());
    }

    @Test
    void repelirTurnsARepelledMagiaOnItsCaster() {
        DefensiveCriticalOutcome outcome = apply(DefensiveCriticalEffectType.REPELIR_E_SUPRIMIR, MAIOR,
                SpellCatalog.all().get(0), 3, 4);

        assertEquals(7, attacker.getDamageTaken());
        assertTrue(outcome.attackerCastingSuppressed());
        assertTrue(attacker.isSpellCastingPrevented(null));
    }

    @Test
    void surtoArcanoGrantsAQuickCastByTier() {
        assertEquals(DefensiveCriticalOutcome.QuickCast.ANY,
                apply(DefensiveCriticalEffectType.SURTO_ARCANO, MAIOR, null).quickCast());
        assertEquals(DefensiveCriticalOutcome.QuickCast.SEED_OR_BUD,
                apply(DefensiveCriticalEffectType.SURTO_ARCANO, MENOR, null).quickCast());
    }

    @Test
    void theGrantedListStacksArmourAndShield() {
        CombatantSheet armoured = sheet();
        armoured.getCharacter().equip(AbstractItem.fromTemplate(org.aventyrs.core.item.ArmorItem.ROUPA_PESADA));
        armoured.getCharacter().equip(AbstractItem.fromTemplate(org.aventyrs.core.item.ShieldItem.BROQUEL));

        assertEquals(List.of(DefensiveCriticalEffectType.LIBERDADE_DE_ACAO, DefensiveCriticalEffectType.CONTRA_ATACANTE),
                DefensiveCriticalEffects.grantedTo(armoured));
    }
}
