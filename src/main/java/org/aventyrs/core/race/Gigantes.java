package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.feat.GiganteFeat;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.skill.Skill;

import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Defines what the Gigantes race can do under each rule-set. Three of this race's traits are
 * mechanically real today — {@link #getFixedAttributeBonuses()} (+2 Força, +2 Vigor), {@link
 * #generateEmptyCharacter} seeding {@link SizeCategory#PLUS_TWO}, and {@link
 * #getNewFeatCost(FeatCategory)} (Sobrevivência Talentos cost 2, not {@link #BASE_NEW_FEAT_COST}
 * 3) — everything else needs a system this core doesn't have yet:
 *
 * <ul>
 *   <li><b>Perícias e Talentos: nenhum</b> — the rules text explicitly grants no extra Talento
 *   or Perícia (unlike every other race so far), so this is intentionally a no-op: {@link
 *   Race}'s own defaults already express "nothing extra" with no override needed.</li>
 *   <li><b>Idiomas</b> (dialeto Ymiriano + um adicional per Antecedente) — same "no Language/
 *   Idioma concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (~500 anos) — same "no age/lifespan concept" gap as every other
 *   race; purely narrative today.</li>
 *   <li><b>Tamanho é Documento</b> (never loses the Defesa bonus/malus from Categoria de
 *   Tamanho) — DF is computable now ({@code ModifierType#PHYSICAL_DEFENSE} + {@code
 *   DefenseService}), but this clause is a *conditional* bonus keyed on the attacker's Categoria
 *   de Tamanho, which a no-arg {@code @Modifier} can't see; it needs a target-aware hook of its
 *   own, not the flat grant DM's own citations now describe (see
 *   {@code AtaqueADistanciaInteraction}'s own javadoc: a target's DF/DM lookup is "left to a
 *   layer above this core"); there's nothing here to override this against.</li>
 *   <li><b>Rigidez Ymiriana</b> (RD against attackers 2+ Categorias de Tamanho smaller) — a
 *   real RD mechanism exists ({@code
 *   org.aventyrs.core.character.services.DamageService#getTotalDamageReduction}), but it
 *   takes only the *defending* {@link Character}, no attacker at all — same gap {@code
 *   AtaqueADistanciaInteraction} needed {@code attackTarget} for, but on the defense side,
 *   where no equivalent overload exists. A flat, unconditional {@code
 *   org.aventyrs.core.modifier.Modifier}-based RD (like {@code DamageService}'s existing
 *   sources) would incorrectly apply against *every* attacker, not just smaller ones, so this
 *   isn't a case of just adding a {@code @Modifier} method.</li>
 *   <li><b>Tudo é Frágil</b> (1 dano a itens defensivos + Vantagem em dano contra objetos/
 *   construtos/construções, quando o alvo é menor) — needs an Item/Equipamento entity (doesn't
 *   exist — same gap {@code ProfissaoCompetencyAbility#FORJA_VULCANA} cites) to damage, and a
 *   way to classify an attack's target as an object/construct/construção, which this core has
 *   no concept of at all ({@link org.aventyrs.core.sheet.CombatantSheet} only ever represents
 *   a Character). The *shape* of the Vantagem-on-dano half would otherwise fit {@code
 *   SkillCompetencyAbility#resolveDamageBonus} (see {@code AtaqueADistanciaCompetencyAbility
 *   #FRIEZA}), once that classification exists.</li>
 * </ul>
 *
 * <p><b>Cuidado para não Quebrar is real</b> ({@link #resolveGoverningAttributeRollBonus}): a
 * Desvantagem on every Perícia roll governed by Força or Destreza while an ally two or more
 * Categorias de Tamanho smaller is adjacent, lifted by {@code GiganteFeat#ZELO_PELOS_FRAGEIS}.
 *
 * <p>None of the racial traits above fit {@code SkillCompetencyAbility}'s shape well
 * enough today to catalog in a {@code GigantesRacialAbility} enum (unlike {@code Anao}/
 * {@code Elfos}) — two aren't roll-conditioned at all (Tamanho é Documento, Rigidez Ymiriana),
 * one needs a target classification this core can't make (Tudo é Frágil), and one spans
 * multiple {@code SkillType}s by {@link AttributeDomain} rather than naming one (Cuidado para
 * não Quebrar) — so {@link #getRacialAbilities()} is left at {@link Race}'s own empty default
 * rather than forcing a wrong-shaped catalog into existence.
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race: "o
 * gigantes são normalmente neutros" is advisory, not a hard rule.
 */
public class Gigantes implements Race {

    private static final int SOBREVIVENCIA_FEAT_COST = 2;

    /**
     * Cuidado para não Quebrar: "enquanto adjacentes à aliados que pertençam a 2 ou mais Categorias de
     * Tamanho inferiores, sofrem desvantagem em suas rolagens de perícia físicas (baseadas em Força ou
     * Destreza)" — none with {@code GiganteFeat#ZELO_PELOS_FRAGEIS}, which "não te concede mais
     * quaisquer Desvantagens".
     */
    @Override
    public int resolveGoverningAttributeRollBonus(final AttributeDomain domain, final CombatantSheet holder,
                                                  final SceneContext holderContext,
                                                  final ToIntFunction<CombatantSheet> sizeCategoryOf) {
        if ((domain != AttributeDomain.STRENGTH && domain != AttributeDomain.DEXTERITY) || holderContext == null
                || holder.getCharacter().getFeats().contains(GiganteFeat.ZELO_PELOS_FRAGEIS)) {
            return 0;
        }
        int ownSize = sizeCategoryOf.applyAsInt(holder);
        boolean fragileAllyAdjacent = holderContext.getAlliesWithin(Range.ADJACENTE).stream()
                .anyMatch(ally -> ownSize - sizeCategoryOf.applyAsInt(ally) >= FRAGILE_SIZE_GAP);
        return fragileAllyAdjacent ? Skill.DISADVANTAGE_MALUS : 0;
    }

    /** "2 ou mais Categorias de Tamanho inferiores." */
    private static final int FRAGILE_SIZE_GAP = 2;

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.HUMANOIDE;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.STRENGTH, 2, AttributeDomain.VIGOR, 2);
    }

    @Override
    public SizeCategory getBaseSizeCategory() {
        return SizeCategory.PLUS_TWO;
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder().sizeCategory(getBaseSizeCategory());
    }

    @Override
    public int getNewFeatCost(final FeatCategory featCategory) {
        return featCategory == FeatCategory.SOBREVIVENCIA ? SOBREVIVENCIA_FEAT_COST : BASE_NEW_FEAT_COST;
    }
}
