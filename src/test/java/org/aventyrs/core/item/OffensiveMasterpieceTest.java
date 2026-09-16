package org.aventyrs.core.item;

import java.util.List;
import java.util.Map;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageBaseService;
import org.aventyrs.core.character.services.DamageBaseServiceImpl;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.magic.SpellDurationService;
import org.aventyrs.core.magic.SpellDurationServiceImpl;
import org.aventyrs.core.magic.catalog.OcultacaoSpell;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OffensiveMasterpieceTest {

    private final DefenseService defenseService = new DefenseServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();
    private final DamageBaseService damageBaseService = new DamageBaseServiceImpl();
    private final SpellDurationService durationService = new SpellDurationServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneCatalogEntryForEveryOffensiveMasterpiece() {
        assertEquals(17, OffensiveMasterpiece.values().length);
        assertEquals(ItemRarity.COMMON, OffensiveMasterpiece.PRECISA.getRarity());
        assertEquals(ItemRarity.MYTHIC, OffensiveMasterpiece.DENTE_DE_DRAGAO.getRarity());
        assertEquals(ItemRarity.MYTHIC, OffensiveMasterpiece.ESPIRITO_UMBRAL.getRarity());
    }

    /**
     * The Ataque/Danos pair the Defensivas table has no columns for. Authored and read by nothing —
     * see the enum's javadoc — so this pins the transcription, not a behaviour.
     */
    @Test
    void authorsTheTwoColumnsTheDefensiveTableLacks() {
        assertEquals(1, OffensiveMasterpiece.PRECISA.getAttackBonus());
        assertEquals(0, OffensiveMasterpiece.PRECISA.getDamageBonus());
        assertEquals(0, OffensiveMasterpiece.BRUTAL.getAttackBonus());
        assertEquals(1, OffensiveMasterpiece.BRUTAL.getDamageBonus());
        assertEquals(1, OffensiveMasterpiece.BANHADA_EM_PRATA.getAttackBonus());
        assertEquals(1, OffensiveMasterpiece.BANHADA_EM_PRATA.getDamageBonus());
        assertEquals(1, OffensiveMasterpiece.BANHADA_EM_PRATA.getCastingBonus());

        assertEquals(0, DefensiveMasterpiece.REFORCADA.getAttackBonus());
        assertEquals(0, DefensiveMasterpiece.REFORCADA.getDamageBonus());
    }

    @Test
    void isPricedOffTheArmasColumn() {
        assertEquals(EnhancementPriceCategory.WEAPON, OffensiveMasterpiece.PRECISA.getPriceCategory());

        assertEquals(6, OffensiveMasterpiece.PRECISA.getPriceModifier());            // Comum
        assertEquals(12, OffensiveMasterpiece.BRUTAL.getPriceModifier());            // Incomum
        assertEquals(24, OffensiveMasterpiece.MAGISTRAL.getPriceModifier());         // Raro
        assertEquals(36, OffensiveMasterpiece.MITRAL.getPriceModifier());            // Épico
        assertEquals(48, OffensiveMasterpiece.DENTE_DE_DRAGAO.getPriceModifier());   // Mítico
    }

    /** Armas and Armaduras disagree at every tier but Incomum and Mítico, which is why the column matters. */
    @Test
    void theArmasColumnDivergesFromTheArmadurasOne() {
        assertFalse(OffensiveMasterpiece.PRECISA.getPriceModifier()
                == DefensiveMasterpiece.REFORCADA.getPriceModifier());
        assertEquals(OffensiveMasterpiece.BRUTAL.getPriceModifier(),
                DefensiveMasterpiece.CONJURADORA.getPriceModifier());
    }

    @Test
    void defensoraGrantsItsDefensesUnconditionallyAndItsDamageReductionOnlyOnRequirements() {
        AbstractWeapon espada = weaponWith(OffensiveMasterpiece.DEFENSORA);

        Character untrained = characterWith(Map.of(AttributeDomain.DEXTERITY, 3), espada);
        assertEquals(1, defenseService.getTotalDefense(untrained, DefenseType.PHYSICAL));
        assertEquals(1, defenseService.getTotalDefense(untrained, DefenseType.MAGIC));
        assertEquals(0, damageService.getTotalDamageReduction(untrained));

        Character qualified = characterWith(
                Map.of(AttributeDomain.DEXTERITY, 3, AttributeDomain.GNOSE, 3), espada);
        assertEquals(1, damageService.getTotalDamageReduction(qualified));
    }

    @Test
    void brutalAndMagistralRaiseTheirWeaponDanoBaseOnlyOnRequirements() {
        AbstractWeapon brutal = weaponWith(OffensiveMasterpiece.BRUTAL);
        AbstractWeapon magistral = weaponWith(OffensiveMasterpiece.MAGISTRAL);

        assertEquals(DamageBase.of(2, 0), damageBaseService.getDamageBase(
                characterWith(Map.of(), brutal), brutal));
        assertEquals(DamageBase.of(2, 1), damageBaseService.getDamageBase(
                characterWith(Map.of(AttributeDomain.STRENGTH, 3), brutal), brutal));

        assertEquals(DamageBase.of(2, 0), damageBaseService.getDamageBase(
                characterWith(Map.of(AttributeDomain.STRENGTH, 3), magistral), magistral));
        assertEquals(DamageBase.of(2, 1), damageBaseService.getDamageBase(characterWith(
                Map.of(AttributeDomain.STRENGTH, 3, AttributeDomain.DEXTERITY, 3), magistral), magistral));
    }

    /** Adamantina's scale-up is a Característica Adicional, not a Favor, so no Requisitos gate it. */
    @Test
    void adamantinaRaisesItsWeaponDanoBaseWithNoRequirement() {
        AbstractWeapon adamantina = weaponWith(OffensiveMasterpiece.ADAMANTINA);

        assertEquals(DamageBase.of(2, 1),
                damageBaseService.getDamageBase(characterWith(Map.of(), adamantina), adamantina));
    }

    /**
     * "Dano Base da <i>Arma</i> aumenta em +1" is about the weapon the Obra-Prima is fitted to. A
     * second weapon in the same loadout must be swung at its own authored Dano Base — and carrying
     * two Brutais must not stack them onto either.
     */
    @Test
    void anOffensiveMasterpieceRaisesOnlyItsOwnHostDanoBase() {
        AbstractWeapon brutalAdaga = weaponWith(OffensiveMasterpiece.BRUTAL);
        AbstractWeapon brutalEspada = weaponWith(OffensiveMasterpiece.BRUTAL);
        AbstractWeapon plainMaca = weaponWith(null);
        Character character = characterWith(Map.of(AttributeDomain.STRENGTH, 3),
                brutalAdaga, brutalEspada, plainMaca);

        assertEquals(DamageBase.of(2, 1), damageBaseService.getDamageBase(character, brutalAdaga));
        assertEquals(DamageBase.of(2, 1), damageBaseService.getDamageBase(character, brutalEspada));
        assertEquals(DamageBase.of(2, 0), damageBaseService.getDamageBase(character, plainMaca));
    }

    /** A Magia is cast with no weapon in particular, so Poderosa's Duração is not host-scoped. */
    @Test
    void poderosaExtendsEverySpellDurationItsWielderCasts() {
        AbstractWeapon cajado = weaponWith(OffensiveMasterpiece.PODEROSA);
        Character caster = characterWith(Map.of(), cajado);

        assertEquals(25, durationService.resolveDurationInRounds(
                OcultacaoSpell.OCULTAR_SE_NAS_SOMBRAS, caster).orElseThrow());
        assertEquals(24, durationService.resolveDurationInRounds(
                OcultacaoSpell.OCULTAR_SE_NAS_SOMBRAS, characterWith(Map.of(), weaponWith(null)))
                .orElseThrow());
    }

    @Test
    void anOffensiveMasterpieceCannotBeFittedToADefensiveItem() {
        AbstractItem armadura = AbstractItem.builder()
                .name("Peitoral").category(ItemCategory.ARMOR).build();

        assertThrows(IllegalArgumentException.class,
                () -> armadura.setMasterpiece(OffensiveMasterpiece.PRECISA));
    }

    /** The offensive catalog needs no per-copy wrapper, so it is fitted bare. */
    @Test
    void anOffensiveMasterpieceIsFittedWithoutAWrapper() {
        AbstractWeapon espada = weaponWith(OffensiveMasterpiece.PRECISA);

        assertEquals(OffensiveMasterpiece.PRECISA, espada.getMasterpiece());
    }

    /** Names recur across the two lists; nothing else about a shared name does. */
    @Test
    void sharesNamesWithTheDefensiveCatalogAndNothingElse() {
        assertEquals(DefensiveMasterpiece.MITRAL.getName(), OffensiveMasterpiece.MITRAL.getName());
        assertFalse(DefensiveMasterpiece.MITRAL.getRarity() != OffensiveMasterpiece.MITRAL.getRarity());
        assertFalse(DefensiveMasterpiece.MITRAL.getFavorDescription()
                .equals(OffensiveMasterpiece.MITRAL.getFavorDescription()));
        assertTrue(DefensiveMasterpiece.CONJURADORA.getRarity() == OffensiveMasterpiece.CONJURADORA.getRarity());
        assertFalse(DefensiveMasterpiece.CONJURADORA.getFavorDescription()
                .equals(OffensiveMasterpiece.CONJURADORA.getFavorDescription()));
    }

    private AbstractWeapon weaponWith(final OffensiveMasterpiece masterpiece) {
        AbstractWeapon weapon = AbstractWeapon.builder()
                .name("Arma de teste")
                .category(ItemCategory.LIGHT_BLADE)
                .weightClass(ItemWeightClass.LIGHT)
                .damageBase(DamageBase.of(2, 0))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
        if (masterpiece != null) {
            weapon.setMasterpiece(masterpiece);
        }
        return weapon;
    }

    private Character characterWith(final Map<AttributeDomain, Integer> attributes, final Item... equipment) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(attributes))
                .equipment(List.of(equipment))
                .build();
    }
}
