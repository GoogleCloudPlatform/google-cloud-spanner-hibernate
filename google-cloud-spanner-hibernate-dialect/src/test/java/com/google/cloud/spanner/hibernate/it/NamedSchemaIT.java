/*
 * Copyright 2019-2025 Google LLC
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

package com.google.cloud.spanner.hibernate.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.cloud.spanner.IntegrationTest;
import com.google.cloud.spanner.hibernate.annotations.PooledBitReversedSequenceGenerator;
import com.google.cloud.spanner.hibernate.it.model.Album;
import com.google.cloud.spanner.hibernate.it.model.Concert;
import com.google.cloud.spanner.hibernate.it.model.Singer;
import com.google.cloud.spanner.hibernate.it.model.TicketSale;
import com.google.cloud.spanner.hibernate.it.model.Track;
import com.google.cloud.spanner.hibernate.it.model.Venue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(IntegrationTest.class)
@RunWith(JUnit4.class)
public class NamedSchemaIT {

  private static final HibernateIntegrationTestEnv TEST_ENV = new HibernateIntegrationTestEnv();

  // Entities without a schema annotation will default to this schema
  private static final String DEFAULT_SCHEMA = "shop";

  @BeforeClass
  public static void setup() {
    TEST_ENV.createDatabase(ImmutableList.of());

    try (SessionFactory factory =
        TEST_ENV
            .createTestHibernateConfig(
                ImmutableList.of(
                    Singer.class,
                    Album.class,
                    Track.class,
                    Venue.class,
                    Concert.class,
                    TicketSale.class,
                    AuditLog.class),
                ImmutableMap.of(
                    AvailableSettings.HBM2DDL_AUTO, "create",
                    AvailableSettings.SHOW_SQL, "true",
                    AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, "true",
                    AvailableSettings.DEFAULT_SCHEMA, DEFAULT_SCHEMA))
            .buildSessionFactory()) {}
  }

  @AfterClass
  public static void cleanup() {
    TEST_ENV.cleanup();
  }

  @Test
  public void testDomainModelWithNamedSchema() {
    try (SessionFactory factory =
        TEST_ENV
            .createTestHibernateConfig(
                ImmutableList.of(
                    Singer.class,
                    Album.class,
                    Track.class,
                    Venue.class,
                    Concert.class,
                    TicketSale.class,
                    AuditLog.class),
                ImmutableMap.of(
                    AvailableSettings.HBM2DDL_AUTO, "create",
                    AvailableSettings.SHOW_SQL, "true",
                    AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, "true",
                    AvailableSettings.DEFAULT_SCHEMA, DEFAULT_SCHEMA))
            .buildSessionFactory()) {

      factory.inTransaction(
          session -> {
            Singer singer = new Singer("John", "Doe");
            session.persist(singer);

            Album album = new Album(singer, "Spanner Hits");
            album.setMarketingBudget(new java.math.BigDecimal("1000.00"));
            session.persist(album);

            Venue venue = new Venue("Cloud Arena", new Venue.VenueDescription());
            venue.getDescription().setLocation("San Francisco");
            venue.getDescription().setCapacity(5000);
            session.persist(venue);

            Concert concert =
                new Concert(
                    venue,
                    singer,
                    "Winter Tour",
                    OffsetDateTime.now(),
                    OffsetDateTime.now().plusHours(3));
            session.persist(concert);

            TicketSale ticket = new TicketSale(concert, "Alice User");
            session.persist(ticket);

            // --- Named Schema Entity (reporting) ---
            AuditLog log = new AuditLog("Created new concert for " + singer.getFullName(), "INFO");
            session.persist(log);
          });

      factory.inTransaction(
          session -> {
            List<Album> albums =
                session
                    .createQuery(
                        "select a from Album a join a.singer s where s.lastName = :lname",
                        Album.class)
                    .setParameter("lname", "Doe")
                    .getResultList();

            assertEquals(1, albums.size());
            assertEquals("Spanner Hits", albums.get(0).getTitle());
            assertNotNull("Singer relationship should be fetched", albums.get(0).getSinger());

            List<TicketSale> tickets =
                session
                    .createQuery(
                        "select t from TicketSale t "
                            + "join fetch t.concert c "
                            + "join fetch c.venue v "
                            + "where v.name = :venueName",
                        TicketSale.class)
                    .setParameter("venueName", "Cloud Arena")
                    .getResultList();

            assertEquals(1, tickets.size());
            assertEquals("Alice User", tickets.get(0).getCustomerName());

            List<AuditLog> logs =
                session
                    .createQuery("from AuditLog a where a.level = :level", AuditLog.class)
                    .setParameter("level", "INFO")
                    .getResultList();

            assertEquals(1, logs.size());
            assertEquals("INFO", logs.get(0).getLevel());
          });
    }
  }

  // --- Extra Entity for Schema Testing ---

  @Entity(name = "AuditLog")
  @Table(
      name = "audit_logs",
      schema = "reporting",
      indexes = @Index(name = "idx_audit_level", columnList = "level"))
  static class AuditLog {
    @Id
    @PooledBitReversedSequenceGenerator(
        sequenceName = "audit_log_id_sequence",
        schema = "reporting")
    private Long id;

    private String message;

    private String level;

    public AuditLog() {}

    public AuditLog(String message, String level) {
      this.message = message;
      this.level = level;
    }

    public Long getId() {
      return id;
    }

    public String getMessage() {
      return message;
    }

    public String getLevel() {
      return level;
    }
  }
}
