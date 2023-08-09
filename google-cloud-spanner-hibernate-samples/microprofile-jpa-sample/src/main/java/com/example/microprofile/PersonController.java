/*
 * Copyright 2019-2020 Google LLC
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

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * An example JAX-RS controller that uses Panache repository for CRUD operations with a JPA entity.
 *
 * <p>Optionally, you can also inject an {@link EntityManager} for direct JPA access.
 */
@Path("/person")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class PersonController {

  /**
   * Inject the JPA {@link EntityManager} for direct JPA access.
   */
  @Inject
  EntityManager entityManager;

  /**
   * Create a new {@link Person} entity with auto-generated ID.
   *
   * @param person JSON payload for Person
   *
   * @return created {@link Person} entity
   */
  @POST
  @Path("/")
  @Transactional
  public Person create(Person person) {
    Person p = new Person();
    p.setName(person.getName());

    entityManager.persist(p);

    return p;
  }

  /**
   * Retrieve a {@link Person} entity by ID.
   *
   * @param id UUID String to lookup
   *
   * @return a {@link Person} entity
   */
  @GET
  @Path("/{id}")
  public Person retrieve(@PathParam("id") String id) {
    return entityManager.find(Person.class, UUID.fromString(id));
  }
}
