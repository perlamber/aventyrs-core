package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Anao;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.TitleArchetype;

import java.util.List;

/**
 * Talentos Anões — the Ymirian half of the Anão's own tree.
 *
 * <p><b>All five carry real effects.</b> {@link #FILHO_DE_YMIR} goes through {@code
 * Feat}'s PV-multiplier hook and the weapon-aware {@code resolveDamageBaseIncrease} (its "de
 * armas" scope excludes only a bare-handed Ataque Desarmado); {@link #VIGOR_DO_INVERNO} through
 * the multiplier hook plus a {@code Feat#resolveCombatStartBlessings} pair (RD + Resistência a
 * Críticos, lasting ½ Multiplicador de PV Rodadas); {@link #VANTAGEM_DE_TAMANHO} and
 * {@link #GLORIA_YMIRIANA} are conditioned on <i>who is on the other side of the roll</i>, and
 * became expressible when {@code SceneContext#getOpposedCharacter()} landed — the target on an
 * attack roll, the attacker on a defence roll. Between them they are the reason two new {@code
 * Feat} hooks exist: a {@code SceneContext}-aware {@code resolveDefenseBonus} and {@code
 * resolveCriticalMarginIncrease}.
 *
 * <p>{@link #CONSELHEIRO_DE_GUERRA_YMIRIANO} is real through {@link
 * ConselheiroDeGuerraYmirianoFeat} — a +1 Gnose {@code resolveAttributeBonus} grant (reaching
 * every Atributo-total reader via {@code Character#getEffectiveAttributeTotal}) plus a free
 * Habilidade de Força via {@code Feat#getGrantedAttributeAbilities}, the first Talento to grant
 * a Habilidade de Atributo outside the {@code AttributeAbilityService} slot economy.
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
            return character.getEffectiveAttributeTotal(AttributeDomain.VIGOR) / 2;
        }
    },

    /**
     * "Seu Multiplicador de PV e Dano Base de armas aumentam em +1." Both halves real.
     *
     * <p>Note the Pré-requisito is a bare "Vigor 4" — no race clause — so despite the Anão tag
     * this Talento is open to anyone who reaches it, exactly as printed.
     */
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

        /** "de armas" — any wielded {@link Weapon} (an Arma Natural is still an arma), never a
         * bare-handed Ataque Desarmado, which {@code DamageBaseService} passes here as a {@code
         * null} weapon. */
        @Override
        public int resolveDamageBaseIncrease(final Character character, final Weapon weapon) {
            return weapon == null ? 0 : 1;
        }
    },

    /**
     * "Você adquire Bônus Racial de +1 em Gnose e 1 Habilidade de Força (que você cumpra os
     * requisitos)."
     *
     * <p><b>Real</b>, through {@link ConselheiroDeGuerraYmirianoFeat} — grant that
     * choice-carrying instance (with the picked Habilidade de Força) in place of this bare
     * constant, which stays the catalog / rules-text entry. The Gnose bonus is an ordinary
     * {@link Feat#resolveAttributeBonus} grant, reaching every Atributo-total reader now that
     * they route through {@code Character#getEffectiveAttributeTotal}; the free Habilidade de
     * Força rides {@link Feat#getGrantedAttributeAbilities} into {@code
     * Character#getAttributeAbilities()} without spending an {@code AttributeAbilityService}
     * slot.
     */
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
     * de PV." <b>All three halves real.</b>
     *
     * <p>The PV multiplier rides {@link Feat#resolveLifeMultiplierIncrease}. The combat-start
     * grant rides {@link Feat#resolveCombatStartBlessings} — two {@link Blessing}s, both {@code
     * TargetScope#SELF}, both lasting {@code getLifeMultiplier / 2} Rodadas (floored, this
     * Talento's own +1 included), applied by {@code
     * org.aventyrs.core.character.services.CombatStartBlessingService} when the caller turns the
     * Cena into a Cena de Combate:
     * <ul>
     *   <li>RD, at {@link DamageService#DEFAULT_DAMAGE_REDUCTION} (the "recebe RD" with no number
     *   convention — and exactly one RC/RD instance's -2 per {@code
     *   docs/rules/defesas-e-resistencias.txt}), now summed for real by {@code
     *   DamageServiceImpl}'s {@code CombatantSheet} overload;</li>
     *   <li>Resistência a Críticos, one instance ({@link ModifierType#CRITICAL_RESISTANCE} {@code
     *   2}), subtracted from an attacker's Margem Crítica Menor widening by {@code
     *   AbstractSkillInteraction} — see that {@code ModifierType}'s javadoc for the pieces of the
     *   RC rule that still can't be expressed (the Maior clause, PRIMORDIAL scoping).</li>
     * </ul>
     */
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

        @Override
        public List<Blessing> resolveCombatStartBlessings(final Character character) {
            int rounds = new HitPointsServiceImpl().getLifeMultiplier(character) / 2;
            return List.of(
                    new Blessing(ModifierType.DAMAGE_REDUCTION, DamageService.DEFAULT_DAMAGE_REDUCTION,
                            rounds, TargetScope.SELF, name()),
                    new Blessing(ModifierType.CRITICAL_RESISTANCE, CRITICAL_RESISTANCE_INSTANCE,
                            rounds, TargetScope.SELF, name()));
        }
    };

    private static final int GLORIA_CRITICAL_MARGIN_INCREASE = 2;

    /** One instance of Resistência à Críticos — a -2 to an attacker's Margem Crítica Menor, per
     * {@code docs/rules/defesas-e-resistencias.txt}. */
    private static final int CRITICAL_RESISTANCE_INSTANCE = 2;

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
