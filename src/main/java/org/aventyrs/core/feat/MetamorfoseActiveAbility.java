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
 * <p><b>What activating it does today:</b> enters the Forma, which suppresses weapons through
 * {@code FormType#getEquipmentPolicy()} (defensive items keep working, per the clause) and expires
 * on its own via the {@code FormEffect} {@code ActiveAbilityService} applies. What it does
 * <em>not</em> do is grant the row's Arma Natural or its Habilidade — see {@link
 * FormaMetamorfica} for why each is blocked, and note the Arma Natural specifically is the Forma
 * delta CLAUDE.md records as needing sheet reach ({@code Character#getNaturalWeapons()} has no
 * sheet to ask which shape its owner is in).
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
