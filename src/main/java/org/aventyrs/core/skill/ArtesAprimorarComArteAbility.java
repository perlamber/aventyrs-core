package org.aventyrs.core.skill;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;

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
     * {@code DamageService.getTotalDamageReduction}'s normal competency scan.
     */
    @Modifier(ModifierType.DAMAGE_REDUCTION)
    int damageReduction() {
        return chosenSkill == SkillType.ESQUIVA_E_APARAR ? BENEFIT_BONUS : 0;
    }

    // TODO: "Perícias de Ataque - Dano Base +1" branch (when chosenSkill.isAttackSkill()):
    // needs a dealt-damage/Dano Base computation — DamageService only models damage
    // *received* (RD/RA/half-damage/shields), and no weapon/attack-damage entity exists to
    // add a base-damage bonus to. When one does, the bonus must apply only to attacks made
    // *with* the chosen Perícia, which this instance can already answer via getChosenSkill().

    // TODO: "Outras Perícias – Margem Crítica Menor +1" branch (any other chosenSkill):
    // needs a critical-margin system (same gap as
    // DominioDoManaCompetencyAbility.LETALIDADE_ARCANA).
}
