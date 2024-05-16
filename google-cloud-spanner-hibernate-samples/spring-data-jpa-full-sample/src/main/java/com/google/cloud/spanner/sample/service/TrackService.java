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

import com.google.cloud.spanner.sample.entities.Album;
import com.google.cloud.spanner.sample.entities.Track;
import com.google.cloud.spanner.sample.repository.AlbumRepository;
import com.google.cloud.spanner.sample.repository.TrackRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Service class for fetching and saving Track records. */
@Service
public class TrackService {

  private final RandomDataService randomDataService;

  private final TrackRepository trackRepository;

  private final AlbumRepository albumRepository;

  /** Constructor with auto-injected dependencies. */
  public TrackService(
      RandomDataService randomDataService,
      TrackRepository trackRepository,
      AlbumRepository albumRepository) {
    this.randomDataService = randomDataService;
    this.trackRepository = trackRepository;
    this.albumRepository = albumRepository;
  }

  /** Generates the specified number of random Track records. */
  @Transactional
  public void generateRandomTracks(int numAlbums, int numTracksPerAlbum) {
    Random random = new Random();

    List<Album> albums = albumRepository.findAll(Pageable.ofSize(numAlbums)).toList();
    for (Album album : albums) {
      List<Track> tracks = new ArrayList<>(numTracksPerAlbum);
      for (int trackNumber = 1; trackNumber <= numTracksPerAlbum; trackNumber++) {
        Track track = Track.createNew(album, trackNumber);
        track.setTitle(randomDataService.getRandomTrackTitle());
        track.setSampleRate(random.nextDouble());
        tracks.add(track);
      }
      trackRepository.saveAll(tracks);
    }
  }
}
