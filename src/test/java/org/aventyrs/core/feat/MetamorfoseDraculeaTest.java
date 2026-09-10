package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import java.util.List;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ActiveAbilityService;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Vampiro;
import org.aventyrs.core.race.Vampiro.VampiroLineage;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Metamorfose Dracúlea — the acquisition-time choice of Formas, the one-activatable-shape-per-Forma
 * grant, and the equipment rule that comes with being an animal: "armas não podem ser utilizadas",
 * while "itens defensivos continuam concedendo seus benefícios".
 */
class MetamorfoseDraculeaTest {

    private final ActiveAbilityService activeAbilityService = new ActiveAbilityServiceImpl();

    private static final Weapon SWORD = AbstractWeapon.builder()
            .name("Espada").category(ItemCategory.HEAVY_BLADE)
            .damageBase(DamageBase.of(1, 1)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character vampiro(final VampiroLineage lineage) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new Vampiro(lineage, new Human()))
                .feats(new ArrayList<>())
                .build();
    }

    /** A Nosferatu who picked Lobo and Morcego — the ordinary two-Forma case. */
    private static Character shapeshifter() {
        Character vampiro = vampiro(VampiroLineage.NOSFERATU);
        vampiro.grantFeat(MetamorfoseDraculeaFeat.of(vampiro,
                EnumSet.of(FormaMetamorfica.LOBO_DENTES_DE_SABRE, FormaMetamorfica.MORCEGO_ATROZ)));
        return vampiro;
    }

    private static ActiveAbility transformationInto(final Character character, final FormaMetamorfica forma) {
        return character.getActiveAbilities().stream()
                .filter(MetamorfoseActiveAbility.class::isInstance)
                .map(MetamorfoseActiveAbility.class::cast)
                .filter(ability -> ability.getForma() == forma)
                .findFirst()
                .map(ActiveAbility.class::cast)
                .orElseThrow(() -> new AssertionError("no transformation into " + forma));
    }

    // ---------- The acquisition-time choice ----------

    @Test
    void theLineageDecidesHowManyFormasMayBeChosen() {
        assertEquals(2, MetamorfoseDraculeaFeat.choicesFor(vampiro(VampiroLineage.NOSFERATU)));
        assertEquals(1, MetamorfoseDraculeaFeat.choicesFor(vampiro(VampiroLineage.DAMPIRO)));
        assertEquals(4, MetamorfoseDraculeaFeat.choicesFor(vampiro(VampiroLineage.RAKSHASA)));
    }

    @Test
    void theFactoryRefusesTheWrongNumberOfFormas() {
        Character dampiro = vampiro(VampiroLineage.DAMPIRO);

        assertThrows(IllegalArgumentException.class, () -> MetamorfoseDraculeaFeat.of(dampiro,
                EnumSet.of(FormaMetamorfica.NEVOA, FormaMetamorfica.MORCEGO_ATROZ)));
        assertEquals(1, MetamorfoseDraculeaFeat.of(dampiro, EnumSet.of(FormaMetamorfica.NEVOA))
                .getChosenFormas().size());
    }

    /** "Rakshasa podem escolher 4, mas não podem se transformar em Névoa." */
    @Test
    void aRakshasaMayTakeFourFormasButNotNevoa() {
        Character rakshasa = vampiro(VampiroLineage.RAKSHASA);
        Set<FormaMetamorfica> withNevoa = EnumSet.of(FormaMetamorfica.NEVOA,
                FormaMetamorfica.MORCEGO_ATROZ, FormaMetamorfica.LOBO_DENTES_DE_SABRE,
                FormaMetamorfica.ARANHA_GIGANTE);
        Set<FormaMetamorfica> withoutNevoa = EnumSet.of(FormaMetamorfica.SERPENTE_ESPINHOSA,
                FormaMetamorfica.MORCEGO_ATROZ, FormaMetamorfica.LOBO_DENTES_DE_SABRE,
                FormaMetamorfica.ARANHA_GIGANTE);

        assertThrows(IllegalArgumentException.class,
                () -> MetamorfoseDraculeaFeat.of(rakshasa, withNevoa));
        assertEquals(4, MetamorfoseDraculeaFeat.of(rakshasa, withoutNevoa).getChosenFormas().size());
    }

    // ---------- Discovering the choice from the catalog ----------

    /**
     * The point of {@code Feat#resolveActiveAbilityChoice}: a client walking the catalog can ask
     * any constant whether it needs a choice, and get the options already filtered for this
     * holder — without knowing that {@code MetamorfoseDraculeaFeat} or {@code FormaMetamorfica}
     * exist at all.
     */
    @Test
    void theCatalogConstantAdvertisesItsChoice() {
        Character nosferatu = vampiro(VampiroLineage.NOSFERATU);

        ActiveAbilityChoice choice = VampiricoFeat.METAMORFOSE_DRACULEA.resolveActiveAbilityChoice(nosferatu);

        assertEquals(2, choice.picks());
        assertEquals(6, choice.options().size(), "the whole table");
        assertTrue(choice.options().stream().allMatch(MetamorfoseActiveAbility.class::isInstance));
        // Each option can be rendered without knowing what it is.
        assertTrue(choice.options().stream().noneMatch(option -> option.getDescription().isBlank()));
    }

    /** The options are filtered per holder, so a client never reimplements a Talento's own rules. */
    @Test
    void aRakshasaIsNotOfferedNevoa() {
        ActiveAbilityChoice choice = VampiricoFeat.METAMORFOSE_DRACULEA
                .resolveActiveAbilityChoice(vampiro(VampiroLineage.RAKSHASA));

        assertEquals(4, choice.picks());
        assertEquals(5, choice.options().size());
        assertTrue(choice.options().stream()
                .map(MetamorfoseActiveAbility.class::cast)
                .noneMatch(ability -> ability.getForma() == FormaMetamorfica.NEVOA));
    }

    /** Every other Talento answers null — the signal is unambiguous. */
    @Test
    void aTalentoWithNoChoiceSaysSo() {
        Character nosferatu = vampiro(VampiroLineage.NOSFERATU);

        assertNull(VampiricoFeat.OSTEOMANCIA.resolveActiveAbilityChoice(nosferatu));
        assertNull(DraconicoFeat.DRACONATO.resolveActiveAbilityChoice(nosferatu));
    }

    /**
     * And the requirement is enforced, not merely advertised: granting the bare constant would
     * hand over a Metamorfose with no Formas, so {@code FeatService#grantFeat} refuses it.
     */
    @Test
    void grantingTheBareConstantIsRefused() {
        Character nosferatu = vampiro(VampiroLineage.NOSFERATU);
        CharacterSheet sheet = CharacterSheet.of(nosferatu, new Player());
        sheet.accumulateExperience(java.math.BigDecimal.valueOf(100));

        assertThrows(IllegalOperationException.class, () -> new FeatServiceImpl()
                .grantFeat(nosferatu, sheet, VampiricoFeat.METAMORFOSE_DRACULEA));
    }

    /** What a client picked from the offered options is what it can then grant, and activate. */
    @Test
    void theChosenOptionsAreTheAbilitiesGranted() throws IllegalOperationException {
        Character nosferatu = vampiro(VampiroLineage.NOSFERATU);
        CharacterSheet sheet = CharacterSheet.of(nosferatu, new Player());
        sheet.accumulateExperience(java.math.BigDecimal.valueOf(100));
        ActiveAbilityChoice choice = VampiricoFeat.METAMORFOSE_DRACULEA.resolveActiveAbilityChoice(nosferatu);
        List<ActiveAbility> picked = choice.options().subList(0, choice.picks());

        new FeatServiceImpl().grantFeat(nosferatu, sheet,
                MetamorfoseDraculeaFeat.ofChosenAbilities(nosferatu, picked));

        assertEquals(picked, nosferatu.getActiveAbilities(), "same instances, so == matching works");
        activeAbilityService.activate(nosferatu, sheet, picked.get(0), 0);
        assertNotNull(sheet.getCurrentForm());
    }

    // ---------- One activatable shape per chosen Forma ----------

    /**
     * The plural {@code Feat#resolveActiveAbilities} hook exists for exactly this: two chosen
     * Formas means two separately activatable shapes, not one ability that asks which.
     */
    @Test
    void eachChosenFormaIsItsOwnActivatableShape() {
        Character character = shapeshifter();

        assertEquals(2, character.getActiveAbilities().size());
        assertEquals(FormaMetamorfica.LOBO_DENTES_DE_SABRE,
                ((MetamorfoseActiveAbility) transformationInto(character, FormaMetamorfica.LOBO_DENTES_DE_SABRE))
                        .getForma());
    }

    @Test
    void activatingOneEntersThatShapeAndCostsAPoderVampiricosThreeHitPoints()
            throws IllegalOperationException {
        Character character = shapeshifter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        activeAbilityService.activate(character, sheet,
                transformationInto(character, FormaMetamorfica.MORCEGO_ATROZ), 0);

        assertTrue(sheet.isInForm(FormType.MORCEGO_ATROZ));
        assertEquals(3, sheet.getDamageTaken(), "3PV — the Poder Vampírico price");
    }

    /** And it lapses on its own after the Poder Vampírico Duração, like any other. */
    @Test
    void theShapeLapsesAfterTwoRodadas() throws IllegalOperationException {
        Character character = shapeshifter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        activeAbilityService.activate(character, sheet,
                transformationInto(character, FormaMetamorfica.LOBO_DENTES_DE_SABRE), 0);

        sheet.finishTurn();
        assertTrue(sheet.isInForm(FormType.LOBO_DENTES_DE_SABRE));

        sheet.finishTurn();

        assertNull(sheet.getCurrentForm());
    }

    // ---------- The equipment rule ----------

    /**
     * "Armas não podem ser utilizadas" while metamorphosed — but an Arma Natural is exempt, since
     * the same clause says those are what replaces them. And it lifts when the shape does.
     */
    @Test
    void weaponsCannotBeUsedWhileMetamorphosedButArmasNaturaisCan() throws IllegalOperationException {
        Character character = shapeshifter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertTrue(sheet.canAttackWith(SWORD), "unremarkable before transforming");

        activeAbilityService.activate(character, sheet,
                transformationInto(character, FormaMetamorfica.LOBO_DENTES_DE_SABRE), 0);

        assertFalse(sheet.canAttackWith(SWORD));
        assertTrue(sheet.canAttackWith(NaturalWeapon.PRESAS_LONGAS), "an Arma Natural is not equipment");
        assertTrue(sheet.canAttackWith(null), "nor is an Ataque Desarmado");

        sheet.enterForm(null);

        assertTrue(sheet.canAttackWith(SWORD), "the restriction goes with the shape");
    }

    /** A Forma whose text says nothing about equipment restricts nothing — Draconato, Anciente. */
    @Test
    void aFormaWithNoEquipmentClauseLeavesWeaponsAlone() {
        CharacterSheet sheet = CharacterSheet.of(shapeshifter(), new Player());

        sheet.enterForm(FormType.DRACONATO);

        assertTrue(sheet.canAttackWith(SWORD));
    }

    /** The table's Arma Natural column is authored, even where nothing grants it yet. */
    @Test
    void everyFormaCarriesItsTableRow() {
        assertEquals(NaturalWeapon.CHIFRES_PODEROSOS, FormaMetamorfica.CAVALO_DE_CHIFRES.getNaturalWeapon());
        assertEquals(NaturalWeapon.CAUDA_CONSTRITORA, FormaMetamorfica.SERPENTE_ESPINHOSA.getNaturalWeapon());
        assertNull(FormaMetamorfica.NEVOA.getNaturalWeapon(), "\"Nenhum\" — Névoa fights with nothing");
        for (FormaMetamorfica forma : FormaMetamorfica.values()) {
            assertFalse(forma.getAbilityDescription().isBlank(), forma + " has no Habilidade text");
        }
    }
}
