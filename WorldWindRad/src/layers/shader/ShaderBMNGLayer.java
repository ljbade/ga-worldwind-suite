/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package layers.shader;

import gov.nasa.worldwind.layers.Earth.BMNGWMSLayer;
import gov.nasa.worldwind.render.DrawContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.media.opengl.GL;

public class ShaderBMNGLayer extends BMNGWMSLayer
{
	private int shaderprogram = -1;

	@Override
	public void render(DrawContext dc)
	{
		GL gl = dc.getGL();

		if (shaderprogram == -1)
		{
			int v = gl.glCreateShader(GL.GL_VERTEX_SHADER);
			int f = gl.glCreateShader(GL.GL_FRAGMENT_SHADER);
			String vsrc = "", fsrc = "", line;

			try
			{
				BufferedReader brv = new BufferedReader(new InputStreamReader(
						this.getClass()
								.getResourceAsStream("vertexshader.glsl")));
				while ((line = brv.readLine()) != null)
				{
					vsrc += line + "\n";
				}

				BufferedReader brf = new BufferedReader(new InputStreamReader(
						this.getClass().getResourceAsStream(
								"fragmentshader.glsl")));
				while ((line = brf.readLine()) != null)
				{
					fsrc += line + "\n";
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			gl.glShaderSource(v, 1, new String[] { vsrc }, new int[] { vsrc
					.length() }, 0);
			gl.glCompileShader(v);
			gl.glShaderSource(f, 1, new String[] { fsrc }, new int[] { fsrc
					.length() }, 0);
			gl.glCompileShader(f);

			shaderprogram = gl.glCreateProgram();
			gl.glAttachShader(shaderprogram, v);
			gl.glAttachShader(shaderprogram, f);
			gl.glLinkProgram(shaderprogram);
			gl.glValidateProgram(shaderprogram);

			gl.glUseProgram(shaderprogram);
			gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "tex0"), 0);
			gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "tex1"), 1);

			System.out.println("Shader program = " + shaderprogram);
		}

		gl.glUseProgram(shaderprogram);
		super.render(dc);
		gl.glUseProgram(0);
	}
}