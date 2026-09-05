package org.aventyrs.core.race;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Ogros race can do under each rule-set. Like {@code Aviano}/{@code Agastias},
 * an Ogro carries one acquisition-time choice — its {@link Aptidao} — held as a constructor
 * field feeding this object's own {@link #getFixedAttributeBonuses()}.
 *
 * <p><b>Why a choice enum and not choosable points.</b> The rules text offers exactly two
 * packages: "+2 pontos Raciais em Força e +1 ponto Racial em Destreza; ou +1 ponto em Força e +2
 * em Destreza". Expressed as {@link #getChoosableAttributeBonusPoints()} = 3 over {Força,
 * Destreza}, {@code CharacterCreationServiceImpl.allocateAttributes} would happily accept +3/+0,
 * which the clause does not offer. Two constants say exactly what is on the table — the same
 * "the choice space is small and fixed at compile time" reasoning CLAUDE.md's third
 * acquisition-choice pattern describes.
 *
 * <p>Three of this race's traits are mechanically real today: {@link
 * #getFixedAttributeBonuses()}, {@link #getCreatureType()} ({@link CreatureType#MONSTRUOSO}) and
 * {@link #getBaseSizeCategory()}/{@link #generateEmptyCharacter} ({@link SizeCategory#PLUS_ONE},
 * "grandes e imponentes"). Everything else needs a system this core doesn't have yet:
 *
 * <ul>
 *   <li><b>Lei do Mais Forte</b> (Graduações em Perícias não baseadas em Força ou Destreza custam
 *   +0.5 EXP até a terceira; Perícias baseadas em Força custam -0.5 EXP até a quinta) — two
 *   separate missing pieces, neither of them the fraction: {@code
 *   SkillGraduationService#getUpgradeCost} takes no {@link Race} at all and so has no notion of a
 *   race-specific discount (the same gap {@code Human}'s/{@code Goblin}'s Aprendizado Rápido
 *   cite), and the discount is scoped by the Perícia's governing {@link AttributeDomain} rather
 *   than by a named Perícia — the same "scoped by AttributeDomain, which nothing supports"
 *   shape {@code Gigantes}' Cuidado para não Quebrar already flags, here on the cost side. The
 *   0.5 itself is representable: Graduação cost is already a {@code BigDecimal}, unlike {@link
 *   #getNewFeatCost}'s {@code int}.</li>
 *   <li><b>Carnívoros Insaciáveis</b> (must eat twice as often, in double portions) — nothing in
 *   this core tracks food, hunger or upkeep of any kind; the same "no Fadiga/asfixia" family of
 *   gap. Purely narrative today.</li>
 *   <li><b>Mordida Poderosa</b> (Presas Longas as an Arma Natural) — no weapon catalog is
 *   authored (only {@code ArmorItem}) and nothing marks a weapon as an Arma Natural, the
 *   two-markers-missing gap CLAUDE.md's "Classifying an attack as Desarmado/Arma Natural" row
 *   names.</li>
 *   <li><b>Bocarra</b> (+1PA and 1PD before a bite attack buys Vantagem plus the Corrente de
 *   Efeitos "Devorar Inteiro": the target is swallowed, takes 1 + metade do Vigor do Ogro per
 *   Rodada, and escapes by dealing double the Ogro's Vigor in damage from inside; capacity and
 *   digestion time both scale off Vigor and the victim's Categoria de Tamanho) — the densest gap
 *   of this race by far. Corrente de Efeitos is an entirely unbuilt system ({@code
 *   EffectChainService} resolves the shared 13-entry catalog, and "Devorar Inteiro" is not one of
 *   them), "a creature is inside another creature" is a containment relation nothing models,
 *   paying extra PA to upgrade a single attack is the "this one delivered attack" scoping gap,
 *   and the per-Rodada damage would need a {@code TemporaryEffect} owned by the <i>swallower</i>
 *   rather than the victim.</li>
 *   <li><b>Visão no Escuro</b> — no vision/senses concept exists in this core.</li>
 *   <li><b>Idiomas</b> (ôgrico + o do Antecedente) — same "no Language/Idioma concept exists" gap
 *   as every other race.</li>
 *   <li><b>Longevidade</b> (adultos aos 12, até 100 anos) — same "no age/lifespan concept" gap as
 *   every other race; purely narrative today.</li>
 *   <li><b>1 Talento adicional</b> (entre {@code org.aventyrs.core.feat.FeatCategory#MONSTRUOSO}
 *   and {@code #SOBREVIVENCIA}) — {@link Race} has no hook to grant a {@code Feat} at creation,
 *   same gap as every other race's free Talentos. The rules text explicitly grants <i>no</i> extra
 *   Perícia ("fez com que os Ogros não adquirissem novas Perícias"), which needs no override at
 *   all — same no-op as {@code Gigantes}' "Perícias e Talentos: nenhum".</li>
 * </ul>
 *
 * <p>None of the six Características Raciais above fit {@code SkillCompetencyAbility}'s shape, so
 * {@link #getRacialAbilities()} is left at {@link Race}'s own empty default, same reasoning as
 * {@code Gigantes}/{@code Orc}/{@code Gorgona}.
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race.
 */
@Getter
public class Ogro implements Race {

    /**
     * The two Atributo packages an Ogro chooses between at creation. Each carries its own
     * complete map rather than a "which one gets the 2" flag, so the constant reads as the rules
     * text writes it.
     */
    @Getter
    @AllArgsConstructor
    public enum Aptidao {

        /** "+2 pontos Raciais em Força e +1 ponto Racial em Destreza." */
        FORCA_BRUTA(Map.of(AttributeDomain.STRENGTH, 2, AttributeDomain.DEXTERITY, 1)),

        /** "...ou +1 ponto em Força e +2 em Destreza." */
        AGILIDADE_BRUTA(Map.of(AttributeDomain.STRENGTH, 1, AttributeDomain.DEXTERITY, 2));

        private final Map<AttributeDomain, Integer> attributeBonuses;
    }

    private final Aptidao aptidao;

    public Ogro(@NonNull final Aptidao aptidao) {
        this.aptidao = aptidao;
    }

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.MONSTRUOSO;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return aptidao.getAttributeBonuses();
    }

    @Override
    public SizeCategory getBaseSizeCategory() {
        return SizeCategory.PLUS_ONE;
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder().sizeCategory(getBaseSizeCategory());
    }
}
