package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageBaseService;
import org.aventyrs.core.character.services.DamageBaseServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEAparar;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA} reclassifies light melee
 * weapons as Armas Naturais for its holder, and the point of the clause is that the
 * reclassification is <b>visible to every other Arma-Natural check</b> ("permite que você utilize
 * quaisquer outros Talentos ou Habilidades que afetem Armas Naturais para afetar sua arma").
 *
 * <p>So these tests exercise it through the consuming services — Dano Base and Defesas — rather
 * than through {@code Character#treatsAsNaturalWeapon} alone: the shared view is only worth
 * having if the clauses actually route through it.
 */
class NaturalWeaponReclassificationTest {

    private final FeatService featService = new FeatServiceImpl();
    private final DamageBaseService damageBaseService = new DamageBaseServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();

    private static final Weapon DAGGER = AbstractWeapon.builder()
            .name("Adaga")
            .category(ItemCategory.LIGHT_BLADE)
            .weightClass(ItemWeightClass.LIGHT)
            .damageBase(DamageBase.of(1, 2))
            .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .build();

    /** Heavy, so outside "armas leves" — the weight control. */
    private static final Weapon GREATSWORD = AbstractWeapon.builder()
            .name("Espada Grande")
            .category(ItemCategory.HEAVY_BLADE)
            .weightClass(ItemWeightClass.HEAVY)
            .damageBase(DamageBase.of(1, 3))
            .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .build();

    /** Light but ranged, so outside "de combate corpo-a-corpo" — the Perícia control. */
    private static final Weapon SLING = AbstractWeapon.builder()
            .name("Funda")
            .category(ItemCategory.PROJECTILE)
            .weightClass(ItemWeightClass.LIGHT)
            .damageBase(DamageBase.of(1, 1))
            .skillType(SkillType.ATAQUE_A_DISTANCIA)
            .build();

    /** A genuine Arma Natural on the same starting row as the dagger — the calibration control. */
    private static final Weapon CLAWS = AbstractWeapon.builder()
            .name("Garras Afiadas")
            .category(ItemCategory.NATURAL_WEAPON)
            .weightClass(ItemWeightClass.LIGHT)
            .damageBase(DamageBase.of(1, 2))
            .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .build();

    private static final Item SHIELD = AbstractItem.builder()
            .name("Escudo Redondo")
            .category(ItemCategory.SHIELD)
            .build();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /**
     * Strength 2 + Ataque Corpo-a-Corpo 5 + Esquiva e Aparar 4 + one Título Aventyr Desperto
     * satisfies the whole tree's ladder, including FERROADA_ESMAGADORA's own
     * {@code requiredAwakenedTitles(1)}.
     *
     * <p>That Título also makes ARTISTA_MARCIAL's grant +2 rather than +1 (it scales with the
     * Título count), which is why the Dano Base assertions below compare against a same-Título
     * control rather than a hard-coded figure.
     */
    private static Character martialArtist() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(2).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trained(new AtaqueCorpoACorpo(), 5))
                .skill(SkillType.ESQUIVA_E_APARAR, trained(new EsquivaEAparar(), 4))
                .build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        return character;
    }

    private static CharacterSkill trained(final org.aventyrs.core.skill.Skill skill, final int graduation) {
        return CharacterSkill.builder()
                .skill(skill)
                .graduation(SkillGraduation.builder().graduationValue(graduation).build())
                .build();
    }

    private void acquire(final Character character, final Feat... feats) throws IllegalOperationException {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(200));
        for (Feat feat : feats) {
            featService.grantFeat(character, sheet, feat);
        }
    }

    private void grantTheStyle(final Character character) throws IllegalOperationException {
        acquire(character, ArtesMarciaisFeat.ARTISTA_MARCIAL,
                ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA);
    }

    // ---------- the view itself ----------

    @Test
    void aLightMeleeWeaponIsNotNaturalWithoutTheStyle() {
        assertFalse(martialArtist().treatsAsNaturalWeapon(DAGGER));
    }

    @Test
    void theStyleMakesALightMeleeWeaponCountAsNatural() throws IllegalOperationException {
        Character character = martialArtist();
        grantTheStyle(character);

        assertTrue(character.treatsAsNaturalWeapon(DAGGER));
    }

    @Test
    void theStyleReachesNeitherAHeavyWeaponNorARangedOne() throws IllegalOperationException {
        Character character = martialArtist();
        grantTheStyle(character);

        assertFalse(character.treatsAsNaturalWeapon(GREATSWORD));
        assertFalse(character.treatsAsNaturalWeapon(SLING));
    }

    /** "Enquanto não estiver utilizando nenhum item do tipo Escudo." */
    @Test
    void wieldingAShieldSuspendsTheReclassification() throws IllegalOperationException {
        Character character = martialArtist();
        grantTheStyle(character);
        assertTrue(character.treatsAsNaturalWeapon(DAGGER));

        character.equip(SHIELD);

        assertFalse(character.treatsAsNaturalWeapon(DAGGER));
    }

    /** An Ataque Desarmado is not a weapon at all — null must be false, never an error. */
    @Test
    void anUnarmedAttackIsNotANaturalWeapon() {
        assertFalse(martialArtist().treatsAsNaturalWeapon(null));
    }

    // ---------- the reclassification reaching other Arma-Natural clauses ----------

    /**
     * ARTISTA_MARCIAL raises the Dano Base of "Ataques Desarmados e Armas Naturais" only. Without
     * the style a dagger gets nothing; with it, the dagger is an Arma Natural and gets the grant —
     * which is the whole promise of the clause.
     */
    @Test
    void theStyleMakesArtistaMarcialReachAReclassifiedWeapon() throws IllegalOperationException {
        Character withoutStyle = martialArtist();
        acquire(withoutStyle, ArtesMarciaisFeat.ARTISTA_MARCIAL);
        DamageBase plain = damageBaseService.getDamageBase(withoutStyle, DAGGER);

        Character withStyle = martialArtist();
        grantTheStyle(withStyle);
        DamageBase reclassified = damageBaseService.getDamageBase(withStyle, DAGGER);

        // Without the style the dagger is an ordinary blade, so ARTISTA_MARCIAL grants nothing.
        assertEquals(DAGGER.getDamageBase().scale(), plain.scale());
        // With it, the dagger gains exactly what a genuine Arma Natural of the same starting row
        // would — self-calibrating, since ARTISTA_MARCIAL's grant scales with the Título count.
        assertEquals(damageBaseService.getDamageBase(withStyle, CLAWS).scale(), reclassified.scale());
        assertTrue(reclassified.scale() > plain.scale());
    }

    /** The heavy control: the style does not reach it, so ARTISTA_MARCIAL still grants nothing. */
    @Test
    void theStyleDoesNotMakeArtistaMarcialReachAHeavyWeapon() throws IllegalOperationException {
        Character character = martialArtist();
        grantTheStyle(character);

        assertEquals(GREATSWORD.getDamageBase().scale(),
                damageBaseService.getDamageBase(character, GREATSWORD).scale());
    }

    /**
     * DEFESA_DE_MAOS_LIMPAS pays out while wielding no weapon "exceto Armas Naturais" — so a
     * reclassified dagger must stop cancelling it.
     */
    @Test
    void theStyleLetsDefesaDeMaosLimpasSurviveWieldingALightBlade() throws IllegalOperationException {
        Character withoutStyle = martialArtist();
        acquire(withoutStyle, ArtesMarciaisFeat.ARTISTA_MARCIAL, ArtesMarciaisFeat.DEFESA_DE_MAOS_LIMPAS);
        int bare = defenseService.getTotalDefense(withoutStyle, DefenseType.PHYSICAL);
        withoutStyle.equip(DAGGER);
        withoutStyle.drawWeapon(DAGGER);
        int cancelled = defenseService.getTotalDefense(withoutStyle, DefenseType.PHYSICAL);

        Character withStyle = martialArtist();
        acquire(withStyle, ArtesMarciaisFeat.ARTISTA_MARCIAL, ArtesMarciaisFeat.DEFESA_DE_MAOS_LIMPAS,
                ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA);
        withStyle.equip(DAGGER);
        withStyle.drawWeapon(DAGGER);

        // Wielding the blade costs the unstyled character the whole bonus (2 + one Título)...
        assertEquals(bare - 3, cancelled);
        // ...and costs the styled one nothing, because the blade counts as an Arma Natural.
        assertEquals(bare, defenseService.getTotalDefense(withStyle, DefenseType.PHYSICAL));
    }

    /** A real Arma Natural needs no Talento — the base case must keep working untouched. */
    @Test
    void anActualNaturalWeaponIsNaturalForAnyone() {
        assertTrue(martialArtist().treatsAsNaturalWeapon(CLAWS));
        assertTrue(CharacterFixture.blank(CharacterFixture.BLANK).build().treatsAsNaturalWeapon(CLAWS));
    }

    /** Reclassification is additive: a shield suspends the style but never unmakes real claws. */
    @Test
    void aShieldDoesNotUnmakeAnActualNaturalWeapon() throws IllegalOperationException {
        Character character = martialArtist();
        grantTheStyle(character);
        character.equip(SHIELD);

        assertTrue(character.treatsAsNaturalWeapon(CLAWS));
    }

    /** Nothing about the style depends on the weapon being equipped — the caller names the swing. */
    @Test
    void theWeaponNeedNotBeEquippedToBeReclassified() throws IllegalOperationException {
        Character character = martialArtist();
        grantTheStyle(character);

        assertEquals(List.of(), character.getEquipment());
        assertTrue(character.treatsAsNaturalWeapon(DAGGER));
    }
}
