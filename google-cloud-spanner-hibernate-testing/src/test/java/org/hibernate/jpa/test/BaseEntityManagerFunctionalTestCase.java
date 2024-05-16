/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test;

import com.google.cloud.spanner.hibernate.SpannerDialect;
import jakarta.persistence.EntityManager;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;

/**
 * A base class for all ejb tests.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public abstract class BaseEntityManagerFunctionalTestCase extends BaseUnitTestCase {

  // IMPL NOTE : Here we use @Before and @After (instead of @BeforeClassOnce and @AfterClassOnce
  // like we do in
  // BaseCoreFunctionalTestCase) because the old HEM test methodology was to create an EMF for each
  // test method.

  private static final Dialect dialect = new SpannerDialect();

  private StandardServiceRegistryImpl serviceRegistry;
  private SessionFactoryImplementor entityManagerFactory;

  private EntityManager em;
  private ArrayList<EntityManager> isolatedEms = new ArrayList<EntityManager>();

  protected Dialect getDialect() {
    return dialect;
  }

  protected SessionFactoryImplementor entityManagerFactory() {
    return entityManagerFactory;
  }

  protected StandardServiceRegistryImpl serviceRegistry() {
    return serviceRegistry;
  }

  @Before
  @SuppressWarnings({"UnusedDeclaration"})
  public void buildEntityManagerFactory() {
    log.trace("Building EntityManagerFactory");

    entityManagerFactory =
        Bootstrap.getEntityManagerFactoryBuilder(buildPersistenceUnitDescriptor(), buildSettings())
            .build()
            .unwrap(SessionFactoryImplementor.class);

    serviceRegistry =
        (StandardServiceRegistryImpl)
            entityManagerFactory.getServiceRegistry().getParentServiceRegistry();

    afterEntityManagerFactoryBuilt();
  }

  private PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
    return new TestingPersistenceUnitDescriptorImpl(getClass().getSimpleName());
  }

  public static class TestingPersistenceUnitDescriptorImpl implements PersistenceUnitDescriptor {
    private final String name;

    public TestingPersistenceUnitDescriptorImpl(String name) {
      this.name = name;
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
      return null;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getProviderClassName() {
      return HibernatePersistenceProvider.class.getName();
    }

    @Override
    public boolean isUseQuotedIdentifiers() {
      return false;
    }

    @Override
    public boolean isExcludeUnlistedClasses() {
      return false;
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
      return null;
    }

    @Override
    public ValidationMode getValidationMode() {
      return null;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
      return null;
    }

    @Override
    public List<String> getManagedClassNames() {
      return null;
    }

    @Override
    public List<String> getMappingFileNames() {
      return null;
    }

    @Override
    public List<URL> getJarFileUrls() {
      return null;
    }

    @Override
    public Object getNonJtaDataSource() {
      return null;
    }

    @Override
    public Object getJtaDataSource() {
      return null;
    }

    @Override
    public Properties getProperties() {
      return null;
    }

    @Override
    public ClassLoader getClassLoader() {
      return null;
    }

    @Override
    public ClassLoader getTempClassLoader() {
      return null;
    }

    @Override
    public void pushClassTransformer(EnhancementContext enhancementContext) {}

    @Override
    public ClassTransformer getClassTransformer() {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  protected Map buildSettings() {
    Map settings = getConfig();
    addMappings(settings);

    if (createSchema()) {
      settings.put(org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, "create-drop");
    }
    // TODO: Figure out what the equivalent in Hibernate 6 is for this.
    // settings.put( org.hibernate.cfg.AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
    settings.put(org.hibernate.cfg.AvailableSettings.DIALECT, getDialect().getClass().getName());
    return settings;
  }

  @SuppressWarnings("unchecked")
  protected void addMappings(Map settings) {
    String[] mappings = getMappings();
    if (mappings != null) {
      settings.put(AvailableSettings.HBM_XML_FILES, String.join(",", mappings));
    }
  }

  protected static final String[] NO_MAPPINGS = new String[0];

  protected String[] getMappings() {
    return NO_MAPPINGS;
  }

  protected Map getConfig() {
    Map<Object, Object> config = Environment.getProperties();
    ArrayList<Class> classes = new ArrayList<Class>();

    classes.addAll(Arrays.asList(getAnnotatedClasses()));
    config.put(AvailableSettings.LOADED_CLASSES, classes);
    for (Map.Entry<Class, String> entry : getCachedClasses().entrySet()) {
      config.put(
          AvailableSettings.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : getCachedCollections().entrySet()) {
      config.put(
          AvailableSettings.COLLECTION_CACHE_PREFIX + "." + entry.getKey(), entry.getValue());
    }
    if (getEjb3DD().length > 0) {
      ArrayList<String> dds = new ArrayList<String>();
      dds.addAll(Arrays.asList(getEjb3DD()));
      config.put(AvailableSettings.ORM_XML_FILES, dds);
    }

    addConfigOptions(config);
    return config;
  }

  protected void addConfigOptions(Map options) {}

  protected static final Class<?>[] NO_CLASSES = new Class[0];

  protected Class<?>[] getAnnotatedClasses() {
    return NO_CLASSES;
  }

  public Map<Class, String> getCachedClasses() {
    return new HashMap<Class, String>();
  }

  public Map<String, String> getCachedCollections() {
    return new HashMap<String, String>();
  }

  public String[] getEjb3DD() {
    return new String[] {};
  }

  protected void afterEntityManagerFactoryBuilt() {}

  protected boolean createSchema() {
    return true;
  }

  @After
  @SuppressWarnings({"UnusedDeclaration"})
  public void releaseResources() {
    try {
      releaseUnclosedEntityManagers();
    } finally {
      if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
        entityManagerFactory.close();
      }
    }
    // Note we don't destroy the service registry as we are not the ones creating it
  }

  private void releaseUnclosedEntityManagers() {
    releaseUnclosedEntityManager(this.em);

    for (EntityManager isolatedEm : isolatedEms) {
      releaseUnclosedEntityManager(isolatedEm);
    }
  }

  private void releaseUnclosedEntityManager(EntityManager em) {
    if (em == null) {
      return;
    }
    if (!em.isOpen()) {
      return;
    }

    if (em.getTransaction().isActive()) {
      em.getTransaction().rollback();
      log.warn(
          "You left an open transaction! Fix your test case. For now, we are closing it for you.");
    }
    if (em.isOpen()) {
      // as we open an EM before the test runs, it will still be open if the test uses a custom EM.
      // or, the person may have forgotten to close. So, do not raise a "fail", but log the fact.
      em.close();
      log.warn("The EntityManager is not closed. Closing it.");
    }
  }

  protected EntityManager getOrCreateEntityManager() {
    if (em == null || !em.isOpen()) {
      em = entityManagerFactory.createEntityManager();
    }
    return em;
  }

  protected EntityManager createIsolatedEntityManager() {
    EntityManager isolatedEm = entityManagerFactory.createEntityManager();
    isolatedEms.add(isolatedEm);
    return isolatedEm;
  }

  protected EntityManager createIsolatedEntityManager(Map props) {
    EntityManager isolatedEm = entityManagerFactory.createEntityManager(props);
    isolatedEms.add(isolatedEm);
    return isolatedEm;
  }

  protected EntityManager createEntityManager() {
    return createEntityManager(Collections.emptyMap());
  }

  protected EntityManager createEntityManager(Map properties) {
    // always reopen a new EM and close the existing one
    if (em != null && em.isOpen()) {
      em.close();
    }
    em = entityManagerFactory.createEntityManager(properties);
    return em;
  }
}
