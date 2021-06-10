/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.extension.apiregions.api.config.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.feature.extension.apiregions.api.config.Mode;
import org.apache.sling.feature.extension.apiregions.api.config.Option;
import org.apache.sling.feature.extension.apiregions.api.config.PlaceholderPolicy;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyDescription;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyType;
import org.apache.sling.feature.extension.apiregions.api.config.Range;
import org.junit.Test;

public class PropertyValidatorTest {
    
    private final PropertyValidator validator = new PropertyValidator();
    
    /**
     * Helper method to validate an error based on the validation mode
     */
    private void validateError(final PropertyDescription prop, final Object value) {
        validateError(prop, value, 1);
    }
    
    /**
     * Helper method to validate an error based on the validation mode
     */
    private void validateError(final PropertyDescription prop, final Object value, final int errors) {
        PropertyValidationResult result;

        // error - strict mode 
        result = validator.validate(value, prop, Mode.STRICT);
        assertEquals(errors, result.getErrors().size());
        assertFalse(result.isValid());
        assertFalse(result.isSkipped());

        // error - mode lenient
        result = validator.validate(value, prop, Mode.LENIENT);
        assertEquals(errors, result.getWarnings().size());
        assertTrue(result.isValid());
        assertFalse(result.isSkipped());

        // error - mode silent
        result = validator.validate(value, prop, Mode.SILENT);
        assertTrue(result.getWarnings().isEmpty());
        assertTrue(result.isValid());
        assertFalse(result.isSkipped());

        // error - mode definitive
        result = validator.validate(value, prop, Mode.DEFINITIVE);
        assertEquals(errors, result.getWarnings().size());
        assertTrue(result.isValid());
        assertFalse(result.isSkipped());
        assertTrue(result.isUseDefaultValue());
        assertEquals(result.getDefaultValue(), prop.getDefaultValue());

        // error - mode silent definitive 
        result = validator.validate(value, prop, Mode.SILENT_DEFINITIVE);
        assertTrue(result.getWarnings().isEmpty());
        assertTrue(result.isValid());
        assertFalse(result.isSkipped());
        assertTrue(result.isUseDefaultValue());
        assertEquals(result.getDefaultValue(), prop.getDefaultValue());
    }

    /**
     * Helper method to validate that a value is valid and not skipped
     */
    private void validateValid(final PropertyDescription prop, final Object value) {
        final PropertyValidationResult result = validator.validate(value, prop);
        assertTrue(result.isValid());
        assertFalse(result.isSkipped());
        assertTrue(result.getErrors().isEmpty());
        assertFalse(result.isUseDefaultValue());
        assertNull(result.getDefaultValue());
    }

    @Test public void testValidateWithNull() {
        final PropertyDescription prop = new PropertyDescription();

        // prop not required - no error
        validateValid(prop, null);

        // prop required - error
        prop.setRequired(true);
        validateError(prop, null);
    }

    @Test public void testValidateBoolean() {
        final PropertyDescription prop = new PropertyDescription();
        prop.setType(PropertyType.BOOLEAN);

        validateValid(prop, Boolean.TRUE);
        validateValid(prop, Boolean.FALSE);
        validateValid(prop, "TRUE");
        validateValid(prop, "FALSE");

        validateError(prop, "yes");
        validateError(prop, 1);
    }

    @Test public void testValidateByte() {
        final PropertyDescription prop = new PropertyDescription();
        prop.setType(PropertyType.BYTE);

        validateValid(prop, (byte)1);
        validateValid(prop, "1");
        validateValid(prop, 1);

        validateError(prop, "yes");
    }

    @Test public void testValidateShort() {
        final PropertyDescription prop = new PropertyDescription();
        prop.setType(PropertyType.SHORT);

        validateValid(prop, (short)1);
        validateValid(prop, "1");
        validateValid(prop, 1);

        validateError(prop, "yes");
    }

    @Test public void testValidateInteger() {
        final PropertyDescription prop = new PropertyDescription();
        prop.setType(PropertyType.INTEGER);

        validateValid(prop, "1");
        validateValid(prop, 1);

        validateError(prop, "yes");
    }

    @Test public void testValidateLong() {
        final PropertyDescription prop = new PropertyDescription();
        prop.setType(PropertyType.LONG);

        validateValid(prop, 1L);
        validateValid(prop, "1");
        validateValid(prop, 1);

        validateError(prop, "yes");
    }

    @Test public void testValidateFloat() {
        final PropertyDescription prop = new PropertyDescription();
        prop.setType(PropertyType.FLOAT);

        validateValid(prop, 1.1);
        validateValid(prop, "1.1");
        validateValid(prop, 1);

        validateError(prop, "yes");
    }

    @Test public void testValidateDouble() {
        final PropertyDescription prop = new PropertyDescription();
        prop.setType(PropertyType.DOUBLE);

        validateValid(prop, 1.1d);
        validateValid(prop, "1.1");
        validateValid(prop, 1);

        validateError(prop, "yes");
    }

    @Test public void testValidateChar() {
        final PropertyDescription prop = new PropertyDescription();
        prop.setType(PropertyType.CHARACTER);

        validateValid(prop, 'x');
        validateValid(prop, "y");

        validateError(prop, "yes");
    }

    @Test public void testValidateUrl() {
        final PropertyDescription prop = new PropertyDescription();
        prop.setType(PropertyType.URL);

        validateValid(prop, "https://sling.apache.org/documentation");

        validateError(prop, "hello world");
    }

    @Test public void testValidateEmail() {
        final PropertyDescription prop = new PropertyDescription();
        prop.setType(PropertyType.EMAIL);

        validateValid(prop, "a@b.com");

        validateError(prop, "hello world");
    }

    @Test public void testValidatePassword() {
        final PropertyDescription prop = new PropertyDescription();
        prop.setType(PropertyType.PASSWORD);

        validateValid(prop, null);
        validateValid(prop, "$[secret:dbpassword]");

        validateError(prop, "secret");
    }

    @Test public void testValidatePath() {
        final PropertyDescription prop = new PropertyDescription();
        prop.setType(PropertyType.PATH);

        validateValid(prop, "/a/b/c");

        validateError(prop, "hello world");
    }
    
    @Test public void testValidateString() {
        final PropertyDescription desc = new PropertyDescription();

        validateValid(desc, "hello world");
        validateValid(desc, "$[prop:KEY]");

        // skip if required
        desc.setRequired(true);
        PropertyValidationResult result = validator.validate("$[prop:KEY]", desc);
        assertTrue(result.isValid());
        assertTrue(result.isSkipped());
        desc.setRequired(false);

        // skip if options
        desc.setOptions(Collections.singletonList(new Option()));
        result = validator.validate("$[prop:KEY]", desc);
        assertTrue(result.isValid());
        assertTrue(result.isSkipped());
        desc.setOptions(null);

        // skip if regexp
        desc.setRegex(".*");        
        result = validator.validate("$[prop:KEY]", desc);
        assertTrue(result.isValid());
        assertFalse(result.isSkipped());
        desc.setRegex(null);

        // empty string - not required
        validateValid(desc, "");

        // empty string - required
        desc.setRequired(true);
        validateError(desc, "");
        desc.setRequired(false);
    }

    @Test public void testValidateRange() {
        final PropertyDescription description = new PropertyDescription();
        description.setType(PropertyType.INTEGER);
         
        // no range set
        validateValid(description, 2);

        // empty range set
        description.setRange(new Range());
        validateValid(description, 2);

        // min set
        description.getRange().setMin(5);
        validateValid(description, 5);
        validateValid(description, 6);

        validateError(description, 4);

        validateValid(description, 5.0);
        validateValid(description, 6.0);

        validateError(description, 4.0);

        // max set
        description.getRange().setMax(6);
        validateValid(description, 5);
        validateValid(description, 6);

        validateError(description, 7);

        validateValid(description, 5.0);
        validateValid(description, 6.0);

        validateError(description, 7.0);
    }   
    
    @Test public void testValidateRegex() {
        final PropertyDescription prop = new PropertyDescription();

        // no regex
        validateValid(prop, "hello world");
        validateValid(prop, "world");

        // regex
        prop.setRegex("h(.*)");
        validateValid(prop, "hello world");

        validateError(prop, "world");

        // apply default
        prop.setDefaultValue("hello world");
        validateError(prop, "world");
    }

    @Test public void testValidateOptions() {
        final PropertyDescription prop = new PropertyDescription();

        // no options
        validateValid(prop, "foo");
        validateValid(prop, "bar");

        // options - with foo
        final List<Option> options = new ArrayList<>();
        final Option o1 = new Option();
        o1.setValue("foo");
        final Option o2 = new Option();
        o2.setValue("7");
        options.add(o1);
        options.add(o2);
        prop.setOptions(options);

        validateValid(prop, "foo");
        validateError(prop, "bar");
        validateValid(prop, 7);
    }
    
    @Test public void testValidateList() {
        final PropertyDescription prop = new PropertyDescription();

        final List<Object> values = new ArrayList<>();
        values.add("a");
        values.add("b");
        values.add("c");

        // default cardinality - no excludes/includes
        validateError(prop, values);

        // cardinality 3 - no excludes/includes
        prop.setCardinality(3);
        validateValid(prop, values);

        values.add("d");
        validateError(prop, values);

        // excludes
        prop.setExcludes(new String[] {"d", "e"});
        validateError(prop, values, 2);

        values.remove("d");
        validateValid(prop, values);

        // includes
        prop.setIncludes(new String[] {"b"});
        validateValid(prop, values);

        prop.setIncludes(new String[] {"x"});
        validateError(prop, values);

        values.add("x");
        values.remove("a");
        validateValid(prop, values);
    }

    @Test public void testValidateArray() {
        final PropertyDescription prop = new PropertyDescription();

        String[] values = new String[] {"a", "b", "c"};

        // default cardinality - no excludes/includes
        validateError(prop, values);

        // cardinality 3 - no excludes/includes
        prop.setCardinality(3);
        validateValid(prop, values);

        values = new String[] {"a", "b", "c", "d"};
        validateError(prop, values);

        // excludes
        prop.setExcludes(new String[] {"d", "e"});
        validateError(prop, values, 2);

        values = new String[] {"a", "b", "c"};
        validateValid(prop, values);

        // includes
        prop.setIncludes(new String[] {"b"});
        validateValid(prop, values);

        prop.setIncludes(new String[] {"x"});
        validateError(prop, values);

        values = new String[] {"b", "c", "x"};
        validateValid(prop, values);
    }

    @Test public void testDeprecation() {
        final PropertyDescription prop = new PropertyDescription();
        prop.setDeprecated("This is deprecated");

        final PropertyValidationResult result = validator.validate("foo", prop);
        assertTrue(result.isValid());
        assertEquals(1, result.getWarnings().size());
        assertEquals("This is deprecated", result.getWarnings().get(0));
    }

    @Test public void testPlaceholdersString() {
        final PropertyDescription desc = new PropertyDescription();
        desc.setType(PropertyType.PATH);

        PropertyValidationResult result = null;

        result = validator.validate("$[env:variable]", desc);
        assertTrue(result.isValid());
        assertTrue(result.isSkipped());

        result = validator.validate("$[prop:variable]", desc);
        assertTrue(result.isValid());
        assertTrue(result.isSkipped());

        result = validator.validate("$[secret:variable]", desc);
        assertTrue(result.isValid());
        assertTrue(result.isSkipped());
    }

    @Test public void testPlaceholdersNumber() {
        final PropertyDescription desc = new PropertyDescription();
        desc.setType(PropertyType.INTEGER);

        PropertyValidationResult result = null;

        result = validator.validate("$[env:variable]", desc);
        assertTrue(result.isValid());
        assertTrue(result.isSkipped());

        result = validator.validate("$[prop:variable]", desc);
        assertTrue(result.isValid());
        assertTrue(result.isSkipped());

        result = validator.validate("$[secret:variable]", desc);
        assertTrue(result.isValid());
        assertTrue(result.isSkipped());
    }

    @Test public void testPlaceholdersArray() {
        final PropertyDescription desc = new PropertyDescription();
        desc.setType(PropertyType.INTEGER);
        desc.setCardinality(-1);

        PropertyValidationResult result = null;

        result = validator.validate(new Object[] {5, "$[env:variable]"}, desc);
        assertTrue(result.isValid());
        assertTrue(result.isSkipped());

        result = validator.validate(new Object[] {"hello", "$[env:variable]"}, desc);
        assertFalse(result.isValid());
        assertTrue(result.isSkipped());
    }

    @Test public void testPlaceholderPolicyRequire() {
        final PropertyDescription desc = new PropertyDescription();
        desc.setPlaceholderPolicy(PlaceholderPolicy.REQUIRE);

        PropertyValidationResult result = null;

        result = validator.validate("$[env:variable]", desc);
        assertTrue(result.isValid());
        assertFalse(result.isSkipped());

        result = validator.validate("hello", desc);
        assertFalse(result.isValid());
        assertFalse(result.isSkipped());
    }

    @Test public void testPlaceholderPolicyDeny() {
        final PropertyDescription desc = new PropertyDescription();
        desc.setPlaceholderPolicy(PlaceholderPolicy.DENY);

        PropertyValidationResult result = null;

        result = validator.validate("$[env:variable]", desc);
        assertFalse(result.isValid());
        assertFalse(result.isSkipped());

        result = validator.validate("hello", desc);
        assertTrue(result.isValid());
        assertFalse(result.isSkipped());
    }

    @Test
    public void testPropertyDescRegex() {
        final PropertyDescription desc = new PropertyDescription();
        desc.setPlaceholderPolicy(PlaceholderPolicy.ALLOW);
        desc.setRegex("^\\w+ [^ ]+$");
        PropertyValidationResult result = validator.validate("local $[env:AEM_EXTERNALIZER_LOCAL;default=http://localhost:4502]", desc);
        assertTrue(result.isValid());
        assertFalse(result.isSkipped());

        result = validator.validate("author http://abc.def.com:9091", desc);
        assertTrue(result.isValid());
        assertFalse(result.isSkipped());

        result = validator.validate("local http://abc.def.com:9091 http://abc.ghi.com:9091", desc);
        assertFalse(result.isValid());
        assertFalse(result.isSkipped());
    }

    @Test
    public void testLiveValidation() {
        assertFalse(this.validator.isLiveValues());

        try {
            this.validator.setLiveValues(true);

            final PropertyDescription desc = new PropertyDescription();
            desc.setPlaceholderPolicy(PlaceholderPolicy.DENY);
    
            PropertyValidationResult result = null;
    
            result = validator.validate("$[env:variable]", desc);
            assertTrue(result.isValid());
            assertFalse(result.isSkipped());
    
            result = validator.validate("hello", desc);
            assertTrue(result.isValid());
            assertFalse(result.isSkipped());

        } finally {
            this.validator.setLiveValues(false);
        }
    }
}
