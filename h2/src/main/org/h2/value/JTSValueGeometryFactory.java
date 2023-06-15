/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.value;

import org.h2.api.ErrorCode;
import org.h2.api.ValueGeometryFactory;
import org.h2.message.DbException;
import org.h2.util.StringUtils;
import org.h2.util.geometry.EWKBUtils;
import org.h2.util.geometry.EWKTUtils;
import org.h2.util.geometry.JTSUtils;
import org.locationtech.jts.geom.Geometry;

/**
 * The {@link ValueGeometryFactory} implementation for the JTS geometry
 * framework.
 * 
 * @author Steve Hruda
 */
public class JTSValueGeometryFactory implements ValueGeometryFactory<JTSValueGeometry, Geometry> {

    /**
     * Get or create a geometry value for the given geometry.
     *
     * @param o the geometry object (of type
     *            org.locationtech.jts.geom.Geometry)
     * @return the value
     */
	@Override
    @SuppressWarnings("unchecked")
	public ValueGeometry<Geometry> getFromGeometry(Object o) {
        try {
            Geometry g = (Geometry) o;
            return (ValueGeometry<Geometry>) Value.cache(new JTSValueGeometry(JTSUtils.geometry2ewkb(g), ValueGeometry.UNKNOWN_ENVELOPE));
        } catch (RuntimeException ex) {
            throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, String.valueOf(o));
        }
    }

    /**
     * Get or create a geometry value for the given geometry.
     *
     * @param s the WKT or EWKT representation of the geometry
     * @return the value
     */
	@Override
    @SuppressWarnings("unchecked")
    public ValueGeometry<Geometry> get(String s) {
        try {
            return (ValueGeometry<Geometry>) Value.cache(new JTSValueGeometry(EWKTUtils.ewkt2ewkb(s), ValueGeometry.UNKNOWN_ENVELOPE));
        } catch (RuntimeException ex) {
            throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, s);
        }
    }

    /**
     * Get or create a geometry value for the given internal EWKB representation.
     *
     * @param bytes the WKB representation of the geometry. May not be modified.
     * @return the value
     */
	@Override
    @SuppressWarnings("unchecked")
    public ValueGeometry<Geometry> get(byte[] bytes) {
        return (ValueGeometry<Geometry>) Value.cache(new JTSValueGeometry(bytes, ValueGeometry.UNKNOWN_ENVELOPE));
    }

    /**
     * Get or create a geometry value for the given EWKB value.
     *
     * @param bytes the WKB representation of the geometry
     * @return the value
     */
	@Override
    @SuppressWarnings("unchecked")
    public ValueGeometry<Geometry> getFromEWKB(byte[] bytes) {
        try {
            return (ValueGeometry<Geometry>) Value.cache(new JTSValueGeometry(EWKBUtils.ewkb2ewkb(bytes), ValueGeometry.UNKNOWN_ENVELOPE));
        } catch (RuntimeException ex) {
            throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, StringUtils.convertBytesToHex(bytes));
        }
    }

    /**
     * Creates a geometry value for the given envelope.
     *
     * @param envelope envelope. May not be modified.
     * @return the value
     */
	@Override
    public Value fromEnvelope(double[] envelope) {
        return envelope != null
                ? Value.cache(new JTSValueGeometry(EWKBUtils.envelope2wkb(envelope), envelope))
                : ValueNull.INSTANCE;
    }

	@Override
	public Class<?> getGeometryClass() {
		return Geometry.class;
	}

}