package org.aventyrs.core.feat;

import java.util.List;

/**
 * One choice a Talento makes its holder pick at acquisition — "Escolha uma perícia", "Escolha 2
 * Formas Metamórficas", "escolha duas armas entre…".
 *
 * <p><b>This exists so a client does not have to know which Talentos are special.</b> A UI
 * offering the catalog asks every constant {@link Feat#resolveRequiredChoices(org.aventyrs.core.character.Character)};
 * a non-empty answer means "this one cannot be taken plain". Before it, the only way to discover
 * a Talento's options was to know that a particular acquired-form class existed and read a static
 * off it — which is not discovery, and left twelve of the catalog's thirteen choice-carrying
 * Talentos silently grantable as inert constants.
 *
 * <p>{@code options} are the values themselves, already filtered for the holder where a Talento's
 * own rules narrow them (a Rakshasa is not offered Névoa), so a client never reimplements a
 * Talento's eligibility rules to present its choice correctly. They are also exactly what the
 * matching acquired-form factory accepts back.
 *
 * <p>{@code type} is the token a caller uses to route the answer — to render it, to serialize it,
 * or to cast back when handing the picks to the acquired form. It is carried explicitly rather
 * than inferred from {@code options}, which may legitimately be narrowed to a subtype or, for a
 * holder with no legal pick, be empty.
 *
 * @param type    the kind of value being chosen
 * @param picks   how many of {@code options} must be chosen — exactly, not at most
 * @param options every value this holder may legally pick
 * @param <T>     the chosen value's type
 */
public record FeatChoice<T>(Class<T> type, int picks, List<T> options) {

    public FeatChoice {
        options = List.copyOf(options);
    }

    /** The common case: pick exactly one of options. */
    public static <T> FeatChoice<T> ofOne(final Class<T> type, final List<T> options) {
        return new FeatChoice<>(type, 1, options);
    }
}
