package org.aventyrs.core.monster;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.ResourceFormula;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.ego.EgoAdvantage;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.monster.model.AbilityContext;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A monster as {@code criacao-de-monstros.txt} builds it — the Mestre's <i>choices</i>, from which
 * {@link MonsterRules} derives every number and {@link #spawn(Player)} builds a playable {@link
 * MonsterSheet}.
 *
 * <p>What is chosen here and what is derived:
 *
 * <table>
 *   <caption>Choice → derived</caption>
 *   <tr><td>{@link #powerDegree}</td><td>the Categoria, and with it every ceiling, the ×PV, the
 *   PA and Ego bonuses, and the budgets for Habilidades, GD upgrades, Talentos and Ego points</td></tr>
 *   <tr><td>{@link #attributeBases} (10 points above base 1, max 5)</td><td>the half-Atributo
 *   modifier on every GD, PV/PD/PM</td></tr>
 *   <tr><td>{@link #trainedSkills}, {@link #gnoseUpgrades}, {@link #progressionUpgrades}</td>
 *   <td>each Perícia's GD — there are no Graduações</td></tr>
 *   <tr><td>{@link #models}, {@link #abilities}</td><td>Bônus Racial, PA, Iniciativa, Movimento,
 *   GD steps, the Efeitos Ativos, +2PV each</td></tr>
 *   <tr><td>—</td><td>DF/DM: the Esquiva e Aparar GD plus equipment and Habilidades</td></tr>
 * </table>
 *
 * <p>{@link #adjustments} is the Mestre's override on top; {@link MonsterRules#validate} checks
 * everything else and returns what it finds instead of throwing, so a half-built monster is still
 * a value an editor can hold. {@link #spawn} does not validate: a monster that breaks the budget
 * still plays, exactly as the builder-bypassable {@code Character} always has.
 *
 * <p>The Exemplar-only fields ({@link #attributeAbilities}, {@link #skillCompetencyAbilities},
 * {@link #skillSpecializations}, {@link #egoAdvantages}, the two Famas) are carried for either kind
 * and flagged by validation on a Regular.
 */
@Getter
@Builder(toBuilder = true)
public class MonsterBlueprint {

    /** The creation budget — "recebe, em sua Criação 10 pontos de Atributos Base". */
    public static final int ATTRIBUTE_POINTS = 10;

    /** "iniciados em Base 1". */
    public static final int MINIMUM_ATTRIBUTE_BASE = 1;

    /** "e limitados à Base 5". */
    public static final int MAXIMUM_ATTRIBUTE_BASE = 5;

    /** "Os Pontos de Egos dos Monstros possuem Base 2". */
    public static final int EGO_BASE = 2;

    @NonNull
    private final String name;

    private final int powerDegree;

    @NonNull
    @Builder.Default
    private final MonsterKind kind = MonsterKind.REGULAR;

    @NonNull
    @Builder.Default
    private final SizeCategory sizeCategory = SizeCategory.ZERO;

    /** Atributo bases; a domain left out stays at base 1. */
    @NonNull
    @Singular("attributeBase")
    private final Map<AttributeDomain, Integer> attributeBases;

    /** "treinados em qualquer número de Perícias, de acordo com o que for coerente para seu tipo". */
    @NonNull
    @Singular
    private final Set<SkillType> trainedSkills;

    /** "uma quantidade de Perícias diferentes igual a Gnose do Monstro para elevar o GD em +1 Nível". */
    @NonNull
    @Singular
    private final Set<SkillType> gnoseUpgrades;

    /** "Para cada 4 pontos em seu Grau de Poder uma Perícia é aprimorada em GD +1 nível" — steps per Perícia. */
    @NonNull
    @Singular("progressionUpgrade")
    private final Map<SkillType, Integer> progressionUpgrades;

    @NonNull
    @Singular
    private final List<MonsterModel> models;

    @NonNull
    @Singular
    private final List<MonstrousAbilitySelection> abilities;

    /** Talentos Gerais or Monstruosos. */
    @NonNull
    @Singular
    private final List<Feat> feats;

    /** Ego points allocated above every Ego's base 2. */
    @NonNull
    @Singular("egoPoint")
    private final Map<EgoDomain, Integer> egoAllocation;

    @NonNull
    @Singular("equipmentItem")
    private final List<Item> equipment;

    @NonNull
    @Builder.Default
    private final MonsterAdjustments adjustments = MonsterAdjustments.NONE;

    /** See {@link MonsterTemplate#isUndead()}. */
    private final boolean undead;

    @NonNull
    @Singular
    private final Set<CriticalEffectType> criticalEffectImmunities;

    // ---- Exemplar only ------------------------------------------------------------------------

    @NonNull
    @Singular
    private final List<AttributeAbility> attributeAbilities;

    @NonNull
    @Singular
    private final List<SkillCompetencyAbility> skillCompetencyAbilities;

    @NonNull
    @Singular("skillSpecialization")
    private final Map<SkillType, List<SkillSpecialization>> skillSpecializations;

    @NonNull
    @Singular
    private final Map<EgoDomain, EgoAdvantage> egoAdvantages;

    private final int famaPositiva;

    private final int famaNegativa;

    /** The Categoria its Grau de Poder puts it in. */
    public MonsterCategory getCategory() {
        return MonsterCategory.forPowerDegree(powerDegree);
    }

    /** Its base in domain — 1 when the blueprint names none. */
    public int getAttributeBase(@NonNull final AttributeDomain domain) {
        return attributeBases.getOrDefault(domain, MINIMUM_ATTRIBUTE_BASE);
    }

    public int getProgressionUpgrades(@NonNull final SkillType skill) {
        return progressionUpgrades.getOrDefault(skill, 0);
    }

    /**
     * The context selection's hooks answer against: this blueprint's Categoria, its picks, and
     * whether it is the first held Habilidade to name its {@code sharedTraitKey()} (so a shared
     * trait is granted once).
     */
    public AbilityContext contextFor(@NonNull final MonstrousAbilitySelection selection) {
        String key = selection.ability().sharedTraitKey();
        boolean owns = true;
        if (key != null) {
            for (MonstrousAbilitySelection held : abilities) {
                if (held == selection) {
                    break;
                }
                if (key.equals(held.ability().sharedTraitKey())) {
                    owns = false;
                    break;
                }
            }
        }
        return new AbilityContext(getCategory(), selection.choices(), owns);
    }

    /** Each held Habilidade, resolved at this blueprint's Categoria. */
    public List<MonstrousAbilityGrant> resolveGrants() {
        MonsterCategory category = getCategory();
        return abilities.stream()
                .map(selection -> new MonstrousAbilityGrant(selection.ability(), contextFor(selection)))
                .toList();
    }

    /** A fresh, independent monster with a new identity — see {@link MonsterTemplate#spawn(Player)}. */
    public MonsterSheet spawn(@NonNull final Player gm) {
        MonsterSheet sheet = MonsterSheet.of(buildCharacter(), gm, this);
        applyStandingEffects(sheet);
        return sheet;
    }

    /** {@link #spawn(Player)} with a known id — the persistence-restore path. */
    public MonsterSheet spawn(@NonNull final Player gm, @NonNull final UUID id) {
        MonsterSheet sheet = MonsterSheet.of(buildCharacter(), gm, this, id);
        applyStandingEffects(sheet);
        return sheet;
    }

    /**
     * Puts on sheet what its Habilidades hold from the first Rodada — a standing Regeneração, a
     * Roubo de Vida ({@link MonstrousAbility#resolveStandingEffects}) — and takes off for one that
     * never lands. {@link #spawn} already calls it; call it yourself only on a sheet built from
     * {@link #buildCharacter()} some other way (a client wrapping the {@code Character} in a {@code
     * CharacterSheet}). Fresh effects every call, so call it once per sheet.
     */
    public void applyStandingEffects(@NonNull final CombatantSheet sheet) {
        MonsterCategory category = getCategory();
        for (MonstrousAbilitySelection selection : abilities) {
            selection.ability().resolveStandingEffects(contextFor(selection), sheet.getCharacter())
                    .forEach(sheet::applyEffect);
        }
        if (sheet.isFlying()) {
            // A holder that keepsFlying() is airborne from the start — apply what flight grants it.
            sheet.setFlying(true);
        }
    }

    /**
     * The {@link Character} this blueprint describes. Every call builds new mutable parts (skills,
     * feats, equipment lists, Efeitos Ativos), so two spawns never share state.
     */
    public Character buildCharacter() {
        MonsterCategory category = getCategory();
        List<MonstrousAbilityGrant> grants = resolveGrants();

        CharacterAttributes.CharacterAttributesBuilder attributes = CharacterAttributes.builder();
        for (AttributeDomain domain : AttributeDomain.values()) {
            attributes = assign(attributes, domain, AttributeValue.builder()
                    .domain(domain)
                    .base(getAttributeBase(domain))
                    .fixedRacialBonus(MonsterRules.racialBonus(this, domain))
                    .build());
        }

        CharacterEgos.CharacterEgosBuilder egos = CharacterEgos.builder();
        for (EgoDomain domain : EgoDomain.values()) {
            EgoValue value = EgoValue.builder().base(EGO_BASE + egoAllocation.getOrDefault(domain, 0)).build();
            switch (domain) {
                case AUTOCONTROLE -> egos.autocontrole(value);
                case RECURSOS -> egos.recursos(value);
                case SORTE -> egos.sorte(value);
                case INICIATIVA -> egos.iniciativa(value);
            }
        }

        Map<SkillType, CharacterSkill> skills = new EnumMap<>(SkillType.class);
        for (SkillType skill : trainedSkills) {
            skills.put(skill, CharacterSkill.builder()
                    .skill(skill.newSkillInstance())
                    .specializations(skillSpecializations.getOrDefault(skill, List.of()))
                    .graduation(SkillGraduation.builder().build())
                    .build());
        }

        List<AttributeAbility> allAttributeAbilities = new ArrayList<>(grants);
        allAttributeAbilities.addAll(attributeAbilities);

        List<ActiveAbility> actives = new ArrayList<>();
        for (MonstrousAbilitySelection selection : abilities) {
            actives.addAll(selection.ability().resolveActiveAbilities(contextFor(selection)));
        }
        attributeAbilities.forEach(ability -> ability.resolveActiveAbility().ifPresent(actives::add));

        List<Feat> allFeats = new ArrayList<>(feats);
        for (MonstrousAbilitySelection selection : abilities) {
            for (Feat granted : selection.ability().resolveGrantedFeats(contextFor(selection))) {
                if (!allFeats.contains(granted)) {
                    allFeats.add(granted);
                }
            }
        }

        Character character = Character.builder()
                .name(name)
                .race(MonsterTemplate.MONSTER_RACE)
                .attributes(attributes.build())
                .egos(egos.build())
                .egoAdvantages(egoAdvantages)
                .actionProfile(MonsterTemplate.DEFAULT_ACTION_PROFILE)
                .actionPoints(MonsterRules.fixedActionPoints(this))
                .skills(skills)
                .attributeAbilities(allAttributeAbilities)
                .skillCompetencyAbilities(skillCompetencyAbilities)
                .activeAbilities(actives)
                .equipment(new ArrayList<>(equipment))
                .feats(allFeats)
                .sizeCategory(sizeCategory)
                .resourceFormula(ResourceFormula.MONSTER)
                .lifeMultiplier(MonsterRules.lifeMultiplier(this))
                .manaMultiplier(MonsterRules.manaMultiplier(this))
                .determinationMultiplier(MonsterRules.determinationMultiplier(this))
                .build();
        // "Aprende 2 Árvores de Magia" — the raw mutator, like every grant here: the monster's own
        // spell budget is its Habilidade's, not SpellService's (which takes a CharacterSheet).
        for (MonstrousAbilitySelection selection : abilities) {
            for (org.aventyrs.core.magic.Spell spell : selection.ability().resolveGrantedSpells(contextFor(selection))) {
                if (!character.getSpells().contains(spell)) {
                    character.grantSpell(spell);
                }
            }
        }
        return character;
    }

    private static CharacterAttributes.CharacterAttributesBuilder assign(
            final CharacterAttributes.CharacterAttributesBuilder builder, final AttributeDomain domain,
            final AttributeValue value) {
        CharacterAttributes.assign(builder, domain, value);
        return builder;
    }
}
