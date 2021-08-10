package org.srg.smartclient.isomorphic;

import org.srg.smartclient.isomorphic.criteria.OperatorId;

import java.util.*;

// https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/DataSourceField.html
public class DSField {

    private static Set<OperatorId> NUMERIC_OP_IDS = new HashSet<>(Arrays.asList(
            OperatorId.EQUALS, OperatorId.NOT_EQUAL,
            OperatorId.GREATER_OR_EQUAL, OperatorId.GREATER_THAN, OperatorId.LESS_OR_EQUAL, OperatorId.LESS_THAN,
            OperatorId.NOT_NULL, OperatorId.IS_NULL,
            OperatorId.BETWEEN, OperatorId.BETWEEN_INCLUSIVE
    ));

    private static Set<OperatorId> TEXT_SPECIFIC = new HashSet<>(Arrays.asList(
            OperatorId.IEQUALS, OperatorId.INOT_EQUAL,
            OperatorId.CONTAINS, OperatorId.ICONTAINS, OperatorId.NOT_CONTAINS, OperatorId.INOT_CONTAINS,
            OperatorId.STARTS_WITH, OperatorId.ISTARTS_WITH, OperatorId.NOT_STARTS_WITH, OperatorId.INOT_STARTS_WITH,
            OperatorId.ENDS_WITH, OperatorId.IENDS_WITH, OperatorId.NOT_ENDS_WITH, OperatorId.INOT_ENDS_WITH
    ));

    /*
     * https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/JpaHibernateRelations.html
     */
    private static Set<OperatorId> ENTITY_SPECIFIC = new HashSet<>(Arrays.asList(
            OperatorId.CONTAINS, OperatorId.ICONTAINS, OperatorId.INOT_CONTAINS,

            /**
             * criterion.value should an array of primaryKey values for the related DataSource.
             * this criterion matches any country which contains any of the passed records
             * (or for "notInSet", which contains none of the passed records).
             */
            OperatorId.IN_SET,  OperatorId.NOT_IN_SET,

            //Matches records which have no related
            OperatorId.IS_NULL,

            // matches records which have at least one related record
            OperatorId.NOT_NULL
    ));

    public static class JoinTableDescr {
        private String tableName;
        private String sourceColumn;
        private String destColumn;

        public JoinTableDescr() {
        }

        public JoinTableDescr(String tableName, String sourceColumn, String destColumn) {
            this.tableName = tableName;
            this.sourceColumn = sourceColumn;
            this.destColumn = destColumn;
        }

        public String getTableName() {
            return tableName;
        }

        public String getSourceColumn() {
            return sourceColumn;
        }

        public String getDestColumn() {
            return destColumn;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public void setSourceColumn(String sourceColumn) {
            this.sourceColumn = sourceColumn;
        }

        public void setDestColumn(String destColumn) {
            this.destColumn = destColumn;
        }
    }


    // https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/types/FieldType.html
    public enum FieldType {
        ANY,
        BINARY,
        BOOLEAN(OperatorId.EQUALS, OperatorId.NOT_EQUAL),
        CREATOR,
        CREATORTIMESTAMP,
        CUSTOM,
        DATE(NUMERIC_OP_IDS),
        DATETIME(NUMERIC_OP_IDS),
        ENUM(NUMERIC_OP_IDS),
        FLOAT(NUMERIC_OP_IDS),
        IMAGE,
        IMAGEFILE,
        INTEGER(NUMERIC_OP_IDS),
        INTENUM(NUMERIC_OP_IDS),
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
        TEXT( TEXT_SPECIFIC, NUMERIC_OP_IDS),
        TIME(NUMERIC_OP_IDS),

        /**
         * This type is not originally supported by SmartClient.
         * It can be used ONLY in conjunction with foreignKey
         */
        ENTITY(ENTITY_SPECIFIC);


        public final Set<OperatorId> defaultOperators;

        FieldType() {
            this(Collections.EMPTY_SET);
        }
        FieldType(OperatorId... operatorIds) {
            this.defaultOperators = Collections.unmodifiableSet(new HashSet<OperatorId>(Arrays.asList(operatorIds)));
        }

        FieldType(Set<OperatorId>... operatorIds) {
            HashSet<OperatorId> r = new HashSet<>();
            for ( Set<OperatorId> s: operatorIds) {
                r.addAll(s);
            }
            this.defaultOperators = Collections.unmodifiableSet(r);
        }
    }

    /**
     * This is used for client-side or server-side summaries
     * Client-side: Function to produce a summary value based on an array of records and a field definition. An example usage is the listGrid summary row, where a row is shown at the bottom of the listGrid containing summary information about each column.
     * Server-side: Function used for getting summarized field value using Server Summaries feature or when Including values from multiple records
     *
     * @see https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/types/SummaryFunctionType.html
     */
    public enum SummaryFunctionType {
        /**
         * iterates through the set of records, picking up all numeric values for the specified field and determining the mean value.
         */
        AVG,

        /**
         * Client: iterates through the set of records, producing a string with each value concatenated to the end.
         * Server: implemented as SQL CONCAT function.
         */
        CONCAT,

        /**
         * Client: returns a numeric count of the total number of records passed in.
         * Server: acts exactly like SQL COUNT function.
         */
        COUNT,

        /**
         * Client: Currently the same as the min function.
         * Server: implemented as SQL MIN function.
         */
        FIRST,

        /**
         * Client: iterates through the set of records, picking up all values for the specified field and finding the maximum value.
         */
        MAX,

        /**
         * Client: iterates through the set of records, picking up all values for the specified field and finding the minimum value.
         */
        MIN,

        /**
         * Client: iterates through the set of records, picking up all numeric values for the specified field and multiplying them together.
         */
        MULTIPLIER,

        /**
         * Client: iterates through the set of records, picking up and summing all numeric values for the specified field.
         */
        SUM,

        /**
         * Client: returns field.summaryValueTitle if specified, otherwise field.title
         * Server: not supported.
         */
        TITLE
    }

    private String name;
    private String title;
    private String titleField;
    private boolean required;
    private boolean primaryKey;
    private Boolean hidden;
    private FieldType type;
    private String foreignKey;
    private String foreignDisplayField;

    /**
     * The field containing treeField: true will display the Tree. If no field specifies this property,
     * if a field named after the Tree.titleProperty of the Tree is present in TreeGrid.fields,
     * that field will show the tree. Note that when using a DataSource,
     * you typically define the title field via DataSource.titleField and the generated ResultTree automatically
     * uses this field. If none of the above rules apply,
     * the first field in TreeGrid.fields is assigned to display the Tree.
     */
    private boolean treeField;

    /**
     * Set of search-operators valid for this field.
     * If not specified, all operators that are valid for the field type are allowed.
     */
    private Set<OperatorId> validOperators;

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


    /**
     * When field.includeFrom is specified and multiple records exist in the related DataSource per record in the including DataSource, includeSummaryFunction indicates which SummaryFunctionType is used to produce the field value.
     *
     * @see https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/DataSourceField.html#includeSummaryFunction
     */
    private SummaryFunctionType includeSummaryFunction;

    private String displayField;
    private String rootValue;
    private String dbName;

    /**
     * The table name to use when qualifying the column name for this field during server-side SQL query generation.
     */
    private String tableName;

    /**
     * For a SQL DataSource field that specifies a foreignKey, this property defines the table alias name to use in generated SQL.
     * Aliasing is necessary when the same table appears more than once in a query. This can happen when using Multiple includeFrom \n fields referring to the same related DataSource. It can also happen when a foreignKey definition references the same dataSource that the field is defined in; this happens with hierarchical structures, for example where every Employee reports to another Employee. This is a so-called "self join", and it always requires relatedTableAlias to be specified; failure to do so will result in invalid SQL.
     *
     * In case of indirect relationship, when more than single join is needed to join the target table, and includeVia is missing, generated alias is a concatenation of relatedTableAlias and FK field names starting with the first relatedTableAlias met in chain of relations leading to the target table.
     *
     * @see https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=attr..DataSourceField.relatedTableAlias
     */
    private String relatedTableAlias;

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
     * Server internal representation of join table used for Many To Many
     */
    private JoinTableDescr joinTable;

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
    private Boolean canFilter;

    // https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/CustomQuerying.html
    private boolean customSQL;

    public String getTitle() {
        return title;
    }

    public DSField setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getTitleField() {
        return titleField;
    }

    public void setTitleField(String titleField) {
        this.titleField = titleField;
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

    public boolean isTreeField() {
        return treeField;
    }

    public void setTreeField(boolean treeField) {
        this.treeField = treeField;
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

    public Boolean isCanFilter() {
        return canFilter;
    }

    public void setCanFilter(Boolean canFilter) {
        this.canFilter = canFilter;
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

    public JoinTableDescr getJoinTable() {
        return joinTable;
    }

    public void setJoinTable(JoinTableDescr joinTable) {
        this.joinTable = joinTable;
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

    public String getRelatedTableAlias() {
        assert (this.getForeignKey() == null || !this.getForeignKey().isBlank())
                : "getRelatedTableAlias() MUST NOT been called for a non FK fields";

//        if (this.getForeignKey() == null || this.getForeignKey().isBlank()) {
//            return null;
//        }
//
//        if (this.relatedTableAlias == null) {
//            /**
//             * Automatic aliases are generated according to the rule:
//             *   first table in possible chain of relations being the name of the field sub-select is getting value
//             *   for (with underscore "_" in front) and the rest aliases are built up using foreign key field names
//             *   in the chained relations leading to the target table.
//             *
//             * This allows to avoid any conflicts with the tables/aliases from the main query.
//             *
//             * from here https://www.smartclient.com/smartgwt/javadoc/com/smartgwt/client/docs/serverds/DataSourceField.html#includeSummaryFunction
//             */
//
//            final String alias = "_%s".formatted(this.getName());

        return relatedTableAlias;
    }

    public SummaryFunctionType getIncludeSummaryFunction() {
        return includeSummaryFunction;
    }

    public void setIncludeSummaryFunction(SummaryFunctionType includeSummaryFunction) {
        this.includeSummaryFunction = includeSummaryFunction;
    }

    public void setRelatedTableAlias(String relatedTableAlias) {
        this.relatedTableAlias = relatedTableAlias;
    }

    public String getMultipleValueSeparator() {
        return multipleValueSeparator;
    }

    public void setMultipleValueSeparator(String multipleValueSeparator) {
        this.multipleValueSeparator = multipleValueSeparator;
    }

    public Set<OperatorId> getValidOperators() {
        if (validOperators == null && getType() != null) {

            // if validOperators is not set explicitly, then use the default ones
            return getType().defaultOperators;
        }
        return validOperators;
    }

    public void setValidOperators(Set<OperatorId> validOperators) {
        this.validOperators = validOperators;
    }
}
