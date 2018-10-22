/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.sql.expression.function.scalar.string;

import org.elasticsearch.xpack.sql.expression.Expression;
import org.elasticsearch.xpack.sql.expression.FieldAttribute;
import org.elasticsearch.xpack.sql.expression.function.scalar.BinaryScalarFunction;
import org.elasticsearch.xpack.sql.expression.gen.script.ScriptTemplate;
import org.elasticsearch.xpack.sql.tree.Location;
import org.elasticsearch.xpack.sql.type.DataType;

import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;

import static org.elasticsearch.xpack.sql.expression.gen.script.ParamsBuilder.paramsBuilder;

/**
 * Base class for binary functions that have the first parameter a string, the second parameter a number
 * or a string and the result can be a string or a number.
 */
public abstract class BinaryStringFunction<T,R> extends BinaryScalarFunction {

    protected BinaryStringFunction(Location location, Expression left, Expression right) {
        super(location, left, right);
    }

    /*
     * the operation the binary function handles can receive one String argument, a number or String as second argument
     * and it can return a number or a String. The BiFunction below is the base operation for the subsequent implementations.
     * T is the second argument, R is the result of applying the operation.
     */
    protected abstract BiFunction<String, T, R> operation();

    @Override
    protected TypeResolution resolveType() {
        if (!childrenResolved()) {
            return new TypeResolution("Unresolved children");
        }

        if (!left().dataType().isString()) {
            return new TypeResolution("'%s' requires first parameter to be a string type, received %s", functionName(), left().dataType());
        }
                
        return resolveSecondParameterInputType(right().dataType());
    }

    protected abstract TypeResolution resolveSecondParameterInputType(DataType inputType);

    @Override
    public Object fold() {
        @SuppressWarnings("unchecked")
        T fold = (T) right().fold();
        return operation().apply((String) left().fold(), fold);
    }

    @Override
    protected String scriptMethodName() {
        return operation().toString().toLowerCase(Locale.ROOT);
    }

    @Override
    public ScriptTemplate scriptWithField(FieldAttribute field) {
        return new ScriptTemplate(processScript("doc[{}].value"),
                paramsBuilder().variable(field.isInexact() ? field.exactAttribute().name() : field.name()).build(),
                dataType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(left(), right());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        BinaryStringFunction<?,?> other = (BinaryStringFunction<?,?>) obj;
        return Objects.equals(other.left(), left())
            && Objects.equals(other.right(), right());
    }
}