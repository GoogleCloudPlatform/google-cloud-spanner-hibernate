package org.hibernate.jpa.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

final class EntityDependencyTracker {

  private Map<String, ArrayList<String>> dependencyGraph = new HashMap<>();

  /**
   * Adds a dependency relationship describing that the {@code childEntity} must be deleted
   * before {@code parentEntity}.
   *
   * A null {@code childEntity} means the {@code parentEntity} can be deleted without any dependencies.
   */
  public void addDependency(String parentEntity, @Nullable String childEntity) {
    dependencyGraph.putIfAbsent(parentEntity, new ArrayList<>());
    if (childEntity != null) {
      dependencyGraph.get(parentEntity).add(childEntity);
    }
  }

  public List<String> getEntityOrder() {
    List<String> results = new ArrayList<>();
    for (String node : dependencyGraph.keySet()) {
      visitNode(node, results);
    }
    return results;
  }

  private void visitNode(String entity, List<String> accumulator) {
    if (dependencyGraph.containsKey(entity)) {
      List<String> parentNodes = dependencyGraph.get(entity);
      for (String node : parentNodes) {
        visitNode(node, accumulator);
      }
    }

    if (!accumulator.contains(entity)) {
      accumulator.add(entity);
    }
  }
}
