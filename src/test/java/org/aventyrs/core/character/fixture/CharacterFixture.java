package org.aventyrs.core.character.fixture;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.CharacterAttributeService;
import org.aventyrs.core.character.services.FreeActionsService;
import org.aventyrs.core.character.services.MagicPointsService;
import org.aventyrs.core.character.services.ReactionsService;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistancia;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaCompetencyAbility;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoCompetencyAbility;
import org.aventyrs.core.skill.atletismo.Atletismo;
import org.aventyrs.core.skill.atletismo.AtletismoCompetencyAbility;
import org.aventyrs.core.skill.dominiodomana.DominioDoMana;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaCompetencyAbility;
import org.aventyrs.core.skill.persuasao.Persuasao;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistancia;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaCompetencyAbility;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoCompetencyAbility;
import org.aventyrs.core.skill.atletismo.Atletismo;
import org.aventyrs.core.skill.atletismo.AtletismoCompetencyAbility;
import org.aventyrs.core.skill.dominiodomana.DominioDoMana;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaCompetencyAbility;
import org.aventyrs.core.skill.persuasao.Persuasao;
import org.aventyrs.core.util.SimpleFixture;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CharacterFixture extends SimpleFixture {

    public static final String BLANK = "Blank";

    /**
     * A character holding every currently-wired unconditional base-Attribute-substituting
     * Habilidade de Competência at once (one per Perícia — see CLAUDE.md's "Unconditional
     * Perícia base-Attribute substitution" section), each Perícia's own Attributes given
     * distinct values so a feature test can tell whether a roll used the substituted
     * Attribute or leaked another Perícia's.
     */
    public static final String ATTRIBUTE_SUBSTITUTIONS = "AttributeSubstitutions";

    public static void loadTemplates() {
        loadCharacterTemplates();
        loadAttributeSubstitutionsTemplate();
    }

    /**
     * Fixture Factory builds Character through its (package-private) no-arg constructor and
     * then sets every Rule property via reflection — it never goes through the Lombok
     * builder, so none of Character's {@code @Builder.Default} values apply automatically.
     * Every field must be listed here, including the ones that just mirror those defaults.
     *
     * <p>{@code id} is the one field where that matters beyond just "don't forget it": this
     * Rule's {@code UUID.randomUUID()} call runs once, when {@link #loadTemplates()} is
     * called — not once per {@link #blank}/{@code gimme} — so every Character built from
     * {@link #BLANK} within the same test shares that one {@code id}. Fine for tests that
     * don't care about identity; a test that needs several distinct Characters (e.g. for
     * {@code Scene}'s allies) must override {@code .id(UUID.randomUUID())} on each one via
     * {@link #blank}'s returned builder.
     */
    private static void loadCharacterTemplates() {
        Fixture.of(Character.class).addTemplate(BLANK, new Rule() {
            {
                this.add("id", UUID.randomUUID());
                this.add("player", new Player());
                this.add("name", "Test");
                this.add("race", new Human());
                this.add("sexo", null);
                this.add("deity", null);
                this.add("tendencia", 1);
                this.add("attributes", CharacterAttributes.builder().build());
                this.add("egos", CharacterEgos.builder().build());
                this.add("egoAdvantages", Map.of());
                this.add("skills", Map.of());
                this.add("attributeAbilities", List.of());
                this.add("activeAbilities", List.of());
                this.add("skillCompetencyAbilities", List.of());
                this.add("abilityChoices", List.of());
                this.add("primaryTitle", null);
                this.add("secondaryTitle", null);
                this.add("tertiaryTitle", null);
                this.add("actionProfile", ActionProfile.REFLEXOS_RAPIDOS);
                this.add("actionPoints", ActionPointsService.DEFAULT_ACTION_POINTS);
                this.add("temporaryActionPointsBonus", 0);
                this.add("reactions", ReactionsService.DEFAULT_REACTIONS);
                this.add("freeActions", FreeActionsService.DEFAULT_FREE_ACTIONS);
                this.add("manaMultiplier", MagicPointsService.DEFAULT_MANA_MULTIPLIER);
                this.add("sizeCategory", SizeCategory.ZERO);
                this.add("status", CharacterStatus.CLEAN);
            }
        });
    }

    /**
     * Força is left at its default (untouched) so ACUIDADE/ACROBATA's substituted Destreza is
     * distinguishable from what Ataque Corpo-a-Corpo/Atletismo would roll without it; Carisma
     * is likewise left untouched as Persuasão's control (no substituting ability targets it).
     * Foco and Instinto need a total above {@link CharacterAttributeService#MAX_ATTRIBUTE_BASE}
     * to stay distinguishable from Destreza's own total, so the excess is modeled as
     * {@code variable} (spells/feats/equipment), not {@code base} — a real character can never
     * have a base above 5; only bonuses from other sources push the total higher.
     */
    private static void loadAttributeSubstitutionsTemplate() {
        Fixture.of(Character.class).addTemplate(ATTRIBUTE_SUBSTITUTIONS, new Rule() {
            {
                this.add("id", UUID.randomUUID());
                this.add("player", new Player());
                this.add("name", "Test");
                this.add("race", new Human());
                this.add("sexo", null);
                this.add("deity", null);
                this.add("tendencia", 1);
                this.add("attributes", CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(2).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(5).build())
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(5).variable(3).build())
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(5).variable(6).build())
                        .build());
                this.add("egos", CharacterEgos.builder().build());
                this.add("egoAdvantages", Map.of());
                this.add("skills", Map.of(
                        SkillType.ATAQUE_CORPO_A_CORPO, skillWithGraduation(new AtaqueCorpoACorpo(), 1),
                        SkillType.ATLETISMO, skillWithGraduation(new Atletismo(), 2),
                        SkillType.ATAQUE_A_DISTANCIA, skillWithGraduation(new AtaqueADistancia(), 3),
                        SkillType.DOMINIO_DO_MANA, skillWithGraduation(new DominioDoMana(), 4),
                        SkillType.PERSUASAO, skillWithGraduation(new Persuasao(), 0)));
                this.add("attributeAbilities", List.of());
                this.add("activeAbilities", List.of());
                this.add("skillCompetencyAbilities", List.of(
                        AtaqueCorpoACorpoCompetencyAbility.ACUIDADE,
                        AtletismoCompetencyAbility.ACROBATA,
                        AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO,
                        DominioDoManaCompetencyAbility.MAGIA_SELVAGEM));
                this.add("abilityChoices", List.of());
                this.add("primaryTitle", null);
                this.add("secondaryTitle", null);
                this.add("tertiaryTitle", null);
                this.add("actionProfile", ActionProfile.REFLEXOS_RAPIDOS);
                this.add("actionPoints", ActionPointsService.DEFAULT_ACTION_POINTS);
                this.add("temporaryActionPointsBonus", 0);
                this.add("reactions", ReactionsService.DEFAULT_REACTIONS);
                this.add("freeActions", FreeActionsService.DEFAULT_FREE_ACTIONS);
                this.add("manaMultiplier", MagicPointsService.DEFAULT_MANA_MULTIPLIER);
                this.add("sizeCategory", SizeCategory.ZERO);
                this.add("status", CharacterStatus.CLEAN);
            }
        });
    }

    private static CharacterSkill skillWithGraduation(final Skill skill, final int graduationValue) {
        return CharacterSkill.builder()
                .skill(skill)
                .graduation(SkillGraduation.builder().graduationValue(graduationValue).build())
                .build();
    }

    public static Character.CharacterBuilder blank(final String templateName) {
        return ((Character) Fixture.from(Character.class).gimme(templateName)).toBuilder();
    }
}
