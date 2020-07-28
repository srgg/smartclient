package org.srg.smartclient;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.srg.smartclient.isomorphic.DSField;
import org.srg.smartclient.jpa.*;
import org.srg.smartclient.utils.JsonSerde;

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

		final JpaRelation relation = JpaRelation.describeRelation(mm, et, attr);

		JsonTestSupport.assertJsonEquals("""
      		{
      			type:'ONE_TO_MANY',
      			sourceType:'Client',
      			sourceAttribute:'projects',
      			
      			targetType:'Project',
      			targetAttribute:'client',
      			isInverse:true,
      			
      			joinColumns: []
    		}""", relation);

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

		final JpaRelation jpaRelation = JpaRelation.describeRelation(mm, et, attr);
		JsonTestSupport.assertJsonEquals("""
				{
					type:'ONE_TO_MANY',
   					sourceType:'Employee',
   					sourceAttribute:'roles',
   					targetType:'EmployeeRole',
   					targetAttribute:'employee',
   					isInverse:true,
   					joinColumns:[]				
				}""", jpaRelation);

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
	public void manyToOneWithAssociationOverride_ShouldOverrideDBName() {
		final Metamodel mm = emf.getMetamodel();
		final String dsId = "TestDS";

		{
			final EntityType<Employee> employeeEntityType = mm.entity(Employee.class);
			final Attribute<Employee, ?> employeeStatusesAttribute = employeeEntityType.getDeclaredAttribute("statuses");

			final JpaRelation jpaRelation = JpaRelation.describeRelation(mm, employeeEntityType, employeeStatusesAttribute);
			JsonTestSupport.assertJsonEquals("""
				{
					type:'ONE_TO_MANY',
					sourceType:'Employee',
					sourceAttribute:'statuses',

					targetType:'EmployeeStatus',
					targetAttribute: 'owner',
					isInverse: true,
					joinColumns: []
				}""", jpaRelation);

			final DSField dsf = jpaAwareHandlerFactory.describeField(mm, dsId, employeeEntityType, employeeStatusesAttribute);

			JsonTestSupport.assertJsonEquals("""
      		{
				name:'statuses',
				required:false,
				primaryKey:false,
				hidden:true,
				type:'ENTITY',
				foreignKey:'EmployeeStatusDS.id',
				includeFrom: 'EmployeeStatusDS.owner',
				dbName:'statuses',
				multiple:true      		
			}""", dsf, Option.IGNORING_EXTRA_FIELDS);
		}

		final EntityType<EmployeeStatus> employeeStatusEntityType = mm.entity(EmployeeStatus.class);
		final Attribute<EmployeeStatus, ?> employeeStatusAttribute = (Attribute<EmployeeStatus, ?>) employeeStatusEntityType.getAttribute("owner");

		final JpaRelation relation = JpaRelation.describeRelation(mm, employeeStatusEntityType, employeeStatusAttribute);

		JsonTestSupport.assertJsonEquals("""
      		{
      			type:'MANY_TO_ONE',
      			sourceType:'EmployeeStatus',
      			sourceAttribute:'owner',
      			
      			targetType:'Employee',
      			targetAttribute: 'id',
      			isInverse: false,
      			joinColumns: [
					{
						insertable:true,
						name:'employee_id',
						nullable:false,
						referencedColumnName:'',
						table:'',
						unique:false,
						updatable:true
					}
				]      			
    		}""", relation);

		final DSField dsf = jpaAwareHandlerFactory.describeField(mm, dsId, employeeStatusEntityType, employeeStatusAttribute);

		JsonTestSupport.assertJsonEquals("""
      		{
				name:'owner',
				required:true,
				primaryKey:false,
				hidden:false,
				type:'INTEGER',
				foreignKey:'EmployeeDS.id',
				includeFrom: null,
				dbName:'employee_id',
				multiple:false      		
			}""", dsf, Option.IGNORING_EXTRA_FIELDS);
	}

	@Test
	public void smartClientFieldAnnotation_onMethod() {
		final Metamodel mm = emf.getMetamodel();
		final String dsId = "TestDS";

		{
			final EntityType<EmployeeStatus> employeeStatusEntityType = mm.entity(EmployeeStatus.class);
			final Attribute<?, ?> employeeStatusOwnerAttribute = employeeStatusEntityType.getAttribute("owner");

			final DSField dsf = jpaAwareHandlerFactory.describeField(mm, dsId, employeeStatusEntityType, employeeStatusOwnerAttribute);

			JsonTestSupport.assertJsonEquals("""
      		{
   				name:'owner',
			   	required:true,
   				primaryKey:false,
   				hidden:false,
   				type:'INTEGER',
   				foreignKey:'EmployeeDS.id',
   				foreignDisplayField:'name',
			    dbName:'employee_id'
      		}""", dsf, Option.IGNORING_EXTRA_FIELDS);

		}
	}

	@Disabled("Disabled until @ManyToMany will be supported")
	@Test
	public void manyToMany() {
		final Metamodel mm = emf.getMetamodel();
		final String dsId = "TestDS";

		{
			final EntityType<Project> projectEntityType = mm.entity(Project.class);
			final Attribute<Project, ?> projectTeamAttribute = projectEntityType.getDeclaredAttribute("teamMembers");

			final JpaRelation jpaRelation = JpaRelation.describeRelation(mm, projectEntityType, projectTeamAttribute);
			JsonTestSupport.assertJsonEquals("""
					{
						type:'MANY_TO_MANY',
						sourceType:'Project',
						sourceAttribute:'teamMembers',

						targetType:'Employee',
						targetAttribute: 'id',
						isInverse: false,
						joinColumns: [
							{
								name:'project_id',
								nullable:true,
								insertable:true,
								unique:false,
								updatable:true,
								referencedColumnName:'',
								table:''
							}
						]
					}""", jpaRelation);

			final DSField dsf = jpaAwareHandlerFactory.describeField(mm, dsId, projectEntityType, projectTeamAttribute);

			JsonTestSupport.assertJsonEquals("""
				{
					name:'teamMembers',
   					required:false,
   					primaryKey:false,
   					hidden:true,
   					type:'ENTITY',
   					foreignKey:'EmployeeDS.id',
   					includeFrom:null,
   					foreignDisplayField:null,
				   	dbName:'project_id',
   					multiple:true
				}""", dsf, Option.IGNORING_EXTRA_FIELDS);
		}
		{
			final EntityType<Employee> employeeEntityType = mm.entity(Employee.class);
			final Attribute<Employee, ?> employeeProjectsAttribute = employeeEntityType.getDeclaredAttribute("projects");

			final JpaRelation jpaRelation = JpaRelation.describeRelation(mm, employeeEntityType, employeeProjectsAttribute);
			JsonTestSupport.assertJsonEquals("""
					{
						type:'MANY_TO_MANY',
						sourceType:'Employee',
						sourceAttribute:'projects',

						targetType:'Project',
						targetAttribute: 'teamMembers',
						isInverse: true,
						joinColumns: []
					}""", jpaRelation);

			final DSField dsf = jpaAwareHandlerFactory.describeField(mm, dsId, employeeEntityType, employeeProjectsAttribute);

			JsonTestSupport.assertJsonEquals("""
				{
					name:'projects',
   					required:false,
   					primaryKey:false,
   					hidden:true,
   					type:'ENTITY',
   					foreignKey:'ProjectDS.id',
   					includeFrom:'ProjectDS.teamMembers',
   					foreignDisplayField:null,
				   	dbName:'projects',
   					multiple:true
				}""", dsf, Option.IGNORING_EXTRA_FIELDS);

		}
	}
}
