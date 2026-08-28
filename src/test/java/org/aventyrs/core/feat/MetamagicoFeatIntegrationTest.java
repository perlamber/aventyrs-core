package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.MagicPointsService;
import org.aventyrs.core.character.services.MagicPointsServiceImpl;
import org.aventyrs.core.character.services.SpellService;
import org.aventyrs.core.character.services.SpellServiceImpl;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.dominiodomana.DominioDoMana;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * That each real {@link MetamagicoFeat} clause actually reaches the service that consumes it —
 * {@link MetamagicoFeatTest} pins the formulas themselves, these pin the wiring.
 */
class MetamagicoFeatIntegrationTest {

    private final SpellService spellService = new SpellServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();
    private final MagicPointsService magicPointsService = new MagicPointsServiceImpl();
    private final RestService restService = new RestServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    private static Character casterWithDominioDoMana(final int graduation) {
        return character()
                .skill(SkillType.DOMINIO_DO_MANA, CharacterSkill.builder()
                        .skill(new DominioDoMana())
                        .graduation(SkillGraduation.builder().graduationValue(graduation).build())
                        .build())
                .build();
    }

    // ---------- the cap ladder reaches SpellService ----------

    @Test
    void aCharacterWithNoTalentosIsCappedAtSemente() {
        assertEquals(BranchLevel.SEMENTE, spellService.getMaxBranchLevel(character().build()));
    }

    @Test
    void arcanistaRaisesTheCapToBroto() {
        Character character = character().build();
        character.grantFeat(MetamagicoFeat.ARCANISTA);

        assertEquals(BranchLevel.BROTO, spellService.getMaxBranchLevel(character));
    }

    @Test
    void arcanistaPlusExperienteRaisesTheCapToMuda() {
        Character character = character().build();
        character.grantFeat(MetamagicoFeat.ARCANISTA);
        character.grantFeat(MetamagicoFeat.ARCANISTA_EXPERIENTE);

        assertEquals(BranchLevel.MUDA, spellService.getMaxBranchLevel(character));
    }

    @Test
    void threeRungsReachEmergente() {
        Character character = character().build();
        character.grantFeat(MetamagicoFeat.ARCANISTA);
        character.grantFeat(MetamagicoFeat.ARCANISTA_EXPERIENTE);
        character.grantFeat(MetamagicoFeat.MESTRE_ARCANISTA);

        assertEquals(BranchLevel.EMERGENTE, spellService.getMaxBranchLevel(character));
    }

    /** The whole ladder lands exactly on FLORESCENTE — no rung missing, none double-counted. */
    @Test
    void allFourRungsReachFlorescenteExactly() {
        Character character = character().build();
        character.grantFeat(MetamagicoFeat.ARCANISTA);
        character.grantFeat(MetamagicoFeat.ARCANISTA_EXPERIENTE);
        character.grantFeat(MetamagicoFeat.MESTRE_ARCANISTA);
        character.grantFeat(MetamagicoFeat.DESAFIADOR_DA_REALIDADE);

        assertEquals(BranchLevel.FLORESCENTE, spellService.getMaxBranchLevel(character));
    }

    // ---------- ARCANISTA's DM reaches DefenseService ----------

    @Test
    void arcanistasMagicDefenseBonusReachesTheDefenseTotal() {
        Character character = casterWithDominioDoMana(6);
        int before = defenseService.getTotalDefense(character, DefenseType.MAGIC);

        character.grantFeat(MetamagicoFeat.ARCANISTA);

        assertEquals(before + 3, defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    @Test
    void arcanistaLeavesPhysicalDefenseAlone() {
        Character character = casterWithDominioDoMana(6);
        int before = defenseService.getTotalDefense(character, DefenseType.PHYSICAL);

        character.grantFeat(MetamagicoFeat.ARCANISTA);

        assertEquals(before, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
    }

    // ---------- MENTE_EXPANDIDA reaches MagicPointsService and RestService ----------

    @Test
    void menteExpandidaRaisesTheResolvedManaMultiplier() {
        Character character = character().build();
        int before = magicPointsService.getManaMultiplier(character);

        character.grantFeat(MetamagicoFeat.MENTE_EXPANDIDA);

        assertEquals(before + 1, magicPointsService.getManaMultiplier(character));
    }

    @Test
    void menteExpandidaRaisesMaxMagicPointsThroughTheMultiplier() {
        Character character = character().build();
        int before = magicPointsService.getMaxMagicPoints(character);

        character.grantFeat(MetamagicoFeat.MENTE_EXPANDIDA);

        int focus = character.getAttributes().getFocus().getTotal();
        assertEquals(before + focus, magicPointsService.getMaxMagicPoints(character));
    }

    @Test
    void menteExpandidaRaisesManaRecoveredOnARest() {
        Character character = character().build();
        int before = restService.getRecoveredMagicPoints(character, RestType.LONGO);

        character.grantFeat(MetamagicoFeat.MENTE_EXPANDIDA);

        assertEquals(before + 2, restService.getRecoveredMagicPoints(character, RestType.LONGO));
    }

    @Test
    void aTalentoWithNoRealClauseChangesNothing() {
        Character character = casterWithDominioDoMana(6);
        int defense = defenseService.getTotalDefense(character, DefenseType.MAGIC);
        int multiplier = magicPointsService.getManaMultiplier(character);
        int recovery = restService.getRecoveredMagicPoints(character, RestType.LONGO);
        BranchLevel cap = spellService.getMaxBranchLevel(character);

        character.grantFeat(MetamagicoFeat.CONJURACAO_RAPIDA);

        assertEquals(defense, defenseService.getTotalDefense(character, DefenseType.MAGIC));
        assertEquals(multiplier, magicPointsService.getManaMultiplier(character));
        assertEquals(recovery, restService.getRecoveredMagicPoints(character, RestType.LONGO));
        assertEquals(cap, spellService.getMaxBranchLevel(character));
    }
}
