package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.AbstractFeat;
import org.aventyrs.core.feat.ElementalFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.feat.FeatRequirements;
import org.aventyrs.core.feat.FeralFeat;
import org.aventyrs.core.feat.FormaMetamorfica;
import org.aventyrs.core.feat.MetamorfoseDraculeaFeat;
import org.aventyrs.core.feat.OrquicoFeat;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.race.Agastias;
import org.aventyrs.core.race.Colosso;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Phase D: each reduction reaches only the damage its rules text names, Talentos can hold
 * immunity / Meio-Dano / RE, "seu elemento" is readable off the Raças Elementais, and a Talento can
 * retype an attack.
 */
class DamageTypeScopeTest {

    private final DamageService damageService = new DamageServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet(final Race race, final Feat... feats) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>(List.of(feats)));
        if (race != null) {
            builder.race(race);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    private int hit(final CombatantSheet target, final DamageDescriptor descriptor) {
        return damageService.calculateFinalDamage(target, null, descriptor, null, 10, false);
    }

    // ---------- the reduction matrix ----------

    /** RD 3, RDS 1, RM 2 and one RE instance (-2) against Fogo, all on one Talento. */
    private static final Feat EVERY_REDUCTION = new AbstractFeat(FeatCategory.DESTINO, "test",
            FeatRequirements.builder().build()) {
        @Override
        public int resolveDamageReduction(final Character character) {
            return 3;
        }

        @Override
        public int resolveDamageTakenReduction(final Character character) {
            return 1;
        }

        @Override
        public int resolveMagicReduction(final Character character) {
            return 2;
        }

        @Override
        public int resolveElementalResistanceInstances(final ElementalType element, final Character character,
                                                        final CombatantSheet holder) {
            return element == ElementalType.FOGO ? 1 : 0;
        }
    };

    @Test
    void aPlainPhysicalHitTakesRdAndRds() {
        assertEquals(10 - 3 - 1, hit(sheet(null, EVERY_REDUCTION), new DamageDescriptor(DamageType.FISICO)));
    }

    @Test
    void anUntypedHitIsStillReadAsPlainPhysical() {
        assertEquals(10 - 3 - 1,
                damageService.calculateFinalDamage(sheet(null, EVERY_REDUCTION), null, (DamageType) null, null, 10, false));
    }

    /** "RD … reduz o Dano Físico não-PRIMORDIAL e não-ELEMENTAL" — no RD on a magic hit any more. */
    @Test
    void aMagicHitTakesRdsAndRmButNotRd() {
        assertEquals(10 - 1 - 2, hit(sheet(null, EVERY_REDUCTION), new DamageDescriptor(DamageType.MAGICO)));
    }

    @Test
    void anElementalHitOfTheResistedElementTakesRdsAndRe() {
        CharacterSheet target = sheet(null, EVERY_REDUCTION);

        assertEquals(10 - 1 - 2, hit(target, new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO)));
        assertEquals(10 - 1 - 2, hit(target, new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.FOGO)));
    }

    @Test
    void anElementalHitOfAnotherElementTakesOnlyRds() {
        assertEquals(10 - 1,
                hit(sheet(null, EVERY_REDUCTION), new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.GELO)));
    }

    @Test
    void aPrimordialHitTakesNoneOfThem() {
        assertEquals(10, hit(sheet(null, EVERY_REDUCTION), new DamageDescriptor(DamageType.PRIMORDIAL)));
    }

    // ---------- "seu elemento" ----------

    @Test
    void eachRacaElementalCarriesItsTablesElement() {
        assertEquals(ElementalType.TERRA, new Colosso(new Human()).getElement());
        assertEquals(ElementalType.ELETRICIDADE, new Agastias(new Human(), Agastias.Linhagem.TROVEJANTE).getElement());
        assertEquals(ElementalType.MAGMA, new Agastias(new Human(), Agastias.Linhagem.VULCANO).getElement());
    }

    @Test
    void resistenciaElementalGrantsReAgainstTheHoldersOwnElement() {
        CharacterSheet colosso = sheet(new Colosso(new Human()), ElementalFeat.RESISTENCIA_ELEMENTAL);

        assertEquals(1, colosso.getElementalResistanceInstances(ElementalType.TERRA));
        assertEquals(0, colosso.getElementalResistanceInstances(ElementalType.FOGO));
    }

    @Test
    void resistenciaElementalSuperiorHalvesTheHoldersOwnElement() {
        CharacterSheet colosso = sheet(new Colosso(new Human()), ElementalFeat.RESISTENCIA_ELEMENTAL_SUPERIOR);

        assertEquals(5, hit(colosso, new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.TERRA)));
        assertEquals(10, hit(colosso, new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO)));
    }

    @Test
    void imunidadeElementalNullifiesTheHoldersOwnElement() {
        CharacterSheet colosso = sheet(new Colosso(new Human()), ElementalFeat.IMUNIDADE_ELEMENTAL);

        assertEquals(0, hit(colosso, new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.TERRA)));
        assertEquals(10, hit(colosso, new DamageDescriptor(DamageType.FISICO)));
    }

    @Test
    void arcanismoElementalDiscountsOnlyMagiasOfTheHoldersElement() {
        SpellService spellService = new SpellServiceImpl();
        Character plain = sheet(new Colosso(new Human())).getCharacter();
        Character arcanist = sheet(new Colosso(new Human()), ElementalFeat.ARCANISMO_ELEMENTAL).getCharacter();
        // A Magia with a real price for the plain character (a Semente can be free).
        Spell terra = MagicTree.CORPO_ROCHOSO.getSpells().stream()
                .filter(spell -> spellService.getAcquisitionCost(plain, spell).signum() > 0).findFirst().orElseThrow();
        Spell fogo = MagicTree.PIROMANCIA.getSpells().stream()
                .filter(spell -> spellService.getAcquisitionCost(plain, spell).signum() > 0).findFirst().orElseThrow();

        assertEquals(spellService.getAcquisitionCost(plain, terra).subtract(new BigDecimal("0.5")),
                spellService.getAcquisitionCost(arcanist, terra));
        assertEquals(spellService.getAcquisitionCost(plain, fogo), spellService.getAcquisitionCost(arcanist, fogo));
    }

    // ---------- Forma immunity ----------

    @Test
    void nevoaIsImmuneToPhysicalDamageExceptFire() {
        CharacterSheet mist = sheet(null, new MetamorfoseDraculeaFeat(Set.of(FormaMetamorfica.NEVOA)));
        mist.enterForm(FormType.NEVOA);

        assertEquals(0, hit(mist, new DamageDescriptor(DamageType.FISICO)));
        assertEquals(10, hit(mist, new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.FOGO)));
        assertEquals(10, hit(mist, new DamageDescriptor(DamageType.MAGICO)));
    }

    // ---------- retyping ----------

    private static Weapon blade() {
        return AbstractWeapon.builder().name("Espada").category(ItemCategory.LIGHT_BLADE)
                .weightClass(ItemWeightClass.LIGHT).damageBase(DamageBase.of(1, 0))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
    }

    private static InteractionResult swing(final CombatantSheet attacker, final Weapon weapon) {
        return new AtaqueCorpoACorpoInteraction().applyTo(attacker, null, null, null, weapon);
    }

    @Test
    void desprezoNaturalRetypesArmasNaturaisOnly() {
        CharacterSheet feral = sheet(null, FeralFeat.DESPREZO_NATURAL);

        assertEquals(new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.NATURAL),
                swing(feral, NaturalWeapon.GARRAS_AFIADAS).getRetypedDamage());
        assertNull(swing(feral, blade()).getRetypedDamage());
    }

    @Test
    void paladinoDeEponaRetypesEveryWeaponToTerraAndAddsHalfVigor() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.VIGOR, 5)))
                .feats(new ArrayList<>(List.of(OrquicoFeat.PALADINO_DE_EPONA)))
                .build();
        Character plainCharacter = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.VIGOR, 5)))
                .feats(new ArrayList<>())
                .build();
        InteractionResult paladin = swing(CharacterSheet.of(character, new Player()), blade());
        InteractionResult plain = swing(CharacterSheet.of(plainCharacter, new Player()), blade());

        assertEquals(new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.TERRA), paladin.getRetypedDamage());
        int plainBonus = plain.getDamageBonus() == null ? 0 : plain.getDamageBonus().getValue();
        assertEquals(plainBonus + 2, paladin.getDamageBonus().getValue());
    }

    @Test
    void noRetypingTalentoReportsNone() {
        assertNull(swing(sheet(null), blade()).getRetypedDamage());
    }
}
