/*
 * Copyright 2019-2024 Google LLC
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

import com.google.cloud.spanner.sample.entities.Concert;
import com.google.cloud.spanner.sample.entities.TicketSale;
import com.google.cloud.spanner.sample.repository.ConcertRepository;
import com.google.cloud.spanner.sample.repository.TicketSaleRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

/** Service class for fetching and saving TicketSale records. */
@Service
public class TicketSaleService {

  private final TicketSaleRepository repository;

  private final ConcertRepository concertRepository;

  private final RandomDataService randomDataService;

  /** Constructor with auto-injected dependencies. */
  public TicketSaleService(
      TicketSaleRepository repository,
      ConcertRepository concertRepository,
      RandomDataService randomDataService) {
    this.repository = repository;
    this.concertRepository = concertRepository;
    this.randomDataService = randomDataService;
  }

  /** Deletes all TicketSale records in the database. */
  @Transactional
  public void deleteAllTicketSales() {
    repository.deleteAll();
  }

  /** Generates the specified number of random TicketSale records. */
  @Transactional
  public List<TicketSale> generateRandomTicketSales(int count) {
    Random random = new Random();

    List<Concert> concerts = concertRepository.findAll();
    List<TicketSale> ticketSales = new ArrayList<>(count);
    if (concerts.isEmpty()) {
      return ticketSales;
    }
    for (int i = 0; i < count; i++) {
      TicketSale ticketSale = new TicketSale();
      ticketSale.setConcert(concerts.get(random.nextInt(concerts.size())));
      ticketSale.setCustomerName(
          randomDataService.getRandomFirstName() + " " + randomDataService.getRandomLastName());
      ticketSale.setPrice(
          BigDecimal.valueOf(random.nextDouble() * 300).setScale(2, RoundingMode.HALF_UP));
      int numSeats = random.nextInt(5) + 1;
      List<String> seats = new ArrayList<>(numSeats);
      for (int n = 0; n < numSeats; n++) {
        seats.add("A" + random.nextInt(100) + 1);
      }
      ticketSale.setSeats(seats);
      ticketSales.add(ticketSale);
    }
    return repository.saveAll(ticketSales);
  }
}
