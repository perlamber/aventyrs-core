package org.aventyrs.core.ego;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The acquirable, per-character form of {@link ResourcesAdvantage#MORAL_HERDADA}, carrying the
 * Fama Positiva/Negativa choice the player made when acquiring it. Grant <em>this</em> in
 * {@code Character.egoAdvantages} instead of the bare enum constant (which stays the
 * catalog/rules-text entry) — the same instance-based pattern {@link
 * org.aventyrs.core.skill.artes.ArtesAprimorarComArteAbility} already establishes for a choice
 * that feeds an ability's own bonus methods.
 */
public class MoralHerdadaAbility implements EgoAdvantage {

    public enum FamaChoice {
        POSITIVA,
        NEGATIVA
    }

    /** The rules text spells out "+1", so this is real data, not a generic default. */
    static final int BASE_ROLL_BONUS = 1;

    /** "aumenta em +1 para cada 10 pontos da Fama escolhida" — floor division, real data. */
    static final int FAMA_POINTS_PER_BONUS_STEP = 10;

    @Getter
    private final FamaChoice famaChoice;

    public MoralHerdadaAbility(@NonNull final FamaChoice famaChoice) {
        this.famaChoice = famaChoice;
    }

    @Override
    public EgoDomain getEgoDomain() {
        return ResourcesAdvantage.MORAL_HERDADA.getEgoDomain();
    }

    @Override
    public String getDescription() {
        return ResourcesAdvantage.MORAL_HERDADA.getDescription();
    }

    /**
     * "+1 em rolagens de Artes e Persuasão, este bônus aumenta em +1 para cada 10 pontos da
     * Fama escolhida" — scoped to just those two skills (see {@link EgoAdvantage
     * #resolveSkillSpecificRollBonus}'s own javadoc for why {@link #resolveConditionalRollBonus}
     * can't express this), and scaling with whichever Fama total (Positiva or Negativa, per
     * {@link #famaChoice}) target is currently holding — read live off target every time this is
     * called, since Fama can keep growing after creation (Excelência bonuses, Narrador rewards),
     * and this Vantagem's own rules text describes the bonus as tracking "a Fama escolhida," not
     * a value frozen at acquisition.
     */
    @Override
    public Optional<Integer> resolveSkillSpecificRollBonus(final SkillType skillType, final SceneContext sceneContext, final CombatantSheet target) {
        if (skillType != SkillType.ARTES && skillType != SkillType.PERSUASAO) {
            return Optional.empty();
        }
        // Fama is player-only — it lives on CharacterSheet, not on the shared CombatantSheet, so a
        // combatant without one (a monster) contributes no Fama step. MORAL_HERDADA is a Vantagem
        // de Ego, which only a player character acquires, so in practice this branch is always
        // taken; the guard exists because the parameter's type can no longer promise it.
        if (!(target instanceof CharacterSheet characterSheet)) {
            return Optional.of(BASE_ROLL_BONUS);
        }
        int famaValue = famaChoice == FamaChoice.POSITIVA ? characterSheet.getFamaPositiva() : characterSheet.getFamaNegativa();
        return Optional.of(BASE_ROLL_BONUS + famaValue / FAMA_POINTS_PER_BONUS_STEP);
    }

    /**
     * "Você recebe Fama Positiva ou Negativa (à sua escolha) igual ao seu valor de Recursos" —
     * a one-time grant, not a per-roll computation like {@link #resolveSkillSpecificRollBonus}
     * above. amount is character's own Recursos {@link org.aventyrs.core.character.EgoValue
     * #getTotal()} — {@code EgoValue}'s own javadoc notes Recursos' base never changes outside
     * character creation in this model, and this grant only ever happens once, around creation
     * time, so base and total coincide in practice regardless of which is read here.
     *
     * <p><strong>Not called automatically by anything yet</strong>: {@code
     * CharacterCreationServiceImpl} only ever assembles a plain {@link Character} — Fama lives
     * on {@link CombatantSheet} (see {@link CombatantSheet#increaseFamaPositiva}/{@link
     * CombatantSheet#increaseFamaNegativa}), and no {@code CombatantSheet} exists yet at
     * Character-creation time for this to grant onto (the same Character-then-CombatantSheet
     * ordering gap {@code CharacterAttributeService#upgradeBase}/{@code
     * SkillGraduationService#upgradeGraduation} already work around by taking both explicitly).
     * A caller applies this once a {@code CombatantSheet} has actually been assembled, via
     * {@code CombatantSheet.of(...)}.
     * @return the total Fama (of whichever type {@link #famaChoice} names) characterSheet holds
     *         after this grant
     */
    public int applyStartingFama(final Character character, final CharacterSheet characterSheet) {
        int amount = character.getEgos().getRecursos().getTotal();
        return famaChoice == FamaChoice.POSITIVA
                ? characterSheet.increaseFamaPositiva(amount)
                : characterSheet.increaseFamaNegativa(amount);
    }
}
