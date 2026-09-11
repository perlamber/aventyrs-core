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
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Vampiro;
import org.aventyrs.core.race.Vampiro.VampiroLineage;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
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

    /**
     * No {@code weightClass}, so {@code AbstractCombatantSheet#isTwoHanded} reads it as
     * <b>one-handed</b> — which matters, because Lobo Dentes-de-Sabre's row exempts exactly that.
     */
    private static final Weapon SWORD = AbstractWeapon.builder()
            .name("Espada").category(ItemCategory.HEAVY_BLADE)
            .damageBase(DamageBase.of(1, 1)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();

    /** Explicitly two-handed, for the half of Lobo's clawback that does <em>not</em> apply. */
    private static final Weapon GREATSWORD = AbstractWeapon.builder()
            .name("Montante").category(ItemCategory.HEAVY_BLADE).weightClass(ItemWeightClass.HEAVY)
            .damageBase(DamageBase.of(2, 0)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();

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

    /**
     * A Nosferatu who picked Serpente and Névoa — the fixture the <b>replacement</b> is visible
     * on. {@link #shapeshifter()} cannot show it: Lobo, Morcego and Aranha all grant Presas
     * Longas, which a Nosferatu already has racially, so swapping and adding look identical there.
     * Serpente grants a Cauda Constritora the lineage does not have, and Névoa grants nothing.
     */
    private static Character serpentAndMist() {
        Character vampiro = vampiro(VampiroLineage.NOSFERATU);
        vampiro.grantFeat(MetamorfoseDraculeaFeat.of(vampiro,
                EnumSet.of(FormaMetamorfica.SERPENTE_ESPINHOSA, FormaMetamorfica.NEVOA)));
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

        FeatChoice<?> choice = VampiricoFeat.METAMORFOSE_DRACULEA.resolveRequiredChoices(nosferatu).get(0);

        assertEquals(2, choice.picks());
        assertEquals(ActiveAbility.class, choice.type(), "the type token a client routes on");
        assertEquals(6, choice.options().size(), "the whole table");
        assertTrue(choice.options().stream().allMatch(MetamorfoseActiveAbility.class::isInstance));
        // Each option can be rendered without knowing what it is.
        assertTrue(choice.options().stream().map(ActiveAbility.class::cast)
                .noneMatch(option -> option.getDescription().isBlank()));
    }

    /** The options are filtered per holder, so a client never reimplements a Talento's own rules. */
    @Test
    void aRakshasaIsNotOfferedNevoa() {
        FeatChoice<?> choice = VampiricoFeat.METAMORFOSE_DRACULEA
                .resolveRequiredChoices(vampiro(VampiroLineage.RAKSHASA)).get(0);

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

        assertTrue(VampiricoFeat.OSTEOMANCIA.resolveRequiredChoices(nosferatu).isEmpty());
        assertTrue(DraconicoFeat.DRACONATO.resolveRequiredChoices(nosferatu).isEmpty());
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
        FeatChoice<?> choice = VampiricoFeat.METAMORFOSE_DRACULEA.resolveRequiredChoices(nosferatu).get(0);
        List<ActiveAbility> picked = choice.options().stream()
                .map(ActiveAbility.class::cast).toList().subList(0, choice.picks());

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
     *
     * <p>Tested on <b>Morcego Atroz</b> rather than Lobo: Lobo's own row claws one-handed weapons
     * back ("pode empunhar armas de uma mão com as presas"), so it is the wrong shape to pin the
     * general rule on — see {@link #loboAloneKeepsOneHandedWeaponsUsable}.
     */
    @Test
    void weaponsCannotBeUsedWhileMetamorphosedButArmasNaturaisCan() throws IllegalOperationException {
        Character character = shapeshifter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertTrue(sheet.canAttackWith(SWORD), "unremarkable before transforming");

        activeAbilityService.activate(character, sheet,
                transformationInto(character, FormaMetamorfica.MORCEGO_ATROZ), 0);

        assertFalse(sheet.canAttackWith(SWORD));
        assertTrue(sheet.canAttackWith(NaturalWeapon.PRESAS_LONGAS), "the bat's own fangs");
        assertTrue(sheet.canAttackWith(null), "an Ataque Desarmado is not a weapon to suppress");

        sheet.enterForm(null);

        assertTrue(sheet.canAttackWith(SWORD), "the restriction goes with the shape");
    }

    /**
     * The table's one clawback: Lobo Dentes-de-Sabre's "pode empunhar armas de uma mão com as
     * presas". One-handed weapons survive the suppression <em>in that shape only</em>; two-handed
     * ones do not, and no other Forma grants the exemption.
     */
    @Test
    void loboAloneKeepsOneHandedWeaponsUsable() throws IllegalOperationException {
        Character character = shapeshifter();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        activeAbilityService.activate(character, sheet,
                transformationInto(character, FormaMetamorfica.LOBO_DENTES_DE_SABRE), 0);

        assertTrue(sheet.canAttackWith(SWORD), "uma mão — held in the fangs");
        assertFalse(sheet.canAttackWith(GREATSWORD), "two hands is more than a wolf's jaw can do");

        sheet.enterForm(FormType.MORCEGO_ATROZ);

        assertFalse(sheet.canAttackWith(SWORD), "the clawback belongs to Lobo, not to the Talento");
    }

    /** A Forma whose text says nothing about equipment restricts nothing — Draconato, Anciente. */
    @Test
    void aFormaWithNoEquipmentClauseLeavesWeaponsAlone() {
        CharacterSheet sheet = CharacterSheet.of(shapeshifter(), new Player());

        sheet.enterForm(FormType.DRACONATO);

        assertTrue(sheet.canAttackWith(SWORD));
    }

    /** The table's Arma Natural column is authored data, independent of what reads it. */
    @Test
    void everyFormaCarriesItsTableRow() {
        assertEquals(NaturalWeapon.CHIFRES_PODEROSOS, FormaMetamorfica.CAVALO_DE_CHIFRES.getNaturalWeapon());
        assertEquals(NaturalWeapon.CAUDA_CONSTRITORA, FormaMetamorfica.SERPENTE_ESPINHOSA.getNaturalWeapon());
        assertNull(FormaMetamorfica.NEVOA.getNaturalWeapon(), "\"Nenhum\" — Névoa fights with nothing");
        for (FormaMetamorfica forma : FormaMetamorfica.values()) {
            assertFalse(forma.getAbilityDescription().isBlank(), forma + " has no Habilidade text");
        }
    }

    // ---------- The Arma Natural swap ----------

    /**
     * "São substituídas por armas naturais", read as replacing the holder's own. A Nosferatu has
     * Presas Longas by lineage; as a Serpente Espinhosa they have the Cauda Constritora and
     * nothing else.
     */
    @Test
    void theWornShapesArmaNaturalReplacesTheHoldersOwn() throws IllegalOperationException {
        Character character = serpentAndMist();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertEquals(List.of(NaturalWeapon.PRESAS_LONGAS), sheet.getNaturalWeapons(),
                "out of any Forma, the sheet view is the Character view");

        activeAbilityService.activate(character, sheet,
                transformationInto(character, FormaMetamorfica.SERPENTE_ESPINHOSA), 0);

        assertEquals(List.of(NaturalWeapon.CAUDA_CONSTRITORA), sheet.getNaturalWeapons());
        assertTrue(sheet.canAttackWith(NaturalWeapon.CAUDA_CONSTRITORA));
        assertFalse(sheet.canAttackWith(NaturalWeapon.PRESAS_LONGAS),
                "a snake has no fangs of that kind while it is a snake");
    }

    /**
     * Névoa's column reads "Nenhum", and under replacement that empties the list outright — half
     * of "é incapaz de causar danos" falling out of the weapon path. The other half is still
     * unmodelled: an Ataque Desarmado is not a weapon, so nothing here refuses one.
     */
    @Test
    void nevoaLeavesItsHolderWithNoArmaNaturalAtAll() throws IllegalOperationException {
        Character character = serpentAndMist();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        activeAbilityService.activate(character, sheet,
                transformationInto(character, FormaMetamorfica.NEVOA), 0);

        assertTrue(sheet.getNaturalWeapons().isEmpty());
        for (NaturalWeapon weapon : NaturalWeapon.values()) {
            assertFalse(sheet.canAttackWith(weapon), weapon + " should be unusable as mist");
        }
        assertFalse(sheet.canAttackWith(SWORD), "and weapons were already suppressed");
    }

    /**
     * <b>The reason this is derived rather than swapped.</b> All three ways out of a Forma have to
     * restore the holder's own Armas Naturais, and none of them writes anything: the Duração
     * lapsing, {@code enterForm(null)}, and a second Forma displacing the first.
     */
    @Test
    void everyWayOutOfTheShapeRestoresTheHoldersOwnWeapons() throws IllegalOperationException {
        Character character = serpentAndMist();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        List<NaturalWeapon> own = sheet.getNaturalWeapons();

        // 1. the Duração lapsing on its own
        activeAbilityService.activate(character, sheet,
                transformationInto(character, FormaMetamorfica.SERPENTE_ESPINHOSA), 0);
        assertEquals(List.of(NaturalWeapon.CAUDA_CONSTRITORA), sheet.getNaturalWeapons());
        for (int rodada = 0; rodada < 5; rodada++) {
            sheet.finishTurn();
            sheet.startNewRound();
        }
        assertNull(sheet.getCurrentForm(), "the Poder Vampírico's 2 Rodadas are long spent");
        assertEquals(own, sheet.getNaturalWeapons(), "restored with nothing to restore");

        // 2. leaving by hand
        sheet.enterForm(FormType.SERPENTE_ESPINHOSA);
        assertEquals(List.of(NaturalWeapon.CAUDA_CONSTRITORA), sheet.getNaturalWeapons());
        sheet.enterForm(null);
        assertEquals(own, sheet.getNaturalWeapons());

        // 3. a second Forma displacing the first
        sheet.enterForm(FormType.SERPENTE_ESPINHOSA);
        sheet.enterForm(FormType.NEVOA);
        assertTrue(sheet.getNaturalWeapons().isEmpty(), "the new shape's answer, not the old one's");
    }

    /**
     * A shape the holder never picked is not theirs — a GM bare-{@code enterForm}ing someone into
     * Aranha Gigante grants no fangs and triggers no replacement, which mirrors their having no
     * way to enter it. {@code shapeshifter()} picked Lobo and Morcego.
     */
    @Test
    void anUnchosenShapeGrantsNothingAndReplacesNothing() {
        CharacterSheet sheet = CharacterSheet.of(shapeshifter(), new Player());

        sheet.enterForm(FormType.ARANHA_GIGANTE);

        assertEquals(List.of(NaturalWeapon.PRESAS_LONGAS), sheet.getNaturalWeapons(),
                "still exactly what the lineage grants");
    }

    /** And a Forma from outside this table replaces nothing either — Draconato has no row. */
    @Test
    void aFormaOutsideTheTableLeavesTheHoldersWeaponsAlone() {
        CharacterSheet sheet = CharacterSheet.of(serpentAndMist(), new Player());

        sheet.enterForm(FormType.DRACONATO);

        assertEquals(List.of(NaturalWeapon.PRESAS_LONGAS), sheet.getNaturalWeapons());
    }

    // ---------- The three live Habilidades ----------

    /** Aranha Gigante — "Vantagem em Furtividade", and only while actually a spider. */
    @Test
    void aranhaGiganteGrantsFurtividadeVantagemOnlyWhileWorn() {
        Character character = vampiro(VampiroLineage.NOSFERATU);
        character.grantFeat(MetamorfoseDraculeaFeat.of(character,
                EnumSet.of(FormaMetamorfica.ARANHA_GIGANTE, FormaMetamorfica.NEVOA)));
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int before = rollBonus(sheet, SkillType.FURTIVIDADE);
        int meleeBefore = rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO);

        sheet.enterForm(FormType.ARANHA_GIGANTE);

        assertEquals(before + Skill.ADVANTAGE_BONUS, rollBonus(sheet, SkillType.FURTIVIDADE));
        assertEquals(meleeBefore, rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO),
                "the spider's Vantagem is Furtividade's alone — Lobo is the one with the attack half");

        sheet.enterForm(null);

        assertEquals(before, rollBonus(sheet, SkillType.FURTIVIDADE));
    }

    /** Lobo Dentes-de-Sabre — "Vantagem em Perícias de Ataque", both of them, and nothing else. */
    @Test
    void loboGrantsVantagemOnPericiasDeAtaqueOnly() {
        CharacterSheet sheet = CharacterSheet.of(shapeshifter(), new Player());
        int meleeBefore = rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO);
        int rangedBefore = rollBonus(sheet, SkillType.ATAQUE_A_DISTANCIA);
        int furtividadeBefore = rollBonus(sheet, SkillType.FURTIVIDADE);

        sheet.enterForm(FormType.LOBO_DENTES_DE_SABRE);

        assertEquals(meleeBefore + Skill.ADVANTAGE_BONUS, rollBonus(sheet, SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(rangedBefore + Skill.ADVANTAGE_BONUS, rollBonus(sheet, SkillType.ATAQUE_A_DISTANCIA));
        assertEquals(furtividadeBefore, rollBonus(sheet, SkillType.FURTIVIDADE),
                "not a Perícia de Ataque");
    }

    private static int rollBonus(final CharacterSheet sheet, final SkillType skillType) {
        return skillType.newInteraction().applyTo(sheet, null, null).getSkillRollBonus();
    }
}
