package org.srg.smartclient.isomorphic;

/**
 *
 * @see https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/DmiOverview.html
 * @see https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/ServerObject.html
 */
public class ServerObject {

    public enum LookupStyle {
        /**
         * For use with the Spring framework. bean contains the name of the bean to invoke.
         * Which application context is used can be configured via web.xml (see the example web.xml in the SDK).
         *
         * See also ServerInit for special concerns with framework initialization when using Spring.
         */
        Spring,

        /**
         * For use with CDI (Contexts and Dependency Injection).
         * Use {@link #bean} to configure the name of the bean to invoke or, alternatively,
         * {@link #className} to configure its class name.
         */
        CDI,

        /**
         * A new instance of the class specified by className will be created and the DMI method will be invoked
         * on that instance (unless the specified method is static, in which case no instance is created,
         * but the class specified by {@link #className} is still used)
         */
        New,

        /**
         * A custom factory provides the class instance on which the DMI method is to be invoked.
         * In this case, {@link #className} specifies the className of the factory that
         * will provide the instance on which the DMI method is to be invoked.
         *
         * The class specified by {@link #className} must provide exactly one method
         * named create that must return the class instance on which you wish the DMI method to be invoked.
         * Like the DMI methods, the create method can request a standard set of values as arguments.
         *
         * See DMI for a list of available values.
         */
        Factory,

        /**
         * The instance on which the DMI method is to be invoked is looked up
         * in the scope defined by {@link #attributeScope} via the attribute name specified in {@link #attributeName}.
         */
        Attribute
    }

    /**
     * You can optionally specify an ID on the ServerObject config block -
     * in which case you can use that value as the "className" argument when calling DMI.call().
     */
    private String id;

    /**
     * Specifies the name of the attribute by which to look up the DMI instance.
     */
    private String attributeName;

    /**
     * Specifies the scope in which the DMI instance is to be looked up.
     */
    private String attributeScope;

    /**
     * For use when {@link ServerObject#lookupStyle} is "spring" or "cdi", id (name) of the bean to ask Spring (CDI) to create.
     */
    private String bean;

    /**
     * Specifies the fully-qualified class name that provides the server-side endpoint
     * of the DMI (lookupStyle:"new") or the class name of the factory that produces
     * the DMI instance (lookupStyle:"factory").
     */
    private String className;

    /**
     * For a ServerObject defined at the DataSource level, by default we only allow it
     * to intercept standard CRUD operations (ie, ordinary fetches, adds, updates and removes).
     */
    private boolean crudOnly;

    /**
     * By default, for DMI DSResponses, DSResponse.data is filtered on the server to just the set of fields defined on the DataSource.
     */
    private boolean dropExtraFields;

    /**
     * Specifies the mechanism for locating the class instance on which to invoke the method.
     */
    private LookupStyle lookupStyle;

    /**
     * Specifies the name of the method to call for operations using this ServerObject.
     */
    private String methodName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeScope() {
        return attributeScope;
    }

    public void setAttributeScope(String attributeScope) {
        this.attributeScope = attributeScope;
    }

    public String getBean() {
        return bean;
    }

    public void setBean(String bean) {
        this.bean = bean;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean isCrudOnly() {
        return crudOnly;
    }

    public void setCrudOnly(boolean crudOnly) {
        this.crudOnly = crudOnly;
    }

    public boolean isDropExtraFields() {
        return dropExtraFields;
    }

    public void setDropExtraFields(boolean dropExtraFields) {
        this.dropExtraFields = dropExtraFields;
    }

    public LookupStyle getLookupStyle() {
        return lookupStyle;
    }

    public void setLookupStyle(LookupStyle lookupStyle) {
        this.lookupStyle = lookupStyle;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
