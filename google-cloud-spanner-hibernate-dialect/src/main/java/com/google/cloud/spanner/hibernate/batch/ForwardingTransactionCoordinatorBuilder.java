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

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

abstract class ForwardingTransactionCoordinatorBuilder implements TransactionCoordinatorBuilder {
  protected abstract TransactionCoordinatorBuilder delegate();

  @Override
  public TransactionCoordinator buildTransactionCoordinator(
      TransactionCoordinatorOwner owner, Options options) {
    return delegate().buildTransactionCoordinator(owner, options);
  }

  @Override
  public boolean isJta() {
    return delegate().isJta();
  }

  @Override
  public PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode() {
    return delegate().getDefaultConnectionHandlingMode();
  }

  @Override
  public DdlTransactionIsolator buildDdlTransactionIsolator(JdbcContext jdbcContext) {
    return delegate().buildDdlTransactionIsolator(jdbcContext);
  }

  @Override
  public ConnectionAcquisitionMode getDefaultConnectionAcquisitionMode() {
    return delegate().getDefaultConnectionAcquisitionMode();
  }

  @Override
  public ConnectionReleaseMode getDefaultConnectionReleaseMode() {
    return delegate().getDefaultConnectionReleaseMode();
  }
}
