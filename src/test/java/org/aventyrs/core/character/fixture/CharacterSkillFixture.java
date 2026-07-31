package org.aventyrs.core.character.fixture;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.skill.Artes;
import org.aventyrs.core.skill.AtaqueADistancia;
import org.aventyrs.core.skill.AtaqueCorpoACorpo;
import org.aventyrs.core.skill.Atletismo;
import org.aventyrs.core.skill.Attention;
import org.aventyrs.core.skill.DirigirECavalgar;
import org.aventyrs.core.skill.DominioDoMana;
import org.aventyrs.core.skill.EmpatiaSelvagem;
import org.aventyrs.core.skill.EsquivaEAparar;
import org.aventyrs.core.skill.Furtividade;
import org.aventyrs.core.skill.MedicinaECura;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.util.SimpleFixture;

public class CharacterSkillFixture extends SimpleFixture {

    public static final String ATTENTION_1 = "Attention1";
    public static final String ARTES_1 = "Artes1";
    public static final String ATLETISMO_1 = "Atletismo1";
    public static final String DIRIGIR_E_CAVALGAR_1 = "DirigirECavalgar1";
    public static final String DOMINIO_DO_MANA_1 = "DominioDoMana1";
    public static final String ATAQUE_A_DISTANCIA_1 = "AtaqueADistancia1";
    public static final String ATAQUE_CORPO_A_CORPO_1 = "AtaqueCorpoACorpo1";
    public static final String ESQUIVA_E_APARAR_1 = "EsquivaEAparar1";
    public static final String EMPATIA_SELVAGEM_1 = "EmpatiaSelvagem1";
    public static final String FURTIVIDADE_1 = "Furtividade1";
    public static final String MEDICINA_E_CURA_1 = "MedicinaECura1";

    public static void loadTemplates() {
        loadBasicSkillTemplates();
    }

    private static void loadBasicSkillTemplates() {
        Fixture.of(CharacterSkill.class).addTemplate(ATTENTION_1, new Rule() {
            {
                this.add("skill", new Attention());
            }
        });
        Fixture.of(CharacterSkill.class).addTemplate(ARTES_1, new Rule() {
            {
                this.add("skill", new Artes());
            }
        });
        Fixture.of(CharacterSkill.class).addTemplate(ATLETISMO_1, new Rule() {
            {
                this.add("skill", new Atletismo());
            }
        });
        Fixture.of(CharacterSkill.class).addTemplate(DIRIGIR_E_CAVALGAR_1, new Rule() {
            {
                this.add("skill", new DirigirECavalgar());
            }
        });
        Fixture.of(CharacterSkill.class).addTemplate(DOMINIO_DO_MANA_1, new Rule() {
            {
                this.add("skill", new DominioDoMana());
            }
        });
        Fixture.of(CharacterSkill.class).addTemplate(ATAQUE_A_DISTANCIA_1, new Rule() {
            {
                this.add("skill", new AtaqueADistancia());
            }
        });
        Fixture.of(CharacterSkill.class).addTemplate(ATAQUE_CORPO_A_CORPO_1, new Rule() {
            {
                this.add("skill", new AtaqueCorpoACorpo());
            }
        });
        Fixture.of(CharacterSkill.class).addTemplate(ESQUIVA_E_APARAR_1, new Rule() {
            {
                this.add("skill", new EsquivaEAparar());
            }
        });
        Fixture.of(CharacterSkill.class).addTemplate(EMPATIA_SELVAGEM_1, new Rule() {
            {
                this.add("skill", new EmpatiaSelvagem());
            }
        });
        Fixture.of(CharacterSkill.class).addTemplate(FURTIVIDADE_1, new Rule() {
            {
                this.add("skill", new Furtividade());
            }
        });
        Fixture.of(CharacterSkill.class).addTemplate(MEDICINA_E_CURA_1, new Rule() {
            {
                this.add("skill", new MedicinaECura());
            }
        });
    }

    public static CharacterSkill.CharacterSkillBuilder blank(final String templateName) {
        return ((CharacterSkill)Fixture.from(CharacterSkill.class).gimme(templateName)).toBuilder().graduation(SkillGraduation.INITIAL_BUILDER.build());
    }
}
