package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Feat#suppressesImpliedCondition} — a held Talento vetoing one implication edge, read by
 * {@code CombatantSheet#getActiveConditions}.
 */
class ImpliedConditionSuppressionTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet(final Feat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>(List.of(feats))).build();
        return CharacterSheet.of(character, new Player());
    }

    private static CharacterSheet with(final CharacterSheet sheet, final ConditionType... conditions) {
        for (ConditionType condition : conditions) {
            sheet.applyCondition(new Condition(condition, 2, null));
        }
        return sheet;
    }

    // ---------- SUBMISSAO ----------

    @Test
    void submissaoKeepsAProneHolderFromBeingDesprevenido() {
        CharacterSheet prone = with(sheet(ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_SUBMISSAO), ConditionType.CAIDO);

        assertTrue(prone.hasCondition(ConditionType.CAIDO, null));
        assertFalse(prone.hasCondition(ConditionType.DESPREVENIDO, null));
    }

    @Test
    void withoutTheTalentoCaidoStillConfersDesprevenido() {
        assertTrue(with(sheet(), ConditionType.CAIDO).hasCondition(ConditionType.DESPREVENIDO, null));
    }

    /** Only the Caído → Desprevenido edge is vetoed; a Desprevenido applied outright still lands. */
    @Test
    void aDirectlyAppliedDesprevenidoStillLands() {
        CharacterSheet sheet = with(sheet(ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_SUBMISSAO),
                ConditionType.CAIDO, ConditionType.DESPREVENIDO);

        assertTrue(sheet.hasCondition(ConditionType.DESPREVENIDO, null));
    }

    /** Submissão names Caído alone: Cego still confers Desprevenido on its holder. */
    @Test
    void submissaoDoesNotReachAnotherImplier() {
        assertTrue(with(sheet(ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_SUBMISSAO), ConditionType.CEGO)
                .hasCondition(ConditionType.DESPREVENIDO, null));
    }

    /** The −2 Defesas of Desprevenido is not charged — judged through DefenseService. */
    @Test
    void theProneHolderLosesOnlyWhatCaidoItselfCosts() {
        DefenseServiceImpl defenseService = new DefenseServiceImpl();
        CharacterSheet martialArtist = with(sheet(ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_SUBMISSAO), ConditionType.CAIDO);
        CharacterSheet plain = with(sheet(), ConditionType.CAIDO);

        // Submissão's own +2 DF while Caído, plus the Desprevenido −2 it no longer pays.
        assertEquals(defenseService.getTotalDefense(plain, DefenseType.PHYSICAL) + 2 + 2,
                defenseService.getTotalDefense(martialArtist, DefenseType.PHYSICAL));
        assertEquals(defenseService.getTotalDefense(plain, DefenseType.MAGIC) + 2,
                defenseService.getTotalDefense(martialArtist, DefenseType.MAGIC));
    }

    // ---------- COMBATER_AS_CEGAS ----------

    @Test
    void combaterAsCegasKeepsABlindHolderFromBeingDesprevenido() {
        assertFalse(with(sheet(DuelistaFeat.COMBATER_AS_CEGAS), ConditionType.CEGO)
                .hasCondition(ConditionType.DESPREVENIDO, null));
    }

    /** "rolagens de Perícias feitas às cegas recebem Desvantagem" — the price of the benefit. */
    @Test
    void combaterAsCegasChargesDesvantagemOnRollsMadeBlind() {
        int fighting = new AtaqueCorpoACorpoInteraction()
                .applyTo(with(sheet(DuelistaFeat.COMBATER_AS_CEGAS), ConditionType.CEGO), null, null, null, null)
                .getSkillRollBonus();
        int untrained = new AtaqueCorpoACorpoInteraction()
                .applyTo(with(sheet(), ConditionType.CEGO), null, null, null, null)
                .getSkillRollBonus();

        assertEquals(untrained + Skill.DISADVANTAGE_MALUS, fighting);
    }

    @Test
    void combaterAsCegasChargesNothingWhenSighted() {
        int fighting = new AtaqueCorpoACorpoInteraction()
                .applyTo(sheet(DuelistaFeat.COMBATER_AS_CEGAS), null, null, null, null).getSkillRollBonus();
        int plain = new AtaqueCorpoACorpoInteraction()
                .applyTo(sheet(), null, null, null, null).getSkillRollBonus();

        assertEquals(plain, fighting);
    }
}
