package org.aventyrs.core.item;

import java.util.List;

import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.Effect;
import org.aventyrs.core.effect.Rugido;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.Skill;

import lombok.Getter;

/**
 * The Tipo of a Pedra do Poder: what a bound stone does. There is one constant per entry of {@code
 * docs/rules/pedras-do-poder.txt} (V19, 2026-10-11), "Tipos de Pedras do Poder". The other axis, its Preço and charge
 * economy, is {@link PowerStoneQuality}. A {@link PowerStone} pairs the two.
 *
 * <p><b>A stone acts only while vinculada</b> (bound to a host for its Duração, by {@code PowerStoneService}). It then
 * grants an <em>Efeito Base</em> plus a second effect chosen by the host's {@link Item#getType()}:
 * <ul>
 *   <li>the <em>Efeito Protetivo</em> for an Armadura or Escudo, and</li>
 *   <li>the <em>Efeito Violento</em> for an Arma.</li>
 * </ul>
 * The accessors keep their historical names: {@link #getDefensiveEffect()} is the Protetivo,
 * {@link #getOffensiveEffect()} the Violento. {@link #resolveBonus(ModifierType, ItemType)} sums the Base and whichever
 * mode the host calls for. Since every effect reads through the host's {@link Item#getPowerStone()}, an unbound stone
 * grants nothing, with no service change.
 *
 * <p><b>What reaches real machinery.</b> Typed {@link ItemBonus}es cover:
 * <ul>
 *   <li>Atributo +1 (summed into {@code Character#getEffectiveAttributeTotal});</li>
 *   <li>RD and RDS;</li>
 *   <li>Movimento;</li>
 *   <li>Defesas;</li>
 *   <li>Vantagem on a Perícia;</li>
 *   <li>Resistência a Críticos (read by {@code CombatantSheet#getTotalCriticalResistance});</li>
 *   <li>+1PA (read by {@code ActionPointsServiceImpl});</li>
 *   <li>Margem Crítica Menor.</li>
 * </ul>
 * Dedicated hooks cover:
 * <ul>
 *   <li>Margem Crítica Maior ({@link #resolveMajorCriticalMarginIncrease});</li>
 *   <li>Dano Crítico ({@link #resolveCriticalDamage});</li>
 *   <li>an extra Efeito Crítico ({@link #getViolentCriticalEffect()});</li>
 *   <li>a Corrente ({@link #createViolentEffectChain});</li>
 *   <li>the damage's sacred or profane nature ({@link #getOffensiveSanctity()});</li>
 *   <li>Dano Base ({@link #resolveDamageBaseIncrease});</li>
 *   <li>Terreno Difícil ({@link #ignoresDifficultTerrain()}).</li>
 * </ul>
 * A Violento effect that improves <em>an attack</em> (Margem, crit damage, Efeito, Corrente) applies only to attacks
 * made with the host weapon itself. Every other clause carries a per-constant TODO naming its missing system.
 *
 * <p>A numberless "RD" is {@link DamageService#DEFAULT_DAMAGE_REDUCTION} points, a numberless "RDS" one point, and a
 * numberless "Resistência à Críticos" {@link CombatantSheet#CRITICAL_RESISTANCE_INSTANCE}. These are the same
 * readings every other source uses.
 */
@Getter
public enum PowerStoneType {

    /**
     * Adamante Bruto (Sintética).
     *
     * <p>Real: Base Vigor +1; Protetivo RD and RDS.
     *
     * <p>TODO:
     * <ul>
     *   <li>the Impacto Meteórico Corrente — V19 names it without stating its effect;</li>
     *   <li>the Violento "Ataques Ignoram RD, Acertos Críticos … ignoram Meio-Dano" — no attack-side
     *       mitigation-bypass flag exists.</li>
     * </ul>
     */
    ADAMANTE_BRUTO("Adamante Bruto", "Sintética",
            "Vigor +1, ataques recebem a Corrente de Efeitos – Impacto Meteórico.",
            "RD e RDS.",
            "Ataques Ignoram RD, Acertos Críticos adicionalmente ignoram efeitos de Meio-Dano.",
            List.of(new ItemBonus(ModifierType.VIGOR_BONUS, 1)),
            List.of(new ItemBonus(ModifierType.DAMAGE_REDUCTION, DamageService.DEFAULT_DAMAGE_REDUCTION),
                    new ItemBonus(ModifierType.DAMAGE_TAKEN_REDUCTION, 1)),
            List.of()),

    /**
     * Aqua Marina (Elemental: Água).
     *
     * <p>TODO:
     * <ul>
     *   <li>asphyxia damage — no asphyxia tracking (the Fadiga/asfixia gap);</li>
     *   <li>granting a Natação mode from an item — {@code MovementService} reads only race/Talento/competency
     *       grants;</li>
     *   <li>the first-instance RE;</li>
     *   <li>the +2UD reach — no equipment range pass;</li>
     *   <li>retyping a weapon's damage — no item retype hook.</li>
     * </ul>
     */
    AQUA_MARINA("Aqua Marina", "Elemental: Água",
            "Danos de Asfixia reduzidos à zero, concede Movimento Base de Natação.",
            "Ignora o primeiro dano Elemental: Água sofrido, então concede RE: Água.",
            "Distância do Ataque aumenta em +2UD, Dano causado é Elemental: Água em substituição aos seus tipos."),

    /**
     * Calcita Vulcânica (Elemental: Magma).
     *
     * <p>Real: Base Margem Crítica Menor +1 on every attack, and Resistência a Críticos; Violento Dano Crítico +1d6
     * on the host weapon.
     *
     * <p>TODO: the first-instance RE, and the retyping (no item retype hook).
     */
    CALCITA_VULCANICA("Calcita Vulcânica", "Elemental: Magma",
            "Margem Crítica Menor de ataques aumentada em +1 e Resistência à Críticos.",
            "Ignora o primeiro dano Elemental: Magma sofrido, então concede RE: Magma.",
            "Dano Crítico +1d6, Dano causado é Elemental: Magma em substituição aos seus tipos.",
            List.of(new ItemBonus(ModifierType.LESSER_CRITICAL_MARGIN, 1),
                    new ItemBonus(ModifierType.CRITICAL_RESISTANCE, CombatantSheet.CRITICAL_RESISTANCE_INSTANCE)),
            List.of(), List.of()) {
        @Override
        public CriticalDamage resolveCriticalDamage(final ItemType hostType, final boolean hostWielded) {
            return hostType == ItemType.OFFENSIVE && hostWielded ? CriticalDamage.ofDice(1) : CriticalDamage.NONE;
        }
    },

    /**
     * Cianita Dracônica (Dracônica).
     *
     * <p>Real: Base Instinto +1.
     *
     * <p>TODO:
     * <ul>
     *   <li>"+2 em Danos Elementais" — no element-scoped damage bonus;</li>
     *   <li>the first-elemental-hit-per-Rodada Meio-Dano;</li>
     *   <li>ignoring RE;</li>
     *   <li>Resistência à Correntes −2 — no such number exists.</li>
     * </ul>
     */
    CIANITA_DRACONICA("Cianita Dracônica", "Dracônica",
            "Instinto +1, Bônus de +2 em Danos Elementais.",
            "O primeiro Dano Elemental sofrido em cada Rodada é reduzido à metade (efeito de meio-dano).",
            "Ignora RE e reduz a Resistência à Correntes de Efeitos do alvo em -2.",
            List.of(new ItemBonus(ModifierType.INSTINCT_BONUS, 1)), List.of(), List.of()),

    /**
     * Citrino Incandescente (Elemental: Fogo).
     *
     * <p>Real: Violento Inflamar as an additional Efeito Crítico.
     *
     * <p>TODO: the per-Rodada fire aura (outward damage at a Rodada boundary is caller-dealt), the first-instance RE,
     * and the retyping.
     */
    CITRINO_INCANDESCENTE("Citrino Incandescente", "Elemental: Fogo",
            "Personagens em Distância Muito Curta sofrem 3 pontos de Dano Elemental: Fogo a cada Rodada.",
            "Ignora o primeiro dano Elemental: Fogo sofrido, então concede RE: Fogo.",
            "Dano causado é Elemental: Fogo em substituição aos seus tipos, recebe Inflamar como Efeito Crítico adicional."),

    /**
     * Coração Vazio (Planar).
     *
     * <p>TODO:
     * <ul>
     *   <li>the Base start-of-Rodada self-damage locked until a Descanso Verdadeiro — no per-Rodada item tick;</li>
     *   <li>the Protetivo melee retaliation — no item {@code RETALIATION_DAMAGE} source;</li>
     *   <li>Primordial retyping and Roubo de Vida 2 — {@code LifeStealService} only amplifies an existing source.</li>
     * </ul>
     */
    CORACAO_VAZIO("Coração Vazio", "Planar",
            "Você sofre 2 pontos de Dano Primordial no início de cada Rodada, danos sofridos desta forma só podem ser "
                    + "recuperados com Descansos Verdadeiros.",
            "Inimigos que te ataquem Corpo-a-Corpo sofrem 3 pontos de Dano Primordial, este efeito possui Roubo de Vida 2.",
            "Dano causado é Primordial em substituição aos seus tipos, você recebe Roubo de Vida 2."),

    /**
     * Esfera de AEther (Primordial).
     *
     * <p>Real: Base Foco +1. The Violento "pode ser anexo apenas à Artefatos de Conjuração" is enforced by {@link
     * Item#acceptsPowerStone(PowerStone)} ({@link #isViolentRestrictedToCastingArtifacts()}).
     *
     * <p>Its Violento "Dano e Cura mágica +3" is real on an artefact host ({@code CastingArtifacts#spellEffectBonus}).
     *
     * <p>TODO: the Protetivo magic-damage and Encantamento/Maldição halving, and the Violento's +2 Rodadas on
     * Encantamentos e Maldições (no Maldição tag).
     */
    ESFERA_DE_AETHER("Esfera de AEther", "Primordial",
            "Foco +1",
            "Danos mágicos sofridos reduzidos à metade, Duração de Encantamentos e Maldições sofridas reduzidas à metade.",
            "Pode ser anexo apenas à Artefatos de Conjuração. Dano e Cura mágica +3, Duração de seus Encantamentos e "
                    + "Maldições +2 Rodadas.",
            List.of(new ItemBonus(ModifierType.FOCUS_BONUS, 1)), List.of(), List.of()) {
        @Override
        public boolean isViolentRestrictedToCastingArtifacts() {
            return true;
        }
    },

    /**
     * Fluorita Prismática (Elemental).
     *
     * <p>Real: Base Carisma +1.
     *
     * <p>TODO: Conjuração +1 (no item casting hook yet), Defesas scoped to elemental effects, and Primordial
     * retyping.
     */
    FLUORITA_PRISMATICA("Fluorita Prismática", "Elemental",
            "Carisma +1, Conjuração +1",
            "Defesas +3 para resistir a efeitos Elementais.",
            "Danos causados são Primordiais em substituições aos seus tipos.",
            List.of(new ItemBonus(ModifierType.CHARISMA_BONUS, 1)), List.of(), List.of()),

    /**
     * Hematita do Vendaval (Elemental: Ar).
     *
     * <p>Real:
     * <ul>
     *   <li>Base Destreza +1 and Movimento +3UD;</li>
     *   <li>Violento Vantagem no Ataque (both Perícias de Ataque);</li>
     *   <li>Violento Corrente Rugido, built by {@link #createViolentEffectChain}.</li>
     * </ul>
     *
     * <p>TODO: the first-instance RE.
     */
    HEMATITA_DO_VENDAVAL("Hematita do Vendaval", "Elemental: Ar",
            "Destreza +1, Movimento Base +3UD",
            "Ignora o primeiro dano Elemental: Ar sofrido, então concede RE: Ar.",
            "Vantagem no Ataque e Corrente de Efeitos – Rugido.",
            List.of(new ItemBonus(ModifierType.DEXTERITY_BONUS, 1), new ItemBonus(ModifierType.MOVEMENT, 3)),
            List.of(),
            List.of(new ItemBonus(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, Skill.ADVANTAGE_BONUS),
                    new ItemBonus(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, Skill.ADVANTAGE_BONUS))) {
        @Override
        public Effect createViolentEffectChain(final boolean critical) {
            return new Rugido(critical);
        }
    },

    /**
     * Mitral Puro (Sintética).
     *
     * <p>Real:
     * <ul>
     *   <li>Base Vantagem on both Perícias de Ataque and Movimento +1UD;</li>
     *   <li>Protetivo Resistência a Críticos;</li>
     *   <li>Violento Margem Crítica Menor and Maior +1 on the host weapon.</li>
     * </ul>
     *
     * <p>TODO: Resistência a Correntes +1 (no such number exists).
     */
    MITRAL_PURO("Mitral Puro", "Sintética",
            "Vantagem nas rolagens de Perícia de Ataque, Movimento Base +1UD",
            "Resistência à Críticos e Resistência a Correntes de Efeitos +1.",
            "Margem Crítica Menor e Maior aumentadas em +1.",
            List.of(new ItemBonus(ModifierType.MOVEMENT, 1),
                    new ItemBonus(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, Skill.ADVANTAGE_BONUS),
                    new ItemBonus(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, Skill.ADVANTAGE_BONUS)),
            List.of(new ItemBonus(ModifierType.CRITICAL_RESISTANCE, CombatantSheet.CRITICAL_RESISTANCE_INSTANCE)),
            List.of(new ItemBonus(ModifierType.LESSER_CRITICAL_MARGIN, 1))) {
        @Override
        public int resolveMajorCriticalMarginIncrease(final ItemType hostType, final boolean hostWielded) {
            return hostType == ItemType.OFFENSIVE && hostWielded ? 1 : 0;
        }
    },

    /**
     * Moldavita Selvagem (Natural).
     *
     * <p>TODO:
     * <ul>
     *   <li>Feridas Dolorosas on hit, and immunity to Venenos and to Natural effects — no item → Condição/immunity
     *       path;</li>
     *   <li>the first-instance Natural ignore;</li>
     *   <li>Natural retyping and the Veneno da Moldavita.</li>
     * </ul>
     */
    MOLDAVITA_SELVAGEM("Moldavita Selvagem", "Natural",
            "Seus ataques infligem Feridas Dolorosas, recebe imunidade a Venenos.",
            "Ignora o primeiro dano Elemental: Natural sofrido, você se torna imune a efeitos Naturais.",
            "Dano causado é Elemental: Natural em adição aos seus tipos e são Envenenados por 3 Rodadas (Veneno da "
                    + "Moldavita: 1 ponto de Dano Natural por Rodada e -1 Multiplicador de PV, não cumulativo, Duração 2 "
                    + "Rodadas)."),

    /**
     * Opala Purificadora (Sagrada).
     *
     * <p>Real: Protetivo "Danos profanos reduzidos em -3"; Violento "Dano causado é Sagrado" ({@link
     * #getOffensiveSanctity()}).
     *
     * <p>TODO:
     * <ul>
     *   <li>the 3PV start-of-Rodada regeneration — no per-Rodada item tick;</li>
     *   <li>Possessão and Doenças immunity;</li>
     *   <li>+2d6 against Mortos-Vivos and Abissais.</li>
     * </ul>
     */
    OPALA_PURIFICADORA("Opala Purificadora", "Sagrada",
            "Você recupera 3PV no início de cada Rodada e se torna imune a efeitos de Possessão.",
            "Danos profanos reduzidos em -3. Imunidade a Doenças.",
            "Dano causado é Sagrado substituição aos seus tipos e aumenta em +2d6 contra Mortos-Vivos e Abissais.",
            List.of(), List.of(new ItemBonus(ModifierType.PROFANE_DAMAGE_REDUCTION, 3)), List.of()),

    /**
     * Relâmpago Dourado (Temporal).
     *
     * <p>Real: Base Movimento +2UD; Violento +1PA.
     *
     * <p>TODO: the first movement's Defender o Perímetro exemption (no item exemption hook), and the Protetivo
     * moved-5UD +2PA (movement distance is not recorded).
     */
    RELAMPAGO_DOURADO("Relâmpago Dourado", "Temporal",
            "Movimento Base aumenta em +2UD, seu primeiro Movimento de cada Rodada não causa a Reação Defender o "
                    + "Perímetro nos 2UD iniciais.",
            "Após se mover por ao menos 5UD você recebe +2PA (não cumulativo).",
            "Bônus de +1PA.",
            List.of(new ItemBonus(ModifierType.MOVEMENT, 2)), List.of(),
            List.of(new ItemBonus(ModifierType.ACTION_POINTS, 1))),

    /**
     * Rútilo Subterrâneo (Elemental: Terra).
     *
     * <p>Real: Base Defesas +2, Vantagem on Atletismo, and ignoring Terreno Difícil.
     *
     * <p>TODO: damage to an attacking weapon (nothing deals item damage automatically), the first-instance RE, the
     * retyping, and +1d6 against objects.
     */
    RUTILO_SUBTERRANEO("Rútilo Subterrâneo", "Elemental: Terra",
            "Defesas +2, Vantagem nas Rolagens de Atletismo e seus movimentos ignoram Terreno Difícil.",
            "Armas inimigas corpo-a-corpo sofrem 2 pontos de danos. Ignora o primeiro dano Elemental: Terra sofrido, "
                    + "então concede RE: Terra.",
            "Danos causados são Elemental: Terra em substituição aos seus tipos e infligem +1d6 de danos contra objetos.",
            List.of(new ItemBonus(ModifierType.DEFESAS, 2),
                    new ItemBonus(ModifierType.ATLETISMO_ROLL_BONUS, Skill.ADVANTAGE_BONUS)),
            List.of(), List.of()) {
        @Override
        public boolean ignoresDifficultTerrain() {
            return true;
        }
    },

    /**
     * Sodalita Gélida (Elemental: Gelo).
     *
     * <p>Real: Base Força +1 and RD.
     *
     * <p>TODO: the first-instance RE; the Violento +1d6 (the dano roll's dice are the caller's — reported nowhere
     * yet) and the retyping.
     */
    SODALITA_GELIDA("Sodalita Gélida", "Elemental: Gelo",
            "Força +1 e RD.",
            "Ignora o primeiro dano Elemental: Gelo sofrido, então concede RE: Gelo.",
            "Danos causados são aumentados em +1d6 e são Elemental: Gelo em substituição aos seus tipos.",
            List.of(new ItemBonus(ModifierType.STRENGTH_BONUS, 1),
                    new ItemBonus(ModifierType.DAMAGE_REDUCTION, DamageService.DEFAULT_DAMAGE_REDUCTION)),
            List.of(), List.of()),

    /**
     * Sombra Solidificada (Umbral).
     *
     * <p>Real: Violento Dano Base +1 ({@link #resolveDamageBaseIncrease}).
     *
     * <p>TODO:
     * <ul>
     *   <li>"Item conta como um subordinado" — a Subordinado granted by an item has no carrier;</li>
     *   <li>the non-Umbral Meio-Dano — no Umbral damage scope;</li>
     *   <li>the Umbral retyping.</li>
     * </ul>
     */
    SOMBRA_SOLIDIFICADA("Sombra Solidificada", "Umbral",
            "Item conta como um subordinado (definido na criação).",
            "Danos não-Umbrais reduzidos à metade.",
            "Dano Base da Arma aumentado em +1, Danos causados são Umbrais em substituição aos seus tipos.") {
        @Override
        public int resolveDamageBaseIncrease(final Weapon weapon, final ItemType hostType) {
            return hostType == ItemType.OFFENSIVE ? 1 : 0;
        }
    },

    /**
     * Turmalina Obscura (Profana).
     *
     * <p>Real: Violento "Dano causado é Profano" ({@link #getOffensiveSanctity()}) and Oferenda Maldita as an
     * additional Efeito Crítico.
     *
     * <p>TODO:
     * <ul>
     *   <li>the Profano aura on the living;</li>
     *   <li>the Protetivo Roubo de Vida 2 — {@code LifeStealService} only amplifies;</li>
     *   <li>immunity to Maldições — there is no Maldição tag.</li>
     * </ul>
     */
    TURMALINA_OBSCURA("Turmalina Obscura", "Profana",
            "Personagens vivos em Área Circular Curta sofrem 1d6 pontos de Dano Profano a cada Rodada.",
            "Roubo de Vida 2 e imunidade a Maldições.",
            "Dano causado é Profano em substituição aos seus tipos e recebe Oferenda Maldita como Efeito Crítico adicional.");

    private final String name;
    private final String originLabel;
    private final String baseEffect;
    /** The Efeito Protetivo — an Armadura or Escudo host's. */
    private final String defensiveEffect;
    /** The Efeito Violento — an Arma host's. */
    private final String offensiveEffect;
    private final List<ItemBonus> baseBonuses;
    private final List<ItemBonus> defensiveBonuses;
    private final List<ItemBonus> offensiveBonuses;

    PowerStoneType(final String name, final String originLabel, final String baseEffect,
                   final String defensiveEffect, final String offensiveEffect) {
        this(name, originLabel, baseEffect, defensiveEffect, offensiveEffect,
                List.of(), List.of(), List.of());
    }

    PowerStoneType(final String name, final String originLabel, final String baseEffect,
                   final String defensiveEffect, final String offensiveEffect,
                   final List<ItemBonus> baseBonuses, final List<ItemBonus> defensiveBonuses,
                   final List<ItemBonus> offensiveBonuses) {
        this.name = name;
        this.originLabel = originLabel;
        this.baseEffect = baseEffect;
        this.defensiveEffect = defensiveEffect;
        this.offensiveEffect = offensiveEffect;
        this.baseBonuses = List.copyOf(baseBonuses);
        this.defensiveBonuses = List.copyOf(defensiveBonuses);
        this.offensiveBonuses = List.copyOf(offensiveBonuses);
    }

    /**
     * The nature a bound stone gives its host weapon's damage in its Violento: "Dano causado é Sagrado/Profano em
     * substituição aos seus tipos". Opala Purificadora gives {@code SAGRADO}, Turmalina Obscura {@code PROFANO}, every
     * other stone {@code null}. Read by {@code AttackDelivery} for an attack with that weapon.
     */
    public org.aventyrs.core.character.DamageSanctity getOffensiveSanctity() {
        return switch (this) {
            case OPALA_PURIFICADORA -> org.aventyrs.core.character.DamageSanctity.SAGRADO;
            case TURMALINA_OBSCURA -> org.aventyrs.core.character.DamageSanctity.PROFANO;
            default -> null;
        };
    }

    /**
     * The Efeito Crítico a bound stone's Violento adds to attacks with its host weapon: Citrino Incandescente's
     * Inflamar or Turmalina Obscura's Oferenda Maldita. {@code null} for every other stone. Read by {@code
     * AttackDelivery}.
     */
    public CriticalEffectType getViolentCriticalEffect() {
        return switch (this) {
            case CITRINO_INCANDESCENTE -> CriticalEffectType.INFLAMAR;
            case TURMALINA_OBSCURA -> CriticalEffectType.OFERENDA_MALDITA;
            default -> null;
        };
    }

    /**
     * The Corrente de Efeitos a bound stone's Violento puts on an attack with its host weapon, built fresh for that
     * attack. Hematita do Vendaval's is a Rugido; every other stone returns {@code null}.
     *
     * @param critical whether the attack was an Acerto Crítico (a Rugido's +2)
     */
    public Effect createViolentEffectChain(final boolean critical) {
        return null;
    }

    /**
     * Whether this stone's Violento "pode ser anexo apenas à Artefatos de Conjuração". Such a stone binds to no other
     * weapon. Only {@link #ESFERA_DE_AETHER}'s is.
     */
    public boolean isViolentRestrictedToCastingArtifacts() {
        return false;
    }

    /**
     * How much of modifierType this stone grants while bound to a host of hostType. It always grants its Efeito Base,
     * and adds that host's mode (Protetivo for {@link ItemType#DEFENSIVE}, Violento for {@link ItemType#OFFENSIVE}).
     * Additive, the same convention every other bonus source in this core follows.
     */
    public int resolveBonus(final ModifierType modifierType, final ItemType hostType) {
        return resolveBonus(modifierType, hostType, true);
    }

    /**
     * {@link #resolveBonus(ModifierType, ItemType)}, with the Violento scoped to attacks made <em>with</em> the host.
     * hostWielded is whether the attack being judged uses the host weapon. When it is false, the Violento contributes
     * nothing while the Base still applies. That is how "Margem Crítica Menor aumentada" improves the host weapon's
     * swing and no other.
     */
    public int resolveBonus(final ModifierType modifierType, final ItemType hostType, final boolean hostWielded) {
        int total = sumMatching(baseBonuses, modifierType);
        if (hostType == ItemType.DEFENSIVE) {
            total += sumMatching(defensiveBonuses, modifierType);
        } else if (hostType == ItemType.OFFENSIVE && hostWielded) {
            total += sumMatching(offensiveBonuses, modifierType);
        }
        return total;
    }

    /** "Margem Crítica … Maior aumentada" for an attack with the host weapon. 0 for every stone but Mitral Puro. */
    public int resolveMajorCriticalMarginIncrease(final ItemType hostType, final boolean hostWielded) {
        return 0;
    }

    /** "Dano Crítico +1d6" for a critical hit with the host weapon. {@code NONE} for every stone but Calcita. */
    public CriticalDamage resolveCriticalDamage(final ItemType hostType, final boolean hostWielded) {
        return CriticalDamage.NONE;
    }

    /**
     * Dano Base scale-ups this stone grants when its host is a weapon. 0 for every stone but
     * {@link #SOMBRA_SOLIDIFICADA}, whose Efeito Violento includes "Dano Base da Arma aumentado em +1".
     */
    public int resolveDamageBaseIncrease(final Weapon weapon, final ItemType hostType) {
        return 0;
    }

    /**
     * Whether this stone's Efeito Base lets its bearer ignore Terreno Difícil, whatever the host.
     * {@code false} for every stone but {@link #RUTILO_SUBTERRANEO}.
     */
    public boolean ignoresDifficultTerrain() {
        return false;
    }

    private static int sumMatching(final List<ItemBonus> bonuses, final ModifierType modifierType) {
        return bonuses.stream()
                .filter(bonus -> bonus.modifierType() == modifierType)
                .mapToInt(ItemBonus::value)
                .sum();
    }
}
