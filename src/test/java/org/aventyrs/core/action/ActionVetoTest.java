package org.aventyrs.core.action;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.character.services.RepositionServiceImpl;
import org.aventyrs.core.combat.AttackReceiver;
import org.aventyrs.core.combat.IncomingAttack;
import org.aventyrs.core.combat.IncomingAttackResult;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The single action veto (core 0.1.5): what each Condição refuses, judged through the paths that
 * ask it — a Perícia roll, movement, Reposicionar, and an attack's price — plus the defence a
 * combatant who may not act still makes.
 */
class ActionVetoTest {

    private static final SkillRoll ROLL = new SkillRoll(List.of(3, 3, 3));

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static CharacterSheet sheet() {
        CharacterSkill melee = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        CharacterSkill defence = CharacterSkillFixture.blank(CharacterSkillFixture.ESQUIVA_E_APARAR_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, melee)
                .skill(SkillType.ESQUIVA_E_APARAR, defence)
                .feats(new java.util.ArrayList<>())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static SceneContext at(final CombatantSheet origin, final Range distance) {
        return new SceneContext(List.of(), List.of(origin), Map.of(origin, distance));
    }

    private static void assertRefused(final Runnable action) {
        IllegalOperationException error = assertThrows(IllegalOperationException.class, action::run);
        assertEquals(TranslatableMessages.ACTION_PREVENTED_BY_CONDITION, error.getMessage());
    }

    // ---------- "Não pode realizar Ações" ----------

    @Test
    void imobilizadoRefusesEveryRollButItsEscapeAndTheDefence() {
        CharacterSheet pinned = sheet();
        pinned.applyCondition(new Condition(ConditionType.IMOBILIZADO, null));

        assertRefused(() -> SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(pinned, null, ROLL));
        assertRefused(() -> SkillType.ATLETISMO.newInteraction().applyTo(pinned, null, ROLL));
        // A defence is not an Ação…
        SkillType.ESQUIVA_E_APARAR.newInteraction().applyTo(pinned, null, ROLL);
        // …and neither is a bonuses-only preview (no SkillRoll).
        SkillType.ATLETISMO.newInteraction().applyTo(pinned, null, null);
        assertTrue(pinned.isMovementPrevented(null));
        assertTrue(pinned.isAbilityActivationPrevented(null));
        assertTrue(pinned.isSpellCastingPrevented(null));
        assertFalse(pinned.isActionPrevented(ActionKind.ESCAPE_IMMOBILIZATION, null));
    }

    @Test
    void desacordadoRefusesEveryActionButTheDefence() {
        CharacterSheet out = sheet();
        out.applyCondition(new Condition(ConditionType.DESACORDADO, ConditionType.DESACORDADO_DURATION_IN_ROUNDS));

        assertRefused(() -> SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(out, null, ROLL));
        assertTrue(out.isActionPrevented(ActionKind.STAND_UP, null));
        assertFalse(out.isActionPrevented(ActionKind.DEFENCE, null));
    }

    @Test
    void petrificadoRefusesRollsToo() {
        CharacterSheet stone = sheet();
        stone.applyCondition(new Condition(ConditionType.PETRIFICADO, 2));

        assertRefused(() -> SkillType.ATLETISMO.newInteraction().applyTo(stone, null, ROLL));
    }

    // ---------- Agarrado: Ações de Movimento only ----------

    @Test
    void agarradoRefusesMovementButNotAnAttack() {
        CharacterSheet grabbed = sheet();
        grabbed.applyCondition(new Condition(ConditionType.AGARRADO, null, sheet()));

        assertTrue(grabbed.isMovementPrevented(null));
        assertFalse(new RepositionServiceImpl().canReposition(grabbed, 1, null));
        SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(grabbed, null, ROLL);
    }

    // ---------- Apavorado: "restritas a fugir" within Curta ----------

    @Test
    void apavoradoRefusesAnAttackWithinCurtaOfItsOriginOnly() {
        CharacterSheet terrified = sheet();
        CharacterSheet fear = sheet();
        terrified.applyCondition(new Condition(ConditionType.APAVORADO, 2, fear));

        assertRefused(() -> SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(terrified, at(fear, Range.DISTANCIA_CURTA), ROLL));
        assertFalse(terrified.isMovementPrevented(at(fear, Range.DISTANCIA_CURTA)), "fleeing is allowed");
        // Beyond Curta the restriction lapses.
        SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(terrified, at(fear, Range.DISTANCIA_MEDIA), ROLL);
    }

    @Test
    void aSourcelessApavoradoRestrictsEverywhere() {
        CharacterSheet panicked = sheet();
        panicked.applyCondition(new Condition(ConditionType.APAVORADO, 2));

        assertTrue(panicked.isActionPrevented(ActionKind.ATTACK, null));
        assertFalse(panicked.isActionPrevented(ActionKind.MOVEMENT, null));
    }

    // ---------- Confuso: +1PA, no Ações Livres or Reações ----------

    @Test
    void confusoAddsOnePaToAnAttacksPrice() {
        CharacterSheet confused = sheet();
        ActionPointsService service = new ActionPointsServiceImpl();
        ActionCost before = service.getAttackCost(confused, SkillType.ATAQUE_CORPO_A_CORPO, null, Set.of(), 1);

        confused.applyCondition(new Condition(ConditionType.CONFUSO, 2));

        assertEquals(before.actionPoints() + 1,
                service.getAttackCost(confused, SkillType.ATAQUE_CORPO_A_CORPO, null, Set.of(), 1).actionPoints());
    }

    @Test
    void confusoRefusesAcoesLivresAndReacoes() {
        CharacterSheet confused = sheet();
        confused.applyCondition(new Condition(ConditionType.CONFUSO, 2));

        assertTrue(confused.isActionPrevented(ActionKind.FREE_ACTION, null));
        assertTrue(confused.isActionPrevented(ActionKind.REACTION, null));
        assertFalse(new RepositionServiceImpl().canReposition(confused, 1, null), "Reposicionar is an Ação Livre");
        assertRefused(() -> SkillType.ATLETISMO.newInteraction()
                .applyTo(confused, null, new SkillRoll(List.of(3, 3, 3), null, null, ActionCost.REACTION)));
        // An ordinary roll still goes.
        SkillType.ATLETISMO.newInteraction().applyTo(confused, null, ROLL);
    }

    // ---------- Caído: Movimento Base halved ----------

    @Test
    void caidoHalvesMovimentoBaseOnce() {
        CharacterSheet prone = sheet();
        int before = new MovementServiceImpl().getMovementBase(prone);

        prone.applyCondition(new Condition(ConditionType.CAIDO, null));

        assertEquals(before / 2, new MovementServiceImpl().getMovementBase(prone));
    }

    // ---------- The defence of one who may not act ----------

    private IncomingAttackResult attacked(final CharacterSheet defender, final List<Integer> faces) {
        MonsterSheet brute = GenericMonster.BRUTAMONTES.spawn(new Player());
        return new AttackReceiver().resolve(IncomingAttack.builder()
                .defender(defender)
                .attacker(brute)
                .difficultyLevel(brute.getSkillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO).level())
                .attackBonus(brute.getSkillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO).bonus())
                .defenseType(DefenseType.PHYSICAL)
                .defenseRoll(new SkillRoll(faces))
                .build());
    }

    @Test
    void anImobilizadoDefenderSucceedsUnlessItIsAFalhaCritica() {
        CharacterSheet free = sheet();
        assertFalse(attacked(free, List.of(1, 2, 2)).getDefended(), "a low roll fails an ordinary defence");

        CharacterSheet pinned = sheet();
        pinned.applyCondition(new Condition(ConditionType.IMOBILIZADO, null));

        assertTrue(attacked(pinned, List.of(1, 2, 2)).getDefended());
        assertFalse(attacked(pinned, List.of(1, 1, 1)).getDefended(), "a Falha Crítica still fails");
    }
}
