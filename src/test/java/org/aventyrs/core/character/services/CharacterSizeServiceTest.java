package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.VigorAbility;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeericoFeat;
import org.aventyrs.core.feat.GiganteFeat;
import org.aventyrs.core.feat.GnomoFeat;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterSizeServiceTest {

    private final CharacterSizeService sizeService = new CharacterSizeServiceImpl();

    private Character characterOfSize(SizeCategory sizeCategory, VigorAbility... abilities) {
        return characterOfSize(sizeCategory, abilities, new Feat[0]);
    }

    private Character characterHolding(SizeCategory sizeCategory, Feat... feats) {
        return characterOfSize(sizeCategory, new VigorAbility[0], feats);
    }

    private Character characterOfSize(SizeCategory sizeCategory, VigorAbility[] abilities, Feat... feats) {
        Character.CharacterBuilder builder = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder().build())
                .sizeCategory(sizeCategory)
                .feats(new ArrayList<>(List.of(feats)));
        for (VigorAbility ability : abilities) {
            builder.attributeAbility(ability);
        }
        return builder.build();
    }

    @Test
    void defaultsToTheCharactersOwnSizeCategory() {
        Character character = characterOfSize(SizeCategory.ZERO);
        assertEquals(SizeCategory.ZERO, sizeService.getEffectiveSizeCategory(character));
    }

    @Test
    void sangueDeGiganteIncreasesEffectiveSizeCategoryByOne() {
        Character character = characterOfSize(SizeCategory.ZERO, VigorAbility.SANGUE_DE_GIGANTE);
        assertEquals(SizeCategory.PLUS_ONE, sizeService.getEffectiveSizeCategory(character));
    }

    @Test
    void bonusIsClampedAtThePlusTenCeiling() {
        Character character = characterOfSize(SizeCategory.PLUS_TEN, VigorAbility.SANGUE_DE_GIGANTE);
        assertEquals(SizeCategory.PLUS_TEN, sizeService.getEffectiveSizeCategory(character));
    }

    /**
     * A Talento stating an <b>absolute</b> Categoria de Tamanho ("muda para -2") replaces the
     * character's own, whatever that was — the distinction {@code ModifierType.SIZE_CATEGORY}
     * cannot express. Asserted from two different bases so the answer can't be a coincidental
     * one-step shift, which is exactly what {@code GnomoFeat#DUENDE}'s own comment warns about.
     */
    @Test
    void aFeatOverrideReplacesTheCharactersOwnSizeCategory() {
        assertEquals(SizeCategory.MINUS_TWO, sizeService.getEffectiveSizeCategory(
                characterHolding(SizeCategory.ZERO, GnomoFeat.DUENDE)));
        assertEquals(SizeCategory.MINUS_TWO, sizeService.getEffectiveSizeCategory(
                characterHolding(SizeCategory.PLUS_THREE, GnomoFeat.DUENDE)));
        assertEquals(SizeCategory.MINUS_THREE, sizeService.getEffectiveSizeCategory(
                characterHolding(SizeCategory.ZERO, FeericoFeat.PIXIE)));
    }

    /** A Talento's own shift is summed with the {@code @Modifier} scan, on the same base. */
    @Test
    void aFeatShiftStacksWithTheAbilityScan() {
        assertEquals(SizeCategory.PLUS_ONE, sizeService.getEffectiveSizeCategory(
                characterHolding(SizeCategory.ZERO, GiganteFeat.GIGANTE_DO_CLA_EMPUSA)));
        assertEquals(SizeCategory.PLUS_TWO, sizeService.getEffectiveSizeCategory(
                characterOfSize(SizeCategory.ZERO, new VigorAbility[] {VigorAbility.SANGUE_DE_GIGANTE},
                        GiganteFeat.GIGANTE_DO_CLA_EMPUSA)));
    }

    /** Override first, then every shift on top of it — never the other way round. */
    @Test
    void aShiftAppliesOnTopOfAnOverrideRatherThanBeingDiscardedByIt() {
        Character character = characterOfSize(SizeCategory.PLUS_THREE,
                new VigorAbility[] {VigorAbility.SANGUE_DE_GIGANTE},
                GnomoFeat.DUENDE, GiganteFeat.GIGANTE_DO_CLA_EMPUSA);

        assertEquals(SizeCategory.ZERO, sizeService.getEffectiveSizeCategory(character));
    }
}
