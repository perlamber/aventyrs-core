package org.aventyrs.core.title.bruxo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.EgoPointsService;
import org.aventyrs.core.character.services.EgoPointsServiceImpl;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneSummon;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.subordinate.SubordinateBenefit;
import org.aventyrs.core.subordinate.SubordinateBenefits;
import org.aventyrs.core.subordinate.SubordinateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EGO_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.REQUIRED_TITLE_TRAIT_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.SUMMON_CANNOT_FIGHT;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_LOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FamiliarMaiorTest {

    private static final Familiar CORVO =
            new Familiar("Corvo", CreatureType.ABISSAL, SubordinateBenefit.TORRE_DEFESAS);

    private final EgoPointsService egos = new EgoPointsServiceImpl();
    private final Player gm = new Player();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void theRitualCostsOnePermanentEgoPointAndBindsTheFamiliarForLife() {
        Bruxo bruxo = familiarBruxo();
        Character holder = holding(bruxo);
        int before = holder.getEgos().getEgo(EgoDomain.SORTE).getTotal();

        Character after = bruxo.performFamiliarRitual(holder, CORVO, EgoDomain.SORTE, egos);

        assertEquals(before - 1, after.getEgos().getEgo(EgoDomain.SORTE).getTotal());
        assertEquals(CORVO, bruxo.getFamiliar().orElseThrow());
        assertSame(bruxo, after.getPrimaryTitle());
        assertEquals(TITLE_ABILITY_CHOICE_LOCKED, assertThrows(IllegalOperationException.class,
                () -> bruxo.performFamiliarRitual(after, CORVO, EgoDomain.SORTE, egos)).getMessage());
    }

    @Test
    void theRitualNeedsFamiliarMaior() {
        Bruxo bruxo = new Bruxo(List.of(BruxoSpecialization.ILUMINADO), List.of());

        assertEquals(REQUIRED_TITLE_TRAIT_NOT_HELD, assertThrows(IllegalOperationException.class,
                () -> bruxo.performFamiliarRitual(holding(bruxo), CORVO, EgoDomain.SORTE, egos)).getMessage());
    }

    @Test
    void anEmptyEgoCannotPayAndNothingIsBound() {
        Bruxo bruxo = familiarBruxo();
        Character holder = egos.sacrificePermanent(holding(bruxo), EgoDomain.SORTE,
                holding(bruxo).getEgos().getEgo(EgoDomain.SORTE).getTotal());

        assertEquals(NOT_ENOUGH_EGO_POINTS, assertThrows(IllegalOperationException.class,
                () -> bruxo.performFamiliarRitual(holder, CORVO, EgoDomain.SORTE, egos)).getMessage());
        assertTrue(bruxo.getFamiliar().isEmpty());
    }

    @Test
    void theFamiliarJoinsTheCenaAsAPermanentSubordinadoAndCannotFight() {
        Bruxo bruxo = familiarBruxo();
        Character holder = bruxo.performFamiliarRitual(holding(bruxo), CORVO, EgoDomain.SORTE, egos);
        CharacterSheet sheet = CharacterSheet.of(holder, gm);
        CharacterSheet foe = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), gm);
        Scene scene = new Scene();
        scene.addParticipant(sheet, 20, UUID.randomUUID());
        scene.addParticipant(foe, 10, UUID.randomUUID());

        SceneSummon summon = bruxo.summonFamiliar(scene, sheet, gm, new SubordinateServiceImpl());
        CombatantSheet token = summon.getSummon();

        assertTrue(scene.getAllParticipants().contains(token));
        assertEquals(FamiliarTemplate.SIZE, token.getCharacter().getSizeCategory());
        assertEquals(CreatureType.ABISSAL, token.getCreatureType());
        assertEquals(List.of(SubordinateBenefit.TORRE_DEFESAS), SubordinateBenefits.of(sheet).stream()
                .map(subordinate -> subordinate.getBenefit()).toList());
        assertEquals(bruxo.getBruxoAbilityCount(), bruxo.getFamiliarLeash());
        assertEquals(SUMMON_CANNOT_FIGHT, assertThrows(IllegalOperationException.class,
                () -> new AttackDelivery().resolve(DeliveredAttack.builder()
                        .attacker(token).defender(foe).attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                        .defenseType(DefenseType.PHYSICAL).defenseValue(5)
                        .attackRoll(new SkillRoll(List.of(3, 3, 3))).build())).getMessage());
    }

    @Test
    void noFamiliarBoundNothingToSummon() {
        Bruxo bruxo = familiarBruxo();
        CharacterSheet sheet = CharacterSheet.of(holding(bruxo), gm);
        Scene scene = new Scene();
        scene.addParticipant(sheet, 20, UUID.randomUUID());

        assertEquals(REQUIRED_TITLE_TRAIT_NOT_HELD, assertThrows(IllegalOperationException.class,
                () -> bruxo.summonFamiliar(scene, sheet, gm, new SubordinateServiceImpl())).getMessage());
    }

    @Test
    void theFamiliarRestoresWithTheTitulo() {
        Bruxo restored = new Bruxo(List.of(BruxoSpecialization.ILUMINADO), List.of(BruxoAbility.FAMILIAR_MAIOR),
                Map.of(), Set.of(), CORVO);

        assertEquals(CORVO, restored.getFamiliar().orElseThrow());
    }

    private static Bruxo familiarBruxo() {
        return new Bruxo(List.of(BruxoSpecialization.ILUMINADO), List.of(BruxoAbility.FAMILIAR_MAIOR));
    }

    /** A Bruxo's holder with Carisma 3, so the familiar's Subordinado fits. */
    private static Character holding(final Bruxo bruxo) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(3).build())
                        .build())
                .build();
        character.grantTitle(bruxo, TitleSlot.PRIMARY);
        return character;
    }
}
