package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageBaseService;
import org.aventyrs.core.character.services.DamageBaseServiceImpl;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Talento → {@code Character#getNaturalWeapons()} → {@code DamageBaseService} path, from the
 * grantors this change wires: {@link BestialFeat}'s Heranças, {@link DraconicoFeat#SOPRO_DE_DRAGAO}
 * and {@link ArmamentoDraconicoFeat}.
 */
class NaturalWeaponGrantTest {

    private final DamageBaseService damageBaseService = new DamageBaseServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private Character withFeats(final Feat... feats) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>(List.of(feats)))
                .build();
    }

    @Test
    void herancaFelinaGrantsGarrasAfiadas() {
        Character character = withFeats(BestialFeat.HERANCA_FELINA);

        assertEquals(List.of(NaturalWeapon.GARRAS_AFIADAS), character.getNaturalWeapons());
    }

    @Test
    void herancaBovideaGrantsChifresPoderosos() {
        assertEquals(List.of(NaturalWeapon.CHIFRES_PODEROSOS),
                withFeats(BestialFeat.HERANCA_BOVIDEA).getNaturalWeapons());
    }

    @Test
    void herancaCaninaGrantsPresasLongas() {
        assertEquals(List.of(NaturalWeapon.PRESAS_LONGAS),
                withFeats(BestialFeat.HERANCA_CANINA).getNaturalWeapons());
    }

    @Test
    void soproDeDragaoGrantsArmaDeSopro() {
        assertEquals(List.of(NaturalWeapon.ARMA_DE_SOPRO),
                withFeats(DraconicoFeat.SOPRO_DE_DRAGAO).getNaturalWeapons());
    }

    @Test
    void armamentoDraconicoGrantsBothChosenWeaponsAndSeveralHerancasAggregate() {
        Character character = withFeats(
                BestialFeat.HERANCA_FELINA,
                ArmamentoDraconicoFeat.of(NaturalWeapon.CHIFRES_PODEROSOS, NaturalWeapon.CAUDA_CHICOTE));

        assertTrue(character.getNaturalWeapons().containsAll(List.of(
                NaturalWeapon.GARRAS_AFIADAS, NaturalWeapon.CHIFRES_PODEROSOS, NaturalWeapon.CAUDA_CHICOTE)));
        assertEquals(3, character.getNaturalWeapons().size());
    }

    @Test
    void aBlankCharacterHasNoNaturalWeapons() {
        assertEquals(List.of(), CharacterFixture.blank(CharacterFixture.BLANK).build().getNaturalWeapons());
    }

    @Test
    void aGrantedNaturalWeaponFeedsDamageBaseServiceAtItsAuthoredRow() {
        Character character = withFeats(BestialFeat.HERANCA_FELINA);

        // Garras Afiadas is authored at 1d6+1, and this character brings no scale-ups.
        assertEquals(DamageBase.of(1, 1),
                damageBaseService.getDamageBase(character, NaturalWeapon.GARRAS_AFIADAS));
    }

    /**
     * {@code Character#getNaturalWeapons()} is the <b>Forma-blind</b> view and stays that way: it
     * is what the character has out of any shape. A Forma can add to it and even suppress it
     * outright ({@code RacialTraitSuppression}), but only through the sheet twin — this method has
     * no sheet to ask and must never grow one.
     *
     * <p>Pinned here because the swap was implemented by <em>deriving</em> rather than by writing
     * to the character. An implementation that swapped the list on transforming would show up as
     * this assertion breaking, which is exactly the signal wanted.
     */
    @Test
    void theCharacterViewIsFormaBlindAndReadsOnlyTheShortHook() {
        Character character = withFeats(BestialFeat.HERANCA_FELINA);

        assertEquals(List.of(NaturalWeapon.GARRAS_AFIADAS), character.getNaturalWeapons());
        // The long hook defaults down to the short one for every Talento but the shapeshifter's,
        // so the two agree wherever no Forma is in play — including with no sheet at all.
        assertEquals(character.getNaturalWeapons(), character.getFeats().stream()
                .flatMap(feat -> feat.getGrantedNaturalWeapons(character, null).stream())
                .toList());
    }

    @Test
    void soproDeDragaoWidensTheMinorCriticalMarginOnlyForAnArmaDeSoproAttack() {
        Weapon arco = AbstractWeapon.builder().name("Arco").category(ItemCategory.BOW)
                .damageBase(DamageBase.of(1, 1)).skillType(SkillType.ATAQUE_A_DISTANCIA).build();

        assertEquals(1, DraconicoFeat.SOPRO_DE_DRAGAO.resolveCriticalMarginIncrease(
                SkillType.ATAQUE_A_DISTANCIA, null, null, NaturalWeapon.ARMA_DE_SOPRO));
        assertEquals(0, DraconicoFeat.SOPRO_DE_DRAGAO.resolveCriticalMarginIncrease(
                SkillType.ATAQUE_A_DISTANCIA, null, null, arco));
        assertEquals(0, DraconicoFeat.SOPRO_DE_DRAGAO.resolveCriticalMarginIncrease(
                SkillType.ATAQUE_A_DISTANCIA, null, null, (org.aventyrs.core.skill.AttackSource) null));
    }
}
