/*
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Nick Santos
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package com.google.javascript.rhino.jstype;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.javascript.rhino.jstype.JSTypeNative.ALL_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.BOOLEAN_OBJECT_FUNCTION_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.BOOLEAN_OBJECT_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.BOOLEAN_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.GENERATOR_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.ITERABLE_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.ITERATOR_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.I_TEMPLATE_ARRAY_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.NULL_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.NULL_VOID;
import static com.google.javascript.rhino.jstype.JSTypeNative.NUMBER_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.STRING_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.STRING_VALUE_OR_OBJECT_TYPE;
import static com.google.javascript.rhino.testing.TypeSubject.types;

import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;
import com.google.javascript.rhino.testing.Asserts;
import junit.framework.TestCase;

/**
 * Tests {@link JSTypeRegistry}.
 *
 */
public class JSTypeRegistryTest extends TestCase {
  // TODO(user): extend this class with more tests, as JSTypeRegistry is
  // now much larger
  public void testGetBuiltInType_boolean() {
    JSTypeRegistry typeRegistry = new JSTypeRegistry(null);
    assertTypeEquals(
        typeRegistry.getNativeType(JSTypeNative.BOOLEAN_TYPE),
        typeRegistry.getType(null, "boolean"));
  }

  public void testGetBuiltInType_iterable() {
    JSTypeRegistry typeRegistry = new JSTypeRegistry(null);
    assertTypeEquals(
        typeRegistry.getNativeType(ITERABLE_TYPE), typeRegistry.getGlobalType("Iterable"));
  }

  public void testGetBuiltInType_iterator() {
    JSTypeRegistry typeRegistry = new JSTypeRegistry(null);
    assertTypeEquals(
        typeRegistry.getNativeType(ITERATOR_TYPE), typeRegistry.getGlobalType("Iterator"));
  }

  public void testGetBuiltInType_generator() {
    JSTypeRegistry typeRegistry = new JSTypeRegistry(null);
    assertTypeEquals(
        typeRegistry.getNativeType(GENERATOR_TYPE), typeRegistry.getGlobalType("Generator"));
  }

  public void testGetBuildInType_iTemplateArray() {
    JSTypeRegistry typeRegistry = new JSTypeRegistry(null);
    assertTypeEquals(
        typeRegistry.getNativeType(I_TEMPLATE_ARRAY_TYPE),
        typeRegistry.getGlobalType("ITemplateArray"));
  }

  public void testGetBuiltInType_Promise() {
    JSTypeRegistry registry = new JSTypeRegistry(null);
    ObjectType promiseType = registry.getNativeObjectType(JSTypeNative.PROMISE_TYPE);
    assertTypeEquals(promiseType, registry.getGlobalType("Promise"));

    // Test that it takes one parameter of type
    // function(function((IThenable<TYPE>|TYPE|null|{then: ?})=): ?, function(*=): ?): ?
    FunctionType promiseCtor = promiseType.getConstructor();
    Node paramList = promiseCtor.getParametersNode();
    Node firstParameter = paramList.getFirstChild();
    assertNotNull(firstParameter);
    FunctionType paramType = paramList.getFirstChild().getJSType().toMaybeFunctionType();
    assertEquals(
        "function(function((IThenable<TYPE>|TYPE|null|{then: ?})=): ?, function(*=): ?): ?",
        paramType.toString());
  }

  public void testGetDeclaredType() {
    JSTypeRegistry typeRegistry = new JSTypeRegistry(null);
    JSType type = typeRegistry.createAnonymousObjectType(null);
    String name = "Foo";
    typeRegistry.declareType(null, name, type);
    assertTypeEquals(type, typeRegistry.getType(null, name));

    // Ensure different instances are independent.
    JSTypeRegistry typeRegistry2 = new JSTypeRegistry(null);
    assertEquals(null, typeRegistry2.getType(null, name));
    assertTypeEquals(type, typeRegistry.getType(null, name));
  }

  public void testPropertyOnManyTypes() {
    // Given
    JSTypeRegistry typeRegistry = new JSTypeRegistry(null);

    // By default the UnionTypeBuilder will treat a union of more than 30
    // types as an unknown type. We don't want that for property checking
    // so test that the limit is higher.
    for (int i = 0; i < 100; i++) {
      JSType type = typeRegistry.createObjectType("type: " + i, null);

      // When
      typeRegistry.registerPropertyOnType("foo", type);

      // Then
      assertWithMessage("Registered property `foo` on <%s> types.", i + 1)
          .about(types())
          .that(typeRegistry.getGreatestSubtypeWithProperty(type, "foo"))
          .isNotUnknown();
    }
  }

  public void testReadableTypeName() {
    JSTypeRegistry registry = new JSTypeRegistry(null);

    assertEquals("*", getReadableTypeNameHelper(registry, ALL_TYPE));

    assertEquals("boolean", getReadableTypeNameHelper(registry, BOOLEAN_TYPE));
    assertEquals("Boolean", getReadableTypeNameHelper(registry, BOOLEAN_OBJECT_TYPE));
    assertEquals("function", getReadableTypeNameHelper(registry, BOOLEAN_OBJECT_FUNCTION_TYPE));

    assertEquals(
        "(String|string)", getReadableTypeNameHelper(registry, STRING_VALUE_OR_OBJECT_TYPE));

    assertEquals("(null|undefined)", getReadableTypeNameHelper(registry, NULL_VOID));
    assertEquals("(null|undefined)", getReadableTypeNameHelper(registry, NULL_VOID, true));

    assertEquals(
        "(number|string|null)",
        getReadableTypeNameHelper(registry, union(registry, NUMBER_TYPE, STRING_TYPE, NULL_TYPE)));

    assertEquals(
        "(Number|String)",
        getReadableTypeNameHelper(
            registry, union(registry, NUMBER_TYPE, STRING_TYPE, NULL_TYPE), true));
  }

  private JSType union(JSTypeRegistry registry, JSTypeNative... types) {
    return registry.createUnionType(types);
  }

  private String getReadableTypeNameHelper(JSTypeRegistry registry, JSTypeNative type) {
    return getReadableTypeNameHelper(registry, registry.getNativeType(type), false);
  }

  private String getReadableTypeNameHelper(
      JSTypeRegistry registry, JSTypeNative type, boolean deref) {
    return getReadableTypeNameHelper(registry, registry.getNativeType(type), deref);
  }

  private String getReadableTypeNameHelper(JSTypeRegistry registry, JSType type) {
    return getReadableTypeNameHelper(registry, type, false);
  }

  private String getReadableTypeNameHelper(JSTypeRegistry registry, JSType type, boolean deref) {
    Node n = new Node(Token.ADD);
    n.setJSType(type);
    return registry.getReadableJSTypeName(n, deref);
  }

  private void assertTypeEquals(JSType a, JSType b) {
    Asserts.assertTypeEquals(a, b);
  }
}
