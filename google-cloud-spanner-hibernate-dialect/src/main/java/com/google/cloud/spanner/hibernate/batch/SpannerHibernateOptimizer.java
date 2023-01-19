/*
 * Copyright 2019-2022 Google LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package com.google.cloud.spanner.hibernate.batch;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Maps;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.internal.DefaultFlushEventListener;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinator.TransactionDriver;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/** Optimizes batching of DML specifically for Spanner. */
public class SpannerHibernateOptimizer implements ServiceContributor {
  @Override
  public void contribute(StandardServiceRegistryBuilder builder) {
    // Register an initiator for our custom SpannerService
    builder.addInitiator(new SpannerServiceInitiator());
    // Register our customer BatchBuilder. This perform JDBC batch-merging if enabled.
    builder.addInitiator(new SpannerBatchBuilderInitiator());
    // If enabled, registers the custom flush handler which coordinates with the BatchBuilder.
    builder.addService(IntegratorService.class, new SpannerIntegratorService());
    // Mock transaction coordinator. Proof of concept for a future BatchDML + Auto-Commit
    // optimization.
    builder.addService(
        TransactionCoordinatorBuilder.class, new SpannerTransactionCoordinatorBuilder());
  }

  /** Service definition for this optimizer. */
  public interface SpannerService extends Service {
    void startFlush(FlushEvent event);

    void endFlush(FlushEvent event);

    SpannerInProgressFlush getInProgressFlush(JdbcCoordinator coordinator);
  }

  private static class SpannerServiceImpl implements SpannerService {
    private final ConcurrentMap<JdbcCoordinator, SpannerInProgressFlush> flushes =
        Maps.newConcurrentMap();
    private final int batchSize;

    public SpannerServiceImpl(int batchSize) {
      this.batchSize = batchSize;
    }

    @Override
    public void startFlush(FlushEvent event) {
      JdbcCoordinator coordinator = event.getSession().getJdbcCoordinator();
      SpannerInProgressFlush flush = new SpannerInProgressFlush(coordinator, batchSize);
      flushes.put(coordinator, flush);
    }

    @Override
    public void endFlush(FlushEvent event) {
      JdbcCoordinator coordinator = event.getSession().getJdbcCoordinator();
      SpannerInProgressFlush flush = flushes.remove(coordinator);
      flush.doExecute();
    }

    @Override
    public SpannerInProgressFlush getInProgressFlush(JdbcCoordinator coordinator) {
      return flushes.get(coordinator);
    }
  }

  private static class SpannerServiceInitiator implements StandardServiceInitiator<SpannerService> {
    @Override
    public Class<SpannerService> getServiceInitiated() {
      return SpannerService.class;
    }

    @Override
    public SpannerService initiateService(Map config, ServiceRegistryImplementor registry) {
      String option = AvailableSettings.STATEMENT_BATCH_SIZE;
      int batchSize = ConfigurationHelper.getInt(option, config, 1);
      return new SpannerServiceImpl(batchSize);
    }
  }

  private static class SpannerBatchStatement {
    private final PreparedStatement statement;
    private final String sql;
    // TODO(thunes): Validate expectations at the end of a batch. This will require
    // changes to java-spanner and java-spanner-jdbc to provide access to the modified
    // row count results from the ExecuteBatchDml call.
    private final Expectation expectation;

    public SpannerBatchStatement(PreparedStatement statement, String sql, Expectation expectation) {
      this.statement = statement;
      this.sql = sql;
      this.expectation = expectation;
    }
  }

  private static class SpannerInProgressFlush {
    private final JdbcCoordinator coordinator;
    private final SqlExceptionHelper exceptionHelper;
    private final List<SpannerBatchStatement> buffered = new ArrayList<>();
    private final int batchSize;
    private SpannerBatchStatement current = null;

    public SpannerInProgressFlush(JdbcCoordinator coordinator, int batchSize) {
      this.coordinator = coordinator;
      this.batchSize = batchSize;
      exceptionHelper =
          coordinator
              .getJdbcSessionOwner()
              .getJdbcSessionContext()
              .getServiceRegistry()
              .getService(JdbcServices.class)
              .getSqlExceptionHelper();
    }

    PreparedStatement getBatchStatement(String sql, boolean callable, Expectation expectation) {
      PreparedStatement statement =
          coordinator.getStatementPreparer().prepareStatement(sql, callable);
      current = new SpannerBatchStatement(statement, sql, expectation);
      return statement;
    }

    void addToBatch(List<BatchObserver> observers) {
      if (buffered.isEmpty()) {
        sendControlStatement("START BATCH DML");
      }

      try {
        current.statement.executeUpdate();
        buffered.add(current);
      } catch (SQLException e) {
        abortBatch(e);
        throw exceptionHelper.convert(e, "exception buffering statement in batch", current.sql);
      } catch (RuntimeException e) {
        abortBatch(e);
        throw e;
      } finally {
        current = null;
      }

      if (buffered.size() >= batchSize) {
        observers.forEach(BatchObserver::batchImplicitlyExecuted);
        doExecute();
      }
    }

    private void doExecute() {
      if (buffered.size() > 0) {
        try {
          sendControlStatement("RUN BATCH");
        } finally {
          buffered.clear();
        }
      }
    }

    private void sendControlStatement(String sql) {
      Connection connection = coordinator.getLogicalConnection().getPhysicalConnection();
      try {
        connection.createStatement().execute(sql);
      } catch (SQLException e) {
        abortBatch(e);
        throw exceptionHelper.convert(e, "exception running batch", sql);
      } catch (RuntimeException e) {
        abortBatch(e);
        throw e;
      }
    }

    private void abortBatch(Exception cause) {
      try {
        if (buffered.size() > 0) {
          // We do not use sendControlStatment here because it will recursively call
          // abortBatch on an exception.
          coordinator
              .getLogicalConnection()
              .getPhysicalConnection()
              .createStatement()
              .execute("ABORT BATCH");
        }
        coordinator.abortBatch();
      } catch (RuntimeException | SQLException e) {
        cause.addSuppressed(e);
      }
    }
  }

  private static class SpannerBatch implements Batch {
    private final SpannerService service;
    private final BatchKey key;
    private final JdbcCoordinator coordinator;
    private final List<BatchObserver> observers = new ArrayList<>();

    public SpannerBatch(SpannerService service, BatchKey key, JdbcCoordinator coordinator) {
      // TODO(thunes): Copied from Hibernate BatchingBatch
      if (!key.getExpectation().canBeBatched()) {
        throw new HibernateException("attempting to batch an operation which cannot be batched");
      }

      this.service = service;
      this.key = key;
      this.coordinator = coordinator;
    }

    @Override
    public BatchKey getKey() {
      return key;
    }

    @Override
    public void addObserver(BatchObserver observer) {
      observers.add(observer);
    }

    private SpannerInProgressFlush flush() {
      return Verify.verifyNotNull(
          service.getInProgressFlush(coordinator), "Spanner in-progress flush record missing");
    }

    @Override
    public PreparedStatement getBatchStatement(String sql, boolean callable) {
      return flush().getBatchStatement(sql, callable, key.getExpectation());
    }

    @Override
    public void addToBatch() {
      flush().addToBatch(observers);
    }

    @Override
    public void execute() {
      // No-op. We ignore all execute() calls and instead rely on implicit batching and a
      // final "RUN BATCH" at the end of the overall flush operation.
    }

    @Override
    public void release() {
      observers.clear();
    }
  }

  /** Service initiator for the Spanner batch optimizer. */
  public static class SpannerBatchBuilderInitiator
      implements StandardServiceInitiator<BatchBuilder> {

    @Override
    public Class<BatchBuilder> getServiceInitiated() {
      return BatchBuilder.class;
    }

    @Override
    public BatchBuilder initiateService(Map config, ServiceRegistryImplementor registry) {
      SpannerService service = registry.requireService(SpannerService.class);
      String option = AvailableSettings.STATEMENT_BATCH_SIZE;
      int batchSize = ConfigurationHelper.getInt(option, config, 1);
      return new SpannerBatchBuilder(service, batchSize);
    }
  }

  /** Specific {@link BatchBuilder} implementation for Spanner. */
  public static class SpannerBatchBuilder implements BatchBuilder {
    private final SpannerService service;
    private final BatchBuilderImpl fallback;

    public SpannerBatchBuilder(SpannerService service, int batchSize) {
      this.service = service;
      this.fallback = new BatchBuilderImpl(batchSize);
    }

    @Override
    public Batch buildBatch(BatchKey key, JdbcCoordinator coordinator) {
      if (service.getInProgressFlush(coordinator) != null) {
        return new SpannerBatch(service, key, coordinator);
      }

      return fallback.buildBatch(key, coordinator);
    }
  }

  private static class SpannerIntegratorService implements IntegratorService {
    @Override
    public Iterable<Integrator> getIntegrators() {
      return Arrays.asList(new SpannerIntegrator());
    }
  }

  private static class SpannerIntegrator implements Integrator {
    private static final String ENABLED_PROPERTY =
        SpannerHibernateOptimizer.class.getName() + ".enabled";

    @Override
    public void integrate(
        Metadata metadata,
        SessionFactoryImplementor sessionFactory,
        SessionFactoryServiceRegistry registry) {

      ConfigurationService config = registry.getService(ConfigurationService.class);
      boolean enabled = config.getSetting(ENABLED_PROPERTY, StandardConverters.BOOLEAN, false);
      if (enabled) {
        EventListenerRegistry listeners = registry.getService(EventListenerRegistry.class);
        listeners.addDuplicationStrategy(
            new DuplicationStrategy() {
              @Override
              public boolean areMatch(Object listener, Object original) {
                return (listener instanceof SpannerFlushListener)
                    && (original instanceof DefaultFlushEventListener);
              }

              @Override
              public Action getAction() {
                return Action.REPLACE_ORIGINAL;
              }
            });
        SpannerService service = registry.requireService(SpannerService.class);
        listeners.appendListeners(EventType.FLUSH, new SpannerFlushListener(service));
      }
    }

    @Override
    public void disintegrate(
        SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry registry) {
      // No-op
    }
  }

  private static class SpannerFlushListener extends DefaultFlushEventListener {
    private final SpannerService service;

    public SpannerFlushListener(SpannerService service) {
      this.service = Preconditions.checkNotNull(service);
    }

    @Override
    public void onFlush(FlushEvent event) throws HibernateException {
      try {
        service.startFlush(event);
        super.onFlush(event);
      } finally {
        service.endFlush(event);
      }
    }
  }

  /** Transaction coordinator that is used for Spanner DML batches. */
  public static class SpannerTransactionCoordinatorBuilder
      extends ForwardingTransactionCoordinatorBuilder {

    @Override
    protected TransactionCoordinatorBuilder delegate() {
      return JdbcResourceLocalTransactionCoordinatorBuilderImpl.INSTANCE;
    }

    @Override
    public TransactionCoordinator buildTransactionCoordinator(
        TransactionCoordinatorOwner owner, Options options) {

      if (!(owner instanceof JdbcCoordinator)) {
        throw new UnsupportedOperationException(
            "SpannerTransactionCoordinator requires JdbcCoordinator session owner");
      }

      JdbcCoordinator jdbcCoordinator = (JdbcCoordinator) owner;
      TransactionCoordinator txnDelegate = super.buildTransactionCoordinator(owner, options);
      TransactionDriver txnDriver = txnDelegate.getTransactionDriverControl();

      return new ForwardingTransactionCoordinator() {
        @Override
        protected TransactionCoordinator delegate() {
          return txnDelegate;
        }

        @Override
        public TransactionDriver getTransactionDriverControl() {
          return new ForwardingTransactionDriver() {
            @Override
            protected TransactionDriver delegate() {
              return txnDriver;
            }

            @Override
            public void commit() {
              // Inform the driver that a flush + commit is about to occur so the
              // BatchDML RPC can be deferred until commit() is called.
              Connection connection =
                  jdbcCoordinator.getLogicalConnection().getPhysicalConnection();
              super.commit();
            }
          };
        }
      };
    }
  }
}
