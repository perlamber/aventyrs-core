package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ActiveAbilityService;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.MovementService;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Vampiro;
import org.aventyrs.core.race.Vampiro.VampiroLineage;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.santo.Santo;
import org.aventyrs.core.character.TitleSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Poder Vampírico activation path — {@link VampiricoFeat#OSTEOMANCIA} / {@link
 * VampiricoFeat#CELERIDADE_VAMPIRICA} / {@link VampiricoFeat#ARMAMENTO_DE_ORLOK} / {@link
 * VampiricoFeat#DOM_DE_MIRCALLA} grant a {@link PoderVampiricoActiveAbility} triggered through
 * {@code ActiveAbilityService#activate}.
 */
class PoderVampiricoActivationTest {

    private final ActiveAbilityService activeAbilityService = new ActiveAbilityServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();
    private final MovementService movementService = new MovementServiceImpl();
    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character vampiroHolding(final VampiricoFeat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .race(new Vampiro(VampiroLineage.NOSFERATU, new Human()))
                .feats(new ArrayList<>())
                .build();
        for (VampiricoFeat feat : feats) {
            character.grantFeat(feat);
        }
        return character;
    }

    private static ActiveAbility abilityOf(final VampiricoFeat feat) {
        return feat.resolveActiveAbility().orElseThrow();
    }

    private int rollBonus(final CharacterSheet sheet, final SkillType skillType) {
        return skillType.newInteraction().applyTo(sheet, null, new SkillRoll(List.of(3, 3, 3))).getSkillRollBonus();
    }

    @Test
    void osteomanciaGrantsADefesasBonusForTheDuracaoAndThenExpires() throws IllegalOperationException {
        Character vampiro = vampiroHolding(VampiricoFeat.OSTEOMANCIA);
        CharacterSheet sheet = CharacterSheet.of(vampiro, new Player());
        int before = defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL);
        int hpBefore = hitPointsService.getCurrentHitPoints(vampiro, sheet);

        activeAbilityService.activate(vampiro, sheet, abilityOf(VampiricoFeat.OSTEOMANCIA), 0);

        assertEquals(before + 2, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
        assertEquals(hpBefore - PoderVampiricoActiveAbility.HIT_POINT_COST,
                hitPointsService.getCurrentHitPoints(vampiro, sheet));

        sheet.tickTemporaryEffects();
        sheet.tickTemporaryEffects();

        assertEquals(before, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
    }

    @Test
    void osteomanciaScalesWithTitulosDespertos() throws IllegalOperationException {
        Character vampiro = vampiroHolding(VampiricoFeat.OSTEOMANCIA);
        vampiro.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet sheet = CharacterSheet.of(vampiro, new Player());
        int before = defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL);

        activeAbilityService.activate(vampiro, sheet, abilityOf(VampiricoFeat.OSTEOMANCIA), 0);

        assertEquals(before + 3, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
    }

    @Test
    void celeridadeVampiricaRaisesActionPointsAndMovementWhileActive() throws IllegalOperationException {
        Character vampiro = vampiroHolding(VampiricoFeat.CELERIDADE_VAMPIRICA);
        vampiro.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet sheet = CharacterSheet.of(vampiro, new Player());
        int actionPointsBefore = actionPointsService.getMaxActionPoints(sheet, 0);
        int movementBefore = movementService.getMovementBase(sheet);

        activeAbilityService.activate(vampiro, sheet, abilityOf(VampiricoFeat.CELERIDADE_VAMPIRICA), 0);

        assertEquals(actionPointsBefore + 1, actionPointsService.getMaxActionPoints(sheet, 0));
        assertEquals(movementBefore + 1, movementService.getMovementBase(sheet));
    }

    @Test
    void poderVampiricoDuradouroExtendsTheDuracaoByOneRoundPerTitulo() throws IllegalOperationException {
        Character vampiro = vampiroHolding(VampiricoFeat.OSTEOMANCIA, VampiricoFeat.DOM_DE_MIRCALLA,
                VampiricoFeat.PODER_VAMPIRICO_DURADOURO);
        vampiro.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet sheet = CharacterSheet.of(vampiro, new Player());
        int before = defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL);

        activeAbilityService.activate(vampiro, sheet, abilityOf(VampiricoFeat.OSTEOMANCIA), 0);

        // Base 2 + 1 Título = 3 Rodadas: still active after 2 ticks.
        sheet.tickTemporaryEffects();
        sheet.tickTemporaryEffects();
        assertTrue(defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL) > before);

        sheet.tickTemporaryEffects();
        assertEquals(before, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
    }

    @Test
    void activateRefusesWhenPayingThreePvWouldBeFatal() {
        Character vampiro = vampiroHolding(VampiricoFeat.OSTEOMANCIA);
        CharacterSheet sheet = CharacterSheet.of(vampiro, new Player());
        int current = hitPointsService.getCurrentHitPoints(vampiro, sheet);
        sheet.applyDamage(current - PoderVampiricoActiveAbility.HIT_POINT_COST); // exactly 3 PV left

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(vampiro, sheet, abilityOf(VampiricoFeat.OSTEOMANCIA), 0));
        assertEquals(0, sheet.getTemporaryBonus(ModifierType.DEFESAS));
    }

    @Test
    void activateRefusesAPoderTheCharacterDoesNotHold() {
        Character vampiro = vampiroHolding();
        CharacterSheet sheet = CharacterSheet.of(vampiro, new Player());

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(vampiro, sheet, abilityOf(VampiricoFeat.OSTEOMANCIA), 0));
    }

    @Test
    void domDeMircallaRaisesACharismaGovernedRollWhileActiveButNotAStrengthOne()
            throws IllegalOperationException {
        Character vampiro = vampiroHolding(VampiricoFeat.DOM_DE_MIRCALLA);
        CharacterSheet sheet = CharacterSheet.of(vampiro, new Player());
        int persuasaoBefore = rollBonus(sheet, SkillType.PERSUASAO);
        int atletismoBefore = rollBonus(sheet, SkillType.ATLETISMO);

        activeAbilityService.activate(vampiro, sheet, abilityOf(VampiricoFeat.DOM_DE_MIRCALLA), 0);

        assertEquals(persuasaoBefore + 1, rollBonus(sheet, SkillType.PERSUASAO));
        assertEquals(atletismoBefore, rollBonus(sheet, SkillType.ATLETISMO));
    }

    @Test
    void mestreVampiroPermanentlyRaisesARollGovernedByTheLineageAttribute() {
        // Nosferatu's lineage Atributo is Força; Atletismo is Força-governed.
        Character vampiro = vampiroHolding();
        CharacterSheet sheet = CharacterSheet.of(vampiro, new Player());
        int atletismoBefore = rollBonus(sheet, SkillType.ATLETISMO);
        int persuasaoBefore = rollBonus(sheet, SkillType.PERSUASAO);

        vampiro.grantFeat(VampiricoFeat.MESTRE_VAMPIRO);

        assertEquals(atletismoBefore + 1, rollBonus(sheet, SkillType.ATLETISMO));
        assertEquals(persuasaoBefore, rollBonus(sheet, SkillType.PERSUASAO));

        assertEquals(1, VampiricoFeat.MESTRE_VAMPIRO.resolveAttributeBonus(AttributeDomain.STRENGTH, vampiro));
        assertEquals(0, VampiricoFeat.MESTRE_VAMPIRO.resolveAttributeBonus(AttributeDomain.CHARISMA, vampiro));
    }
}
