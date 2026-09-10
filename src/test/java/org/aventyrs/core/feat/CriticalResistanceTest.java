package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Elfo;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.race.Troll;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoCompetencyAbility;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Resistência a Críticos as a <b>standing</b> value — what a combatant's Raça ({@code
 * Race#getCriticalResistance()}) and held Talentos ({@code Feat#resolveCriticalResistance})
 * grant, totalled with any round-scoped {@code ModifierType#CRITICAL_RESISTANCE} bonus by
 * {@code CombatantSheet#getTotalCriticalResistance} and subtracted from an <em>attacker</em>'s
 * Margem Crítica Menor widening by {@code AbstractSkillInteraction}.
 *
 * <p>Tested by the effect, per the {@code testing-a-feat} convention: a defender holding the
 * trait makes an attack that <em>would</em> have crit stop critting. The attacker throughout is
 * a character with {@code ATAQUE_PRECISO} (+1 to the Margem Crítica Menor, unconditional) rolling
 * 6+5+2 — a single 6, which reads as Acerto Crítico Menor only because that +1 lets the 5 count
 * alongside it. One RC instance (-2) more than cancels the +1, so the same roll reads NONE.
 */
class CriticalResistanceTest {

    private final FeatService featService = new FeatServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .feats(new ArrayList<>());
    }

    /** An attacker whose Margem Crítica Menor is widened by exactly +1. */
    private static CharacterSheet precisAttacker() {
        return CharacterSheet.of(character()
                .skillCompetencyAbility(AtaqueCorpoACorpoCompetencyAbility.ATAQUE_PRECISO)
                .build(), new Player());
    }

    private static CharacterSheet sheetOf(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    /** The margin-widened attack, resolved against defender. */
    private static CriticalResult attack(final CharacterSheet attacker, final CombatantSheet defender) {
        InteractionResult result = new AtaqueCorpoACorpoInteraction()
                .applyTo(attacker, null, new SkillRoll(List.of(6, 5, 2)), defender, null);
        return result.getCriticalResult();
    }

    @Test
    void aDefenderWithNoCriticalResistanceLetsTheWidenedMarginStand() {
        CharacterSheet defender = sheetOf(character().build());

        assertEquals(0, defender.getTotalCriticalResistance(null));
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, attack(precisAttacker(), defender));
    }

    /**
     * {@code MonstruosoFeat#ANATOMIA_INCOMUM} — "Você recebe Resistência a Críticos", stating no
     * figure, so exactly one instance. Its only Pré-requisito is "apenas personagens
     * não-humanos", so it is acquired for real here — on an Elfo, which is not one, and which
     * grants no RC of its own to muddle the total — through {@code FeatService#grantFeat} rather
     * than the plain mutator.
     */
    @Test
    void anatomiaIncomumNarrowsAnAttackersCriticalMargin() throws IllegalOperationException {
        Character defender = character().race(new Elfo()).build();
        CharacterSheet sheet = sheetOf(defender);
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        featService.grantFeat(defender, sheet, MonstruosoFeat.ANATOMIA_INCOMUM);

        assertEquals(CombatantSheet.CRITICAL_RESISTANCE_INSTANCE, sheet.getTotalCriticalResistance(null));
        assertEquals(CriticalResult.NONE, attack(precisAttacker(), sheet));
    }

    /** {@code Troll}'s Anatomia Vegetal — the race-granted half of the same mechanism. */
    @Test
    void trollAnatomiaVegetalNarrowsAnAttackersCriticalMargin() {
        CharacterSheet defender = sheetOf(character().race(new Troll()).build());

        assertEquals(CombatantSheet.CRITICAL_RESISTANCE_INSTANCE, defender.getTotalCriticalResistance(null));
        assertEquals(CriticalResult.NONE, attack(precisAttacker(), defender));
    }

    /**
     * The three sources are additive, not first-wins: a Troll holding {@code ANATOMIA_INCOMUM}
     * with {@code AnaoFeat#VIGOR_DO_INVERNO}'s combat-start Blessing in force carries three
     * instances at once.
     */
    @Test
    void everySourceOfCriticalResistanceSums() {
        Character defender = character().race(new Troll()).build();
        defender.grantFeat(MonstruosoFeat.ANATOMIA_INCOMUM);
        CharacterSheet sheet = sheetOf(defender);
        sheet.grantTemporaryBonus(ModifierType.CRITICAL_RESISTANCE,
                CombatantSheet.CRITICAL_RESISTANCE_INSTANCE, 3);

        assertEquals(3 * CombatantSheet.CRITICAL_RESISTANCE_INSTANCE, sheet.getTotalCriticalResistance(null));
    }

    /**
     * {@code SobrevivenciaFeat#PROTETOR_TERRITORIALISTA} — RC only "enquanto estiver em seu
     * Terreno Predileto". Terreno is a Scene-wide fact, which is why it can be read off the
     * attacker's own {@code SceneContext} at the crit call site; see {@code
     * Feat#resolveCriticalResistance} for that caveat.
     */
    @Test
    void protetorTerritorialistaGrantsCriticalResistanceOnlyInTheChosenTerrain() {
        Character defender = character().build();
        defender.grantFeat(TerrenoPrediletoFeat.of(TerrainType.FOREST));
        defender.grantFeat(SobrevivenciaFeat.PROTETOR_TERRITORIALISTA);
        CharacterSheet sheet = sheetOf(defender);

        assertEquals(CombatantSheet.CRITICAL_RESISTANCE_INSTANCE,
                sheet.getTotalCriticalResistance(inTerrain(TerrainType.FOREST)));
        assertEquals(0, sheet.getTotalCriticalResistance(inTerrain(TerrainType.DESERT)));
        // No Scene at all reads as "condition not met", the convention every Feat hook follows.
        assertEquals(0, sheet.getTotalCriticalResistance(null));

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, attack(precisAttacker(), sheet));
    }

    /** The standing grants of the two constants whose own Pré-requisitos put them out of reach here. */
    @Test
    void theOtherStandingGrantsAreOneInstanceEach() {
        Character holder = character().build();

        assertEquals(CombatantSheet.CRITICAL_RESISTANCE_INSTANCE,
                DuelistaFeat.CORACAO_DE_FERRO.resolveCriticalResistance(holder, null));
        assertEquals(CombatantSheet.CRITICAL_RESISTANCE_INSTANCE,
                ElementalFeat.TRANSFORMACAO_ELEMENTAL.resolveCriticalResistance(holder, null));
    }

    /** A race without the trait grants nothing — the default every other Raça keeps. */
    @Test
    void aRaceWithoutAnatomiaVegetalGrantsNoCriticalResistance() {
        Race blank = character().build().getRace();

        assertEquals(0, blank.getCriticalResistance());
    }

    private static SceneContext inTerrain(final TerrainType terrain) {
        return new SceneContext(List.of(), List.of(), Map.of(), terrain);
    }
}
