package org.aventyrs.core.race;

import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Colosso (Elemental: Terra) race can do under each rule-set — see {@link
 * AbstractMesticoRace}'s own javadoc for the shared Mestiço Mortal/Físico Mortal mechanism and
 * validation every Mestiço Elemental shares.
 *
 * <p>Two traits are mechanically real: {@link #getFixedAttributeBonuses()} (+2 Vigor, or +3 if
 * {@code parentRace} also grants Vigor; -1 Destreza, unenforced floor of 1) and Categoria de
 * Tamanho inherited from {@code parentRace.getBaseSizeCategory()} shifted by +1 ("tendo sua
 * Categoria de Tamanho igual à de sua contraparte mortal +1").
 *
 * <p>Everything else needs a system this core doesn't have yet:
 * <ul>
 *   <li><b>Idiomas/Longevidade</b> (idiomas do parente mortal; mesma expectativa de vida) — same
 *   "no Language/age concept" gaps as every other race.</li>
 *   <li><b>2 Talentos adicionais</b> (Sobrevivência, substituível por Talento Racial do parente
 *   se houver; + 1 Talento Elemental) — same "no Feat catalog" gap; "Sobrevivência"/"Elemental"
 *   map to {@link org.aventyrs.core.feat.FeatCategory#SOBREVIVENCIA}/{@link
 *   org.aventyrs.core.feat.FeatCategory#ELEMENTAL} directly.</li>
 *   <li><b>Treinamento em Conhecimentos + Especialização Cosmologia</b> — same "no hook for
 *   granting starting Perícia training" gap as every other race; {@code
 *   ConhecimentosSpecialization.COSMOLOGIA} itself already exists.</li>
 *   <li><b>Lentos, Grandes e Pesados</b> (-2UD Movimento Base; nunca podem ser treinados em
 *   Dirigir e Cavalgar) — this core has no aggregated "Movimento Base" stat at all (same gap
 *   {@code Pequenino}'s own Ligeiro/Sempre Veloz cites), and no "cannot train in this Perícia"
 *   concept either — this codebase only ever models unenforced *prerequisites*, never an
 *   outright *ban* on training.</li>
 *   <li><b>Placidez de Epona</b> (+1 ponto temporário de Autocontrole por sessão de jogo) — same
 *   "no game session concept, no temporary Ego points" gap {@code Orc}'s own Placitude Térrea
 *   already cites.</li>
 *   <li><b>Resistente ao AEther</b> (+1 Multiplicador de PV; -1 dano sofrido; -1 Multiplicador
 *   de PM) — the PV-multiplier half would otherwise be a plain {@code
 *   @Modifier(ModifierType.LIFE_MULTIPLIER)} (mirroring {@code VigorAbility#SOBRE_HUMANO}), but
 *   {@code HitPointsServiceImpl#getLifeMultiplier} only scans {@code
 *   character.getAttributeAbilities()}, not {@code race.getRacialAbilities()} — same gap {@code
 *   Orc}'s own Vigor de Epona already cites (unlike {@code SENTIDOS_ABSOLUTOS}'s roll-bonus
 *   path, this generic scan was never extended to Race). The flat "-1 dano sofrido" (unscoped by
 *   damage type, unlike RD/RA's existing mechanism) and the PM-multiplier malus would each need
 *   their own new modifier hook too.</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "normalmente Neutros e justos" is advisory, not a hard rule.
 */
public class Colosso extends AbstractMesticoRace {

    private static final AttributeDomain PRIMARY_ATTRIBUTE = AttributeDomain.VIGOR;
    private static final AttributeDomain REDUCED_ATTRIBUTE = AttributeDomain.DEXTERITY;
    private static final int PRIMARY_BONUS = 2;
    private static final int PRIMARY_BONUS_WHEN_PARENT_GRANTS_IT = 3;
    private static final int REDUCED_BONUS = -1;
    private static final int SIZE_CATEGORY_OFFSET = 1;

    public Colosso(@NonNull final Race parentRace) {
        this(parentRace, List.of(), List.of());
    }

    public Colosso(@NonNull final Race parentRace, @NonNull final List<SkillCompetencyAbility> inheritedRacialAbilities,
                    @NonNull final List<AttributeAbility> inheritedAttributeAbilities) {
        super(parentRace, inheritedRacialAbilities, inheritedAttributeAbilities);
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        int primaryBonus = parentGrants(PRIMARY_ATTRIBUTE) ? PRIMARY_BONUS_WHEN_PARENT_GRANTS_IT : PRIMARY_BONUS;
        return Map.of(PRIMARY_ATTRIBUTE, primaryBonus, REDUCED_ATTRIBUTE, REDUCED_BONUS);
    }

    @Override
    protected int getSizeCategoryOffset() {
        return SIZE_CATEGORY_OFFSET;
    }
}
