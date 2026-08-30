package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.race.Anao;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.TitleArchetype;

/**
 * Talentos Anões — the Ymirian half of the Anão's own tree.
 *
 * <p><b>Four of the five carry real effects.</b> {@link #FILHO_DE_YMIR} and {@link
 * #VIGOR_DO_INVERNO} go through {@code Feat}'s multiplier hooks; {@link #VANTAGEM_DE_TAMANHO} and
 * {@link #GLORIA_YMIRIANA} are conditioned on <i>who is on the other side of the roll</i>, and
 * became expressible when {@code SceneContext#getOpposedCharacter()} landed — the target on an
 * attack roll, the attacker on a defence roll. Between them they are the reason two new {@code
 * Feat} hooks exist: a {@code SceneContext}-aware {@code resolveDefenseBonus} and {@code
 * resolveCriticalMarginIncrease}.
 *
 * <p>{@link #CONSELHEIRO_DE_GUERRA_YMIRIANO} is the one constant still inert, and the opposed
 * sheet does not help it: it grants an Atributo bonus and a free Habilidade, neither of which is
 * about an opponent.
 *
 * <p><b>The tree tag is {@code Anão}, not the general tag printed beside it.</b> Every constant
 * here carries a second tag (Sobrevivência, Bruto, Perito) that is supplementary — racial beats
 * general, per {@code docs/rules/talentos-index.md}'s scope decisions.
 *
 * <p><b>{@code requiredRace} follows the Pré-requisito line, not the tag.</b> Three of these say
 * "Personagem da Raça Anão" and get {@link Anao} as a requirement; {@link #FILHO_DE_YMIR} and
 * {@link #CONSELHEIRO_DE_GUERRA_YMIRIANO} name only an Attribute, so they are left open to any
 * race. That asymmetry is the source document's, transcribed rather than regularised — a
 * Talento printed under a racial heading is not automatically restricted to that race.
 */
public enum AnaoFeat implements Feat {

    /**
     * "Você recebe Bônus de +Metade do Vigor em Defesas para resistir aos ataques de oponentes
     * de Categorias de Tamanhos superiores à sua."
     */
    VANTAGEM_DE_TAMANHO(
            "Você recebe Bônus de +Metade do Vigor em Defesas para resistir aos ataques de "
                    + "oponentes de Categorias de Tamanhos superiores à sua.",
            FeatRequirements.builder()
                    .requiredRace(Anao.class)
                    .requiredAwakenedTitles(1)
                    .requiredTitleArchetype(TitleArchetype.BRUTO)
                    .build()) {
        /**
         * Overrides the {@code SceneContext}-aware form, since the whole clause is conditioned on
         * who is attacking. It deliberately returns 0 with no Scene: a Defesa asked for outside a
         * roll has no attacker to compare against, and defaulting to the unconditional bonus
         * would grant it against every opponent, including the smaller ones the clause excludes.
         */
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character,
                                        final SceneContext sceneContext) {
            if (sceneContext == null || sceneContext.getOpposedCharacter() == null) {
                return 0;
            }
            SizeCategory attackerSize = sceneContext.getOpposedCharacter().getCharacter().getSizeCategory();
            if (attackerSize.getCategory() <= character.getSizeCategory().getCategory()) {
                return 0;
            }
            // "Metade do Vigor", rounded down — the same reading every other "Metade de X" clause
            // in this catalog takes.
            return character.getAttributes().getVigor().getTotal() / 2;
        }
    },

    /**
     * "Seu Multiplicador de PV e Dano Base de armas aumentam em +1." Both halves real.
     *
     * <p>Note the Pré-requisito is a bare "Vigor 4" — no race clause — so despite the Anão tag
     * this Talento is open to anyone who reaches it, exactly as printed.
     */
    // TODO: "de armas" cannot be honoured — resolveDamageBaseIncrease(Character) sees neither
    //  the Weapon nor the SkillType, so the scale-up also applies to an Ataque Desarmado, which
    //  this clause excludes. Over-grants in that one case; granting nothing would be further
    //  from the text. Closing it needs the hook widened to take the AttackSource, which is the
    //  same missing distinction CLAUDE.md's "Classifying an attack as Desarmado/Arma Natural"
    //  row names.
    FILHO_DE_YMIR(
            "Seu Multiplicador de PV e Dano Base de armas aumentam em +1.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(4)
                    .build()) {
        @Override
        public int resolveLifeMultiplierIncrease(final Character character) {
            return 1;
        }

        @Override
        public int resolveDamageBaseIncrease(final Character character) {
            return 1;
        }
    },

    /**
     * "Você adquire Bônus Racial de +1 em Gnose e 1 Habilidade de Força (que você cumpra os
     * requisitos)."
     */
    // TODO: a Talento cannot grant an Atributo bonus — Race#getFixedAttributeBonuses() is the
    //  only racial-bonus hook and it belongs to the Race, not to an acquired Talento; nothing
    //  reads a Feat for AttributeValue at all.
    // TODO: the free StrengthAbility is the "grant an extra acquisition slot" gap —
    //  AttributeAbilityService#getUnlockedAbilitySlots counts slots from the raw Atributo base
    //  with no notion of an extra one, the same shape Anao's own Pequenos Gigantes cites.
    CONSELHEIRO_DE_GUERRA_YMIRIANO(
            "Você adquire Bônus Racial de +1 em Gnose e 1 Habilidade de Força (que você cumpra "
                    + "os requisitos).",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.GNOSE)
                    .requiredAttributeValue(5)
                    .build()),

    /**
     * "Você recebe os benefícios de Abatedores de Gigantes contra qualquer alvo que não seja
     * menor que você. Sua Margem Crítica Menor aumenta em +2 para atacar alvos maiores que você."
     */
    GLORIA_YMIRIANA(
            "Você recebe os benefícios de Abatedores de Gigantes contra qualquer alvo que não "
                    + "seja menor que você. Sua Margem Crítica Menor aumenta em +2 para atacar "
                    + "alvos maiores que você.",
            FeatRequirements.builder()
                    .requiredRace(Anao.class)
                    .requiredAwakenedTitles(1)
                    .requiredTitleArchetype(TitleArchetype.BRUTO)
                    .build()) {
        /**
         * "Os benefícios de Abatedores de Gigantes" is that racial ability's Vantagem on Ataque
         * rolls, with its size threshold widened from "2+ Categorias maiores" to merely "not
         * smaller" — so this grants {@code Skill#ADVANTAGE_BONUS} on either Perícia de Ataque.
         *
         * <p>It stacks with {@code AnoesRacialAbility#ABATEDORES_DE_GIGANTES} itself against a
         * target 2+ Categorias larger, where both conditions hold. That is the rules text as
         * written: this Talento restates the ability's benefit on a wider set of targets rather
         * than replacing it, and the two reach the roll through different scans.
         */
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return skillType.isAttackSkill() && targetIsNotSmallerThan(sceneContext, character)
                    ? Skill.ADVANTAGE_BONUS : 0;
        }

        /** "+2 para atacar alvos <b>maiores</b> que você" — a stricter test than the half above. */
        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                  final Character character) {
            return skillType.isAttackSkill() && targetIsLargerThan(sceneContext, character)
                    ? GLORIA_CRITICAL_MARGIN_INCREASE : 0;
        }
    },

    /**
     * "Seu Multiplicador de PV aumenta em 1. No início de cada combate você recebe RD e
     * Resistência a Críticos por uma quantidade de Rodadas igual à metade de seu Multiplicador
     * de PV." The multiplier half is real.
     */
    // TODO: the RD half needs a start-of-combat trigger, which nothing has — a Feat is scanned
    //  where a service asks, never fired by an event, and this codebase has no observer
    //  mechanism anywhere. Note the Duração would be computable once it did
    //  (HitPointsService#getLifeMultiplier / 2, this Talento's own +1 included).
    // TODO: Resistência a Críticos is not a stat this core computes — distinct from
    //  Race#getCriticalEffectImmunities(), which is an all-or-nothing filter keyed on an
    //  identity, not a resistance value. Same unbuilt piece ProfissaoCompetencyAbility and
    //  Troll's own Anatomia Vegetal both cite.
    VIGOR_DO_INVERNO(
            "Seu Multiplicador de PV aumenta em 1. No início de cada combate você recebe RD e "
                    + "Resistência a Críticos por uma quantidade de Rodadas igual à metade de seu "
                    + "Multiplicador de PV.",
            FeatRequirements.builder()
                    .requiredRace(Anao.class)
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(5)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveLifeMultiplierIncrease(final Character character) {
            return 1;
        }
    };

    private static final int GLORIA_CRITICAL_MARGIN_INCREASE = 2;

    /**
     * The attack target's Categoria de Tamanho, or {@code null} when this roll opposes nobody.
     * On a Perícia de Ataque {@code SceneContext#getOpposedCharacter()} is the target — see that
     * field's javadoc for why one reference serves both directions.
     */
    private static SizeCategory opposedSize(final SceneContext sceneContext) {
        if (sceneContext == null || sceneContext.getOpposedCharacter() == null) {
            return null;
        }
        return sceneContext.getOpposedCharacter().getCharacter().getSizeCategory();
    }

    private static boolean targetIsNotSmallerThan(final SceneContext sceneContext, final Character character) {
        SizeCategory target = opposedSize(sceneContext);
        return target != null && target.getCategory() >= character.getSizeCategory().getCategory();
    }

    private static boolean targetIsLargerThan(final SceneContext sceneContext, final Character character) {
        SizeCategory target = opposedSize(sceneContext);
        return target != null && target.getCategory() > character.getSizeCategory().getCategory();
    }

    private final String description;
    private final FeatRequirements featRequirements;

    AnaoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ANAO;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return featRequirements;
    }
}
