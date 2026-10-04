package org.aventyrs.core.skill.furtividade;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The Habilidades de Competência available to characters trained in Furtividade. {@link
 * #LADINO_TEORICO} is real — its Atributo Base substitution rides the mechanism {@link
 * SkillCompetencyAbility#getSubstituteAttributeDomain()} provides. Each of the rest needs a
 * system this core doesn't have yet (a specialization-scoped action gate, a GD-*increase*
 * expression, an observation-state flag, or weapon/trap damage); see each constant's TODO.
 *
 * <p><b>Hiding itself is modelled</b> — {@code
 * org.aventyrs.core.character.services.HidingService} turns a Furtividade total into {@code
 * ConditionType#ESCONDIDO}, resolves each observer's Atenção against it and gives the hider away
 * when they attack, move or roll against someone. What still blocks the two constants gated on
 * <i>being</i> hidden is narrower: no {@code SkillCompetencyAbility} hook receives the {@code
 * CombatantSheet} a Condição is held on, so a Habilidade cannot read its own holder's state.
 */
@Getter
@AllArgsConstructor
public enum FurtividadeCompetencyAbility implements SkillCompetencyAbility {

    // Real (core 0.0.103): skill.CompetencyActions#esconderOutros turns a Furtividade total under
    // Maestria da Ocultação or Infiltrador into the ally's Hidden, one nível below the tier the roll
    // reached as an Especialista ("a GD aumenta em +1 Nível", table ruling); the effect half applies
    // it on the ally's own sheet. ⚠️ "adjacente" is the caller's — this core holds no positions.
    ESCONDER_OUTROS("Você pode efetuar uma rolagem de Furtividade, nas Especializações " +
            "Maestria da Ocultação e Infiltrador, em um aliado adjacente, a GD para esta " +
            "ação aumenta em +1 Nível."),

    /**
     * Real (core 0.0.100): Vantagem on every Perícia roll its holder makes while {@link
     * ConditionType#ESCONDIDO} in a Combat Scene — read off the holder's own sheet, which the longest
     * {@link SkillCompetencyAbility#resolveConditionalRollBonus} now carries.
     */
    ACAO_SURPRESA("Em Cenas de Combate você recebe Vantagem em suas Rolagens de Perícia " +
            "enquanto estiver Furtivo.") {
        @Override
        public Optional<Integer> resolveConditionalRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                                             final SkillTrait requestedAbility, final CombatantSheet holder) {
            return sceneContext != null && sceneContext.isCombatScene() && hidden(holder, sceneContext)
                    ? Optional.of(Skill.ADVANTAGE_BONUS) : Optional.empty();
        }
    },

    // TODO: lifts the "can't hide while observed" restriction described in Furtividade's own
    // rules text, at +1 GD. The restriction still isn't modeled: HidingService#isHiddenFrom
    // answers who is fooled by an *existing* concealment, which is not the same question as
    // whether anyone currently has eyes on you — HidingService#hide accepts any roll and the
    // Narrador adjudicates, so there is nothing here to be exempt from. This is also a GD
    // *increase*, which getDifficultyReduction() can't express.
    AGORA_ESTOU_AGORA_NAO_ESTOU("Você pode fazer rolagens de Furtividade mesmo quando " +
            "observado (GD aumentado em +1 nível)."),

    /**
     * Real for attacks (core 0.0.100): +2 dano while {@link ConditionType#ESCONDIDO}, +3 from the 5ª and +4
     * from the 10ª Graduação in Furtividade, on every Perícia de Ataque. Traps have no representation —
     * that half is the Narrador's.
     */
    MORTE_OCULTA("Suas armadilhas e seu ataques enquanto escondido causam +2 pontos de " +
            "danos adicionais, este benefício aumenta em +1 na 5ª e 10ª graduação.") {
        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor,
                                                         final AttackSource attackSource, final CombatantSheet holder) {
            if (attackingSkillType == null || !attackingSkillType.isAttackSkill() || !hidden(holder, sceneContext)) {
                return Optional.empty();
            }
            int graduation = holder.getCharacter().getEffectiveGraduation(SkillType.FURTIVIDADE);
            int bonus = MORTE_OCULTA_BASE + (graduation >= 5 ? 1 : 0) + (graduation >= 10 ? 1 : 0);
            return Optional.of(new DamageBonus(bonus, DamageType.FISICO));
        }
    },

    // Substitutes Gnose for Destreza — see SkillCompetencyAbility.getSubstituteAttributeDomain().
    LADINO_TEORICO("Você pode substituir o Atributo Base desta perícia por Gnose.") {
        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.GNOSE);
        }
    };

    /** Morte Oculta's "+2 pontos de danos adicionais", before the 5ª/10ª Graduação steps. */
    private static final int MORTE_OCULTA_BASE = 2;

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.FURTIVIDADE;
    }

    /** Whether holder is Escondido right now — {@code false} with no sheet to read. */
    private static boolean hidden(final CombatantSheet holder, final SceneContext sceneContext) {
        return holder != null && holder.hasCondition(ConditionType.ESCONDIDO, sceneContext);
    }
}
