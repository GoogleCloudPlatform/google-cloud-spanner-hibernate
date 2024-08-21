/*
 * Copyright 2019-2023 Google LLC
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

package com.google.cloud.spanner.sample.entities;

import com.google.cloud.spanner.sample.CreationCommitTimestamp;
import com.google.cloud.spanner.sample.UpdateCommitTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Base class for all entities that are used by this sample application.
 *
 * <p>This class defines the createdAt and updatedAt properties that are present in all entities.
 */
@MappedSuperclass
public class AbstractEntity {

  @Column @CreationTimestamp private OffsetDateTime createdAt;

  @Column @UpdateTimestamp private OffsetDateTime updatedAt;

  /**
   * This column shows how to automatically set the value to the pending_commit_timestamp() when a
   * row is INSERTED. This achieved like this:
   *
   * <ol>
   *   <li>Add a @CreationCommitTimestamp annotation.
   *   <li>Set updatable=false to prevent it from being set when the row is updated.
   *   <li>Add a ColumnTransformer to change the behavior when the value is read. The read
   *       expression is set to a fixed timestamp. This means that Hibernate will always execute a
   *       "select timestamp '0001-01-01T00:00:00Z'" instead of "select commitTimestampCreated".
   *       This again prevents the column from being included in select statements, which again
   *       means that the application will never fail due to the limitation that Spanner sets that
   *       columns that have a pending_commit_timestamp value set cannot be read during the same
   *       transaction. See <a
   *       href="https://cloud.google.com/spanner/docs/commit-timestamp#dml">https://cloud.google.com/spanner/docs/commit-timestamp#dml</a>
   *       for more information on this. This annotation can be removed if you are absolutely sure
   *       that your application never reads the column in a transaction that has written a value to
   *       the column.
   * </ol>
   */
  @Column(updatable = false, columnDefinition = "timestamp options (allow_commit_timestamp=true)")
  @CreationCommitTimestamp
  @ColumnTransformer(read = "timestamp '0001-01-01T00:00:00Z'")
  private OffsetDateTime commitTimestampCreated;

  /**
   * This column shows how to automatically set the value to the pending_commit_timestamp() when a
   * row is UPDATED. This achieved like this:
   *
   * <ol>
   *   <li>Add a @CreationCommitTimestamp annotation.
   *   <li>Set insertable=false to prevent it from being set when the row is updated.
   *   <li>Add a ColumnTransformer to change the behavior when the value is read. The read
   *       expression is set to a fixed timestamp. This means that Hibernate will always execute a
   *       "select timestamp '0001-01-01T00:00:00Z'" instead of "select commitTimestampCreated".
   *       This again prevents the column from being included in select statements, which again
   *       means that the application will never fail due to the limitation that Spanner sets that
   *       columns that have a pending_commit_timestamp value set cannot be read during the same
   *       transaction. See <a
   *       href="https://cloud.google.com/spanner/docs/commit-timestamp#dml">https://cloud.google.com/spanner/docs/commit-timestamp#dml</a>
   *       for more information on this. This annotation can be removed if you are absolutely sure
   *       that your application never reads the column in a transaction that has written a value to
   *       the column.
   * </ol>
   */
  @Column(insertable = false, columnDefinition = "timestamp options (allow_commit_timestamp=true)")
  @UpdateCommitTimestamp()
  @ColumnTransformer(read = "timestamp '0001-01-01T00:00:00Z'")
  private OffsetDateTime commitTimestampUpdated;

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public OffsetDateTime getCommitTimestampCreated() {
    return commitTimestampCreated;
  }

  public void setCommitTimestampCreated(OffsetDateTime commitTimestampCreated) {
    this.commitTimestampCreated = commitTimestampCreated;
  }

  public OffsetDateTime getCommitTimestampUpdated() {
    return commitTimestampUpdated;
  }

  public void setCommitTimestampUpdated(OffsetDateTime commitTimestampUpdated) {
    this.commitTimestampUpdated = commitTimestampUpdated;
  }
}
