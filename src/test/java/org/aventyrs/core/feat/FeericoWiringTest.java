package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ActiveAbilityService;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.race.Aviano;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.race.Fada;
import org.aventyrs.core.race.Furia;
import org.aventyrs.core.race.Gorgona;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.NascidoDaFloresta;
import org.aventyrs.core.race.Satiro;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.FormEffect;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every Talento Feérico clause wired in core 0.1.7.8 beside the Efeito de Voo (which {@code
 * FlightServiceTest} covers): the named-Raça gates, Asas' creation branch, Ninfa's Sexo, Sirenídeo's
 * guelras, the Sátiro lineages' Tendência GD and mutual exclusion, Fauno's Monstruoso type,
 * Ancienteforme's Nascido da Floresta Rodadas and Sentir o Fluxo de Mana's sense.
 */
class FeericoWiringTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .skillCompetencyAbilities(new ArrayList<>())
                .equipment(new ArrayList<>());
    }

    private static Character titled(final Character character) {
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        return character;
    }

    private static SceneContext against(final Character opponent) {
        return new SceneContext(List.of(), List.of(), Map.of(), null, false, 0, false,
                CharacterSheet.of(opponent, new Player()));
    }

    @Test
    void pixieAndSirenideoAreForFadasAndFuriasOnly() {
        assertTrue(FeericoFeat.PIXIE.isEligible(character().race(new Fada()).build()));
        assertTrue(FeericoFeat.SIRENIDEO.isEligible(character().race(new Furia()).build()));
        // A Górgona is Feérica too, but not one of the two Raças named.
        assertFalse(FeericoFeat.PIXIE.isEligible(character().race(new Gorgona()).build()));
        assertFalse(FeericoFeat.SIRENIDEO.isEligible(character().race(new Gorgona()).build()));
    }

    @Test
    void espiritoDaFlorestaIsForFadasFuriasAndSatiros() {
        assertTrue(FeericoFeat.ESPIRITO_DA_FLORESTA.isEligible(titled(character().race(new Satiro()).build())));
        assertTrue(FeericoFeat.ESPIRITO_DA_FLORESTA.isEligible(titled(character().race(new Fada()).build())));
        assertFalse(FeericoFeat.ESPIRITO_DA_FLORESTA.isEligible(titled(character().race(new Gorgona()).build())));
    }

    @Test
    void asasOpenToAFeericoOnlyAtCreation() {
        Character fada = character().race(new Fada()).build();
        Character aviano = character().race(new Aviano(Aviano.Subtipo.RAPINANTE)).build();

        assertFalse(FeericoFeat.ASAS.isEligible(fada));
        assertTrue(FeericoFeat.ASAS.isEligibleAtCreation(fada, null));
        assertTrue(FeericoFeat.ASAS.isEligible(aviano));
    }

    @Test
    void ninfaNeedsAFemaleCharacter() {
        assertTrue(FeericoFeat.NINFA.isEligible(titled(character().race(new Fada()).sexo(Character.Sexo.FEMININO).build())));
        assertFalse(FeericoFeat.NINFA.isEligible(titled(character().race(new Fada()).sexo(Character.Sexo.MASCULINO).build())));
        assertFalse(FeericoFeat.NINFA.isEligible(titled(character().race(new Fada()).sexo(null).build())));
    }

    @Test
    void sirenideoBreathesUnderwater() {
        Character sirenideo = character().race(new Fada()).build();
        sirenideo.grantFeat(FeericoFeat.SIRENIDEO);

        assertTrue(sirenideo.canBreatheUnderwater());
    }

    @Test
    void faunoAndLupercalExcludeEachOther() {
        Character fauno = character().race(new Satiro()).build();
        fauno.grantFeat(FeericoFeat.FAUNO);
        Character lupercal = character().race(new Satiro()).build();
        lupercal.grantFeat(FeericoFeat.LUPERCAL);

        assertFalse(FeericoFeat.LUPERCAL.isEligible(fauno));
        assertFalse(FeericoFeat.FAUNO.isEligible(lupercal));
        assertTrue(FeericoFeat.FAUNO.isEligible(character().race(new Satiro()).build()));
    }

    @Test
    void faunoMayTakeTalentosMonstruosos() {
        Character fauno = character().race(new Satiro()).build();
        fauno.grantFeat(FeericoFeat.FAUNO);

        assertTrue(fauno.getPrerequisiteCreatureTypes().contains(CreatureType.MONSTRUOSO));
        assertTrue(fauno.getPrerequisiteCreatureTypes().contains(CreatureType.FEERICO));
    }

    @Test
    void theSatiroLineagesEaseRollsAgainstOneTendencia() {
        Character good = character().race(new Human()).alignment(9).build();
        Character evil = character().race(new Human()).alignment(2).build();
        Character satiro = character().race(new Satiro()).build();

        assertEquals(1, FeericoFeat.FAUNO.resolveDifficultyReduction(SkillType.PERSUASAO, satiro, against(good)));
        assertEquals(0, FeericoFeat.FAUNO.resolveDifficultyReduction(SkillType.PERSUASAO, satiro, against(evil)));
        assertEquals(1, FeericoFeat.LUPERCAL.resolveDifficultyReduction(SkillType.ATTENTION, satiro, against(evil)));
        assertEquals(0, FeericoFeat.LUPERCAL.resolveDifficultyReduction(SkillType.FURTIVIDADE, satiro, against(evil)));
        assertEquals(0, FeericoFeat.LUPERCAL.resolveDifficultyReduction(SkillType.ARTES, satiro, null));
    }

    @Test
    void ancienteformeLastsTwoMoreRodadasForANascidoDaFloresta() {
        ActiveAbilityService activeAbilityService = new ActiveAbilityServiceImpl();
        assertEquals(5, ancienteformeRounds(new NascidoDaFloresta(new Human()), activeAbilityService));
        assertEquals(3, ancienteformeRounds(new Fada(), activeAbilityService));
    }

    private static int ancienteformeRounds(final org.aventyrs.core.race.Race race,
                                           final ActiveAbilityService activeAbilityService) {
        Character character = titled(character().race(race)
                .attributes(org.aventyrs.core.character.CharacterAttributes.builder()
                        .vigor(org.aventyrs.core.character.AttributeValue.builder()
                                .domain(org.aventyrs.core.character.AttributeDomain.VIGOR).base(4).build())
                        .build())
                .build());
        character.grantFeat(FeericoFeat.DRIADE);
        character.grantFeat(FeericoFeat.ANCIENTEFORME);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        ActiveAbility ability = FeericoFeat.ANCIENTEFORME.resolveActiveAbility().orElseThrow();

        activeAbilityService.activate(character, sheet, ability, 0);

        Optional<FormEffect> form = sheet.getRunningEffects().stream()
                .filter(FormEffect.class::isInstance).map(FormEffect.class::cast).findFirst();
        return form.orElseThrow().getRemainingRounds();
    }

    @Test
    void sentirOFluxoDeManaSensesCastingWithinCurta() {
        Character sensing = character().race(new Fada()).build();
        sensing.grantFeat(FeericoFeat.SENTIR_O_FLUXO_DE_MANA);

        assertEquals(Optional.of(Range.DISTANCIA_CURTA), sensing.getSpellcastingSenseRange());
        assertEquals(Optional.empty(), character().race(new Fada()).build().getSpellcastingSenseRange());
    }
}
