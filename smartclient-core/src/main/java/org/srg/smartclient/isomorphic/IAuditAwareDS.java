package org.srg.smartclient.isomorphic;

/**
 * @see <a href="https://www.smartclient.com/smartclient-release/showcase/?id=auditing">example</a>
 */
public interface IAuditAwareDS {

    /**
     * Enables saving of a log of changes to this DataSource in a second DataSource with the same fields, called the "audit DataSource".
     *
     * @see <a href="https://www.smartclient.com/smartclient-release/isomorphic/system/reference/?id=attr..DataSource.audit"></a>
     */
    default boolean isAudit(){
        return false;
    }

    /**
     * For DataSources with {@code #isAudit() auditing enabled}, specifies the field name used to store the names of the fields which were updated. If empty string is specified as the field name, the audit DataSource will not store this field.
     * Note that this field will only be set for {@code DSRequest#OperationType update} operations.
     */
    default String getAuditChangedFieldsFieldName() {
        return "audit_changedFields";
    }

    /**
     * For DataSources with {@code #isAudit() auditing enabled}, optionally specifies the ID of the audit DataSource. If this property is not specified, the ID of the audit DataSource will be audit_[OriginalDSID].
     */
    default String getAuditDataSourceID() {
        return null;
    }


    /**
     * For DataSources with {@code #isAudit() auditing enabled}, optionally specifies the serverConstructor for the automatically generated audit DataSource.
     *  The default is to use the same serverConstructor as the DataSource where audit="true" was declared.
     */
    default String getAuditDSConstructor() {
        return null;
    }

    /**
     * For audit DataSources, this required property specifies the ID of the {@code #isAudit() audited} DataSource. Automatically populated for auto-generated audit DataSources.
     */
    default String getAuditedDataSourceID() {
        return null;
    }

    /**
     *
     * For DataSources with {@code #isAudit() auditing enabled}, specifies the field name used to store the revision number for the change (in a field of type "sequence"). If empty string is specified as the field name, the audit DataSource will not store this field.
     */
    default String getAuditRevisionFieldName() {
        return "audit_revision";
    }

    /**
     * For DataSources with {@code #isAudit() auditing enabled}, specifies the field name used to store the timestamp when the operation was performed (in a field of type "datetime"). If empty string is specified as the field name, the audit DataSource will not store this field.
     */
    default String getAuditTimeStampFieldName() {
        return "audit_changeTime";
    }

    /**
     * For DataSources with {@code #isAudit() auditing enabled}, specifies the field name used to store the operationType (in a field of type "text"). If empty string is specified as the field name, the audit DataSource will not store this field.
     */
    default String getAuditTypeFieldName() {
        return "audit_operationType";
    }

    /**
     * For DataSources with {@code #isAudit() auditing enabled}, specifies the field name used to store the user who performed the operation. If empty string is specified as the field name, the audit DataSource will not store this field.
     */
    default String getAuditUserFieldName() {
        return "audit_modifier";
    }
}
