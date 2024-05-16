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

package com.google.cloud.spanner.sample.repository;

import com.google.cloud.spanner.sample.entities.Singer;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.stream.Stream;
import org.hibernate.jpa.AvailableHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface SingerRepository extends JpaRepository<Singer, String> {

  /** Get all singers that have a last name that starts with the given prefix. */
  @Query("SELECT s FROM Singer s WHERE starts_with(s.lastName, :lastName)=true")
  Stream<Singer> searchByLastNameStartsWith(@Param("lastName") String lastName);

  // The hint value used here is generated by calling the method:
  // Hints.forceIndexFrom("singer", "idx_singer_active", ReplaceMode.ALL).toComment()
  // manually and then copy-paste the value to the annotation.
  @QueryHints(
      @QueryHint(
          name = AvailableHints.HINT_COMMENT,
          value =
              "{\n"
                  + "  \"spanner_replacements\": [\n"
                  + "    {\n"
                  + "      \"regex\": \" from singer \",\n"
                  + "      \"replacement\": \" from singer @{FORCE_INDEX=idx_singer_active} \",\n"
                  + "      \"replace_mode\": \"ALL\"\n"
                  + "    }\n"
                  + "  ]\n"
                  + "}"))
  List<Singer> findByActive(boolean active);
}
