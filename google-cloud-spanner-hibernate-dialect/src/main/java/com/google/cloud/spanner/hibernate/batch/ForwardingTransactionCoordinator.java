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

import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.resource.transaction.spi.SynchronizationRegistry;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;

abstract class ForwardingTransactionCoordinator implements TransactionCoordinator {

  protected abstract TransactionCoordinator delegate();

  @Override
  public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder() {
    return delegate().getTransactionCoordinatorBuilder();
  }

  @Override
  public TransactionDriver getTransactionDriverControl() {
    return delegate().getTransactionDriverControl();
  }

  @Override
  public SynchronizationRegistry getLocalSynchronizations() {
    return delegate().getLocalSynchronizations();
  }

  @Override
  public JpaCompliance getJpaCompliance() {
    return delegate().getJpaCompliance();
  }

  @Override
  public void explicitJoin() {
    delegate().explicitJoin();
  }

  @Override
  public boolean isJoined() {
    return delegate().isJoined();
  }

  @Override
  public void pulse() {
    delegate().pulse();
  }

  @Override
  public boolean isActive() {
    return delegate().isActive();
  }

  @Override
  public IsolationDelegate createIsolationDelegate() {
    return delegate().createIsolationDelegate();
  }

  @Override
  public void addObserver(TransactionObserver observer) {
    delegate().addObserver(observer);
  }

  @Override
  public void removeObserver(TransactionObserver observer) {
    delegate().removeObserver(observer);
  }

  @Override
  public void setTimeOut(int seconds) {
    delegate().setTimeOut(seconds);
  }

  @Override
  public int getTimeOut() {
    return delegate().getTimeOut();
  }
}
