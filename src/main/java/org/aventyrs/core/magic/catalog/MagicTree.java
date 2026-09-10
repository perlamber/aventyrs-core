package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellBranch;
import org.aventyrs.core.magic.SpellTree;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Every Árvore de Magia of {@code docs/rules/magias.txt}'s complete section — the twenty trees of
 * the "Lista de Arcanismos e Preces Divinas", holding 145 Magias between them.
 *
 * <p>Each constant carries exactly what the tree's ALL-CAPS heading states and nothing else: its
 * name, the one or two {@link MagicType}s of its parenthesised category tag, and the {@link
 * ElementalType} qualifying that tag when it is Elemental. The heading is the <b>only</b> place
 * the document states a category — it is a property of the tree, never of an individual Magia,
 * which is why {@code Spell#getPrimaryType()} delegates here rather than being authored 145
 * times.
 *
 * <h2>A catalog enum under an interface, which is not the contradiction it looks like</h2>
 *
 * {@link SpellTree} is an interface so that trees can be authored per family and so a consumer
 * can add their own; this enum is the twenty <em>this ruleset</em> ships. The interface's own
 * javadoc rejected a central enum on the grounds that it "would have to sit empty until the first
 * one lands" — that is no longer true, since the source document supplies all twenty at once, and
 * an enum is what gives a consumer a stable {@code name()} to persist a Conjurador's Árvores
 * against.
 *
 * <h2>The seven Umbral trees are deliberately absent</h2>
 *
 * Sombras da Umbra's <b>44</b> Magias are a draft, but <b>not an unrepresentable one</b> — that
 * was this javadoc's earlier claim and it did not survive checking. Only {@code GD da
 * Conjuração:} is blank on all 44; {@code Duração:} is filled on 14, {@code Alcance:} on 9,
 * {@code Efeito Crítico:} on 8 and {@code Perícia Chave para Conjuração:} on 5. {@code Spell}
 * already tolerates a null GD (7 authored Magias have one) and a null Perícia (2 do), so thinness
 * alone would not keep them out.
 *
 * <p>What keeps them out is <b>acquisition</b>: they are gated behind a <i>Força Umbral</i>
 * Talento that {@code MetamagicoFeat} does not carry, whose own prerequisite ("Apenas Devotos de
 * Senhores Umbrais") needs a devotion concept this core has none of. Authoring them would produce
 * a catalog no character could ever reach. Two of the seven also contradict themselves between
 * their expanded block and the document's earlier outline.
 *
 * <p>{@link MagicType#UMBRAL} is a real constant so they can be typed the day the Talento lands.
 * See {@code docs/rules/magias-index.md} for the full listing and for the two rules that would
 * fill the universally blank columns.
 *
 * @see SpellCatalog
 */
public enum MagicTree implements SpellTree {

    ALIADOS_DA_NATUREZA("Aliados da Natureza", MagicType.NATURAL, MagicType.INVOCACAO, null,
            AliadosDaNaturezaSpell.class),
    ANULACAO("Anulação", MagicType.PRIMORDIAL, null, null,
            AnulacaoSpell.class),
    ARSENAL_ELEMENTAL("Arsenal Elemental", MagicType.ENCANTAMENTO, MagicType.ELEMENTAL, ElementalType.TODOS,
            ArsenalElementalSpell.class),
    ARTESAO("Artesão", MagicType.ENCANTAMENTO, MagicType.ELEMENTAL, ElementalType.MAGMA,
            ArtesaoSpell.class),
    CORPO_ROCHOSO("Corpo Rochoso", MagicType.ENCANTAMENTO, MagicType.ELEMENTAL, ElementalType.TERRA,
            CorpoRochosoSpell.class),
    DOMINIO_PRIMORDIAL("Domínio Primordial", MagicType.PRIMORDIAL, null, null,
            DominioPrimordialSpell.class),
    FURIA_DE_TESLA("Fúria de Tesla", MagicType.ENCANTAMENTO, MagicType.ELEMENTAL, ElementalType.ELETRICIDADE,
            FuriaDeTeslaSpell.class),
    IRA_DE_VULCANO("Ira de Vulcano", MagicType.ELEMENTAL, null, ElementalType.MAGMA,
            IraDeVulcanoSpell.class),
    MORTE("Morte", MagicType.PROFANA, null, null,
            MorteSpell.class),
    OCULTACAO("Ocultação", MagicType.ENCANTAMENTO, null, null,
            OcultacaoSpell.class),
    PIROMANCIA("Piromancia", MagicType.DIVINA, MagicType.ELEMENTAL, ElementalType.FOGO,
            PiromanciaSpell.class),
    POLIMORFISMO("Polimorfismo", MagicType.ENCANTAMENTO, MagicType.NATURAL, null,
            PolimorfismoSpell.class),
    PROFANAR("Profanar", MagicType.PROFANA, null, null,
            ProfanarSpell.class),
    PROTECAO_INVERNAL("Proteção Invernal", MagicType.ENCANTAMENTO, MagicType.ELEMENTAL, ElementalType.GELO,
            ProtecaoInvernalSpell.class),
    REANIMAR("Reanimar", MagicType.PROFANA, MagicType.INVOCACAO, null,
            ReanimarSpell.class),
    REGENERACAO("Regeneração", MagicType.ENCANTAMENTO, MagicType.ELEMENTAL, ElementalType.AGUA,
            RegeneracaoSpell.class),
    TEMPO("Tempo", MagicType.ENCANTAMENTO, MagicType.TEMPORAL, null,
            TempoSpell.class),
    TRANSPORTE("Transporte", MagicType.TEMPORAL, MagicType.INVOCACAO, null,
            TransporteSpell.class),
    VIDA("Vida", MagicType.NATURAL, MagicType.DIVINA, null,
            VidaSpell.class),
    VOO("Voo", MagicType.ENCANTAMENTO, MagicType.ELEMENTAL, ElementalType.VENTO,
            VooSpell.class);

    private final String name;
    private final MagicType magicType;
    private final MagicType secondaryMagicType;
    private final ElementalType elementalType;

    /**
     * The enum holding this tree's Magias, or {@code null} while it is unauthored.
     *
     * <p>A {@link Class} literal rather than the constants themselves, because the two enums name
     * each other: every {@code <Tree>Spell} constant reports {@code MagicTree.X} as its tree, so
     * holding real constants here would make the two static initialisers depend on each other's
     * order. A class literal triggers no initialisation; {@link Class#getEnumConstants()} does,
     * and by the time anyone calls {@link #getSpells()} this enum is long since initialised — the
     * same deferral {@code SkillType#getExcellencyClass()} already relies on.
     */
    private final Class<? extends Spell> spellClass;

    MagicTree(final String name, final MagicType magicType, final MagicType secondaryMagicType,
              final ElementalType elementalType, final Class<? extends Spell> spellClass) {
        this.name = name;
        this.magicType = magicType;
        this.secondaryMagicType = secondaryMagicType;
        this.elementalType = elementalType;
        this.spellClass = spellClass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MagicType getMagicType() {
        return magicType;
    }

    @Override
    public Optional<MagicType> getSecondaryMagicType() {
        return Optional.ofNullable(secondaryMagicType);
    }

    @Override
    public Optional<ElementalType> getElementalType() {
        return Optional.ofNullable(elementalType);
    }

    /**
     * This tree's two ramificações, or an empty list for one that runs straight through —
     * derived from {@link MagicBranch} rather than stored, so the two enums cannot disagree
     * about which branches belong where.
     */
    @Override
    public List<SpellBranch> getBranches() {
        return MagicBranch.of(this);
    }

    @Override
    public List<Spell> getSpells() {
        return spellClass == null ? List.of() : List.of(spellClass.getEnumConstants());
    }

    /** This tree's ramificação in the given role, for a tree that diverges. */
    public Optional<MagicBranch> getBranch(final BranchRole role) {
        return Arrays.stream(MagicBranch.values())
                .filter(branch -> branch.getTree() == this && branch.getRole() == role)
                .findFirst();
    }
}
