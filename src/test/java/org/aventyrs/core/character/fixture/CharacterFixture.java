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
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.CharacterAttributeService;
import org.aventyrs.core.character.services.FreeActionsService;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.HitPointsService;
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
import org.aventyrs.core.skill.attention.Attention;
import org.aventyrs.core.skill.attention.AttentionCompetencyAbility;
import org.aventyrs.core.skill.conhecimentos.Conhecimentos;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagem;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemCompetencyAbility;
import org.aventyrs.core.skill.furtividade.Furtividade;
import org.aventyrs.core.skill.furtividade.FurtividadeCompetencyAbility;
import org.aventyrs.core.skill.persuasao.PersuasaoCompetencyAbility;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
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
     *
     * <p>{@code feats} is set to the same immutable {@code List.of()} every other trait list
     * here uses — a test that actually grants a Feat (via {@code
     * org.aventyrs.core.character.services.FeatService#grantFeat} or {@code Character
     * #grantFeat} directly) must first swap in a fresh mutable list, e.g. {@code
     * .toBuilder().feats(new ArrayList<>()).build()} — see {@code Character#feats}'s own
     * javadoc for why a shared mutable instance can't just be defaulted here instead (it would
     * alias across every Character built from this template).
     *
     * <p>{@code equipment} carries the exact same caveat, for the exact same reason — a test
     * that equips an {@code Item} (via {@code Character#equip}) must first do {@code
     * .toBuilder().equipment(new ArrayList<>()).build()}, or pass the items straight to the
     * builder as {@code .equipment(List.of(ArmorItem.ARMADURA_COMPLETA))} when nothing needs to
     * mutate the list afterwards.
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
                this.add("feats", List.of());
                this.add("equipment", List.of());
                this.add("drawnWeapons", List.of());
                this.add("spells", List.of());

                this.add("regaliasCraftedByGrade", Map.of());
                this.add("primaryTitle", null);
                this.add("secondaryTitle", null);
                this.add("tertiaryTitle", null);
                this.add("centelhaSuperiorSelected", false);
                // CONSCIENCIA_DEFENSIVA is the one profile that adjusts none of PA/Reações/Ações
                // Livres, so a fixture-built Character carries no hidden Perfil de Ação bonus.
                this.add("actionProfile", ActionProfile.CONSCIENCIA_DEFENSIVA);
                this.add("actionPoints", ActionPointsService.DEFAULT_ACTION_POINTS);
                this.add("temporaryActionPointsBonus", 0);
                this.add("reactions", ReactionsService.DEFAULT_REACTIONS);
                this.add("freeActions", FreeActionsService.DEFAULT_FREE_ACTIONS);
                this.add("manaMultiplier", MagicPointsService.DEFAULT_MANA_MULTIPLIER);
                this.add("lifeMultiplier", HitPointsService.DEFAULT_LIFE_MULTIPLIER);
                this.add("determinationMultiplier", DeterminationPointsService.DEFAULT_DETERMINATION_MULTIPLIER);
                this.add("sizeCategory", SizeCategory.ZERO);
            }
        });
    }

    /**
     * Every Attribute here carries a distinct total, so a roll that used the wrong one is
     * always visible in the assertion: Força 2, Destreza 5, Carisma 1, Gnose 7, Foco 8,
     * Instinto 11. Força stays low so ACUIDADE/ACROBATA's substituted Destreza is
     * distinguishable from what Ataque Corpo-a-Corpo/Atletismo would roll without it, and
     * Carisma is the lowest of all so both Perícias based on it (Persuasão, Empatia Selvagem)
     * visibly gain from substituting.
     *
     * <p>Conhecimentos is the no-leak control — Gnose-based and named by no substituting
     * Habilidade de Competência, so its roll must stay on Gnose no matter how many
     * substitutions the character holds. Persuasão can no longer serve that role now that
     * {@code PersuasaoCompetencyAbility#FORCA_OPRESSORA} is wired.
     *
     * <p>Foco and Instinto need a total above {@link CharacterAttributeService#MAX_ATTRIBUTE_BASE}
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
                        .gnose(AttributeValue.builder().domain(AttributeDomain.GNOSE).base(7).build())
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(1).build())
                        .build());
                this.add("egos", CharacterEgos.builder().build());
                this.add("egoAdvantages", Map.of());
                this.add("skills", Map.of(
                        SkillType.ATAQUE_CORPO_A_CORPO, skillWithGraduation(new AtaqueCorpoACorpo(), 1),
                        SkillType.ATLETISMO, skillWithGraduation(new Atletismo(), 2),
                        SkillType.ATAQUE_A_DISTANCIA, skillWithGraduation(new AtaqueADistancia(), 3),
                        SkillType.DOMINIO_DO_MANA, skillWithGraduation(new DominioDoMana(), 4),
                        SkillType.PERSUASAO, skillWithGraduation(new Persuasao(), 0),
                        // These three stay below ExcellencyTier.FOCADO's threshold of 3, so no
                        // unlocked Excelência adds a roll bonus of its own (EmpatiaSelvagemExcellency
                        // .FOCADO grants one) and each assertion isolates the substitution alone.
                        SkillType.ATTENTION, skillWithGraduation(new Attention(), 1),
                        SkillType.FURTIVIDADE, skillWithGraduation(new Furtividade(), 2),
                        SkillType.EMPATIA_SELVAGEM, skillWithGraduation(new EmpatiaSelvagem(), 1),
                        // The no-leak control: Conhecimentos is the Perícia no substituting
                        // Habilidade de Competência names, so its roll must stay on Gnose.
                        SkillType.CONHECIMENTOS, skillWithGraduation(new Conhecimentos(), 0)));
                this.add("attributeAbilities", List.of());
                this.add("activeAbilities", List.of());
                this.add("skillCompetencyAbilities", List.of(
                        AtaqueCorpoACorpoCompetencyAbility.ACUIDADE,
                        AtletismoCompetencyAbility.ACROBATA,
                        AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO,
                        DominioDoManaCompetencyAbility.MAGIA_SELVAGEM,
                        AttentionCompetencyAbility.ALMA_DE_SHERLOCK,
                        PersuasaoCompetencyAbility.FORCA_OPRESSORA,
                        FurtividadeCompetencyAbility.LADINO_TEORICO,
                        // INSTINTO_ANIMAL is deliberately left off — holding both Empatia Selvagem
                        // substitutions at once resolves by first-match, which is its own case (see
                        // AttributeSubstitutionFeatureTest) rather than this template's concern.
                        EmpatiaSelvagemCompetencyAbility.ACADEMICO_SELVAGEM));
                this.add("abilityChoices", List.of());
                this.add("feats", List.of());
                this.add("equipment", List.of());
                this.add("drawnWeapons", List.of());
                this.add("spells", List.of());

                this.add("regaliasCraftedByGrade", Map.of());
                this.add("primaryTitle", null);
                this.add("secondaryTitle", null);
                this.add("tertiaryTitle", null);
                this.add("centelhaSuperiorSelected", false);
                // CONSCIENCIA_DEFENSIVA is the one profile that adjusts none of PA/Reações/Ações
                // Livres, so a fixture-built Character carries no hidden Perfil de Ação bonus.
                this.add("actionProfile", ActionProfile.CONSCIENCIA_DEFENSIVA);
                this.add("actionPoints", ActionPointsService.DEFAULT_ACTION_POINTS);
                this.add("temporaryActionPointsBonus", 0);
                this.add("reactions", ReactionsService.DEFAULT_REACTIONS);
                this.add("freeActions", FreeActionsService.DEFAULT_FREE_ACTIONS);
                this.add("manaMultiplier", MagicPointsService.DEFAULT_MANA_MULTIPLIER);
                this.add("lifeMultiplier", HitPointsService.DEFAULT_LIFE_MULTIPLIER);
                this.add("determinationMultiplier", DeterminationPointsService.DEFAULT_DETERMINATION_MULTIPLIER);
                this.add("sizeCategory", SizeCategory.ZERO);
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
