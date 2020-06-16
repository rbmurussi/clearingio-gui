package org.clearingio.gui.swing.tree.table;

import org.beanio.BeanReaderErrorHandler;
import org.beanio.BeanReaderException;
import org.beanio.annotation.Record;
import org.clearingio.file.StreamFactoryClearingIO;
import org.clearingio.ipm.MsgIpm;
import org.clearingio.ipm.annotation.PDS;
import org.clearingio.ipm.annotation.Subfield;
import org.clearingio.ipm.file.RdwDataInputStream;
import org.clearingio.ipm.file.RdwFileIO;
import org.clearingio.iso8583.annotation.Bit;
import org.clearingio.iso8583.annotation.enumeration.Encode;
import org.clearingio.iso8583.builder.Packing;
import org.clearingio.iso8583.builder.MsgBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.*;
import java.util.List;

public class TreeTableMain extends JFrame {

	private StreamFactoryClearingIO streamFactoryClearingIO = new StreamFactoryClearingIO();
	private final Logger LOGGER = LoggerFactory.getLogger(TreeTableMain.class);

	static {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");
	}

	public TreeTableMain() {
		super("ClearingIO");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setLayout(new GridLayout(0, 1));

		JMenuBar jMenuBar = new JMenuBar();
		setJMenuBar(jMenuBar);

		JMenu jMenuFile = new JMenu("File");
		jMenuBar.add(jMenuFile);

		JMenuItem jMenuItemOpenISO8583 = new JMenuItem("Open ISO-8583");
		jMenuItemOpenISO8583.addActionListener((e) -> openISO8583());
		jMenuFile.add(jMenuItemOpenISO8583);

		JMenuItem jMenuItemOpenIncomingELO = new JMenuItem("Open Incoming ELO");
		jMenuItemOpenIncomingELO.addActionListener((e) -> selectFileGetList(this, "IncomingELO"));
		jMenuFile.add(jMenuItemOpenIncomingELO);

		JMenuItem jMenuItemOpenIncomingVisa = new JMenuItem("Open Incoming VISA");
		jMenuItemOpenIncomingVisa.addActionListener((e) -> selectFileGetList(this, "IncomingVisa"));
		jMenuFile.add(jMenuItemOpenIncomingVisa);

		JMenuItem jMenuItemOpenOutgoingVisa = new JMenuItem("Open Outgoing VISA");
		jMenuItemOpenOutgoingVisa.addActionListener((e) -> selectFileGetList(this, "OutgoingVisa"));
		jMenuFile.add(jMenuItemOpenOutgoingVisa);

		JMenuItem jMenuItemOpenIncomingCabal = new JMenuItem("Open Incoming CABAL");
		jMenuItemOpenIncomingCabal.addActionListener((e) -> selectFileGetList(this, "IncomingCABAL"));
		jMenuFile.add(jMenuItemOpenIncomingCabal);

		JMenuItem jMenuItemOpenOutgoingCabal = new JMenuItem("Open Outgoing CABAL");
		jMenuItemOpenOutgoingCabal.addActionListener((e) -> selectFileGetList(this, "OutgoingCABAL"));
		jMenuFile.add(jMenuItemOpenOutgoingCabal);

		JMenuItem jMenuItemOpenMPEFullFileReplacement = new JMenuItem("Open MPE Full File Replacement");
		jMenuItemOpenMPEFullFileReplacement.addActionListener((e) -> selectFileGetList(this, "MPEFullFileReplacement"));
		jMenuFile.add(jMenuItemOpenMPEFullFileReplacement);

		JMenuItem jMenuItemOpenMPEDailyUpdate = new JMenuItem("Open MPE Daily Update");
		jMenuItemOpenMPEDailyUpdate.addActionListener((e) -> selectFileGetList(this, "MPEDailyUpdate"));
		jMenuFile.add(jMenuItemOpenMPEDailyUpdate);

		JMenu jMenuConversion = new JMenu("Conversion");
		jMenuBar.add(jMenuConversion);
		
		JMenuItem jMenuItemBlockedToRdw = new JMenuItem("Blocked to RDW");
		jMenuItemBlockedToRdw.addActionListener((e) -> {
			try {
				RdwFileIO.fromFileBlocked(getSelectedFile());
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, this.parseException(ex), ex.getMessage(), JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		});
		jMenuConversion.add(jMenuItemBlockedToRdw);
		
		JMenuItem jMenuItemRdwToBlocked = new JMenuItem("RDW to Blocked");
		jMenuItemRdwToBlocked.addActionListener((e) -> {
			try {
				RdwFileIO.toFileBlocked(getSelectedFile());
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, this.parseException(ex), ex.getMessage(), JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		});
		jMenuConversion.add(jMenuItemRdwToBlocked);
		
		JMenuItem jMenuItemRdwMpeCompressToMpeNoCompress = new JMenuItem("RDW MPE Compress to MPE NO Compress");
		jMenuItemRdwMpeCompressToMpeNoCompress.addActionListener((e) -> {
			try {
				RdwFileIO.toMPENonCompress(getSelectedFile(), Encode.EBCDIC, Encode.ASCII);
			} catch (IOException | ParseException ex) {
				JOptionPane.showMessageDialog(this, this.parseException(ex), ex.getMessage(), JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		});
		jMenuConversion.add(jMenuItemRdwMpeCompressToMpeNoCompress);
		
		setSize(800, 600);
	}

	private File getSelectedFile() {
		JFileChooser jFileChooser = new JFileChooser();
		jFileChooser.showOpenDialog(this);
		File file = jFileChooser.getSelectedFile();
		return file;
	}
	
	private void selectFileGetList(TreeTableMain treeTableMain, String outgoingVisa) {
		try {
			File file = getSelectedFile();
			System.out.println(file.getAbsolutePath());
			BeanReaderErrorHandler ex = new BeanReaderErrorHandler() {
				@Override
				public void handleError(BeanReaderException e) throws Exception {
					System.err.println(e.getRecordContext().getRecordText());
					//JOptionPane.showMessageDialog(null, parseException(e), e.getMessage(), JOptionPane.ERROR_MESSAGE);
				}
			};
			List<Object> list = streamFactoryClearingIO.createReader(outgoingVisa, file, ex);
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
		File file = getSelectedFile();
		System.out.println(file.getAbsolutePath());

		List<Object> list = new ArrayList<>();

		try(RdwDataInputStream in = new RdwDataInputStream(new FileInputStream(file))) {
			MsgBuilder<MsgIpm> msgBuilder = new MsgBuilder<>(MsgIpm.class, Encode.EBCDIC);
			int i = 1;
			while(in.hasNext()) {
				byte[] b = in.next();
				try {
					list.add(msgBuilder.unpack(b));
				} catch (RuntimeException ex) {
					LOGGER.error(i + "=>" + new String(b, "cp037"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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
			if(obj.getClass().equals(String.class)) {
				return obj;
			}
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

	private static List<MyDataNode> parseMyDataNode(Object obj, int i) {
		List<MyDataNode> listMyDataNode = new ArrayList<MyDataNode>();
		List<Field> listField = defFields(obj.getClass());
		for (Field field: listField) {
			Object ret = getObject(field.getName(), obj);
			if(ret == null) continue;
			List<MyDataNode> children = null;
			if (ret instanceof Packing) {
				children = parseMyDataNode(ret, 0);
			}
			if(ret instanceof Collection) {
				children = new ArrayList<>();
				Iterator iterator = Collection.class.cast(ret).iterator();
				int index = 0;
				while (iterator.hasNext()) {
					index++;
					children.addAll(parseMyDataNode(iterator.next(), index));
				}
			}
			String value = parse(ret);

			String description = "";
			if(field.isAnnotationPresent(org.beanio.annotation.Field.class))
				description = field.getAnnotation(org.beanio.annotation.Field.class).name();
			if(field.isAnnotationPresent(Bit.class))
				description = field.getAnnotation(Bit.class).name();
			if(field.isAnnotationPresent(PDS.class))
				description = field.getAnnotation(PDS.class).name();
			if(field.isAnnotationPresent(Subfield.class))
				description = field.getAnnotation(Subfield.class).name();
			String name = field.getName() + (i == 0 ? "" : "[" + i + "]");
			listMyDataNode.add(new MyDataNode(name, value, description, children));
		}
		return listMyDataNode;
	}

	private static List<Field> defFields(Class<?> tClass) {
		List<Field> list = new ArrayList<Field>();
		if(tClass.equals(String.class)) {
			try {
				list.add(tClass.getDeclaredField("value"));
			} catch (NoSuchFieldException e) {
			}
			return list;
		}
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
			List<MyDataNode> children = parseMyDataNode(object, 0);
			String description = "";
			if(object.getClass().isAnnotationPresent(Record.class))
				description = object.getClass().getAnnotation(Record.class).name();
			rootNodes.add(new MyDataNode(object.getClass().getSimpleName(), object.toString(), description, children));
			i++;
		}
		MyDataNode root = new MyDataNode(fileName, "", "", rootNodes);
		return root;
	}

//	private static MyDataNode createDataStructure() {
//		List<MyDataNode> children1 = new ArrayList<MyDataNode>();
//		children1.add(new MyDataNode("field1", "value1", "comment1", null));
//		children1.add(new MyDataNode("field2", "value2", "comment2", null));
//		children1.add(new MyDataNode("field3", "value3", "comment3", null));
//		children1.add(new MyDataNode("field4", "value4", "comment4", null));
//
//		List<MyDataNode> rootNodes = new ArrayList<MyDataNode>();
//		rootNodes.add(new MyDataNode("line1", "values", "", children1));
//		rootNodes.add(new MyDataNode("line2", "values", "", children1));
//		rootNodes.add(new MyDataNode("line3", "values", "", children1));
//		rootNodes.add(new MyDataNode("line4", "values", "", children1));
//		rootNodes.add(new MyDataNode("line5", "values", "", children1));
//		rootNodes.add(new MyDataNode("line6", "values", "", children1));
//		rootNodes.add(new MyDataNode("line7", "values", "", children1));
//
//		MyDataNode root = new MyDataNode(" root", "", "", rootNodes);
//		return root;
//	}

	public static void main(final String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		new TreeTableMain().setVisible(true);
	}
}
