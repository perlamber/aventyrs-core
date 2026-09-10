package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.skill.SkillTrait;

import java.util.List;
import java.util.Optional;

/**
 * The acquired, per-character form of a {@link BestialFeat} Herança, carrying the {@link
 * SkillTrait} the player chose — "Bestiais Bovídeos recebem uma Habilidade de Competência de
 * Atletismo", "Bestiais Reptilianos recebem uma Especialização adicional de 'Atletismo'". Grant
 * <em>this</em> in {@code Character#feats} in place of the bare enum constant, the same
 * catalog-vs-acquired split {@link ConselheiroDeGuerraYmirianoFeat} keeps for its Habilidade de
 * Força.
 *
 * <p><b>One class for the whole tree</b>, parameterised by which Herança it is, rather than six
 * near-identical ones: every Herança is built from the same four clauses (see {@link
 * BestialFeat}'s javadoc) and differs only in <i>which</i> Atributo, Arma Natural and Perícia it
 * names — all of which the constant itself already answers. This is the first choice-carrying
 * form to serve more than one constant; the earlier ones ({@link FocoEmPericiaFeat}, {@link
 * TerrenoPrediletoFeat}, …) each wrap exactly one because each is the only constant of its shape.
 *
 * <p><b>The delegation is explicit and deliberately short.</b> Replacing the constant in {@code
 * getFeats()} would otherwise drop the effects the constant itself overrides, so the two that
 * exist are forwarded by hand: {@link #resolveAttributeBonus} (every Herança's "+1 de bônus
 * racial") and {@link #getGrantedNaturalWeapons} (Bovídea/Canina/Felina's). {@code AbstractFeat}
 * deliberately does <em>not</em> forward everything to {@link #catalogEntry()} — a choice-carrying
 * form replaces its constant rather than decorating it, and a blanket forward would make it
 * impossible to <i>drop</i> a clause. Add a line here if a Herança ever grows a third hook.
 */
@Getter
public final class HerancaBestialFeat extends AbstractFeat {

    private final BestialFeat heranca;
    private final SkillTrait chosenTrait;

    public HerancaBestialFeat(@NonNull final BestialFeat heranca, @NonNull final SkillTrait chosenTrait) {
        super(heranca.getFeatCategory(), heranca.getDescription(), heranca.getFeatRequirements());
        this.heranca = heranca;
        this.chosenTrait = chosenTrait;
    }

    public static HerancaBestialFeat of(@NonNull final BestialFeat heranca,
                                        @NonNull final SkillTrait chosenTrait) {
        return new HerancaBestialFeat(heranca, chosenTrait);
    }

    /**
     * The trait a character chose for heranca, if they hold that Herança in its acquired form —
     * mirrors {@link FocoEmPericiaFeat#chosenBy}. Takes the Herança because a Bestial routinely
     * holds several at once, each with its own pick.
     */
    public static Optional<SkillTrait> chosenBy(final Character character, final BestialFeat heranca) {
        return character.getFeats().stream()
                .filter(HerancaBestialFeat.class::isInstance)
                .map(HerancaBestialFeat.class::cast)
                .filter(held -> held.getHeranca() == heranca)
                .map(HerancaBestialFeat::getChosenTrait)
                .findFirst();
    }

    @Override
    public Feat catalogEntry() {
        return heranca;
    }

    /** "Recebem uma Habilidade de Competência / Especialização de &lt;Perícia&gt;." */
    @Override
    public List<SkillTrait> getGrantedSkillTraits(final Character character) {
        return List.of(chosenTrait);
    }

    /** Forwarded — "+1 de bônus racial em &lt;Atributo&gt;", fixed per Herança. */
    @Override
    public int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
        return heranca.resolveAttributeBonus(domain, character);
    }

    /** Forwarded — the Arma Natural the Bovídea/Canina/Felina Heranças hand over. */
    @Override
    public List<NaturalWeapon> getGrantedNaturalWeapons(final Character character) {
        return heranca.getGrantedNaturalWeapons(character);
    }
}
