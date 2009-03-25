package layers.sky;

import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.OrbitView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;

import nasa.worldwind.view.RollOrbitView;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;

public class Skysphere extends AbstractLayer
{
	private int vertexCount, triCount;
	private DoubleBuffer vb, nb, tb;
	private IntBuffer ib;
	private Texture texture;
	private boolean inited = false;
	private String texturePath = "data/skysphere/sky10.png";

	public Skysphere()
	{

	}

	private void initializeTextures(DrawContext dc)
	{
		try
		{
			InputStream stream = this.getClass().getResourceAsStream(
					"/" + texturePath);
			if (stream == null)
			{
				File file = new File(texturePath);
				if (file.exists())
				{
					stream = new FileInputStream(file);
				}
			}

			texture = TextureIO.newTexture(stream, true, null);
			texture.bind();
		}
		catch (IOException e)
		{
			String msg = Logging
					.getMessage("layers.IOExceptionDuringInitialization");
			Logging.logger().severe(msg);
			throw new WWRuntimeException(msg, e);
		}

		GL gl = dc.getGL();
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
				GL.GL_LINEAR_MIPMAP_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
				GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S,
				GL.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T,
				GL.GL_CLAMP_TO_EDGE);
		// Enable texture anisotropy, improves "tilted" world map quality.
		/*int[] maxAnisotropy = new int[1];
		gl
				.glGetIntegerv(GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT,
						maxAnisotropy, 0);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT,
				maxAnisotropy[0]);*/
	}

	@Override
	protected void doRender(DrawContext dc)
	{
		GL gl = dc.getGL();

		if (!inited)
		{
			initializeTextures(dc);
			inited = true;
		}

		//set up projection matrix
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		dc.getGLU().gluPerspective(
				dc.getView().getFieldOfView().degrees,
				dc.getView().getViewport().getWidth()
						/ dc.getView().getViewport().getHeight(), 0.1, 5.0); //TODO

		//set up modelview matrix
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		Angle heading = Angle.ZERO;
		Angle pitch = Angle.ZERO;
		Angle roll = Angle.ZERO;
		if (dc.getView() instanceof OrbitView)
		{
			heading = ((OrbitView) dc.getView()).getHeading();
			pitch = ((OrbitView) dc.getView()).getPitch();
		}
		if (dc.getView() instanceof RollOrbitView)
		{
			roll = ((RollOrbitView) dc.getView()).getRoll();
		}

		Matrix transform = Matrix.IDENTITY;
		transform = transform.multiply(Matrix.fromRotationY(heading
				.multiply(-1.0)));
		transform = transform.multiply(Matrix.fromRotationX(pitch
				.addDegrees(90)));
		transform = transform.multiply(Matrix.fromRotationZ(roll)); //TODO check

		Vec4 up = Vec4.UNIT_Y; //TODO recalculate if forward is parallel to up
		Vec4 forward = Vec4.UNIT_Z.transformBy4(transform);
		dc.getGLU().gluLookAt(0, 0, 0, forward.x, forward.y, forward.z, up.x,
				up.y, up.z);

		// Enable/Disable features
		gl.glPushAttrib(GL.GL_ENABLE_BIT);
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glDisable(GL.GL_LIGHTING);
		gl.glDisable(GL.GL_BLEND);

		//rotate upside-down texture
		gl.glRotated(180, 0, 0, 1);
		//rotate sun to be in correct position
		gl.glRotated(-83, 0, 1, 0);
		
		gl.glMatrixMode(GL.GL_TEXTURE);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glScaled(1.0d, 2.0d, 1.0d);
		
		gl.glColor3d(1, 1, 1);
		texture.bind();
		drawSphere(gl, 20, 20, 1);
		
		gl.glPopMatrix();

		// Restore enable bits and matrix
		gl.glPopAttrib();
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPopMatrix();
	}

	private void drawSphere(GL gl, int slices, int segments, double radius)
	{
		if (vb == null)
		{
			setGeometryData(slices, segments, radius, false, true);
		}

		gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
		gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);

		vb.rewind();
		nb.rewind();
		tb.rewind();
		ib.rewind();

		gl.glVertexPointer(3, GL.GL_DOUBLE, 0, vb);
		gl.glNormalPointer(GL.GL_DOUBLE, 0, nb);
		gl.glTexCoordPointer(2, GL.GL_DOUBLE, 0, tb);
		gl.glDrawElements(GL.GL_TRIANGLES, ib.limit(), GL.GL_UNSIGNED_INT, ib);

		gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
		gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
	}

	/*private DoubleBuffer cb;
	private void randomiseColor()
	{
		Random random = new Random();
		cb = BufferUtil.newDoubleBuffer(vertexCount * 3);
		for (int i = 0; i < cb.limit(); i++)
		{
			cb.put(random.nextDouble());
		}
	}*/

	private void setGeometryData(int zSamples, int radialSamples,
			double radius, boolean projected, boolean interior)
	{
		// allocate vertices
		vertexCount = (zSamples - 2) * (radialSamples + 1) + 2;
		vb = BufferUtil.newDoubleBuffer(vertexCount * 3);

		// allocate normals if requested
		nb = BufferUtil.newDoubleBuffer(vertexCount * 3);

		// allocate texture coordinates
		tb = BufferUtil.newDoubleBuffer(vertexCount * 2);

		// allocate index buffer
		triCount = 2 * (zSamples - 2) * radialSamples;
		ib = BufferUtil.newIntBuffer(triCount * 3);

		//sphere center
		Vec4 center = Vec4.UNIT_W;

		// generate geometry
		double fInvRS = 1.0f / radialSamples;
		double yFactor = 2.0f / (zSamples - 1);
		double pi = Math.PI;
		double twopi = pi * 2d;
		double halfpi = pi / 2d;
		double invpi = 1d / pi;

		// Generate points on the unit circle to be used in computing the mesh
		// points on a sphere slice.
		double[] aSin = new double[radialSamples + 1];
		double[] aCos = new double[radialSamples + 1];
		for (int iR = 0; iR < radialSamples; iR++)
		{
			double fAngle = twopi * fInvRS * iR;
			aCos[iR] = Math.cos(fAngle);
			aSin[iR] = Math.sin(fAngle);
		}
		aSin[radialSamples] = aSin[0];
		aCos[radialSamples] = aCos[0];

		// generate the sphere itself
		int i = 0;
		for (int iY = 1; iY < (zSamples - 1); iY++)
		{
			double yFraction = -1.0f + yFactor * iY; // in (-1,1)
			double y = radius * yFraction;

			// compute center of slice
			Vec4 sliceCenter = center.add3(new Vec4(0, y, 0));

			// compute radius of slice
			double sliceRadius = Math.sqrt(Math.abs(radius * radius - y * y));

			// compute slice vertices with duplication at end point
			int iSave = i;
			for (int r = 0; r < radialSamples; r++)
			{
				double radialFraction = r * fInvRS; // in [0,1)
				Vec4 radial = new Vec4(aCos[r], 0, aSin[r]);
				Vec4 v = radial.multiply3(sliceRadius).add3(sliceCenter);
				vb.put(v.x).put(v.y).put(v.z);

				Vec4 normal = new Vec4(vb.get(i * 3), vb.get(i * 3 + 1), vb
						.get(i * 3 + 2));
				normal = normal.subtract3(center).normalize3();
				if (!interior) // later we may allow interior texture vs. exterior
					nb.put(normal.x).put(normal.y).put(normal.z);
				else
					nb.put(-normal.x).put(-normal.y).put(-normal.z);

				if (!projected)
					tb.put(radialFraction).put(0.5f * (yFraction + 1.0f));
				else
					tb.put(radialFraction).put(
							invpi * (halfpi + Math.asin(yFraction)));

				i++;
			}

			double[] d3 = new double[3];
			vb.position(iSave * 3);
			vb.get(d3);
			vb.position(i * 3);
			vb.put(d3);
			nb.position(iSave * 3);
			nb.get(d3);
			nb.position(i * 3);
			nb.put(d3);

			if (!projected)
				tb.put(1.0f).put(0.5f * (yFraction + 1.0f));
			else
				tb.put(1.0f).put(invpi * (halfpi + Math.asin(yFraction)));

			i++;
		}

		// south pole
		vb.position(i * 3);
		vb.put(center.x).put(center.y - radius).put(center.z);

		nb.position(i * 3);
		if (!interior) // allow for inner texture orientation later.
			nb.put(0).put(-1).put(0);
		else
			nb.put(0).put(1).put(0);

		tb.position(i * 2);
		tb.put(0.5f).put(0.0f);

		// i++;

		// north pole
		vb.put(center.x).put(center.y + radius).put(center.z);

		if (!interior)
			nb.put(0).put(1).put(0);
		else
			nb.put(0).put(-1).put(0);

		tb.put(0.5f).put(1.0f);


		// generate connectivity
		int index = 0;
		for (int iZ = 0, iZStart = 0; iZ < (zSamples - 3); iZ++)
		{
			int i0 = iZStart;
			int i1 = i0 + 1;
			iZStart += (radialSamples + 1);
			int i2 = iZStart;
			int i3 = i2 + 1;
			for (int j = 0; j < radialSamples; j++, index += 6)
			{
				if (!interior)
				{
					ib.put(i0++);
					ib.put(i1);
					ib.put(i2);
					ib.put(i1++);
					ib.put(i3++);
					ib.put(i2++);
				}
				else
				// inside view
				{
					ib.put(i0++);
					ib.put(i2);
					ib.put(i1);
					ib.put(i1++);
					ib.put(i2++);
					ib.put(i3++);
				}
			}
		}

		// south pole triangles
		for (int j = 0; j < radialSamples; j++, index += 3)
		{
			if (!interior)
			{
				ib.put(j);
				ib.put(vertexCount - 2);
				ib.put(j + 1);
			}
			else
			{ // inside view
				ib.put(j);
				ib.put(j + 1);
				ib.put(vertexCount - 2);
			}
		}

		// north pole triangles
		int iOffset = (zSamples - 3) * (radialSamples + 1);
		for (int j = 0; j < radialSamples; j++, index += 3)
		{
			if (!interior)
			{
				ib.put(j + iOffset);
				ib.put(j + 1 + iOffset);
				ib.put(vertexCount - 1);
			}
			else
			{ // inside view
				ib.put(j + iOffset);
				ib.put(vertexCount - 1);
				ib.put(j + 1 + iOffset);
			}
		}
	}
}