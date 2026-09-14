package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ActiveAbilityService;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.MovementService;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.race.Bestial;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code BestialFeat#METAMORFOSE_SELVAGEM} — 2PD to become an animal, and everything that hangs
 * off being one.
 *
 * <p>Its Duração is the one <b>reading</b> here rather than a transcription: the clause states
 * none, so the transformation is a toggle. The test that pins that is deliberately explicit about
 * it, since a later source correction would land there first.
 */
class MetamorfoseSelvagemTest {

    private final ActiveAbilityService activeAbilityService = new ActiveAbilityServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();
    private final MovementService movementService = new MovementServiceImpl();

    private static final Weapon SWORD = AbstractWeapon.builder()
            .name("Espada").category(ItemCategory.HEAVY_BLADE)
            .damageBase(DamageBase.of(1, 1)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** A Bestial with a Título and one Herança — Metamorfose Selvagem's own gate, plus an animal to become. */
    private static Character bestial() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new Bestial()).feats(new ArrayList<>()).build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        character.grantFeat(BestialFeat.HERANCA_BOVIDEA);
        character.grantFeat(BestialFeat.METAMORFOSE_SELVAGEM);
        return character;
    }

    private static ActiveAbility transformationOf(final Character character) {
        return character.getActiveAbilities().stream()
                .filter(MetamorfoseSelvagemActiveAbility.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no transformation granted"));
    }

    @Test
    void transformingSpendsTwoDeterminationAndEntersAnAnimalShape() throws IllegalOperationException {
        Character character = bestial();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        activeAbilityService.activate(character, sheet, transformationOf(character), 0);

        assertEquals(2, sheet.getDeterminationSpent());
        assertEquals(0, sheet.getManaSpent());
        assertTrue(sheet.isInForm(FormType.ANIMAL));
    }

    /**
     * <b>A reading, not a transcription.</b> The clause states no Duração, so the shape is a
     * toggle: no number of Rodadas ends it, and only the holder does. If the source is later found
     * to give it a figure, this is the test that should fail first.
     */
    @Test
    void theShapeNeverLapsesOnItsOwn() throws IllegalOperationException {
        Character character = bestial();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        activeAbilityService.activate(character, sheet, transformationOf(character), 0);

        for (int rodada = 0; rodada < 10; rodada++) {
            sheet.finishTurn();
            sheet.startNewRound();
        }

        assertTrue(sheet.isInForm(FormType.ANIMAL), "ten Rodadas later, still an animal");

        sheet.enterForm(null);

        assertNull(sheet.getCurrentForm(), "only its holder ends it");
    }

    // ---------- What being an animal is worth ----------

    @Test
    void theBonusesApplyOnlyWhileTransformed() throws IllegalOperationException {
        Character character = bestial();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int defenseBefore = defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, null);
        int movementBefore = movementService.getMovementBase(sheet);

        activeAbilityService.activate(character, sheet, transformationOf(character), 0);

        assertEquals(defenseBefore + 2, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, null));
        assertEquals(movementBefore + 2, movementService.getMovementBase(sheet));

        sheet.enterForm(null);

        assertEquals(defenseBefore, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, null),
                "a form-gated bonus cannot outlive the form");
        assertEquals(movementBefore, movementService.getMovementBase(sheet));
    }

    /** "Vantagem em suas rolagens [de] Ataque e Dano com armas naturais" — both halves, scoped. */
    @Test
    void vantagemAppliesToBothRollsButOnlyWithArmasNaturais() throws IllegalOperationException {
        Character character = bestial();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        activeAbilityService.activate(character, sheet, transformationOf(character), 0);

        InteractionResult withClaws = attackWith(sheet, NaturalWeapon.GARRAS_AFIADAS);
        InteractionResult withSword = attackWith(sheet, SWORD);

        assertEquals(Skill.ADVANTAGE_BONUS, withClaws.getSkillRollBonus() - withSword.getSkillRollBonus());
        DamageBonus clawDano = withClaws.getDamageBonus();
        assertTrue(clawDano != null && clawDano.getValue() >= Skill.ADVANTAGE_BONUS,
                "the dano half needs the actor's own sheet, which it now gets");
    }

    /** And none of it applies to a Bestial who has not transformed. */
    @Test
    void anUntransformedBestialGetsNothing() {
        Character character = bestial();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertEquals(attackWith(sheet, SWORD).getSkillRollBonus(),
                attackWith(sheet, NaturalWeapon.GARRAS_AFIADAS).getSkillRollBonus());
    }

    // ---------- Equipment ----------

    /** "É impossível usar equipamentos enquanto este Talento estiver ativo." */
    @Test
    void noEquipmentCanBeUsedWhileAnAnimal() throws IllegalOperationException {
        Character character = bestial();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertTrue(sheet.canAttackWith(SWORD));

        activeAbilityService.activate(character, sheet, transformationOf(character), 0);

        assertFalse(sheet.canAttackWith(SWORD));
        assertTrue(sheet.canAttackWith(NaturalWeapon.GARRAS_AFIADAS), "claws are not equipment");

        sheet.enterForm(null);

        assertTrue(sheet.canAttackWith(SWORD));
    }

    private InteractionResult attackWith(final CharacterSheet sheet, final Weapon weapon) {
        return new AtaqueCorpoACorpoInteraction().applyTo(sheet, null, null, null, weapon);
    }
}
