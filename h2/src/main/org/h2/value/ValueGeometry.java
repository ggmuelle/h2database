/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.value;

import static org.h2.util.geometry.EWKBUtils.EWKB_SRID;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

import org.h2.mvstore.rtree.SpatialKey;
import org.h2.util.Bits;
import org.h2.util.StringUtils;
import org.h2.util.Utils;
import org.h2.util.geometry.EWKBUtils;
import org.h2.util.geometry.EWKTUtils;
import org.h2.util.geometry.GeometryUtils.EnvelopeTarget;

/**
 * Implementation of the GEOMETRY data type.
 *
 * @author Thomas Mueller
 * @author Noel Grandin
 * @author Nicolas Fortin, Atelier SIG, IRSTV FR CNRS 24888
 */
public abstract class ValueGeometry<T> extends Value {

    private static final double[] UNKNOWN_ENVELOPE = new double[0];

    /**
     * As conversion from/to WKB cost a significant amount of CPU cycles, WKB
     * are kept in ValueGeometry instance.
     *
     * We always calculate the WKB, because not all WKT values can be
     * represented in WKB, but since we persist it in WKB format, it has to be
     * valid in WKB
     */
    private final byte[] bytes;

    private final int hashCode;

    /**
     * Geometry type and dimension system in OGC geometry code format (type +
     * dimensionSystem * 1000).
     */
    private final int typeAndDimensionSystem;

    /**
     * Spatial reference system identifier.
     */
    private final int srid;

    /**
     * The envelope of the value. Calculated only on request.
     */
    private double[] envelope;

    /**
     * The value. Converted from WKB only on request as conversion from/to WKB
     * cost a significant amount of CPU cycles.
     */
    private T geometry;

    /**
     * Create a new geometry object.
     *
     * @param bytes the EWKB bytes
     * @param envelope the envelope
     */
    protected ValueGeometry(byte[] bytes, double[] envelope, T geometry) {
//        if (bytes.length < 9 || bytes[0] != 0) {
//            throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, StringUtils.convertBytesToHex(bytes));
//        }
        this.bytes = bytes;
        this.envelope = envelope;
        this.geometry = geometry;
        int t = Bits.readInt(bytes, 1);
        srid = (t & EWKB_SRID) != 0 ? Bits.readInt(bytes, 5) : 0;
        typeAndDimensionSystem = (t & 0xffff) % 1_000 + EWKBUtils.type2dimensionSystem(t) * 1_000;
        hashCode = Arrays.hashCode(bytes);
    }

    public abstract T getGeometry();
    
    /**
     * Returns the internal geometry instance which should be immutable.
     * @return the internal geometry instance which should be immutable
     */
    @SuppressWarnings("unchecked")
    public T getGeometryNoCopy() {
    	if (geometry == null) {
    		geometry = (T)getGeometryFactory().getGeometry(bytes);
    	}
    	return geometry;
    }

    /**
     * Returns geometry type and dimension system in OGC geometry code format
     * (type + dimensionSystem * 1000).
     *
     * @return geometry type and dimension system
     */
    public int getTypeAndDimensionSystem() {
        return typeAndDimensionSystem;
    }

    /**
     * Returns geometry type.
     *
     * @return geometry type and dimension system
     */
    public int getGeometryType() {
        return typeAndDimensionSystem % 1_000;
    }

    /**
     * Return a minimal dimension system that can be used for this geometry.
     *
     * @return dimension system
     */
    public int getDimensionSystem() {
        return typeAndDimensionSystem / 1_000;
    }

    /**
     * Return a spatial reference system identifier.
     *
     * @return spatial reference system identifier
     */
    public int getSRID() {
        return srid;
    }

    /**
     * Return an envelope of this geometry. Do not modify the returned value.
     *
     * @return envelope of this geometry
     */
    public double[] getEnvelopeNoCopy() {
        if (envelope == UNKNOWN_ENVELOPE) {
            EnvelopeTarget target = new EnvelopeTarget();
            EWKBUtils.parseEWKB(bytes, target);
            envelope = target.getEnvelope();
        }
        return envelope;
    }

    /**
     * Test if this geometry envelope intersects with the other geometry
     * envelope.
     *
     * @param r the other geometry
     * @return true if the two overlap
     */
    protected abstract boolean _intersectsBoundingBox(ValueGeometry<T> r);
    
    /**
     * Test if this geometry envelope intersects with the other geometry
     * envelope.
     * 
     * @param r the other geometry
     * @return true if the two overlap
     */
    @SuppressWarnings("unchecked")
    public final boolean intersectsBoundingBox(ValueGeometry<?> r) {
		if (!getClass().isInstance(r)) {
			return false; // not supported and should never happen
		}

		return _intersectsBoundingBox((ValueGeometry<T>) r);
	}

    /**
     * Get the union.
     *
     * @param r the other geometry
     * @return the union of this geometry envelope and another geometry envelope
     */
    protected abstract Value _getEnvelopeUnion(ValueGeometry<T> r);
    
    /**
	 * Get the union.
	 *
	 * @param r the other geometry
	 * @return the union of this geometry envelope and another geometry envelope
	 */
	@SuppressWarnings("unchecked")
	public final Value getEnvelopeUnion(ValueGeometry<?> r) {
		if (!getClass().isInstance(r)) {
			return ValueNull.INSTANCE; // not supported and should never happen
		}

		return _getEnvelopeUnion((ValueGeometry<T>) r);
	}
    
    @Override
    public TypeInfo getType() {
        return TypeInfo.TYPE_GEOMETRY;
    }

    @Override
    public int getValueType() {
        return GEOMETRY;
    }

    @Override
    public StringBuilder getSQL(StringBuilder builder) {
        // Using bytes is faster than converting to EWKT.
        builder.append("X'");
        return StringUtils.convertBytesToHex(builder, getBytesNoCopy()).append("'::Geometry");
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public Object getObject() {
        if (ValueGeometry.isGeometryFactoryInitialized()) {
            return getGeometry();
        }
        return getEWKT();
    }

    @Override
    public byte[] getBytes() {
        return Utils.cloneByteArray(bytes);
    }

    @Override
    public byte[] getBytesNoCopy() {
        return bytes;
    }

    @Override
    public void set(PreparedStatement prep, int parameterIndex) throws SQLException {
        prep.setBytes(parameterIndex, bytes);
    }

    @Override
    public int getMemory() {
        return bytes.length * 20 + 24;
    }

    @Override
    public abstract boolean equals(Object other);

    /**
     * Get the value in Extended Well-Known Text format.
     *
     * @return the extended well-known text
     */
    public String getEWKT() {
        return EWKTUtils.ewkb2ewkt(bytes, getDimensionSystem());
    }

    /**
     * Get the value in extended Well-Known Binary format.
     *
     * @return the extended well-known binary
     */
    public byte[] getEWKB() {
        return bytes;
    }
    
    /**
     * Returns the {@link SpatialKey} for the given row key. 
     * @param id the row key
     * @return the {@link SpatialKey} for the given row key
     */	    
    public abstract SpatialKey getSpatialKey(long id);
}
