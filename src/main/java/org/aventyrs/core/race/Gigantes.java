package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;
import java.util.Map;

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
 *   Tamanho) — DF (Defesa Física) isn't a concept this core computes at all, same as DM (see
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
 *   no concept of at all ({@link org.aventyrs.core.sheet.CharacterSheet} only ever represents
 *   a Character). The *shape* of the Vantagem-on-dano half would otherwise fit {@code
 *   SkillCompetencyAbility#resolveDamageBonus} (see {@code AtaqueADistanciaCompetencyAbility
 *   #FRIEZA}), once that classification exists.</li>
 *   <li><b>Cuidado para não Quebrar</b> (Desvantagem on Força/Destreza-based Perícia rolls
 *   while adjacent to an ally 2+ Categorias de Tamanho smaller) — unlike {@code
 *   AnoesRacialAbility#ABATEDORES_DE_GIGANTES}, this isn't scoped to one {@code SkillType}; it
 *   applies to *every* Perícia whose governing {@link AttributeDomain} is Força or Destreza
 *   (Atletismo, Ataque Corpo-a-Corpo, Ataque à Distância, Esquiva e Aparar, Dirigir e
 *   Cavalgar, …), which doesn't fit {@code SkillCompetencyAbility#getSkillType()}'s
 *   one-fixed-SkillType-per-constant shape at all — this needs a mechanism scoped by {@code
 *   AttributeDomain} instead, which doesn't exist. The adjacency/size-comparison condition
 *   itself would be expressible today via {@code SceneContext#getAllies()}/{@code
 *   #getDistanceTo} (no {@code attackTarget}-style parameter needed, since it only reasons
 *   about the acting Character's own known allies) — it's specifically the "which Perícias
 *   does this apply to" scoping that's missing, not the proximity check.</li>
 * </ul>
 *
 * <p>None of the four racial traits above fit {@code SkillCompetencyAbility}'s shape well
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

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.STRENGTH, 2, AttributeDomain.VIGOR, 2);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder().sizeCategory(SizeCategory.PLUS_TWO);
    }

    @Override
    public int getNewFeatCost(final FeatCategory featCategory) {
        return featCategory == FeatCategory.SOBREVIVENCIA ? SOBREVIVENCIA_FEAT_COST : BASE_NEW_FEAT_COST;
    }
}
