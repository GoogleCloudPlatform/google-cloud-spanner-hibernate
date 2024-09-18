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

import static org.hibernate.generator.EventType.INSERT;

import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.generator.EventType;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.internal.GeneratedValuesProcessor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.SingleTableEntityPersister;

/**
 * This custom persister ensures two things:
 *
 * <ol>
 *   <li>Hibernate does not execute a SELECT after inserting/updating an entity with a commit
 *       timestamp column to fetch the generated value. This value can only be fetched after the
 *       transaction has committed.
 *   <li>Batching of inserts is still supported, even though the entity has a generated commit
 *       timestamp.
 * </ol>
 */
public class SingleTableWithCommitTimestampEntityPersister extends SingleTableEntityPersister {

  public SingleTableWithCommitTimestampEntityPersister(
      PersistentClass persistentClass,
      EntityDataAccess cacheAccessStrategy,
      NaturalIdDataAccess naturalIdRegionAccessStrategy,
      RuntimeModelCreationContext creationContext)
      throws HibernateException {
    super(persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext);
  }

  @Override
  protected GeneratedValuesProcessor createGeneratedValuesProcessor(
      EventType timing, List<AttributeMapping> generatedAttributes) {
    // Skip all commit timestamp generated attributes, as these are not selectable.
    // This is what prevents the commit timestamp column from being selected directly after being
    // updated.
    return new GeneratedValuesProcessor(
        this,
        getGeneratedAttributesWithoutCommitTimestamp(generatedAttributes),
        timing,
        getFactory());
  }

  List<AttributeMapping> getGeneratedAttributesWithoutCommitTimestamp(
      List<AttributeMapping> attributes) {
    return attributes.stream()
        .filter(attribute -> !(attribute.getGenerator() instanceof CommitTimestampGeneration))
        .collect(Collectors.toList());
  }

  @Override
  public boolean hasInsertGeneratedProperties() {
    List<AttributeMapping> generatedAttributes =
        GeneratedValuesProcessor.getGeneratedAttributes(this, INSERT);
    return super.hasInsertGeneratedProperties()
        && !getGeneratedAttributesWithoutCommitTimestamp(generatedAttributes).isEmpty();
  }
}
