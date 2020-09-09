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
import org.h2.util.geometry.GeometryUtils.EnvelopeAndDimensionSystemTarget;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;

/**
 * The {@link ValueGeometryFactory} implementation for the JTS geometry
 * framework.
 * 
 * @author Steve Hruda
 */
public class JTSValueGeometryFactory implements ValueGeometryFactory<JTSValueGeometry, Geometry> {

	@Override
	public Geometry getGeometry(byte[] bytes) throws DbException {
		try {
			return new WKBReader().read(bytes);
		} catch (ParseException ex) {
            throw DbException.convert(ex);
        }
	}

	@Override
	public Geometry getGeometry(String s) throws DbException {
		try {
			EnvelopeAndDimensionSystemTarget target = new EnvelopeAndDimensionSystemTarget();
			EWKTUtils.parseEWKT(s, target);
			return getGeometry(EWKTUtils.ewkt2ewkb(s, target.getDimensionSystem()));
		} catch (RuntimeException ex) {
			throw DbException.convert(ex);
		}
	}

	@Override
	public Geometry getGeometry(String s, int srid) throws DbException {
		try {
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), srid);
			return new WKTReader(geometryFactory).read(s);
		}catch(ParseException ex)
		{
			throw DbException.convert(ex);
		}
	}

	@Override
	public JTSValueGeometry get(Geometry g) {
        byte[] bytes = convertToWKB(g);
        return (JTSValueGeometry) Value.cache(new JTSValueGeometry(bytes, null, g));
    }


    @Override
	public JTSValueGeometry get(String s) {
    	return get(getGeometry(s));
    }

    @Override
    public JTSValueGeometry get(String s, int srid) {
        try {
            GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), srid);
            Geometry g = new WKTReader(geometryFactory).read(s);
            return get(g);
        } catch (ParseException ex) {
            throw DbException.convert(ex);
        }
    }

    @Override
	public JTSValueGeometry get(byte[] bytes) {
    	Geometry geometry = null;
        return (JTSValueGeometry) Value.cache(new JTSValueGeometry(bytes, null, geometry));
    }

    @Override
	public Value get(double[] envelope) {
		return envelope != null
				? (JTSValueGeometry) Value.cache(new JTSValueGeometry(EWKBUtils.envelope2wkb(envelope), envelope, null))
				: ValueNull.INSTANCE;
    }
    
    /**
     * Get or create a geometry value for the given EWKB value.
     *
     * @param bytes the WKB representation of the geometry
     * @return the value
     */
    public ValueGeometry<?> getFromEWKB(byte[] bytes) {
    	try {
    		EnvelopeAndDimensionSystemTarget target = new EnvelopeAndDimensionSystemTarget();
    		EWKBUtils.parseEWKB(bytes, target);
    		return (ValueGeometry<?>) Value.cache(new JTSValueGeometry(
    				EWKBUtils.ewkb2ewkb(bytes, target.getDimensionSystem()), target.getEnvelope(), null));
    	} catch (RuntimeException ex) {
    		throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, StringUtils.convertBytesToHex(bytes));
    	}
    }

	@Override
	public JTSValueGeometry getFromGeometry(Object g) {
    	if(!isGeometryTypeSupported(g))
    		throw new AssertionError("The given object is not compatible with this ValueGeometryFactory instance!");

    	return get((Geometry)g);
	}

	@Override
	public JTSValueGeometry get(Value g) {
		if(!(g instanceof JTSValueGeometry))
			throw new AssertionError("The given value is not compatible with this ValueGeometryFactory instance!");

		return (JTSValueGeometry) g;
	}

	@Override
	public boolean isGeometryTypeSupported(Object g) {
		return g instanceof Geometry;
	}

	@Override
	public Class<Geometry> getGeometryType() {
		return Geometry.class;
	}

    private static byte[] convertToWKB(Geometry g) {
        boolean includeSRID = g.getSRID() != 0;
        int dimensionCount = getDimensionCount(g);
        WKBWriter writer = new WKBWriter(dimensionCount, includeSRID);
        return writer.write(g);
    }

    private static int getDimensionCount(Geometry geometry) {
        ZVisitor finder = new ZVisitor();
        geometry.apply(finder);
        return finder.isFoundZ() ? 3 : 2;
    }

    /**
     * A visitor that checks if there is a Z coordinate.
     */
    static class ZVisitor implements CoordinateSequenceFilter {
        boolean foundZ;

        public boolean isFoundZ() {
            return foundZ;
        }

        @Override
        public void filter(CoordinateSequence coordinateSequence, int i) {
            if (!Double.isNaN(coordinateSequence.getZ(i))) {
                foundZ = true;
            }
        }

        @Override
        public boolean isDone() {
            return foundZ;
        }

        @Override
        public boolean isGeometryChanged() {
            return false;
        }
    }
}