package org.srg.smartclient.runtime;

import org.srg.smartclient.DSHandler;
import org.srg.smartclient.IHandler;
import org.srg.smartclient.RelationSupport;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DataSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DSRuntime implements IDSRuntime {
    private DSRuntime() {}

    Map<String, IHandler> handlers = new HashMap<>();
    Map<String, Map<String, RelationSupport.ForeignKeyRelation>> fkRelations = new HashMap<>();
    Map<String, Map<String, RelationSupport.ImportFromRelation>> ifRelations = new HashMap<>();

    @Override
    public IHandler getHandlerById(String id) {
        return handlers.get(id);
    }

    /**
     * Creates a new instance with combined set of handlers (Copy on write)
     *
     * @param handlers
     * @return new instance
     */
    public synchronized DSRuntime registerHandlers(IHandler... handlers) {
        final DSRuntime r = new DSRuntime();
        r.handlers.putAll(this.handlers);
        r.fkRelations.putAll(this.fkRelations);
        r.ifRelations.putAll(this.ifRelations);

        for(IHandler h: handlers) {
            r.handlers.put(h.id(), h);
        }

        for(IHandler h: handlers) {
            if (h instanceof DSHandler dsh) {
                r.initDSRuntime(dsh.dataSource());
            }
        }

        return r;
    }

    @Override
    public RelationSupport.ForeignKeyRelation getForeignKeyRelation(String dsId, String fieldName) {
        final Map<String, RelationSupport.ForeignKeyRelation> fks = fkRelations.get(dsId);
        if (fks == null) {
            throw new IllegalArgumentException("Nothing known about DataSource with id '%s'".formatted(dsId));
        }

        final RelationSupport.ForeignKeyRelation fkr = fks.get(fieldName);
        if (fkr == null) {
            throw new IllegalArgumentException("Nothing known about ForeignKey relation for '%s.%s'".formatted(dsId, fieldName));
        }

        return fkr;
    }

    @Override
    public RelationSupport.ImportFromRelation getImportFromRelation(String dsId, String fieldName){
        final Map<String, RelationSupport.ImportFromRelation> fks = ifRelations.get(dsId);
        if (fks == null) {
            throw new IllegalArgumentException("Nothing known about DataSource with id '%s'".formatted(dsId));
        }

        final RelationSupport.ImportFromRelation fkr = fks.get(fieldName);
        if (fkr == null) {
            throw new IllegalArgumentException("Nothing known about Foreign ImportFrom relation for '%s.%s'".formatted(dsId, fieldName));
        }

        return fkr;
    }

    protected void initDSRuntime(DataSource ds) {
        for(DSField f: ds.getFields()) {
            Map<String, RelationSupport.ForeignKeyRelation> fks = this.fkRelations.get(ds.getId());
            if (fks == null) {
                fks = new HashMap<>();
                this.fkRelations.put(ds.getId(), fks);
            }

            Map<String, RelationSupport.ImportFromRelation> ifs = this.ifRelations.get(ds.getId());
            if (ifs == null) {
                ifs = new HashMap<>();
                this.ifRelations.put(ds.getId(), ifs);
            }

            if (f.getForeignKey() != null && !f.getForeignKey().isBlank()) {
                fks.put(f.getName(), RelationSupport.describeForeignKey(this, ds, f));
            }

            if (f.isIncludeField()) {
                ifs.put(f.getName(), RelationSupport.describeImportFrom(this, ds, f));
            }
        }

    }

    public static DSRuntime create(Iterable<IHandler> handlers) {
        final DSRuntime r = new DSRuntime();

        for (IHandler h :handlers) {
            r.handlers.put(h.id(), h);
        }

        for (IHandler h : r.handlers.values()) {
            if ( h instanceof DataSource ds) {
                r.initDSRuntime(ds);
            }
        }

        return r;
    }

    @Override
    public Iterator<IHandler> iterator() {
        return handlers.values().iterator();
    }
}
