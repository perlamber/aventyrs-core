package org.aventyrs.core.skill.artes;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * The acquirable, per-character form of {@link ArtesCompetencyAbility#APRIMORAR_COM_ARTE},
 * carrying the Perícia the player chose when acquiring it. Grant <em>this</em> in
 * {@code Character.skillCompetencyAbilities} instead of the bare enum constant (which stays
 * the catalog/rules-text entry): because {@code ModifierResolver} invokes {@code @Modifier}
 * methods on the source instance, storing the choice on the instance makes the ability's
 * modifiers choice-dependent without any change to the modifier-scanning services.
 */
@Getter
public class ArtesAprimorarComArteAbility implements SkillCompetencyAbility {

    /**
     * The rules text spells out "+1" for every branch, so this is real data — not
     * {@code DamageService.DEFAULT_DAMAGE_REDUCTION}.
     */
    public static final int BENEFIT_BONUS = 1;

    private final SkillType chosenSkill;

    public ArtesAprimorarComArteAbility(@NonNull final SkillType chosenSkill) {
        this.chosenSkill = chosenSkill;
    }

    @Override
    public SkillType getSkillType() {
        return ArtesCompetencyAbility.APRIMORAR_COM_ARTE.getSkillType();
    }

    @Override
    public String getDescription() {
        return ArtesCompetencyAbility.APRIMORAR_COM_ARTE.getDescription();
    }

    /**
     * The "Esquiva e Aparar - Redução de Danos Sofridos (RDS) +1" branch — picked up by
     * {@code DamageService.getTotalDamageReduction}'s normal competency scan. Unlike the
     * other two branches below, this one is unconditionally active once chosen (RD/RA apply
     * regardless of which Perícia a hit came from), so it fits the existing
     * {@code @Modifier}/{@code ModifierResolver} machinery directly.
     */
    @Modifier(ModifierType.DAMAGE_REDUCTION)
    int damageReduction() {
        return chosenSkill == SkillType.ESQUIVA_E_APARAR ? BENEFIT_BONUS : 0;
    }

    /**
     * The "Perícias de Ataque - Dano Base +1" branch: how much this ability adds to Dano
     * Base for an attack made <em>with</em> attackingSkillType. Unlike {@link #damageReduction()},
     * this branch only applies while attacking with the specific Perícia chosen — not
     * unconditionally — so it can't be a no-arg {@code @Modifier} method (those can't tell
     * which Perícia a given roll is for); it takes attackingSkillType explicitly instead.
     * Now genuinely consumed: {@code
     * org.aventyrs.core.character.services.DamageBaseService} reaches it through {@link
     * #resolveDamageBaseIncrease(SkillType, org.aventyrs.core.character.Character)}, the
     * interface hook this method is the implementation of. It survives as its own named method
     * because the branch is this ability's, not the interface's — the hook is where the rest of
     * the codebase asks the question, this is where the answer lives.
     */
    public int getBaseDamageBonus(final SkillType attackingSkillType) {
        return chosenSkill.isAttackSkill() && chosenSkill == attackingSkillType ? BENEFIT_BONUS : 0;
    }

    /**
     * {@link SkillCompetencyAbility#resolveDamageBaseIncrease}'s hook, delegating to {@link
     * #getBaseDamageBonus(SkillType)} above — this ability's chosen Perícia is what scopes it,
     * never {@link #getSkillType()} (which is always Artes), which is exactly why the service
     * doesn't pre-filter by that. The holder's own state isn't consulted: the choice was frozen
     * at acquisition, on this instance.
     */
    @Override
    public int resolveDamageBaseIncrease(final SkillType attackingSkillType, final Character character) {
        return getBaseDamageBonus(attackingSkillType);
    }

    /**
     * The "Outras Perícias – Margem Crítica Menor +1" branch: how much this ability reduces
     * rolledSkillType's critical margin by — only when the chosen Perícia is neither a
     * Perícia de Ataque nor Esquiva e Aparar (those have their own branches above) and
     * matches rolledSkillType. Same context-parameter reasoning as
     * {@link #getBaseDamageBonus}: this is scoped to one specific, dynamically-chosen
     * Perícia, so it can't be a no-arg {@code @Modifier} method either. This is a narrower
     * critical-margin concept than {@code DominioDoManaCompetencyAbility.LETALIDADE_ARCANA}
     * needs (that one is scoped to a Magia, which still doesn't exist as an entity) — no
     * roll-resolution engine exists to consult this automatically, so a future one must call
     * it directly, passing whichever Perícia was rolled.
     */
    public int getCriticalMarginReduction(final SkillType rolledSkillType) {
        boolean isOtherPericia = chosenSkill != SkillType.ESQUIVA_E_APARAR && !chosenSkill.isAttackSkill();
        return isOtherPericia && chosenSkill == rolledSkillType ? BENEFIT_BONUS : 0;
    }

    /**
     * {@link SkillCompetencyAbility#resolveCriticalMarginIncrease}'s own hook, now that a real
     * roll-resolution consumer exists ({@code AbstractSkillInteraction} sums it into {@code
     * SkillRoll#getCriticalResult(int)}'s margin parameter) — just delegates to {@link
     * #getCriticalMarginReduction}, which already carries the real branch logic; sceneContext is
     * unused since this branch isn't conditioned on Scene facts, unlike {@code
     * org.aventyrs.core.ego.SorteAdvantage#ACE}/{@code
     * org.aventyrs.core.ability.DexterityAbility#LETALIDADE_PROGRESSIVA}'s own overrides.
     */
    @Override
    public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext) {
        return getCriticalMarginReduction(skillType);
    }
}
