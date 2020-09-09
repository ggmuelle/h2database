/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.value;

import org.h2.mvstore.db.SpatialKey;
import org.h2.util.geometry.EWKTUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * The {@link ValueGeometry} implementation for the JTS geometry framework.
 * 
 * @author Steve Hruda
 */
public class JTSValueGeometry extends ValueGeometry<Geometry> {

	JTSValueGeometry(byte[] bytes, double[] envelope, Geometry geometry) {
		super(bytes, envelope, geometry);
	}

	@Override
	public Geometry getGeometry() {
		return (Geometry) getGeometryNoCopy().copy();
	}

	@Override
	public boolean _intersectsBoundingBox(ValueGeometry<Geometry> r) {
		// the Geometry object caches the envelope
		return getGeometryNoCopy().getEnvelopeInternal().intersects(
				r.getGeometryNoCopy().getEnvelopeInternal());
	}

	@Override
	public Value _getEnvelopeUnion(ValueGeometry<Geometry> r) {
		GeometryFactory gf = new GeometryFactory();
		Envelope mergedEnvelope = new Envelope(getGeometryNoCopy().getEnvelopeInternal());
		mergedEnvelope.expandToInclude(r.getGeometryNoCopy().getEnvelopeInternal());
		return getGeometryFactory().getFromGeometry(gf.toGeometry(mergedEnvelope));
	}

	@Override
    public String getString() {
		return EWKTUtils.ewkb2ewkt(value, getDimensionSystem());        
//		return new WKTWriter(3).write(getGeometryNoCopy());
    }

	@Override
	public SpatialKey getSpatialKey(long id) {
		Envelope env = getGeometryNoCopy().getEnvelopeInternal();
        return new SpatialKey(id,
                (float) env.getMinX(), (float) env.getMaxX(),
                (float) env.getMinY(), (float) env.getMaxY());
	}

} 