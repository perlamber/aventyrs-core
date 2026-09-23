package org.aventyrs.core.title.giganteenfurecido;

import org.aventyrs.core.action.ReactionContext;
import org.aventyrs.core.action.ReactionOption;
import org.aventyrs.core.action.ReactionTrigger;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.ReactionOptionsServiceImpl;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.FrenzyMode;
import org.aventyrs.core.sheet.FrightfulCondition;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.aventyrs.core.title.TitleAttackModifiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.combatant;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.context;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.enterFrenzy;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.gigante;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.holder;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_ACTIVATION_LIMIT_REACHED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_TARGET_OUT_OF_RANGE;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_TRIGGER_NOT_MET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Retaliação Furiosa, Frenesi Assustador, Fanático de Cyt, and Frenesi Reativo — the four Reação-shaped traits. */
class BerserkerInteractionsTest {

    private static final GiganteEnfurecidoSpecialization BERSERKER = GiganteEnfurecidoSpecialization.BERSERKER;

    private final ReactionOptionsServiceImpl reactionOptions = new ReactionOptionsServiceImpl();

    private CharacterSheet sheet;
    private GiganteEnfurecido title;

    @BeforeEach
    void setup() {
        GiganteEnfurecidoFixtures.loadTemplates();
        title = gigante(List.of(BERSERKER), BerserkerAbility.RETALIACAO_FURIOSA, BerserkerAbility.FRENESI_ASSUSTADOR,
                BerserkerAbility.DESPREZAR_DANOS, BerserkerAbility.FANATICO_DE_CYT,
                GiganteEnfurecidoAbility.FRENESI_REATIVO);
        sheet = holder(title);
    }

    private List<AventyrTitleAbility> offered(final ReactionTrigger trigger, final CharacterSheet attacker,
                                              final SceneContext context) {
        return reactionOptions.getAvailableReactions(ReactionContext.builder()
                        .trigger(trigger).reactor(sheet).reactorContext(context).attacker(attacker).build())
                .stream().map(ReactionOption::ability).toList();
    }

    // --- Retaliação Furiosa -------------------------------------------------------------------

    @Test
    void retaliacaoIsOfferedOnlyAgainstAnAdjacentEnemy() {
        CharacterSheet adjacent = combatant(0);
        CharacterSheet distant = combatant(0);
        SceneContext context = context(1, Map.of(), Map.of(adjacent, Range.ADJACENTE, distant, Range.DISTANCIA_CURTA));

        assertTrue(offered(ReactionTrigger.ADJACENT_ENEMY_ATTACKS_OTHER, adjacent, context)
                .contains(BerserkerAbility.RETALIACAO_FURIOSA));
        assertFalse(offered(ReactionTrigger.ADJACENT_ENEMY_ATTACKS_OTHER, distant, context)
                .contains(BerserkerAbility.RETALIACAO_FURIOSA));
    }

    private void retaliate(final CharacterSheet enemy, final SceneContext context) {
        title.activateAbility(BerserkerAbility.RETALIACAO_FURIOSA, TitleAbilityActivationRequest.builder()
                .activator(sheet).target(enemy).sceneContext(context).build());
    }

    @Test
    void retaliacaoFixesTheDamageOfTheNextAttackOnThatEnemyWhateverTheWeapon() {
        CharacterSheet enemy = combatant(0);
        CharacterSheet bystander = combatant(0);
        SceneContext context = context(1, Map.of(), Map.of(enemy, Range.ADJACENTE, bystander, Range.ADJACENTE));

        retaliate(enemy, context);

        int strength = sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.STRENGTH, sheet);
        assertEquals(new TitleAttackModifiers.DamageOverride(1, strength / 2),
                TitleAttackModifiers.resolve(sheet, null, enemy, context).damageOverride());
        assertNull(TitleAttackModifiers.resolve(sheet, null, bystander, context).damageOverride());

        TitleAttackModifiers.consumeCharges(sheet, null);
        assertNull(TitleAttackModifiers.resolve(sheet, null, enemy, context).damageOverride());
    }

    @Test
    void atZeroHitPointsTheRetaliationHitsFor2d6PlusFullForca() {
        CharacterSheet enemy = combatant(0);
        SceneContext context = context(1, Map.of(), Map.of(enemy, Range.ADJACENTE));
        sheet.applyDamage(new HitPointsServiceImpl().getMaxHitPoints(sheet.getCharacter(), sheet));

        retaliate(enemy, context);

        int strength = sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.STRENGTH, sheet);
        assertEquals(new TitleAttackModifiers.DamageOverride(2, strength),
                TitleAttackModifiers.resolve(sheet, null, enemy, context).damageOverride());
    }

    @Test
    void retaliacaoIsOncePerRoundPerEnemy() {
        CharacterSheet enemy = combatant(0);
        CharacterSheet other = combatant(0);
        SceneContext round1 = context(1, Map.of(), Map.of(enemy, Range.ADJACENTE, other, Range.ADJACENTE));

        retaliate(enemy, round1);
        IllegalOperationException again = assertThrows(IllegalOperationException.class, () -> retaliate(enemy, round1));
        assertEquals(TITLE_ABILITY_TARGET_OUT_OF_RANGE, again.getMessage());
        retaliate(other, round1);
        retaliate(enemy, context(2, Map.of(), Map.of(enemy, Range.ADJACENTE)));
    }

    // --- Frenesi Assustador -------------------------------------------------------------------

    private InteractionResult scare(final SceneContext context) {
        return title.activateAbility(BerserkerAbility.FRENESI_ASSUSTADOR, TitleAbilityActivationRequest.builder()
                .activator(sheet).sceneContext(context).build());
    }

    @Test
    void assustadorOnlyFollowsAKillOrACritical() {
        SceneContext context = context(1, Map.of(), Map.of(combatant(9), Range.DISTANCIA_CURTA));

        IllegalOperationException refused = assertThrows(IllegalOperationException.class, () -> scare(context));
        assertEquals(TITLE_ABILITY_TRIGGER_NOT_MET, refused.getMessage());
    }

    @Test
    void assustadorScaresFoesWithMoreAutocontroleAndClimbsTheLadderEachRound() {
        CharacterSheet steady = combatant(9);
        CharacterSheet timid = combatant(1);
        CharacterSheet distant = combatant(9);
        Map<org.aventyrs.core.sheet.CombatantSheet, Range> enemies =
                Map.of(steady, Range.DISTANCIA_CURTA, timid, Range.ADJACENTE, distant, Range.DISTANCIA_MEDIA);

        title.recordTriumph(sheet);
        InteractionResult first = scare(context(1, Map.of(), enemies));

        assertEquals(1, first.getInflictedConditions().size());
        assertTrue(steady.hasCondition(ConditionType.ABALADO, null));
        assertFalse(timid.hasCondition(ConditionType.ABALADO, null));
        assertFalse(distant.hasCondition(ConditionType.ABALADO, null));

        IllegalOperationException sameRound = assertThrows(IllegalOperationException.class,
                () -> scare(context(1, Map.of(), enemies)));
        assertEquals(TITLE_ABILITY_ACTIVATION_LIMIT_REACHED, sameRound.getMessage());

        scare(context(2, Map.of(), enemies));
        assertTrue(steady.hasCondition(ConditionType.ASSUSTADO, null));
        assertFalse(steady.hasCondition(ConditionType.APAVORADO, null));
    }

    @Test
    void scaredFoesLoseMinorCriticalsAndCorrentesWhileTheGiganteIsDown() {
        CharacterSheet steady = combatant(9);
        title.recordTriumph(sheet);
        scare(context(1, Map.of(), Map.of(steady, Range.DISTANCIA_CURTA)));

        assertFalse(steady.isMinorCriticalAndChainSuppressed());
        sheet.applyDamage(new HitPointsServiceImpl().getMaxHitPoints(sheet.getCharacter(), sheet));
        assertTrue(steady.isMinorCriticalAndChainSuppressed());
    }

    @Test
    void theFearKeepsItsOriginAsItDecays() {
        CharacterSheet steady = combatant(9);
        steady.applyCondition(new FrightfulCondition(ConditionType.ASSUSTADO, 1, sheet));

        steady.tickTemporaryEffects();

        assertTrue(steady.hasCondition(ConditionType.ABALADO, null));
        sheet.applyDamage(new HitPointsServiceImpl().getMaxHitPoints(sheet.getCharacter(), sheet));
        assertTrue(steady.isMinorCriticalAndChainSuppressed());
    }

    // --- Fanático de Cyt ----------------------------------------------------------------------

    @Test
    void fanaticoIsOfferedOnlyDuringAFrenesi() {
        SceneContext context = context(1, Map.of(), Map.of());
        assertFalse(offered(ReactionTrigger.SELF_WOULD_DROP_TO_ZERO_HP, null, context)
                .contains(BerserkerAbility.FANATICO_DE_CYT));

        enterFrenzy(sheet);
        assertTrue(offered(ReactionTrigger.SELF_WOULD_DROP_TO_ZERO_HP, null, context)
                .contains(BerserkerAbility.FANATICO_DE_CYT));
    }

    @Test
    void fanaticoSpendsEveryAutocontroleLeavesOneHitPointAndCannotDie() {
        enterFrenzy(sheet, FrenzyMode.BERSERKER);
        var hitPoints = new HitPointsServiceImpl();
        int max = hitPoints.getMaxHitPoints(sheet.getCharacter(), sheet);
        int spent = sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE);
        assertTrue(new DamageServiceImpl().wouldDropToZeroOrBelow(sheet, max * 5));

        title.activateAbility(BerserkerAbility.FANATICO_DE_CYT,
                TitleAbilityActivationRequest.builder().activator(sheet).build());
        sheet.applyDamage(max * 5);

        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        assertEquals(1, max - sheet.getDamageTaken());
        assertEquals(spent, sheet.getTotalLifeSteal());
        assertTrue(sheet.isCompelledToAttackNearest());
        assertTrue(sheet.treatsBeneficialSpellsAsHostile());

        sheet.applyDamage(max * 5);
        assertEquals(CharacterStatus.COMMA, hitPoints.getStatus(sheet));
        sheet.endFrenzy();
        assertEquals(CharacterStatus.DEAD, hitPoints.getStatus(sheet));
    }

    // --- Frenesi Reativo ----------------------------------------------------------------------

    @Test
    void frenesiReativoStartsTheFrenesiAsAReactionForOneMoreAutocontrole() {
        SceneContext context = context(1, Map.of(), Map.of());
        assertTrue(offered(ReactionTrigger.SELF_HIT_BY_SUCCESSFUL_ATTACK, combatant(0), context)
                .contains(GiganteEnfurecidoAbility.FRENESI_REATIVO));
        int before = sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE);

        title.activateAbility(GiganteEnfurecidoAbility.FRENESI_REATIVO,
                GiganteEnfurecidoFixtures.frenesi(sheet, FrenzyMode.BERSERKER));

        assertEquals(before - 3, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        assertTrue(sheet.getOwnFrenzy().orElseThrow().hasMode(FrenzyMode.BERSERKER));
        assertFalse(offered(ReactionTrigger.SELF_HIT_BY_SUCCESSFUL_ATTACK, combatant(0), context)
                .contains(GiganteEnfurecidoAbility.FRENESI_REATIVO));
    }
}
