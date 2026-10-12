package org.aventyrs.core.item;

import lombok.Getter;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.Desestabilizar;
import org.aventyrs.core.effect.DisparoEsmagador;
import org.aventyrs.core.effect.Effect;
import org.aventyrs.core.effect.Incendiar;
import org.aventyrs.core.effect.TrespassarLonginquo;
import org.aventyrs.core.util.DiceRoller;

/**
 * The six "Projéteis" rows of {@code docs/rules/equipamentos.txt} — what a projectile <em>is</em>, shared by its
 * Flecha and Virote forms ({@link ProjectileItem}). Every row prints "Arma" for Dano and Alcance: the projectile
 * contributes no Dano Base and no reach of its own, only the columns kept here.
 *
 * <ul>
 *   <li><b>Efeito Crítico</b> — the Arco/Besta column reads "Projétil (17)": the number is the weapon's margin, the
 *       effect is the projectile's. {@code AttackDelivery} adds it to the shot's Efeitos Críticos.</li>
 *   <li><b>Tipo</b> (Perfurante/Esmagamento/Cortante) is prose only — Corte/Perfuração/Impacto are the standing gap
 *       (CLAUDE.md, damage-type row), so the hit stays plain Físico.</li>
 *   <li><b>Corrente de Efeitos</b> — the "Efeitos Adicionais" line where it names one, built per shot by {@link
 *       #createEffectChain(DiceRoller)} and chained when the Corrente threshold is cleared.</li>
 *   <li><b>Recuperável</b> — Bodkin's "não pode ser recuperada após ser disparado"; every other one is
 *       half-recovered at the end of combat (table ruling, 2026-10-12).</li>
 *   <li><b>Lote</b> — "Preço para N unidades": the Preço buys a lot of that many.</li>
 * </ul>
 */
@Getter
public enum ProjectileHead {

    /** Bodkin (Incomum) — Preço 2 / 15. Perfurante, Excruciante. "Margem Crítica Menor da arma aumenta em +1 número." */
    BODKIN("Bodkin", ItemRarity.UNCOMMON, 2, 15, CriticalEffectType.EXCRUCIANTE, 1, false),

    /** Esférica (Raro) — Preço 3 / 15. Esmagamento, Estilhaçador. Corrente – Disparo Esmagador. */
    ESFERICA("Esférica", ItemRarity.RARE, 3, 15, CriticalEffectType.ESTILHACADOR, 0, true),

    /** Folha Cortante (Incomum) — Preço 3 / 15. Cortante, Dilacerar. Corrente – Trespassar Longínquo. */
    FOLHA_CORTANTE("Folha Cortante", ItemRarity.UNCOMMON, 3, 15, CriticalEffectType.DILACERAR, 0, true),

    /** Ponta Simples (Comum) — Preço 3 / 30. Perfurante, Sangramento. No Efeito Adicional. */
    PONTA_SIMPLES("Ponta Simples", ItemRarity.COMMON, 3, 30, CriticalEffectType.SANGRAMENTO, 0, true),

    /** Ponta Dupla (Raro) — Preço 4 / 10. Perfurante, Ferida Profunda. Corrente – Desestabilizar. */
    PONTA_DUPLA("Ponta Dupla", ItemRarity.RARE, 4, 10, CriticalEffectType.FERIDA_PROFUNDA, 0, true),

    /** Porta Fogo (Raro) — Preço 5 / 10. Esmagamento, Inflamar. Corrente – Incendiar. */
    PORTA_FOGO("Porta Fogo", ItemRarity.RARE, 5, 10, CriticalEffectType.INFLAMAR, 0, true);

    private final String name;
    private final ItemRarity rarity;
    /** The Preço of one lot of {@link #getLotSize()} units. */
    private final int price;
    private final int lotSize;
    private final CriticalEffectType criticalEffect;
    /** "Margem Crítica Menor da arma aumenta em +N números" — Bodkin's 1, 0 for every other head. */
    private final int criticalMarginIncrease;
    private final boolean recoverable;

    ProjectileHead(final String name, final ItemRarity rarity, final int price, final int lotSize,
                   final CriticalEffectType criticalEffect, final int criticalMarginIncrease,
                   final boolean recoverable) {
        this.name = name;
        this.rarity = rarity;
        this.price = price;
        this.lotSize = lotSize;
        this.criticalEffect = criticalEffect;
        this.criticalMarginIncrease = criticalMarginIncrease;
        this.recoverable = recoverable;
    }

    /**
     * The Corrente de Efeitos this head puts on a shot, or {@code null} when its Efeitos Adicionais name none. A fresh
     * instance per shot; roller is the attack's own ({@code DeliveredAttack#getDiceRoller()}), {@code null} when the
     * caller supplied none — a dice-bearing Corrente then reports its die instead of rolling it.
     */
    public Effect createEffectChain(final DiceRoller roller) {
        return switch (this) {
            case ESFERICA -> new DisparoEsmagador();
            case FOLHA_CORTANTE -> new TrespassarLonginquo();
            case PONTA_DUPLA -> new Desestabilizar();
            case PORTA_FOGO -> new Incendiar(roller);
            case BODKIN, PONTA_SIMPLES -> null;
        };
    }
}
