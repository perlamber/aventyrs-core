package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.Deity;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellCastRequest;
import org.aventyrs.core.magic.SpellCastingResult;
import org.aventyrs.core.magic.SpellCastingService;
import org.aventyrs.core.magic.SpellCastingServiceImpl;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.magic.catalog.IraDeVulcanoSpell;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link DestinoFeat#ARCANISMO_DRUIDICO}, held by a character who acquired it through {@link
 * FeatService#grantFeat}, read off what {@link SpellCastingService} reports for a cast.
 *
 * <p>{@code VidaSpell#REVIGORAR} is the Magia Natural under test (Vida is {@code Natural/Divina}):
 * GD Médio, 2PA, Pessoal — so a cast needs no target.
 */
class ArcanismoDruidicoFeatTest {

    private final FeatService featService = new FeatServiceImpl();
    private final SpellCastingService spellCastingService = new SpellCastingServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** Satisfies the enforced half of the Pré-requisito: Devoto de Gaea. */
    private static Character.CharacterBuilder devotoDeGaea() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).deity(Deity.GAEA);
    }

    private static Character titled(final Character.CharacterBuilder builder) {
        return builder.primaryTitle(new Santo(List.of(), List.of())).build();
    }

    private static CharacterSheet fundedSheet(final Character character) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.TEN);
        return sheet;
    }

    private void acquire(final Character character, final CharacterSheet sheet) {
        featService.grantFeat(character, sheet, DestinoFeat.ARCANISMO_DRUIDICO);
    }

    /** A Scene on the caster's own Turn of the first Rodada (Round 0). */
    private static Scene sceneOnTurnOf(final CharacterSheet sheet) {
        Scene scene = new Scene();
        scene.addParticipant(sheet, 1);
        scene.next();
        return scene;
    }

    private SpellCastingResult cast(final CharacterSheet sheet, final Scene scene, final Spell spell) {
        return spellCastingService.castSpell(SpellCastRequest.builder()
                .caster(sheet)
                .spell(spell)
                .scene(scene)
                .sceneContext(scene.buildContext(sheet, Map.of()))
                .build());
    }

    // ---------- Pré-requisito ----------

    @Test
    void requiresDevotionToGaea() {
        Character unDevoted = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build();
        Character devoted = devotoDeGaea().build();

        assertThrows(IllegalOperationException.class, () -> acquire(unDevoted, fundedSheet(unDevoted)));
        acquire(devoted, fundedSheet(devoted));
        assertEquals(List.of(DestinoFeat.ARCANISMO_DRUIDICO), devoted.getFeats());
    }

    // ---------- "A GD para Conjurar suas Magias Naturais é reduzida em -1 Nível" ----------

    @Test
    void easesTheGdDaConjuracaoOfAMagiaNaturalByOneNivel() {
        Character character = devotoDeGaea().build();
        CharacterSheet sheet = fundedSheet(character);
        Scene scene = sceneOnTurnOf(sheet);
        SpellCastingResult before = cast(sheet, scene, VidaSpell.REVIGORAR);

        acquire(character, sheet);
        SpellCastingResult after = cast(sheet, scene, VidaSpell.REVIGORAR);

        assertEquals(DifficultyLevel.MEDIUM, before.getCastingDifficultyLevel());
        assertEquals(0, before.getCastingDifficultyReduction());
        assertEquals(DifficultyLevel.MEDIUM.easier(1), after.getCastingDifficultyLevel());
        assertEquals(1, after.getCastingDifficultyReduction());
    }

    @Test
    void leavesAMagiaThatIsNotNaturalAlone() {
        Character character = titled(devotoDeGaea());
        CharacterSheet sheet = fundedSheet(character);
        acquire(character, sheet);
        Spell piroclastica = IraDeVulcanoSpell.SOPRO_DE_MAGMA_MENOR;

        assertEquals(0, spellCastingService.resolveCastingDifficultyReduction(piroclastica, sheet));
        assertEquals(piroclastica.getActivationTime(), spellCastingService.resolveActivationTime(piroclastica, sheet, 0));
    }

    // ---------- "o Tempo de Conjuração da primeira Magia Natural … em Rodadas Ímpares … -1PA" ----------

    @Test
    void theFirstMagiaNaturalOfTheFirstRodadaCostsOnePaLess() {
        Character character = titled(devotoDeGaea());
        CharacterSheet sheet = fundedSheet(character);
        Scene scene = sceneOnTurnOf(sheet);
        assertEquals(ActivationTime.pa(2), cast(sheet, scene, VidaSpell.REVIGORAR).getActivationTime());

        acquire(character, sheet);

        assertEquals(ActivationTime.pa(1), cast(sheet, scene, VidaSpell.REVIGORAR).getActivationTime());
    }

    @Test
    void onlyTheFirstMagiaNaturalOfTheRodadaIsReduced() {
        Character character = titled(devotoDeGaea());
        CharacterSheet sheet = fundedSheet(character);
        acquire(character, sheet);
        Scene scene = sceneOnTurnOf(sheet);

        SpellCastingResult first = cast(sheet, scene, VidaSpell.REVIGORAR);
        scene.recordAction(sheet, first.getRecordedAction());
        SpellCastingResult second = cast(sheet, scene, VidaSpell.REVIGORAR);

        assertEquals(ActivationTime.pa(1), first.getActivationTime());
        assertEquals(ActivationTime.pa(2), second.getActivationTime());
        // The GD half is not "primeira" — it applies to every Magia Natural.
        assertEquals(1, second.getCastingDifficultyReduction());
    }

    @Test
    void anEarlierMagiaThatIsNotNaturalDoesNotSpendIt() {
        Character character = titled(devotoDeGaea());
        CharacterSheet sheet = fundedSheet(character);
        acquire(character, sheet);
        Scene scene = sceneOnTurnOf(sheet);
        Spell notNatural = IraDeVulcanoSpell.SOPRO_DE_MAGMA_MENOR;
        scene.recordAction(sheet, new CombatantAction(notNatural.getAttackSkillType(), null, notNatural, null,
                scene.getCurrentRound(), null));

        assertEquals(ActivationTime.pa(1), cast(sheet, scene, VidaSpell.REVIGORAR).getActivationTime());
    }

    /** "Rodadas Ímpares" counts from 1: Round 0 and 2 are the first and third Rodadas, Round 1 the second. */
    @Test
    void appliesOnOddRodadasCountedFromOne() {
        Character character = titled(devotoDeGaea());
        CharacterSheet sheet = fundedSheet(character);
        acquire(character, sheet);

        assertEquals(ActivationTime.pa(1), spellCastingService.resolveActivationTime(VidaSpell.REVIGORAR, sheet, 0));
        assertEquals(ActivationTime.pa(2), spellCastingService.resolveActivationTime(VidaSpell.REVIGORAR, sheet, 1));
        assertEquals(ActivationTime.pa(1), spellCastingService.resolveActivationTime(VidaSpell.REVIGORAR, sheet, 2));
    }

    @Test
    void theActivationHalfNeedsATituloDespertoWhileTheGdHalfDoesNot() {
        Character character = devotoDeGaea().build();
        CharacterSheet sheet = fundedSheet(character);
        acquire(character, sheet);

        SpellCastingResult result = cast(sheet, sceneOnTurnOf(sheet), VidaSpell.REVIGORAR);

        assertEquals(ActivationTime.pa(2), result.getActivationTime());
        assertEquals(1, result.getCastingDifficultyReduction());
    }

    @Test
    void neverReducesBelowOnePa() {
        Character character = titled(devotoDeGaea());
        CharacterSheet sheet = fundedSheet(character);
        acquire(character, sheet);
        Spell onePaNatural = new TestSpell() {
            @Override
            public SpellTree getTree() {
                return MagicTree.VIDA;
            }

            @Override
            public ActivationTime getActivationTime() {
                return ActivationTime.pa(1);
            }
        };

        assertEquals(ActivationTime.pa(1), spellCastingService.resolveActivationTime(onePaNatural, sheet, 0));
    }

    @Test
    void aMagiaCastAsAReacaoHasNoPaToReduce() {
        Character character = titled(devotoDeGaea());
        CharacterSheet sheet = fundedSheet(character);
        acquire(character, sheet);
        // Aliviar a Dor's Efeito Alternativo, Procrastinar Ferimento, is a Reação.
        Spell reacao = VidaSpell.ALIVIAR_A_DOR.getAlternateVersion().orElseThrow();

        assertEquals(ActivationTime.REACAO, spellCastingService.resolveActivationTime(reacao, sheet, 0));
    }

    @Test
    void noOtherDestinoTalentoTouchesACast() {
        Character any = titled(devotoDeGaea());
        for (DestinoFeat feat : DestinoFeat.values()) {
            if (feat != DestinoFeat.ARCANISMO_DRUIDICO) {
                assertEquals(0, feat.resolveCastingDifficultyReduction(VidaSpell.REVIGORAR, any), feat.name());
                assertEquals(0, feat.resolveCastingActionPointReduction(VidaSpell.REVIGORAR, any, 0, List.of()),
                        feat.name());
            }
        }
    }
}
