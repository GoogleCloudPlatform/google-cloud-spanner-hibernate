/*
 * Copyright 2019-2024 Google LLC
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

package com.google.cloud.spanner.sample;

import java.lang.reflect.Member;
import java.util.EnumSet;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;

/** Generator for pending_commit_timestamp(). */
public class CommitTimestampGeneration implements OnExecutionGenerator {
  private final EnumSet<EventType> eventTypes;

  public CommitTimestampGeneration(
      CreationCommitTimestamp annotation, Member member, GeneratorCreationContext context) {
    this.eventTypes = EventTypeSets.INSERT_ONLY;
  }

  public CommitTimestampGeneration(
      UpdateCommitTimestamp annotation, Member member, GeneratorCreationContext context) {
    this.eventTypes = EventTypeSets.UPDATE_ONLY;
  }

  @Override
  public boolean referenceColumnsInSql(Dialect dialect) {
    return true;
  }

  @Override
  public boolean writePropertyValue() {
    return false;
  }

  @Override
  public String[] getReferencedColumnValues(Dialect dialect) {
    return new String[] {"pending_commit_timestamp()"};
  }

  @Override
  public EnumSet<EventType> getEventTypes() {
    return eventTypes;
  }
}
