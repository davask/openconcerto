/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.checks.ValidChangeSupport;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;

import interpreterDJava.JavaEditor;
import koala.dynamicjava.interpreter.Interpreter;
import koala.dynamicjava.interpreter.InterpreterException;
import koala.dynamicjava.interpreter.TreeInterpreter;
import koala.dynamicjava.parser.wrapper.JavaCCParserFactory;

public class SQLJavaEditor extends JavaEditor implements ValueWrapper<String> {

    private final List<Object> mVar;
    private Map<String, SQLField> mapField;
    private Map<String, SQLRow> mapRow;

    private int salarieID;

    public SQLJavaEditor(Map<String, List<?>> m) {
        super();

        this.salarieID = 1;

        this.mVar = new ArrayList<Object>();
        this.mapField = new HashMap<String, SQLField>();
        this.mapRow = new HashMap<String, SQLRow>();

        // On stocke les variables par String.length
        for (String s : m.keySet()) {
            List<?> element = m.get(s);

            for (Iterator<?> iterator = element.iterator(); iterator.hasNext();) {

                Object o = iterator.next();
                // System.out.println(o.getClass());

                if (o instanceof SQLRow) {
                    SQLRow element2 = (SQLRow) o;
                    String name = element2.getString("NOM").trim();
                    // System.err.println("Ajout de la row " + name);
                    this.addNewLitteral(name);
                    this.mVar.add(o);
                    this.mapRow.put(name, element2);
                } else if (o instanceof SQLField) {
                    final SQLField field2 = ((SQLField) o);
                    String name = field2.getTable().getName();
                    // System.err.println("Ajout du field " + name);
                    this.addNewLitteral(name);
                    this.mVar.add(o);
                    this.mapField.put(name, field2);
                }
            }
        }

        this.supp = new PropertyChangeSupport(this);
        this.validSupp = new ValidChangeSupport(this);

        this.textFormule.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void update(DocumentEvent e) {
                checkFormule(SQLJavaEditor.this.textFormule.getText().trim(), SQLJavaEditor.this.varAssign);
                SQLJavaEditor.this.supp.firePropertyChange("value", null, null);
            }
        });

    }

    /***********************************************************************************************
     * SQLObject
     **********************************************************************************************/
    private final PropertyChangeSupport supp;
    private final ValidChangeSupport validSupp;

    @Override
    public void setValue(String val) {
        this.setText(val);
    }

    @Override
    public void resetValue() {
        this.setValue("");
    }

    @Override
    public String getValue() {
        return this.getText();
    }

    public void addValueListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    @Override
    public void rmValueListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

    @Override
    public ValidState getValidState() {
        return ValidState.createCached(this.isCodeValid(), "la formule n'est pas valide");
    }

    public void addValidListener(ValidListener l) {
        this.validSupp.addValidListener(l);
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.validSupp.removeValidListener(l);
    }

    public JComponent getComp() {
        return this;
    }

    /**
     * Vérifie que la formule est correcte et renvoie sa valeur.
     * 
     * @param formule formule à tester
     * @param varCallName variable qui recoit la formule
     * @return la valeur de retour de la formule
     */
    public Object checkFormule(String formule, String varCallName) {
        Map<String, SQLRow> mapCacheRow = new HashMap<String, SQLRow>();
        try {

            // Si la formule est vide --> OK
            if (formule.trim().length() == 0) {
                this.status.setText("Code correct");
                this.setCodeValid(true);
                return null;
            }

            Interpreter interpreter = new TreeInterpreter(new JavaCCParserFactory());

            // Exportation des variables vers l'interpreteur
            SQLTable tableSal = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("SALARIE");

            // SQLRow du salarié sur lequel on se base pour calculer la formule
            SQLRow rowSal = tableSal.getRow(this.salarieID);

            final StringWriter bW = new StringWriter(1024);
            Set<SQLRow> set = rowSal.getForeignRows();
            for (SQLRow row : set) {
                mapCacheRow.put(row.getTable().getName(), row);
                Set<SQLRow> set2 = row.getForeignRows();
                for (SQLRow row2 : set2) {
                    mapCacheRow.put(row2.getTable().getName(), row2);
                }
            }
            // System.err.println("LOAD Variable");
            for (final Object o : this.mVar) {
                // Les SQLFields sont des fields des tables étrangères de la table salarié
                if (o instanceof SQLField) {

                    SQLField field = (SQLField) o;

                    if (formule.indexOf(field.getName()) >= 0) {
                        SQLRow rowAssoc = mapCacheRow.get(field.getTable().getName());

                        // on recupere la row associee exemple : SALARIE.INFOS_SALARIE_PAYE
                        Set<SQLRow> foreignRows = null;
                        if (rowAssoc == null) {
                            foreignRows = rowSal.getForeignRows(field.getTable().getName());
                        }

                        if (rowAssoc != null || ((rowSal != null) && (foreignRows != null))) {
                            if (rowAssoc == null) {
                                Set<SQLRow> rowList = foreignRows;

                                if (rowList.size() != 0) {
                                    Iterator<SQLRow> iterList = rowList.iterator();
                                    rowAssoc = iterList.next();
                                    mapCacheRow.put(rowAssoc.getTable().getName(), rowAssoc);
                                }
                            }
                            if (rowAssoc != null) {
                                defineVariable(interpreter, bW, field.getName(), rowAssoc.getObject(field.getName()));
                            } else {
                                defineVariable(interpreter, bW, field.getName(), null);
                            }
                        } else {
                            defineVariable(interpreter, bW, field.getName(), null);
                        }
                    }
                } else {

                    // FIXME Recursivite
                    // Variables de paye deja definie
                    if (o instanceof SQLRow) {

                        SQLRow rowTmp = (SQLRow) o;

                        if (formule.indexOf(rowTmp.getString("NOM")) >= 0) {
                            if (rowTmp.getString("FORMULE").trim().length() == 0) {
                                defineVariable(interpreter, bW, rowTmp.getString("NOM"), rowTmp.getObject("VALEUR"));
                            } else {
                                if (!rowTmp.getString("NOM").equalsIgnoreCase(varCallName)) {
                                    Object ob = checkFormule(rowTmp.getString("FORMULE"), rowTmp.getString("NOM"));
                                    if (ob != null) {
                                        defineVariable(interpreter, bW, rowTmp.getString("NOM"), ob);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            bW.write(formule);
            bW.flush();
            bW.close();

            // Interpret the script
            final Reader bR = new StringReader(bW.getBuffer().toString());

            Object interpreterResult = interpreter.interpret(bR, "memory");
            bR.close();
            try {
                // this.status.setText(interpreter.getVariableNames().toString());
                this.status.setText("Code correct, valeur de retour = " + interpreter.getVariable(varCallName).toString());
                this.setCodeValid(true);
                return interpreter.getVariable(varCallName);
            } catch (IllegalStateException iSE) {
                if (interpreterResult != null) {
                    // this.status.setText(interpreter.getVariableNames().toString());
                    this.status.setText("Code correct, valeur de retour = " + interpreterResult.toString());
                    this.setCodeValid(true);
                    bR.close();
                    return interpreterResult;
                } else {
                    // this.status.setText(interpreter.getVariableNames().toString());
                    this.status.setText("Aucune valeur de retour");
                    this.setCodeValid(false);
                    return null;
                }
            }
        } catch (Exception e) {
            if (e instanceof InterpreterException) {
                String m = "";

                InterpreterException ex = (InterpreterException) e;
                System.out.println(ex.getMessage());
                if (ex.getSourceInformation() != null) {
                    m += " ligne:" + (ex.getSourceInformation().getLine());
                }
                m += ex.getMessage();
                int in = m.indexOf('\n');
                if (in > 0) {
                    m = m.substring(0, in);
                }
                this.setCodeValid(false);
                this.status.setText(m);

            } else {
                this.setCodeValid(false);
                System.err.println("err-----");
                e.printStackTrace();
            }
        }
        // Highlight the occurrences of the word "public"
        // highlight(textFormule, "int");

        // Creates highlights around all occurrences of pattern in textComp
        this.setCodeValid(false);
        return null;
    }

    @Override
    protected void setCodeValid(boolean codeValid) {
        super.setCodeValid(codeValid);
        this.validSupp.fireValidChange(this.getValidState());
    }

    public void setSalarieID(int id) {
        // System.err.println("------>> SELECTION_COMBO_CHANGE");
        this.salarieID = id;
        checkFormule(this.textFormule.getText().trim(), this.varAssign);
    }
}
