/*
 * Copyright 2004-2015 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.api;

import org.h2.value.Value;
import org.h2.value.ValueGeometry;

/**
 * Interface of a factory which provides methods for the conversion of a
 * geometry object of a framework like JTS into a {@link ValueGeometry}. 
 *
 * @author Steve Hruda
 * @param <T> the type of your {@link ValueGeometry} implementation
 * @param <S> the type of the frameworks geometry object
 */
public interface ValueGeometryFactory<T extends ValueGeometry<S>, S> {

    /**
     * Get or create a geometry value for the given geometry.
     *
     * @param o the geometry object (of type
     *            org.locationtech.jts.geom.Geometry)
     * @return the value
     */
    public ValueGeometry<S> getFromGeometry(Object o);

    
    /**
     * Get or create a geometry value for the given geometry.
     *
     * @param s the WKT or EWKT representation of the geometry
     * @return the value
     */
    public ValueGeometry<S> get(String s);

    
    /**
     * Get or create a geometry value for the given internal EWKB representation.
     *
     * @param bytes the WKB representation of the geometry. May not be modified.
     * @return the value
     */
    public ValueGeometry<S> get(byte[] bytes);
    
    /**
     * Get or create a geometry value for the given EWKB value.
     *
     * @param bytes the WKB representation of the geometry
     * @return the value
     */
    public ValueGeometry<S> getFromEWKB(byte[] bytes);
    
    /**
     * Creates a geometry value for the given envelope.
     *
     * @param envelope envelope. May not be modified.
     * @return the value
     */
    public Value fromEnvelope(double[] envelope);


    /**
     * @return the concrete class representing geometries
     */
	public Class<?> getGeometryClass();

} 
