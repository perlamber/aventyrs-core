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
 * <p><b>The rules text and {@link MagicType} disagree, and this enum follows the enum.</b>
 * MAGIA_ALTERNATIVA names eight types — Divina, Elemental, Encantamento, Invocação,
 * <em>Temporal</em>, Primordial, Profana and <em>Umbral</em> — while {@link MagicType} has seven:
 * those minus Temporal and Umbral, plus a {@code NATURAL} the ability text omits. One constant
 * per existing {@code MagicType} value is generated here, so this enum stays complete with
 * respect to the type system; whichever list is authoritative, resolving it means changing
 * {@link MagicType} first and this enum follows. Worth settling before authoring a tree typed
 * {@code NATURAL}, which could never be exempted if NATURAL isn't a real Tipo de Magia.
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
    PRIMORDIAL(MagicType.PRIMORDIAL);

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
