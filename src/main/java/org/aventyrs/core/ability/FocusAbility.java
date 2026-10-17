package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum FocusAbility implements AttributeAbility {

    CONCENTRACAO_PROFUNDA("Você pode gastar 1PA e 3PM para entrar em estado de Concentração Profunda por 2 " +
            "Rodadas. Enquanto Concentração Profunda estiver ativa você pode adicionar metade do seu valor de " +
            "Foco às suas rolagens de Perícias.") {
        @Override
        public Optional<ActiveAbility> resolveActiveAbility() {
            return Optional.of(new ConcentracaoProfundaActiveAbility());
        }
    },

    CONEXAO_COM_O_MANA("Seu multiplicador de Pontos de Magia é aumentado em +1.") {
        @Modifier(ModifierType.MANA_MULTIPLIER)
        public int manaMultiplierBonus() {
            return 1;
        }
    },

    // All three branches are real. The rest branch via resolveRestMagicPointsBonus — a real Descanso
    // only (RecoveryOrigin#DESCANSO; "Verdadeiro" read as this core's RestType tiers, an inference).
    // The +1PM via resolveHealingReceivedManaBonus, paid by AbstractCombatantSheet#heal once a Magia's
    // or Título's heal actually restored PV. The +1 Roubo de Mana via resolveManaStealBonus, added
    // once by AttackDelivery to a hit that already steals Mana (a weapon's Benção Elduriana) —
    // "amplify, never grant", like Roubo de Vida.
    CANALIZADOR_DE_MANA("Descansos Verdadeiros Longos ou superiores permitem que você recupere +2PM adicionais. " +
            "Magias e Habilidades de Títulos Aventyr que te façam recuperar PV recuperam +1PM adicional (não " +
            "aplicável a efeitos de Roubo de Mana). Caso possua um ou mais efeitos de Roubo de Mana, seu Roubo " +
            "de Mana total aumenta em +1 (não cumulativo).") {
        @Override
        public int resolveRestMagicPointsBonus(final RestType restType) {
            return restType.isAtLeast(RestType.LONGO) ? 2 : 0;
        }

        @Override
        public int resolveHealingReceivedManaBonus(final org.aventyrs.core.sheet.HealingSource source) {
            return source != null && source.isMagiaOrTituloAbility() ? 1 : 0;
        }

        @Override
        public int resolveManaStealBonus() {
            return 1;
        }
    },

    // Real for a Magia whose Efeito is authored as a SpellDamage (Ira de Vulcano, part of
    // Piromancia): SpellCastingService#resolvePrimaryDamage upgrades that Magia's "Metade do
    // Foco" term to full Foco when the caster has cast no Magia yet this Rodada — via
    // AttributeAbility#upgradesFirstSpellOfRoundFocusScaling below. Still prose on a Magia whose
    // numeric effect isn't a plain damage-to-the-target figure (healing, a delayed/positional
    // wave, distance falloff) — those have no SpellDamage yet.
    MAGIA_PODEROSA("Algumas de suas magias são mais poderosas que o normal. A cada Rodada, quando conjurar sua " +
            "primeira magia, você pode adicionar seu valor integral de Foco, ao invés da metade, aos seus " +
            "efeitos.") {
        @Override
        public boolean upgradesFirstSpellOfRoundFocusScaling() {
            return true;
        }
    },

    // Real, via org.aventyrs.core.ability.MagiaAlternativaAbility — one constant per MagicType,
    // so the chosen Tipo de Magia is which constant a character holds (the same shape
    // GnoseAbility.PERITO_TEORICO uses). Grant that constant, not this one; this stays the
    // catalog/rules-text entry. Holding it exempts its holder from Spell#isEligible's branch
    // gate on every Árvore de Magia of that type. Note the rules text names Temporal and Umbral,
    // which MagicType lacks, and omits the NATURAL it has — see MagiaAlternativaAbility's javadoc.
    MAGIA_ALTERNATIVA("Você pode adquirir magias de ramos adicionais de algumas de suas Árvores de Magia. " +
            "Escolha um Tipo de Magia entre Divina, Elemental, Encantamento, Invocação, Temporal, Primordial, " +
            "Profana ou Umbral: você pode aprender magias de ambas as ramificações dos tipos de magia escolhidos.");

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.FOCUS;
    }
}
