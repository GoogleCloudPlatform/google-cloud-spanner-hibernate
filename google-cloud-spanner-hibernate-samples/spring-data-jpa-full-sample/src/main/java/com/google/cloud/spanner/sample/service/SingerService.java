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
import com.google.cloud.spanner.sample.entities.Album;
import com.google.cloud.spanner.sample.entities.Concert;
import com.google.cloud.spanner.sample.entities.Singer;
import com.google.cloud.spanner.sample.entities.Track;
import com.google.cloud.spanner.sample.repository.SingerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Service class for fetching and saving Singer records. */
@Service
public class SingerService {

  private static final Logger log = LoggerFactory.getLogger(SingerService.class);

  private final RandomDataService randomDataService;

  private final SingerRepository repository;

  @PersistenceContext private EntityManager entityManager;

  /** Constructor with auto-injected dependencies. */
  public SingerService(RandomDataService randomDataService, SingerRepository repository) {
    this.randomDataService = randomDataService;
    this.repository = repository;
  }

  public List<Singer> getActiveSingers() {
    return repository.findByActive(true);
  }

  /**
   * Prints all singers whose last name start with the given prefix. Also prints the related albums
   * and tracks.
   *
   * <p>This method uses a read-only transaction. It is highly recommended to use a read-only
   * transaction for workloads that only read, as these do not take locks on Cloud Spanner.
   */
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public void printSingersWithLastNameStartingWith(String prefix) {
    log.info("Fetching all singers whose last name start with an '{}'", prefix);
    repository
        .searchByLastNameStartsWith(prefix)
        .forEach(
            singer -> {
              log.info("Singer: {}", singer.getFullName());
              log.info("# albums: {}", singer.getAlbums().size());
              for (Album album : singer.getAlbums()) {
                log.info("  Album: {}", album.getTitle());
                log.info("  # tracks: {}", album.getTracks().size());
                for (Track track : album.getTracks()) {
                  log.info(
                      "    Track #{}: {}", track.getTrackId().getTrackNumber(), track.getTitle());
                }
              }
              log.info("# concerts: {}", singer.getConcerts().size());
              for (Concert concert : singer.getConcerts()) {
                log.info("  Concert: {} starts at {}", concert.getName(), concert.getStartTime());
              }
            });
  }

  /** Deletes all singer records in the database. */
  @Transactional
  public void deleteAllSingers() {
    repository.deleteAll();
  }

  /** Generates the specified number of random singer records. */
  @Transactional
  @TransactionTag("generate_random_singers")
  public List<Singer> generateRandomSingers(int count) {
    List<Singer> singers = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      Singer singer = new Singer();
      singer.setFirstName(randomDataService.getRandomFirstName());
      singer.setLastName(randomDataService.getRandomLastName());
      singer.setNickNames(randomDataService.getRandomNickNames());
      singers.add(singer);
    }
    return repository.saveAll(singers);
  }
}
