package org.srg.smartclient.dmi;

import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.DSRequest;
import org.srg.smartclient.isomorphic.DSResponse;

import java.util.Collections;

public class DMIFactory_With_Zero_Arg {
    public static class SimpleDMI {
        public DSResponse oneArg(DSRequest request) {
            final DSField dsf = new DSField();
            dsf.setType(DSField.FieldType.INTEGER);
            dsf.setName("data");
            return DSResponse.successFetch(0,1,1,
                    Collections.singletonList(dsf), Collections.singletonList(new Object[]{24}) );
        }
    }

    public SimpleDMI create() {
        return new SimpleDMI();
    }
}
