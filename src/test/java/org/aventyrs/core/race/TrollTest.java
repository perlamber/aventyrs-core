package org.aventyrs.core.race;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TargetScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrollTest {

    private final Troll troll = new Troll();

    @Test
    void generateEmptyCharacterSeedsTheYoungestZeroSizeCategory() {
        Character character = troll.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(troll)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.ZERO, character.getSizeCategory());
    }

    @Test
    void hasMonstruosoCreatureTypeAndZeroBaseSizeCategory() {
        assertEquals(CreatureType.MONSTRUOSO, troll.getCreatureType());
        assertEquals(SizeCategory.ZERO, troll.getBaseSizeCategory());
    }

    @Test
    void hasAFixedTwoPointStrengthRacialBonus() {
        assertEquals(Map.of(AttributeDomain.STRENGTH, 2), troll.getFixedAttributeBonuses());
    }

    @Test
    void anatomiaVegetalGrantsTheThreeNamedCriticalEffectImmunities() {
        assertEquals(Set.of(CriticalEffectType.ATORDOANTE,
                        CriticalEffectType.FERIDA_PROFUNDA,
                        CriticalEffectType.SANGRAMENTO),
                troll.getCriticalEffectImmunities());
    }

    /**
     * The Blessing the ability itself declares — what it actually buys once granted is {@code
     * ReactiveRegenerationTest}'s subject.
     */
    @Test
    void regeneracaoReativaIsAHabilidadeRacialGrantingATwoHitPointBlessing() {
        assertEquals(List.of(TrollsRacialAbility.REGENERACAO_REATIVA), troll.getRacialAbilities());

        Character character = Character.builder().name("Troll").race(troll)
                .player(new Player())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(3).build())
                        .build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();
        List<Blessing> blessings = TrollsRacialAbility.REGENERACAO_REATIVA
                .resolveDamageTakenBlessings(CharacterSheet.of(character, new Player()), 7);

        assertEquals(1, blessings.size());
        Blessing regeneration = blessings.get(0);
        assertEquals(ModifierType.REGENERATION, regeneration.getModifierType());
        assertEquals(2, regeneration.getValue());
        // Rodadas equal to the holder's own Vigor, and a total capped at the triggering damage.
        assertEquals(3, regeneration.getRounds());
        assertEquals(7, regeneration.getTotalLimit());
        assertEquals(TargetScope.SELF, regeneration.getScope());
        // "Efeito não cumulativo" — a floor the ability applies itself, on a Character built
        // straight off the builder with no Talentos at all.
        assertEquals(1, regeneration.getMaximumSimultaneous());

        assertTrue(new Human().getRacialAbilities().isEmpty());
    }

    @Test
    void hasNoChoosableRacialBonuses() {
        assertEquals(0, troll.getChoosableAttributeBonusPoints());
        assertTrue(troll.getChoosableAttributes().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, troll.getNewFeatCost(FeatCategory.TROLL));
        assertEquals(Race.BASE_NEW_SKILL_COST, troll.getNewSkillCost());
    }

    @Test
    void isNotMestico() {
        assertFalse(troll.isMestico());
    }
}
