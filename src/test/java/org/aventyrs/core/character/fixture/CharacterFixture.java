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
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.util.SimpleFixture;

import java.util.List;

public class CharacterFixture extends SimpleFixture {

    public static final String BLANK = "Blank";

    public static void loadTemplates() {
        loadCharacterTemplates();
    }

    /**
     * Character has no no-arg constructor (only the all-args one Lombok's {@code @Builder}
     * generates), so Fixture Factory can only build it by matching that constructor's full
     * parameter list — every field must be present in the Rule, even the ones that just
     * mirror Character's own {@code @Builder.Default} values.
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
                this.add("skills", List.of());
                this.add("attributeAbilities", List.of());
                this.add("actionProfile", ActionProfile.REFLEXOS_RAPIDOS);
                this.add("actionPoints", ActionPointsService.DEFAULT_ACTION_POINTS);
                this.add("sizeCategory", SizeCategory.ZERO);
                this.add("status", CharacterStatus.CLEAN);
            }
        });
    }

    public static Character.CharacterBuilder blank(final String templateName) {
        return ((Character) Fixture.from(Character.class).gimme(templateName)).toBuilder();
    }
}
