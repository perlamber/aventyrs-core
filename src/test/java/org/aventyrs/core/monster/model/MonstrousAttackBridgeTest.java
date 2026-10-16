package org.aventyrs.core.monster.model;

import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.AttackEffectsPreview;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.effect.EffectChainService;
import org.aventyrs.core.effect.EffectChainServiceImpl;
import org.aventyrs.core.effect.monstrous.Ofuscar;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterBlueprint;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.MonstrousAbilitySelection;
import org.aventyrs.core.monster.SampleMonster;
import org.aventyrs.core.monster.model.cireneia.CireneiaAbility;
import org.aventyrs.core.scene.InitiativePosition;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.aventyrs.core.monster.model.MonsterTestKit.held;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Corrente/resistance bridge (core 0.1.7.2): a Habilidade Monstruosa reaches the holder's attacks and the
 * Resistência à Correntes de Efeitos margin even when the foe is a plain {@link CharacterSheet} built from its
 * blueprint — which is what a live scene holds.
 */
class MonstrousAttackBridgeTest {

    private final AttackDelivery attackDelivery = new AttackDelivery();
    private final EffectChainService effectChains = new EffectChainServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    /** A Predador Pantera holding Ofuscar, as the client's scene builds it: the blueprint's Character on a plain sheet. */
    private static CharacterSheet sceneFoe(final int powerDegree) {
        MonsterBlueprint blueprint = SampleMonster.PANTERA_DE_CIRENEIA.get().toBuilder().powerDegree(powerDegree)
                .ability(MonstrousAbilitySelection.of(CireneiaAbility.OFUSCAR)).build();
        return CharacterSheet.of(blueprint.buildCharacter(), new Player());
    }

    private DeliveredAttackResult claw(final CombatantSheet attacker, final MonsterSheet target) {
        return attackDelivery.resolve(DeliveredAttack.from(target, DefenseType.PHYSICAL)
                .attacker(attacker)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackSource(NaturalWeapon.GARRAS_AFIADAS)
                .attackRoll(new SkillRoll(List.of(6, 6, 6)))
                .build());
    }

    private static boolean carries(final DeliveredAttackResult result, final Class<?> corrente) {
        Interaction<CombatantSheet> next = result.getAttackResult().getNextInteraction();
        while (next != null) {
            if (corrente.isInstance(next)) {
                return true;
            }
            next = next.getNextInteraction();
        }
        return false;
    }

    @Test
    void ofuscarsPassiveLowersItsTargetsResistanceByTwo() {
        CharacterSheet pantera = sceneFoe(30);
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        int unaided = effectChains.getRequiredMargin(null, InitiativePosition.UNKNOWN, capanga, InitiativePosition.UNKNOWN);
        assertEquals(unaided - 2,
                effectChains.getRequiredMargin(pantera, InitiativePosition.UNKNOWN, capanga, InitiativePosition.UNKNOWN));
    }

    @Test
    void theCorrenteRidesOnlyWhileTheActivesWindowRuns() {
        CharacterSheet pantera = sceneFoe(30);
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());

        assertFalse(carries(claw(pantera, capanga), Ofuscar.class));
        assertFalse(AttackEffectsPreview.of(pantera, SkillType.ATAQUE_CORPO_A_CORPO, NaturalWeapon.GARRAS_AFIADAS, null)
                .effectChains().stream().anyMatch(Ofuscar.class::isInstance));

        new ActiveAbilityServiceImpl().activate(pantera.getCharacter(), pantera, held(pantera, "Ofuscar"), 1);

        assertTrue(AttackEffectsPreview.of(pantera, SkillType.ATAQUE_CORPO_A_CORPO, NaturalWeapon.GARRAS_AFIADAS, null)
                .effectChains().stream().anyMatch(Ofuscar.class::isInstance));
        assertTrue(carries(claw(pantera, capanga), Ofuscar.class));
    }

    @Test
    void ofuscarTakesAPaAndGivesOneCumulatively() {
        CharacterSheet pantera = sceneFoe(30);
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        ActionPointsServiceImpl actionPoints = new ActionPointsServiceImpl();
        int foeBefore = actionPoints.getMaxActionPoints(capanga, 1);
        int panteraBefore = actionPoints.getMaxActionPoints(pantera, 1);

        new Ofuscar(pantera).applyTo(capanga);
        new Ofuscar(pantera).applyTo(capanga);

        assertEquals(foeBefore - 2, actionPoints.getMaxActionPoints(capanga, 1));
        assertEquals(panteraBefore + 2, actionPoints.getMaxActionPoints(pantera, 1));
    }

    @Test
    void atAbominacaoTheCorrenteIsAlwaysOn() {
        CharacterSheet pantera = sceneFoe(65);
        assertTrue(AttackEffectsPreview.of(pantera, SkillType.ATAQUE_CORPO_A_CORPO, NaturalWeapon.GARRAS_AFIADAS, null)
                .effectChains().stream().anyMatch(Ofuscar.class::isInstance));
    }
}
