package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.UUID;
import model.CoffeeRepository;
import model.Customer;
import model.CustomerRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = CoffeeApplication.class)
@TestPropertySource("classpath:application-test.properties")
public class CoffeeApplicationTests {

  @Autowired
  CustomerRepository customerRepository;

  @Autowired
  CoffeeRepository coffeeRepository;

  @Autowired
  TestRestTemplate restTemplate;

  @Before
  @After
  public void cleanupTestEnvironment() {
    customerRepository.deleteAll();
    coffeeRepository.deleteAll();
  }

  @Test
  public void testCreateCustomer() {
    LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
    map.add("name", "Bob");
    map.add("email", "bob@hello-world.com");

    HttpHeaders headers = new HttpHeaders();
    HttpEntity<LinkedMultiValueMap<String, Object>> request = new HttpEntity<>(map, headers);
    ResponseEntity<String> response =
        this.restTemplate.postForEntity("/createCustomer", request, String.class);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    ArrayList<Customer> allCustomers = new ArrayList<>();
    for (Customer c : customerRepository.findAll()) {
      allCustomers.add(c);
    }

    assertThat(allCustomers).hasSize(1);
    assertThat(allCustomers.get(0).getName()).isEqualTo("Bob");
  }

  @Test
  public void testCreateCoffee() {
    LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
    map.add("name", "Bob");
    map.add("email", "bob@hello-world.com");

    HttpHeaders headers = new HttpHeaders();
    HttpEntity<LinkedMultiValueMap<String, Object>> request = new HttpEntity<>(map, headers);
    ResponseEntity<String> response =
        this.restTemplate.postForEntity("/createCustomer", request, String.class);
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    UUID customerId = customerRepository.findAll().iterator().next().getId();

    map = new LinkedMultiValueMap<>();
    map.add("customerId", customerId.toString());
    map.add("size", "large");
    map.add("coffeeCount", 3);

    request = new HttpEntity<>(map, headers);
    response = this.restTemplate.postForEntity("/orderCoffee", request, String.class);
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    Customer customer = customerRepository.findById(customerId).get();
    assertThat(customer.getCoffees()).hasSize(3);
  }
}
