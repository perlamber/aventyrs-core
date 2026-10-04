package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Hidden;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.artes.ArtesCompetencyAbility;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemCompetencyAbility;
import org.aventyrs.core.skill.empatiaselvagem.TrainedCompanion;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararCompetencyAbility;
import org.aventyrs.core.skill.furtividade.FurtividadeCompetencyAbility;
import org.aventyrs.core.skill.furtividade.FurtividadeSpecialization;
import org.aventyrs.core.skill.medicinaecura.MedicinaECuraCompetencyAbility;
import org.aventyrs.core.skill.persuasao.PersuasaoCompetencyAbility;
import org.aventyrs.core.skill.persuasao.PersuasaoSpecialization;
import org.aventyrs.core.subordinate.SubordinateBenefit;
import org.aventyrs.core.subordinate.SubordinateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase 4 of the Habilidades de Competência plan: the activated abilities (core 0.0.103). */
class CompetencyActionsTest {

    /** A sure success at any GD this test states: three 6s. */
    private static final List<Integer> HIGH = List.of(6, 6, 6);
    /** A sure failure: three 1s. */
    private static final List<Integer> LOW = List.of(1, 1, 1);

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** Every Atributo 5, skill trained to graduation, holding abilities. */
    private static CharacterSheet holding(final SkillType skill, final int graduation,
                                          final SkillCompetencyAbility... abilities) {
        CharacterAttributes.CharacterAttributesBuilder attributes = CharacterAttributes.builder();
        attributes.vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(5).build())
                .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(5).build())
                .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(5).build())
                .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(5).build())
                .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(5).build())
                .gnose(AttributeValue.builder().domain(AttributeDomain.GNOSE).base(5).build())
                .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(5).build());
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK).attributes(attributes.build());
        if (graduation > 0) {
            builder.skill(skill, CharacterSkill.builder().skill(skill.newSkillInstance())
                    .specializations(List.copyOf(SkillTraitCatalog.specializationsOf(skill)))
                    .graduation(SkillGraduation.builder().graduationValue(graduation).build()).build());
        }
        for (SkillCompetencyAbility ability : abilities) {
            builder.skillCompetencyAbility(ability);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    private static CharacterSheet plain() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    // Estudar Defesas

    @Test
    void aSuccessfulStudyEasesEveryAttackOnThatTargetForTwoRodadas() {
        CharacterSheet holder = holding(SkillType.ESQUIVA_E_APARAR, 5, EsquivaEApararCompetencyAbility.ESTUDAR_DEFESAS);
        CharacterSheet foe = plain();
        int before = SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(holder, null, null, foe).getDifficultyReduction();

        InteractionResult study = CompetencyActions.estudarDefesas(holder, foe, null, HIGH);

        assertTrue(study.getSucceeded());
        assertEquals(before + 1, SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(holder, null, null, foe)
                .getDifficultyReduction());
        holder.startNewRound();
        assertTrue(holder.hasStudiedDefensesOf(foe));
        holder.startNewRound();
        assertFalse(holder.hasStudiedDefensesOf(foe), "por 2 Rodadas");
    }

    @Test
    void aFailedStudyEasesNothing() {
        CharacterSheet holder = holding(SkillType.ESQUIVA_E_APARAR, 5, EsquivaEApararCompetencyAbility.ESTUDAR_DEFESAS);
        CharacterSheet foe = plain();

        CompetencyActions.estudarDefesas(holder, foe, null, LOW);

        assertFalse(holder.hasStudiedDefensesOf(foe));
    }

    @Test
    void studyingRequiresTheAbility() {
        assertThrows(IllegalOperationException.class,
                () -> CompetencyActions.estudarDefesas(plain(), plain(), null, HIGH));
    }

    // Esconder Outros

    @Test
    void anAllyIsHiddenOneNivelBelowTheRollsTier() {
        CharacterSheet holder = holding(SkillType.FURTIVIDADE, 5, FurtividadeCompetencyAbility.ESCONDER_OUTROS);
        Hidden ownRoll = Hidden.fromRoll(22, true);

        Hidden ally = CompetencyActions.esconderOutros(holder, 22, FurtividadeSpecialization.MAESTRIA_DA_OCULTACAO);

        assertEquals(ownRoll.getDifficultyLevel().easier(1), ally.getDifficultyLevel());
        CharacterSheet hidden = plain();
        CompetencyActions.esconderOutros(hidden, ally);
        assertTrue(hidden.getHidden().isPresent());
    }

    @Test
    void esconderOutrosNeedsMaestriaDaOcultacaoOrInfiltrador() {
        CharacterSheet holder = holding(SkillType.FURTIVIDADE, 5, FurtividadeCompetencyAbility.ESCONDER_OUTROS);

        assertThrows(IllegalOperationException.class,
                () -> CompetencyActions.esconderOutros(holder, 22, FurtividadeSpecialization.GOLPISTA));
        assertThrows(IllegalOperationException.class, () -> CompetencyActions.esconderOutros(holder, 22, null));
    }

    // Milagreiro

    @Test
    void milagreiroHealsADescansoCurtoOncePerDescansoLongo() {
        CharacterSheet patient = plain();
        patient.applyDamage(10);
        int expected = Math.min(10, new RestServiceImpl().getRecoveredHitPoints(patient.getCharacter(), RestType.CURTO));

        assertEquals(expected, CompetencyActions.applyMilagreiro(patient, true, false));
        assertEquals(-1, CompetencyActions.applyMilagreiro(patient, true, false), "once until a Descanso Longo");
        patient.clearRestCooldowns(RestType.LONGO);
        assertTrue(CompetencyActions.canAttemptMilagreiro(patient));
    }

    @Test
    void aFailedMilagreiroStillUsesThePatientsTurn() {
        CharacterSheet patient = plain();

        assertEquals(0, CompetencyActions.applyMilagreiro(patient, false, false));
        assertFalse(CompetencyActions.canAttemptMilagreiro(patient));
    }

    @Test
    void milagreMaiorNeedsTheCorrenteMargin() {
        assertTrue(CompetencyActions.milagreMaior(InteractionResult.builder().succeeded(true).margin(5).build()));
        assertFalse(CompetencyActions.milagreMaior(InteractionResult.builder().succeeded(true).margin(4).build()));
        assertFalse(CompetencyActions.milagreMaior(InteractionResult.builder().succeeded(false).margin(9).build()));
    }

    @Test
    void theMilagreiroRollIsAgainstGdDificil() {
        CharacterSheet healer = holding(SkillType.MEDICINA_E_CURA, 5, MedicinaECuraCompetencyAbility.MILAGREIRO);

        assertTrue(CompetencyActions.rollMilagreiro(healer, null, HIGH).getSucceeded());
        assertFalse(CompetencyActions.rollMilagreiro(healer, null, LOW).getSucceeded());
    }

    // Medicina Alternativa

    @Test
    void medicinaAlternativasDieIsPaidAtTheEndOfTheNextDescanso() {
        CharacterSheet patient = plain();
        patient.applyDamage(15);
        patient.spendMagicPoints(5);
        CharacterSheet control = plain();
        control.applyDamage(15);
        control.spendMagicPoints(5);

        patient.owePendingRestBonus(4);
        new RestServiceImpl().applyRest(patient.getCharacter(), patient, RestType.CURTO);
        new RestServiceImpl().applyRest(control.getCharacter(), control, RestType.CURTO);

        assertEquals(control.getDamageTaken() - 4, patient.getDamageTaken());
        assertEquals(control.getManaSpent() - 4, patient.getManaSpent());
        assertEquals(0, patient.getPendingRestBonus(), "paid once");
    }

    @Test
    void theMedicinaAlternativaRollIsAgainstGdMedio() {
        CharacterSheet healer = holding(SkillType.MEDICINA_E_CURA, 1, MedicinaECuraCompetencyAbility.MEDICINA_ALTERNATIVA);

        assertTrue(CompetencyActions.rollMedicinaAlternativa(healer, null, HIGH).getSucceeded());
    }

    // Aliado da Natureza

    @Test
    void aTrainedCreatureIsKeptAndCalledOncePerCena() {
        CharacterSheet trainer = holding(SkillType.EMPATIA_SELVAGEM, 5, EmpatiaSelvagemCompetencyAbility.ALIADO_DA_NATUREZA);
        TrainedCompanion wolf = new TrainedCompanion("Lobo", SubordinateBenefit.CAVALEIRO_DAMAGE);

        assertTrue(CompetencyActions.trainCompanion(trainer, null, HIGH, wolf).getSucceeded());
        assertEquals(List.of(wolf), trainer.getCharacter().getTrainedCompanions());

        CompetencyActions.callCompanion(trainer, wolf, null, new SubordinateServiceImpl());
        assertFalse(CompetencyActions.canCallCompanion(trainer), "one per Cena");
        trainer.endCombat();
        assertTrue(CompetencyActions.canCallCompanion(trainer));
    }

    @Test
    void aFailedTrainingKeepsNothing() {
        CharacterSheet trainer = holding(SkillType.EMPATIA_SELVAGEM, 5, EmpatiaSelvagemCompetencyAbility.ALIADO_DA_NATUREZA);

        CompetencyActions.trainCompanion(trainer, null, LOW, new TrainedCompanion("Urso", SubordinateBenefit.TORRE_DEFESAS));

        assertTrue(trainer.getCharacter().getTrainedCompanions().isEmpty());
    }

    @Test
    void onlyACavaleiroPeaoOrTorreMayBeTrained() {
        assertThrows(IllegalArgumentException.class,
                () -> new TrainedCompanion("Rainha", SubordinateBenefit.RAINHA_INITIATIVE));
    }

    // Espalhar Emoções

    @Test
    void espalharEmocoesGrantsVantagemOnTheSameEspecializacaoForTheRestOfTheCena() {
        CharacterSheet holder = holding(SkillType.PERSUASAO, 5, PersuasaoCompetencyAbility.ESPALHAR_EMOCOES);
        PersuasaoCompetencyAbility.ESPALHAR_EMOCOES.resolveSuccessBlessings(SkillType.PERSUASAO,
                PersuasaoSpecialization.INTIMIDACAO, null).forEach(holder::grantBlessing);

        assertEquals(java.util.Optional.of(Skill.ADVANTAGE_BONUS), PersuasaoCompetencyAbility.ESPALHAR_EMOCOES
                .resolveConditionalRollBonus(SkillType.PERSUASAO, null, PersuasaoSpecialization.INTIMIDACAO, holder));
        assertTrue(PersuasaoCompetencyAbility.ESPALHAR_EMOCOES.resolveConditionalRollBonus(SkillType.PERSUASAO, null,
                PersuasaoSpecialization.COMUNICACAO, holder).isEmpty(), "another Especialização");
        for (int round = 0; round < 5; round++) {
            holder.finishTurn();
            holder.startNewRound();
        }
        assertTrue(holder.hasEffectFrom("ESPALHAR_EMOCOES:INTIMIDACAO"), "until the Cena ends");
        holder.endCombat();
        assertFalse(holder.hasEffectFrom("ESPALHAR_EMOCOES:INTIMIDACAO"));
    }

    @Test
    void negociacaoSpreadsNothing() {
        assertTrue(PersuasaoCompetencyAbility.ESPALHAR_EMOCOES.resolveSuccessBlessings(SkillType.PERSUASAO,
                PersuasaoSpecialization.NEGOCIACAO, null).isEmpty());
    }

    @Test
    void aBlessingUntilCombatEndsOutlivesItsRounds() {
        CharacterSheet sheet = plain();
        sheet.grantBlessing(new Blessing(ModifierType.SKILL_ROLL_BONUS, 1, 1, TargetScope.SELF, "teste").untilCombatEnds());
        sheet.finishTurn();
        sheet.finishTurn();

        assertTrue(sheet.hasEffectFrom("teste"));
        sheet.endCombat();
        assertFalse(sheet.hasEffectFrom("teste"));
    }

    // Espalhar Reputação

    @Test
    void espalharReputacaoIsRolledAgainstAtLeastMedio() {
        CharacterSheet performer = holding(SkillType.ARTES, 1, ArtesCompetencyAbility.ESPALHAR_REPUTACAO);

        InteractionResult easy = CompetencyActions.espalharReputacao(performer, null, HIGH, DifficultyLevel.VERY_EASY);
        InteractionResult hard = CompetencyActions.espalharReputacao(performer, null, HIGH, DifficultyLevel.HARD);

        InteractionResult medio = CompetencyActions.espalharReputacao(performer, null, HIGH, DifficultyLevel.MEDIUM);

        assertEquals(medio.getMargin(), easy.getMargin(), "a GD below Médio is raised to Médio");
        assertEquals(DifficultyLevel.HARD.getBaseValue() - DifficultyLevel.MEDIUM.getBaseValue(),
                medio.getMargin() - hard.getMargin());
    }

    @Test
    void espalharReputacaoRequiresTheAbility() {
        assertThrows(IllegalOperationException.class,
                () -> CompetencyActions.espalharReputacao(plain(), null, HIGH, DifficultyLevel.MEDIUM));
    }

    @Test
    void milagreirosMarkIsARestScopedUseThatPersists() {
        CharacterSheet patient = plain();
        CompetencyActions.applyMilagreiro(patient, false, false);

        CharacterSheet reloaded = plain();
        patient.getAllRestScopedUses().forEach((source, uses) ->
                CompetencyUses.restoreRestScopedUses(reloaded, source, uses));

        assertFalse(CompetencyActions.canAttemptMilagreiro(reloaded));
        reloaded.clearRestCooldowns(RestType.LONGO);
        assertTrue(CompetencyActions.canAttemptMilagreiro(reloaded));
    }
}
