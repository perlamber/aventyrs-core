package org.aventyrs.core.effect;

import java.util.List;

import lombok.Getter;

import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellBodyChange;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * A Corrente de Efeitos that changes the body (core 0.0.99) — Ogrificar's Gigantecer and Serra-Pernas's Espremer,
 * built from their {@code SpellChainKind}. It grants like {@link BodyChangeEffect} and under the same source, which
 * is what lets the two Correntes read differently with no flag:
 *
 * <ul>
 *   <li><b>Gigantecer</b>, "em substituição ao efeito anterior": +2 Força <i>and</i> Destreza and +1 Categoria. The
 *       Atributo Ogrificar's {@code Efeito:} raised is granted again under the same source, so it is replaced rather
 *       than doubled, and the other is added.</li>
 *   <li><b>Espremer</b>, "em adicional aos efeitos anteriores": −1 Categoria on top of Serra-Pernas's own −2 Força e
 *       Destreza, which it does not touch.</li>
 * </ul>
 */
@Getter
public class BodyChangeChain extends AbstractEffect implements EffectChain {

    private final Spell spell;
    private final SpellBodyChange bodyChange;

    public BodyChangeChain(final Spell spell, final SpellBodyChange bodyChange) {
        this.spell = spell;
        this.bodyChange = bodyChange;
    }

    @Override
    public String getDescription() {
        return spell.getEffectChainDescription();
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        List<Blessing> granted = BodyChangeGrant.grant(target, spell, bodyChange, null);
        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target))
                .blessings(granted.isEmpty() ? null : granted))
                .build();
    }
}
