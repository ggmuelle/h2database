/*
 * Copyright 2004-2022 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.value;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.h2.api.SpatialDriver;
import org.h2.api.ValueGeometryFactory;
import org.h2.util.geometry.EWKBUtils;
import org.h2.util.geometry.EWKTUtils.EWKTTarget;
import org.h2.util.geometry.GeometryUtils;

/**
 * Implementation of the GEOMETRY data type.
 *
 * @author Thomas Mueller
 * @author Noel Grandin
 * @author Nicolas Fortin, Atelier SIG, IRSTV FR CNRS 24888
 */
public abstract class ValueGeometry<T> extends ValueBytesBase {

    /**
     * Factory which provides a couple of methods to create a {@link IGeometry}
     * instance.
     */
	private static final ValueGeometryFactory<? extends ValueGeometry<?>, ?> GEOMETRY_FACTORY;

	static {
		ServiceLoader<SpatialDriver> geometryFactories = ServiceLoader.load(SpatialDriver.class);
		Iterator<SpatialDriver> geometryFactoryIterator = geometryFactories.iterator();
		GEOMETRY_FACTORY = (geometryFactoryIterator.hasNext()
				? geometryFactories.iterator().next().createGeometryFactory()
				: new JTSValueGeometryFactory());
	}	
	
    public static final double[] UNKNOWN_ENVELOPE = new double[0];

    /**
     * Geometry type and dimension system in OGC geometry code format (type +
     * dimensionSystem * 1000).
     */
    protected int typeAndDimensionSystem;

    /**
     * Spatial reference system identifier.
     */
    protected int srid;

    /**
     * The envelope of the value. Calculated only on request.
     */
    protected double[] envelope;

    /**
     * The value. Converted from WKB only on request as conversion from/to WKB
     * cost a significant amount of CPU cycles.
     */
    protected T geometry;

    /**
     * Create a new geometry object.
     *
     * @param bytes the EWKB bytes
     * @param envelope the envelope
     */
    protected ValueGeometry(byte[] bytes, double[] envelope) {
        super(bytes);
    }

    /**
     * Get or create a geometry value for the given geometry.
     *
     * @param o the geometry object (of type
     *            org.locationtech.jts.geom.Geometry)
     * @return the value
     */
    public static ValueGeometry<?> getFromGeometry(Object o) {
    	return GEOMETRY_FACTORY.getFromGeometry(o);
    }

    /**
     * Get or create a geometry value for the given geometry.
     *
     * @param s the WKT or EWKT representation of the geometry
     * @return the value
     */
    public static ValueGeometry<?> get(String s) {
    	return GEOMETRY_FACTORY.get(s);
    }

    /**
     * Get or create a geometry value for the given internal EWKB representation.
     *
     * @param bytes the WKB representation of the geometry. May not be modified.
     * @return the value
     */
    public static ValueGeometry<?> get(byte[] bytes) {
    	return GEOMETRY_FACTORY.get(bytes);
    }

    /**
     * Get or create a geometry value for the given EWKB value.
     *
     * @param bytes the WKB representation of the geometry
     * @return the value
     */
    public static ValueGeometry<?> getFromEWKB(byte[] bytes) {
    	return GEOMETRY_FACTORY.getFromEWKB(bytes);
    }

    /**
     * Creates a geometry value for the given envelope.
     *
     * @param envelope envelope. May not be modified.
     * @return the value
     */
    public static Value fromEnvelope(double[] envelope) {
        return GEOMETRY_FACTORY.fromEnvelope(envelope);
    }

    public static Class<?> getGeometryClass() {
        return GEOMETRY_FACTORY.getGeometryClass();
    }
    
    /**
     * Get a copy of geometry object. Geometry object is mutable. The returned
     * object is therefore copied before returning.
     *
     * @return a copy of the geometry object
     */
    public abstract T getGeometry();

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
    public abstract double[] getEnvelopeNoCopy();

    /**
     * Test if this geometry envelope intersects with the other geometry
     * envelope.
     *
     * @param r the other geometry
     * @return true if the two overlap
     */
    public boolean intersectsBoundingBox(ValueGeometry<?> r) {
        return GeometryUtils.intersects(getEnvelopeNoCopy(), r.getEnvelopeNoCopy());
    }

    /**
     * Get the union.
     *
     * @param r the other geometry
     * @return the union of this geometry envelope and another geometry envelope
     */
    public Value getEnvelopeUnion(ValueGeometry<?> r) {
        return fromEnvelope(GeometryUtils.union(getEnvelopeNoCopy(), r.getEnvelopeNoCopy()));
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
    public StringBuilder getSQL(StringBuilder builder, int sqlFlags) {
        builder.append("GEOMETRY ");
        if ((sqlFlags & ADD_PLAN_INFORMATION) != 0) {
            EWKBUtils.parseEWKB(value, new EWKTTarget(builder.append('\''), getDimensionSystem()));
            builder.append('\'');
        } else {
            super.getSQL(builder, DEFAULT_SQL_FLAGS);
        }
        return builder;
    }

    @Override
    public int getMemory() {
        return value.length * 20 + 24;
    }

}
