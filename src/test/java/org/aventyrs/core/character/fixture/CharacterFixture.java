package org.aventyrs.core.character.fixture;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.Human;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.FreeActionsService;
import org.aventyrs.core.character.services.ReactionsService;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.util.SimpleFixture;

import java.util.List;
import java.util.Map;

public class CharacterFixture extends SimpleFixture {

    public static final String BLANK = "Blank";

    public static void loadTemplates() {
        loadCharacterTemplates();
    }

    /**
     * Fixture Factory builds Character through its (package-private) no-arg constructor and
     * then sets every Rule property via reflection — it never goes through the Lombok
     * builder, so none of Character's {@code @Builder.Default} values apply automatically.
     * Every field must be listed here, including the ones that just mirror those defaults.
     */
    private static void loadCharacterTemplates() {
        Fixture.of(Character.class).addTemplate(BLANK, new Rule() {
            {
                this.add("player", new Player());
                this.add("name", "Test");
                this.add("race", new Human());
                this.add("attributes", CharacterAttributes.builder().build());
                this.add("egos", CharacterEgos.builder().build());
                this.add("autocontroleAdvantage", null);
                this.add("skills", Map.of());
                this.add("attributeAbilities", List.of());
                this.add("skillCompetencyAbilities", List.of());
                this.add("abilityChoices", List.of());
                this.add("actionProfile", ActionProfile.REFLEXOS_RAPIDOS);
                this.add("actionPoints", ActionPointsService.DEFAULT_ACTION_POINTS);
                this.add("temporaryActionPointsBonus", 0);
                this.add("reactions", ReactionsService.DEFAULT_REACTIONS);
                this.add("freeActions", FreeActionsService.DEFAULT_FREE_ACTIONS);
                this.add("sizeCategory", SizeCategory.ZERO);
                this.add("status", CharacterStatus.CLEAN);
            }
        });
    }

    public static Character.CharacterBuilder blank(final String templateName) {
        return ((Character) Fixture.from(Character.class).gimme(templateName)).toBuilder();
    }
}
