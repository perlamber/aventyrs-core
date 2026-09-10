package org.aventyrs.core.title;

/**
 * Which broad kind of Título Aventyr a {@link AventyrTitle} is — the axis a Talento gates on
 * when its Pré-requisito names not just "1 Título Aventyr Desperto" but a *kind* of one.
 *
 * <p>The four <i>Centelha Aventyr</i> Talentos are what make this a real, enumerable concept
 * rather than flavour text: each requires "Ter desperto ao menos 1 Título Aventyr
 * &lt;archetype&gt;", and each grants a different effect keyed to that same archetype —
 * Centelha Aventyr Bruta widens a Margem Crítica, Elemental mimics a Magia, Precisa reduces a
 * GD, Consagrada is the Abençoado equivalent. Roughly 15 Talentos across the catalog gate this
 * way; the rest name only a count, which is {@code
 * org.aventyrs.core.feat.FeatRequirements#requiredAwakenedTitles} instead.
 *
 * <p>This is deliberately <b>not</b> the same axis as "which Título family is this" — that
 * question is still answered by which concrete class implements {@link AventyrTitle} (see its
 * javadoc for why no identity enum exists). An archetype groups several families; {@code
 * org.aventyrs.core.title.santo.Santo} is one {@link #ABENCOADO} Título among however many
 * others eventually land.
 */
public enum TitleArchetype {

    /** Os brutamontes — martial Títulos, gating e.g. Centelha Aventyr Bruta. */
    BRUTO,

    /** Os conjuradores — arcane Títulos, gating e.g. Centelha Aventyr Elemental. */
    ARCANO,

    /** Os guerreiros divinos — holy Títulos, gating e.g. Centelha Aventyr Consagrada. Santo's. */
    ABENCOADO,

    /** Os peritos — skill Títulos, gating e.g. Centelha Aventyr Precisa. */
    ESPECIALISTA
}
