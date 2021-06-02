package org.srg.smartclient.dmi;

import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;
import org.srg.smartclient.isomorphic.DataSource;

import java.util.Collections;
import java.util.List;

public record StaticDMI() {

    public StaticDMI(){
        throw new IllegalStateException("Constructor must not be invocked");
    }

    public static DSResponse staticDMI1arg(DSRequest request) {
        final DSField dsf = new DSField();
        dsf.setType(DSField.FieldType.INTEGER);
        dsf.setName("data");

        return DSResponse.successFetch(0, 1, 1, List.of(dsf), Collections.singletonList(new Object[] {42}));
    }
}
