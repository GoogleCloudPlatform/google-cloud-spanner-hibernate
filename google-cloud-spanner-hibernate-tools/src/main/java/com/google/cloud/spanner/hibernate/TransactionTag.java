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

package com.google.cloud.spanner.hibernate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for adding transaction tags to Hibernate transactions.
 *
 * <p>Usage:
 * <ol>
 *   <li>Add the {@link TransactionTagInterceptor} to your Hibernate configuration.</li>
 *   <li>Add the {@link TransactionTag} annotation to a method that is also tagged with
 *   {@link jakarta.transaction.Transactional}.</li>
 * </ol>
 *
 * <p>Example:
 *
 * <pre>{@code
 * // Add TransactionTagInterceptor to the Hibernate configuration.
 * @Component
 * public class TaggingHibernatePropertiesCustomizer implements HibernatePropertiesCustomizer {
 *   @Override
 *   public void customize(Map<String, Object> hibernateProperties) {
 *     hibernateProperties.put(AvailableSettings.INTERCEPTOR, new TransactionTagInterceptor(
 *         ImmutableSet.of(SampleApplication.class.getPackageName()), false));
 *   }
 * }
 *
 * @Service
 * public class VenueService {
 *   @Transactional
 *   @TransactionTag("generate_random_venues")
 *   public List<Venue> generateRandomVenues(int count) {
 *     // Code that is executed in a transaction...
 *   }
 * }
 * }</pre>
 *
 * <p>See <a href="https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/blob/-/google-cloud-spanner-hibernate-samples/spring-data-jpa-full-sample">
 * Spring Data JPA Full Sample</a> for a working sample application.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TransactionTag {

  /**
   * The transaction tag value. Max length is 50 characters.
   * See <a href="https://cloud.google.com/spanner/docs/introspection/troubleshooting-with-tags#limitations">
   *   Limitations</a> for all limitations on transaction tag values.
   */
  String value();
}
