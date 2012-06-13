/*******************************************************************************
 * Copyright 2012 Geoscience Australia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package au.gov.ga.worldwind.common.layers.model.gdal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import au.gov.ga.worldwind.common.layers.model.ModelLayer;
import au.gov.ga.worldwind.common.util.FastShape;

/**
 * Unit tests for the {@link GDALRasterModelProvider}
 * 
 * @author James Navin (james.navin@ga.gov.au)
 */
public class GDALRasterModelProviderTest
{

	private GDALRasterModelProvider classUnderTest;
	private Mockery mockContext;
	private ModelLayer modelLayer;

	// Test raster properties (in WGS84 lat/lon)
	private static final List<RasterProperties> TEST_RASTERS = new ArrayList<RasterProperties>();
	static
	{
		TEST_RASTERS.add(new RasterProperties()
		{
			{
				{
					name = "testgrid.tif";
					testBand = 1;
					maxLat = -25.5230437;
					minLat = -27.6117002;
					minLon = 141.7329060;
					maxLon = 144.7855579;
					xCellSize = 0.040166471522998;
					yCellSize = -0.040166471522998;
					width = 76;
					height = 52;
					minValue = -2558.0;
					maxValue = -699.0;
				}
			}
		});
	}

	@BeforeClass
	public static void init()
	{
		// IMPORTANT: This fixes resolution of GDAL path in GDALUtils for the case when 
		// tests are executed from Ant scripts other than Common
		try
		{
			String userdir = new File(System.getProperty("user.dir")).getCanonicalPath();
			System.setProperty("user.dir", userdir);
		}
		catch (IOException e)
		{
		}
	}

	@Before
	public void setup()
	{
		classUnderTest = new GDALRasterModelProvider();

		mockContext = new Mockery();

		modelLayer = mockContext.mock(ModelLayer.class);
	}

	@Test
	public void testConstructWithNull()
	{
		classUnderTest = new GDALRasterModelProvider(null);

		// Expect default parameters
		GDALRasterModelParameters defaults = new GDALRasterModelParameters();
		GDALRasterModelParameters actuals = classUnderTest.getModelParameters();

		assertEquals(defaults.getBand(), actuals.getBand());
		assertEquals(defaults.getDefaultColor(), actuals.getDefaultColor());
		assertEquals(defaults.getColorMap(), actuals.getColorMap());
		assertEquals(defaults.getMaxVariance(), actuals.getMaxVariance(), 0.001);
	}

	@Test
	public void loadWithNullUrl()
	{
		try
		{
			classUnderTest.doLoadData(null, modelLayer);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e)
		{
			// pass
		}
	}

	@Test
	public void loadWithNullLayer()
	{
		try
		{
			URL url = getClass().getResource("testgrid.tif");
			classUnderTest.doLoadData(url, null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e)
		{
			// pass
		}
	}

	@Test
	public void loadValidUrl() throws Exception
	{
		RasterProperties testRaster = TEST_RASTERS.get(0);
		URL url = getClass().getResource(testRaster.name);

		final FastShapeMatcher matcher = new FastShapeMatcher();
		mockContext.checking(new Expectations()
		{
			{
				{
					allowing(modelLayer).addShape(with(matcher));
				}
			}
		});

		boolean dataLoaded = classUnderTest.doLoadData(url, modelLayer);
		FastShape shape = matcher.shape;

		assertTrue("Data did not load as expected", dataLoaded);

		assertNotNull("Shape is null", shape);
		assertTrue("Force sorted primitives is not true", shape.isForceSortedPrimitives());
		assertTrue("Shape is not lighted", shape.isLighted());
		assertTrue("Two-sided lighting is not enabled", shape.isTwoSidedLighting());
		assertTrue(shape.isCalculateNormals());

		// With MaxVariance = 0 we expect every pixel to have a corresponding position
		List<Position> positions = shape.getPositions();
		assertNotNull(positions);
		assertEquals(testRaster.width * testRaster.height, positions.size());

		// Colour buffer should have a 4 element entry per-point 
		assertEquals(4, shape.getColorBufferElementSize());
		float[] colourBuffer = shape.getColorBuffer();
		assertNotNull(colourBuffer);
		assertEquals(testRaster.width * testRaster.height * 4, colourBuffer.length);

		// Sector will be sampled from 'bottom-left' corners of cells
		Sector sector = shape.getSector();
		assertNotNull(sector);
		assertEquals(testRaster.minLon, sector.getMinLongitude().degrees, 0.0001);
		assertEquals(testRaster.maxLon - testRaster.xCellSize, sector.getMaxLongitude().degrees, 0.0001);
		assertEquals(testRaster.minLat - testRaster.yCellSize, sector.getMinLatitude().degrees, 0.0001);
		assertEquals(testRaster.maxLat, sector.getMaxLatitude().degrees, 0.0001);
	}

	/** A simple properties class that holds raster details for use in tests */
	private static class RasterProperties
	{
		String name;
		int testBand = 1;
		double minLat;
		double minLon;
		double maxLat;
		double maxLon;
		double xCellSize;
		double yCellSize;
		int width;
		int height;
		double minValue;
		double maxValue;
	}

	/**
	 * A matcher that accepts any FastShape, and provides access for later
	 * inspection
	 */
	private static class FastShapeMatcher extends BaseMatcher<FastShape>
	{
		public FastShape shape;

		@Override
		public boolean matches(Object item)
		{
			this.shape = (FastShape) item;
			return true;
		}

		@Override
		public void describeTo(Description description)
		{
		}
	}
}