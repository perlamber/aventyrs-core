package org.aventyrs.core.skill.furtividade;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The Habilidades de Competência available to characters trained in Furtividade. {@link
 * #LADINO_TEORICO} is real — its Atributo Base substitution rides the mechanism {@link
 * SkillCompetencyAbility#getSubstituteAttributeDomain()} provides. Each of the rest needs a
 * system this core doesn't have yet (a specialization-scoped action gate, a GD-*increase*
 * expression, an observation-state flag, or weapon/trap damage); see each constant's TODO. The
 * hidden state itself now exists — {@code ConditionType#ESCONDIDO} — but no {@code
 * SkillCompetencyAbility} hook receives the {@code CombatantSheet} a Condição is held on, so the
 * two constants that read it still cannot.
 */
@Getter
@AllArgsConstructor
public enum FurtividadeCompetencyAbility implements SkillCompetencyAbility {

    // TODO: lets the character roll Furtividade (Maestria da Ocultação/Infiltrador
    // specializations only) to hide an adjacent ally, at +1 GD — "an adjacent ally" is now
    // checkable for real (org.aventyrs.core.scene.SceneContext#hasAllyWithin(Range.ADJACENTE)),
    // and a character now genuinely holds typed CharacterSkill.getSpecializations() that could
    // in principle name Maestria da Ocultação/Infiltrador — but gating *this ability's own*
    // bonus on which specialization the caller requested for a given roll still isn't
    // expressible: SkillRoll#getRequestedAbility() only feeds AbstractSkillInteraction's
    // generic possession check, not a per-ability @Modifier method (same reflection limitation
    // resolveDamageBonus/resolveAttackRollBonus were built to work around, by taking the
    // relevant data as an explicit parameter instead — no such explicit-parameter resolve
    // method exists for "which specialization was this roll requested with" yet). This is also
    // a GD *increase*, which SkillCompetencyAbility.getDifficultyReduction() has no way to
    // express (it only ever subtracts) — unaffected by this specialization change.
    ESCONDER_OUTROS("Você pode efetuar uma rolagem de Furtividade, nas Especializações " +
            "Maestria da Ocultação e Infiltrador, em um aliado adjacente, a GD para esta " +
            "ação aumenta em +1 Nível."),

    // TODO: the "Furtivo" state is now ConditionType.ESCONDIDO and "Cenas de Combate" is
    // SceneContext#isCombatScene(), but a held trait cannot see its holder's Condições —
    // resolveConditionalRollBonus takes a SceneContext and a SkillTrait, and a Condition lives on
    // the CombatantSheet, which no SkillCompetencyAbility hook receives. Applying across every
    // <Skill>Interaction is no longer part of the gap: the hook is summed generically for
    // whichever Perícia is being rolled.
    ACAO_SURPRESA("Em Cenas de Combate você recebe Vantagem em suas Rolagens de Perícia " +
            "enquanto estiver Furtivo."),

    // TODO: lifts the "can't hide while observed" restriction described in Furtividade's own
    // rules text, at +1 GD — that base restriction itself isn't modeled anywhere (no
    // visibility/observation-state system exists), and this too is a GD *increase*, which
    // getDifficultyReduction() can't express.
    AGORA_ESTOU_AGORA_NAO_ESTOU("Você pode fazer rolagens de Furtividade mesmo quando " +
            "observado (GD aumentado em +1 nível)."),

    // TODO: +2 damage on traps/attacks made while hidden, then +1 more at the 5th and 10th
    // graduation (a graduation-tiered scaling bonus, not a flat one — same shape as
    // DominioDoManaCompetencyAbility.LETALIDADE_ARCANA). "Enquanto escondido" is now
    // ConditionType.ESCONDIDO, but a held trait cannot see its holder's Condições —
    // resolveDamageBonus takes the attackTarget's sheet and the acting Character, never the
    // actor's own sheet, which is where a Condition lives. Traps have no representation either.
    MORTE_OCULTA("Suas armadilhas e seu ataques enquanto escondido causam +2 pontos de " +
            "danos adicionais, este benefício aumenta em +1 na 5ª e 10ª graduação."),

    // Substitutes Gnose for Destreza — see SkillCompetencyAbility.getSubstituteAttributeDomain().
    LADINO_TEORICO("Você pode substituir o Atributo Base desta perícia por Gnose.") {
        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.GNOSE);
        }
    };

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.FURTIVIDADE;
    }
}
