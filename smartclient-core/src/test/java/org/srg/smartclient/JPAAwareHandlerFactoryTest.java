package org.srg.smartclient;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.JDBCHandler;
import org.srg.smartclient.JPAAwareHandlerFactory;
import org.srg.smartclient.JsonTestSupport;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.jpa.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

public class JPAAwareHandlerFactoryTest {

	private EntityManagerFactory emf;
	private JPAAwareHandlerFactory jpaAwareHandlerFactory;


	@BeforeAll
	public static void setupJsonMapper() {
		JsonTestSupport.defaultMapper = JsonSerde.createMapper();
	}

	@BeforeEach
	public void setupJPADispatcher() throws Exception {
		emf = JpaTestSupport.createEntityManagerFactory(
				"testPU",
				SimpleEntity.class,
				Employee.class,
				EmployeeStatus.class,
				EmployeeRole.class,
				ClientData.class,
				Client.class,
				Project.class
		);

		jpaAwareHandlerFactory = new JPAAwareHandlerFactory();
	}

    @Test
    public void hiddenPropertySetFromAnnotationShouldOverrideDefaultsForPrimaryKey() {
//        final EntityManagerFactory emf = JpaTestSupport.createEntityManagerFactory(
//                "testPU_hidden",
//				EmployeeStatus.class,
//                Employee.class,
//                EmployeeRole.class,
//                ClientData.class,
//                Client.class,
//                Project.class
//            );

//        final JPAAwareHandlerFactory hf = new JPAAwareHandlerFactory();
        final JDBCHandler jdbcHandler = jpaAwareHandlerFactory.createHandler(emf, (database, callback) -> {}, null, EmployeeRole.class);
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

    @Test
    public void oneToMany() {
		final Metamodel mm = emf.getMetamodel();
		final String dsId = "TestDS";

		final EntityType<Client> et = mm.entity(Client.class);
		final Attribute<Client, ?> attr = (Attribute<Client, ?>) et.getAttribute("projects");

		final DSField dsf = jpaAwareHandlerFactory.describeField(mm, dsId, et, attr);

		JsonTestSupport.assertJsonEquals("""
      		{
				name:'projects',
				required:false,
				primaryKey:false,
				hidden:true,
				type:'ENTITY',
				foreignKey:'ProjectDS.id',
				foreignDisplayField:null,
				dbName:'projects',
				multiple:true
    		}""", dsf, Option.IGNORING_EXTRA_FIELDS);
	}

	@Test
	public void oneToManyRelationWithCompositeForeignKey() {
		final Metamodel mm = emf.getMetamodel();
		final String dsId = "TestDS";

		final EntityType<Employee> et = mm.entity(Employee.class);
		final Attribute<Employee, ?> attr = (Attribute<Employee, ?>) et.getAttribute("roles");

		final DSField dsf = jpaAwareHandlerFactory.describeField(mm, dsId, et, attr);

		JsonTestSupport.assertJsonEquals("""
      		{
				name:'roles',
				required:false,
				primaryKey:false,
				hidden:true,
				type:'ENTITY',
				foreignKey:'EmployeeRoleDS.employee',
				foreignDisplayField:null,
				dbName:'roles',
				multiple:true
    		}""", dsf, Option.IGNORING_EXTRA_FIELDS);
	}

	@Test
	public void associationOverrideShouldOverrideDBName() {
		final Metamodel mm = emf.getMetamodel();
		final String dsId = "TestDS";

		final EntityType<EmployeeStatus> et = mm.entity(EmployeeStatus.class);
		final Attribute<EmployeeStatus, ?> attr = (Attribute<EmployeeStatus, ?>) et.getAttribute("owner");

		final DSField dsf = jpaAwareHandlerFactory.describeField(mm, dsId, et, attr);

		JsonTestSupport.assertJsonEquals("""
      		{
				name:'owner',
				required:true,
				primaryKey:false,
				hidden:false,
				type:'INTEGER',
				foreignKey:'EmployeeDS.id',
				dbName:'employee_id',
				multiple:false      		
			}""", dsf, Option.IGNORING_EXTRA_FIELDS);
	}
}
