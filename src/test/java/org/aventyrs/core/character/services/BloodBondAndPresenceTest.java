package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.VampiricoFeat;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.race.Vampiro;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.CarmillaPresence;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.PROGENY_NOT_PERMITTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase H: Laços-de-Sangue, gerar Prole, and the Vampírico Talentos built on them. */
class BloodBondAndPresenceTest {

    private final BloodBondService bonds = new BloodBondServiceImpl();
    private final VampiricPresenceService presence = new VampiricPresenceServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character character(final Race race, final Feat... feats) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .race(race)
                .feats(new ArrayList<>(List.of(feats)))
                .build();
    }

    private static Vampiro nosferatu() {
        return new Vampiro(Vampiro.VampiroLineage.NOSFERATU, new Human());
    }

    private static SceneContext opposing(final CombatantSheet opponent) {
        return new SceneContext(List.of(), List.of(opponent), Map.of(opponent, Range.ADJACENTE),
                TerrainType.values()[0], false, 0, false, opponent);
    }

    // ---------- Laços-de-Sangue ----------

    @Test
    void aMestreVampiroSiresAProleBoundToThem() {
        Character master = character(nosferatu(), VampiricoFeat.LACOS_ROMPIDOS, VampiricoFeat.MESTRE_VAMPIRO);
        assertTrue(bonds.canSireProgeny(master));

        Vampiro progenyRace = bonds.sire(master, new Human());
        Character progeny = character(progenyRace);

        assertEquals(Vampiro.VampiroLineage.NOSFERATU, progenyRace.getLineage());
        assertEquals(Optional.of(master.getId()), bonds.getMaster(progeny));
        assertTrue(bonds.isBoundTo(progeny, master));
    }

    @Test
    void lacosRompidosSeversTheBond() {
        Character master = character(nosferatu(), VampiricoFeat.LACOS_ROMPIDOS, VampiricoFeat.MESTRE_VAMPIRO);
        Character progeny = character(bonds.sire(master, new Human()), VampiricoFeat.LACOS_ROMPIDOS);

        assertTrue(bonds.getMaster(progeny).isEmpty());
        assertFalse(bonds.isBoundTo(progeny, master));
    }

    @Test
    void onlyAMestreWhoIsNoDampiroMaySire() {
        assertFalse(bonds.canSireProgeny(character(nosferatu())));
        Character dampiro = character(new Vampiro(Vampiro.VampiroLineage.DAMPIRO, new Human()),
                VampiricoFeat.MESTRE_VAMPIRO);
        assertFalse(bonds.canSireProgeny(dampiro));

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> bonds.sire(dampiro, new Human()));
        assertEquals(PROGENY_NOT_PERMITTED, refused.getMessage());
    }

    // ---------- Laços Rompidos / Mestre Vampiro rolls ----------

    @Test
    void lacosRompidosGrantsVantagemOnAtencaoAgainstAnotherVampiro() {
        Character holder = character(nosferatu(), VampiricoFeat.LACOS_ROMPIDOS);
        CombatantSheet vampire = CharacterSheet.of(character(nosferatu()), new Player());
        CombatantSheet human = CharacterSheet.of(character(new Human()), new Player());

        assertEquals(Skill.ADVANTAGE_BONUS, VampiricoFeat.LACOS_ROMPIDOS.resolveSkillRollBonus(
                SkillType.ATTENTION, opposing(vampire), null, holder));
        assertEquals(0, VampiricoFeat.LACOS_ROMPIDOS.resolveSkillRollBonus(
                SkillType.ATTENTION, opposing(human), null, holder));
        assertEquals(0, VampiricoFeat.LACOS_ROMPIDOS.resolveSkillRollBonus(
                SkillType.ATLETISMO, opposing(vampire), null, holder));
    }

    @Test
    void mestreVampiroWidensItToAnyoneAndEasesTheGdAgainstVampiros() {
        Character holder = character(nosferatu(), VampiricoFeat.LACOS_ROMPIDOS, VampiricoFeat.MESTRE_VAMPIRO);
        CombatantSheet vampire = CharacterSheet.of(character(nosferatu()), new Player());
        CombatantSheet human = CharacterSheet.of(character(new Human()), new Player());

        assertEquals(Skill.ADVANTAGE_BONUS, VampiricoFeat.LACOS_ROMPIDOS.resolveSkillRollBonus(
                SkillType.ATTENTION, opposing(human), null, holder));
        assertEquals(1, VampiricoFeat.MESTRE_VAMPIRO.resolveDifficultyReduction(SkillType.ATTENTION, holder, opposing(vampire)));
        assertEquals(0, VampiricoFeat.MESTRE_VAMPIRO.resolveDifficultyReduction(SkillType.ATTENTION, holder, opposing(human)));
    }

    // ---------- Poder Vampírico Duradouro ----------

    @Test
    void poderVampiricoDuradouroCountsOnlyPoderes() {
        Character twoPoderes = character(nosferatu(), VampiricoFeat.OSTEOMANCIA, VampiricoFeat.CELERIDADE_VAMPIRICA);
        Character onePoder = character(nosferatu(), VampiricoFeat.OSTEOMANCIA, VampiricoFeat.LACOS_ROMPIDOS);

        assertTrue(VampiricoFeat.PODER_VAMPIRICO_DURADOURO.isEligible(twoPoderes, null));
        assertFalse(VampiricoFeat.PODER_VAMPIRICO_DURADOURO.isEligible(onePoder, null));
        assertTrue(VampiricoFeat.PRESENCA_DE_CARMILLA.isPoderVampirico());
        assertFalse(VampiricoFeat.MESTRE_VAMPIRO.isPoderVampirico());
    }

    // ---------- Presença de Carmilla ----------

    @Test
    void presencaDeCarmillaIsAPoderThatHoldsItsMarker() {
        Character holder = character(nosferatu(), VampiricoFeat.PRESENCA_DE_CARMILLA);

        assertInstanceOf(CarmillaPresence.class, VampiricoFeat.PRESENCA_DE_CARMILLA.resolveActiveAbility()
                .orElseThrow().resolveEffects(holder).get(0));
    }

    @Test
    void itDrainsOnePvPerWoundedLivingNeighbour() {
        CharacterSheet vampire = CharacterSheet.of(character(nosferatu(), VampiricoFeat.PRESENCA_DE_CARMILLA), new Player());
        vampire.applyDamage(5);
        CombatantSheet woundedHuman = CharacterSheet.of(character(new Human()), new Player());
        woundedHuman.applyDamage(1);
        CombatantSheet healthyHuman = CharacterSheet.of(character(new Human()), new Player());
        CombatantSheet woundedVampire = CharacterSheet.of(character(nosferatu()), new Player());
        woundedVampire.applyDamage(1);
        Map<CombatantSheet, Range> distances = new HashMap<>();
        distances.put(woundedHuman, Range.DISTANCIA_MUITO_CURTA);
        distances.put(healthyHuman, Range.ADJACENTE);
        distances.put(woundedVampire, Range.ADJACENTE);
        SceneContext context = new SceneContext(List.of(woundedHuman), List.of(healthyHuman, woundedVampire),
                distances, TerrainType.values()[0], true, 1, false, null);

        assertEquals(0, presence.drain(vampire, context));

        vampire.applyEffect(new CarmillaPresence(2));
        assertEquals(1, presence.drain(vampire, context));
        assertEquals(4, vampire.getDamageTaken());
    }
}
