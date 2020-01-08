package org.hibernate.dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.dialect.entities.Child;
import org.hibernate.dialect.entities.Child.ChildId;
import org.hibernate.dialect.entities.GrandParent;
import org.hibernate.dialect.entities.Parent;
import org.hibernate.dialect.entities.Parent.ParentId;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

public class InterleavedTest extends BaseEntityManagerFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[]{
        Parent.class,
        GrandParent.class,
        Child.class
    };
  }

  /**
   * We override the default with the 'create-drop' mode because we would like to verify that the
   * interleaved tables are created and dropped in the correct order:
   * parents created before children; children dropped before parents.
   */
  @Override
  protected Map buildSettings() {
    Map settings = super.buildSettings();
    settings.put(org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, "create-drop");
    return settings;
  }

  @Test
  public void testInterleavedEntities() {
    super.buildEntityManagerFactory();

    doInJPA(this::entityManagerFactory, entityManager -> {
      GrandParent grandParent = new GrandParent();
      grandParent.setName("Grandparent1");
      entityManager.persist(grandParent);

      Parent parent = new Parent();
      parent.setParentId(new ParentId(grandParent.grandParentId, 1L));
      parent.setName("A_Parent");
      entityManager.persist(parent);

      Child child = new Child();
      child.setChildId(new ChildId(parent.parentId, 2L));
      child.setName("Foobar");
      entityManager.persist(child);

      verifyEntities(entityManager, GrandParent.class);
      verifyEntities(entityManager, Parent.class);
      verifyEntities(entityManager, Child.class);
    });
  }

  private static void verifyEntities(EntityManager entityManager, Class<?> entityClass) {
    Session session = entityManager.unwrap(Session.class);
    List<?> entityList =
        session.createQuery(
            "from " + entityClass.getSimpleName(), entityClass).list();
    assertThat(entityList).hasSize(1);
  }
}
