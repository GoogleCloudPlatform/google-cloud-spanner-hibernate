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

package com.example;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

/**
 * An example JAX-RS controller that uses Panache repository for CRUD operations with a JPA entity.
 *
 * <p>Optionally, you can also inject an {@link EntityManager} for direct JPA access.
 */
@Path("/person")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonController {

  /**
   * Inject a Panache repository for CRUD operations.
   */
  @Inject PersonRepository personRepository;

  /**
   * Optionally, inject the JPA {@link EntityManager} for direct JPA access.
   */
  @Inject EntityManager entityManager;

  /**
   * Create a new {@link Person} entity with auto-generated ID.
   *
   * @param person JSON payload for Person
   * @return created {@link Person} entity
   */
  @POST
  @Path("/")
  @Transactional
  public Person create(Person person) {
    Person p = new Person();
    p.setName(person.getName());

    personRepository.persist(p);

    return p;
  }

  /**
   * Retrieve a {@link Person} entity by ID.
   *
   * @param id UUID String to lookup
   * @return a {@link Person} entity
   */
  @GET
  @Path("/{id}")
  public Person retrieve(@PathParam("id") String id) {
    return personRepository.findById(UUID.fromString(id));

    // Optionally, you can use EntityManager instead.
    // entityManager.find(Person.class, UUID.fromString(id));
  }
}
