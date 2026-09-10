package org.aventyrs.core.race;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoblinsRacialAbilityTest {

    private CharacterSheet sheetOf(final Race race) {
        Character character = race.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(race)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    /** A combat SceneContext with one ally at the given distance — or none at all when null. */
    private SceneContext contextWithAllyAt(final CombatantSheet ally, final Range range) {
        if (ally == null) {
            return new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false);
        }
        return new SceneContext(List.of(ally), List.of(), Map.of(ally, range), null, true, 1, false);
    }

    @Test
    void everyAbilityHasADescription() {
        for (GoblinsRacialAbility ability : GoblinsRacialAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void bothAbilitiesApplyToEveryPericia() {
        for (GoblinsRacialAbility ability : GoblinsRacialAbility.values()) {
            for (SkillType skillType : SkillType.values()) {
                assertTrue(ability.matchesSkillType(skillType));
            }
            assertFalse(ability.matchesSkillType(null));
        }
    }

    // ---- Poder dos Números ------------------------------------------------------------------

    @Test
    void poderDosNumerosGrantsAdvantageWhileAnAllyIsWithinDistanciaCurta() {
        SceneContext context = contextWithAllyAt(sheetOf(new Human()), Range.DISTANCIA_CURTA);

        assertEquals(Optional.of(Skill.ADVANTAGE_BONUS),
                GoblinsRacialAbility.PODER_DOS_NUMEROS.resolveConditionalRollBonus(context, null));
    }

    @Test
    void poderDosNumerosGrantsNoAdvantageWhenTheNearestAllyIsAtDistanciaMedia() {
        SceneContext context = contextWithAllyAt(sheetOf(new Human()), Range.DISTANCIA_MEDIA);

        assertEquals(Optional.empty(),
                GoblinsRacialAbility.PODER_DOS_NUMEROS.resolveConditionalRollBonus(context, null));
    }

    @Test
    void poderDosNumerosGrantsNoAdvantageWithoutASceneContext() {
        assertEquals(Optional.empty(),
                GoblinsRacialAbility.PODER_DOS_NUMEROS.resolveConditionalRollBonus(null, null));
    }

    @Test
    void poderDosNumerosGrantsADamageBonusOnlyWhenANearbyAllyIsItselfAGoblin() {
        SceneContext withGoblin = contextWithAllyAt(sheetOf(new Goblin()), Range.ADJACENTE);
        SceneContext withHuman = contextWithAllyAt(sheetOf(new Human()), Range.ADJACENTE);

        Optional<DamageBonus> bonus = GoblinsRacialAbility.PODER_DOS_NUMEROS
                .resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, withGoblin, null, null);
        assertTrue(bonus.isPresent());
        assertEquals(Skill.ADVANTAGE_BONUS, bonus.get().getValue());
        assertEquals(DamageType.FISICO, bonus.get().getType());

        assertEquals(Optional.empty(), GoblinsRacialAbility.PODER_DOS_NUMEROS
                .resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, withHuman, null, null));
    }

    @Test
    void poderDosNumerosGrantsNoDamageBonusWhenTheGoblinAllyIsTooFarAway() {
        SceneContext context = contextWithAllyAt(sheetOf(new Goblin()), Range.DISTANCIA_MEDIA);

        assertEquals(Optional.empty(), GoblinsRacialAbility.PODER_DOS_NUMEROS
                .resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, context, null, null));
    }

    // ---- Autodesconfiança em Combate -------------------------------------------------------

    @Test
    void autodesconfiancaAppliesADamageMalusWhenNoAllyIsCloseEnoughToHelp() {
        SceneContext alone = contextWithAllyAt(null, null);

        Optional<DamageBonus> malus = GoblinsRacialAbility.AUTODESCONFIANCA_EM_COMBATE
                .resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, alone, null, null);
        assertTrue(malus.isPresent());
        assertEquals(Skill.DISADVANTAGE_MALUS, malus.get().getValue());
    }

    @Test
    void autodesconfiancaAppliesWhenTheOnlyAllyIsAtDistanciaMediaOrFarther() {
        SceneContext distant = contextWithAllyAt(sheetOf(new Goblin()), Range.DISTANCIA_MEDIA);

        assertTrue(GoblinsRacialAbility.AUTODESCONFIANCA_EM_COMBATE
                .resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, distant, null, null).isPresent());
    }

    @Test
    void autodesconfiancaDoesNotApplyOutsideACenaDeCombate() {
        SceneContext nonCombat = new SceneContext(List.of(), List.of(), Map.of());

        assertEquals(Optional.empty(), GoblinsRacialAbility.AUTODESCONFIANCA_EM_COMBATE
                .resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, nonCombat, null, null));
    }

    @Test
    void autodesconfiancaDoesNotApplyWithoutASceneContext() {
        assertEquals(Optional.empty(), GoblinsRacialAbility.AUTODESCONFIANCA_EM_COMBATE
                .resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, null, null, null));
    }

    // ---- The two are exact complements ------------------------------------------------------

    @Test
    void theTwoAbilitiesCanNeverBothApplyToADanoRoll() {
        List<SceneContext> contexts = List.of(
                contextWithAllyAt(null, null),
                contextWithAllyAt(sheetOf(new Goblin()), Range.ADJACENTE),
                contextWithAllyAt(sheetOf(new Goblin()), Range.DISTANCIA_CURTA),
                contextWithAllyAt(sheetOf(new Goblin()), Range.DISTANCIA_MEDIA),
                contextWithAllyAt(sheetOf(new Human()), Range.ADJACENTE),
                contextWithAllyAt(sheetOf(new Human()), Range.DISTANCIA_LONGA));

        for (SceneContext context : contexts) {
            boolean bonus = GoblinsRacialAbility.PODER_DOS_NUMEROS
                    .resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, context, null, null).isPresent();
            boolean malus = GoblinsRacialAbility.AUTODESCONFIANCA_EM_COMBATE
                    .resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, context, null, null).isPresent();
            assertFalse(bonus && malus);
        }
    }
}
