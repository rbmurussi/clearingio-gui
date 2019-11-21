package org.clearingio.gui.swing.tree.table;

import org.beanio.annotation.Record;
import org.clearingio.file.StreamFactoryClearingIO;
import org.clearingio.ipm.MsgIpm;
import org.clearingio.ipm.annotation.PDS;
import org.clearingio.ipm.file.RdwDataInputStream;
import org.clearingio.iso8583.annotation.Bit;
import org.clearingio.iso8583.annotation.enumeration.Encode;
import org.clearingio.iso8583.builder.DataElement;
import org.clearingio.iso8583.builder.MsgBuilder;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TreeTableMain extends JFrame {

	private StreamFactoryClearingIO streamFactoryClearingIO = new StreamFactoryClearingIO();

	public TreeTableMain() {
		super("ClearingIO");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setLayout(new GridLayout(0, 1));

		JMenuBar jMenuBar = new JMenuBar();
		setJMenuBar(jMenuBar);

		JMenu fileMenu = new JMenu("File");
		jMenuBar.add(fileMenu);

		JMenuItem jMenuItemOpenISO8583 = new JMenuItem("Open ISO-8583");
		jMenuItemOpenISO8583.addActionListener((e) -> openISO8583());
		fileMenu.add(jMenuItemOpenISO8583);

		JMenuItem jMenuItemOpenIncomingELO = new JMenuItem("Open Incoming ELO");
		jMenuItemOpenIncomingELO.addActionListener((e) -> selectFileGetList(this, "IncomingELO"));
		fileMenu.add(jMenuItemOpenIncomingELO);

		JMenuItem jMenuItemOpenIncomingVisa = new JMenuItem("Open Incoming VISA");
		jMenuItemOpenIncomingVisa.addActionListener((e) -> selectFileGetList(this, "IncomingVisa"));
		fileMenu.add(jMenuItemOpenIncomingVisa);

		JMenuItem jMenuItemOpenOutgoingVisa = new JMenuItem("Open Outgoing VISA");
		jMenuItemOpenOutgoingVisa.addActionListener((e) -> selectFileGetList(this, "OutgoingVisa"));
		fileMenu.add(jMenuItemOpenOutgoingVisa);

		JMenuItem jMenuItemOpenIncomingCabal = new JMenuItem("Open Incoming CABAL");
		jMenuItemOpenIncomingCabal.addActionListener((e) -> selectFileGetList(this, "IncomingCABAL"));
		fileMenu.add(jMenuItemOpenIncomingCabal);

		JMenuItem jMenuItemOpenOutgoingCabal = new JMenuItem("Open Outgoing CABAL");
		jMenuItemOpenOutgoingCabal.addActionListener((e) -> selectFileGetList(this, "OutgoingCABAL"));
		fileMenu.add(jMenuItemOpenOutgoingCabal);

		setSize(800, 600);
	}

	private void selectFileGetList(TreeTableMain treeTableMain, String outgoingVisa) {
		try {
			JFileChooser jFileChooser = new JFileChooser();
			jFileChooser.showOpenDialog(treeTableMain);
			File file = jFileChooser.getSelectedFile();
			System.out.println(file.getAbsolutePath());
			List<Object> list = streamFactoryClearingIO.createReader(outgoingVisa, file);
			MyDataNode myDataNode = load(list.iterator(), file.getAbsolutePath());
			MyAbstractTreeTableModel treeTableModel = new MyDataModel(myDataNode);
			MyTreeTable myTreeTable = new MyTreeTable(treeTableModel);
			treeTableMain.add(new JScrollPane(myTreeTable));
			treeTableMain.pack();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, this.parseException(ex), ex.getMessage(), JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	private void openISO8583() {
		JFileChooser jFileChooser = new JFileChooser();
		jFileChooser.showOpenDialog(this);
		File file = jFileChooser.getSelectedFile();
		System.out.println(file.getAbsolutePath());

		List<Object> list = new ArrayList<>();

		try(RdwDataInputStream in = new RdwDataInputStream(new FileInputStream(file))) {
			MsgBuilder<MsgIpm> msgBuilder = new MsgBuilder<>(MsgIpm.class, Encode.EBCDIC);
			while(in.hasNext()) {
				list.add(msgBuilder.unpack(in.next()));
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, parseException(e), e.getMessage(), JOptionPane.ERROR_MESSAGE);
		}

		MyDataNode myDataNode = load(list.iterator(), file.getAbsolutePath());
		MyAbstractTreeTableModel treeTableModel = new MyDataModel(myDataNode);
		MyTreeTable myTreeTable = new MyTreeTable(treeTableModel);
		add(new JScrollPane(myTreeTable));
		pack();
	}

	private static String parseException(Exception e) {
		StackTraceElement st[] = e.getStackTrace();
		String err = "";
		for(int i = 0; i < st.length; i++){
			err += st[i].toString() + '\n';
		}
		return err;
	}

	private static Object getObject(String name, Object obj) {
		try {
			name = name.substring(0,1).toUpperCase().concat(name.substring(1));
			Method method = obj.getClass().getMethod("get" + name);
			Object ret = method.invoke(obj);
			return ret;
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	private static String parse(Object ret) {
		return ret != null ? String.valueOf(ret): null;
	}

	private static List<MyDataNode> parseMyDataNode(Object obj) {
		List<MyDataNode> listMyDataNode = new ArrayList<MyDataNode>();
		List<Field> listField = defFields(obj.getClass());
		for (Field field: listField) {
			Object ret = getObject(field.getName(), obj);
			if(ret == null) continue;
			List<MyDataNode> children = null;
			String value = null;
			if (ret instanceof DataElement) {
				children = parseMyDataNode(ret);
				listMyDataNode.add(new MyDataNode(field.getName(), "", field.getAnnotation(Bit.class).name(), null));
			} else {
				value = parse(ret);
			}
			String description = "";
			if(field.isAnnotationPresent(org.beanio.annotation.Field.class))
				description = field.getAnnotation(org.beanio.annotation.Field.class).name();
			if(field.isAnnotationPresent(Bit.class))
				description = field.getAnnotation(Bit.class).name();
			if(field.isAnnotationPresent(PDS.class))
				description = field.getAnnotation(PDS.class).name();
			listMyDataNode.add(new MyDataNode(field.getName(), value, description, children));
		}
		return listMyDataNode;
	}

	private static List<Field> defFields(Class<?> tClass) {
		List<Field> list = new ArrayList<Field>();
		if(!tClass.equals(Object.class)) {
			list = defFields(tClass.getSuperclass());
		}
		Field[] declaredFields = tClass.getDeclaredFields();
		for(Field field : declaredFields) {
			list.add(field);
		}
		return list;
	}

	private static MyDataNode load(Iterator<Object> itObject, String fileName) {
		List<MyDataNode> rootNodes = new ArrayList<MyDataNode>();
		int i = 1;
		while(itObject.hasNext()) {
			Object object = itObject.next();
			List<MyDataNode> children = parseMyDataNode(object);
			String description = "";
			if(object.getClass().isAnnotationPresent(Record.class))
				description = object.getClass().getAnnotation(Record.class).name();
			rootNodes.add(new MyDataNode(object.getClass().getSimpleName(), object.toString(), description, children));
			i++;
		}
		MyDataNode root = new MyDataNode(fileName, "", "", rootNodes);
		return root;
	}

	private static MyDataNode createDataStructure() {
		List<MyDataNode> children1 = new ArrayList<MyDataNode>();
		children1.add(new MyDataNode("field1", "value1", "comment1", null));
		children1.add(new MyDataNode("field2", "value2", "comment2", null));
		children1.add(new MyDataNode("field3", "value3", "comment3", null));
		children1.add(new MyDataNode("field4", "value4", "comment4", null));

		List<MyDataNode> rootNodes = new ArrayList<MyDataNode>();
		rootNodes.add(new MyDataNode("line1", "values", "", children1));
		rootNodes.add(new MyDataNode("line2", "values", "", children1));
		rootNodes.add(new MyDataNode("line3", "values", "", children1));
		rootNodes.add(new MyDataNode("line4", "values", "", children1));
		rootNodes.add(new MyDataNode("line5", "values", "", children1));
		rootNodes.add(new MyDataNode("line6", "values", "", children1));
		rootNodes.add(new MyDataNode("line7", "values", "", children1));

		MyDataNode root = new MyDataNode(" root", "", "", rootNodes);
		return root;
	}

	public static void main(final String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		new TreeTableMain().setVisible(true);
	}
}
