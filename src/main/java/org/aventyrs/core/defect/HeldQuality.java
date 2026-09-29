package org.aventyrs.core.defect;

import lombok.NonNull;
import org.aventyrs.core.feat.QualidadeFeat;

import java.util.List;
import java.util.Optional;

/**
 * A Qualidade one character holds. What {@code Character#getQualities()} stores and a consumer persists.
 *
 * @param quality      the Qualidade
 * @param qualityClass Menor or Maior
 * @param choices      answers to {@link Quality#resolveChoices}, in order
 * @param source       how it was acquired
 */
public record HeldQuality(@NonNull Quality quality, @NonNull QualityClass qualityClass, List<Object> choices,
                          @NonNull QualitySource source) {

    public HeldQuality {
        choices = choices == null ? List.of() : List.copyOf(choices);
    }

    /** This Qualidade's effects — a Maior's and its Menor's — folded into {@code Character#getFeats()}. */
    public List<QualidadeFeat> effects() {
        return quality.effectsAt(qualityClass);
    }

    public <T> Optional<T> choice(final Class<T> type) {
        return choices.stream().filter(type::isInstance).map(type::cast).findFirst();
    }
}
