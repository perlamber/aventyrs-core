package org.aventyrs.core.feat;

import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.TemporaryEffect;

import java.util.ArrayList;
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

    /** What this Forma is worth per Título Aventyr Desperto — see {@link Uplift}. */
    private final Uplift uplift;

    FormaActiveAbility(final Feat talento, final FormType form, final Uplift uplift) {
        this.talento = talento;
        this.form = form;
        this.uplift = uplift;
    }

    /**
     * A Forma's per-Título figures, authored from its own rules text. The two Talentos disagree on
     * more than one number — Draconato raises Categoria de Tamanho, Força and Foco by +2 each,
     * while Ancienteforme raises Defesas and Categoria by +2 but Carisma and Foco by only +1 —
     * so one flat "per Título" value could not carry both.
     *
     * <p>Each figure is multiplied by the holder's Títulos Despertos at activation. A zero means
     * the Talento's text names nothing of that kind, not that it names zero.
     *
     * @param sizeCategory   per-Título shift to {@code ModifierType#SIZE_CATEGORY}
     * @param defesas        per-Título bonus to both Defesas, 0 when the text names none
     * @param attribute      per-Título bonus to each Atributo in attributes
     * @param attributes     which Atributos the Forma raises
     */
    record Uplift(int sizeCategory, int defesas, int attribute, List<AttributeDomain> attributes) {
        Uplift {
            attributes = List.copyOf(attributes);
        }
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
     * The uplift the Forma carries, as round-scoped {@link TemporaryBonus}es lasting exactly the
     * Duração — a {@link ModifierType#SIZE_CATEGORY} shift, a Defesas bonus where the text names
     * one, and one {@code <ATTR>_BONUS} per Atributo, each figure multiplied by the holder's
     * Títulos Despertos (see {@link Uplift}).
     *
     * <p>A character with no Título gets none of it, which is what "para cada Título Aventyr que
     * você possuir" says — and both Talentos already demand one Título to acquire, so the figure
     * is never zero in practice.
     *
     * <p><b>Partial reach, and deliberately so.</b> The Atributo bonuses land on a Perícia roll
     * governed by that Atributo and nowhere else — that is the whole of what {@code
     * ModifierType.<ATTR>_BONUS} reaches (see its javadoc): PV/PM/PD and Conjuração read
     * {@code Character#getEffectiveAttributeTotal}, which has no sheet and so cannot see a
     * round-scoped grant. A Forma lasting three Rodadas raising max PV would need those totals
     * recomputed per Rodada, which this core does not do. The {@code SIZE_CATEGORY} half has no
     * such limit: {@code CharacterSizeService} gained a sheet-taking overload for exactly this.
     *
     * <p>Returns the list; {@code resolveEffect} is the single-effect shim the interface keeps and
     * is not what {@code ActiveAbilityService} applies.
     */
    @Override
    public List<TemporaryEffect> resolveEffects(final Character character) {
        int titles = character.getAllTitles().size();
        List<TemporaryEffect> effects = new ArrayList<>();
        add(effects, ModifierType.SIZE_CATEGORY, uplift.sizeCategory() * titles);
        add(effects, ModifierType.DEFESAS, uplift.defesas() * titles);
        for (AttributeDomain domain : uplift.attributes()) {
            add(effects, domain.getBonusModifierType(), uplift.attribute() * titles);
        }
        return List.copyOf(effects);
    }

    /** Skips a figure of zero rather than holding a bonus worth nothing for the Duração. */
    private void add(final List<TemporaryEffect> effects, final ModifierType type, final int value) {
        if (value != 0) {
            effects.add(new TemporaryBonus(type, value, getDurationInRounds()));
        }
    }

    /** The single-effect shim — the size shift, which every Forma here carries. */
    @Override
    public TemporaryEffect resolveEffect(final Character character) {
        return resolveEffects(character).stream().findFirst().orElse(null);
    }
}
