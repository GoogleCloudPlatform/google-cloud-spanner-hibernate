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

package com.google.cloud.spanner.sample.service;

import com.google.cloud.spanner.sample.entities.Concert;
import com.google.cloud.spanner.sample.entities.Singer;
import com.google.cloud.spanner.sample.entities.Venue;
import com.google.cloud.spanner.sample.repository.ConcertRepository;
import com.google.cloud.spanner.sample.repository.SingerRepository;
import com.google.cloud.spanner.sample.repository.VenueRepository;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Service class for fetching and saving Concert records. */
@Service
public class ConcertService {

  private final ConcertRepository repository;

  private final SingerRepository singerRepository;

  private final VenueRepository venueRepository;

  private final RandomDataService randomDataService;

  /** Constructor with auto-injected dependencies. */
  public ConcertService(
      ConcertRepository repository,
      SingerRepository singerRepository,
      VenueRepository venueRepository,
      RandomDataService randomDataService) {
    this.repository = repository;
    this.singerRepository = singerRepository;
    this.venueRepository = venueRepository;
    this.randomDataService = randomDataService;
  }

  /** Deletes all Concert records in the database. */
  @Transactional
  public void deleteAllConcerts() {
    repository.deleteAll();
  }

  /** Generates the specified number of random Concert records. */
  @Transactional
  public List<Concert> generateRandomConcerts(int count) {
    Random random = new Random();

    List<Singer> singers = singerRepository.findAll(Pageable.ofSize(20)).toList();
    List<Venue> venues = venueRepository.findAll();
    List<Concert> concerts = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      Concert concert = new Concert();
      concert.setName(randomDataService.getRandomConcertName());
      concert.setSinger(singers.get(random.nextInt(singers.size())));
      concert.setVenue(venues.get(random.nextInt(venues.size())));
      concert.setStartTime(
          OffsetDateTime.of(
              random.nextInt(30) + 1995,
              random.nextInt(12) + 1,
              random.nextInt(28) + 1,
              random.nextInt(24),
              random.nextBoolean() ? 0 : 30,
              0,
              0,
              ZoneOffset.ofHours(random.nextInt(24) - 12)));
      concert.setEndTime(concert.getStartTime().plus(Duration.ofHours(random.nextInt(6) + 1)));
      concerts.add(concert);
    }
    return repository.saveAll(concerts);
  }
}
