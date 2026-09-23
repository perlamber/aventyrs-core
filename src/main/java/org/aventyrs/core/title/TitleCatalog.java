package org.aventyrs.core.title;

import org.aventyrs.core.title.giganteenfurecido.GiganteEnfurecido;
import org.aventyrs.core.title.santo.Santo;
import org.aventyrs.core.title.senhordabriga.SenhorDaBriga;

import java.util.List;

/**
 * Every authored Título Aventyr family, each as a freshly awakened instance — no Especialização
 * and no Habilidade chosen yet. The {@code FeatCatalog}/{@code ItemCatalog} equivalent for
 * Títulos, added for the first question that needs the list: which Título a player may pick for
 * {@code DestinoFeat#DESPERTAR_ANTECIPADO}.
 *
 * <p>An {@link AventyrTitle} is a per-character held instance, so this hands out new objects on
 * every call rather than shared constants. Two instances of one family are compared by class,
 * the same "which concrete class" identity {@link AventyrTitle}'s javadoc describes.
 *
 * <p><b>A new Título must be added here</b>, or no Talento will offer it.
 */
public final class TitleCatalog {

    private TitleCatalog() {
    }

    /** One fresh, empty instance per Título family. */
    public static List<AventyrTitle> all() {
        return List.of(new Santo(List.of(), List.of()), new SenhorDaBriga(List.of(), List.of()),
                new GiganteEnfurecido(List.of(), List.of()));
    }

    /** Whether title and other are the same Título family, whatever each one holds. */
    public static boolean isSameFamily(final AventyrTitle title, final AventyrTitle other) {
        return title.getClass() == other.getClass();
    }
}
