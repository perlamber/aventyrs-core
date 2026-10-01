package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.Deity;

import java.util.List;

/**
 * The acquired form of a Talento de Devoção whose pick is a Divindade (core 0.0.87) — {@link
 * DevotoFeat#SINCRETISMO_RELIGIOSO}'s "segunda Divindade" and {@link DevotoFeat#FALSA_DEVOCAO}'s falsely followed
 * one. Grant <em>this</em> in place of the bare constant (core refuses that, {@code FEAT_REQUIRES_CHOICE}).
 *
 * <p>Neither constant has an effect of its own to forward: what each changes is which Divindades its holder counts
 * as devoted to, and {@code Character#getGenuineDevotions()}/{@code #getFeignedDevotions()} read it off this class.
 */
@Getter
public final class ChosenDeityFeat extends AbstractFeat {

    private final DevotoFeat talento;
    private final Deity chosenDeity;

    public ChosenDeityFeat(@NonNull final DevotoFeat talento, @NonNull final Deity chosenDeity) {
        super(talento.getFeatCategory(), talento.getDescription(), talento.getFeatRequirements());
        this.talento = talento;
        this.chosenDeity = chosenDeity;
    }

    @Override
    public Feat catalogEntry() {
        return talento;
    }

    @Override
    public boolean isEspecialistaTagged() {
        return talento.isEspecialistaTagged();
    }

    /** The Divindades character chose through talento, in the order held. */
    public static List<Deity> chosenBy(final Character character, final DevotoFeat talento) {
        return character.getFeats().stream()
                .filter(ChosenDeityFeat.class::isInstance)
                .map(ChosenDeityFeat.class::cast)
                .filter(held -> held.getTalento() == talento)
                .map(ChosenDeityFeat::getChosenDeity)
                .toList();
    }
}
