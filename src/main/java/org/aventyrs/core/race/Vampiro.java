package org.aventyrs.core.race;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_INHERITED_RACIAL_ABILITIES;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_PARENT_RACE;

/**
 * The Vampiro race — the first {@link CreatureType#RENASCIDO} (Morto-Vivo) race in this core, and
 * a Mestiço of a bespoke shape: like {@code MeioElfo} it carries its acquisition-time choices as
 * constructor fields rather than extending {@code AbstractMesticoRace} (whose "+2/+3 elemental"
 * conditional does not fit). Two choices are made at creation:
 *
 * <ul>
 *   <li>a <b>{@link VampiroLineage}</b> — one of the seven sub-raças (Asanbosam, Baobhan Sith,
 *   Dampiro, Nosferatu, Rakshasa, Strigoi, Vlokoslav), which fixes the +1 Atributo and the Armas
 *   Naturais the vampire develops for feeding; and</li>
 *   <li>a <b>parent {@link Race}</b> (Mestiço Mortal — "escolha uma raça Feérica, Humanoide ou
 *   Monstruosa que não seja mestiça"), constrained by the lineage per the "RAÇAS POSSÍVEIS"
 *   table ({@link VampiroLineage#getAllowedParentTypes()}), plus an optional Atributo Herdado
 *   (+1 on one Atributo the parent grants) and up to 2 Características Raciais inherited from it.</li>
 * </ul>
 *
 * <h2>What is mechanically real</h2>
 * <ul>
 *   <li><b>{@link #getFixedAttributeBonuses()}</b> — the lineage's +1, plus the Atributo Herdado
 *   +1 when chosen (validated against {@code parentRace.getFixedAttributeBonuses().keySet()}),
 *   merged the same way {@code MeioElfo}'s inherited Atributo is.</li>
 *   <li><b>{@link #getCreatureType()}</b> — {@link CreatureType#RENASCIDO}. <b>{@link
 *   #getPrerequisiteCreatureType()}</b> returns the parent's type instead, because the rules say
 *   "Para critérios de pré-requisitos, Vampiros podem ser considerados Feéricos, Humanoides ou
 *   Monstruosos, conforme sua raça em vida" — so an Asanbosam (Monstruoso parent) can still take
 *   {@code MonstruosoFeat}, a Baobhan {@code FeericoFeat}, and so on.</li>
 *   <li><b>{@link #getGrantedNaturalWeapons()}</b> — the lineage's Presas Longas and/or Garras
 *   Afiadas, surfaced through {@code Character#getNaturalWeapons()} and usable as an {@code
 *   AttackSource} with no further wiring (Dano Base and Perícia come from the {@link
 *   NaturalWeapon} catalog).</li>
 *   <li><b>{@link #generateEmptyCharacter}</b> / <b>{@link #getBaseSizeCategory()}</b> — the
 *   parent's Categoria de Tamanho, unchanged ("permanecem nas mesma Categoria de Tamanho de sua
 *   raça anterior").</li>
 *   <li><b>{@link #getRacialAbilities()}</b> — the up-to-2 inherited Características, the same
 *   partially-real "2 aleatórias" treatment {@code MeioElfo} documents (this core rolls nothing;
 *   the caller supplies the already-resolved pair, validated against the parent's list).</li>
 * </ul>
 *
 * <h2>What needs a system this core doesn't have</h2>
 * <ul>
 *   <li><b>Anatomia de Morto-Vivo</b> ("não precisam dormir ou respirar"; "não podem recuperar
 *   PV com magias Divinas … são curados com Magia Profana" — Profana heals a Vampiro for
 *   1d6+metade do Vigor instead of damaging; "imunes a efeitos Naturais") — the "Fadiga/asfixia,
 *   and healing inversion" gap (nothing tracks sleep/breath, and {@code CombatantSheet#heal} has
 *   no hook to redirect a recovery into damage or vice-versa), the missing Divine-vs-Profana
 *   magic-source distinction, and the missing damage-type immunity (no way to nullify "Natural"
 *   damage). {@code RENASCIDO} exists as the tag those systems will key on; none reads it yet.</li>
 *   <li><b>Vulnerabilidade Vampírica</b> (enfraquecidos/destruídos pela luz do sol — a -2 or
 *   per-Rodada -1 to the Multiplicador de PV; Vulneráveis a Dyospiros e ao Fogo) — no
 *   time-of-day/sunlight state, no round-scoped Multiplicador de PV reduction ({@code
 *   ModifierType#LIFE_MULTIPLIER} is summed, never subtracted per Rodada), no vulnerability
 *   (damage-amplification) mechanism, and no material column on {@code Item} for "Dyospiros".</li>
 *   <li><b>Sangue, Poder e Dependência</b> (perde 3PV/dia, dobrado sem Descanso Longo; PV
 *   perdidos assim só voltam com Roubo de Vida) — no per-day upkeep clock, and the Poder
 *   Vampírico activation (Ação Livre, 2 Rodadas, 3PV) is the same activation/Duração gap the
 *   whole {@code VampiricoFeat} tree is blocked on.</li>
 *   <li><b>Roubo de Vida 1 nas Armas Naturais desta Característica</b> — {@code LifeStealService}
 *   sums only an active {@code LifeSteal} effect on the sheet plus {@code
 *   AttributeAbility#resolveLifeStealBonus}; there is no {@code Race}/{@code
 *   SkillCompetencyAbility} hook and no per-weapon scoping, so this cannot be expressed. Same gap
 *   {@code VampiricoFeat#SEDE_DE_SANGUE}/{@code ARMAMENTO_DE_ORLOK} cite.</li>
 *   <li><b>Falsos Descansos</b> (Vampiros não recuperam PV com Descansos; Dampiros recuperam
 *   metade) — {@code RestService} has no race hook to suppress or halve recovery.</li>
 *   <li><b>1 Talento adicional</b> (Racial da raça em vida ou Vampírico) — {@code Race} has no
 *   hook to grant a {@code Feat} at creation, the same gap every other race's free Talentos hit.
 *   "Vampiros não recebem Perícias adicionais" needs nothing — it is the baseline.</li>
 *   <li><b>Idiomas / Longevidade</b> — the standard "no Language/Idioma concept" and "no
 *   age/lifespan concept" gaps; the Dampiro's mortality and ~3× lifespan are narrative only.</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same as every other race.
 */
@Getter
public class Vampiro implements Race {

    private static final int MAX_INHERITED_RACIAL_ABILITIES = 2;

    /**
     * The seven sub-raças. Each fixes the +1 Atributo the pós-vida grants, the Armas Naturais the
     * vampire develops for feeding (Sangue, Poder e Dependência), and which {@link CreatureType}s
     * a parent race may have (the "RAÇAS POSSÍVEIS" table — "Todas" is all three living types).
     */
    @Getter
    @AllArgsConstructor
    public enum VampiroLineage {

        /** Monstros e Monstruosos transformados — bebem sangue e se fartam da carne das vítimas. */
        ASANBOSAM(AttributeDomain.VIGOR, Set.of(NaturalWeapon.PRESAS_LONGAS), Set.of(CreatureType.MONSTRUOSO)),

        /** Fadas, Fúrias e Górgonas oferecidas a entidades obscuras — as Bruxas Verdadeiras. */
        BAOBHAN_SITH(AttributeDomain.CHARISMA, Set.of(NaturalWeapon.GARRAS_AFIADAS), Set.of(CreatureType.FEERICO)),

        /** Filhos de Asanbosam, Baobhan ou Nosferatu com mortais — têm as forças, não as fraquezas. */
        DAMPIRO(AttributeDomain.DEXTERITY, Set.of(NaturalWeapon.PRESAS_LONGAS), allLivingTypes()),

        /** Humanoides castos drenados até a última gota e alimentados com Sangue Nosferatu. */
        NOSFERATU(AttributeDomain.STRENGTH, Set.of(NaturalWeapon.PRESAS_LONGAS), Set.of(CreatureType.HUMANOIDE)),

        /** Verdadeiros devotos de Lacerto convertidos em vida por um rito excruciante. */
        RAKSHASA(AttributeDomain.INSTINCT,
                Set.of(NaturalWeapon.PRESAS_LONGAS, NaturalWeapon.GARRAS_AFIADAS), allLivingTypes()),

        /** Vampiros Artificiais, criados por Arcanismo — uma seita única de rigor hierárquico. */
        STRIGOI(AttributeDomain.FOCUS, Set.of(NaturalWeapon.GARRAS_AFIADAS), allLivingTypes()),

        /** Servos do Muitos-Olhos, marcados por um terceiro olho e vestes sobrenaturalmente brancas. */
        VLOKOSLAV(AttributeDomain.GNOSE, Set.of(NaturalWeapon.GARRAS_AFIADAS), allLivingTypes());

        private final AttributeDomain bonusAttribute;
        private final Set<NaturalWeapon> naturalWeapons;
        private final Set<CreatureType> allowedParentTypes;

        private static Set<CreatureType> allLivingTypes() {
            return Set.of(CreatureType.HUMANOIDE, CreatureType.FEERICO, CreatureType.MONSTRUOSO);
        }
    }

    private final VampiroLineage lineage;
    private final Race parentRace;
    private final AttributeDomain chosenInheritedAttribute;
    private final List<SkillCompetencyAbility> inheritedRacialAbilities;

    public Vampiro(@NonNull final VampiroLineage lineage, @NonNull final Race parentRace) {
        this(lineage, parentRace, null, List.of());
    }

    public Vampiro(@NonNull final VampiroLineage lineage, @NonNull final Race parentRace,
                   final AttributeDomain chosenInheritedAttribute,
                   @NonNull final List<SkillCompetencyAbility> inheritedRacialAbilities) {
        if (parentRace.isMestico() || !lineage.getAllowedParentTypes().contains(parentRace.getCreatureType())) {
            throw new IllegalOperationException(INVALID_PARENT_RACE);
        }
        if (chosenInheritedAttribute != null
                && !parentRace.getFixedAttributeBonuses().containsKey(chosenInheritedAttribute)) {
            throw new IllegalOperationException(INVALID_PARENT_RACE);
        }
        if (inheritedRacialAbilities.size() > MAX_INHERITED_RACIAL_ABILITIES
                || !parentRace.getRacialAbilities().containsAll(inheritedRacialAbilities)) {
            throw new IllegalOperationException(INVALID_INHERITED_RACIAL_ABILITIES);
        }
        this.lineage = lineage;
        this.parentRace = parentRace;
        this.chosenInheritedAttribute = chosenInheritedAttribute;
        this.inheritedRacialAbilities = inheritedRacialAbilities;
    }

    /** {@link CreatureType#RENASCIDO} — a Vampiro <em>is</em> a Morto-Vivo. See {@link #getPrerequisiteCreatureType()}. */
    @Override
    public CreatureType getCreatureType() {
        return CreatureType.RENASCIDO;
    }

    /** The parent race's type — "conforme sua raça em vida", what every "Apenas … Feérico/Humanoide/Monstruoso" gate checks. */
    @Override
    public CreatureType getPrerequisiteCreatureType() {
        return parentRace.getCreatureType();
    }

    @Override
    public boolean isMestico() {
        return true;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        final Map<AttributeDomain, Integer> bonuses = new HashMap<>();
        bonuses.merge(lineage.getBonusAttribute(), 1, Integer::sum);
        if (chosenInheritedAttribute != null) {
            bonuses.merge(chosenInheritedAttribute, 1, Integer::sum);
        }
        return Map.copyOf(bonuses);
    }

    @Override
    public SizeCategory getBaseSizeCategory() {
        return parentRace.getBaseSizeCategory();
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder().sizeCategory(getBaseSizeCategory());
    }

    @Override
    public List<SkillCompetencyAbility> getRacialAbilities() {
        return inheritedRacialAbilities;
    }

    @Override
    public List<NaturalWeapon> getGrantedNaturalWeapons() {
        return List.copyOf(EnumSet.copyOf(lineage.getNaturalWeapons()));
    }
}
