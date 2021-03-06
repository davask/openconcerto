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
 
 package org.openconcerto.utils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * Useful for defining product wide values, like version, from a property file.
 * 
 * @author Sylvain
 */
public class ProductInfo {

    public static final String PROPERTIES_NAME = "/product.properties";
    public static final String ORGANIZATION_NAME = "ORGANIZATION_NAME";
    public static final String ORGANIZATION_ID = "ORGANIZATION_ID";
    public static final String NAME = "NAME";
    public static final String ID = "ID";
    public static final String VERSION = "VERSION";

    private static ProductInfo INSTANCE;

    /**
     * If {@link #setInstance(ProductInfo)} was called with a non-null value, return that ;
     * otherwise use {@link #createDefault()}.
     * 
     * @return the current instance, can be <code>null</code>.
     */
    public synchronized static final ProductInfo getInstance() {
        if (INSTANCE == null) {
            try {
                setInstance(createDefault());
            } catch (IOException e) {
                throw new IllegalStateException("unable to load default product properties", e);
            }
        }
        return INSTANCE;
    }

    /**
     * Set the current instance.
     * 
     * @param i the new instance, can be <code>null</code>.
     */
    public synchronized static void setInstance(ProductInfo i) {
        INSTANCE = i;
    }

    /**
     * Create a product info from the default properties file, {@value #PROPERTIES_NAME}.
     * 
     * @return the default properties, or <code>null</code> if they couldn't be found.
     * @throws IOException if properties couldn't be loaded.
     */
    public static final ProductInfo createDefault() throws IOException {
        final Properties p = PropertiesUtils.createFromResource(ProductInfo.class, PROPERTIES_NAME);
        return p == null ? null : new ProductInfo(p);
    }

    static private final String sanitizeDomain(final String s) {
        return FileUtils.sanitize(s).replace(' ', '_');
    }

    static private final String sanitizeDomainPart(final String s) {
        return sanitizeDomain(s).replace('.', '_');
    }

    private final Properties props;

    public ProductInfo(final String name) {
        this(name, "ILM Informatique", "fr.ilm-informatique");
    }

    public ProductInfo(final String name, final String orgName, final String orgID) {
        this(CollectionUtils.createMap(NAME, name, ORGANIZATION_NAME, orgName, ORGANIZATION_ID, orgID));
    }

    public ProductInfo(final Map<String, String> map) {
        this(PropertiesUtils.createFromMap(map));
    }

    public ProductInfo(final Properties props) {
        if (props == null)
            throw new NullPointerException("Null properties");
        if (props.getProperty(NAME) == null)
            throw new IllegalArgumentException("Missing " + NAME);
        this.props = props;
    }

    /**
     * The properties.
     * 
     * @return the associated properties.
     */
    private final Properties getProps() {
        return this.props;
    }

    public final String getProperty(String name) {
        return this.getProps().getProperty(name);
    }

    public final String getProperty(String name, String def) {
        final String res = this.getProperty(name);
        return StringUtils.isEmpty(res, true) ? def : res;
    }

    public final String getOrganizationName() {
        return this.getProperty(ORGANIZATION_NAME);
    }

    // com.acme
    public final String getOrganizationID() {
        final String res = this.getProperty(ORGANIZATION_ID);
        if (res != null)
            return sanitizeDomain(res);
        final String name = this.getOrganizationName();
        if (name != null)
            return "com." + sanitizeDomainPart(this.getOrganizationName());
        return null;
    }

    // my app
    public final String getName() {
        return this.getProperty(NAME, "unnamed product");
    }

    // my_app
    public final String getID() {
        return sanitizeDomainPart(this.getProperty(ID, this.getName()));
    }

    // com.acme.my_app
    public final String getFullID() {
        final String orgID = this.getOrganizationID();
        return orgID == null ? null : orgID + '.' + this.getID();
    }

    public final String getVersion() {
        return this.getProperty(VERSION);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " for " + getName() + " " + getVersion();
    }
}
