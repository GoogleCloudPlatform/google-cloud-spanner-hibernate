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
import com.google.cloud.spanner.sample.entities.Singer;
import com.google.cloud.spanner.sample.repository.AlbumRepository;
import com.google.cloud.spanner.sample.repository.SingerRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Service class for fetching and saving Album records. */
@Service
public class AlbumService {

  private final RandomDataService randomDataService;

  private final AlbumRepository albumRepository;

  private final SingerRepository singerRepository;

  /** Constructor with auto-injected dependencies. */
  public AlbumService(
      RandomDataService randomDataService,
      AlbumRepository albumRepository,
      SingerRepository singerRepository) {
    this.randomDataService = randomDataService;
    this.albumRepository = albumRepository;
    this.singerRepository = singerRepository;
  }

  public List<Album> getAlbums(String title) {
    return this.albumRepository.getAlbumsByTitle(title);
  }

  /** Deletes all Album records in the database. */
  @Transactional
  public void deleteAllAlbums() {
    albumRepository.deleteAll();
  }

  /** Generates the specified number of random Album records. */
  @Transactional
  @TransactionTag("generate_random_albums")
  public List<Album> generateRandomAlbums(int count) {
    Random random = new Random();

    // Get the first 20 singers and link the albums to those.
    List<Singer> singers = singerRepository.findAll(Pageable.ofSize(20)).toList();
    List<Album> albums = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      Album album = new Album();
      album.setTitle(randomDataService.getRandomAlbumTitle());
      byte[] picture = new byte[random.nextInt(400) + 100];
      random.nextBytes(picture);
      album.setCoverPicture(picture);
      album.setMarketingBudget(
          BigDecimal.valueOf(random.nextInt())
              .divide(BigDecimal.valueOf(100L), RoundingMode.HALF_UP)
              .setScale(2, RoundingMode.HALF_UP));
      album.setReleaseDate(
          LocalDate.of(random.nextInt(100) + 1923, random.nextInt(12) + 1, random.nextInt(28) + 1));
      album.setSinger(singers.get(random.nextInt(singers.size())));
      albums.add(album);
    }
    return albumRepository.saveAll(albums);
  }
}
