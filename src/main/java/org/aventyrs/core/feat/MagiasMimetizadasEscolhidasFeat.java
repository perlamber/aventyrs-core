package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.MimetizedSpell;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.catalog.MagicTree;

import java.util.List;
import java.util.Set;

/**
 * The acquired form of the {@link MetamagicoFeat} Aptidão ladder rungs that say "Escolha uma magia
 * &lt;rung&gt; de cada Árvore de Magia conhecida através do talento 'Aptidão Mágica Ampla', você é
 * capaz de mimetizar as magias escolhidas ao custo de N PD", carrying the chosen {@link Spell}s:
 * {@link MetamagicoFeat#APTIDAO_MAGICA_ASSOMBROSA} (Muda, 3PD), {@link
 * MetamagicoFeat#APTIDAO_MAGICA_SUPREMA} (Emergente, 5PD) and {@link
 * MetamagicoFeat#APTIDAO_MAGICA_DRACONICA} (one Florescente, 5PD).
 *
 * <p>Each constant's {@code resolveRequiredChoices} offers the rung's Magias of the Árvores its
 * holder chose for Aptidão Mágica Ampla ({@link ArvoresMimetizadasFeat#chosenBy}). ⚠️ "Uma de cada
 * Árvore" and Suprema's "que você conheça uma Muda de seu ramo" are not validated on the picks —
 * the usual builders-aren't-gatekeepers restraint.
 */
@Getter
public final class MagiasMimetizadasEscolhidasFeat extends AbstractFeat {

    private final MetamagicoFeat talento;
    private final Set<Spell> chosenSpells;

    public MagiasMimetizadasEscolhidasFeat(@NonNull final MetamagicoFeat talento, @NonNull final Set<Spell> chosenSpells) {
        super(talento.getFeatCategory(), talento.getDescription(), talento.getFeatRequirements());
        this.talento = talento;
        this.chosenSpells = Set.copyOf(chosenSpells);
    }

    public static MagiasMimetizadasEscolhidasFeat of(@NonNull final MetamagicoFeat talento, @NonNull final Spell... spells) {
        return new MagiasMimetizadasEscolhidasFeat(talento, Set.of(spells));
    }

    /**
     * The Magias of rung in the Árvores holder chose for Aptidão Mágica Ampla — what a rung of the
     * ladder offers. Empty when the holder has no Ampla choice recorded.
     */
    public static List<Spell> optionsFor(final Character holder, final BranchLevel rung) {
        return ArvoresMimetizadasFeat.chosenBy(holder, MetamagicoFeat.APTIDAO_MAGICA_AMPLA).orElse(Set.of()).stream()
                .sorted()
                .flatMap((MagicTree tree) -> tree.getSpells().stream())
                .filter(spell -> spell.getBranchLevel() == rung)
                .toList();
    }

    @Override
    public Feat catalogEntry() {
        return talento;
    }

    @Override
    public List<MimetizedSpell> getGrantedMimetizedSpells(final Character character) {
        int cost = talento == MetamagicoFeat.APTIDAO_MAGICA_ASSOMBROSA ? ASSOMBROSA_COST : SUPREMA_AND_DRACONICA_COST;
        return chosenSpells.stream()
                .map(spell -> MimetizedSpell.builder().spell(spell).determinationPointCost(cost).build())
                .toList();
    }

    /** "ao custo de 3PD cada". */
    private static final int ASSOMBROSA_COST = 3;

    /** Suprema's and Dracônica's "ao custo de 5PD". */
    private static final int SUPREMA_AND_DRACONICA_COST = 5;
}
