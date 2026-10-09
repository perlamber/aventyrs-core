package org.aventyrs.core.combat;

import lombok.NonNull;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.Effect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.TitleAttackModifiers;

import java.util.ArrayList;
import java.util.List;

/**
 * What an attack would carry beyond its damage, asked <em>before</em> it is made — the Efeitos Críticos and
 * Correntes de Efeitos a sheet screen lists beside each weapon, the same way {@code CriticalService} answers
 * the Margem Crítica and Dano Crítico beforehand.
 *
 * <p>Assembled from the same sources {@link AttackDelivery} chains on a landed hit, minus anything only the
 * attack itself can say: no target (so no anatomy immunity, no target-scoped Talento), no roll (so no
 * roll-activated Talento, no Golpe Trovejante doubling, no Explosão Cataclísmica's Cataclismo), and no
 * request-named additions (an {@code EmpoweredAttack}'s Efeito Crítico). A Corrente still has to clear the
 * defender's Defesa by {@code EffectChainService}'s margin to fire; this only says what would be in it.
 *
 * @param criticalEffects       the Efeitos Críticos an Acerto Crítico applies, in order — the source's own
 *                              (or a Título's override, repeated by Finalização), each Título's additions, then
 *                              each Talento's (Abrir Feridas's Sangramento)
 * @param hitCriticalEffects    the Efeitos Críticos a plain hit applies at Menor — Finalização's repetitions
 * @param effectChains          the Correntes de Efeitos the attack carries: its Títulos', its Talentos' and a
 *                              foe's own stat block's
 * @param criticalEffectChains  the stages added only on an Acerto Crítico, whether or not the Corrente
 *                              threshold was cleared (Arte do Escudo Atacante's Rugido, Abrir Defesas)
 */
public record AttackEffectsPreview(List<CriticalEffectType> criticalEffects,
                                   List<CriticalEffectType> hitCriticalEffects,
                                   List<EffectChain> effectChains,
                                   List<Effect> criticalEffectChains) {

    public AttackEffectsPreview {
        criticalEffects = List.copyOf(criticalEffects);
        hitCriticalEffects = List.copyOf(hitCriticalEffects);
        effectChains = List.copyOf(effectChains);
        criticalEffectChains = List.copyOf(criticalEffectChains);
    }

    /**
     * What holder's attack with attackSource, rolled on attackSkill, carries right now. sceneContext may be
     * {@code null} — a clause scoped to the Scene then simply doesn't apply.
     */
    public static AttackEffectsPreview of(@NonNull final CombatantSheet holder, @NonNull final SkillType attackSkill,
                                          final AttackSource attackSource, final SceneContext sceneContext) {
        List<EffectChain> chains = new ArrayList<>(
                TitleAttackModifiers.resolve(holder, attackSource, null, sceneContext).effectChains());
        holder.getCharacter().getFeats().forEach(feat -> chains.addAll(feat.resolveEffectChains(
                holder.getCharacter(), attackSkill, attackSource, holder, sceneContext, null)));
        if (holder instanceof MonsterSheet foe) {
            chains.addAll(foe.getAttackEffectChains());
        }
        List<Effect> criticalChains = new ArrayList<>();
        holder.getCharacter().getFeats().forEach(feat -> criticalChains.addAll(feat.resolveCriticalHitEffectChains(
                holder.getCharacter(), attackSkill, attackSource, holder, null)));
        SkillCompetencyAbility.allFor(holder.getCharacter(), holder).forEach(ability ->
                criticalChains.addAll(ability.resolveCriticalHitEffects(attackSkill, holder)));
        // The source's own, then what the holder's Talentos add on an Acerto Crítico — Abrir Feridas's
        // "'Sangramento' como Efeito Crítico adicional" — in AttackDelivery#allCriticalEffects's order.
        List<CriticalEffectType> criticals = new ArrayList<>(
                CriticalEffectResolver.typesOf(holder, attackSource, attackSkill, true, null, 0));
        holder.getCharacter().getFeats().forEach(feat -> feat.resolveExtraCriticalEffects(holder.getCharacter(),
                        attackSkill, attackSource, CriticalResult.ACERTO_CRITICO_MENOR)
                .forEach(effect -> criticals.add(effect.getType())));
        return new AttackEffectsPreview(
                criticals,
                CriticalEffectResolver.typesOf(holder, attackSource, attackSkill, false, null, 0),
                chains, criticalChains);
    }

    public boolean isEmpty() {
        return criticalEffects.isEmpty() && hitCriticalEffects.isEmpty() && effectChains.isEmpty()
                && criticalEffectChains.isEmpty();
    }
}
