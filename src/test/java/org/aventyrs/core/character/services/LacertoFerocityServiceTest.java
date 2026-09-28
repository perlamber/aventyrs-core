package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.BestialFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.GorgonaFeat;
import org.aventyrs.core.feat.IndomitoFeat;
import org.aventyrs.core.feat.MonstruosoFeat;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Gorgona;
import org.aventyrs.core.race.Indomito;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.LacertoFerocity;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.LACERTO_FEROCITY_MIMIC_ALREADY_USED;
import static org.aventyrs.core.util.TranslatableMessages.LACERTO_FEROCITY_MIMIC_TOO_EARLY;
import static org.aventyrs.core.util.TranslatableMessages.LACERTO_FEROCITY_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EGO_POINTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase I: Ferocidade de Lacerto, and the Talentos that grant, veto, mimic or convert it. */
class LacertoFerocityServiceTest {

    private final LacertoFerocityService service = new LacertoFerocityServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheetOf(final Race race, final Feat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(race)
                .feats(new ArrayList<>(List.of(feats)))
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.grantTemporaryEgoPoints(EgoDomain.AUTOCONTROLE, "test", 3);
        return sheet;
    }

    private static SceneContext round(final int round, final CombatantSheet... alliesInCurta) {
        Map<CombatantSheet, Range> distances = new java.util.HashMap<>();
        for (CombatantSheet ally : alliesInCurta) {
            distances.put(ally, Range.DISTANCIA_CURTA);
        }
        return new SceneContext(List.of(alliesInCurta), List.of(), distances, TerrainType.values()[0],
                true, round, false, null);
    }

    // ---------- the Característica ----------

    @Test
    void anIndomitoTurnsFerociousOnTheFourthRodada() {
        CharacterSheet apedemak = sheetOf(new Indomito(Indomito.Tribo.APEDEMAK));

        assertTrue(service.refresh(apedemak, round(3)).isEmpty());
        assertFalse(apedemak.isFerocious());

        assertTrue(service.refresh(apedemak, round(4)).isPresent());
        assertTrue(apedemak.isFerocious());
        assertEquals(LacertoFerocity.DAMAGE_REDUCTION, apedemak.getTemporaryBonus(ModifierType.DAMAGE_REDUCTION));
        assertEquals(2, apedemak.getTemporaryBonus(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS));
        assertEquals(2, apedemak.getTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS));

        apedemak.endCombat();
        assertFalse(apedemak.isFerocious());
    }

    @Test
    void anImpuroTurnsFerociousOnTheThird() {
        assertEquals(Optional.of(3), service.getFerocityRound(sheetOf(new Indomito(Indomito.Tribo.IMPURO))));
    }

    @Test
    void everyTwoTalentosMonstruososBringItForwardOneRodada() {
        CharacterSheet sheet = sheetOf(new Indomito(Indomito.Tribo.BASTET),
                MonstruosoFeat.FEROCIDADE, MonstruosoFeat.PELE_RIJA, MonstruosoFeat.SANGUE_ACIDO);

        assertEquals(Optional.of(3), service.getFerocityRound(sheet));
    }

    @Test
    void aRaceWithoutItNeverTurnsFerocious() {
        CharacterSheet gorgona = sheetOf(new Gorgona());

        assertTrue(service.getFerocityRound(gorgona).isEmpty());
        assertTrue(service.refresh(gorgona, round(10)).isEmpty());
        IllegalOperationException refused = assertThrows(IllegalOperationException.class, () -> service.decline(gorgona));
        assertEquals(LACERTO_FEROCITY_NOT_HELD, refused.getMessage());
    }

    @Test
    void aTemporaryAutocontrolePointEndsItAndKeepsItOffForTheCombat() {
        CharacterSheet apedemak = sheetOf(new Indomito(Indomito.Tribo.APEDEMAK));
        service.refresh(apedemak, round(4));
        int before = apedemak.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE);

        service.decline(apedemak);

        assertFalse(apedemak.isFerocious());
        assertEquals(before - 1, apedemak.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        assertTrue(service.refresh(apedemak, round(5)).isEmpty());

        apedemak.endCombat();
        assertTrue(service.refresh(apedemak, round(4)).isPresent());
    }

    @Test
    void decliningNeedsATemporaryAutocontrolePoint() {
        CharacterSheet apedemak = sheetOf(new Indomito(Indomito.Tribo.APEDEMAK));
        apedemak.spendEgoPoints(EgoDomain.AUTOCONTROLE, org.aventyrs.core.sheet.EgoPointType.TEMPORARY,
                apedemak.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));

        IllegalOperationException refused = assertThrows(IllegalOperationException.class, () -> service.decline(apedemak));
        assertEquals(NOT_ENOUGH_EGO_POINTS, refused.getMessage());
    }

    // ---------- Talentos ----------

    @Test
    void aceitarASelvageriaGrantsItOnAnImpurosTiming() {
        CharacterSheet gorgona = sheetOf(new Gorgona(), GorgonaFeat.ACEITAR_A_SELVAGERIA);

        assertEquals(Optional.of(Indomito.IMPURO_FEROCITY_ROUND), service.getFerocityRound(gorgona));
        assertTrue(service.refresh(gorgona, round(3)).isPresent());
    }

    @Test
    void renegarALacertoKeepsItOffWhileAStandingAllyIsNear() {
        CharacterSheet indomito = sheetOf(new Indomito(Indomito.Tribo.APEDEMAK), IndomitoFeat.RENEGAR_A_LACERTO);
        CharacterSheet ally = sheetOf(new Gorgona());

        assertTrue(service.refresh(indomito, round(4, ally)).isEmpty());

        ally.applyDamage(1000);
        assertTrue(service.refresh(indomito, round(4, ally)).isPresent());
    }

    @Test
    void aceitarALacertoMimicsItOncePerCombatWithADieOnArmasNaturais() {
        CharacterSheet bestial = sheetOf(new Gorgona(), BestialFeat.ACEITAR_A_LACERTO);

        IllegalOperationException early = assertThrows(IllegalOperationException.class,
                () -> service.mimic(bestial, round(2)));
        assertEquals(LACERTO_FEROCITY_MIMIC_TOO_EARLY, early.getMessage());

        LacertoFerocity mimicked = service.mimic(bestial, round(3));
        assertTrue(mimicked.isMimicked());
        assertTrue(bestial.isFerocious());
        assertEquals(2, bestial.getTemporaryBonus(ModifierType.DAMAGE_REDUCTION));
        assertEquals(1, mimicked.extraDamageDiceFor(bestial.getCharacter(), NaturalWeapon.GARRAS_AFIADAS));
        assertEquals(0, LacertoFerocity.natural().extraDamageDiceFor(bestial.getCharacter(), NaturalWeapon.GARRAS_AFIADAS));

        IllegalOperationException again = assertThrows(IllegalOperationException.class,
                () -> service.mimic(bestial, round(4)));
        assertEquals(LACERTO_FEROCITY_MIMIC_ALREADY_USED, again.getMessage());
    }

    @Test
    void selvageriaConvertsFerocidadesBonusIntoDanoBase() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>(List.of(MonstruosoFeat.FEROCIDADE, MonstruosoFeat.SELVAGERIA)))
                .build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertTrue(MonstruosoFeat.FEROCIDADE.resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, null, null,
                character, NaturalWeapon.GARRAS_AFIADAS).isEmpty());
        assertEquals(1 + 2, MonstruosoFeat.SELVAGERIA.resolveDamageBaseIncrease(character, NaturalWeapon.GARRAS_AFIADAS));
    }
}
