package org.aventyrs.core.character.fixture;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.skill.Artes;
import org.aventyrs.core.skill.Attention;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.util.SimpleFixture;

public class CharacterSkillFixture extends SimpleFixture {

    public static final String ATTENTION_1 = "Attention1";
    public static final String ARTES_1 = "Artes1";

    public static void loadTemplates() {
        loadBasicSkillTemplates();
    }

    private static void loadBasicSkillTemplates() {
        Fixture.of(CharacterSkill.class).addTemplate(ATTENTION_1, new Rule() {
            {
                this.add("skill", new Attention());
            }
        });
        Fixture.of(CharacterSkill.class).addTemplate(ARTES_1, new Rule() {
            {
                this.add("skill", new Artes());
            }
        });
    }

    public static CharacterSkill.CharacterSkillBuilder blank(final String templateName) {
        return ((CharacterSkill)Fixture.from(CharacterSkill.class).gimme(templateName)).toBuilder().graduation(SkillGraduation.INITIAL_BUILDER.build());
    }
}
