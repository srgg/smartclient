package org.srg.smartclient.isomorphic;

import java.util.Map;
import java.util.Objects;

// https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/DataSourceField.html
public class DSField {

    // https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/types/FieldType.html
    public enum FieldType {
        ANY,
        BINARY,
        BOOLEAN,
        CREATOR,
        CREATORTIMESTAMP,
        CUSTOM,
        DATE,
        DATETIME,
        ENUM,
        FLOAT,
        IMAGE,
        IMAGEFILE,
        INTEGER,
        INTENUM,
        LINK,
        LOCALECURRENCY,
        LOCALEFLOAT,
        LOCALEINT,
        MODIFIER,
        MODIFIERTIMESTAMP,
        NTEXT,
        PASSWORD,
        PHONENUMBER,
        SEQUENCE,
        TEXT,
        TIME,

        /**
         * This is not originally supported by SmartClient.
         * It can be used ONLY in conjunction with foreignKey
         */
        ENTITY,
    }

    private String name;
    private String title;
    private boolean required;
    private boolean primaryKey;
    private Boolean hidden;
    private FieldType type;
    private String foreignKey;
    private String foreignDisplayField;

    /**
     * Indicates this field should be fetched from another, related DataSource.
     * The includeFrom attribute should be of the form "dataSourceId.fieldName", for example:
     *
     *      <field includeFrom="supplyItem.itemName"/>
     *
     * A {@link #foreignKey} declaration must exist between the two DataSources, establishing either a 1-to-1 relationship or a many-to-1 relationship from this DataSource to the related DataSource.
     *
     * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/DataSourceField.html#includeFrom
     */
    private String includeFrom;

    /**
     * For a field that uses {@link #includeFrom}, specifies which foreignKey field should be used to find records in the related DataSource.
     * includeVia only needs to be set when you have more than one foreignKey to the same related DataSource. If you have multiple foreignKeys to multiple different DataSources, there is no need to set includeVia.
     *
     * For example, perhaps you have a DataSource "moneyTransfer" where each record represents a money transfer, where the source and payment currencies are different, and the list of currencies is stored in a related DataSource "currency". Each "moneyTransfer" record is linked to 2 "currency" records, through two different foreignKey fields, "sourceCurrencyId" and "paymentCurrencyId".
     *
     * The following declarations would be required to use includeFrom to get a include the field "currencySymbol" from each of the two related "currency" records.
     *
     *     <field name="sourceCurrencyId" foreignKey="currency.id"/>
     *     <field name="paymentCurrencyId" foreignKey="currency.id"/>
     *     <field name="sourceCurrencySymbol" includeFrom="currency.currencySymbol" includeVia="sourceCurrencyId"/>
     *     <field name="paymentCurrencySymbol" includeFrom="currency.currencySymbol" includeVia="paymentCurrencyId"/>
     *
     *  https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/DataSourceField.html#includeVia
     */
    private String includeVia;

    private String displayField;
    private String rootValue;
    private String dbName;

    /**
     * The table name to use when qualifying the column name for this field during server-side SQL query generation.
     */
    private String tableName;

    /**
     * Indicates that this field should always be Array-valued.  If the value derived from
     *  {@link com.smartgwt.client.data.DataSource#getDataFormat XML or JSON data} is singular, it will be wrapped in an Array.
     *  <p>
     *  JPA and Hibernate DataSources use <code>multiple:true</code> as part of the declaration of
     *  One-To-Many and Many-to-Many relations - @see <a href="https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/JpaHibernateRelations.html">com.smartgwt.client.docs.JpaHibernateRelations</a> for details.
     *  <p>
     *  <h4>Criteria on multiple:true fields: client-side filtering</h4>
     *  <p>
     *  For simple Criteria, the criteria value is compared to <i>each</i> field value in the
     *  <code>multiple:true</code> field, according to the
     *  {@link DSRequest#getTextMatchStyle textMatchStyle}.  If <i>any</i> field value matches the
     *  filter value, the field is considered to match the criteria.
     *  <p>
     * For {@link org.srg.smartclient.isomorphic.criteria.AdvancedCriteria}, for normal {@link org.srg.smartclient.isomorphic.criteria.OperatorId search
     * operators} the field
     *  value is considered as matching the <code>Criterion</code> if <i>any</i> of the field values
     *  match the Criterion.  Specifically, this is true of all operators that have an
     *  {@link com.smartgwt.client.types.OperatorValueType operatorValueType} of "fieldType" or "valueRange".
     *  <p>
     *  For operators that compare against other fields in same record, such as "equalsField",
     *  if the other field is <i>not</i> <code>multiple:true</code>, matching works the same as for
     *  normal operators, that is, as if <code>criterion.value</code> directly contained the value
     *  rather than the name of another field.
     *  <p>
     *  If the other field is <i>also</i> multiple:true, only "equalsField", "notEqualsField",
     *  "iEqualsField" and "iNotEqualsField" are allowed (any other <code>operator</code> will
     *  cause a warning and be ignored) and the set of values in the field must be identical (aside
     *  from case, for operators prefixed with "i") and in identical order to match.
     *  <p>
     *  For the <code>inSet</code> operator, the field matches if there is any intersection between
     *  the field values and the array of values provided in <code>criterion.value</code>.
     *  <code>notInSet</code> is the reverse.
     *  <p>
     *  Finally, for "isBlank", "notBlank", "isNull" and "notNull", an empty Array is considered non-null.  For example,
     *  if you use dataFormat:"json" and the field value is provided to the browser as
     *  <code>[]</code> (JSON for an empty Array), the field is considered non-null.
     *  <p>
     *  <h4>Server-side Representation and Storage</h4>
     *  <p>
     *  Values for multiple:true fields appear as Java Lists when received in server code such as a
     *  DMI.  The Smart GWT Server supports simple storage of values that are multiple:true, controlled
     *  via the {@link com.smartgwt.client.docs.serverds.DataSourceField#multipleStorage multipleStorage} setting.
     *  <p>
     *  For server-side behavior of JPA and Hibernate relation fields that are multiple:true, see
     *  {@link com.smartgwt.client.docs.JpaHibernateRelations}.
     *  <p>
     *  For non-relation fields, the Smart GWT Server supports simple storage of values that are
     * multiple:true, controlled via the {@link com.smartgwt.client.docs.serverds.DataSourceField#multipleStorage
     * multipleStorage} setting, with some limited support
     * for server-side filtering, as described in the {@link com.smartgwt.client.docs.serverds.DataSourceField#multipleStorage
     * multipleStorage} docs.
     *  <p>
     *  For the built-in SQL, Hibernate and JPA connectors, if criteria are specified for a
     *  multiple:true field where <code>multipleStorage</code> is null or "none", the Smart GWT
     *  server knows nothing about how the multiple values are stored, so as a fallback the criteria
     *  will operate as though the field were a normal, non-multiple "text" field.  This will
     *  generally <b>not</b> match the client-side filtering behavior described above, so filtering
     *  should either be performed entirely on the client (for example, via
     *  {@link com.smartgwt.client.widgets.grid.ListGrid#getDataFetchMode dataFetchMode:"local"} or entirely on the server (via
     *  {@link com.smartgwt.client.data.ResultSet#getUseClientFiltering ResultSet.useClientFiltering}:"false")
     *  <p>
     *  The server-side filtering is done through a criteria transform which happens with
     *  {@link com.smartgwt.client.docs.serverds.DataSource#transformMultipleFields transformMultipleFields}.
     *  <p>
     *  <h4>XML Serialization</h4>
     *  <P>
     *  Specifically for XML serialization and deserialization, <code>multiple:true</code> behaves
     *  similarly to the
     *  <a href='http://www.google.com/search?hl=en&q=soap+array' target='_blank'>SOAP array idiom</a>, that is,
     *  there will be a "wrapper element" named after the field name, whose contents will be several
     *  elements of the specified {@link #getType()}  field.type}.
     *  <P>
     * For example, {@link com.smartgwt.client.widgets.layout.Layout#getMembers Layout.members} is declared with
     * <code>type:"Canvas",
     *  multiple:true</code>.  The correct XML format is thus:
     *  <pre>
     *  &lt;VLayout&gt;
     *      &lt;members&gt;
     *          &lt;Canvas ID="myCanvas" ... /&gt;
     *          &lt;ListGrid ID="myGrid" .../&gt;
     *          &lt;Toolstrip ID="myToolStrip" ... /&gt;
     *      &lt;/members&gt;
     *  &lt;/VLayout&gt;
     *  </pre>
     *  <P>
     * See {@link com.smartgwt.client.data.DataSourceField#getChildTagName childTagName} for customizing the tagName used for
     * subelements.
     *
     * @return Current multiple value. Default value is false
     * @see com.smartgwt.client.docs.ComponentSchema ComponentSchema overview and related methods
     */
    private boolean multiple;

    /** For fields that are {@link #isMultiple()}  multiple:true}, the separator used
     * between values when they are displayed. Default value is ", "
     */
    private String multipleValueSeparator;

    /**
     * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/CustomQuerying.html
     */
    private String customSelectExpression;

    /**
     * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/DataSourceField.html#valueMap
     */
    private Map valueMap;

    /**
     * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/DataSourceField.html#valueMapEnum
     */
    private String valueMapEnum;

    private boolean canEdit;

    // https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/CustomQuerying.html
    private boolean customSQL;

    public String getTitle() {
        return title;
    }

    public DSField setTitle(String title) {
        this.title = title;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public DSField setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public DSField setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
        return this;
    }

    public Boolean isHidden() {
        return hidden;
    }

    public DSField setHidden(Boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public FieldType getType() {
        return type;
    }

    public DSField setType(FieldType type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public DSField setName(String name) {
        this.name = name;
        return this;
    }

    public String getForeignKey() {
        return foreignKey;
    }

    public DSField setForeignKey(String foreignKey) {
        this.foreignKey = foreignKey;
        return this;
    }

    public String getForeignDisplayField() {
        return foreignDisplayField;
    }

    public DSField setForeignDisplayField(String foreignDisplayField) {
        this.foreignDisplayField = foreignDisplayField;
        return this;
    }

    public String getIncludeFrom() {
        return includeFrom;
    }

    public DSField setIncludeFrom(String includeFrom) {
        this.includeFrom = includeFrom;
        return this;
    }

    public String getIncludeVia() {
        return includeVia;
    }

    public DSField setIncludeVia(String includeVia) {
        this.includeVia = includeVia;
        return this;
    }

    public String getDisplayField() {
        return displayField;
    }
    public DSField setDisplayField(String displayField) {
        this.displayField = displayField;
        return this;
    }

    public String getRootValue() {
        return rootValue;
    }

    public DSField setRootValue(String rootValue) {
        this.rootValue = rootValue;
        return this;
    }

    public String getDbName() {
        if (dbName == null || dbName.isBlank()) {
            return getName();
        }
        return dbName;
    }

    public DSField setDbName(String dbName) {
        this.dbName = dbName;
        return this;
    }


    public boolean isCanEdit() {
        return canEdit;
    }

    public DSField setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
        return this;
    }

    public boolean isCustomSQL() {
        return getCustomSelectExpression() != null && !getCustomSelectExpression().isBlank();
    }

    public String getCustomSelectExpression() {
        return customSelectExpression;
    }

    public DSField setCustomSelectExpression(String customSelectExpression) {
        this.customSelectExpression = customSelectExpression;
        return this;
    }

    public Map getValueMap() {
        return valueMap;
    }

    public DSField setValueMap(Map valueMap) {
        this.valueMap = valueMap;
        return this;
    }

    public String getValueMapEnum() {
        return valueMapEnum;
    }

    public DSField setValueMapEnum(String valueMapEnum) {
        this.valueMapEnum = valueMapEnum;
        return this;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DSField)) return false;
        DSField dsField = (DSField) o;
        return getName().equals(dsField.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public String toString() {
        return "DSField{" +
                "name='" + name + '\'' +
                '}';
    }

    public boolean isIncludeField() {
        return getIncludeFrom() != null && !getIncludeFrom().isBlank();
    }
}
