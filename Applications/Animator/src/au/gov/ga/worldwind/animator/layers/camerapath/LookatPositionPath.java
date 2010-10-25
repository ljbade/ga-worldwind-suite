package au.gov.ga.worldwind.animator.layers.camerapath;

import gov.nasa.worldwind.geom.Position;
import au.gov.ga.worldwind.animator.animation.Animation;
import au.gov.ga.worldwind.animator.animation.AnimationContext;
import au.gov.ga.worldwind.animator.animation.KeyFrame;

/**
 * An {@link AbstractCameraPositionPath} that draws the current animation's lookat position along with nodes representing key frames.
 */
public class LookatPositionPath extends AbstractCameraPositionPath
{

	public LookatPositionPath(Animation animation)
	{
		super(animation);
	}

	@Override
	protected Position getPathPositionAtFrame(AnimationContext context, int frame)
	{
		Position lookatPositionAtFrame = getAnimation().getCamera().getLookatPositionAtFrame(context, frame);
		
		return lookatPositionAtFrame;
	}

	@Override
	protected boolean isPathFrame(KeyFrame keyFrame)
	{
		return keyFrame.hasValueForParameter(getAnimation().getCamera().getLookAtLat()) || 
		   	   keyFrame.hasValueForParameter(getAnimation().getCamera().getLookAtLon()) || 
		   	   keyFrame.hasValueForParameter(getAnimation().getCamera().getLookAtElevation());
	}

}