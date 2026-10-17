package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.ego.EgoAdvantage;
import org.aventyrs.core.ego.InitiativeAdvantage;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.CHOSEN_EGO_ADVANTAGE_NOT_AVAILABLE;

/**
 * The acquired, per-character form of {@link MobilidadeFeat#INICIATIVA_APRIMORADA} — "Você adquire 1 ponto permanente
 * de Iniciativa e então, se sua Iniciativa se tornar 4 ou mais, você adquire uma Vantagem de Iniciativa." Grant
 * <em>this</em> in {@code Character#feats} in place of the bare constant (core 0.1.7.21).
 *
 * <p>Table ruling (2026-10-17): the Vantagem is <b>picked when the Talento is acquired</b>, the {@link
 * VantagemDeEgoEscolhidaFeat} shape, and granted through {@link Feat#getGrantedEgoAdvantages} only while the
 * Iniciativa total ({@code Character#getEffectiveEgoTotal}, this Talento's own point included) is {@value
 * #MINIMUM_INITIATIVE} or more. Below that it is recorded and grants nothing.
 *
 * <p>{@link AbstractFeat} forwards nothing to its catalog entry, so the permanent point is restated here.
 */
@Getter
public final class VantagemDeIniciativaAprimoradaFeat extends AbstractFeat {

    /** "se sua Iniciativa se tornar 4 ou mais". */
    public static final int MINIMUM_INITIATIVE = 4;

    private final EgoAdvantage chosenAdvantage;

    public VantagemDeIniciativaAprimoradaFeat(@NonNull final EgoAdvantage chosenAdvantage) {
        super(MobilidadeFeat.INICIATIVA_APRIMORADA.getFeatCategory(),
                MobilidadeFeat.INICIATIVA_APRIMORADA.getDescription(),
                MobilidadeFeat.INICIATIVA_APRIMORADA.getFeatRequirements());
        this.chosenAdvantage = chosenAdvantage;
    }

    /** The validated factory — refuses anything but a Vantagem de Iniciativa holder does not already hold. */
    public static VantagemDeIniciativaAprimoradaFeat of(@NonNull final Character holder,
                                                        @NonNull final EgoAdvantage chosenAdvantage) {
        if (!optionsFor(holder).contains(chosenAdvantage)) {
            throw new IllegalOperationException(CHOSEN_EGO_ADVANTAGE_NOT_AVAILABLE);
        }
        return new VantagemDeIniciativaAprimoradaFeat(chosenAdvantage);
    }

    /** Every Vantagem de Iniciativa holder may pick: each one they do not already hold. */
    public static List<EgoAdvantage> optionsFor(@NonNull final Character holder) {
        List<EgoAdvantage> held = holder.getAllEgoAdvantages();
        return Arrays.stream(InitiativeAdvantage.values())
                .map(EgoAdvantage.class::cast)
                .filter(advantage -> !held.contains(advantage))
                .toList();
    }

    /** The Vantagem a character picked for Iniciativa Aprimorada, if they hold it in its acquired form. */
    public static Optional<EgoAdvantage> chosenBy(final Character character) {
        return character.getFeats().stream()
                .filter(VantagemDeIniciativaAprimoradaFeat.class::isInstance)
                .map(VantagemDeIniciativaAprimoradaFeat.class::cast)
                .map(VantagemDeIniciativaAprimoradaFeat::getChosenAdvantage)
                .findFirst();
    }

    @Override
    public Feat catalogEntry() {
        return MobilidadeFeat.INICIATIVA_APRIMORADA;
    }

    /** "Você adquire 1 ponto permanente de Iniciativa". */
    @Override
    public int resolveEgoBonus(final EgoDomain domain, final Character character) {
        return MobilidadeFeat.INICIATIVA_APRIMORADA.resolveEgoBonus(domain, character);
    }

    /** "… se sua Iniciativa se tornar 4 ou mais, você adquire uma Vantagem de Iniciativa." */
    @Override
    public List<EgoAdvantage> getGrantedEgoAdvantages(final Character character) {
        return character.getEffectiveEgoTotal(EgoDomain.INICIATIVA) >= MINIMUM_INITIATIVE
                ? List.of(chosenAdvantage) : List.of();
    }
}
