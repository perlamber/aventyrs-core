package org.aventyrs.core.magic;

/**
 * picks free Magias of rung, each from a different Árvore de Magia, that a Talento hands its
 * holder — one step of the {@code MetamagicoFeat} ladder ("Escolha 2 Árvores de Magia [...], você
 * aprende a conjurar as magias do tipo Broto destas árvores"). Returned by {@code
 * Feat#resolveFreeSpellPicks}; how many are still owed is derived from {@code
 * Character#getSpells()} by {@code SpellService#getOwedFreeSpells}, never stored.
 */
public record FreeSpellPick(BranchLevel rung, int picks) {

    public FreeSpellPick {
        if (rung == null || picks < 1) {
            throw new IllegalArgumentException("A free pick needs a rung and at least one pick");
        }
    }
}
