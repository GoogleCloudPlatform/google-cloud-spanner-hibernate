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

import static com.google.cloud.spanner.testing.EmulatorSpannerHelper.isUsingEmulator;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.IntegrationTest;
import com.google.cloud.spanner.hibernate.hints.Hints;
import com.google.cloud.spanner.hibernate.hints.Hints.HashJoinBuildSide;
import com.google.cloud.spanner.hibernate.hints.Hints.HashJoinExecution;
import com.google.cloud.spanner.hibernate.hints.Hints.JoinMethod;
import com.google.cloud.spanner.hibernate.hints.ReplaceQueryPartsHint.ReplaceMode;
import com.google.cloud.spanner.hibernate.it.model.Album;
import com.google.cloud.spanner.hibernate.it.model.AllTypes;
import com.google.cloud.spanner.hibernate.it.model.Concert;
import com.google.cloud.spanner.hibernate.it.model.Singer;
import com.google.cloud.spanner.hibernate.it.model.Track;
import com.google.cloud.spanner.hibernate.it.model.Venue;
import com.google.cloud.spanner.hibernate.it.model.Venue.VenueDescription;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.junit.After;
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

  /**
   * Creates the test database and session factory.
   */
  @BeforeClass
  public static void createDataDatabase() {
    TEST_ENV.createDatabase(ImmutableList.of());
    sessionFactory = TEST_ENV.createTestHibernateConfig(
        ImmutableList.of(
            Singer.class, Album.class, Track.class, Venue.class, Concert.class, AllTypes.class),
        ImmutableMap.of("hibernate.hbm2ddl.auto", "update")).buildSessionFactory();
    try (Session session = sessionFactory.openSession()) {
      final Transaction transaction = session.beginTransaction();
      Singer peter = new Singer("Peter", "Allison");
      session.persist(peter);
      Singer alice = new Singer("Alice", "Peterson");
      session.persist(alice);

      session.persist(new Album(peter, "Album 1"));
      session.persist(new Album(peter, "Album 2"));

      session.persist(new Album(alice, "Album 1"));
      session.persist(new Album(alice, "Album 2"));

      transaction.commit();
    }
  }

  /**
   * Drops the test database.
   */
  @AfterClass
  public static void dropTestDatabase() {
    if (sessionFactory != null) {
      sessionFactory.close();
    }
    TEST_ENV.cleanup();
  }

  /**
   * Clean up any data that the test might have added.
   */
  @After
  public void deleteTestData() {
    try (Session session = sessionFactory.openSession()) {
      final Transaction transaction = session.beginTransaction();
      session.createMutationQuery("delete from Concert "
              + "where not singer.id=:id1 "
              + "  and not singer.id=:id2")
          .setParameter("id1", Long.reverse(50000))
          .setParameter("id2", Long.reverse(50001))
          .executeUpdate();
      session.createMutationQuery("delete from Venue "
              + "where not id in (select venue.id from Concert)")
          .executeUpdate();
      session.createMutationQuery("delete from Track where 1=1").executeUpdate();
      session.createMutationQuery("delete from Album "
              + "where not (singer.id=:id1 or singer.id=:id2) "
              + "or not (title='Album 1' or title='Album 2')")
          .setParameter("id1", Long.reverse(50000))
          .setParameter("id2", Long.reverse(50001))
          .executeUpdate();
      session.createMutationQuery("delete from Singer "
              + "where not id=:id1 "
              + "  and not id=:id2")
          .setParameter("id1", Long.reverse(50000))
          .setParameter("id2", Long.reverse(50001))
          .executeUpdate();

      transaction.commit();
    }
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
      final Transaction transaction = session.beginTransaction();
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
      final Transaction transaction = session.beginTransaction();
      Singer peter = session.get(Singer.class, Long.reverse(50000L));
      Album album = new Album(peter, "Album 3");
      album.setMarketingBudget(new BigDecimal("990429.23"));
      album.setReleaseDate(LocalDate.of(2023, 9, 27));
      album.setCoverPicture(new byte[]{10, 20, 30, 1, 2, 3, 127, 127, 0, 0, -128, -100});
      session.save(album);
      transaction.commit();

      // Reload the album from the database and verify that we read back the same values.
      session.refresh(album);
      assertEquals(peter, album.getSinger());
      assertEquals("Album 3", album.getTitle());
      assertEquals(new BigDecimal("990429.23"), album.getMarketingBudget());
      assertEquals(LocalDate.of(2023, 9, 27), album.getReleaseDate());
      assertArrayEquals(new byte[]{10, 20, 30, 1, 2, 3, 127, 127, 0, 0, -128, -100},
          album.getCoverPicture());
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
      //       Hibernate 5 does not support JSON without additional plugins.
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
      final Transaction transaction = session.beginTransaction();
      Venue venue = new Venue("Venue 2", new VenueDescription());
      session.save(venue);
      Concert concert = new Concert(venue, peter);
      concert.setName("Peter Live!");
      concert.setStartTime(OffsetDateTime.of(LocalDate.of(2023, 9, 26), LocalTime.of(19, 30),
          ZoneOffset.of("+02:00")));
      concert.setEndTime(OffsetDateTime.of(LocalDate.of(2023, 9, 27), LocalTime.of(2, 0),
          ZoneOffset.of("+02:00")));
      session.save(concert);
      transaction.commit();

      session.refresh(concert);
      assertEquals(Instant.from(OffsetDateTime.of(2023, 9, 26, 17, 30, 0, 0, ZoneOffset.UTC)),
          Instant.from(concert.getStartTime()));
      assertEquals(Instant.from(OffsetDateTime.of(2023, 9, 27, 0, 0, 0, 0, ZoneOffset.UTC)),
          Instant.from(concert.getEndTime()));
    }
  }

  @Test
  public void testSingerAlbumAssociation() {
    try (Session session = sessionFactory.openSession()) {
      Singer peter = session.get(Singer.class, Long.reverse(50000L));
      assertNotNull(peter.getAlbums());
      assertEquals(2, peter.getAlbums().size());
      for (Album album : peter.getAlbums()) {
        assertEquals(peter, album.getSinger());
      }

      Singer singer = new Singer("First", "Last");
      // Adding albums in this way does not automatically save the albums as well, as we have not
      // specified any cascade actions on the albums collection.
      singer.setAlbums(new ArrayList<>(
          ImmutableList.of(new Album(singer, "Title 1"), new Album(singer, "Title 2"))));
      final Transaction transaction = session.beginTransaction();
      session.save(singer);
      transaction.commit();

      // Verify that the albums of the singer were not saved.
      session.clear();
      singer = session.get(Singer.class, singer.getId());
      assertNotNull(singer.getAlbums());
      assertTrue(singer.getAlbums().isEmpty());

      // Manually saving the albums as well does work.
      Singer singer2 = new Singer("First", "Last");
      singer2.setAlbums(new ArrayList<>(
          ImmutableList.of(new Album(singer, "Title 1"), new Album(singer, "Title 2"))));
      final Transaction transaction2 = session.beginTransaction();
      session.save(singer2);
      for (Album album : singer2.getAlbums()) {
        session.save(album);
      }
      transaction2.commit();

      singer2 = session.get(Singer.class, singer2.getId());
      assertNotNull(singer2.getAlbums());
      assertEquals(2, singer2.getAlbums().size());
    }
  }

  @Test
  public void testAlbumTracksAssociation() {
    try (Session session = sessionFactory.openSession()) {
      final Transaction transaction = session.beginTransaction();
      Singer peter = session.get(Singer.class, Long.reverse(50000L));

      Album album = new Album(peter, "Album 3");
      // We need to save the album before adding tracks to it, as the tracks must use the same id
      // as the album (Track is INTERLEAVED IN PARENT Album).
      session.save(album);
      album.setTracks(ImmutableList.of(
          new Track(album, 1L, "Track 1"),
          new Track(album, 2L, "Track 2")
      ));
      for (Track track : album.getTracks()) {
        session.save(track);
      }
      transaction.commit();

      // Reload the album from the database.
      session.clear();
      Album album2 = session.get(Album.class, album.getId());
      assertEquals(2, album2.getTracks().size());

      // Verify that we can delete an album with tracks, as Cloud Spanner will cascade-delete the
      // tracks of an album. Hibernate does not know about this.
      Transaction deleteTransaction = session.beginTransaction();
      session.delete(album2);
      deleteTransaction.commit();

      // The album should no longer be present in the database.
      session.clear();
      assertNull(session.get(Album.class, album.getId()));
    }
  }

  @Test
  public void testVenueConcertAssociation() {
    try (Session session = sessionFactory.openSession()) {
      // Do not use a transaction on the emulator. The reason for this is that the cascade-save that
      // is triggered when saving a Venue with multiple Concerts requires two parallel transactions:
      // 1. Our 'business' transaction.
      // 2. One internal transaction that is started by Hibernate to generate ID values for the
      //    concerts.
      // The emulator does not support parallel transactions.
      final Transaction transaction = isUsingEmulator()
          ? null
          : session.beginTransaction();
      Singer peter = session.get(Singer.class, Long.reverse(50000L));
      Venue venue = new Venue("Concert Hall", new VenueDescription());
      venue.setConcerts(ImmutableList.of(
          new Concert(venue, peter, "Concert 1",
              OffsetDateTime.of(LocalDate.of(2023, 9, 29), LocalTime.of(20, 0),
                  ZoneOffset.of("+02")),
              OffsetDateTime.of(LocalDate.of(2023, 9, 30), LocalTime.of(1, 30),
                  ZoneOffset.of("+02"))),
          new Concert(venue, peter, "Concert 1",
              OffsetDateTime.of(LocalDate.of(2023, 10, 3), LocalTime.of(15, 0),
                  ZoneOffset.of("+02")),
              OffsetDateTime.of(LocalDate.of(2023, 10, 3), LocalTime.of(20, 0),
                  ZoneOffset.of("+02")))
      ));
      // This should also cascade-save all the concerts of the venue, as the association is defined
      // with cascade = CascadeType.ALL.
      session.save(venue);
      if (transaction == null) {
        // Create a transaction here if we are using the emulator. The fact that we 'saved' the
        // venue outside of this transaction is not a problem for Hibernate. It will still include
        // the save action in this transaction.
        session.beginTransaction().commit();
      } else {
        transaction.commit();
      }

      session.clear();
      Venue venue2 = session.get(Venue.class, venue.getId());
      assertEquals(2, venue2.getConcerts().size());

      // Verify that we can delete a venue with concerts, because we have set cascade=ALL on the
      // association. Note that the cascade-delete is managed by Hibernate. It is not something that
      // is done by the database, for example as a result of the foreign key constraint having a
      // cascade-delete clause.
      session.clear();
      Transaction deleteTransaction = session.beginTransaction();
      session.delete(venue2);
      deleteTransaction.commit();

      session.clear();
      assertNull(session.get(Venue.class, venue.getId()));
    }
  }

  @Test
  public void testAllTypes() {
    try (Session session = sessionFactory.openSession()) {
      final Transaction transaction = session.beginTransaction();
      AllTypes saved = new AllTypes();
      saved.setId(1L);
      saved.setColBool(true);
      saved.setColBytes("test".getBytes(StandardCharsets.UTF_8));
      saved.setColDate(LocalDate.of(2024, 2, 2));
      saved.setColFloat64(3.14d);
      saved.setColInt64(100L);
      saved.setColJson("{\"key\":\"value\"}");
      saved.setColNumeric(new BigDecimal("6.626"));
      saved.setColString("test");
      saved.setColTimestamp(Instant.ofEpochMilli(1000L));
      saved.setColBoolArray(Arrays.asList(Boolean.TRUE, null, Boolean.FALSE));
      saved.setColBytesArray(
          Arrays.asList("test1".getBytes(StandardCharsets.UTF_8), null, "test2".getBytes(
              StandardCharsets.UTF_8)));
      saved.setColDateArray(
          Arrays.asList(LocalDate.of(2000, 1, 1), null, LocalDate.of(1970, 1, 1)));
      saved.setColFloat64Array(Arrays.asList(3.14d, null, 6.626d));
      saved.setColInt64Array(Arrays.asList(1L, null, 2L));
      saved.setColJsonArray(Arrays.asList("{\"key\":\"value1\"}", null, "{\"key\":\"value2\"}"));
      saved.setColNumericArray(Arrays.asList(BigDecimal.ZERO, null, BigDecimal.TEN));
      saved.setColStringArray(Arrays.asList("test1", null, "test2"));
      saved.setColTimestampArray(
          Arrays.asList(Instant.ofEpochMilli(349078), null, Instant.ofEpochMilli(239587215)));

      session.persist(saved);
      transaction.commit();
      session.clear();

      // Verify that we can read it back and that the values are correct.
      AllTypes fetched = session.get(AllTypes.class, 1L);

      assertEquals(saved.getId(), fetched.getId());
      assertEquals(saved.getColBool(), fetched.getColBool());
      assertArrayEquals(saved.getColBytes(), fetched.getColBytes());
      assertEquals(saved.getColDate(), fetched.getColDate());
      assertEquals(saved.getColFloat64(), fetched.getColFloat64());
      assertEquals(saved.getColInt64(), fetched.getColInt64());
      assertEquals(saved.getColJson(), fetched.getColJson());
      assertEquals(saved.getColNumeric(), fetched.getColNumeric());
      assertEquals(saved.getColString(), fetched.getColString());
      assertEquals(saved.getColTimestamp(), fetched.getColTimestamp());

      assertEquals(saved.getColBoolArray(), fetched.getColBoolArray());
      for (int i = 0; i < saved.getColBytesArray().size(); i++) {
        assertArrayEquals(saved.getColBytesArray().get(i), fetched.getColBytesArray().get(i));
      }
      assertEquals(saved.getColDateArray(), fetched.getColDateArray());
      assertEquals(saved.getColFloat64Array(), fetched.getColFloat64Array());
      assertEquals(saved.getColInt64Array(), fetched.getColInt64Array());
      assertEquals(saved.getColJsonArray(), fetched.getColJsonArray());
      assertEquals(saved.getColNumericArray(), fetched.getColNumericArray());
      assertEquals(saved.getColStringArray(), fetched.getColStringArray());
      assertEquals(saved.getColTimestampArray(), fetched.getColTimestampArray());
    }
  }

  @Test
  public void testHints() {
    try (Session session = sessionFactory.openSession()) {
      CriteriaBuilder cb = session.getCriteriaBuilder();
      CriteriaQuery<Singer> cr = cb.createQuery(Singer.class);
      Root<Singer> root = cr.from(Singer.class);
      root.join("albums", JoinType.LEFT);
      cr.select(root);
      Query<Singer> query = session.createQuery(cr)
          .addQueryHint(
              Hints.forceIndexFrom("Singer", "idx_singer_active", ReplaceMode.ALL).toQueryHint())
          .addQueryHint(
              Hints.forceIndexJoin("Album", "idx_album_title", ReplaceMode.ALL).toQueryHint());
      assertEquals(2, query.getResultList().size());

      // Verify that adding a hint for a non-existing index fails.
      Query<Singer> invalidQuery = session.createQuery(cr).addQueryHint(
          Hints.forceIndexFrom("Singer", "idx_does_not_exist", ReplaceMode.ALL).toQueryHint());
      HibernateException exception = assertThrows(HibernateException.class,
          invalidQuery::getResultList);
      assertTrue(exception.getMessage(), exception.getMessage()
          .contains("does not have a secondary index called idx_does_not_exist"));

      Query<Singer> joinMethodQuery = session.createQuery(cr).addQueryHint(
          Hints.joinMethod("Album", JoinMethod.MERGE_JOIN, ReplaceMode.ALL)
              .toQueryHint());
      assertEquals(2, joinMethodQuery.getResultList().size());

      // Verify that adding combined hints works.
      Query<Singer> hashJoinExecutionQuery = session.createQuery(cr).addQueryHint(
          Hints.hashJoinExecution("Album", HashJoinExecution.ONE_PASS, ReplaceMode.ALL)
              .toQueryHint());
      assertEquals(2, hashJoinExecutionQuery.getResultList().size());

      if (!isUsingEmulator()) {
        Query<Singer> forceStreamableQuery = session.createQuery(cr).addQueryHint(
            Hints.forceStreamable(true).toQueryHint());
        assertEquals(2, forceStreamableQuery.getResultList().size());

        Query<Singer> optimizerVersionQuery = session.createQuery(cr).addQueryHint(
            Hints.optimizerVersion("1").toQueryHint());
        assertEquals(2, optimizerVersionQuery.getResultList().size());

        Query<Singer> allowDistributedMergeQuery = session.createQuery(cr).addQueryHint(
            Hints.allowDistributedMerge(true).toQueryHint());
        assertEquals(2, allowDistributedMergeQuery.getResultList().size());

        Query<Singer> hashJoinBuildSideQuery = session.createQuery(cr).addQueryHint(
            Hints.hashJoinBuildSide("Album", HashJoinBuildSide.BUILD_RIGHT, ReplaceMode.ALL)
                .toQueryHint());
        assertEquals(2, hashJoinBuildSideQuery.getResultList().size());

        Query<Singer> hashJoinQuery = session.createQuery(cr).addQueryHint(
            Hints.hashJoin(
                "Album",
                    HashJoinBuildSide.BUILD_RIGHT,
                    HashJoinExecution.ONE_PASS,
                    ReplaceMode.ALL)
                .toQueryHint());
        assertEquals(2, hashJoinQuery.getResultList().size());
      }
    }
  }
}
