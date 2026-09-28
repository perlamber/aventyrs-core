package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;

import java.util.Optional;

/**
 * The acquired, per-character form of the constants that open with "Receba +1 de Bônus Racial em
 * um Atributo, a sua escolha" — {@link MonstruosoFeat#ALFA} (one of the Atributos the holder's
 * Raça grants, or Força), {@link MonstruosoFeat#DUAS_CABECAS} (Gnose or Instinto) and {@link
 * HumanoFeat#LIMIAR_DA_EVOLUCAO} (any Atributo). Grant <em>this</em> in {@code Character#feats} in
 * place of the bare constant; each constant's {@code resolveRequiredChoices} lists the Atributos
 * it offers.
 *
 * <p>Only the Atributo clause lives here. Each constant's other clause (a Vantagem or Desvantagem
 * scoped to who is rolled against, or Limiar's Habilidade on a second Despertar) is unbuilt, so
 * there is nothing to forward. A choice-carrying form replaces its constant rather than
 * decorating it, so when one of those becomes real it lands here too, keyed on {@link
 * #getTalento()}.
 *
 * <p>{@link MonstruosoFeat#CERBERO} reads {@link MonstruosoFeat#DUAS_CABECAS}' pick through
 * {@link #chosenBy} to grant "o Atributo faltante".
 */
@Getter
public final class AtributoRacialEscolhidoFeat extends AbstractFeat {

    /** The "+1 de Bônus Racial" every one of these constants grants. */
    private static final int RACIAL_BONUS = 1;

    private final Feat talento;
    private final AttributeDomain chosenAttribute;

    public AtributoRacialEscolhidoFeat(@NonNull final Feat talento,
                                       @NonNull final AttributeDomain chosenAttribute) {
        super(talento.getFeatCategory(), talento.getDescription(), talento.getFeatRequirements());
        this.talento = talento;
        this.chosenAttribute = chosenAttribute;
    }

    public static AtributoRacialEscolhidoFeat of(@NonNull final Feat talento,
                                                 @NonNull final AttributeDomain chosenAttribute) {
        return new AtributoRacialEscolhidoFeat(talento, chosenAttribute);
    }

    /** The Atributo a character chose for talento, if they hold it in its acquired form. */
    public static Optional<AttributeDomain> chosenBy(final Character character, final Feat talento) {
        return character.getFeats().stream()
                .filter(AtributoRacialEscolhidoFeat.class::isInstance)
                .map(AtributoRacialEscolhidoFeat.class::cast)
                .filter(held -> held.getTalento() == talento)
                .map(AtributoRacialEscolhidoFeat::getChosenAttribute)
                .findFirst();
    }

    @Override
    public Feat catalogEntry() {
        return talento;
    }

    @Override
    public int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
        return domain == chosenAttribute ? RACIAL_BONUS : 0;
    }
}
