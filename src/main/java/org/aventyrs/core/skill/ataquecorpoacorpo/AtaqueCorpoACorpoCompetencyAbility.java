package org.aventyrs.core.skill.ataquecorpoacorpo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The Habilidades de Competência available to characters trained in Ataque Corpo-a-Corpo.
 * Three are fully real — the unconditional Attribute substitutions of {@link #ACUIDADE}
 * (Destreza) and {@link #SAGACIDADE_ARCANA} (Foco), both via {@link SkillCompetencyAbility
 * #getSubstituteAttributeDomain()}, and all three tiers of {@link #BRUTALIDADE}, since {@link
 * DamageBase} landed. The rest still need a system this core doesn't have (critical margin
 * scoped to one Perícia, or Malefício/status-effect tracking); see each constant's TODO.
 */
@Getter
@AllArgsConstructor
public enum AtaqueCorpoACorpoCompetencyAbility implements SkillCompetencyAbility {

    // The Desvantagem-on-Damage-rolls-with-a-Categoria-Pesada-weapon half of this ability is
    // still TODO: this codebase has no damage-roll concept to apply Desvantagem to (same gap
    // as AtaqueADistanciaCompetencyAbility.FRIEZA) or a way to track a weapon's category on a
    // specific roll. The substitution half is real — see getSubstituteAttributeDomain() below.
    ACUIDADE("Você pode substituir o Atributo Base desta perícia por Destreza, se arma for " +
            "de Categoria Pesada você sofre Desvantagem nas rolagens de Danos.") {
        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.DEXTERITY);
        }
    },

    /**
     * All three tiers are real. Which one applies is read live off the holder's own Ataque
     * Corpo-a-Corpo Graduação every time it's asked — there is no "crossing a threshold"
     * trigger to fire and nothing to migrate, because neither half is ever stored: the flat
     * bonus is resolved per dano roll ({@link #resolveDamageBonus}) and the Dano Base increase
     * per attack ({@link #resolveDamageBaseIncrease}), so raising a Graduação changes the
     * answer on the next call by itself. Same recompute-on-demand discipline as {@code
     * HitPointsService#getStatus} and {@code InitiativeEntry#getEffectiveInitiativeValue}.
     *
     * <p>"Convertido" is exclusive, not additive: from {@value #BRUTALIDADE_CONVERSION_GRADUATION}
     * Graduações the flat dano bonus is <em>replaced</em> by the Dano Base increase, never held
     * alongside it — which is why {@link #resolveDamageBonus} returns empty from that point on.
     * The two are different mechanics and must not be summed (see {@link DamageBase}); a
     * scale-up is worth far more than the +1 it costs, which is the point of the conversion.
     */
    BRUTALIDADE("Você recebe Bônus de +1 em rolagens de Danos de Ataques Corpo-a-Corpo, com " +
            "5 Graduações este Bônus é convertido em Dano Base, com 10 Graduações o " +
            "aumento no Dano Base muda para +2.") {
        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext, final CombatantSheet attackTarget, final Character actor) {
            if (attackingSkillType != SkillType.ATAQUE_CORPO_A_CORPO || actor == null) {
                return Optional.empty();
            }
            return graduationOf(actor) < BRUTALIDADE_CONVERSION_GRADUATION
                    ? Optional.of(new DamageBonus(BRUTALIDADE_DAMAGE_BONUS, DamageType.FISICO))
                    : Optional.empty();
        }

        @Override
        public int resolveDamageBaseIncrease(final SkillType attackingSkillType, final Character character) {
            if (attackingSkillType != SkillType.ATAQUE_CORPO_A_CORPO || character == null) {
                return 0;
            }
            int graduation = graduationOf(character);
            if (graduation >= BRUTALIDADE_DOUBLED_GRADUATION) {
                return 2;
            }
            return graduation >= BRUTALIDADE_CONVERSION_GRADUATION ? 1 : 0;
        }
    },

    /**
     * Real, and the same unconditional shape as {@link #ACUIDADE} — the rules text names no
     * circumstance, so it always substitutes (see {@link SkillCompetencyAbility
     * #getSubstituteAttributeDomain()}). Nothing else in the clause needs a system this core
     * lacks, unlike ACUIDADE's Desvantagem half.
     *
     * <p>A character holding <em>both</em> this and ACUIDADE substitutes whichever comes first
     * in their own {@code skillCompetencyAbilities} list, since {@link SkillCompetencyAbility
     * #resolveAttributeDomain} takes the first match — the rules name no precedence between two
     * substitutions for the same Perícia, so none is invented here.
     */
    SAGACIDADE_ARCANA("Você pode substituir o Atributo Base desta perícia por Foco.") {
        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.FOCUS);
        }
    },

    // TODO: +1 to this Perícia's own Margem Crítica Menor, unconditionally — a
    // Perícia-scoped critical-margin concept now exists (see ArtesAprimorarComArteAbility
    // #getCriticalMarginReduction), but that one is parameterized by a dynamically-chosen
    // Perícia; this constant would need its own always-on equivalent (mirroring how
    // #damageReduction() is unconditional there), which isn't wired anywhere yet since
    // nothing calls either version in an actual roll.
    ATAQUE_PRECISO("A margem crítica menor de seus Ataques Corpo-a-Corpo é aumentada em +1 " +
            "número."),

    // TODO: a critical hit inflicts the Malefício Desprevenido on the target for 1 Rodada —
    // no Malefício/status-effect system, critical-hit-trigger detection, or Rodada-scoped
    // duration tracking exists yet.
    ABRIR_DEFESAS("Após um acerto crítico seu alvo recebe o Malefício Desprevenido por 1 " +
            "Rodada.");

    /** The Graduação at which BRUTALIDADE's flat dano bonus is converted into +1 Dano Base. */
    private static final int BRUTALIDADE_CONVERSION_GRADUATION = 5;

    /** The Graduação at which BRUTALIDADE's Dano Base increase becomes +2. */
    private static final int BRUTALIDADE_DOUBLED_GRADUATION = 10;

    /** BRUTALIDADE's flat dano-roll bonus, before it converts. */
    private static final int BRUTALIDADE_DAMAGE_BONUS = 1;

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }

    /**
     * character's own Ataque Corpo-a-Corpo Graduação — 0 when untrained, the same reading
     * {@code Feat#isEligible} already applies to an untrained Perícia.
     */
    private static int graduationOf(final Character character) {
        CharacterSkill characterSkill = character.getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO);
        return characterSkill == null ? 0 : characterSkill.getGraduation().getGraduationValue();
    }
}
