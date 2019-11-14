/*
 * Copyright 2019 Google LLC
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

package com.example.microprofile;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;

/**
 * Create an {@link EntityManager} bean that can be injected via CDI.
 */
@ApplicationScoped
public class Producer {

  /**
   * Produces a JPA {@link EntityManager}.
   * @return EntityManager for the persistence unit.
   */
  @Produces
  EntityManager entityManager() {
    return Persistence.createEntityManagerFactory("spanner-example")
        .createEntityManager();
  }

  /**
   * Closes the {@link EntityManager} on shutdown.
   * @param entityManager {@link EntityManager} to close.
   */
  public void close(EntityManager entityManager) {
    entityManager.close();
  }
}
