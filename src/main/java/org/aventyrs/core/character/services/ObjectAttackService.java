package org.aventyrs.core.character.services;

import org.aventyrs.core.item.Item;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

/**
 * Atacar Objetos / Desarmar (acoes-de-combate.txt 20261013, core 0.1.6.19) — "Tempo de Ação: 2PA. Inflige danos aos
 * equipamentos do alvo. Rolagem de Perícia de Ataque, GD aumentado em +1 nível. Em caso de sucesso inflige Dano ao
 * Equipamento escolhido. Danos causados à objetos desta forma são reduzidos à metade (efeito de Meio-Dano). Recebe a
 * Corrente de Efeitos – Desarmar. Vulnerável a reação Defender o Perímetro."
 *
 * <p>⚠️ A reading: a flat Defesa has no GD tier, so "+1 nível" is {@link #LEVEL_STEP} added to it — the gap between
 * Médio and Difícil. The damage halves once (Meio-Dano never quarters) and then meets the item's own reductions
 * ({@code Item#applyDamage}). The Defender o Perímetro it provokes is the caller's window ({@code
 * ReactionTrigger#PROVOKING_MANOEUVRE}). Unattended scenery is the Narrador's to adjudicate.
 */
public interface ObjectAttackService {

    /** "Tempo de Ação: 2PA". */
    ActionCost OBJECT_ATTACK_COST = ActionCost.ofActionPoints(2);

    /** One GD level on a flat Defesa — Difícil's value less Médio's. */
    int LEVEL_STEP = 5;

    /**
     * Strikes item, equipped on target.
     *
     * @param rawDamage the attack's dano roll, before the halving
     * @throws org.aventyrs.core.sheet.IllegalOperationException ({@code ITEM_NOT_EQUIPPED}) when target does not wear
     *         or wield item
     */
    ObjectAttackResult strike(CombatantSheet attacker, CombatantSheet target, int targetDefense, Item item,
                              SkillType attackSkill, SkillRoll roll, int rawDamage, SceneContext sceneContext);

    /**
     * What the strike did.
     *
     * @param itemDamage how much Dureza the item lost
     * @param destroyed  whether the item is destroyed now
     * @param disarmed   whether the Corrente Desarmar landed
     */
    record ObjectAttackResult(ActionCost actionCost, int total, int required, boolean hit, int itemDamage,
                              boolean destroyed, boolean disarmed, InteractionResult rollResult) {
    }
}
