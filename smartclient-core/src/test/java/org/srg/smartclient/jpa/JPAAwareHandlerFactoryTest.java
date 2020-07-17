package org.srg.smartclient.jpa;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.JDBCHandler;
import org.srg.smartclient.JsonTestSupport;

import javax.persistence.EntityManagerFactory;

public class JPAAwareHandlerFactoryTest {

    @Test
    public void hiddenPropertySetFromAnnotationShouldOverrideDefaultsForPrimaryKey() {
        final EntityManagerFactory emf = JpaTestSupport.createEntityManagerFactory(
                "testPU_hidden",
                Employee.class,
                EmployeeRole.class,
                ClientData.class,
                Client.class,
                Project.class
            );

        final JPAAwareHandlerFactory hf = new JPAAwareHandlerFactory();
        final JDBCHandler jdbcHandler = hf.createHandler(emf, (database, callback) -> {}, null, EmployeeRole.class);
        assert jdbcHandler != null;


        JsonTestSupport.assertJsonEquals(
        """
        [{
			name:"role"
			,type:"TEXT"
			,primaryKey:true
			,canEdit:false
			,hidden: false
		},
		{
			name:"employee"
			,foreignKey:"EmployeeDS.id"
			,primaryKey:true
			,canEdit:false
			,hidden: true
		}]""", jdbcHandler.getFields(), Option.IGNORING_EXTRA_FIELDS);

    }
}
