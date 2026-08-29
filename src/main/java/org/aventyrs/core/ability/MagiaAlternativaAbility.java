package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.magic.MagicType;

/**
 * The concrete, already-resolved form of {@link FocusAbility#MAGIA_ALTERNATIVA} — one constant
 * per {@link MagicType}, so "which Tipo de Magia was chosen" is which constant a character
 * holds, not a separately-persisted value. Grant the matching constant in {@code
 * Character.attributeAbilities} instead of {@code FocusAbility#MAGIA_ALTERNATIVA} itself (which
 * stays the catalog/rules-text entry — see its own comment), exactly the shape {@link
 * PeritoTeoricoAbility} already uses for its own per-Perícia choice.
 *
 * <p>Holding one exempts its holder from {@code org.aventyrs.core.magic.Spell#isEligible}'s
 * <b>branch</b> gate on every Árvore de Magia of that type — "você pode aprender magias de ambas
 * as ramificações dos tipos de magia escolhidos". It loosens neither the level cap nor the climb
 * requirement.
 *
 * <p><b>The rules text's eight types are now all present, and {@link MagicType} carries a ninth.</b>
 * MAGIA_ALTERNATIVA names Divina, Elemental, Encantamento, Invocação, <em>Temporal</em>,
 * Primordial, Profana and <em>Umbral</em>. Temporal and Umbral were missing from {@link
 * MagicType} until the Magia catalog was authored — {@code TEMPO} and {@code TRANSPORTE} are two
 * fully-specified Temporal trees — and both exist now, so every type the ability text names has a
 * constant here. The one remaining disagreement runs the other way: {@code NATURAL} is a {@link
 * MagicType} the ability text omits, and three complete trees are typed with it, so a constant is
 * generated for it too rather than leaving those trees unexemptable. One constant per {@code
 * MagicType} value stays the rule.
 *
 * <p>Whenever a {@link MagicType} constant is added, add a matching constant here too.
 */
@Getter
@AllArgsConstructor
public enum MagiaAlternativaAbility implements AttributeAbility {

    PROFANA(MagicType.PROFANA),
    DIVINA(MagicType.DIVINA),
    INVOCACAO(MagicType.INVOCACAO),
    ENCANTAMENTO(MagicType.ENCANTAMENTO),
    NATURAL(MagicType.NATURAL),
    ELEMENTAL(MagicType.ELEMENTAL),
    PRIMORDIAL(MagicType.PRIMORDIAL),
    TEMPORAL(MagicType.TEMPORAL),
    UMBRAL(MagicType.UMBRAL);

    private final MagicType magicType;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.FOCUS;
    }

    @Override
    public String getDescription() {
        return FocusAbility.MAGIA_ALTERNATIVA.getDescription();
    }
}
