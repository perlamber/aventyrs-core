package org.aventyrs.core.feat;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Which Talento tree a {@link Feat} belongs to — the tag its rules-text header carries in
 * parentheses, and the enum class that holds every Talento of that tree (one enum per category,
 * mirroring {@code <Skill>CompetencyAbility}'s one-enum-per-domain shape).
 *
 * <p><b>Single-valued on purpose.</b> A Talento's header often carries several tags — {@code
 * Violência Descomunal (Aventyr/Bruto/Assassino)} — but most of those are not categories at all:
 *
 * <ul>
 *   <li>{@code Aventyr} is a <i>tier</i>, held as {@code
 *       FeatRequirements#requiredAwakenedTitles};</li>
 *   <li>{@code Bruto}/{@code Arcano}/{@code Abençoado}/{@code Especialista} are Título
 *       archetypes, held as {@code FeatRequirements#requiredTitleArchetype} (see {@code
 *       org.aventyrs.core.title.TitleArchetype});</li>
 *   <li>{@code Tellus} is the setting, carried by the regional Talentos only;</li>
 *   <li>{@code Geral} marks the general division, which {@link Type} already records.</li>
 * </ul>
 *
 * <p>Once those are stripped, roughly a dozen Talentos carry two genuine categories, and most of
 * those pair one general category with one race ({@code Anão/Sobrevivência}) — modeled as the
 * general category plus {@code FeatRequirements#requiredRace}, matching their own "Apenas
 * personagens da Raça X" prerequisite text. The handful that remain keep their primary tag, with
 * the secondary noted in the Talento's description. That is well under the bar for widening this
 * to a set.
 *
 * <p>{@link Type} is what a caller groups by when it needs the coarse division rather than the
 * tree — and is also what {@code org.aventyrs.core.race.Race#getNewFeatCost} keys its per-race
 * discounts off.
 *
 * <p>Counts below are how many Talentos the V19 catalog authors in that tree (see {@code
 * docs/rules/talentos-index.md}); a tree with no enum class yet has none written.
 */
@Getter
@AllArgsConstructor
public enum FeatCategory {

    // ---- Talentos Gerais --------------------------------------------------------------------

    /**
     * Aventyr — 19. The Talentos whose header carries <i>only</i> the {@code Aventyr} tier tag,
     * with no tree of their own (Furta-Talantes, Artesão da Vontade, the four <i>Centelha
     * Aventyr</i>…). {@code Aventyr} is therefore both a tier — every Talento here also sets
     * {@code FeatRequirements#requiredAwakenedTitles} — and the tree of last resort for those
     * that have no other. A Talento tagged {@code Aventyr} <b>and</b> a real tree belongs to
     * that tree, not here.
     */
    AVENTYR(Type.GERAL),
    /** Arte Marcial — 8. */
    ARTE_MARCIAL(Type.GERAL),
    /** Artífice — 3. */
    ARTIFICE(Type.GERAL),
    /** Artilharia — 10. */
    ARTILHARIA(Type.GERAL),
    /** Assassino — 16. */
    ASSASSINO(Type.GERAL),
    /** Cavalaria — 5. */
    CAVALARIA(Type.GERAL),
    /** Destino — 17. */
    DESTINO(Type.GERAL),
    /** Duelista — 19. */
    DUELISTA(Type.GERAL),
    /** Escudeiro — 13. */
    ESCUDEIRO(Type.GERAL),
    /** Metamágico — 15. */
    METAMAGICO(Type.GERAL),
    /** Mobilidade — 14. */
    MOBILIDADE(Type.GERAL),
    /** Perito — 18. */
    PERITO(Type.GERAL),
    /** Regalia — 5. */
    REGALIA(Type.GERAL),
    /** Sobrevivência — 19. */
    SOBREVIVENCIA(Type.GERAL),

    // ---- Talentos Raciais -------------------------------------------------------------------

    /** Anão — 5. */
    ANAO(Type.RACIAL),
    /** Aviano — 3. */
    AVIANO(Type.RACIAL),
    /** Bestial — 9. */
    BESTIAL(Type.RACIAL),
    /** Dracônico — 5. */
    DRACONICO(Type.RACIAL),
    /** Elemental — 9. */
    ELEMENTAL(Type.RACIAL),
    /** Élfico — 7. */
    ELFICO(Type.RACIAL),
    /** Fadas — 1. */
    FADAS(Type.RACIAL),
    /** Feérico — 13. */
    FEERICO(Type.RACIAL),
    /** Feral — 6. */
    FERAL(Type.RACIAL),
    /** Fúrias — 1. */
    FURIAS(Type.RACIAL),
    /** Gigante — 4. */
    GIGANTE(Type.RACIAL),
    /** Goblin — 2. */
    GOBLIN(Type.RACIAL),
    /** Gnomo — 4. */
    GNOMO(Type.RACIAL),
    /** Górgona — 7. */
    GORGONA(Type.RACIAL),
    /** Humano — 3. */
    HUMANO(Type.RACIAL),
    /** Indômito — 1. */
    INDOMITO(Type.RACIAL),
    /** Mestiço — 1. */
    MESTICO(Type.RACIAL),
    /** Monstruoso — 14. */
    MONSTRUOSO(Type.RACIAL),
    /** Ôgrico — 4. */
    OGRICO(Type.RACIAL),
    /** Órquico — 5. */
    ORQUICO(Type.RACIAL),
    /** Pequenino — 4. */
    PEQUENINO(Type.RACIAL),
    /** Troll — 5. */
    TROLL(Type.RACIAL),
    /** Vampírico — 11. */
    VAMPIRICO(Type.RACIAL);

    private final Type type;

    /**
     * The coarse division a Talento belongs to — the two headings the catalog itself is split
     * under. Talentos de Devoção are deliberately absent: their effect is split across
     * Adepto/Fiel/Fundamentalista devotion tiers this core has no concept of, so none is
     * authored (see {@code docs/rules/talentos-index.md}).
     */
    public enum Type {
        GERAL, RACIAL
    }
}
