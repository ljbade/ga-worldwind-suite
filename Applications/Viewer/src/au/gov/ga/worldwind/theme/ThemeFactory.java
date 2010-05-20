package au.gov.ga.worldwind.theme;

import gov.nasa.worldwind.util.WWXML;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import au.gov.ga.worldwind.panels.dataset.IDataset;
import au.gov.ga.worldwind.panels.dataset.ILayerDefinition;
import au.gov.ga.worldwind.panels.dataset.LayerDefinition;
import au.gov.ga.worldwind.panels.dataset.LazyDataset;
import au.gov.ga.worldwind.util.Icons;

public class ThemeFactory
{
	public static Theme createFromXML(Object source, URL context)
	{
		if (source == null)
			return null;

		Element element = null;
		if (source instanceof Element)
			element = (Element) source;
		else if (source instanceof Document)
			element = ((Document) source).getDocumentElement();
		else
		{
			Document document = WWXML.openDocument(source);
			if (document == null)
				return null;

			element = document.getDocumentElement();
		}

		BasicTheme theme = new BasicTheme(WWXML.getText(element, "ThemeName"));

		theme.setMenuBar(getBoolean(element, "MenuBar", false));
		theme.setStatusBar(getBoolean(element, "StatusBar", false));

		theme.setHUDs(parseHUDs(element, "HUD"));
		theme.setPanels(parsePanels(element, "Panel"));
		theme.setDatasets(parseDatasets(element, "Dataset", context));
		theme.setLayers(parseLayers(element, "Layer", context));

		theme.setInitialLatitude(WWXML.getDouble(element, "InitialLatitude", null));
		theme.setInitialLongitude(WWXML.getDouble(element, "InitialLongitude", null));
		theme.setInitialAltitude(WWXML.getDouble(element, "InitialAltitude", null));
		theme.setInitialHeading(WWXML.getDouble(element, "InitialHeading", null));
		theme.setInitialPitch(WWXML.getDouble(element, "InitialPitch", null));

		return theme;
	}

	private static boolean getBoolean(Element context, String path, boolean def)
	{
		Boolean b = WWXML.getBoolean(context, path, null);
		if (b == null)
			return def;
		return b;
	}

	private static URL getURL(Element element, String path, URL context)
			throws MalformedURLException
	{
		String text = WWXML.getText(element, path);
		return getURL(text, context);
	}

	private static URL getURL(String text, URL context) throws MalformedURLException
	{
		if (text == null || text.length() == 0)
			return null;
		if (context == null)
			return new URL(text);
		return new URL(context, text);
	}

	private static List<ThemeHUD> parseHUDs(Element context, String path)
	{
		List<ThemeHUD> huds = new ArrayList<ThemeHUD>();
		Element[] elements = WWXML.getElements(context, path, null);
		if (elements != null)
		{
			for (Element element : elements)
			{
				String className = WWXML.getText(element, "@className");
				String position = WWXML.getText(element, "@position");
				boolean enabled = getBoolean(element, "@enabled", true);

				try
				{
					Class<?> c = Class.forName(className);
					Class<? extends ThemeHUD> tc = c.asSubclass(ThemeHUD.class);
					ThemeHUD hud = tc.newInstance();
					hud.setPosition(position);
					hud.setOn(enabled);
					huds.add(hud);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return huds;
	}

	private static List<ThemePanel> parsePanels(Element context, String path)
	{
		List<ThemePanel> panels = new ArrayList<ThemePanel>();
		Element[] elements = WWXML.getElements(context, path, null);
		if (elements != null)
		{
			for (Element element : elements)
			{
				String className = WWXML.getText(element, "@className");
				boolean enabled = getBoolean(element, "@enabled", true);
				String name = WWXML.getText(element, "@name");
				Double weightD = WWXML.getDouble(element, "@weight", null);
				float weight = weightD != null ? weightD.floatValue() : 1f;
				boolean resizable = getBoolean(element, "@resizable", true);

				try
				{
					Class<?> c = Class.forName(className);
					Class<? extends ThemePanel> tc = c.asSubclass(ThemePanel.class);
					ThemePanel panel = tc.newInstance();
					panel.setDisplayName(name);
					panel.setOn(enabled);
					panel.setResizable(resizable);
					panel.setWeight(weight);
					panels.add(panel);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return panels;
	}

	private static List<IDataset> parseDatasets(Element context, String path, URL urlContext)
	{
		List<IDataset> datasets = new ArrayList<IDataset>();
		Element[] elements = WWXML.getElements(context, path, null);
		if (elements != null)
		{
			for (Element element : elements)
			{
				try
				{
					String name = WWXML.getText(element, "@name");
					URL url = getURL(element, "@url", urlContext);
					URL description = getURL(element, "@description", urlContext);
					String icon = WWXML.getText(element, "@icon");
					URL iconURL = null;
					if (icon == null)
						iconURL = Icons.earth.getURL();
					else
						iconURL = getURL(icon, urlContext);

					IDataset dataset = new LazyDataset(name, url, description, iconURL, true);
					datasets.add(dataset);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return datasets;
	}

	private static List<ILayerDefinition> parseLayers(Element context, String path, URL urlContext)
	{
		List<ILayerDefinition> layers = new ArrayList<ILayerDefinition>();
		Element[] elements = WWXML.getElements(context, path, null);
		if (elements != null)
		{
			for (Element element : elements)
			{
				try
				{
					String name = WWXML.getText(element, "@name");
					URL url = getURL(element, "@url", urlContext);
					URL description = getURL(element, "@description", urlContext);
					URL icon = getURL(element, "@icon", urlContext);
					boolean enabled = getBoolean(element, "@enabled", true);

					ILayerDefinition layer =
							new LayerDefinition(name, url, description, icon, true, enabled);
					layers.add(layer);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return layers;
	}
}
