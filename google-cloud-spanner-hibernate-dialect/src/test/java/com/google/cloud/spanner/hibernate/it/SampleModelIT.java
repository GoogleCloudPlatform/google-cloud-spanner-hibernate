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

package com.google.cloud.spanner.hibernate.it;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.IntegrationTest;
import com.google.cloud.spanner.hibernate.it.model.Album;
import com.google.cloud.spanner.hibernate.it.model.Concert;
import com.google.cloud.spanner.hibernate.it.model.Singer;
import com.google.cloud.spanner.hibernate.it.model.Track;
import com.google.cloud.spanner.hibernate.it.model.Venue;
import com.google.cloud.spanner.hibernate.it.model.Venue.VenueDescription;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration test using the standard sample data model using all data types and all supported
 * features of Cloud Spanner.
 */
@Category(IntegrationTest.class)
@RunWith(JUnit4.class)
public class SampleModelIT {

  private static final HibernateIntegrationTestEnv TEST_ENV = new HibernateIntegrationTestEnv();

  private static SessionFactory sessionFactory;

  /** Creates the test database and session factory. */
  @BeforeClass
  public static void createDataDatabase() {
    TEST_ENV.createDatabase(ImmutableList.of());
    sessionFactory = TEST_ENV.createTestHibernateConfig(
        ImmutableList.of(Singer.class, Album.class, Track.class, Venue.class, Concert.class),
        ImmutableMap.of("hibernate.hbm2ddl.auto", "update")).buildSessionFactory();
    try (Session session = sessionFactory.openSession()) {
      final Transaction transaction = session.beginTransaction();
      Singer peter = new Singer("Peter", "Allison");
      session.save(peter);
      Singer alice = new Singer("Alice", "Peterson");
      session.save(alice);

      session.save(new Album(peter, "Album 1"));
      session.save(new Album(peter, "Album 2"));

      session.save(new Album(alice, "Album 1"));
      session.save(new Album(alice, "Album 2"));

      transaction.commit();
    }
  }

  /** Drops the test database. */
  @AfterClass
  public static void cleanup() {
    if (sessionFactory != null) {
      sessionFactory.close();
    }
    TEST_ENV.cleanup();
  }

  @Test
  public void testGetSinger() {
    try (Session session = sessionFactory.openSession()) {
      // The id generator should start counting at 50000 and the values should be bit-reversed.
      assertNull(session.get(Singer.class, 1L));
      assertNull(session.get(Singer.class, 50000L));
      Singer singer = session.get(Singer.class, Long.reverse(50000L));
      assertNotNull(singer);
      assertEquals("Peter", singer.getFirstName());
      assertEquals("Allison", singer.getLastName());
      assertEquals("Peter Allison", singer.getFullName());
      assertNull(singer.getBirthDate());
      assertTrue(singer.isActive());
      assertNotNull(singer.getCreatedAt());
      assertNotNull(singer.getUpdatedAt());
    }
  }

  @Test
  public void testSaveSinger() {
    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      Singer singer1 = new Singer("singer1", "singer1");
      singer1.setBirthDate(LocalDate.of(1998, 10, 12));
      assertNotNull(session.save(singer1));
      session.flush();
      assertEquals("singer1 singer1", singer1.getFullName());
      assertEquals(LocalDate.of(1998, 10, 12), singer1.getBirthDate());

      Singer singer2 = new Singer(null, "singer2");
      assertNotNull(session.save(singer2));
      session.flush();
      assertEquals("singer2", singer2.getFullName());

      Singer singer3 = new Singer("singer3", null);
      assertNotNull(session.save(singer3));
      session.flush();
      assertEquals("singer3", singer3.getFullName());

      // This should not be allowed, as either firstName or lastName must be not null, but Hibernate
      // does not generate the check constraint.
      // TODO: Revisit this in Hibernate 6.
      Singer singer4 = new Singer(null, null);
      session.save(singer4);
      session.flush();
      assertNull(singer4.getFullName());

      transaction.commit();
    }
  }

  @Test
  public void testSingerGetAlbums() {
    try (Session session = sessionFactory.openSession()) {
      Singer peter = session.get(Singer.class, Long.reverse(50000L));
      assertNotNull(peter.getAlbums());
      assertEquals(2, peter.getAlbums().size());
    }
  }

  @Test
  public void testSaveAlbum() {
    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      Singer peter = session.get(Singer.class, Long.reverse(50000L));
      Album album = new Album(peter, "Album 3");
      album.setMarketingBudget(new BigDecimal("990429.23"));
      album.setReleaseDate(LocalDate.of(2023, 9, 27));
      album.setCoverPicture(new byte[] {10, 20, 30, 1, 2, 3, 127, 127, 0, 0, -128, -100});
      session.save(album);
      transaction.commit();

      // Reload the album from the database and verify that we read back the same values.
      session.refresh(album);
      assertEquals(peter, album.getSinger());
      assertEquals("Album 3", album.getTitle());
      assertEquals(new BigDecimal("990429.23"), album.getMarketingBudget());
      assertEquals(LocalDate.of(2023, 9, 27), album.getReleaseDate());
      assertArrayEquals(new byte[] {10, 20, 30, 1, 2, 3, 127, 127, 0, 0, -128, -100}, album.getCoverPicture());
    }
  }

  @Test
  public void testSaveTrack() {
    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      Singer peter = session.get(Singer.class, Long.reverse(50000L));
      Album album = peter.getAlbums().get(0);
      Track track = new Track(album, 1, "Track 1");
      track.setSampleRate(3.14d);
      session.save(track);
      transaction.commit();

      // Reload the album from the database and verify that we read back the same values.
      session.refresh(track);
      assertEquals(album, track.getAlbum());
      assertEquals(peter, track.getAlbum().getSinger());
      assertEquals(1L, track.getTrackId().getTrackNumber());
      assertEquals("Track 1", track.getTitle());
      assertEquals(3.14d, track.getSampleRate(), 0.0d);
    }
    // Test associations.
    try (Session session = sessionFactory.openSession()) {
      Singer peter = session.get(Singer.class, Long.reverse(50000L));
      Album album = peter.getAlbums().get(0);
      assertNotNull(album.getTracks());
      assertEquals(1, album.getTracks().size());
    }
  }

  @Test
  public void testSaveVenue() {
    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      // TODO: Set VenueDescription fields and verify these in Hibernate 6.
      Venue venue = new Venue("Venue 1", new VenueDescription());
      session.save(venue);
      transaction.commit();

      session.refresh(venue);
      assertEquals("Venue 1", venue.getName());
    }
  }

  @Test
  public void testSaveConcert() {
    try (Session session = sessionFactory.openSession()) {
      Singer peter = session.get(Singer.class, Long.reverse(50000L));
      Transaction transaction = session.beginTransaction();
      Venue venue = new Venue("Venue 2", new VenueDescription());
      session.save(venue);
      Concert concert = new Concert(venue, peter);
      concert.setName("Peter Live!");
      concert.setStartTime(OffsetDateTime.of(LocalDate.of(2023, 9, 26), LocalTime.of(19, 30), ZoneOffset.of("+02:00")));
      concert.setEndTime(OffsetDateTime.of(LocalDate.of(2023, 9, 27), LocalTime.of(2, 0), ZoneOffset.of("+02:00")));
      session.save(concert);
      transaction.commit();

      session.refresh(concert);
      assertEquals(Instant.from(OffsetDateTime.of(2023, 9, 26, 17, 30, 0, 0, ZoneOffset.UTC)), Instant.from(concert.getStartTime()));
      assertEquals(Instant.from(OffsetDateTime.of(2023, 9, 27, 0, 0, 0, 0, ZoneOffset.UTC)), Instant.from(concert.getEndTime()));
    }
  }

}
