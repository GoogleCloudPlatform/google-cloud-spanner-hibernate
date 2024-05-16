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

package codelab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

/** This verifies the sample application. */
public class AppIT {

  @Rule public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  @Test
  public void test() {
    App.main(new String[] {});

    assertThat(systemOutRule.getLog())
        .contains(
            "Singers who were born in 1990 or later:\n"
                + "Jacqueline Long born on 1990-07-29\n"
                + "Dylan Shaw born on 1998-05-02\n"
                + "Albums: \n"
                + "\"Go, Go, Go\" by Melissa Garcia\n");
  }
}
