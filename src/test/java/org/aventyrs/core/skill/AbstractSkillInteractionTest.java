package org.aventyrs.core.skill;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.ability.CharismaAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.ability.DexterityAbility;
import org.aventyrs.core.ego.InitiativeAdvantage;
import org.aventyrs.core.ego.SorteAdvantage;
import org.aventyrs.core.race.Anao;
import org.aventyrs.core.race.AnoesRacialAbility;
import org.aventyrs.core.race.Elfo;
import org.aventyrs.core.race.ElfosRacialAbility;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.artes.ArtesAprimorarComArteAbility;
import org.aventyrs.core.skill.artes.ArtesCompetencyAbility;
import org.aventyrs.core.skill.artes.ArtesSpecialization;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaInteraction;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.aventyrs.core.skill.attention.AttentionCompetencyAbility;
import org.aventyrs.core.skill.attention.AttentionInteraction;
import org.aventyrs.core.skill.attention.AttentionSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Demonstrates the extension point {@code AbstractSkillInteraction#applyTo(CombatantSheet,
 * SceneContext)} exists for: a "gang up" style ability whose bonus scales with how many
 * allies are within a given {@link Range}, clamped to a maximum — the shape a real ability
 * like that would take, using a private test-only subclass (no such ability currently exists
 * in the ruleset) the same way {@code DamageServiceImplTest} uses test-only ability classes to
 * exercise a generic mechanism without needing a real named consumer yet. Which skill this
 * hypothetical ability is attached to is arbitrary — Atletismo here — nothing about it is
 * Atletismo-specific.
 */
class AbstractSkillInteractionTest {

    private static final int PER_ALLY_BONUS = 1;
    private static final int MAX_BONUS = 3;

    private static class GangUpBonusInteraction extends AbstractSkillInteraction {
        GangUpBonusInteraction() {
            super(SkillType.ATLETISMO);
        }

        @Override
        public InteractionResult applyTo(final CombatantSheet target, final SceneContext sceneContext) {
            InteractionResult result = super.applyTo(target, sceneContext);
            if (sceneContext == null) {
                return result;
            }
            int gangUpBonus = Math.min(sceneContext.countAlliesWithin(Range.DISTANCIA_CURTA) * PER_ALLY_BONUS, MAX_BONUS);
            return result.toBuilder()
                    .skillRollBonus(result.getSkillRollBonus() + gangUpBonus)
                    .build();
        }
    }

    private final GangUpBonusInteraction interaction = new GangUpBonusInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet actingSheet(final int strengthBase) {
        CharacterSkill atletismoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATLETISMO_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(strengthBase).build())
                        .build())
                .skill(SkillType.ATLETISMO, atletismoSkill)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void applyToWithNoSceneContextOmitsTheCountScaledBonus() {
        CharacterSheet sheet = actingSheet(2);

        InteractionResult result = interaction.applyTo(sheet);

        assertEquals(2, result.getSkillRollBonus());
    }

    @Test
    void applyToScalesTheBonusByHowManyAlliesAreWithinRange() {
        CharacterSheet sheet = actingSheet(2);
        CharacterSheet adjacentAlly = newSheet();
        CharacterSheet curtaAlly = newSheet();
        SceneContext sceneContext = new SceneContext(
                List.of(adjacentAlly, curtaAlly), List.of(),
                Map.of(adjacentAlly, Range.ADJACENTE, curtaAlly, Range.DISTANCIA_CURTA));

        InteractionResult result = interaction.applyTo(sheet, sceneContext);

        assertEquals(4, result.getSkillRollBonus()); // 2 (base) + 2 allies * 1
    }

    @Test
    void applyToClampsTheCountScaledBonusAtItsMaximum() {
        CharacterSheet sheet = actingSheet(2);
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        CharacterSheet c = newSheet();
        CharacterSheet d = newSheet();
        SceneContext sceneContext = new SceneContext(
                List.of(a, b, c, d), List.of(),
                Map.of(a, Range.ADJACENTE, b, Range.ADJACENTE, c, Range.ADJACENTE, d, Range.ADJACENTE));

        InteractionResult result = interaction.applyTo(sheet, sceneContext);

        assertEquals(5, result.getSkillRollBonus()); // 2 (base) + clamp(4, 3)
    }

    @Test
    void applyToIgnoresAlliesOutsideTheRelevantRange() {
        CharacterSheet sheet = actingSheet(2);
        CharacterSheet farAlly = newSheet();
        SceneContext sceneContext = new SceneContext(
                List.of(farAlly), List.of(), Map.of(farAlly, Range.DISTANCIA_MUITO_LONGA));

        InteractionResult result = interaction.applyTo(sheet, sceneContext);

        assertEquals(2, result.getSkillRollBonus());
    }

    @Test
    void applyToIgnoresEnemyCountForThisAllyScopedBonus() {
        CharacterSheet sheet = actingSheet(2);
        CharacterSheet adjacentEnemy = newSheet();
        SceneContext sceneContext = new SceneContext(
                List.of(), List.of(adjacentEnemy), Map.of(adjacentEnemy, Range.ADJACENTE));

        InteractionResult result = interaction.applyTo(sheet, sceneContext);

        assertEquals(2, result.getSkillRollBonus());
    }

    /**
     * {@link SkillRoll#getRequestedAbility()} validation applies to every skill generically
     * (it's implemented once, in {@code AbstractSkillInteraction} itself) — exercised here via
     * plain {@link AttentionInteraction} rather than the ATLETISMO-flavored {@code
     * GangUpBonusInteraction} above, since it's unrelated to the SceneContext extension point
     * this file otherwise demonstrates.
     */
    private CharacterSheet attentionSheetHolding(final SkillCompetencyAbility... abilities) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK);
        for (SkillCompetencyAbility ability : abilities) {
            builder.skillCompetencyAbility(ability);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToWithNoRequestedAbilitySkipsValidationEntirely() {
        CharacterSheet sheet = attentionSheetHolding();
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4));

        InteractionResult result = new AttentionInteraction().applyTo(sheet, null, skillRoll);

        assertNotNull(result);
    }

    @Test
    void applyToWithARequestedAbilityTheCharacterHoldsSucceeds() {
        CharacterSheet sheet = attentionSheetHolding(AttentionCompetencyAbility.PERCEPCAO_DE_FOXM);
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), AttentionCompetencyAbility.PERCEPCAO_DE_FOXM);

        InteractionResult result = new AttentionInteraction().applyTo(sheet, null, skillRoll);

        assertNotNull(result);
    }

    @Test
    void applyToWithARequestedAbilityTheCharacterLacksThrows() {
        CharacterSheet sheet = attentionSheetHolding();
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), AttentionCompetencyAbility.PERCEPCAO_DE_FOXM);
        AttentionInteraction attentionInteraction = new AttentionInteraction();

        assertThrows(IllegalOperationException.class, () -> attentionInteraction.applyTo(sheet, null, skillRoll));
    }

    @Test
    void applyToWithARequestedAbilityFromADifferentSkillTypeThrowsEvenWhenHeld() {
        // DOM_BARDICO belongs to ARTES, not ATTENTION — holding it doesn't make it a valid
        // request on an AttentionInteraction roll.
        CharacterSheet sheet = attentionSheetHolding(ArtesCompetencyAbility.DOM_BARDICO);
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), ArtesCompetencyAbility.DOM_BARDICO);
        AttentionInteraction attentionInteraction = new AttentionInteraction();

        assertThrows(IllegalOperationException.class, () -> attentionInteraction.applyTo(sheet, null, skillRoll));
    }

    /**
     * A racial ability (see CLAUDE.md's "Racial Abilities reuse SkillCompetencyAbility"
     * section) is held via {@code character.getRace().getRacialAbilities()}, not {@code
     * skillCompetencyAbilities} — {@code validateRequestedAbility} must check both, the same
     * "held" test a {@code SkillCompetencyAbility} would need to pass.
     */
    @Test
    void applyToWithARequestedRacialAbilityTheCharacterHoldsSucceeds() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new Elfo())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), ElfosRacialAbility.SENTIDOS_ABSOLUTOS);

        InteractionResult result = new AttentionInteraction().applyTo(sheet, null, skillRoll);

        assertNotNull(result);
    }

    /**
     * {@code ABATEDORES_DE_GIGANTES#getSkillType()} reports ATAQUE_A_DISTANCIA as its
     * canonical value, but its rules text covers every Perícia de Ataque — {@link
     * SkillTrait#matchesSkillType} is what lets it validate as a requested ability against
     * ATAQUE_CORPO_A_CORPO too, not just its own canonical SkillType.
     */
    @Test
    void applyToWithARequestedRacialAbilityMatchesTheOtherAttackSkillTypeToo() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new Anao())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), AnoesRacialAbility.ABATEDORES_DE_GIGANTES);

        InteractionResult result = new AtaqueCorpoACorpoInteraction().applyTo(sheet, null, skillRoll);

        assertNotNull(result);
    }

    /**
     * Unlike a {@code SkillCompetencyAbility} (held on {@code Character
     * .skillCompetencyAbilities}, a single list for the whole character), a {@code
     * SkillSpecialization} is held on the specific {@code CharacterSkill.getSpecializations()}
     * for that Perícia — so exercising it needs an actually-trained ATTENTION {@code
     * CharacterSkill}, not just {@code attentionSheetHolding}'s untrained fallback.
     */
    private CharacterSheet attentionSheetHoldingSpecializations(final SkillSpecialization... specializations) {
        CharacterSkill attentionSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1)
                .specializations(List.of(specializations))
                .build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATTENTION, attentionSkill)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void applyToWithARequestedSpecializationTheCharacterHoldsSucceeds() {
        CharacterSheet sheet = attentionSheetHoldingSpecializations(AttentionSpecialization.INVESTIGAR);
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), AttentionSpecialization.INVESTIGAR);

        InteractionResult result = new AttentionInteraction().applyTo(sheet, null, skillRoll);

        assertNotNull(result);
    }

    @Test
    void applyToWithARequestedSpecializationTheCharacterLacksThrows() {
        CharacterSheet sheet = attentionSheetHoldingSpecializations();
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), AttentionSpecialization.INVESTIGAR);
        AttentionInteraction attentionInteraction = new AttentionInteraction();

        assertThrows(IllegalOperationException.class, () -> attentionInteraction.applyTo(sheet, null, skillRoll));
    }

    /**
     * {@code CharacterSkill} never validates that a listed specialization actually belongs to
     * its own skill (see CLAUDE.md), so this ATTENTION {@code CharacterSkill} can hold an
     * {@code ArtesSpecialization} it has no business holding — {@code validateRequestedTrait}'s
     * own {@code getSkillType() != skillType} check still rejects requesting it on an
     * {@code AttentionInteraction} roll regardless.
     */
    @Test
    void applyToWithARequestedSpecializationFromADifferentSkillTypeThrowsEvenWhenHeld() {
        CharacterSheet sheet = attentionSheetHoldingSpecializations(ArtesSpecialization.MUSICA);
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), ArtesSpecialization.MUSICA);
        AttentionInteraction attentionInteraction = new AttentionInteraction();

        assertThrows(IllegalOperationException.class, () -> attentionInteraction.applyTo(sheet, null, skillRoll));
    }

    /**
     * INSTINCT total 1 ({@code AttributeValue}'s own untouched default: {@code base} defaults
     * to 1, {@code racialBonus}/{@code variable} to 0) + graduation 0 (reset by {@code
     * CharacterSkillFixture#blank}) gives a fixed {@code skillRollBonus} of 1; dice (6, 5, 5)
     * sum to 16, for a combined total of 17. 17 clears {@code DifficultyLevel.EASY}'s baseValue
     * (14) but not {@code MEDIUM}'s (18) — reached as a plain roll. As an expert (requesting a
     * held Especialização), 17 also clears {@code MEDIUM}'s easier expertValue (16), reaching a
     * whole tier higher for the identical dice and bonus.
     */
    @Test
    void applyToWithAHeldSpecializationRequestedUsesExpertThresholds() {
        CharacterSheet sheet = attentionSheetHoldingSpecializations(AttentionSpecialization.INVESTIGAR);
        SkillRoll plainRoll = new SkillRoll(List.of(6, 5, 5));
        SkillRoll expertRoll = new SkillRoll(List.of(6, 5, 5), AttentionSpecialization.INVESTIGAR);

        InteractionResult plainResult = new AttentionInteraction().applyTo(sheet, null, plainRoll);
        InteractionResult expertResult = new AttentionInteraction().applyTo(sheet, null, expertRoll);

        assertEquals(DifficultyLevel.EASY, plainResult.getReachedDifficultyLevel());
        assertEquals(DifficultyLevel.MEDIUM, expertResult.getReachedDifficultyLevel());
    }

    private CharacterSheet attentionSheetHoldingAttributeAbility(final AttributeAbility... abilities) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK);
        for (AttributeAbility ability : abilities) {
            builder.attributeAbility(ability);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    /**
     * {@code CharismaAbility#DESTINO_FAVORAVEL}'s reactive half: a Sucesso Crítico Maior
     * (three 6s) on any Perícia roll directly grants a non-cumulative temporary point in
     * Sorte and Autocontrole (the roll's own target is unambiguous, unlike DOM_BARDICO's
     * ally-scoped bonus, so this is applied immediately rather than merely reported) and
     * reports the granted domains on {@code egoGainDomains} — exercised generically, through
     * {@link AttentionInteraction}, since this is computed once in {@code
     * AbstractSkillInteraction} for every skill, not per-Interaction.
     */
    @Test
    void applyToGrantsAndReportsEgoGainDomainsOnDestinoFavoravelMajorCriticalSuccess() {
        CharacterSheet sheet = attentionSheetHoldingAttributeAbility(CharismaAbility.DESTINO_FAVORAVEL);
        SkillRoll skillRoll = new SkillRoll(List.of(6, 6, 6));

        InteractionResult result = new AttentionInteraction().applyTo(sheet, null, skillRoll);

        assertEquals(List.of(EgoDomain.SORTE, EgoDomain.AUTOCONTROLE), result.getEgoGainDomains());
        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    @Test
    void applyToRepeatedDestinoFavoravelTriggersDoNotStackPastOnePoint() {
        CharacterSheet sheet = attentionSheetHoldingAttributeAbility(CharismaAbility.DESTINO_FAVORAVEL);
        SkillRoll skillRoll = new SkillRoll(List.of(6, 6, 6));
        AttentionInteraction attentionInteraction = new AttentionInteraction();

        attentionInteraction.applyTo(sheet, null, skillRoll);
        attentionInteraction.applyTo(sheet, null, skillRoll);

        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    /**
     * DESTINO_FAVORAVEL's own "não cumulativo" grant only caps *its own* repeat triggers —
     * see {@code TemporaryPointPool#gainNonCumulative}. Sorte points already held from an
     * unrelated source aren't clamped down by it, and still add on top normally.
     */
    @Test
    void applyToDestinoFavoravelStacksOnTopOfSortePointsFromAnUnrelatedSource() {
        CharacterSheet sheet = attentionSheetHoldingAttributeAbility(CharismaAbility.DESTINO_FAVORAVEL);
        sheet.gainTemporaryEgoPoints(EgoDomain.SORTE, 2);
        SkillRoll skillRoll = new SkillRoll(List.of(6, 6, 6));

        new AttentionInteraction().applyTo(sheet, null, skillRoll);

        assertEquals(3, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void applyToLeavesEgoGainDomainsNullOnANonCriticalRoll() {
        CharacterSheet sheet = attentionSheetHoldingAttributeAbility(CharismaAbility.DESTINO_FAVORAVEL);
        SkillRoll skillRoll = new SkillRoll(List.of(3, 4, 5));

        InteractionResult result = new AttentionInteraction().applyTo(sheet, null, skillRoll);

        assertNull(result.getEgoGainDomains());
    }

    @Test
    void applyToLeavesEgoGainDomainsNullWithoutDestinoFavoravelEvenOnMajorCriticalSuccess() {
        CharacterSheet sheet = attentionSheetHoldingAttributeAbility();
        SkillRoll skillRoll = new SkillRoll(List.of(6, 6, 6));

        InteractionResult result = new AttentionInteraction().applyTo(sheet, null, skillRoll);

        assertNull(result.getEgoGainDomains());
    }

    private CharacterSheet sheetHoldingImpeto() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.INICIATIVA, InitiativeAdvantage.IMPETO)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private SceneContext combatContext(final int currentRound, final boolean wonInitiative) {
        return new SceneContext(List.of(), List.of(), Map.of(), null, true, currentRound, wonInitiative);
    }

    /**
     * {@code EgoAdvantage#resolveConditionalRollBonus} (e.g. {@code InitiativeAdvantage#IMPETO})
     * is summed generically for *every* skill via {@code AbstractSkillInteraction
     * #sumEgoAdvantageRollBonuses} — exercised here through plain {@link AttentionInteraction},
     * a skill wholly unrelated to Iniciativa, to prove it reaches the real {@code
     * skillRollBonus} sum rather than just IMPETO's own isolated {@code
     * resolveConditionalRollBonus} call (see {@code InitiativeAdvantageTest} for that
     * unit-level coverage).
     */
    @Test
    void applyToAddsImpetosVantagemToAnySkillRollDuringTheFirstTwoRoundsOfACombatScene() {
        CharacterSheet sheet = sheetHoldingImpeto();

        InteractionResult roundOne = new AttentionInteraction().applyTo(sheet, combatContext(1, false));
        InteractionResult roundTwo = new AttentionInteraction().applyTo(sheet, combatContext(2, false));

        // Instinto base(1, untouched default) + UNTRAINED_PENALTY(-2) + IMPETO's Vantagem(2) = 1.
        assertEquals(1, roundOne.getSkillRollBonus());
        assertEquals(1, roundTwo.getSkillRollBonus());
    }

    @Test
    void applyToOmitsImpetosVantagemPastTheFirstTwoRoundsOrOutsideACombatScene() {
        CharacterSheet sheet = sheetHoldingImpeto();
        SceneContext nonCombat = new SceneContext(List.of(), List.of(), Map.of(), null, false, 1, false);

        InteractionResult roundThree = new AttentionInteraction().applyTo(sheet, combatContext(3, false));
        InteractionResult outsideCombat = new AttentionInteraction().applyTo(sheet, nonCombat);
        InteractionResult noContext = new AttentionInteraction().applyTo(sheet);

        assertEquals(-1, roundThree.getSkillRollBonus());
        assertEquals(-1, outsideCombat.getSkillRollBonus());
        assertEquals(-1, noContext.getSkillRollBonus());
    }

    /**
     * {@code EgoAdvantage#resolveDamageBonus} is resolved generically for every attack-skill
     * roll — exercised here through {@link AtaqueCorpoACorpoInteraction} specifically (not
     * Ataque à Distância) to prove IMPETO's dano bonus reaches Ataque Corpo a Corpo too, unlike
     * {@code AtaqueADistanciaCompetencyAbility#FRIEZA}'s own dano bonus, which CLAUDE.md notes
     * is still unwired for melee.
     */
    @Test
    void applyToAddsImpetosDamageBonusOnAnAttackSkillRollWhenInitiativeWasWon() {
        CharacterSheet sheet = sheetHoldingImpeto();

        InteractionResult result = new AtaqueCorpoACorpoInteraction().applyTo(sheet, combatContext(1, true));

        assertEquals(Skill.ADVANTAGE_BONUS, result.getDamageBonus().getValue());
    }

    @Test
    void applyToOmitsImpetosDamageBonusWhenInitiativeWasNotWon() {
        CharacterSheet sheet = sheetHoldingImpeto();

        InteractionResult result = new AtaqueCorpoACorpoInteraction().applyTo(sheet, combatContext(1, false));

        assertNull(result.getDamageBonus());
    }

    @Test
    void applyToOmitsImpetosDamageBonusForANonAttackSkillEvenWhenInitiativeWasWon() {
        CharacterSheet sheet = sheetHoldingImpeto();

        InteractionResult result = new AttentionInteraction().applyTo(sheet, combatContext(1, true));

        assertNull(result.getDamageBonus());
    }

    private CharacterSheet sheetHoldingLetalidadeProgressiva() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(DexterityAbility.LETALIDADE_PROGRESSIVA)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    /**
     * {@code DexterityAbility#LETALIDADE_PROGRESSIVA} widens Acerto Crítico Menor's margin for
     * Ataque à Distância during a Cena de Combate's early Rounds — exercised end-to-end through
     * {@link AtaqueADistanciaInteraction} (this ability's own Perícia) to prove {@code
     * AbstractSkillInteraction#sumCriticalMarginIncrease} actually reaches {@code
     * SkillRoll#getCriticalResult(int)}, not just {@code DexterityAbilityTest}'s own
     * isolated {@code resolveCriticalMarginIncrease} coverage. 6+5+2 has only one 6 — not
     * Acerto Crítico Menor at margin 0 — but with the +1 margin this ability grants in Round 1,
     * the 5 now counts alongside the 6 for the qualifying pair.
     */
    @Test
    void applyToWidensAcertoCriticoMenorViaLetalidadeProgressivaDuringCombat() {
        CharacterSheet sheet = sheetHoldingLetalidadeProgressiva();
        SkillRoll roll = new SkillRoll(List.of(6, 5, 2));
        SceneContext combatRoundOne = new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false);

        InteractionResult withoutContext = new AtaqueADistanciaInteraction().applyTo(sheet, null, roll);
        InteractionResult duringCombat = new AtaqueADistanciaInteraction().applyTo(sheet, combatRoundOne, roll);

        assertEquals(CriticalResult.NONE, withoutContext.getCriticalResult());
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, duringCombat.getCriticalResult());
    }

    private CharacterSheet sheetHoldingAceAndArtesMarginSources() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.SORTE, SorteAdvantage.ACE)
                .skillCompetencyAbility(new ArtesAprimorarComArteAbility(SkillType.ATLETISMO))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    /**
     * {@code EgoAdvantage#resolveCriticalMarginIncrease} ({@code SorteAdvantage#ACE}) and
     * {@code SkillCompetencyAbility#resolveCriticalMarginIncrease} ({@code
     * ArtesAprimorarComArteAbility}'s "Margem Crítica Menor" branch) are meant to be additive —
     * exercised together, through the ATLETISMO-flavored {@code GangUpBonusInteraction} above,
     * to prove {@code sumCriticalMarginIncrease} actually sums across sources rather than only
     * ever picking up one. 3+3+1 needs a margin of at least 4 to read as Acerto Crítico Menor
     * (widening the qualifying face down to 2, so both 3s count): ACE alone only grants its
     * +3 outside a Cena de Combate for a non-Ataque skill (ATLETISMO qualifies), and Artes'
     * branch alone only grants +1 — neither alone reaches 4, but together they do.
     */
    @Test
    void applyToSumsCriticalMarginIncreaseAcrossAllThreeAbilitySources() {
        CharacterSheet sheet = sheetHoldingAceAndArtesMarginSources();
        SkillRoll roll = new SkillRoll(List.of(3, 3, 1));
        SceneContext nonCombatContext = new SceneContext(List.of(), List.of(), Map.of(), null, false, 0, false);

        InteractionResult withoutContext = interaction.applyTo(sheet, null, roll);
        InteractionResult withNonCombatContext = interaction.applyTo(sheet, nonCombatContext, roll);

        assertEquals(CriticalResult.NONE, withoutContext.getCriticalResult());
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, withNonCombatContext.getCriticalResult());
    }

    private CharacterSheet sheetHoldingPrecisao() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(DexterityAbility.PRECISAO)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    /**
     * {@code DexterityAbility#PRECISAO} grants Vantagem only on the first Destreza-based
     * Perícia roll each of the holder's own Turns — exercised end-to-end through {@link
     * AtaqueADistanciaInteraction} (a Destreza-based Perícia, see {@code AtaqueADistancia}'s
     * own {@code AttributeDomain}) to prove {@code AbstractSkillInteraction
     * #sumFirstRollOfTurnBonuses} actually reaches {@code skillRollBonus}, correctly gated by
     * {@code CharacterSheet#consumeFirstRollThisTurn}: a second roll on the same, still-active
     * Turn doesn't repeat the bonus, but {@code startTurn} resets it for the next one.
     */
    @Test
    void applyToGrantsPrecisaosVantagemOnlyOnceUntilTheNextTurnStarts() {
        CharacterSheet sheet = sheetHoldingPrecisao();
        SkillRoll roll = new SkillRoll(List.of(2, 3, 4));

        InteractionResult firstRoll = new AtaqueADistanciaInteraction().applyTo(sheet, null, roll);
        InteractionResult secondRollSameTurn = new AtaqueADistanciaInteraction().applyTo(sheet, null, roll);
        sheet.startTurn(1);
        InteractionResult firstRollNextTurn = new AtaqueADistanciaInteraction().applyTo(sheet, null, roll);

        assertEquals(secondRollSameTurn.getSkillRollBonus() + Skill.ADVANTAGE_BONUS, firstRoll.getSkillRollBonus());
        assertEquals(firstRoll.getSkillRollBonus(), firstRollNextTurn.getSkillRollBonus());
    }

    /**
     * {@code AtaqueCorpoACorpo}'s own {@code AttributeDomain} is Força, not Destreza — even as
     * this same character's genuinely-first roll of the Turn, PRECISAO grants nothing for it.
     */
    @Test
    void applyToOmitsPrecisaosVantagemForANonDestrezaBasedRollEvenAsTheFirstOfTheTurn() {
        CharacterSheet sheet = sheetHoldingPrecisao();
        SkillRoll roll = new SkillRoll(List.of(2, 3, 4));

        InteractionResult firstRoll = new AtaqueCorpoACorpoInteraction().applyTo(sheet, null, roll);
        InteractionResult secondRoll = new AtaqueCorpoACorpoInteraction().applyTo(sheet, null, roll);

        assertEquals(secondRoll.getSkillRollBonus(), firstRoll.getSkillRollBonus());
    }
}
