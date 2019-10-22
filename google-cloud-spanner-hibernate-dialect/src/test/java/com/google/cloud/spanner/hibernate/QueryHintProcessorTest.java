package com.google.cloud.spanner.hibernate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class QueryHintProcessorTest {

  @Test
  public void testValidateQueryHint() {
    assertThat(QueryHintProcessor.validateQueryHint("join_METHOD=HASH_JOIN")).isTrue();

    assertThat(QueryHintProcessor.validateQueryHint("test")).isFalse();
    assertThat(QueryHintProcessor.validateQueryHint("foo=bar")).isFalse();
    assertThat(QueryHintProcessor.validateQueryHint("FORCE_JOIN_ORDER")).isFalse();
    assertThat(QueryHintProcessor.validateQueryHint("JOIN_METHOD=")).isFalse();
  }

  @Test
  public void testFormatQueryHints() {
    String queryHintResult = QueryHintProcessor.formatQueryHints(
        CollectionUtils.listOf("JOIN_METHOD=HASH_JOIN", "FORCE_JOIN_ORDER=true"));
    assertThat(queryHintResult).isEqualTo("@{JOIN_METHOD=HASH_JOIN, FORCE_JOIN_ORDER=true}");
  }
}
