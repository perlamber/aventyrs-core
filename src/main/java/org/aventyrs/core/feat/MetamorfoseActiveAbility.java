package org.aventyrs.core.feat;

import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.TemporaryEffect;

import java.util.List;

/**
 * One chosen {@link FormaMetamorfica}, as something the holder can turn into — the metamorphosis
 * state {@code VampiricoFeat#METAMORFOSE_DRACULEA} grants. <b>One instance per chosen Forma</b>,
 * so a Vampiro who picked Lobo and Morcego has two separately activatable shapes rather than one
 * ability that asks which. That is what {@code Feat#resolveActiveAbilities} (the plural hook) was
 * added for, and it is also forced: {@code ActiveAbilityService#activate} identifies a held
 * ability by {@code ==}, so "which shape" has to be part of the ability's identity.
 *
 * <p><b>It is a Poder Vampírico</b>, and takes that family's terms rather than inventing its own —
 * {@code Vampiro}'s Sangue, Poder e Dependência: "Ativar Poderes Vampíricos requer uma Ação Livre,
 * duram por 2 Rodadas e consomem 3PV cada". The costs and Duração are shared with {@link
 * PoderVampiricoActiveAbility} through its constants rather than restated.
 *
 * <p><b>What activating it does:</b> enters the Forma, and expires on its own via the {@code
 * FormEffect} {@code ActiveAbilityService} applies. Everything the shape is <em>worth</em> —
 * suppressed weapons ({@code FormType#getEquipmentPolicy()}), the row's Arma Natural replacing the
 * holder's own, and the three live Habilidades — is resolved <b>from the worn Forma</b> by {@link
 * MetamorfoseDraculeaFeat}'s hooks, never granted here as a bonus alongside it.
 *
 * <p>That split is deliberate and is the convention for anything lasting "while transformed": a
 * bonus scheduled beside the Forma would count down on its own clock and could expire while its
 * holder was still in the shape. Nothing is written on transforming, so nothing has to be restored
 * when the Duração lapses, when {@code enterForm(null)} is called, or when a second Forma displaces
 * this one — all three exits are the same no-op.
 */
@Getter
final class MetamorfoseActiveAbility implements ActiveAbility {

    private final FormaMetamorfica forma;

    MetamorfoseActiveAbility(final FormaMetamorfica forma) {
        this.forma = forma;
    }

    @Override
    public String getDescription() {
        return forma.name() + " — " + forma.getAbilityDescription();
    }

    /** Ação Livre, like every Poder Vampírico. */
    @Override
    public int getActionPointCost() {
        return 0;
    }

    @Override
    public int getMagicPointCost() {
        return 0;
    }

    /** "Consomem 3PV cada" — the race clause's price for any Poder Vampírico. */
    @Override
    public int getHitPointCost() {
        return PoderVampiricoActiveAbility.HIT_POINT_COST;
    }

    @Override
    public int getDurationInRounds() {
        return PoderVampiricoActiveAbility.BASE_DURATION_IN_ROUNDS;
    }

    @Override
    public FormType resolveGrantedForm(final Character character) {
        return forma.getForm();
    }

    /**
     * No bonus of its own — the shape is the whole effect, and {@code ActiveAbilityService}
     * applies the {@code FormEffect} that ends it. Empty rather than the single-effect default,
     * which would need a {@code TemporaryBonus} this ability does not have.
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
