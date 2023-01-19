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

import java.sql.PreparedStatement;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;

/** Batch implementation that forwards all operations to a delegate. */
abstract class ForwardingBatch implements Batch {
  protected abstract Batch delegate();

  @Override
  public BatchKey getKey() {
    return delegate().getKey();
  }

  @Override
  public void addObserver(BatchObserver observer) {
    delegate().addObserver(observer);
  }

  @Override
  public PreparedStatement getBatchStatement(String sql, boolean callable) {
    return delegate().getBatchStatement(sql, callable);
  }

  @Override
  public void addToBatch() {
    delegate().addToBatch();
  }

  @Override
  public void execute() {
    delegate().execute();
  }

  @Override
  public void release() {
    delegate().release();
  }
}
