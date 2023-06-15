/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.value;

import static org.h2.util.geometry.EWKBUtils.EWKB_SRID;

import org.h2.api.ErrorCode;
import org.h2.message.DbException;
import org.h2.util.Bits;
import org.h2.util.StringUtils;
import org.h2.util.geometry.EWKBUtils;
import org.h2.util.geometry.EWKTUtils;
import org.h2.util.geometry.JTSUtils;
import org.h2.util.geometry.GeometryUtils.EnvelopeTarget;
import org.locationtech.jts.geom.Geometry;

/**
 * The {@link ValueGeometry} implementation for the JTS geometry framework.
 * 
 * @author Steve Hruda
 */
public class JTSValueGeometry extends ValueGeometry<Geometry> {

	JTSValueGeometry(byte[] bytes, double[] envelope) {
		super(bytes, envelope);
        if (bytes.length < 9 || bytes[0] != 0) {
            throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, StringUtils.convertBytesToHex(bytes));
        }
        this.value = bytes;
        this.envelope = envelope;
        int t = Bits.readInt(bytes, 1);
        srid = (t & EWKB_SRID) != 0 ? Bits.readInt(bytes, 5) : 0;
        typeAndDimensionSystem = (t & 0xffff) % 1_000 + EWKBUtils.type2dimensionSystem(t) * 1_000;
	}

	@Override
	public Geometry getGeometry() {
        if (geometry == null) {
            try {
                geometry = JTSUtils.ewkb2geometry(value, getDimensionSystem());
            } catch (RuntimeException ex) {
                throw DbException.convert(ex);
            }
        }
        return ((Geometry) geometry).copy();
	}
	
    @Override
    public String getString() {
        return EWKTUtils.ewkb2ewkt(value, getDimensionSystem());
    }
    
    /**
     * Return an envelope of this geometry. Do not modify the returned value.
     *
     * @return envelope of this geometry
     */
    @Override
    public double[] getEnvelopeNoCopy() {
        if (envelope == ValueGeometry.UNKNOWN_ENVELOPE) {
            EnvelopeTarget target = new EnvelopeTarget();
            EWKBUtils.parseEWKB(value, target);
            envelope = target.getEnvelope();
        }
        return envelope;
    }

} 