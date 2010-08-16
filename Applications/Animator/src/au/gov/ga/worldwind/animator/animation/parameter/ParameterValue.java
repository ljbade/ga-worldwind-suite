/**
 * 
 */
package au.gov.ga.worldwind.animator.animation.parameter;

import gov.nasa.worldwind.Restorable;

import java.io.Serializable;

import au.gov.ga.worldwind.animator.math.vector.Vector;

/**
 * A {@link ParameterValue} represents a snapshot of the value of a {@link Parameter}
 * at a given frame.
 * <p/>
 * {@link ParameterValue}s can be associated with key frames to record the state of a {@link Parameter}
 * at a given key frame, or can be calculated by interpolating between two key frames.
 * 
 * @author James Navin (james.navin@ga.gov.au)
 */
public interface ParameterValue<V extends Vector<V>> extends Restorable, Serializable
{
	/**
	 * @return The value of this parameter value
	 */
	V getValue();
	
	/**
	 * Set the value of this parameter value
	 * 
	 * @param value The value to set
	 */
	void setValue(V value);
	
	/**
	 * @return The {@link Parameter} that 'owns' this value
	 */
	Parameter<V> getOwner();
}