package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Habilidades de Competência available to characters trained in Furtividade. Every one
 * of these needs a system this core doesn't have yet (a specialization-scoped action gate, a
 * GD-*increase* expression, a stealth/observation-state flag, cross-skill Vantagem,
 * weapon/trap damage, or attribute substitution) so none are expressible for real today; see
 * each constant's TODO.
 */
@Getter
@AllArgsConstructor
public enum FurtividadeCompetencyAbility implements SkillCompetencyAbility {

    // TODO: lets the character roll Furtividade (Maestria da Ocultação/Infiltrador
    // specializations only) to hide an adjacent ally, at +1 GD — "an adjacent ally" is now
    // checkable for real (org.aventyrs.core.scene.SceneContext#hasAllyWithin(Range.ADJACENTE)),
    // but this still needs a specialization-scoped action gate, and this is a GD *increase*,
    // which SkillCompetencyAbility.getDifficultyReduction() has no way to express (it only
    // ever subtracts) — neither of which exist yet.
    ESCONDER_OUTROS("Você pode efetuar uma rolagem de Furtividade, nas Especializações " +
            "Maestria da Ocultação e Infiltrador, em um aliado adjacente, a GD para esta " +
            "ação aumenta em +1 Nível."),

    // TODO: Vantagem on every Perícia roll (not just Furtividade's own) while "Furtivo",
    // scoped to Cenas de Combate — same structural gap as AtletismoExcellency.LENDA: it
    // would need to apply across every <Skill>Interaction, not just
    // FurtividadeInteraction, plus there's no "is the character currently Furtivo" state or
    // Cena-de-Combate-state tracked anywhere to gate it on (same Cena-de-Combate gap as
    // AttentionCompetencyAbility.ARDIL_DE_MARPLE).
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
    // DominioDoManaCompetencyAbility.LETALIDADE_ARCANA) — no weapon/trap damage system
    // exists yet.
    MORTE_OCULTA("Suas armadilhas e seu ataques enquanto escondido causam +2 pontos de " +
            "danos adicionais, este benefício aumenta em +1 na 5ª e 10ª graduação."),

    // TODO: lets this Perícia use Gnose instead of its normal base Attribute (Destreza) — the
    // substitution mechanism itself now exists (see SkillCompetencyAbility
    // .getSubstituteAttributeDomain() / AtaqueCorpoACorpoCompetencyAbility.ACUIDADE), this
    // constant just doesn't override it yet, and FurtividadeInteraction doesn't yet
    // resolve/pass it into CharacterSkillService.getValueForRoll's substituteAttributeDomain
    // overload (same remaining wiring gap as EmpatiaSelvagemCompetencyAbility
    // .ACADEMICO_SELVAGEM/INSTINTO_ANIMAL).
    LADINO_TEORICO("Você pode substituir o Atributo Base desta perícia por Gnose.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.FURTIVIDADE;
    }
}
