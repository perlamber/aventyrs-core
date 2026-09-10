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

    // The rest-recovery branch ("Descansos Verdadeiros Longos ou superiores... +2PM") is
    // real, via resolveRestMagicPointsBonus below — treating "Descanso Verdadeiro" as this
    // codebase's own RestType tiers themselves (RestServiceImpl.applyRest already always
    // applies a Rest's recovery unconditionally/completely — there's no separate "attempted
    // but interrupted" Rest concept for a "Verdadeiro" qualifier to distinguish from), an
    // inference, not confirmed rules text, same as ArtesInteraction's own UNIMAGINABLE gap.
    // TODO: the other two branches are still unbuilt. "+1PM on spell/Título Aventyr PV
    // healing" needs a way to know a given CombatantSheet#heal call came from a spell or
    // Título Aventyr ability specifically (no healing-spell-effect representation exists —
    // SpellCastingService only resolves the delivery + Domínio do Mana rolls, not a healing
    // effect; no Título Aventyr concept exists at all). "+1 non-cumulative Roubo de Mana"
    // needs a Roubo de Mana effect to exist in the first place — only the unrelated opposite
    // (ManaDrain/Purga-Mana, which drains a target's PM without transferring it to anyone)
    // exists today.
    CANALIZADOR_DE_MANA("Descansos Verdadeiros Longos ou superiores permitem que você recupere +2PM adicionais. " +
            "Magias e Habilidades de Títulos Aventyr que te façam recuperar PV recuperam +1PM adicional (não " +
            "aplicável a efeitos de Roubo de Mana). Caso possua um ou mais efeitos de Roubo de Mana, seu Roubo " +
            "de Mana total aumenta em +1 (não cumulativo).") {
        @Override
        public int resolveRestMagicPointsBonus(final RestType restType) {
            return restType.isAtLeast(RestType.LONGO) ? 2 : 0;
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
