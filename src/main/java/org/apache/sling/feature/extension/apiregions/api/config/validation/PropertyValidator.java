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

import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.sling.feature.extension.apiregions.api.config.Mode;
import org.apache.sling.feature.extension.apiregions.api.config.Option;
import org.apache.sling.feature.extension.apiregions.api.config.PlaceholderPolicy;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyDescription;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyType;

/**
 * Validate a configuration property or framework property
 */
public class PropertyValidator {
    
    private boolean liveValues = false;

    /**
     * Are live values validated?
     * @return {@code true} if live values are validated
     * @since 1.4
     */
    public boolean isLiveValues() {
        return liveValues;
    }

    /**
     * Set whether live values are validated.
     * @param value Flag for validating live values
     * @since 1.4
     */
    public void setLiveValues(final boolean value) {
        this.liveValues = value;
    }

	/**
	 * Validate the value against the property definition
     * @param value The value to validate
     * @param desc The property description
	 * @return A property validation result
	 */
	public PropertyValidationResult validate(final Object value, final PropertyDescription desc) {
        return this.validate(value, desc, Mode.STRICT);
    }

    /**
	 * Validate the value against the property definition
     * @param value The value to validate
     * @param desc The property description
     * @param mode Optional validation mode - this mode is used if the description does not define a mode. Defaults to {@link Mode#STRICT}.
	 * @return A property validation result
     * @since 1.2.0
	 */
	public PropertyValidationResult validate(final Object value, final PropertyDescription desc, final Mode mode) {
        final Context context = new Context();
        context.description = desc;
        context.validationMode = desc.getMode() != null ? desc.getMode() : (mode != null ? mode : Mode.STRICT);

        if ( value == null ) {
            if ( desc.isRequired() ) {
                setResult(context, "No value provided");
            }
		} else {
			final List<Object> values;
			if ( value.getClass().isArray() ) {
				// array
				values = new ArrayList<>();
                for(int i=0;i<Array.getLength(value);i++) {
					values.add(Array.get(value, i));
				}
			} else if ( value instanceof Collection ) { 
				// collection
				values = new ArrayList<>();
				final Collection<?> c = (Collection<?>)value;
				for(final Object o : c) {
					values.add(o);
				}
			} else {
				// single value
				values = null;
				validateValue(context, value);
			}

			if ( values != null ) {
                // array or collection
                for(final Object val : values) {
                    validateValue(context, val);
                }
                validateList(context, values);
            }
            
            if ( desc.getDeprecated() != null ) {
                context.result.getWarnings().add(desc.getDeprecated());
            }
		}
		return context.result;
	}

    void setResult(final Context context, final String msg) {
        if ( context.validationMode == Mode.STRICT ) {
            context.result.getErrors().add(msg);
        } else if ( context.validationMode == Mode.LENIENT || context.validationMode == Mode.DEFINITIVE ) {
            context.result.getWarnings().add(msg);
        }
        if ( context.validationMode == Mode.DEFINITIVE || context.validationMode == Mode.SILENT_DEFINITIVE ) {
            context.result.setUseDefaultValue(true);
            context.result.setDefaultValue(context.description.getDefaultValue());
        }
    }

    /**
     * Validate a multi value
     * @param prop The property description
     * @param values The values
     * @param messages The messages to record errors
     */
    void validateList(final Context context, final List<Object> values) {
        if ( context.description.getCardinality() > 0 && values.size() > context.description.getCardinality() ) {
            setResult(context, "Array/collection contains too many elements, only " + context.description.getCardinality() + " allowed");
        }
        if ( context.description.getIncludes() != null ) {
            for(final String inc : context.description.getIncludes()) {
                boolean found = false;
                for(final Object val : values) {
                    if ( inc.equals(val.toString())) {
                        found = true;
                        break;
                    }
                }
                if ( !found ) {
                    setResult(context, "Required included value " + inc + " not found");
                }
            }
        }
        if ( context.description.getExcludes() != null ) {
            for(final String exc : context.description.getExcludes()) {
                boolean found = false;
                for(final Object val : values) {
                    if ( exc.equals(val.toString())) {
                        found = true;
                        break;
                    }
                }
                if ( found ) {
                    setResult(context, "Required excluded value " + exc + " found");
                }
            }
        }
    }

    private static final List<String> PLACEHOLDERS = Arrays.asList("$[env:", "$[secret:", "$[prop:");

	void validateValue(final Context context, final Object value) {
		if ( value != null ) {
            // check for placeholder
            boolean hasPlaceholder = false;
            if ( value instanceof String ) {
                final String strVal = (String)value;
                for(final String p : PLACEHOLDERS) {
                    if ( strVal.contains(p) ) {
                        hasPlaceholder = true;
                        break;
                    }
                }
            }
            if ( !hasPlaceholder ) {
                switch ( context.description.getType() ) {
                    case BOOLEAN : validateBoolean(context, value);
                                break;
                    case BYTE : validateByte(context, value);
                                break;
                    case CHARACTER : validateCharacter(context, value);
                                break;
                    case DOUBLE : validateDouble(context, value); 
                                break;
                    case FLOAT : validateFloat(context, value); 
                                break;
                    case INTEGER : validateInteger(context, value);
                                break;
                    case LONG : validateLong(context, value);
                                break;
                    case SHORT : validateShort(context, value);
                                break;
                    case STRING : validateRequired(context, value);
                                break;
                    case EMAIL : validateEmail(context, value); 
                                break;
                    case PASSWORD : validatePassword(context, value, false);
                                break;
                    case URL : validateURL(context, value);
                            break;
                    case PATH : validatePath(context, value);
                                break;
                    default : context.result.getErrors().add("Unable to validate value - unknown property type : " + context.description.getType());
                }
                validateRegex(context, value);
                validateOptions(context, value);
                if ( context.description.getType() != PropertyType.PASSWORD ) {
                    validatePlaceholderPolicy(context, value, false);              
                }
            } else {
                // placeholder is present
                if ( context.description.getType() == PropertyType.PASSWORD ) {
                    validatePassword(context, value, true);
                } else if ( context.description.getType() == PropertyType.STRING ) {
                    if ( context.description.getRegex() != null) {
                        // if a regex is specified validate against it, otherwise accept any string
                        validateRegex(context, value);
                    } else if ( context.description.getOptions() != null || context.description.isRequired() ) {
                        // we mark the result as skipped if a regex or options are set
                        context.result.markSkipped();
                    }
                } else {
                    context.result.markSkipped();
                }
                if ( context.description.getType() != PropertyType.PASSWORD ) {
                    validatePlaceholderPolicy(context, value, true);              
                }
            }
        } else {
			setResult(context, "Null value provided for validation");
		}
	}
	
	void validateRequired(final Context context, final Object value) {
        if ( context.description.isRequired() ) {
            final String val = value.toString();
            if ( val.isEmpty() ) {
                setResult(context, "Value is required");
            }
        }
    }

    void validateBoolean(final Context context, final Object value) {
        if ( ! (value instanceof Boolean) ) {
			if ( value instanceof String ) {
				final String v = (String)value;
				if ( ! v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false") ) {
                    setResult(context, "Boolean value must either be true or false, but not " + value);
				}
			} else {
				setResult(context, "Boolean value must either be of type Boolean or String : " + value);
			}
		}
	}

	void validateByte(final Context context, final Object value) {
        if ( ! (value instanceof Byte) ) {
			if ( value instanceof String ) {
				final String v = (String)value;
				try {
					validateRange(context, Byte.valueOf(v));
				} catch ( final NumberFormatException nfe ) {
                    setResult(context, "Value is not a valid Byte : " + value);
                }
            } else if ( value instanceof Number ) {
                validateRange(context, ((Number)value).byteValue());            
			} else {
				setResult(context, "Byte value must either be of type Byte or String : " + value);
			}
		} else {
			validateRange(context, (Byte)value);
		}
	}

	void validateShort(final Context context, final Object value) {
        if ( ! (value instanceof Short) ) {
			if ( value instanceof String ) {
				final String v = (String)value;
				try {
					validateRange(context, Short.valueOf(v));
				} catch ( final NumberFormatException nfe ) {
                    setResult(context, "Value is not a valid Short : " + value);
				}
            } else if ( value instanceof Number ) {
                validateRange(context, ((Number)value).shortValue());            
			} else {
				setResult(context, "Short value must either be of type Short or String : " + value);
			}
		} else {
			validateRange(context, (Short)value);
		}
	}

	void validateInteger(final Context context, final Object value) {
        if ( ! (value instanceof Integer) ) {
			if ( value instanceof String ) {
				final String v = (String)value;
				try {
					validateRange(context, Integer.valueOf(v));
				} catch ( final NumberFormatException nfe ) {
                    setResult(context, "Value is not a valid Integer : " + value);
				}
            } else if ( value instanceof Number ) {
                validateRange(context, ((Number)value).intValue());            
			} else {
				setResult(context, "Integer value must either be of type Integer or String : " + value);
			}
		} else {
			validateRange(context, (Integer)value);
		}
	}

	void validateLong(final Context context, final Object value) {
        if ( ! (value instanceof Long) ) {
			if ( value instanceof String ) {
				final String v = (String)value;
				try {
					validateRange(context, Long.valueOf(v));
				} catch ( final NumberFormatException nfe ) {
                    setResult(context, "Value is not a valid Long : " + value);
				}
            } else if ( value instanceof Number ) {
                validateRange(context, ((Number)value).longValue());            
			} else {
				setResult(context, "Long value must either be of type Long or String : " + value);
			}
		} else {
			validateRange(context, (Long)value);
		}
	}

	void validateFloat(final Context context, final Object value) {
        if ( ! (value instanceof Float) ) {
			if ( value instanceof String ) {
				final String v = (String)value;
				try {
					validateRange(context, Float.valueOf(v));
				} catch ( final NumberFormatException nfe ) {
                    setResult(context, "Value is not a valid Float : " + value);
				}
            } else if ( value instanceof Number ) {
                validateRange(context, ((Number)value).floatValue());            
			} else {
				setResult(context, "Float value must either be of type Float or String : " + value);
			}
		} else {
			validateRange(context, (Float)value);
		}
	}

	void validateDouble(final Context context, final Object value) {
        if ( ! (value instanceof Double) ) {
			if ( value instanceof String ) {
				final String v = (String)value;
				try {
					validateRange(context, Double.valueOf(v));
				} catch ( final NumberFormatException nfe ) {
                    setResult(context, "Value is not a valid Double : " + value);
				}
            } else if ( value instanceof Number ) {
                validateRange(context, ((Number)value).doubleValue());            
			} else {
				setResult(context, "Double value must either be of type Double or String : " + value);
			}
		} else {
			validateRange(context, (Double)value);
		}
	}

	void validateCharacter(final Context context, final Object value) {
        if ( ! (value instanceof Character) ) {
			if ( value instanceof String ) {
				final String v = (String)value;
				if ( v.length() > 1 ) {
                    setResult(context, "Value is not a valid Character : " + value);
				}
			} else {
				setResult(context, "Character value must either be of type Character or String : " + value);
			}
		}
	}

	void validateURL(final Context context, final Object value) {
		final String val = value.toString();
		try {
			new URL(val);
		} catch ( final MalformedURLException mue) {
			setResult(context, "Value is not a valid URL : " + val);
		}
	}

	void validateEmail(final Context context, final Object value) {
		final String val = value.toString();
		// poor man's validation (should probably use InternetAddress)
		if ( !val.contains("@") ) {
			setResult(context, "Not a valid email address " + val);
		}
	}

	void validatePassword(final Context context, final Object value, final boolean hasPlaceholder) {
        if ( !this.isLiveValues() && !hasPlaceholder && context.description.getPlaceholderPolicy() != PlaceholderPolicy.DENY ) {
            setResult(context, "Value for a password must use a placeholder");
        }
	}

	void validatePath(final Context context, final Object value) {
		final String val = value.toString();
		// poor man's validation 
		if ( !val.startsWith("/") ) {
			setResult(context, "Not a valid path " + val);
		}
	}

    void validateRange(final Context context, final Number value) {
	    if ( context.description.getRange() != null ) {
            if ( context.description.getRange().getMin() != null ) {
                if ( value instanceof Float || value instanceof Double ) {
                    final double min = context.description.getRange().getMin().doubleValue();
                    if ( value.doubleValue() < min ) {
                            setResult(context, "Value " + value + " is too low; should not be lower than " + context.description.getRange().getMin());
                    }    
                } else {
                    final long min = context.description.getRange().getMin().longValue();
                    if ( value.longValue() < min ) {
                        setResult(context, "Value " + value + " is too low; should not be lower than " + context.description.getRange().getMin());
                    }    
                }
            }
            if ( context.description.getRange().getMax() != null ) {
                if ( value instanceof Float || value instanceof Double ) {
                    final double max = context.description.getRange().getMax().doubleValue();
                    if ( value.doubleValue() > max ) {
                        setResult(context, "Value " + value + " is too high; should not be higher than " + context.description.getRange().getMax());
                    }    
                } else {
                    final long max = context.description.getRange().getMax().longValue();
                    if ( value.longValue() > max ) {
                        setResult(context, "Value " + value + " is too high; should not be higher than " + context.description.getRange().getMax());
                    }    
                }
            }
		}
	}

    void validateRegex(final Context context, final Object value) {
        if ( context.description.getRegexPattern() != null ) {
            if ( !context.description.getRegexPattern().matcher(value.toString()).matches() ) {
                setResult(context, "Value " + value + " does not match regex " + context.description.getRegex());
            }
        }
    }

    void validateOptions(final Context context, final Object value) {
        if ( context.description.getOptions() != null ) {
            boolean found = false;
            for(final Option opt : context.description.getOptions()) {
                if ( opt.getValue().equals(value.toString() ) ) {
                    found = true; 
                }
            }
            if ( !found ) {
                setResult(context, "Value " + value + " does not match provided options");
            }
        }
    }

    void validatePlaceholderPolicy(final Context context, final Object value, final boolean hasPlaceholder) {
        // only check policy if no live values
        if ( !this.isLiveValues() ) {
            // for policy default and allow nothing needs to be validated
            if ( context.description.getPlaceholderPolicy() == PlaceholderPolicy.DENY && hasPlaceholder ) {
                setResult(context, "Placeholder in value is not allowed");
            }  else if ( context.description.getPlaceholderPolicy() == PlaceholderPolicy.REQUIRE && !hasPlaceholder ) {
                setResult(context, "Value must use a placeholder");
            }
        } 
    }         

    static final class Context {

        public final PropertyValidationResult result = new PropertyValidationResult();

        public PropertyDescription description;

        public Mode validationMode;
    }
}
