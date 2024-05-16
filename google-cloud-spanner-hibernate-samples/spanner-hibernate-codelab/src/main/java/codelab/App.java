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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * The Hibernate codelab solution which demonstrates basic operations in Hibernate such as reading,
 * writing, and deleting data.
 */
public class App {

  public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  /** The main method that does the CRUD operations. */
  public static void main(String[] args) {
    // create a Hibernate sessionFactory and session
    StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure().build();
    SessionFactory sessionFactory =
        new MetadataSources(registry).buildMetadata().buildSessionFactory();
    Session session = sessionFactory.openSession();

    clearData(session);

    writeData(session);

    readData(session);

    // close Hibernate session and sessionFactory
    session.close();
    sessionFactory.close();
  }

  private static void clearData(Session session) {
    session.beginTransaction();

    session.createQuery("delete from Album where 1=1").executeUpdate();
    session.createQuery("delete from Singer where 1=1").executeUpdate();

    session.getTransaction().commit();
  }

  private static void writeData(Session session) {
    session.beginTransaction();

    Singer singerMelissa = new Singer("Melissa", "Garcia", makeDate("1981-03-19"));
    Album albumGoGoGo = new Album(singerMelissa, "Go, Go, Go");
    session.save(singerMelissa);
    session.save(albumGoGoGo);

    session.save(new Singer("Russell", "Morales", makeDate("1978-12-02")));
    session.save(new Singer("Jacqueline", "Long", makeDate("1990-07-29")));
    session.save(new Singer("Dylan", "Shaw", makeDate("1998-05-02")));

    session.getTransaction().commit();
  }

  private static void readData(Session session) {
    List<Singer> singers =
        session.createQuery("from Singer where birthDate >= '1990-01-01' order by lastName").list();
    System.out.println("Singers who were born in 1990 or later:");
    for (Singer singer : singers) {
      System.out.println(
          singer.getFirstName()
              + " "
              + singer.getLastName()
              + " born on "
              + DATE_FORMAT.format(singer.getBirthDate()));
    }

    List<Album> albums = session.createQuery("from Album").list();
    System.out.println("Albums: ");
    for (Album album : albums) {
      System.out.println(
          "\""
              + album.getAlbumTitle()
              + "\" by "
              + album.getSinger().getFirstName()
              + " "
              + album.getSinger().getLastName());
    }
  }

  private static Date makeDate(String dateString) {
    try {
      return DATE_FORMAT.parse(dateString);
    } catch (ParseException e) {
      e.printStackTrace();
      return null;
    }
  }
}
