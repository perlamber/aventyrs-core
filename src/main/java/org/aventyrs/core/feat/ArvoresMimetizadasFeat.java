package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.MimetizedSpell;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.sheet.FormType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The acquired form of the Talentos that say "Escolha uma (duas) Árvore(s) de Magia, você pode
 * mimetizar as magias &lt;rung&gt; da árvore escolhida", carrying the chosen {@link MagicTree}s.
 * Grant <em>this</em> in place of the bare constant; each constant's {@code resolveRequiredChoices}
 * lists the Árvores it offers.
 *
 * <ul>
 *   <li>{@link ElficoFeat#ALMA_FEERICA} — one Árvore Natural: its Broto and Muda, and its Emergente
 *       with 2 Títulos Despertos, each "utilizam PD em substituição aos PM" (the rung's own Mana
 *       cost, paid in PD).</li>
 *   <li>{@link GorgonaFeat#ABENCOADA_PELO_CONCLAVE} — two Árvores Naturais: their Semente, their
 *       Broto at 2PD once a Título is Desperto, their Muda at 3PD once two are — and only in Forma
 *       Feérica.</li>
 *   <li>{@link MetamagicoFeat#APTIDAO_MAGICA_AMPLA} — two Árvores of any kind: their Semente and
 *       Broto, the Broto at 2PD "ao invés de 1PM".</li>
 * </ul>
 *
 * <p>The grant is <b>derived live</b> ({@code Character#getMimetizedSpells}), so a rung gated on a
 * Título appears the moment it is Desperto. A diverging Árvore hands over both ramificações at a
 * rung — "as magias Broto da árvore" names the rung, not a branch. A Semente is free: it costs 0 PM,
 * and every one of these pays its Mana in PD. ⚠️ Abençoada pelo Conclave and Aptidão Mágica Ampla
 * state no figure for their Sementes; the 0 follows from that same reading.
 */
@Getter
public final class ArvoresMimetizadasFeat extends AbstractFeat {

    private final Feat talento;
    private final Set<MagicTree> chosenTrees;

    public ArvoresMimetizadasFeat(@NonNull final Feat talento, @NonNull final Set<MagicTree> chosenTrees) {
        super(talento.getFeatCategory(), talento.getDescription(), talento.getFeatRequirements());
        this.talento = talento;
        this.chosenTrees = Set.copyOf(chosenTrees);
    }

    public static ArvoresMimetizadasFeat of(@NonNull final Feat talento, @NonNull final MagicTree... trees) {
        return new ArvoresMimetizadasFeat(talento, Set.of(trees));
    }

    /** The Árvores a character chose for talento, if they hold it in its acquired form. */
    public static Optional<Set<MagicTree>> chosenBy(final Character character, final Feat talento) {
        return character.getFeats().stream()
                .filter(ArvoresMimetizadasFeat.class::isInstance)
                .map(ArvoresMimetizadasFeat.class::cast)
                .filter(held -> held.getTalento() == talento)
                .map(ArvoresMimetizadasFeat::getChosenTrees)
                .findFirst();
    }

    /** "Árvore de Magia Natural": a Natural Árvore, or an Elemental one of the Natural element. */
    public static boolean isNatural(final MagicTree tree) {
        return tree.hasMagicType(MagicType.NATURAL)
                || tree.getElementalType().filter(element -> element == ElementalType.NATURAL).isPresent();
    }

    /** Every Natural Árvore — the options Alma Feérica and Abençoada pelo Conclave offer. */
    public static List<MagicTree> naturalTrees() {
        return Arrays.stream(MagicTree.values()).filter(ArvoresMimetizadasFeat::isNatural).toList();
    }

    @Override
    public Feat catalogEntry() {
        return talento;
    }

    /** Forwarded — Alma Feérica's "considerado um personagem Feérico para requisitos". */
    @Override
    public Set<CreatureType> getGrantedPrerequisiteCreatureTypes(final Character character) {
        return talento.getGrantedPrerequisiteCreatureTypes(character);
    }

    @Override
    public List<MimetizedSpell> getGrantedMimetizedSpells(final Character character) {
        int titles = character == null ? 0 : character.getAllTitles().size();
        List<MimetizedSpell> granted = new ArrayList<>();
        if (talento == ElficoFeat.ALMA_FEERICA) {
            addRung(granted, BranchLevel.BROTO, BranchLevel.BROTO.getManaCost(), null);
            addRung(granted, BranchLevel.MUDA, BranchLevel.MUDA.getManaCost(), null);
            if (titles >= 2) {
                addRung(granted, BranchLevel.EMERGENTE, BranchLevel.EMERGENTE.getManaCost(), null);
            }
        } else if (talento == GorgonaFeat.ABENCOADA_PELO_CONCLAVE) {
            addRung(granted, BranchLevel.SEMENTE, 0, FormType.FEERICA);
            if (titles >= 1) {
                addRung(granted, BranchLevel.BROTO, CONCLAVE_BROTO_COST, FormType.FEERICA);
            }
            if (titles >= 2) {
                addRung(granted, BranchLevel.MUDA, CONCLAVE_MUDA_COST, FormType.FEERICA);
            }
        } else if (talento == MetamagicoFeat.APTIDAO_MAGICA_AMPLA) {
            addRung(granted, BranchLevel.SEMENTE, 0, null);
            addRung(granted, BranchLevel.BROTO, AMPLA_BROTO_COST, null);
        }
        return granted;
    }

    private void addRung(final List<MimetizedSpell> granted, final BranchLevel rung, final int cost,
                         final FormType requiredForm) {
        for (MagicTree tree : chosenTrees) {
            for (Spell spell : tree.getSpells()) {
                if (spell.getBranchLevel() == rung) {
                    granted.add(MimetizedSpell.builder()
                            .spell(spell)
                            .determinationPointCost(cost)
                            .requiredForm(requiredForm)
                            .build());
                }
            }
        }
    }

    /** Abençoada pelo Conclave: "as magias Broto destas árvores ao custo de 2PD". */
    private static final int CONCLAVE_BROTO_COST = 2;

    /** Abençoada pelo Conclave: "as magias do Tipo Muda (ao custo de 3PD)". */
    private static final int CONCLAVE_MUDA_COST = 3;

    /** Aptidão Mágica Ampla: "Magias Broto conjuradas com este talento utilizam 2PD, ao invés de 1PM". */
    private static final int AMPLA_BROTO_COST = 2;
}
