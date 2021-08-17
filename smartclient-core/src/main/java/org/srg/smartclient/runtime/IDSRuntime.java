package org.srg.smartclient.runtime;

import org.srg.smartclient.IDSLookup;
import org.srg.smartclient.IHandler;
import org.srg.smartclient.RelationSupport;

public interface IDSRuntime extends IDSLookup, Iterable<IHandler> {
    RelationSupport.ForeignKeyRelation getForeignKeyRelation(String dsId, String fieldName);
    RelationSupport.ImportFromRelation getImportFromRelation(String dsId, String fieldName);
}
