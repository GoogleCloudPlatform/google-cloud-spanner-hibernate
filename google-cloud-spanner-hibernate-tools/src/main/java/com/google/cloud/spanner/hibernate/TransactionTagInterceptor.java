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

package com.google.cloud.spanner.hibernate;

import com.google.common.collect.ImmutableSet;
import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Method;
import java.util.Set;
import org.hibernate.Interceptor;

/**
 * {@link Interceptor} that adds transaction tags for read/write transactions.
 *
 * <p>The interceptor adds tags in two ways:
 *
 * <ol>
 *   <li>Based on {@link TransactionTag} annotations that have been placed on transactional methods.
 *   <li>Automatically generated tag names based on the class and method name of methods that start
 *       a transaction.
 * </ol>
 *
 * The method that started the transaction is determined by looking at the call stack when the
 * transaction actually starts, and then going up the call stack until a class is found that is part
 * of one of the give class name prefixes (package names).
 *
 * <p>Auto-tagging of all transactions can be dynamically enabled by starting the application with
 * the system property 'spanner.auto_tag_transactions=true'.
 */
public class TransactionTagInterceptor extends AbstractTransactionTagInterceptor {
  public static final String SPANNER_AUTO_TAG_TRANSACTIONS_PROPERTY_NAME =
      "spanner.auto_tag_transactions";

  private final ImmutableSet<String> classNamePrefixes;

  private final boolean autoTagTransactions;

  /**
   * Creates an {@link Interceptor} that adds transaction tags for read/write transactions that are
   * started. The tag is either the value that is set with a {@link TransactionTag} annotation, or
   * the tag contains the class name and method name of the method that started the transaction if
   * autoTagging has been set to true.
   *
   * @param classNamePrefixes The prefixes of the class names that should be considered part of the
   *     application, and not part of the system call stack. If for example all your application
   *     classes live in the package com.example.myapp or sub-packages of that, then specify
   *     "com.example.myapp" as the prefix.
   * @param autoTagging Indicates whether tags should automatically be added to all transactions,
   *     including those that do not have a {@link TransactionTag}. The tag will be equal to the
   *     class name and method name of the method that started the transaction. The value of this
   *     argument can be overridden by setting the System property
   *     'spanner.auto_tag_transactions=true' or 'spanner.auto_tag_transactions=false'.
   */
  public TransactionTagInterceptor(Set<String> classNamePrefixes, boolean autoTagging) {
    this.classNamePrefixes = ImmutableSet.copyOf(classNamePrefixes);
    if (System.getProperties().containsKey(SPANNER_AUTO_TAG_TRANSACTIONS_PROPERTY_NAME)) {
      this.autoTagTransactions =
          Boolean.parseBoolean(System.getProperty(SPANNER_AUTO_TAG_TRANSACTIONS_PROPERTY_NAME));
    } else {
      this.autoTagTransactions = autoTagging;
    }
  }

  @Override
  protected String getTag() {
    for (String prefix : classNamePrefixes) {
      StackFrame stackFrame =
          StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
              .walk(
                  stream ->
                      stream
                          .skip(1)
                          .filter(frame -> frame.getClassName().startsWith(prefix))
                          .findFirst()
                          .orElse(null));
      if (stackFrame != null) {
        return getTagFromStackFrame(prefix, stackFrame);
      }
    }
    return null;
  }

  String getTagFromStackFrame(String prefix, StackFrame stackFrame) {
    Class<?> declaringClass = stackFrame.getDeclaringClass();
    if (declaringClass.getName().contains("$$")) {
      declaringClass = declaringClass.getSuperclass();
    }
    try {
      Method method =
          declaringClass.getDeclaredMethod(
              stackFrame.getMethodName(), stackFrame.getMethodType().parameterArray());
      if (method.isAnnotationPresent(TransactionTag.class)) {
        TransactionTag transactionTag = method.getAnnotation(TransactionTag.class);
        return transactionTag.value();
      } else if (autoTagTransactions) {
        return getAutoTagFromStackFrame(prefix, stackFrame);
      }
    } catch (NoSuchMethodException ignore) {
      // This should not happen
    }
    return null;
  }

  static String getAutoTagFromStackFrame(String prefix, StackFrame stackFrame) {
    // Check if it is a CGLIB generated class.
    Class<?> clazz = stackFrame.getDeclaringClass();
    if (stackFrame.getClassName().contains("$$")) {
      clazz = clazz.getSuperclass();
    }
    // TODO: Fix tag regex allowed values in Connection API.
    String className = clazz.getName().replace(prefix, "").replace('.', '_');
    if (className.startsWith("_")) {
      className = className.substring(1);
    }
    String tag = className + "_" + stackFrame.getMethodName();
    if (stackFrame.getLineNumber() > 0) {
      String lineNumber = "_L" + stackFrame.getLineNumber();
      if (tag.length() + lineNumber.length() <= 50) {
        tag = tag + lineNumber;
      }
    }
    if (tag.length() > 50) {
      tag = tag.substring(tag.length() - 50);
    }
    return tag;
  }
}
