package org.aventyrs.core.race;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * Habilidades Raciais granted to every Guampo — see {@link Race#getRacialAbilities()} for why
 * these are modeled as ordinary {@link SkillCompetencyAbility} instances rather than a separate
 * type.
 */
@Getter
@AllArgsConstructor
public enum GuamposRacialAbility implements SkillCompetencyAbility {

    /**
     * The RD half of Vigor de Epona — "reduzem em -1 todo dano sofrido", unconditional, so a flat
     * {@code @Modifier} is exactly the right shape (no {@code SceneContext}, no attacker, no
     * damage type to branch on). Deliberately <b>not</b> {@link
     * ModifierType#ABSOLUTE_DAMAGE_REDUCTION}: the clause names no attack it cannot be waived
     * against, and RD is what {@code DamageService}'s {@code ignoreDamageReduction} flag exists
     * to bypass, which is the ordinary reading of a plain "reduz o dano sofrido".
     *
     * <p>The value is a literal 1 rather than {@code DamageService.DEFAULT_DAMAGE_REDUCTION}
     * (+2): that constant is for a clause whose rules text states no number, and this one states
     * its own.
     *
     * <p>This is the <b>first racial ability in the codebase to grant RD</b>, and making it
     * arrive required {@code DamageServiceImpl.sumAcrossSources} to scan {@code
     * SkillCompetencyAbility.allFor(character)} rather than {@code
     * character.getSkillCompetencyAbilities()} alone — see that method's own comment. The +1
     * Multiplicador de PV half of the same trait is not here: it is seeded on the builder by
     * {@link Guampo#generateEmptyCharacter}, since a multiplier is a {@code Character} field
     * rather than a modifier scan.
     */
    VIGOR_DE_EPONA("Guampos recebem +1 Multiplicador de PV e reduzem em -1 todo dano sofrido.") {
        @Modifier(ModifierType.DAMAGE_REDUCTION)
        public int damageReduction() {
            return DAMAGE_REDUCTION;
        }
    };

    private static final int DAMAGE_REDUCTION = 1;

    private final String description;

    /**
     * A representative value only — RD is not scoped to any Perícia, and {@code
     * ModifierResolver}'s {@code DAMAGE_REDUCTION} scan applies no per-{@code SkillType} filter.
     * Same "the constant has to report something" situation as {@code
     * AnoesRacialAbility#ABATEDORES_DE_GIGANTES}'s own enum-level default.
     */
    @Override
    public SkillType getSkillType() {
        return SkillType.CONHECIMENTOS;
    }
}
