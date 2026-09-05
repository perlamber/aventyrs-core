package org.aventyrs.core.race;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.feat.MonstruosoFeat;
import org.aventyrs.core.feat.VampiricoFeat;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.race.Vampiro.VampiroLineage;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VampiroTest {

    private static Character characterOf(final Vampiro vampiro) {
        return vampiro.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(vampiro)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();
    }

    @Test
    void isRenascidoButCountsAsItsLifeRaceForPrerequisites() {
        Vampiro asanbosam = new Vampiro(VampiroLineage.ASANBOSAM, new Monstruoso());

        assertEquals(CreatureType.RENASCIDO, asanbosam.getCreatureType());
        assertEquals(CreatureType.MONSTRUOSO, asanbosam.getPrerequisiteCreatureType());
        assertTrue(asanbosam.isMestico());
    }

    @Test
    void aNosferatuIsHumanoideForPrerequisites() {
        assertEquals(CreatureType.HUMANOIDE,
                new Vampiro(VampiroLineage.NOSFERATU, new Human()).getPrerequisiteCreatureType());
    }

    @Test
    void eachLineageFixesItsOwnAttributeBonus() {
        assertEquals(Map.of(AttributeDomain.STRENGTH, 1),
                new Vampiro(VampiroLineage.NOSFERATU, new Human()).getFixedAttributeBonuses());
        assertEquals(Map.of(AttributeDomain.CHARISMA, 1),
                new Vampiro(VampiroLineage.BAOBHAN_SITH, new Fada()).getFixedAttributeBonuses());
    }

    @Test
    void atributoHerdadoAddsOneOnAParentGrantedAttribute() {
        Vampiro vampiro = new Vampiro(VampiroLineage.NOSFERATU, new Anao(), AttributeDomain.GNOSE, List.of());

        assertEquals(Map.of(AttributeDomain.STRENGTH, 1, AttributeDomain.GNOSE, 1),
                vampiro.getFixedAttributeBonuses());
    }

    @Test
    void atributoHerdadoMergesWhenItMatchesTheLineageBonus() {
        // Dampiro grants Destreza; Elfo grants Destreza racially, so choosing Destreza stacks to 2.
        Vampiro vampiro = new Vampiro(VampiroLineage.DAMPIRO, new Elfo(), AttributeDomain.DEXTERITY, List.of());

        assertEquals(Map.of(AttributeDomain.DEXTERITY, 2), vampiro.getFixedAttributeBonuses());
    }

    @Test
    void rejectsAnAtributoHerdadoTheParentDoesNotGrant() {
        assertThrows(IllegalOperationException.class,
                () -> new Vampiro(VampiroLineage.NOSFERATU, new Human(), AttributeDomain.GNOSE, List.of()));
    }

    @Test
    void rejectsAMesticoParent() {
        assertThrows(IllegalOperationException.class,
                () -> new Vampiro(VampiroLineage.DAMPIRO, new MeioElfo(new Human())));
    }

    @Test
    void rejectsAParentOutsideTheLineagesAllowedTypes() {
        // Asanbosam is created only from Monstros e Monstruosos.
        assertThrows(IllegalOperationException.class,
                () -> new Vampiro(VampiroLineage.ASANBOSAM, new Human()));
        // Nosferatu only from Humanoides.
        assertThrows(IllegalOperationException.class,
                () -> new Vampiro(VampiroLineage.NOSFERATU, new Monstruoso()));
    }

    @Test
    void aDampiroAcceptsAnyLivingParentType() {
        assertEquals(CreatureType.HUMANOIDE, new Vampiro(VampiroLineage.DAMPIRO, new Human()).getPrerequisiteCreatureType());
        assertEquals(CreatureType.FEERICO, new Vampiro(VampiroLineage.DAMPIRO, new Fada()).getPrerequisiteCreatureType());
        assertEquals(CreatureType.MONSTRUOSO, new Vampiro(VampiroLineage.DAMPIRO, new Monstruoso()).getPrerequisiteCreatureType());
    }

    @Test
    void inheritsTheParentsBaseSizeCategoryUnchanged() {
        Character character = characterOf(new Vampiro(VampiroLineage.DAMPIRO, new Anao()));
        assertEquals(SizeCategory.MINUS_ONE, character.getSizeCategory());
        assertEquals(SizeCategory.MINUS_ONE, new Vampiro(VampiroLineage.DAMPIRO, new Anao()).getBaseSizeCategory());
    }

    @Test
    void grantsUpToTwoInheritedRacialAbilitiesFromTheParent() {
        Vampiro vampiro = new Vampiro(VampiroLineage.NOSFERATU, new Anao(), null,
                List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES, AnoesRacialAbility.FILHOS_DA_MONTANHA));

        assertEquals(
                List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES, AnoesRacialAbility.FILHOS_DA_MONTANHA),
                vampiro.getRacialAbilities());
    }

    @Test
    void rejectsAnInheritedAbilityTheParentDoesNotHave() {
        assertThrows(IllegalOperationException.class,
                () -> new Vampiro(VampiroLineage.NOSFERATU, new Human(), null,
                        List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES)));
    }

    @Test
    void developsItsLineagesArmasNaturais() {
        assertEquals(List.of(NaturalWeapon.PRESAS_LONGAS),
                new Vampiro(VampiroLineage.NOSFERATU, new Human()).getGrantedNaturalWeapons());
        assertEquals(List.of(NaturalWeapon.GARRAS_AFIADAS),
                new Vampiro(VampiroLineage.STRIGOI, new Human()).getGrantedNaturalWeapons());

        Vampiro rakshasa = new Vampiro(VampiroLineage.RAKSHASA, new Human());
        assertEquals(Set.of(NaturalWeapon.PRESAS_LONGAS, NaturalWeapon.GARRAS_AFIADAS),
                Set.copyOf(rakshasa.getGrantedNaturalWeapons()));
    }

    @Test
    void aVampiresArmasNaturaisReachCharacterGetNaturalWeapons() {
        Character character = characterOf(new Vampiro(VampiroLineage.NOSFERATU, new Human()));
        assertEquals(List.of(NaturalWeapon.PRESAS_LONGAS), character.getNaturalWeapons());
    }

    @Test
    void gatesTheVampiricoFeatTreeAndOpensTheLifeRacesTree() {
        Character asanbosam = characterOf(new Vampiro(VampiroLineage.ASANBOSAM, new Monstruoso()));
        Character nosferatu = characterOf(new Vampiro(VampiroLineage.NOSFERATU, new Human()));
        Human plainHuman = new Human();
        Character human = plainHuman.generateEmptyCharacter(List.of())
                .player(new Player()).name("H").race(plainHuman)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS).build();

        // Every Vampiro is eligible for the Vampírico tree; no other race is.
        assertTrue(VampiricoFeat.OSTEOMANCIA.isEligible(asanbosam));
        assertTrue(VampiricoFeat.OSTEOMANCIA.isEligible(nosferatu));
        assertFalse(VampiricoFeat.OSTEOMANCIA.isEligible(human));

        // A requiredCreatureType(MONSTRUOSO) Talento checks the life-race: the Asanbosam qualifies,
        // the Nosferatu (Humanoide in life) does not.
        assertTrue(MonstruosoFeat.ALFA.isEligible(asanbosam));
        assertFalse(MonstruosoFeat.ALFA.isEligible(nosferatu));
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        Vampiro vampiro = new Vampiro(VampiroLineage.DAMPIRO, new Human());
        assertEquals(Race.BASE_NEW_FEAT_COST, vampiro.getNewFeatCost(null));
        assertEquals(Race.BASE_NEW_SKILL_COST, vampiro.getNewSkillCost());
    }
}
