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

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

/** Shared service for generating random names and titles. */
@Service
public class RandomDataService {

  private static final List<String> FIRST_NAMES =
      ImmutableList.of(
          "Aarav",
          "Gunnar",
          "Joaquin",
          "Luciano",
          "Mateo",
          "Roman",
          "Santiago",
          "Soren",
          "Aurelio",
          "Laszlo",
          "Elio",
          "Wolf",
          "Amir",
          "Aditya",
          "Ayaan",
          "Dhruv",
          "Harsh",
          "Krishna",
          "Rohan",
          "Vihaan");

  private static final List<String> LAST_NAMES =
      ImmutableList.of(
          "Abreu",
          "Chen",
          "Da Silva",
          "Elliott",
          "Fernandez",
          "Garcia",
          "Gonzalez",
          "Hernandez",
          "Ibrahim",
          "James",
          "Kim",
          "Lopez",
          "Martinez",
          "Nguyen",
          "O'Brien",
          "Perez",
          "Ramos",
          "Smith",
          "Thomas",
          "Wang");

  private static final List<String> NICK_NAMES =
      ImmutableList.of(
          "Sparky",
          "Ace",
          "Maverick",
          "Zip",
          "Doc",
          "Raven",
          "Cricket",
          "Glitch",
          "Shadow",
          "Rusty",
          "Jinx",
          "Flash",
          "Chip",
          "Domino",
          "Zephyr",
          "Scout",
          "Orbit",
          "Nova",
          "Echo",
          "Skipper");

  private static final List<String> NOUNS =
      ImmutableList.of(
          "ocean",
          "river",
          "island",
          "mountain",
          "lake",
          "forest",
          "desert",
          "plain",
          "valley",
          "hill",
          "cloud",
          "star",
          "planet",
          "moon",
          "sun",
          "galaxy",
          "universe",
          "time",
          "space",
          "love");

  private static final List<String> ADJECTIVES =
      ImmutableList.of(
          "wondrous",
          "whimsical",
          "ethereal",
          "mystical",
          "dreamy",
          "serene",
          "tranquil",
          "peaceful",
          "joyful",
          "ecstatic",
          "euphoric",
          "sublime",
          "glorious",
          "majestic",
          "grandiose",
          "spectacular",
          "awe-inspiring",
          "mesmerizing",
          "enchanting",
          "captivating");

  private static final List<String> VERBS =
      ImmutableList.of(
          "run", "jump", "walk", "talk", "eat", "sleep", "drink", "think", "see", "hear", "sing",
          "dance", "play", "read", "write", "draw", "paint", "cook", "clean", "wash", "work",
          "study", "learn", "teach", "help", "share", "care", "love", "hate", "like", "dislike",
          "want", "need", "have", "be", "do", "say", "make", "take", "give", "get", "go", "come",
          "seem", "appear", "happen", "become", "turn", "change", "grow", "shrink", "live", "die",
          "move");

  private static final List<String> ADVERBS =
      ImmutableList.of(
          "quickly",
          "slowly",
          "carefully",
          "carelessly",
          "well",
          "badly",
          "suddenly",
          "gradually",
          "easily",
          "difficultly",
          "loudly",
          "quietly",
          "happily",
          "sadly",
          "correctly",
          "incorrectly",
          "interestingly",
          "boringly",
          "truthfully",
          "falsely",
          "honestly",
          "dishonestly",
          "wisely",
          "foolishly",
          "bravely",
          "cowardly",
          "kindly",
          "cruelly",
          "gently",
          "roughly",
          "successfully",
          "unsuccessfully",
          "completely",
          "incompletely",
          "immediately",
          "eventually",
          "temporarily",
          "permanently",
          "constantly",
          "infrequently",
          "regularly",
          "irregularly",
          "frequently",
          "seldom",
          "often",
          "rarely",
          "never");

  private static final ImmutableList<String> VENUE_TYPES =
      ImmutableList.of("Park", "Stadium", "Concert Hall", "Cinema", "Bar", "Opera");

  private static final ImmutableList<String> PLACES =
      ImmutableList.of(
          "Paris, France",
          "New York City, USA",
          "London, England",
          "Tokyo, Japan",
          "Toronto, Canada",
          "Mexico City, Mexico",
          "São Paulo, Brazil",
          "Istanbul, Turkey",
          "Shanghai, China",
          "Sydney, Australia");

  private final Random random = new Random();

  /** Returns a random first name. */
  public String getRandomFirstName() {
    return FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size()));
  }

  /** Returns a random last name. */
  public String getRandomLastName() {
    return LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
  }

  /** Returns a random list of nicknames. */
  public List<String> getRandomNickNames() {
    int num = random.nextInt(5) + 1;
    List<String> result = new ArrayList<>(num);
    for (int i = 0; i < num; i++) {
      result.add(NICK_NAMES.get(random.nextInt(NICK_NAMES.size())));
    }
    return result;
  }

  /** Returns a random album title. */
  public String getRandomAlbumTitle() {
    return ADJECTIVES.get(random.nextInt(ADJECTIVES.size()))
        + " "
        + NOUNS.get(random.nextInt(NOUNS.size()));
  }

  /** Returns a random track title. */
  public String getRandomTrackTitle() {
    return ADVERBS.get(random.nextInt(ADVERBS.size()))
        + " "
        + VERBS.get(random.nextInt(VERBS.size()));
  }

  /** Returns a random venue name. */
  public String getRandomVenueName() {
    return ADJECTIVES.get(random.nextInt(ADJECTIVES.size()))
        + " "
        + NOUNS.get(random.nextInt(NOUNS.size()));
  }

  /** Returns a random concert name. */
  public String getRandomConcertName() {
    return ADJECTIVES.get(random.nextInt(ADJECTIVES.size()))
        + " "
        + NOUNS.get(random.nextInt(NOUNS.size()));
  }

  /** Returns a random venue type. */
  public String getRandomVenueType() {
    return VENUE_TYPES.get(random.nextInt(VENUE_TYPES.size()));
  }

  /** Returns a random venue location. */
  public String getRandomVenueLocation() {
    return PLACES.get(random.nextInt(PLACES.size()));
  }
}
