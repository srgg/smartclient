package org.srg.smartclient.jpa;

import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.isomorphic.OperationBinding;

public class JpaDSField extends DSField {
    private OperationBinding declaredBinding;

    public OperationBinding getDeclaredBinding() {
        return declaredBinding;
    }

    public void setDeclaredBinding(OperationBinding declaredBinding) {
        this.declaredBinding = declaredBinding;
    }
}
