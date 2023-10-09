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

package com.google.cloud.spanner.hibernate.entities;

import com.google.cloud.spanner.hibernate.BitReversedSequenceStyleGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

/**
 * A test entity that has a many-to-one relationship.
 *
 * @author loite
 */
@Entity
public class Invoice {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoiceId")
  @GenericGenerator(
      name = "invoiceId",
      type = BitReversedSequenceStyleGenerator.class,
      parameters = {
          @Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "1000"),
          @Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = "invoiceId"),
          @Parameter(name = SequenceStyleGenerator.INITIAL_PARAM, value = "1")
      })
  @Column(nullable = false)
  private Long invoiceId;

  @ColumnDefault("'9999'")
  private String number;

  @ManyToOne
  @JoinColumn(foreignKey = @ForeignKey(name = "fk_invoice_customer"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Customer customer;

}
