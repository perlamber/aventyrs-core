package org.aventyrs.core.title.curandeiro;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.magic.SpellCastingServiceImpl;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.aventyrs.core.title.TitleCostPayment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.aventyrs.core.title.curandeiro.CurandeiroFixtures.bystander;
import static org.aventyrs.core.title.curandeiro.CurandeiroFixtures.holder;
import static org.aventyrs.core.util.TranslatableMessages.HIT_POINT_PAYMENT_NOT_PERMITTED;
import static org.aventyrs.core.util.TranslatableMessages.TARGET_ALREADY_AFFECTED_UNTIL_REST;
import static org.aventyrs.core.util.TranslatableMessages.TARGET_NOT_WOUNDED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_ALREADY_USED_THIS_ROUND;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_NOT_PERMITTED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_TARGET_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every Curandeiro trait other than Levantar os Caídos / Curar os Mortos (see {@code CurandeiroIntegrationTest}). */
class CurandeiroTraitsTest {

    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();
    private final RestServiceImpl restService = new RestServiceImpl();

    @BeforeEach
    void setup() {
        CurandeiroFixtures.loadTemplates();
    }

    private static InteractionResult activate(final CombatantSheet activator, final AventyrTitleAbility ability,
                                              final TitleAbilityActivationRequest.TitleAbilityActivationRequestBuilder request) {
        return Curandeiro.heldBy(activator).orElseThrow().activateAbility(ability, request.activator(activator).build());
    }

    private int pd(final CombatantSheet sheet) {
        return determinationPointsService.getCurrentDeterminationPoints(sheet.getCharacter(), sheet);
    }

    private static SceneContext outsideCombat() {
        return new SceneContext(List.of(), List.of(), Map.of(), null, false, 0, false);
    }

    private static SceneContext inCombat(final List<CombatantSheet> allies, final List<CombatantSheet> enemies) {
        Map<CombatantSheet, Range> distances = new HashMap<>();
        allies.forEach(sheet -> distances.put(sheet, Range.ADJACENTE));
        enemies.forEach(sheet -> distances.put(sheet, Range.ADJACENTE));
        return new SceneContext(new ArrayList<>(allies), new ArrayList<>(enemies), distances, null, true, 1, false);
    }

    @Nested
    class Despertar {

        private InteractionResult touch(final CombatantSheet curandeiro, final CombatantSheet target,
                                        final SceneContext context, final Object... choices) {
            DonsDoCurandeiroInteraction.Outcome outcome = null;
            List<Object> rest = new ArrayList<>();
            for (Object choice : choices) {
                if (choice instanceof DonsDoCurandeiroInteraction.Outcome o) {
                    outcome = o;
                } else {
                    rest.add(choice);
                }
            }
            return activate(curandeiro, CurandeiroDespertar.OS_DONS_DE_UM_CURANDEIRO,
                    TitleAbilityActivationRequest.builder().target(target).sceneContext(context)
                            .choice(outcome).choices(rest));
        }

        @Test
        void isAlwaysHeldButCountsTowardNoPrerequisite() {
            Curandeiro title = CurandeiroFixtures.curandeiro(List.of());

            assertEquals(CurandeiroDespertar.OS_DONS_DE_UM_CURANDEIRO, title.getAllAbilities().get(0));
            assertTrue(title.getAbilities().isEmpty());
        }

        @Test
        void grantsVantagemOnMedicinaECura() {
            int with = SkillType.MEDICINA_E_CURA.newInteraction().applyTo(holder()).getSkillRollBonus();
            int without = SkillType.MEDICINA_E_CURA.newInteraction().applyTo(CurandeiroFixtures.medic())
                    .getSkillRollBonus();

            assertEquals(Skill.ADVANTAGE_BONUS, with - without);
        }

        @Test
        void aSuccessHealsAsADescansoCurtoAndCostsThreePa() {
            CharacterSheet curandeiro = holder();
            CharacterSheet target = bystander();
            target.applyDamage(10);

            InteractionResult result = touch(curandeiro, target, null, DonsDoCurandeiroInteraction.Outcome.SUCCEEDED);

            int expected = restService.getRecoveredHitPoints(target.getCharacter(), RestType.CURTO)
                    + Curandeiro.MEDICO_DE_GUERRA_HEALING_BONUS;
            assertEquals(10 - expected, target.getDamageTaken());
            assertEquals(expected, result.getResourceGainValue());
            assertEquals(ActionCost.ofActionPoints(3), result.getActionPointCost());
        }

        @Test
        void beijoDeBorosHealsAsADescansoLongoOutsideCombatOnly() {
            CharacterSheet curandeiro = holder();
            CharacterSheet target = bystander();
            target.applyDamage(10);

            touch(curandeiro, target, outsideCombat(), DonsDoCurandeiroInteraction.Outcome.SUCCEEDED,
                    DonsDoCurandeiroInteraction.Corrente.BEIJO_DE_BOROS);

            int expected = restService.getRecoveredHitPoints(target.getCharacter(), RestType.LONGO)
                    + Curandeiro.MEDICO_DE_GUERRA_HEALING_BONUS;
            assertEquals(Math.max(0, 10 - expected), target.getDamageTaken());

            CharacterSheet other = bystander();
            other.applyDamage(5);
            IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                    () -> touch(curandeiro, other, inCombat(List.of(), List.of()),
                            DonsDoCurandeiroInteraction.Outcome.SUCCEEDED,
                            DonsDoCurandeiroInteraction.Corrente.BEIJO_DE_BOROS));
            assertEquals(TITLE_ABILITY_CHOICE_NOT_PERMITTED, refused.getMessage());
        }

        @Test
        void evenAFailureLocksTheTargetOutUntilTheCurandeirosDescansoLongo() {
            CharacterSheet curandeiro = holder();
            CharacterSheet target = bystander();
            target.applyDamage(10);

            InteractionResult failed = touch(curandeiro, target, null, DonsDoCurandeiroInteraction.Outcome.FAILED);
            assertEquals(10, target.getDamageTaken());
            assertFalse(failed.getSucceeded());

            IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                    () -> touch(curandeiro, target, null, DonsDoCurandeiroInteraction.Outcome.SUCCEEDED));
            assertEquals(TARGET_ALREADY_AFFECTED_UNTIL_REST, refused.getMessage());

            target.clearRestCooldowns(RestType.TOTAL);       // the target's rest lifts nothing
            assertThrows(IllegalOperationException.class,
                    () -> touch(curandeiro, target, null, DonsDoCurandeiroInteraction.Outcome.SUCCEEDED));

            curandeiro.clearRestCooldowns(RestType.LONGO);
            touch(curandeiro, target, null, DonsDoCurandeiroInteraction.Outcome.SUCCEEDED);
            assertTrue(target.getDamageTaken() < 10);
        }

        @Test
        void needsAWoundedTargetAndTheRollsOutcome() {
            CharacterSheet curandeiro = holder();

            IllegalOperationException unwounded = assertThrows(IllegalOperationException.class,
                    () -> touch(curandeiro, bystander(), null, DonsDoCurandeiroInteraction.Outcome.SUCCEEDED));
            assertEquals(TARGET_NOT_WOUNDED, unwounded.getMessage());

            CharacterSheet target = bystander();
            target.applyDamage(3);
            IllegalOperationException noOutcome = assertThrows(IllegalOperationException.class,
                    () -> touch(curandeiro, target, null));
            assertEquals(TITLE_ABILITY_CHOICE_REQUIRED, noOutcome.getMessage());
        }
    }

    @Nested
    class DominioDaCura {

        @Test
        void asPrimarioMedicinaECuraStandsInForDominioDoMana() {
            CharacterSheet primary = holder(List.of(), TitleSlot.PRIMARY);
            CharacterSheet secondary = holder(List.of(), TitleSlot.SECONDARY);

            assertEquals(CurandeiroFixtures.MEDICINA_E_CURA,
                    primary.getCharacter().getEffectiveGraduation(SkillType.DOMINIO_DO_MANA));
            assertEquals(0, secondary.getCharacter().getEffectiveGraduation(SkillType.DOMINIO_DO_MANA));
        }

        @Test
        void theDominioDoManaRollUsesMedicinaECura() {
            int primary = SkillType.DOMINIO_DO_MANA.newInteraction()
                    .applyTo(holder(List.of(), TitleSlot.PRIMARY)).getSkillRollBonus();
            int secondary = SkillType.DOMINIO_DO_MANA.newInteraction()
                    .applyTo(holder(List.of(), TitleSlot.SECONDARY)).getSkillRollBonus();

            assertTrue(primary > secondary, "Medicina e Cura's Graduações outweigh an untrained Domínio do Mana");
        }
    }

    @Nested
    class Especializacoes {

        @Test
        void medicoDeGuerraAddsTwoToEveryHealTheHolderMakes() {
            CharacterSheet target = bystander();
            target.applyDamage(10);

            target.heal(3, HealingSource.spell(VidaSpell.ALIVIAR_A_DOR, holder()));

            assertEquals(10 - 3 - Curandeiro.MEDICO_DE_GUERRA_HEALING_BONUS, target.getDamageTaken());
        }

        @Test
        void martirAltruistaEasesHealingMagiasThatReachOthers() {
            CharacterSheet martir = holder(List.of(CurandeiroSpecialization.MARTIR_ALTRUISTA), TitleSlot.PRIMARY);
            SpellCastingServiceImpl casting = new SpellCastingServiceImpl();

            assertEquals(Curandeiro.MARTIR_ALTRUISTA_DIFFICULTY_REDUCTION,
                    casting.resolveCastingDifficultyReduction(VidaSpell.NOVA_REJUVENESCEDORA, martir)
                            - casting.resolveCastingDifficultyReduction(VidaSpell.NOVA_REJUVENESCEDORA, bystander()));
            // "Pessoal ou Toque" reaches others through its Toque half; Toque Curativo heals no one.
            assertEquals(Curandeiro.MARTIR_ALTRUISTA_DIFFICULTY_REDUCTION,
                    casting.resolveCastingDifficultyReduction(VidaSpell.REVIGORAR, martir)
                            - casting.resolveCastingDifficultyReduction(VidaSpell.REVIGORAR, bystander()));
            assertEquals(casting.resolveCastingDifficultyReduction(VidaSpell.TOQUE_CURATIVO, bystander()),
                    casting.resolveCastingDifficultyReduction(VidaSpell.TOQUE_CURATIVO, martir));
        }
    }

    @Nested
    class CurandeiroVeloz {

        @Test
        void takesTwoPaOffTheNextHealingMagiaAndIsSpentByIt() {
            CharacterSheet curandeiro = holder(CurandeiroAbility.CURANDEIRO_VELOZ);
            SpellCastingServiceImpl casting = new SpellCastingServiceImpl();
            int before = casting.resolveActivationTime(VidaSpell.NOVA_REJUVENESCEDORA, curandeiro, 1).actionPoints();

            activate(curandeiro, CurandeiroAbility.CURANDEIRO_VELOZ, TitleAbilityActivationRequest.builder());

            assertEquals(before - CurandeiroVelozInteraction.ACTION_POINT_REDUCTION,
                    casting.resolveActivationTime(VidaSpell.NOVA_REJUVENESCEDORA, curandeiro, 1).actionPoints());
            Curandeiro.heldBy(curandeiro).orElseThrow().consumeCastingCharges(VidaSpell.NOVA_REJUVENESCEDORA, curandeiro);
            assertEquals(before,
                    casting.resolveActivationTime(VidaSpell.NOVA_REJUVENESCEDORA, curandeiro, 1).actionPoints());
        }

        @Test
        void takesTwoPaOffTheNextHealingHabilidadeDeCurandeiro() {
            CharacterSheet curandeiro = holder(CurandeiroAbility.CURANDEIRO_VELOZ);
            CharacterSheet target = bystander();
            target.applyDamage(5);
            activate(curandeiro, CurandeiroAbility.CURANDEIRO_VELOZ, TitleAbilityActivationRequest.builder());

            InteractionResult touch = activate(curandeiro, CurandeiroDespertar.OS_DONS_DE_UM_CURANDEIRO,
                    TitleAbilityActivationRequest.builder().target(target)
                            .choice(DonsDoCurandeiroInteraction.Outcome.SUCCEEDED));

            assertEquals(ActionCost.ofActionPoints(1), touch.getActionPointCost());
            assertEquals(0, curandeiro.getCharges(CurandeiroAbility.CURANDEIRO_VELOZ));
        }

        @Test
        void aMagiaThatHealsNoOneKeepsTheCharge() {
            CharacterSheet curandeiro = holder(CurandeiroAbility.CURANDEIRO_VELOZ);
            activate(curandeiro, CurandeiroAbility.CURANDEIRO_VELOZ, TitleAbilityActivationRequest.builder());

            Curandeiro.heldBy(curandeiro).orElseThrow().consumeCastingCharges(VidaSpell.TOQUE_CURATIVO, curandeiro);

            assertEquals(1, curandeiro.getCharges(CurandeiroAbility.CURANDEIRO_VELOZ));
        }

        @Test
        void isActivatedOnlyOncePerRodada() {
            CharacterSheet curandeiro = holder(CurandeiroAbility.CURANDEIRO_VELOZ);
            activate(curandeiro, CurandeiroAbility.CURANDEIRO_VELOZ, TitleAbilityActivationRequest.builder());

            IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                    () -> activate(curandeiro, CurandeiroAbility.CURANDEIRO_VELOZ, TitleAbilityActivationRequest.builder()));
            assertEquals(TITLE_ABILITY_ALREADY_USED_THIS_ROUND, refused.getMessage());

            curandeiro.tickTemporaryEffects();
            activate(curandeiro, CurandeiroAbility.CURANDEIRO_VELOZ, TitleAbilityActivationRequest.builder());
            assertEquals(2, curandeiro.getCharges(CurandeiroAbility.CURANDEIRO_VELOZ));
        }
    }

    @Nested
    class BencaoDeBoros {

        @Test
        void whileFallenItsHolderPaysDoublePdAndPm() {
            CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.CURAR_OS_MORTOS,
                    CurandeiroAbility.BENCAO_DE_BOROS);
            int max = new HitPointsServiceImpl().getMaxHitPoints(curandeiro.getCharacter(), curandeiro);
            curandeiro.applyDamage(max);
            int pdBefore = pd(curandeiro);

            InteractionResult result = Curandeiro.heldBy(curandeiro).orElseThrow().activateCurarOsMortos(
                    TitleAbilityActivationRequest.builder().activator(curandeiro).build());

            assertEquals(4, result.getDeterminationPointsSpent());
            assertEquals(pdBefore - 4, pd(curandeiro));
            assertEquals(2, Curandeiro.heldBy(curandeiro).orElseThrow()
                    .resolveManaCostMultiplier(VidaSpell.ALIVIAR_A_DOR, curandeiro));
        }

        @Test
        void theDoublingEndsWithItsWindowAndDoesNotApplyStanding() {
            CharacterSheet curandeiro = holder(CurandeiroAbility.LEVANTAR_OS_CAIDOS, CurandeiroAbility.BENCAO_DE_BOROS);
            Curandeiro title = Curandeiro.heldBy(curandeiro).orElseThrow();
            assertEquals(1, title.resolveManaCostMultiplier(VidaSpell.ALIVIAR_A_DOR, curandeiro));

            int max = new HitPointsServiceImpl().getMaxHitPoints(curandeiro.getCharacter(), curandeiro);
            curandeiro.applyDamage(max);
            for (int round = 0; round <= CurandeiroFixtures.MEDICINA_E_CURA / 2; round++) {
                curandeiro.startNewRound();
            }

            assertEquals(1, title.resolveManaCostMultiplier(VidaSpell.ALIVIAR_A_DOR, curandeiro));
        }
    }

    @Nested
    class MedicoDeGuerraHabilidades {

        @Test
        void curaProtetoraGivesBothFourDefesasOnceNotCumulatively() {
            CharacterSheet curandeiro = holder(MedicoDeGuerraAbility.CURA_PROTETORA);
            CharacterSheet target = bystander();

            activate(curandeiro, MedicoDeGuerraAbility.CURA_PROTETORA, TitleAbilityActivationRequest.builder().target(target));
            activate(curandeiro, MedicoDeGuerraAbility.CURA_PROTETORA, TitleAbilityActivationRequest.builder().target(target));

            assertEquals(CuraProtetoraInteraction.DEFESAS_BONUS, curandeiro.getTemporaryBonus(ModifierType.DEFESAS));
            assertEquals(CuraProtetoraInteraction.DEFESAS_BONUS, target.getTemporaryBonus(ModifierType.DEFESAS));
        }

        @Test
        void encantoRegenerativoHealsTheAllyItsVigor() {
            CharacterSheet curandeiro = holder(MedicoDeGuerraAbility.ENCANTO_REGENERATIVO);
            CharacterSheet ally = bystander();
            ally.applyDamage(10);

            activate(curandeiro, MedicoDeGuerraAbility.ENCANTO_REGENERATIVO,
                    TitleAbilityActivationRequest.builder().target(ally));

            int vigor = ally.getCharacter().getEffectiveAttributeTotal(AttributeDomain.VIGOR);
            assertEquals(10 - vigor - Curandeiro.MEDICO_DE_GUERRA_HEALING_BONUS, ally.getDamageTaken());
        }

        @Test
        void encantoRegenerativoNeedsAnAlly() {
            CharacterSheet curandeiro = holder(MedicoDeGuerraAbility.ENCANTO_REGENERATIVO);

            IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                    () -> activate(curandeiro, MedicoDeGuerraAbility.ENCANTO_REGENERATIVO,
                            TitleAbilityActivationRequest.builder()));
            assertEquals(TITLE_ABILITY_TARGET_REQUIRED, refused.getMessage());
        }

        @Test
        void doutorDeEldurUpgradesTheHoldersRests() {
            CharacterSheet curandeiro = holder(MedicoDeGuerraAbility.DOUTOR_DE_ELDUR);
            curandeiro.applyDamage(12);

            restService.applyRest(curandeiro.getCharacter(), curandeiro, RestType.CURTO);

            int asLongo = restService.getRecoveredHitPoints(curandeiro.getCharacter(), RestType.LONGO);
            assertEquals(Math.max(0, 12 - asLongo), curandeiro.getDamageTaken());
        }

        @Test
        void doutorDeEldurTakesOnePaOffEveryHealingMagia() {
            SpellCastingServiceImpl casting = new SpellCastingServiceImpl();

            assertEquals(casting.resolveActivationTime(VidaSpell.REVIGORAR_MAIOR, bystander(), 1).actionPoints()
                            - Curandeiro.DOUTOR_DE_ELDUR_ACTION_POINT_REDUCTION,
                    casting.resolveActivationTime(VidaSpell.REVIGORAR_MAIOR,
                            holder(MedicoDeGuerraAbility.DOUTOR_DE_ELDUR), 1).actionPoints());
        }

        @Test
        void tratamentoFurtivoIsPassive() {
            assertTrue(MedicoDeGuerraAbility.TRATAMENTO_FURTIVO.isPassive());
        }
    }

    @Nested
    class MartirAltruistaHabilidades {

        private CharacterSheet martir(final AventyrTitleAbility... abilities) {
            return holder(List.of(CurandeiroSpecialization.MEDICO_DE_GUERRA, CurandeiroSpecialization.MARTIR_ALTRUISTA),
                    TitleSlot.PRIMARY, abilities);
        }

        @Test
        void transferirVitalidadePaysPdInLockedPv() {
            CharacterSheet curandeiro = martir(MartirAltruistaAbility.TRANSFERIR_VITALIDADE,
                    MedicoDeGuerraAbility.CURA_PROTETORA);
            CharacterSheet ally = bystander();
            int pdBefore = pd(curandeiro);

            InteractionResult result = activate(curandeiro, MedicoDeGuerraAbility.CURA_PROTETORA,
                    TitleAbilityActivationRequest.builder().target(ally).choices(Set.of(TitleCostPayment.HIT_POINTS)));

            assertEquals(pdBefore, pd(curandeiro));
            assertEquals(2, result.getResourceLossValue());
            assertEquals(2, curandeiro.getLockedDamage());

            curandeiro.heal(10);
            assertEquals(2, curandeiro.getDamageTaken(), "only a Descanso Verdadeiro recovers it");
            restService.applyRest(curandeiro.getCharacter(), curandeiro, RestType.TOTAL, true);
            assertEquals(0, curandeiro.getDamageTaken());
        }

        @Test
        void transferirVitalidadeNeedsTheHabilidadeAndAnAlly() {
            CharacterSheet without = martir(MedicoDeGuerraAbility.CURA_PROTETORA);
            IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                    () -> activate(without, MedicoDeGuerraAbility.CURA_PROTETORA, TitleAbilityActivationRequest.builder()
                            .target(bystander()).choices(Set.of(TitleCostPayment.HIT_POINTS))));
            assertEquals(HIT_POINT_PAYMENT_NOT_PERMITTED, refused.getMessage());

            CharacterSheet with = martir(MartirAltruistaAbility.TRANSFERIR_VITALIDADE, MedicoDeGuerraAbility.CURA_PROTETORA);
            assertThrows(IllegalOperationException.class,
                    () -> activate(with, MedicoDeGuerraAbility.CURA_PROTETORA, TitleAbilityActivationRequest.builder()
                            .choices(Set.of(TitleCostPayment.HIT_POINTS))));
        }

        @Test
        void transferirVitalidadeCoversSingleTargetDivineOrNaturalMagiasOnOthers() {
            CharacterSheet curandeiro = martir(MartirAltruistaAbility.TRANSFERIR_VITALIDADE);
            Curandeiro title = Curandeiro.heldBy(curandeiro).orElseThrow();

            // Revigorar is "Pessoal ou Toque" — single-target through its Toque half, and Vida is Natural/Divina.
            assertTrue(title.permitsHitPointPayment(VidaSpell.REVIGORAR, curandeiro, bystander()));
            assertFalse(title.permitsHitPointPayment(VidaSpell.REVIGORAR, curandeiro, curandeiro));
            assertFalse(title.permitsHitPointPayment(VidaSpell.NOVA_REJUVENESCEDORA, curandeiro, bystander()));
        }

        @Test
        void transferirDeterminacaoGivesTheWholeAmountAndTheAllyKeepsWhatItCanHold() {
            CharacterSheet curandeiro = martir(MartirAltruistaAbility.TRANSFERIR_DETERMINACAO);
            CharacterSheet ally = bystander();
            ally.spendDeterminationPoints(2);
            int pdBefore = pd(curandeiro);

            InteractionResult result = activate(curandeiro, MartirAltruistaAbility.TRANSFERIR_DETERMINACAO,
                    TitleAbilityActivationRequest.builder().target(ally)
                            .choice(TransferenciaInteraction.Mode.POINTS).choices(List.of(3)));

            assertEquals(2, result.getResourceGainValue());
            assertEquals(0, ally.getDeterminationSpent());
            assertEquals(pdBefore - 1 - 3, pd(curandeiro), "the third PD had nowhere to go, and is lost");
        }

        @Test
        void anUnusedEgoLoanGoesBackToItsLenderAtTheEndOfTheCena() {
            CharacterSheet curandeiro = martir(MartirAltruistaAbility.TRANSFERIR_DETERMINACAO);
            CharacterSheet ally = bystander();
            int lenderBefore = curandeiro.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE);
            int allyBefore = ally.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE);

            activate(curandeiro, MartirAltruistaAbility.TRANSFERIR_DETERMINACAO,
                    TitleAbilityActivationRequest.builder().target(ally).choice(TransferenciaInteraction.Mode.EGO_POINT));
            assertEquals(lenderBefore - 1, curandeiro.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
            assertEquals(allyBefore + 1, ally.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));

            ally.startNewScene();

            assertEquals(allyBefore, ally.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
            assertEquals(lenderBefore, curandeiro.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        }

        @Test
        void aUsedEgoLoanIsGoneAndCostsTheAllyNothingOfItsOwn() {
            CharacterSheet curandeiro = martir(MartirAltruistaAbility.TRANSFERIR_ESSENCIA);
            CharacterSheet ally = bystander();
            int lenderBefore = curandeiro.getTemporaryEgoPoints(EgoDomain.SORTE);
            int allyBefore = ally.getTemporaryEgoPoints(EgoDomain.SORTE);
            activate(curandeiro, MartirAltruistaAbility.TRANSFERIR_ESSENCIA,
                    TitleAbilityActivationRequest.builder().target(ally).choice(TransferenciaInteraction.Mode.EGO_POINT));

            ally.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 1);
            ally.startNewScene();

            assertEquals(allyBefore, ally.getTemporaryEgoPoints(EgoDomain.SORTE));
            assertEquals(lenderBefore - 1, curandeiro.getTemporaryEgoPoints(EgoDomain.SORTE));
        }

        @Test
        void transferirRancorArmsEveryAllyCumulativelyForARodada() {
            CharacterSheet curandeiro = martir(MartirAltruistaAbility.TRANSFERIR_RANCOR);
            CharacterSheet ally = bystander();
            CharacterSheet enemy = bystander();
            SceneContext own = inCombat(List.of(ally), List.of(enemy));
            DamageServiceImpl damageService = new DamageServiceImpl();

            damageService.notifyDamageTaken(curandeiro, 3, enemy, own);
            damageService.notifyDamageTaken(curandeiro, 3, enemy, own);

            assertEquals(2, ally.getTemporaryBonus(ModifierType.ATTACK_AND_CONJURATION_DIFFICULTY_REDUCTION));
            assertEquals(2, ally.getTemporaryBonus(ModifierType.EXTRA_DAMAGE_DICE));
            assertEquals(0, enemy.getTemporaryBonus(ModifierType.EXTRA_DAMAGE_DICE));
            InteractionResult attack = SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(ally);
            assertEquals(2, attack.getExtraDamageDice());
        }

        @Test
        void transferirRancorResolvesAlliesFromTheAttackersSnapshotToo() {
            CharacterSheet curandeiro = martir(MartirAltruistaAbility.TRANSFERIR_RANCOR);
            CharacterSheet ally = bystander();
            CharacterSheet enemy = bystander();

            new DamageServiceImpl().notifyDamageTaken(curandeiro, 3, enemy,
                    inCombat(List.of(), List.of(curandeiro, ally)));

            assertEquals(1, ally.getTemporaryBonus(ModifierType.EXTRA_DAMAGE_DICE));
        }

        @Test
        void transferirRancorIsSilentInACombatCenaWhereTheHolderDealtDamage() {
            CharacterSheet curandeiro = martir(MartirAltruistaAbility.TRANSFERIR_RANCOR);
            CharacterSheet ally = bystander();
            CharacterSheet enemy = bystander();
            DamageServiceImpl damageService = new DamageServiceImpl();
            damageService.notifyDamageTaken(enemy, 2, curandeiro, inCombat(List.of(), List.of(curandeiro)));
            assertTrue(curandeiro.hasDealtDamageThisScene());

            damageService.notifyDamageTaken(curandeiro, 3, enemy, inCombat(List.of(ally), List.of(enemy)));

            assertEquals(0, ally.getTemporaryBonus(ModifierType.EXTRA_DAMAGE_DICE));
            assertNull(SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(ally).getExtraDamageDice());
        }
    }
}
