package org.aventyrs.core.feat;

import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.TemporaryEffect;

import java.util.List;

/**
 * The {@link ActiveAbility} behind a Talento that turns its holder into something else — {@code
 * DraconicoFeat#DRACONATO}'s bipedal dragon, {@code FeericoFeat#ANCIENTEFORME}'s living tree. One
 * class for both, because the two clauses are written to the same template: <b>3PA + 3PD</b>, a
 * <b>3 Rodada</b> Duração, and "não poderá ser reativado até que passe por um Descanso Longo".
 *
 * <p><b>It grants a shape and, for now, nothing else.</b> Activating it pays the cost, enters the
 * {@link FormType} and starts the countdown that ends it — all of which are real. What each
 * Talento's rules text <em>also</em> promises while transformed (Draconato's "+2 Categoria de
 * Tamanho, Força e Foco para cada Título", Ancienteforme's Defesas/PV/Carisma/Foco) is not
 * granted here, and deliberately: a round-scoped Atributo bonus and a {@code SizeCategory} shift
 * driven from the sheet are two mechanisms this core does not have (see CLAUDE.md's Forma row).
 * The Forma being real is what those deltas will hang off once they exist, and it already gates
 * every "enquanto em sua Forma X" clause.
 *
 * <p><b>Duração is resolved per holder</b>, not fixed: Ancienteforme's own text extends it for a
 * Nascido da Floresta. That is why {@link #getDurationInRounds()} is not a constant, even though
 * both Talentos currently start from the same 3.
 */
@Getter
final class FormaActiveAbility implements ActiveAbility {

    /** "requer 3PA + 3PD" — identical on both Talentos. */
    private static final int ACTION_POINT_COST = 3;
    private static final int DETERMINATION_POINT_COST = 3;

    /** "A Duração … é de 3 Rodadas" / "dura por apenas 3 Rodadas". */
    private static final int BASE_DURATION_IN_ROUNDS = 3;

    private final Feat talento;
    private final FormType form;

    FormaActiveAbility(final Feat talento, final FormType form) {
        this.talento = talento;
        this.form = form;
    }

    @Override
    public String getDescription() {
        return talento.getDescription();
    }

    @Override
    public int getActionPointCost() {
        return ACTION_POINT_COST;
    }

    /** No Pontos de Magia: both clauses price the transformation in Determinação. */
    @Override
    public int getMagicPointCost() {
        return 0;
    }

    @Override
    public int getDeterminationPointCost() {
        return DETERMINATION_POINT_COST;
    }

    @Override
    public int getDurationInRounds() {
        return BASE_DURATION_IN_ROUNDS;
    }

    /** "Não poderá ser reativado até que passe por um Descanso Longo." */
    @Override
    public RestType getReactivationRest() {
        return RestType.LONGO;
    }

    @Override
    public FormType resolveGrantedForm(final Character character) {
        return form;
    }

    /**
     * No {@link TemporaryEffect} of its own — the shape <em>is</em> the effect, and {@code
     * ActiveAbilityService#activate} applies the {@code FormEffect} that ends it. Returns empty
     * rather than the single-effect default, which would need a bonus this ability does not have.
     */
    @Override
    public List<TemporaryEffect> resolveEffects(final Character character) {
        return List.of();
    }

    @Override
    public TemporaryEffect resolveEffect(final Character character) {
        return null;
    }
}
