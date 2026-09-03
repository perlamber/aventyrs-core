package org.aventyrs.core.sheet;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.Improvement;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.MovementService;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Condições/Malefícios catalogue and its three routes into the rules engine — typed numeric
 * maluses, implied conditions, and the outright prohibitions. Each condition is tested by the
 * effect it has on the consuming service, not by reading the enum back.
 */
class ConditionTest {

    private final DefenseService defenseService = new DefenseServiceImpl();
    private final MovementService movementService = new MovementServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID()).build(), new Player());
    }

    /** A Scene placing origin at distance from the combatant whose roll is being resolved. */
    private static SceneContext at(final CombatantSheet origin, final Range distance) {
        return new SceneContext(List.of(), List.of(origin), Map.of(origin, distance));
    }

    private int rollBonus(final CharacterSheet target, final SkillType skillType, final SceneContext context) {
        return skillType.newInteraction().applyTo(target, context, null).getSkillRollBonus();
    }

    // ---------- Desprevenido: the typed numeric malus route ----------

    @Test
    void desprevenidoCostsTwoDefesasOnBothDefenseTypes() {
        CharacterSheet target = sheet();
        int physicalBefore = defenseService.getTotalDefense(target, DefenseType.PHYSICAL, null);
        int magicBefore = defenseService.getTotalDefense(target, DefenseType.MAGIC, null);

        target.applyCondition(new Condition(ConditionType.DESPREVENIDO, 1));

        assertEquals(physicalBefore - 2, defenseService.getTotalDefense(target, DefenseType.PHYSICAL, null));
        assertEquals(magicBefore - 2, defenseService.getTotalDefense(target, DefenseType.MAGIC, null));
    }

    /** A condition is a state, not a stacking bonus — applying it twice must not double the malus. */
    @Test
    void applyingTheSameConditionTwiceDoesNotStack() {
        CharacterSheet target = sheet();
        int before = defenseService.getTotalDefense(target, DefenseType.PHYSICAL, null);

        target.applyCondition(new Condition(ConditionType.DESPREVENIDO, 1));
        target.applyCondition(new Condition(ConditionType.DESPREVENIDO, 1));

        assertEquals(before - 2, defenseService.getTotalDefense(target, DefenseType.PHYSICAL, null));
    }

    /** Applying one condition must not silently lift an unrelated one already held. */
    @Test
    void adifferentConditionDoesNotReplaceAHeldOne() {
        CharacterSheet target = sheet();
        target.applyCondition(new Condition(ConditionType.DESPREVENIDO, 1));
        target.applyCondition(new Condition(ConditionType.SILENCIO, 1));

        assertTrue(target.hasCondition(ConditionType.DESPREVENIDO, null));
        assertTrue(target.hasCondition(ConditionType.SILENCIO, null));
    }

    @Test
    void removingAConditionLiftsItsMalus() {
        CharacterSheet target = sheet();
        int before = defenseService.getTotalDefense(target, DefenseType.PHYSICAL, null);
        target.applyCondition(new Condition(ConditionType.DESPREVENIDO, 1));

        target.removeCondition(ConditionType.DESPREVENIDO);

        assertEquals(before, defenseService.getTotalDefense(target, DefenseType.PHYSICAL, null));
        assertFalse(target.hasCondition(ConditionType.DESPREVENIDO, null));
    }

    // ---------- Caído: the implication route ----------

    /** "Também considerado Desprevenido" — the implied condition brings its numbers with it. */
    @Test
    void caidoConfersDesprevenidoAndSoCostsTwoDefesas() {
        CharacterSheet target = sheet();
        int before = defenseService.getTotalDefense(target, DefenseType.PHYSICAL, null);

        target.applyCondition(new Condition(ConditionType.CAIDO, 1));

        assertTrue(target.hasCondition(ConditionType.DESPREVENIDO, null));
        assertEquals(before - 2, defenseService.getTotalDefense(target, DefenseType.PHYSICAL, null));
    }

    /** Lifting Caído lifts the Desprevenido it was conferring — no separate bookkeeping. */
    @Test
    void removingCaidoAlsoLiftsTheDesprevenidoItConferred() {
        CharacterSheet target = sheet();
        target.applyCondition(new Condition(ConditionType.CAIDO, 1));

        target.removeCondition(ConditionType.CAIDO);

        assertFalse(target.hasCondition(ConditionType.DESPREVENIDO, null));
    }

    @Test
    void caidoTaxesEveryPericiaRoll() {
        CharacterSheet target = sheet();
        int before = rollBonus(target, SkillType.ATLETISMO, null);

        target.applyCondition(new Condition(ConditionType.CAIDO, 1));

        assertEquals(before + Skill.DISADVANTAGE_MALUS, rollBonus(target, SkillType.ATLETISMO, null));
    }

    /** Desarmado names the two Perícias de Ataque, so a non-attack roll is untouched. */
    @Test
    void desarmadoTaxesOnlyTheAtaquePericias() {
        CharacterSheet target = sheet();
        int meleeBefore = rollBonus(target, SkillType.ATAQUE_CORPO_A_CORPO, null);
        int atletismoBefore = rollBonus(target, SkillType.ATLETISMO, null);

        target.applyCondition(new Condition(ConditionType.DESARMADO, 1));

        assertEquals(meleeBefore + Skill.DISADVANTAGE_MALUS, rollBonus(target, SkillType.ATAQUE_CORPO_A_CORPO, null));
        assertEquals(atletismoBefore, rollBonus(target, SkillType.ATLETISMO, null));
    }

    // ---------- The fear ladder: proximity scoping ----------

    @Test
    void abaladoTaxesRollsOnlyWithinFourUdOfTheFearsOrigin() {
        CharacterSheet target = sheet();
        CharacterSheet fear = sheet();
        int unafflicted = rollBonus(target, SkillType.ATLETISMO, at(fear, Range.DISTANCIA_CURTA));

        target.applyCondition(new Condition(ConditionType.ABALADO, 2, fear));

        // 4UD is Range.DISTANCIA_CURTA — inside the scope.
        assertEquals(unafflicted + Skill.DISADVANTAGE_MALUS,
                rollBonus(target, SkillType.ATLETISMO, at(fear, Range.DISTANCIA_CURTA)));
        // Beyond it, the clause does not apply.
        assertEquals(unafflicted, rollBonus(target, SkillType.ATLETISMO, at(fear, Range.DISTANCIA_MEDIA)));
    }

    /** With no Scene there is no way to test the proximity, so the scoped malus does not apply. */
    @Test
    void aProximityScopedMalusDoesNotApplyWithoutASceneToMeasureIn() {
        CharacterSheet target = sheet();
        CharacterSheet fear = sheet();
        int before = rollBonus(target, SkillType.ATLETISMO, null);

        target.applyCondition(new Condition(ConditionType.ABALADO, 2, fear));

        assertEquals(before, rollBonus(target, SkillType.ATLETISMO, null));
    }

    /** Apavorado reaches 8UD where Abalado reaches only 4UD. */
    @Test
    void apavoradoReachesEightUdWhereAbaladoReachesFour() {
        CharacterSheet target = sheet();
        CharacterSheet fear = sheet();
        SceneContext eightUd = at(fear, Range.DISTANCIA_MEDIA);
        int before = rollBonus(target, SkillType.ATLETISMO, eightUd);

        target.applyCondition(new Condition(ConditionType.APAVORADO, 2, fear));

        assertEquals(before + Skill.DISADVANTAGE_MALUS, rollBonus(target, SkillType.ATLETISMO, eightUd));
    }

    /** "Recebe a Condição desprevenido enquanto adjacente" — the implication is range-scoped too. */
    @Test
    void assustadoConfersDesprevenidoOnlyWhileAdjacent() {
        CharacterSheet target = sheet();
        CharacterSheet fear = sheet();
        target.applyCondition(new Condition(ConditionType.ASSUSTADO, 2, fear));

        assertTrue(target.hasCondition(ConditionType.DESPREVENIDO, at(fear, Range.ADJACENTE)));
        assertFalse(target.hasCondition(ConditionType.DESPREVENIDO, at(fear, Range.DISTANCIA_CURTA)));
    }

    /** Apavorado's Desprevenido reaches 4UD, one band wider than Assustado's adjacency. */
    @Test
    void apavoradoConfersDesprevenidoWithinFourUd() {
        CharacterSheet target = sheet();
        CharacterSheet fear = sheet();
        target.applyCondition(new Condition(ConditionType.APAVORADO, 2, fear));

        assertTrue(target.hasCondition(ConditionType.DESPREVENIDO, at(fear, Range.DISTANCIA_CURTA)));
        assertFalse(target.hasCondition(ConditionType.DESPREVENIDO, at(fear, Range.DISTANCIA_MEDIA)));
    }

    // ---------- Decay ----------

    /**
     * "Ao fim da duração alvo se torna Assustado", and Assustado in turn becomes Abalado — the
     * ladder steps down rather than simply ending, carrying the same origin each time.
     */
    @Test
    void theFearLadderStepsDownOneRungPerExpiry() {
        CharacterSheet target = sheet();
        CharacterSheet fear = sheet();
        target.applyCondition(new Condition(ConditionType.APAVORADO, 1, fear));

        target.tickTemporaryEffects();
        assertFalse(target.hasCondition(ConditionType.APAVORADO, null));
        assertTrue(target.hasCondition(ConditionType.ASSUSTADO, null));

        // The successor arrives with the stated 2-Rodada duration, so it takes two ticks.
        target.tickTemporaryEffects();
        assertTrue(target.hasCondition(ConditionType.ASSUSTADO, null));
        target.tickTemporaryEffects();
        assertTrue(target.hasCondition(ConditionType.ABALADO, null));
    }

    /** The successor keeps the origin, so its proximity scope still resolves. */
    @Test
    void aDecayedConditionKeepsTheOriginOfTheFear() {
        CharacterSheet target = sheet();
        CharacterSheet fear = sheet();
        target.applyCondition(new Condition(ConditionType.ASSUSTADO, 1, fear));
        int before = rollBonus(target, SkillType.ATLETISMO, at(fear, Range.DISTANCIA_CURTA));

        target.tickTemporaryEffects();

        assertTrue(target.hasCondition(ConditionType.ABALADO, null));
        assertEquals(before, rollBonus(target, SkillType.ATLETISMO, at(fear, Range.DISTANCIA_CURTA)));
    }

    /** A condition with no successor simply ends. */
    @Test
    void aConditionWithNoSuccessorJustExpires() {
        CharacterSheet target = sheet();
        target.applyCondition(new Condition(ConditionType.DESPREVENIDO, 1));

        target.tickTemporaryEffects();

        assertTrue(target.getActiveConditions(null).isEmpty());
    }

    // ---------- The prohibitions ----------

    @Test
    void agarradoStopsMovementOutright() {
        CharacterSheet target = sheet();
        assertEquals(4, movementService.getMovementBase(target));

        target.applyCondition(new Condition(ConditionType.AGARRADO, 1));

        assertEquals(0, movementService.getMovementBase(target));
        assertEquals(0, movementService.getMovementBase(target, 0));
    }

    @Test
    void imobilizadoStopsMovementOutrightToo() {
        CharacterSheet target = sheet();
        target.applyCondition(new Condition(ConditionType.IMOBILIZADO, 1));

        assertEquals(0, movementService.getMovementBase(target));
    }

    /** Agarrado also taxes Perícia rolls, unlike Imobilizado. */
    @Test
    void agarradoTaxesPericiaRollsWhileImobilizadoDoesNot() {
        CharacterSheet grabbed = sheet();
        CharacterSheet pinned = sheet();
        int before = rollBonus(grabbed, SkillType.ATLETISMO, null);

        grabbed.applyCondition(new Condition(ConditionType.AGARRADO, 1));
        pinned.applyCondition(new Condition(ConditionType.IMOBILIZADO, 1));

        assertEquals(before + Skill.DISADVANTAGE_MALUS, rollBonus(grabbed, SkillType.ATLETISMO, null));
        assertEquals(before, rollBonus(pinned, SkillType.ATLETISMO, null));
    }

    /** "Não pode ser curado e nem regenerar" — refused outright, and the Sangramento stays. */
    @Test
    void feridasDolorosasRefusesHealing() {
        CharacterSheet target = sheet();
        target.applyDamage(5);
        target.applyCondition(new Condition(ConditionType.FERIDAS_DOLOROSAS, 1));

        target.heal(3);

        assertEquals(5, target.getDamageTaken());
    }

    @Test
    void healingResumesOnceFeridasDolorosasIsLifted() {
        CharacterSheet target = sheet();
        target.applyDamage(5);
        target.applyCondition(new Condition(ConditionType.FERIDAS_DOLOROSAS, 1));
        target.heal(3);

        target.removeCondition(ConditionType.FERIDAS_DOLOROSAS);
        target.heal(3);

        assertEquals(2, target.getDamageTaken());
    }

    @Test
    void silencioForbidsBothAbilityActivationAndSpellCasting() {
        CharacterSheet target = sheet();
        target.applyCondition(new Condition(ConditionType.SILENCIO, 1));

        assertTrue(target.isAbilityActivationPrevented(null));
        assertTrue(target.isSpellCastingPrevented(null));
        // …and nothing else does.
        assertFalse(target.isMovementPrevented(null));
        assertFalse(target.isHealingPrevented());
    }

    /** Amaldiçoado is a marker by design — it carries no mechanics of its own. */
    @Test
    void amaldicoadoIsAMarkerWithNoEffectsOfItsOwn() {
        CharacterSheet target = sheet();
        int defenseBefore = defenseService.getTotalDefense(target, DefenseType.PHYSICAL, null);
        int rollBefore = rollBonus(target, SkillType.ATLETISMO, null);

        target.applyCondition(new Condition(ConditionType.AMALDICOADO, null));

        assertTrue(target.hasCondition(ConditionType.AMALDICOADO, null));
        assertEquals(defenseBefore, defenseService.getTotalDefense(target, DefenseType.PHYSICAL, null));
        assertEquals(rollBefore, rollBonus(target, SkillType.ATLETISMO, null));
    }

    /** An open-ended condition (null duration) never expires from ticking. */
    @Test
    void anOpenEndedConditionSurvivesTicking() {
        CharacterSheet target = sheet();
        target.applyCondition(new Condition(ConditionType.AMALDICOADO, null));

        target.tickTemporaryEffects();
        target.tickTemporaryEffects();

        assertTrue(target.hasCondition(ConditionType.AMALDICOADO, null));
    }

    /** Two conditions each conferring Desprevenido must not charge its -2 twice. */
    @Test
    void twoConditionsConferringDesprevenidoCostItOnlyOnce() {
        CharacterSheet target = sheet();
        int before = defenseService.getTotalDefense(target, DefenseType.PHYSICAL, null);

        target.applyCondition(new Condition(ConditionType.CAIDO, 1));
        target.applyCondition(new Condition(ConditionType.FLANQUEADO, 1));

        assertEquals(before - 2, defenseService.getTotalDefense(target, DefenseType.PHYSICAL, null));
    }

    /** Every catalogue entry carries its rules text, so a UI can always render one. */
    @Test
    void everyConditionTypeHasADescription() {
        for (ConditionType type : ConditionType.values()) {
            assertFalse(type.getDescription().isBlank(), type.name() + " has no description");
        }
    }
    // ---------- Desarmado: the effect that inflicts it ----------

    private static AbstractWeapon weapon(final String name) {
        return weapon(name, ItemWeightClass.LIGHT);
    }

    private static AbstractWeapon weapon(final String name, final ItemWeightClass weightClass) {
        return AbstractWeapon.builder()
                .name(name)
                .category(ItemCategory.LIGHT_BLADE)
                .weightClass(weightClass)
                .damageBase(DamageBase.of(1, 2))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
    }

    private static CharacterSheet armedWith(final Weapon... weapons) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .equipment(new ArrayList<>(List.of(weapons)))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void disarmingDropsTheWeaponAndInflictsTheCondition() {
        Weapon sword = weapon("Espada");
        CharacterSheet target = armedWith(sword);

        Optional<Weapon> dropped = target.disarm(sword);

        assertEquals(Optional.of(sword), dropped);
        assertTrue(target.hasCondition(ConditionType.DESARMADO, null));
        // Handed back to the caller, not stowed — this core models no ground to drop it on.
        assertFalse(target.getCharacter().getEquipment().contains(sword));
        assertFalse(target.getInventory().contains(sword));
    }

    /** Losing one of two blades is not being Desarmado — you still have something to fight with. */
    @Test
    void losingOneOfTwoWeaponsDoesNotInflictTheCondition() {
        Weapon sword = weapon("Espada");
        Weapon dagger = weapon("Adaga");
        CharacterSheet target = armedWith(sword, dagger);

        assertEquals(Optional.of(sword), target.disarm(sword));

        assertFalse(target.hasCondition(ConditionType.DESARMADO, null));

        // …but losing the second one is.
        target.disarm(dagger);
        assertTrue(target.hasCondition(ConditionType.DESARMADO, null));
    }

    @Test
    void aWeaponThatIsNotWieldedCannotBeDisarmed() {
        CharacterSheet target = armedWith();

        assertEquals(Optional.empty(), target.disarm(weapon("Espada")));
        assertFalse(target.hasCondition(ConditionType.DESARMADO, null));
    }

    /**
     * "Não pode ser desarmado" — the Manopla de Segurança Aprimoramento. Exercised through the
     * real mechanism (a fitted {@code Improvement}) rather than by stubbing {@code
     * Weapon#isDisarmable()}, since that aggregation is the part worth pinning. No catalogued
     * constant overrides it yet — the offensive Aprimoramento catalogue is unauthored.
     */
    @Test
    void aWeaponWithAnUndisarmableImprovementStaysInHand() {
        AbstractWeapon bonded = weapon("Espada Acorrentada");
        bonded.setImprovement(new Improvement() {
            @Override
            public String getName() {
                return "Manopla de Segurança";
            }

            @Override
            public String getDescription() {
                return "Não pode ser desarmado.";
            }

            @Override
            public boolean preventsDisarming() {
                return true;
            }
        });
        CharacterSheet target = armedWith(bonded);

        assertFalse(bonded.isDisarmable());
        assertEquals(Optional.empty(), target.disarm(bonded));
        assertTrue(target.getCharacter().getEquipment().contains(bonded));
        assertFalse(target.hasCondition(ConditionType.DESARMADO, null));
    }

    /** Being disarmed lasts until you re-arm, so it never counts down. */
    @Test
    void theConditionIsOpenEndedAndSurvivesTicking() {
        Weapon sword = weapon("Espada");
        CharacterSheet target = armedWith(sword);
        target.disarm(sword);

        target.tickTemporaryEffects();
        target.tickTemporaryEffects();

        assertTrue(target.hasCondition(ConditionType.DESARMADO, null));
    }

    @Test
    void rearmingLiftsTheConditionAndPutsTheWeaponBack() {
        Weapon sword = weapon("Espada");
        CharacterSheet target = armedWith(sword);
        target.disarm(sword);

        assertTrue(target.rearm(sword));

        assertFalse(target.hasCondition(ConditionType.DESARMADO, null));
        assertTrue(target.getCharacter().getEquipment().contains(sword));
    }

    // ---------- Devorado: unarmed, and unable to re-arm ----------

    /** Nothing you dropped is reachable from inside a creature. */
    @Test
    void aDevoredCharacterCannotPickAWeaponBackUp() {
        Weapon sword = weapon("Espada");
        CharacterSheet target = armedWith(sword);
        target.disarm(sword);
        target.applyCondition(new Condition(ConditionType.DEVORADO, null));

        assertFalse(target.rearm(sword));

        assertFalse(target.getCharacter().getEquipment().contains(sword));
        // …and the Desarmado it was under is not lifted by the failed attempt.
        assertTrue(target.hasCondition(ConditionType.DESARMADO, null));
    }

    /** Once regurgitated, re-arming works again — the block is the condition, not a lost weapon. */
    @Test
    void rearmingWorksAgainOnceDevoradoIsLifted() {
        Weapon sword = weapon("Espada");
        CharacterSheet target = armedWith(sword);
        target.disarm(sword);
        target.applyCondition(new Condition(ConditionType.DEVORADO, null));
        assertFalse(target.rearm(sword));

        target.removeCondition(ConditionType.DEVORADO);

        assertTrue(target.rearm(sword));
        assertTrue(target.getCharacter().getEquipment().contains(sword));
    }

    /**
     * "Podem efetuar rolagens de Ataque Corpo-a-Corpo desarmado ou com Armas Leves" — a light
     * weapon already in hand still works inside a creature; a heavier one cannot be brought to
     * bear. A question, not a gate: nothing refuses such an attack, so this is what a caller
     * consults when deciding which attacks to offer.
     */
    @Test
    void aDevoredCharacterMayOnlyAttackUnarmedOrWithALightWeapon() {
        AbstractWeapon dagger = weapon("Adaga", ItemWeightClass.LIGHT);
        AbstractWeapon greatsword = weapon("Espada Grande", ItemWeightClass.HEAVY);
        CharacterSheet target = armedWith();

        target.applyCondition(new Condition(ConditionType.DEVORADO, null));

        assertTrue(target.canAttackWith(null), "an Ataque Desarmado is always allowed");
        assertTrue(target.canAttackWith(dagger));
        assertFalse(target.canAttackWith(greatsword));
    }

    /** Without the condition nothing is restricted — the weight class is irrelevant. */
    @Test
    void anUnafflictedCharacterMayAttackWithAnyWeapon() {
        assertTrue(armedWith().canAttackWith(weapon("Espada Grande", ItemWeightClass.HEAVY)));
    }

    /** Being Devorado is not itself the Desarmado Malefício — its Desvantagem is never charged. */
    @Test
    void devoradoDoesNotConferTheDesarmadoMalefico() {
        CharacterSheet target = armedWith();
        int meleeBefore = rollBonus(target, SkillType.ATAQUE_CORPO_A_CORPO, null);

        target.applyCondition(new Condition(ConditionType.DEVORADO, null));

        assertFalse(target.hasCondition(ConditionType.DESARMADO, null));
        assertEquals(meleeBefore, rollBonus(target, SkillType.ATAQUE_CORPO_A_CORPO, null));
    }

    /** Being disarmed taxes both Ataque Perícias, but not an ordinary one. */
    @Test
    void beingDisarmedTaxesTheAtaquePericiasOnly() {
        Weapon sword = weapon("Espada");
        CharacterSheet target = armedWith(sword);
        int meleeBefore = rollBonus(target, SkillType.ATAQUE_CORPO_A_CORPO, null);
        int atletismoBefore = rollBonus(target, SkillType.ATLETISMO, null);

        target.disarm(sword);

        assertEquals(meleeBefore + Skill.DISADVANTAGE_MALUS,
                rollBonus(target, SkillType.ATAQUE_CORPO_A_CORPO, null));
        assertEquals(atletismoBefore, rollBonus(target, SkillType.ATLETISMO, null));
    }
}
