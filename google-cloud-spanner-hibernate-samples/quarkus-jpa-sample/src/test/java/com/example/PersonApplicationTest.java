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

package com.example;

import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PersonApplicationTest {

  @Test
  public void testPersonEndpoint() {

    Map<String, String> jsonPayload = new HashMap<>();
    jsonPayload.put("name", "Ray");

    RestAssured.given()
        .when()
        .contentType("application/json")
        .body(jsonPayload)
        .post("/person")
        .then()
        .statusCode(200)
        .body(containsString("\"name\":\"Ray\""));
  }
}
