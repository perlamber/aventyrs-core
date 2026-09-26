package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ShieldItem;
import org.aventyrs.core.scene.Altitude;
import org.aventyrs.core.scene.EnvironmentalState;
import org.aventyrs.core.scene.LightLevel;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEAparar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ESPECIALISTA_EM_ESCUDO through its acquired form, and ASAS_ADAMANTINAS' flight gate — judged by
 * the Defesas {@link DefenseService} reports, against the same character without the Talento.
 */
class EscudeiroFeatTest {

    private final DefenseService defenseService = new DefenseServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSkill dodgeAt(final int graduation) {
        return CharacterSkill.builder()
                .skill(new EsquivaEAparar())
                .graduation(SkillGraduation.builder().graduationValue(graduation).build())
                .build();
    }

    private static CharacterSheet sheet(final List<Feat> feats, final int dodgeGraduation, final Item... equipment) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ESQUIVA_E_APARAR, dodgeAt(dodgeGraduation))
                .equipment(new ArrayList<>(List.of(equipment)))
                .feats(new ArrayList<>(feats))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private int gain(final DefenseType type, final Feat feat, final int graduation, final Item... equipment) {
        return defenseService.getTotalDefense(sheet(List.of(feat), graduation, equipment), type)
                - defenseService.getTotalDefense(sheet(List.of(), graduation, equipment), type);
    }

    // ---------- ESPECIALISTA_EM_ESCUDO ----------

    @Test
    void theBareConstantDeclaresTheShieldChoice() {
        List<FeatChoice<?>> choices = EscudeiroFeat.ESPECIALISTA_EM_ESCUDO.resolveRequiredChoices(
                CharacterFixture.blank(CharacterFixture.BLANK).build());

        assertEquals(1, choices.size());
        assertEquals(ShieldItem.class, choices.get(0).type());
        assertEquals(List.of(ShieldItem.values()), choices.get(0).options());
    }

    @Test
    void wieldingTheChosenShieldGrantsOneToBothDefesas() {
        Feat feat = EspecialistaEmEscudoFeat.of(ShieldItem.ESCUDO_MEDIO);

        assertEquals(1, gain(DefenseType.PHYSICAL, feat, 1, ShieldItem.ESCUDO_MEDIO));
        assertEquals(1, gain(DefenseType.MAGIC, feat, 1, ShieldItem.ESCUDO_MEDIO));
    }

    @Test
    void theLadderRisesWithEsquivaEApararGraduations() {
        Feat feat = EspecialistaEmEscudoFeat.of(ShieldItem.ESCUDO_MEDIO);

        assertEquals(2, gain(DefenseType.PHYSICAL, feat, 4, ShieldItem.ESCUDO_MEDIO));
        assertEquals(3, gain(DefenseType.PHYSICAL, feat, 7, ShieldItem.ESCUDO_MEDIO));
    }

    /** "Especialista" in a kind of Escudo: any copy forged from the chosen entry counts. */
    @Test
    void aForgedCopyOfTheChosenShieldCounts() {
        Feat feat = EspecialistaEmEscudoFeat.of(ShieldItem.ESCUDO_MEDIO);

        assertEquals(1, gain(DefenseType.PHYSICAL, feat, 1, AbstractItem.fromTemplate(ShieldItem.ESCUDO_MEDIO)));
    }

    @Test
    void anotherShieldGrantsNothing() {
        assertEquals(0, gain(DefenseType.PHYSICAL, EspecialistaEmEscudoFeat.of(ShieldItem.ESCUDO_MEDIO), 7,
                ShieldItem.BROQUEL));
    }

    @Test
    void noShieldAtAllGrantsNothing() {
        assertEquals(0, gain(DefenseType.PHYSICAL, EspecialistaEmEscudoFeat.of(ShieldItem.ESCUDO_MEDIO), 7));
    }

    @Test
    void theChoiceIsReadableForTheTalentosThatNameIt() {
        Character holder = CharacterFixture.blank(CharacterFixture.BLANK)
                .equipment(new ArrayList<>(List.of(ShieldItem.ESCUDO_DE_CORPO)))
                .feats(new ArrayList<>(List.of(EspecialistaEmEscudoFeat.of(ShieldItem.ESCUDO_DE_CORPO))))
                .build();

        assertEquals(ShieldItem.ESCUDO_DE_CORPO, EspecialistaEmEscudoFeat.chosenBy(holder).orElseThrow());
        assertTrue(EspecialistaEmEscudoFeat.isUsingChosenShield(holder));
        assertFalse(EspecialistaEmEscudoFeat.isUsingChosenShield(
                CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build()));
    }

    // ---------- ASAS_ADAMANTINAS ----------

    private static SceneContext context(final boolean flying) {
        return new SceneContext(List.of(), List.of(), Map.of(), null, false, 0, false, null, null,
                new EnvironmentalState(LightLevel.NORMAL, Altitude.ORDINARY, flying, false));
    }

    private int wingsGain(final SceneContext context) {
        return defenseService.getTotalDefense(sheet(List.of(EscudeiroFeat.ASAS_ADAMANTINAS), 0), DefenseType.PHYSICAL,
                context)
                - defenseService.getTotalDefense(sheet(List.of(), 0), DefenseType.PHYSICAL, context);
    }

    @Test
    void wingsFoldedGrantTwo() {
        assertEquals(2, wingsGain(context(false)));
    }

    @Test
    void wingsInFlightGrantNothing() {
        assertEquals(0, wingsGain(context(true)));
    }

    /** No Scene to ask whether they are flying: "cannot tell" grants nothing. */
    @Test
    void noSceneGrantsNothing() {
        assertEquals(0, wingsGain(null));
    }
}
