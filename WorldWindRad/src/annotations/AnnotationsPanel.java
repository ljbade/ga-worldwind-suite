package annotations;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.ViewStateIterator;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.OrbitView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import nasa.worldwind.view.FlyToOrbitViewStateIterator;

import settings.Settings;
import util.FlatJButton;
import util.Icons;
import util.Util;

public class AnnotationsPanel extends JPanel
{
	private WorldWindow wwd;
	private JList list;
	private DefaultListModel model;
	private ListItem dragging;
	private AnnotationsLayer layer;
	private Frame frame;

	private class ListItem
	{
		public final JCheckBox check;
		public final Annotation annotation;

		public ListItem(Annotation annotation, JCheckBox check)
		{
			this.annotation = annotation;
			this.check = check;
		}
	}

	public AnnotationsPanel(WorldWindow wwd, Frame frame)
	{
		this.wwd = wwd;
		this.frame = frame;
		createPanel();
		populateList();
		layer = new AnnotationsLayer(wwd);
		wwd.getModel().getLayers().add(layer);
	}

	private void createPanel()
	{
		setLayout(new BorderLayout());

		GridBagConstraints c;
		JPanel panel = new JPanel(new GridBagLayout());
		add(panel, BorderLayout.NORTH);

		FlatJButton add = new FlatJButton(Icons.add);
		add.setToolTipText("Add annotation");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1d / 3d;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(add, c);
		add.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				addNew();
			}
		});

		final FlatJButton edit = new FlatJButton(Icons.edit);
		edit.setToolTipText("Edit selected");
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1d / 3d;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(edit, c);
		edit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				editSelected();
			}
		});

		final FlatJButton delete = new FlatJButton(Icons.delete);
		delete.setToolTipText("Delete selected");
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 1d / 3d;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(delete, c);
		delete.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				deleteSelected();
			}
		});

		model = new DefaultListModel();
		list = new JList(model);
		list.setCellRenderer(new CheckboxListCellRenderer());
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
		add(scrollPane, BorderLayout.CENTER);

		ListSelectionListener lsl = new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				edit.setEnabled(list.getSelectedIndex() >= 0);
				delete.setEnabled(list.getSelectedIndex() >= 0);
			}
		};
		list.getSelectionModel().addListSelectionListener(lsl);
		lsl.valueChanged(null);

		list.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				int index = list.locationToIndex(e.getPoint());
				if (index >= 0)
				{
					Rectangle rect = list.getCellBounds(index, index);
					if (rect.contains(e.getPoint()))
					{
						Rectangle checkRect = new Rectangle(rect.x, rect.y,
								rect.height, rect.height);
						ListItem listItem = (ListItem) model.get(index);
						if (checkRect.contains(e.getPoint()))
						{
							toggleCheck(listItem);
						}
						else if (e.getClickCount() == 2)
						{
							flyTo(listItem);
						}
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				dragging = getListItemUnderMouse(e.getPoint());
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				dragging = null;
			}
		});

		list.addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (dragging != null)
				{
					int index = list.locationToIndex(e.getPoint());
					moveTo(dragging, index);
				}
			}
		});

		list.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_DELETE)
				{
					deleteSelected();
				}
				else if (e.getKeyCode() == KeyEvent.VK_SPACE)
				{
					toggleCheck(null);
				}
				else if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					flyTo(null);
				}
			}
		});
	}

	private ListItem getListItemUnderMouse(Point point)
	{
		int index = list.locationToIndex(point);
		if (index >= 0)
		{
			Rectangle rect = list.getCellBounds(index, index);
			if (rect.contains(point))
			{
				return (ListItem) model.get(index);
			}
		}
		return null;
	}

	private void toggleCheck(ListItem item)
	{
		if (item == null)
		{
			item = (ListItem) list.getSelectedValue();
		}
		if (item != null)
		{
			item.check.setSelected(!item.check.isSelected());
			item.annotation.setVisible(item.check.isSelected());
			list.repaint();
			refreshLayer();
		}
	}

	private void flyTo(ListItem item)
	{
		if (item == null)
		{
			item = (ListItem) list.getSelectedValue();
		}
		if (item != null)
		{
			flyToAnnotation(item.annotation);
		}
	}

	private void populateList()
	{
		model.removeAllElements();
		for (Annotation annotation : Settings.get().getAnnotations())
		{
			addAnnotation(annotation);
		}
		list.repaint();
	}

	private void addAnnotation(Annotation annotation)
	{
		JCheckBox check = new JCheckBox("", annotation.isVisible());
		ListItem item = new ListItem(annotation, check);
		model.addElement(item);
	}

	private void addNew()
	{
		View view = wwd.getView();
		if (view instanceof OrbitView)
		{
			OrbitView orbitView = (OrbitView) view;
			Position pos = orbitView.getCenterPosition();
			double minZoom = orbitView.getZoom() * 5;
			Annotation annotation = new Annotation("",
					pos.getLatitude().degrees, pos.getLongitude().degrees,
					minZoom);
			AnnotationEditor editor = new AnnotationEditor(wwd, frame,
					"New annotation", annotation);
			int value = editor.getOkCancel();
			if (value == JOptionPane.OK_OPTION)
			{
				Settings.get().getAnnotations().add(annotation);
				addAnnotation(annotation);
				list.repaint();
				refreshLayer();
			}
		}
	}

	private void editSelected()
	{
		ListItem item = (ListItem) list.getSelectedValue();
		if (item != null)
		{
			Annotation editing = new Annotation(item.annotation);
			AnnotationEditor editor = new AnnotationEditor(wwd, frame,
					"Edit annotation", editing);
			int value = editor.getOkCancel();
			if (value == JOptionPane.OK_OPTION)
			{
				item.annotation.setValuesFrom(editing);
				list.repaint();
				refreshLayer();
			}
		}
	}

	private void deleteSelected()
	{
		ListItem item = (ListItem) list.getSelectedValue();
		if (item != null)
		{
			int value = JOptionPane.showConfirmDialog(this,
					"Are you sure you want to delete the annotation '"
							+ item.annotation.getLabel() + "'?",
					"Delete annotation", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			if (value == JOptionPane.YES_OPTION)
			{
				model.removeElement(item);
				Settings.get().getAnnotations().remove(item.annotation);
				list.repaint();
				refreshLayer();
			}
		}
	}

	private void moveTo(ListItem item, int index)
	{
		int srcIndex = model.indexOf(item);
		if (srcIndex != index)
		{
			model.remove(srcIndex);
			model.add(index, item);
			Settings.get().getAnnotations().remove(item.annotation);
			Settings.get().getAnnotations().add(index, item.annotation);
			list.setSelectedIndex(index);
			list.repaint();
		}
	}

	private void refreshLayer()
	{
		layer.refresh();
		wwd.redraw();
	}

	private void flyToAnnotation(Annotation annotation)
	{
		View view = wwd.getView();
		if (view instanceof OrbitView)
		{
			OrbitView orbitView = (OrbitView) view;
			Position center = orbitView.getCenterPosition();
			Position newCenter = Position.fromDegrees(annotation.getLatitude(),
					annotation.getLongitude(), 0);
			long lengthMillis = Util.getScaledLengthMillis(center.getLatLon(),
					newCenter.getLatLon());

			double zoom = orbitView.getZoom();
			double minZoom = annotation.getMinZoom();
			double maxZoom = annotation.getMaxZoom();
			if (minZoom >= 0 && zoom > minZoom)
				zoom = Math.max(minZoom, 1000);
			else if (maxZoom >= 0 && zoom < maxZoom)
				zoom = maxZoom;

			ViewStateIterator vsi = FlyToOrbitViewStateIterator
					.createPanToIterator(wwd.getModel().getGlobe(), center,
							newCenter, orbitView.getHeading(), orbitView
									.getHeading(), orbitView.getPitch(),
							orbitView.getPitch(), orbitView.getZoom(), zoom,
							lengthMillis, true);

			view.applyStateIterator(vsi);
		}
	}

	private class CheckboxListCellRenderer extends JPanel implements
			ListCellRenderer
	{
		private JPanel panel;
		private JLabel label;

		public CheckboxListCellRenderer()
		{
			setLayout(new BorderLayout());
			panel = new JPanel(new BorderLayout());
			add(panel, BorderLayout.WEST);
			label = new JLabel();
			label.setOpaque(true);
			add(label, BorderLayout.CENTER);
		}

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus)
		{
			Color background = isSelected ? list.getSelectionBackground()
					: list.getBackground();

			if (value instanceof ListItem)
			{
				final Annotation annotation = ((ListItem) value).annotation;
				final JCheckBox check = ((ListItem) value).check;
				if (panel.getComponentCount() != 1
						|| panel.getComponent(0) != check)
				{
					panel.removeAll();
					panel.add(check, BorderLayout.CENTER);
				}
				label.setText(annotation.getLabel());
				check.setBackground(background);
			}
			else
			{
				label.setText(value.toString());
				panel.removeAll();
			}

			label.setBackground(background);
			panel.setBackground(background);

			return this;
		}
	}
}