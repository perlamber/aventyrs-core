package org.aventyrs.core.title.bruxo;

import lombok.NonNull;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.SpellTree;

import java.util.EnumSet;
import java.util.Set;

/**
 * Which Árvores a Bruxo trait's "Você aprende um Misticismo …" clause admits.
 *
 * <p>Four shapes appear in the rules text:
 * <ul>
 *   <li>"à sua escolha": any tree ({@link #ANY});</li>
 *   <li>"escolhido entre magias do tipo Elemental: Ar ou Divina": a tree of that element or that
 *       type ({@link #elementOr});</li>
 *   <li>"um novo Misticismo de Invocação": a tree tagged {@link MagicType#INVOCACAO}
 *       ({@link #INVOCATION});</li>
 *   <li>"que não seja de Invocação": any other tree ({@link #NON_INVOCATION}).</li>
 * </ul>
 *
 * <p>Table ruling (2026-10-05): <b>either half</b> of a tree's two-part tag counts ({@link
 * SpellTree#hasMagicType}), so Piromancia ({@code Divina/Elemental: Fogo}) qualifies as Divina, and
 * Arsenal Elemental ({@code Elemental: Todos}) qualifies for every element. ⚠️ "Elemental: Natural" also
 * admits a tree tagged {@link MagicType#NATURAL} standalone (Aliados da Natureza, Polimorfismo, Vida),
 * since the catalog uses "Natural" at both levels without reconciling them.
 */
public record MisticismoFilter(Set<ElementalType> elements, Set<MagicType> types, Boolean invocation) {

    /** "Você aprende um Misticismo à sua escolha." */
    public static final MisticismoFilter ANY = new MisticismoFilter(Set.of(), Set.of(), null);

    /** Invocação Dupla: "um novo Misticismo de Invocação". */
    public static final MisticismoFilter INVOCATION = new MisticismoFilter(Set.of(), Set.of(), true);

    /** Pacto de Conjuração: "um novo Misticismo que não seja de Invocação". */
    public static final MisticismoFilter NON_INVOCATION = new MisticismoFilter(Set.of(), Set.of(), false);

    public MisticismoFilter {
        elements = elements.isEmpty() ? Set.of() : EnumSet.copyOf(elements);
        types = types.isEmpty() ? Set.of() : EnumSet.copyOf(types);
    }

    /** "escolhido entre magias do tipo Elemental: {element} ou {type}". */
    public static MisticismoFilter elementOr(@NonNull final ElementalType element, @NonNull final MagicType type) {
        return new MisticismoFilter(Set.of(element), Set.of(type), null);
    }

    /** Whether tree may be learned through the clause this filter describes. */
    public boolean admits(@NonNull final SpellTree tree) {
        if (invocation != null && tree.hasMagicType(MagicType.INVOCACAO) != invocation) {
            return false;
        }
        if (elements.isEmpty() && types.isEmpty()) {
            return true;
        }
        return types.stream().anyMatch(tree::hasMagicType) || matchesElement(tree);
    }

    private boolean matchesElement(final SpellTree tree) {
        if (elements.contains(ElementalType.NATURAL) && tree.hasMagicType(MagicType.NATURAL)) {
            return true;
        }
        return tree.hasMagicType(MagicType.ELEMENTAL) && tree.getElementalType()
                .map(element -> element == ElementalType.TODOS || elements.contains(element))
                .orElse(false);
    }
}
