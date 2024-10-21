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

package com.google.cloud.spanner.sample.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BatchService {

  private static final Logger log = LoggerFactory.getLogger(BatchService.class);

  private final SingerService singerService;
  private final AlbumService albumService;
  private final TrackService trackService;
  private final VenueService venueService;
  private final ConcertService concertService;
  private final TicketSaleService ticketSaleService;
  @PersistenceContext
  private EntityManager entityManager;

  public BatchService(SingerService singerService, AlbumService albumService,
      TrackService trackService, VenueService venueService, ConcertService concertService,
      TicketSaleService ticketSaleService) {
    this.singerService = singerService;
    this.albumService = albumService;
    this.trackService = trackService;
    this.venueService = venueService;
    this.concertService = concertService;
    this.ticketSaleService = ticketSaleService;
  }

  private void runWithAutoBatchDml(Runnable runnable) {
    // Enable auto_batch_dml on the JDBC connection and run the runnable.
    // Then flush the Hibernate session and reset the JDBC connection.
    Session session = entityManager.unwrap(Session.class);
    try {
      session.doWork(connection -> connection.createStatement().execute("set auto_batch_dml=true"));
      runnable.run();
      session.flush();
    } finally {
      session.doWork(
          connection -> connection.createStatement().execute("set auto_batch_dml=false"));
    }
  }

  @Transactional
  public void deleteAllData() {
    log.info("Deleting all existing data");
    runWithAutoBatchDml(() -> {
      ticketSaleService.deleteAllTicketSales();
      concertService.deleteAllConcerts();
      albumService.deleteAllAlbums();
      singerService.deleteAllSingers();
    });
  }

  @Transactional
  public void generateRandomData() {
    runWithAutoBatchDml(() -> {
      singerService.generateRandomSingers(10);
      log.info("Created 10 singers");
      albumService.generateRandomAlbums(30);
      log.info("Created 30 albums");
      trackService.generateRandomTracks(30, 15);
      log.info("Created 20 tracks each for 30 albums");
      venueService.generateRandomVenues(20);
      log.info("Created 20 venues");
      concertService.generateRandomConcerts(50);
      log.info("Created 50 concerts");
      ticketSaleService.generateRandomTicketSales(250);
      log.info("Created 250 ticket sales");
    });
  }

}
