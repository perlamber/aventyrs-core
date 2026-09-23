package org.aventyrs.core.title.giganteenfurecido;

import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.SpellEmpowerment;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.AreaDamage;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Frenzy;
import org.aventyrs.core.sheet.FrenzyMode;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.combatant;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.context;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.dice;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.enterFrenzy;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.gigante;
import static org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecidoFixtures.holder;
import static org.aventyrs.core.util.TranslatableMessages.FRENZY_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.TITA_ENLOUQUECIDO_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_ACTIVATION_LIMIT_REACHED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_LOCKED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gritos de Guerra, Frenesi Arcano and Cataclismo Elemental. */
class TitaEnlouquecidoInteractionsTest {

    private static final GiganteEnfurecidoSpecialization TITA = GiganteEnfurecidoSpecialization.TITA_ENLOUQUECIDO;

    private CharacterSheet sheet;
    private GiganteEnfurecido title;
    private CharacterSheet adjacentFoe;
    private CharacterSheet nearFoe;
    private CharacterSheet farFoe;
    private CharacterSheet nearAlly;
    private CharacterSheet farAlly;
    private SceneContext context;

    @BeforeEach
    void setup() {
        GiganteEnfurecidoFixtures.loadTemplates();
        title = gigante(List.of(TITA), TitaEnlouquecidoAbility.GRITOS_DE_GUERRA, TitaEnlouquecidoAbility.COLOSSO_ENFURECIDO,
                TitaEnlouquecidoAbility.FRENESI_ARCANO, TitaEnlouquecidoAbility.CATACLISMO_ELEMENTAL);
        sheet = holder(title);
        adjacentFoe = combatant(0);
        nearFoe = combatant(0);
        farFoe = combatant(0);
        nearAlly = combatant(0);
        farAlly = combatant(0);
        context = context(3, Map.of(nearAlly, Range.DISTANCIA_CURTA, farAlly, Range.DISTANCIA_MEDIA),
                Map.of(adjacentFoe, Range.ADJACENTE, nearFoe, Range.DISTANCIA_CURTA, farFoe, Range.DISTANCIA_MEDIA));
    }

    private InteractionResult grito(final GritoDeGuerra grito, final SceneContext sceneContext) {
        return title.activateAbility(TitaEnlouquecidoAbility.GRITOS_DE_GUERRA, TitleAbilityActivationRequest.builder()
                .activator(sheet).choice(grito).sceneContext(sceneContext).diceRoller(dice(3)).build());
    }

    @Test
    void gritosNeedAFrenesi() {
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> grito(GritoDeGuerra.DESDENHO, context));
        assertEquals(FRENZY_REQUIRED, refused.getMessage());
    }

    @Test
    void gritosNeedAChoiceAndAreOncePerRound() {
        enterFrenzy(sheet);
        IllegalOperationException noChoice = assertThrows(IllegalOperationException.class,
                () -> title.activateAbility(TitaEnlouquecidoAbility.GRITOS_DE_GUERRA,
                        TitleAbilityActivationRequest.builder().activator(sheet).sceneContext(context).build()));
        assertEquals(TITLE_ABILITY_CHOICE_REQUIRED, noChoice.getMessage());

        grito(GritoDeGuerra.DESDENHO, context);
        IllegalOperationException again = assertThrows(IllegalOperationException.class,
                () -> grito(GritoDeGuerra.ESPINHOSO, context));
        assertEquals(TITLE_ABILITY_ACTIVATION_LIMIT_REACHED, again.getMessage());
        grito(GritoDeGuerra.ESPINHOSO, context(4, Map.of(), Map.of(nearFoe, Range.DISTANCIA_CURTA)));
    }

    @Test
    void gritoDeDesdenhoHalvesDamageForTheGiganteAndAlliesInMediaForOneRound() {
        enterFrenzy(sheet);

        Blessing blessing = grito(GritoDeGuerra.DESDENHO, context).getBlessings().get(0);

        assertEquals(ModifierType.HALF_DAMAGE, blessing.getModifierType());
        assertEquals(TargetScope.SELF_AND_ALLIES, blessing.getScope());
        assertEquals(Range.DISTANCIA_MEDIA, blessing.getReach());
        assertEquals(1, blessing.getRounds());
        assertTrue(blessing.isCountsDownAtTurnStart());
        sheet.grantBlessing(blessing);
        sheet.finishTurn();
        assertEquals(1, sheet.getTemporaryBonus(ModifierType.HALF_DAMAGE));
    }

    @Test
    void gritoEspinhosoHitsEnemiesInCurtaAndDoublesOnTheAdjacent() {
        enterFrenzy(sheet);
        int multiplier = new HitPointsServiceImpl().getLifeMultiplier(sheet.getCharacter(), sheet);
        int expected = 3 + multiplier / 2;

        List<AreaDamage> hits = grito(GritoDeGuerra.ESPINHOSO, context).getAreaDamage();

        assertEquals(2, hits.size());
        assertEquals(expected * 2, hitOn(hits, adjacentFoe).amount());
        assertEquals(expected, hitOn(hits, nearFoe).amount());
        assertEquals(DamageType.PRIMORDIAL, hitOn(hits, nearFoe).descriptor().damageType());
    }

    private static AreaDamage hitOn(final List<AreaDamage> hits, final CharacterSheet target) {
        return hits.stream().filter(hit -> hit.target() == target).findFirst().orElseThrow();
    }

    @Test
    void gritoInspiradorGivesAlliesInCurtaTheFrenesiDrawbacksIncludedForOneRound() {
        enterFrenzy(sheet);

        InteractionResult result = grito(GritoDeGuerra.INSPIRADOR, context);

        assertEquals(List.of(nearAlly), result.getFrenzyRecipients());
        Frenzy inspired = nearAlly.getFrenzy().orElseThrow();
        assertTrue(inspired.isInspired());
        assertEquals(1, inspired.getRemainingRounds());
        assertEquals(2, nearAlly.getTemporaryBonus(ModifierType.STRENGTH_BONUS));
        assertTrue(nearAlly.isSpellCastingPrevented(null));
        assertTrue(nearAlly.getOwnFrenzy().isEmpty());
        assertTrue(farAlly.getFrenzy().isEmpty());
    }

    @Test
    void frenesiArcanoNeedsTitaActiveAndGrantsOneEmpoweredCastPerRound() {
        TitleAbilityActivationRequest request = TitleAbilityActivationRequest.builder()
                .activator(sheet).choice(SpellEmpowerment.DANO).sceneContext(context).build();
        enterFrenzy(sheet);
        IllegalOperationException withoutTita = assertThrows(IllegalOperationException.class,
                () -> title.activateAbility(TitaEnlouquecidoAbility.FRENESI_ARCANO, request));
        assertEquals(TITA_ENLOUQUECIDO_REQUIRED, withoutTita.getMessage());

        sheet.endFrenzy();
        enterFrenzy(sheet, FrenzyMode.TITA_ENLOUQUECIDO);
        title.activateAbility(TitaEnlouquecidoAbility.FRENESI_ARCANO, request);

        assertEquals(1, sheet.getRemainingEnhancedAttacks(SpellEmpowerment.DANO));
        IllegalOperationException again = assertThrows(IllegalOperationException.class,
                () -> title.activateAbility(TitaEnlouquecidoAbility.FRENESI_ARCANO, request));
        assertEquals(TITLE_ABILITY_ACTIVATION_LIMIT_REACHED, again.getMessage());
    }

    private InteractionResult cataclismo(final Set<ElementalType> elements) {
        return title.activateAbility(TitaEnlouquecidoAbility.CATACLISMO_ELEMENTAL, TitleAbilityActivationRequest.builder()
                .activator(sheet).choices(elements).build());
    }

    @Test
    void cataclismoNeedsAFrenesiAndAtLeastOneOfferedElement() {
        IllegalOperationException noFrenzy = assertThrows(IllegalOperationException.class,
                () -> cataclismo(Set.of(ElementalType.FOGO)));
        assertEquals(FRENZY_REQUIRED, noFrenzy.getMessage());

        enterFrenzy(sheet);
        IllegalOperationException none = assertThrows(IllegalOperationException.class, () -> cataclismo(Set.of()));
        assertEquals(TITLE_ABILITY_CHOICE_REQUIRED, none.getMessage());
        IllegalOperationException all = assertThrows(IllegalOperationException.class,
                () -> cataclismo(Set.of(ElementalType.TODOS)));
        assertEquals(TITLE_ABILITY_CHOICE_LOCKED, all.getMessage());
    }

    @Test
    void cataclismoCostsAnAutocontroleAndGivesReAgainstEachChosenElement() {
        enterFrenzy(sheet);
        int before = sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE);

        cataclismo(Set.of(ElementalType.FOGO, ElementalType.GELO));

        assertEquals(before - 1, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        var damage = new DamageServiceImpl();
        assertEquals(8, damage.calculateFinalDamage(sheet, null,
                new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO), null, 10, false));
        assertEquals(10, damage.calculateFinalDamage(sheet, null,
                new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.VENTO), null, 10, false));
    }

    @Test
    void underCataclismoEveryGritoAlsoHitsEveryoneInCurtaOncePerElement() {
        enterFrenzy(sheet);
        cataclismo(Set.of(ElementalType.FOGO, ElementalType.GELO));

        List<AreaDamage> hits = grito(GritoDeGuerra.DESDENHO, context).getAreaDamage();

        assertEquals(6, hits.size());
        assertTrue(hits.stream().filter(hit -> hit.target() == adjacentFoe).allMatch(hit -> hit.amount() == 2));
        assertTrue(hits.stream().filter(hit -> hit.target() == nearAlly).allMatch(hit -> hit.amount() == 1));
        assertTrue(hits.stream().noneMatch(hit -> hit.target() == farFoe || hit.target() == farAlly));
        assertTrue(hits.stream().allMatch(hit -> hit.descriptor().damageType() == DamageType.ELEMENTAL));
        assertSame(sheet, hits.get(0).source());
    }

    @Test
    void withoutCataclismoADesdenhoDealsNoDamage() {
        enterFrenzy(sheet);

        assertNull(grito(GritoDeGuerra.DESDENHO, context).getAreaDamage());
    }
}
