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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.apache.tools.ant.util.TeeOutputStream;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This verifies the sample application.
 *
 * @author Chengyuan Zhao
 */
public class SampleApplicationTests {

  private static PrintStream systemOut;

  private static ByteArrayOutputStream baos;

  @BeforeClass
  public static void setUp() {
    systemOut = System.out;
    baos = new ByteArrayOutputStream();
    TeeOutputStream out = new TeeOutputStream(systemOut, baos);
    System.setOut(new PrintStream(out));
  }

  @Test
  public void testSample() {
    SampleApplication.main(null);

    assertThat(baos.toString()).contains(
        "insert into Person_Sample_Application (address, name, nickName, id) values (?, ?, ?, ?)");
    assertThat(baos.toString()).contains("Found saved Person with generated ID:");
    assertThat(baos.toString()).contains(";person;purson;address");
  }
}
