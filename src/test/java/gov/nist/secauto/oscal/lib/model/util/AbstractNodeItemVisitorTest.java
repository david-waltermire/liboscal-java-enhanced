/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib.model.util;

import gov.nist.secauto.metaschema.core.metapath.item.node.AbstractRecursionPreventingNodeItemVisitor;
import gov.nist.secauto.metaschema.core.metapath.item.node.IDefinitionNodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItemFactory;
import gov.nist.secauto.metaschema.core.model.constraint.IAllowedValue;
import gov.nist.secauto.metaschema.core.model.constraint.IAllowedValuesConstraint;
import gov.nist.secauto.metaschema.databind.model.IBoundModule;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.OscalCompleteModule;
import gov.nist.secauto.oscal.lib.model.util.AllowedValueCollectingNodeItemVisitor.AllowedValuesRecord;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.NonNull;

class AbstractNodeItemVisitorTest {

  @Test
  void testAllowedValues() {
    IBoundModule module = OscalBindingContext.instance().registerModule(OscalCompleteModule.class);
    AllowedValueCollectingNodeItemVisitor walker = new AllowedValueCollectingNodeItemVisitor();
    walker.visit(module);

    System.out.println();
    System.out.println("Allowed Values");
    System.out.println("--------------");
    Map<IDefinitionNodeItem<?, ?>,
        List<AllowedValueCollectingNodeItemVisitor.AllowedValuesRecord>> allowedValuesByTarget
            = walker.getAllowedValueLocations().stream()
                .flatMap(location -> location.getAllowedValues().stream())
                .collect(Collectors.groupingBy(AllowedValuesRecord::getTarget,
                    LinkedHashMap::new,
                    Collectors.mapping(Function.identity(), Collectors.toUnmodifiableList())));
    sortMap(allowedValuesByTarget, Comparator.comparing(IDefinitionNodeItem::getMetapath)).entrySet().stream()
        .forEachOrdered(entry -> {
          IDefinitionNodeItem<?, ?> target = entry.getKey();
          List<AllowedValueCollectingNodeItemVisitor.AllowedValuesRecord> allowedValuesRecords = entry.getValue();

          System.out.println("node:             " + metapath(target));
          allowedValuesRecords.forEach(record -> {
            assert target.equals(record.getTarget());

            IAllowedValuesConstraint constraint = record.getAllowedValues();
            System.out.println("- allowed-values:");
            System.out.println("    location:     " + metapath(record.getLocation()));
            System.out.println("    target:       " + constraint.getTarget());
            System.out.println("    values:       " + values(constraint));
            System.out.println("    allow-other:  " + constraint.isAllowedOther());
            // System.out.println(" extensible: " + constraint.getExtensible());
            // System.out.println(" source: " + constraint.getSource());
          });
        });
  }

  private static String metapath(@NonNull IDefinitionNodeItem<?, ?> item) {
    return metapath(item.getMetapath());
  }

  private static String metapath(@NonNull String path) {
    return path.replaceAll("\\[1\\]", "");
  }

  private static String values(@NonNull IAllowedValuesConstraint constraint) {
    return constraint.getAllowedValues().values().stream()
        .map(IAllowedValue::getValue)
        .collect(Collectors.joining(", "));
  }

  @NonNull
  private static <K, V> NavigableMap<K, V> sortMap(@NonNull Map<K, V> map, @NonNull Comparator<? super K> comparator) {
    TreeMap<K, V> retval = new TreeMap<>(comparator);
    retval.putAll(map);
    return retval;
  }

  @Test
  void testAssemblyRecursion() {
    IBoundModule module = OscalBindingContext.instance().registerModule(OscalCompleteModule.class);
    RecursionCollectingNodeItemVisitor walker = new RecursionCollectingNodeItemVisitor();
    walker.visit(module);
    Set<RecursionCollectingNodeItemVisitor.AssemblyRecord> recursiveAssemblies
        = walker.getRecursiveAssemblyDefinitions();

    System.out.println("Recursive Assemblies");
    System.out.println("--------------------");
    recursiveAssemblies.forEach(record -> {
      System.out.println(record.getDefinition().getFormalName());
      record.getLocations().forEach(location -> {
        System.out.println("- " + metapath(location));
      });
    });
  }

  @Test
  void testRecursion() {
    AbstractRecursionPreventingNodeItemVisitor<Void, Void> visitor
        = new AbstractRecursionPreventingNodeItemVisitor<>() {
          @Override
          protected Void defaultResult() {
            return null;
          }
        };

    IBoundModule module = OscalBindingContext.instance().registerModule(OscalCompleteModule.class);
    visitor.visitMetaschema(INodeItemFactory.instance().newModuleNodeItem(module), null);
  }

  @Test
  void testErDiagram() throws IOException {
    MermaidErDiagramGenerator visitor = new MermaidErDiagramGenerator();

    IBoundModule module = OscalBindingContext.instance().registerModule(OscalCompleteModule.class);

    try (PrintWriter writer = new PrintWriter(System.out)) {
      visitor.generate(module, writer);
    }
  }
}
