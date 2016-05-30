/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.xacml.geoxacml.cond;

import java.util.List;

import org.geotools.xacml.geoxacml.attr.GeometryAttribute;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BooleanAttribute;
import com.sun.xacml.attr.DoubleAttribute;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.cond.Expression;

/**
 * Checks isWithinDistance
 * 
 * @author Christian Mueller
 * 
 */
public class GeometryIsWithinDistance extends GeometryScalarFunction {

    public static final String NAME = NAME_PREFIX + "geometry-is-within-distance";

    public GeometryIsWithinDistance() {
        super(NAME, 0, new String[] { GeometryAttribute.identifier, GeometryAttribute.identifier,
                DoubleAttribute.identifier }, new boolean[] { false, false, false },
                BooleanAttribute.identifier, false);

    }

    public EvaluationResult evaluate(List<? extends Expression> inputs, EvaluationCtx context) {

        AttributeValue[] argValues = new AttributeValue[inputs.size()];
        EvaluationResult result = evalArgs(inputs, context, argValues);
        if (result != null)
            return result;

        GeometryAttribute geomAttr1 = (GeometryAttribute) (argValues[0]);
        GeometryAttribute geomAttr2 = (GeometryAttribute) (argValues[1]);
        DoubleAttribute withinDistance = (DoubleAttribute) (argValues[2]);

        boolean evalResult = false;

        try {
            evalResult = geomAttr1.getGeometry().isWithinDistance(geomAttr2.getGeometry(),
                    withinDistance.getValue());
        } catch (Throwable t) {
            return exceptionError(t);
        }
        return EvaluationResult.getInstance(evalResult);
    }

}
