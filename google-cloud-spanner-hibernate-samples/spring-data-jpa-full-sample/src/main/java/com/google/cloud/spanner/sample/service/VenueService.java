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

import com.google.cloud.spanner.hibernate.TransactionTag;
import com.google.cloud.spanner.sample.entities.Venue;
import com.google.cloud.spanner.sample.entities.Venue.VenueDescription;
import com.google.cloud.spanner.sample.repository.VenueRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

/** Service class for fetching and saving Venue records. */
@Service
public class VenueService {

  private final VenueRepository repository;

  private final RandomDataService randomDataService;

  /** Constructor with auto-injected dependencies. */
  public VenueService(VenueRepository repository, RandomDataService randomDataService) {
    this.repository = repository;
    this.randomDataService = randomDataService;
  }

  /** Deletes all Venue records in the database. */
  @Transactional
  public void deleteAllVenues() {
    repository.deleteAll();
  }

  /**
   * Generates the specified number of random Venue records.
   *
   * <p>The {@link TransactionTag} annotation adds a transaction tag to the read/write transaction
   * and all the statements that are executed in this transaction. This tag only works if you also
   * add a {@link com.google.cloud.spanner.hibernate.TransactionTagInterceptor} to your Hibernate
   * configuration. See {@link com.google.cloud.spanner.sample.TaggingHibernatePropertiesCustomizer}
   * for how this is done in this sample application.
   */
  @Transactional
  @TransactionTag("generate_random_venues")
  public List<Venue> generateRandomVenues(int count) {
    Random random = new Random();

    List<Venue> venues = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      Venue venue = new Venue();
      venue.setName(randomDataService.getRandomVenueName());
      if (random.nextBoolean()) {
        VenueDescription description = new VenueDescription();
        description.setCapacity(random.nextInt(100_000));
        description.setType(randomDataService.getRandomVenueType());
        description.setLocation(randomDataService.getRandomVenueLocation());
        venue.setDescription(description);
      } else {
        venue.setDescription(null);
      }
      venues.add(venue);
    }
    return repository.saveAll(venues);
  }
}
