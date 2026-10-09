package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.ego.AutocontroleAdvantage;
import org.aventyrs.core.ego.EgoAdvantage;
import org.aventyrs.core.ego.InitiativeAdvantage;
import org.aventyrs.core.ego.MoralHerdadaAbility;
import org.aventyrs.core.ego.ResourcesAdvantage;
import org.aventyrs.core.ego.SorteAdvantage;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.aventyrs.core.util.TranslatableMessages.CHOSEN_EGO_ADVANTAGE_NOT_AVAILABLE;

/**
 * The acquired, per-character form of {@link DestinoFeat#AUTOCONHECIMENTO} — "Escolha um Ego que você possua
 * valor 2 ou superior. Você adquire uma Vantagem do Ego escolhido." Grant <em>this</em> in {@code
 * Character#feats} in place of the bare constant.
 *
 * <p><b>The chosen Ego needs no field of its own</b> — an {@link EgoAdvantage} reports its own {@link
 * EgoAdvantage#getEgoDomain()}, so recording the Vantagem records the Ego with it, the same reasoning {@link
 * HabilidadeDeAtributoEscolhidaFeat} gives for its Atributo.
 *
 * <p>The Vantagem is granted through {@link Feat#getGrantedEgoAdvantages}, beside — never in place of — the one
 * chosen at creation for that Ego. Table rulings (2026-10-08): the Ego is read at its <b>total</b> value, so a
 * point a Talento or Título added counts toward the 2; and any Ego may be chosen, including one whose Vantagem
 * the character already holds, as long as the Vantagem itself is a different one — holding the same Vantagem
 * twice would grant nothing.
 */
@Getter
public final class VantagemDeEgoEscolhidaFeat extends AbstractFeat {

    /** "um Ego que você possua valor 2 ou superior". */
    public static final int MINIMUM_EGO_VALUE = 2;

    private final EgoAdvantage chosenAdvantage;

    public VantagemDeEgoEscolhidaFeat(@NonNull final EgoAdvantage chosenAdvantage) {
        super(DestinoFeat.AUTOCONHECIMENTO.getFeatCategory(), DestinoFeat.AUTOCONHECIMENTO.getDescription(),
                DestinoFeat.AUTOCONHECIMENTO.getFeatRequirements());
        this.chosenAdvantage = chosenAdvantage;
    }

    /**
     * The validated factory — use this at grant time. Refuses a Vantagem of an Ego below {@value
     * #MINIMUM_EGO_VALUE}, and one the holder already has.
     */
    public static VantagemDeEgoEscolhidaFeat of(@NonNull final Character holder,
                                                @NonNull final EgoAdvantage chosenAdvantage) {
        if (optionsFor(holder).stream().noneMatch(option -> sameVantagem(option, chosenAdvantage))) {
            throw new IllegalOperationException(CHOSEN_EGO_ADVANTAGE_NOT_AVAILABLE);
        }
        return new VantagemDeEgoEscolhidaFeat(chosenAdvantage);
    }

    /**
     * Every Vantagem holder may take: each Vantagem de Ego of an Ego at {@value #MINIMUM_EGO_VALUE} or more, bar
     * the ones already held. Moral Herdada is offered once per Fama, as the {@link MoralHerdadaAbility} it is
     * granted as.
     */
    public static List<EgoAdvantage> optionsFor(@NonNull final Character holder) {
        List<EgoAdvantage> catalog = new ArrayList<>();
        Stream.of(AutocontroleAdvantage.values(), InitiativeAdvantage.values(), SorteAdvantage.values(),
                        ResourcesAdvantage.values())
                .flatMap(Arrays::stream)
                .forEach(advantage -> {
                    if (advantage == ResourcesAdvantage.MORAL_HERDADA) {
                        Arrays.stream(MoralHerdadaAbility.FamaChoice.values())
                                .forEach(fama -> catalog.add(new MoralHerdadaAbility(fama)));
                    } else {
                        catalog.add(advantage);
                    }
                });
        List<EgoAdvantage> held = holder.getAllEgoAdvantages();
        return catalog.stream()
                .filter(advantage -> egoValueOf(holder, advantage.getEgoDomain()) >= MINIMUM_EGO_VALUE)
                .filter(advantage -> held.stream().noneMatch(owned -> sameVantagem(owned, advantage)))
                .toList();
    }

    /** The Vantagem a character chose for Autoconhecimento, if they hold it in its acquired form. */
    public static Optional<EgoAdvantage> chosenBy(final Character character) {
        return character.getFeats().stream()
                .filter(VantagemDeEgoEscolhidaFeat.class::isInstance)
                .map(VantagemDeEgoEscolhidaFeat.class::cast)
                .map(VantagemDeEgoEscolhidaFeat::getChosenAdvantage)
                .findFirst();
    }

    /** Whether two Vantagens are the same one — every Moral Herdada counts as one, whichever Fama it follows. */
    public static boolean sameVantagem(final EgoAdvantage one, final EgoAdvantage other) {
        return one == other || isMoralHerdada(one) && isMoralHerdada(other);
    }

    private static boolean isMoralHerdada(final EgoAdvantage advantage) {
        return advantage == ResourcesAdvantage.MORAL_HERDADA || advantage instanceof MoralHerdadaAbility;
    }

    private static int egoValueOf(final Character holder, final EgoDomain domain) {
        return holder.getEgos().getEgo(domain).getTotal();
    }

    @Override
    public Feat catalogEntry() {
        return DestinoFeat.AUTOCONHECIMENTO;
    }

    /** "Você adquire uma Vantagem do Ego escolhido." */
    @Override
    public List<EgoAdvantage> getGrantedEgoAdvantages(final Character character) {
        return List.of(chosenAdvantage);
    }
}
