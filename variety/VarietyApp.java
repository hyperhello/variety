import java.util.*;
import java.math.*;
import java.text.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import com.apple.eawt.*;

/* 
threading model is breaking down. a CurrentBasis object that threads on all cores and can be inspected for status would be good.
save that for later since there will probably be changes to the algorithm. right now focus on calculate() which needs better
gating. 

OKAY we need stuff. We need to make it stop drawing graphs over the basis. I want to parenthesize the output code so I can see what's going on.
And we really need a decent way of explaining what we want to eliminate from the system, and chain such things.



*/

// partly cokeandcode.com Space Invaders
public class VarietyApp
{
	public static void main(String arg[])
	{
		try
		{
			Application apple = Application.getApplication();
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
		catch (Error e) {}

		SwingUtilities.invokeLater(new Runnable() { 
			public void run() 
			{
				VarietyApp app = new VarietyApp();
				/*app.new Dot("A", 100, -100);
				app.new Dot("B", -50, 80);
				app.new Dot("C", 80, -110);
				app.new Dot("D", -200, -100);
				app.new Dot("E", -200, -50);*/
				
				app.calculation = true;
				app.calculate();
				
			} 
		});
		
		System.out.println("This is a test output!");
		throw new Error("This is a test error!");
	}
	
	JFrame frame;
	JTabbedPane tabbedPane;
	
	VarietyApp()
	{
		frame = new JFrame("Variety");
		frame.setLayout(new BorderLayout());
		
		final JMenu editMenu = new JMenu("Edit");
		class EditItem extends JMenuItem
		{
			EditItem(String title, char shortcut)
			{
				super(title);
				editMenu.add(this);
				if (shortcut != 0)
					setAccelerator(KeyStroke.getKeyStroke(Character.toUpperCase(shortcut), 
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | (Character.isUpperCase(shortcut) ? Event.SHIFT_MASK : 0)));
				addActionListener(new ActionListener() { public void actionPerformed(ActionEvent a) { go(); } });
			}
			void go()
			{
			}
		}
		new EditItem("Undo", 'z');
		new EditItem("Redo", 'Z');
		editMenu.addSeparator();
		new EditItem("Cut", 'x') {
			void go()
			{
			}
		};
		new EditItem("Copy", 'c');
		new EditItem("Paste", 'v');
		new EditItem("Delete", (char)0);
		new EditItem("Select All", 'a');
		JMenuBar mb = new JMenuBar();
		mb.add(editMenu);
		frame.setJMenuBar(mb);
		
		frame.add("North", input);

		tabbedPane = new JTabbedPane();
		//tabbedPane.setBackground(Color.white);
		tabbedPane.addTab( "Vitality", vitality);
		tabbedPane.addTab( "CFG", cfg );
		tabbedPane.addTab( "BNF", bnf );
		tabbedPane.addTab( "Algebra", algebra );
		tabbedPane.addTab( "Basis", groebner );
		tabbedPane.addTab( "Geometry", geometry );//if (tabbedPane.getSelectedComponent() == GraphPane.this)
		frame.add("Center", tabbedPane);
		//canvas.addNotify();

		frame.pack();
		//frame.setResizable(false);
		frame.setSize(800,600);
		frame.setVisible(true);
		
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e)
				{ System.exit(0); }
		});
		
		input.ideal.selectAll();
		input.ideal.grabFocus();
	}
	
	class Pane extends JPanel
	{
		Pane()
		{
			setLayout(new BorderLayout());
		}
	}
	class TextPane extends Pane
	{
		JTextArea area = new JTextArea();
		JScrollPane scroll = new JScrollPane(area, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		
		TextPane()
		{
			area.setFont(new Font("Monospaced", Font.PLAIN, 14));
			area.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
			area.setLineWrap(true);
			add("Center", scroll);
		}
		void setText(final String text)
		{
			SwingUtilities.invokeLater(new Runnable() { 
				public void run() 
				{
					int oldUpdatePolicy = ((DefaultCaret)area.getCaret()).getUpdatePolicy();
					((DefaultCaret)area.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
					area.setText(text);
					((DefaultCaret)area.getCaret()).setUpdatePolicy(oldUpdatePolicy);
				} 
			});
		}
	}
	class TreePane extends Pane
	{
		class Node extends DefaultMutableTreeNode
		{
			String text;
			public String toString()
			{
				return text;
			}
			
			Node(Node parentNode, String text)
			{
				this.text = text;
				setUserObject(this);
				
				if (parentNode != null)
					parentNode.add(this);
			}
			
			/* useful functions from DefaultMutableTreeNode: 
			removeAllChildren()
			*/
		}
		Node root()
		{
			return root;
		}
		
		Node root = new Node(null, "root");
		DefaultTreeModel treeModel = new DefaultTreeModel( root );
		JTree tree = new JTree(treeModel);
		JScrollPane scroll = new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		
		TreePane()
		{
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION );
			tree.addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent e)
				{
					DefaultMutableTreeNode dmt = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
					if (dmt == null || (dmt.getUserObject() instanceof Node) == false)
						return;
					
					Node node = (Node)dmt.getUserObject();
					
				}
			});
			
			add("Center", scroll);
			
			Node one = new Node(root, "One");
			new Node(one, "One point one");
			new Node(one, "One point two");
			new Node(root, "Two");
		
			tree.expandRow(0);
			tree.setRootVisible(false);
			tree.setShowsRootHandles(true);
		}
	}
	class VitalityPane extends TreePane
	{
		/*
		
		What do we want from Vitality now? We want the ability to make a parsing object from a string of rules, and to jack into it with Java classes.
		For example, our old serializer in Console. Can we unify parsing and math? Ah, not now. Let's just get the shit running.
		
		
		
		
		Commas in tuples indicate a hard break. 
		
		Triple: a, b, c
		a: digit, digit
		
		Instead of [optional] how about [] is repetition? number[3] is three numbers, number[] is any number of numbers.
		
		Or is | and that is also union? x < 10 | x > 80
		
		Let's revisit functions and subideals and all that.
		
		f(x) : (y) { y^2 = x }	// sqrt function
		
		power(x, 0) : 1
		power(x, n) : x*power(x,n-1)	// etc
		
		power(x, n) : 1 { n=0 } : x * power(x, n-1)
			
		digit : '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
		num : ['-'] {digit} [ '.' {digit} ]
		
		twoterminal : (v,i,anode,cathode) { v = anode.v - cathode.v; i = anode.i = cathode.i }
		resistor(twoterminal) : (r) { v = i*r }
		
		R1 : resistor(twoterminal) = 10
		
		twoTerminal(anode, cathode)
		*/
		
		JTextPane rules;
		Vitality vitality;
		
		VitalityPane()
		{
			rules = new JTextPane();
			rules.setText("s: dan toby | thomas 'e'");
			rules.setFont(new Font("Monospaced", Font.PLAIN, 13));
			rules.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			add("North", rules);

			DocumentListener listener = new DocumentListener() {
				public void changedUpdate(DocumentEvent e)
					{ changeRules(); }
				public void insertUpdate(DocumentEvent e)
					{ changeRules(); }
				public void removeUpdate(DocumentEvent e)
					{ changeRules(); }
			};
			
			changeRules();
			
			rules.getStyledDocument().addDocumentListener(listener);
		}
		void changeRules()
		{
			System.out.println("new rules: " + rules.getText());

			vitality = new Vitality() 
			{
				public void prepare()
				{
					Terminal ter = new Terminal("zero");
					Choice cho = new Choice(new Rule[] { new Terminal("zero"), new Terminal("0") });
					Concatenation con = new Concatenation(new Rule[] { 
						new Choice(new Rule[] { new Terminal("zero"), new Terminal("0") }),
						new Choice(new Rule[] { new Terminal("one"), new Terminal("1") }),
						new Choice(new Rule[] { new Terminal("two"), new Terminal("2") }),
						});
					Repetition rep = new Repetition(0, 10000, new Choice(new Rule[] {
						new Terminal("0"), new Terminal("1"), new Terminal("2"), new Terminal("3"), new Terminal("4"), 
						new Terminal("zero"), new Terminal("one"), new Terminal("two"), new Terminal("three"), new Terminal("four")
						}) );
					
					init(rep, "");//input.getText());
					
					// so obviously we want the ability to include the rules the user typed in. The ability to include polynomials or whatever in ideal format would be great,
					// and to attach emitters afterward on a per basis is needed.
					
					// Line : (a*x + b) { ... }
					// sqrt(x^2) : x { x > 0 }
					// log(e^y) : y
					
					// Quarternion : ( w,i,j,k )
				}
			};
			
			outputToRoot();
		}
		
		void outputToRoot()
		{
			root().removeAllChildren();

			vitality.new Walker() 
			{
				TreePane.Node node = VitalityPane.this.root();
				ArrayList<TreePane.Node> parents = new ArrayList<TreePane.Node>();
				
				void enterResult(Vitality.Rule.Result result, int mark)
				{
					String spaces = "                       ".substring(0,0);
					String output = spaces + result.toString() 
						+ (result.successful() ? (" • " + result.charactersConsumed()) : "") 
						+ (" !! " + result.charactersTested()) 
						+ " '" + vitality.text.substring(mark, mark + result.charactersConsumed()) + "'";
					
					parents.add(node);
					node = new Node(node, output);
				}
				void leaveResult(Vitality.Rule.Result result, int mark)
				{
					node = parents.get(parents.size()-1);
					parents.remove(parents.size()-1);
				}
			}.run();

			treeModel.nodeStructureChanged(bnf.root());
		}
		
		void calculate()
		{
			String from = vitality.text, to = input.getText();
			int change = 0, changeEnd = to.length(), changeDelta = to.length() - from.length();
		
			// doing them in this order means that turning "dan" into "dandan" marks the second dan as the change
			
			while (change < from.length() && change < to.length() && from.charAt(change) == to.charAt(change))
				change++;
			
			while (changeEnd - changeDelta > change && changeEnd > change && from.charAt(changeEnd - changeDelta - 1) == to.charAt(changeEnd - 1))
				changeEnd--;
		
			if (change != changeEnd || changeDelta != 0)
			{
				vitality.set(change, (changeEnd - change) - changeDelta, to.substring(change, changeEnd));
				
				outputToRoot();
				
				// the goal HERE is to synergize vitality.set() and the walker! We want to refresh just the parts of the tree that have been reparsed.
			}
		}
		
	}
	abstract class GraphPane extends Pane
	{
		Canvas canvas;
		BufferStrategy strategy;
		
		GraphPane()
		{
			canvas = new Canvas() {
				public void paint(Graphics g)
				{
					render((Graphics2D)g);
				}
			};
						//canvas.setMinimumSize(new Dimension(800,600));
			canvas.setFocusable(false);
			canvas.setBackground(Color.white);
			//canvas.createBufferStrategy(2);
			//strategy = canvas.getBufferStrategy();
			add("Center", canvas);

			/*canvas.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e)
				{
				}
			});*/
			canvas.addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseMoved(MouseEvent e)
					{ hover(e.getX(), e.getY()); }
				public void mouseDragged(MouseEvent e)
					{ drag(e.getX(), e.getY()); }
			});
			canvas.addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent e)
					{ hover(e.getX(), e.getY()); }
			});
		}
		
		abstract void setFormulas(Variety.Formula[] formulas, java.util.List<Variety.Inequality> ineq);
		abstract void render(Graphics2D g);
		abstract void hover(int x, int y);
		abstract void drag(int x, int y);
	}
	
	class InputPane extends Pane
	{
		class State
		{
			String ideal = "y = x^3; 1 < y < 2";
			String order = "x, y";
			boolean strict = true;
		}
		State state = new State();
		
		//String[] start = { "(Ax - x)^2 + (Ay - y)^2 = r^2\n(Bx - x)^2 + (By - y)^2 = r^2;\n(Cx - x)^2 + (Cy - y)^2 = r^2", "Ax Ay Bx By Cx Cy Dx Dy Ex Ey x y r" };
		//String[] start = { "y = x^2; 1 < y < 2", "x, y" };
		//String[] start = { "s: dan toby | thomas 'e'", "x, y" };
		
		JTextPane ideal;
		JLabel errorReport;
		JTextField order;
		JCheckBox strict;
		Style style;
		
		InputPane()
		{
			ideal = new JTextPane();
			ideal.setText(state.ideal);
			ideal.setFont(new Font("Monospaced", Font.PLAIN, 13));
			ideal.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			
		    style = ideal.addStyle("I'm a Style", null);
		   /* StyleConstants.setForeground(style, Color.red);
			ideal.getStyledDocument().setCharacterAttributes(0, 8, style, false);
			    StyleConstants.setForeground(style, Color.blue);
			ideal.getStyledDocument().setCharacterAttributes(5, 18, style, false);*/

			order = new JTextField(state.order);

			DocumentListener listener = new DocumentListener() {
				public void changedUpdate(DocumentEvent e)
					{ stateChanged(); }
				public void insertUpdate(DocumentEvent e)
					{ stateChanged(); }
				public void removeUpdate(DocumentEvent e)
					{ stateChanged(); }
			};
						
			ideal.getStyledDocument().addDocumentListener(listener);
			order.getDocument().addDocumentListener(listener);

			ideal.getCaret().addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent ce)
					{ caretChanged(); }
			});
			
			strict = new JCheckBox("Strict ");
			strict.setSelected(state.strict);
			strict.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) 
					{ stateChanged(); }
				});

			errorReport = new JLabel("");
			errorReport.setForeground(Color.red);
			
			JPanel southPanel = new JPanel(new BorderLayout());
			southPanel.add("West", new JLabel(" Ordering: "));
			southPanel.add("Center", order);
			southPanel.add("East", strict);

			add("North", ideal);
			add("Center", errorReport);
			add("South", southPanel);
		}
		
		String getText()
		{
			return state.ideal;
		}
		String[] getIdeal()
		{
			ArrayList<String> list = new ArrayList<String>();
			
			String[] lines = state.ideal.split("\n");
			for (int l = 0; l < lines.length; l++)
			{
				String[] semis = lines[l].split("//")[0].split(";");
				for (int s = 0; s < semis.length; s++)
				{
					//System.out.println("\"" + semis[s] + "\"");
					list.add(semis[s]);
				}
			}
			
			return list.toArray(new String[0]);
		}
		String getOrdering()
		{
			return state.order;
		}
		boolean getStrict()
		{
			return strict.isSelected();
		}
		void setError(String error)
		{
			errorReport.setText((error == null) ? " " : error);
		}
		
		Runnable delayedTasks = null;
		synchronized void setDelayedTasks()
		{
			if (delayedTasks == null)
			{
				delayedTasks = new Runnable() { public void run() { doDelayedTasks(); } };
				SwingUtilities.invokeLater(delayedTasks);
			}
		}
		synchronized void doDelayedTasks()
		{
			delayedTasks = null;
			
			// apply waiting color changes
			for (int i = 0; i < colorChanges.size(); i++)
			{
			    StyleConstants.setForeground(style, colorChanges.get(i).color);
				ideal.getStyledDocument().setCharacterAttributes(colorChanges.get(i).offset, colorChanges.get(i).length, style, true);
			}
			colorChanges.clear();

			// let panels respond to changes
			State newState = new State();
			newState.ideal = ideal.getText();
			newState.order = order.getText();
			newState.strict = strict.isSelected();
			
			State oldState = state;
			state = newState;
			calculate();
		}
		
		class ColorChange
			{ int offset, length; Color color; }
		ArrayList<ColorChange> colorChanges = new ArrayList<ColorChange>();

		synchronized void setColor(final int offset, final int length, final Color color)
		{
			// you apparently can't set colors in response to changed()
			ColorChange change = new ColorChange();
			change.offset = offset;
			change.length = length;
			change.color = color;
			colorChanges.add(change);
			setDelayedTasks();
		}
		synchronized void stateChanged()
		{
			// ideal, order, strict may have changed
			if (state.ideal.equals(ideal.getText()) && state.order.equals(order.getText()) && state.strict == strict.isSelected())
				return;
			
			// clear pending color changes and respond to the change
			colorChanges.clear();
			setDelayedTasks();
		}
		
		boolean selUpdate = false;
		int selOffset, selLength;
		synchronized void caretChanged()
		{
			if (selUpdate == false)
			{
				selUpdate = true;
				SwingUtilities.invokeLater(new Runnable() { public void run() { doSelectionChanges(); } });
			}
		}
		synchronized void doSelectionChanges()
		{
			int dot = ideal.getCaret().getDot(), mark = ideal.getCaret().getMark();
			int offset = (dot < mark) ? dot : mark, length = Math.abs(dot-mark);
			if (offset != selOffset || length != selLength)
			{
				selOffset = offset;
				selLength = length;
				reselect(selOffset, selLength);
			}
			selUpdate = false;
		}
	}
	
	CalculationThread calcThread = null;
	boolean calculation = false;
	
	VarietyBNF syntaxBNF = null;
	String syntaxText = null;
	VarietyBNF.Rule.Result syntaxResult = null;
	
	// this might be broken down by panel
	synchronized void calculate()
	{
		if (calculation == false)
			return;
		
		if (calcThread != null)
			calcThread.nevermind();

		calcThread = new CalculationThread();
		calcThread.start();
		
		// and the geometry is separate and not threaded
		//geometry.canvas.repaint();
		
		//calculateBNF();
		//calculateCFG();
		
		vitality.calculate();
	}
	
	void calculateBNF()
	{
		// color the text
		if (syntaxBNF == null)
		{
			/*StringBuilder builder = new StringBuilder();
			try
			{
				BufferedReader br = new BufferedReader(new FileReader("VarietySyntax.txt"));
				while (br.ready())
					builder.append(br.readLine() + "\n");
				br.close();	
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			syntaxBNF = new VarietyBNF(builder.toString()) {
					
				void init()
				{
					install("S", new Emitter() { void run() { input.setColor(offset, length, Color.black); } });
					//install("AFTERCODE", new Emitter() { void run() { input.setColor(offset, length, Color.black); } });
					install("COMMENT", new Emitter() { void run() { input.setColor(offset, length, Color.gray); } });
					install("NUMBER", new Emitter() { void run() { input.setColor(offset, length, Color.blue); } });
					install("LITERAL", new Emitter() { void run() { input.setColor(offset, length, new Color(0.8f, 0, 0.8f)); } });
					install("STRING", new Emitter() { void run() { input.setColor(offset, length, Color.red); } });
				}
			};*/
			
			/*syntaxBNF = new VarietyBNF("S : LIST '.' LIST \n"
									+ "LIST : DIGIT LIST | DIGIT \n"
									+ "DIGIT : '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ") {
									};*/
	

			// let's try JUST parsing.
			
			syntaxBNF = new VarietyBNF("S: UNIT S | UNIT \n"
								+ "UNIT: STATEMENT | ';' | LOOSETOKEN \n"
								+ "STATEMENT: EXPRESSION AFTEREXPRESSION \n"
								+ "AFTEREXPRESSION: RELATION STATEMENT | RELATION EXPRESSION | '' \n"
								+ "RELATION: '=' | '!=' | '<>' | '<=' | '<' | '>=' | '>' \n"
								+ "EXPRESSION: TERM '+' TERM | TERM '-' TERM | TERM \n"
								+ "TERM: FACTOR '*' TERM | FACTOR '/' TERM | FACTOR \n"
								+ "FACTOR: WS VALUE WS '^' WS VALUE WS | WS VALUE WS \n"
								+ "VALUE: NUMBER | STRING | LITERAL | TUPLE \n"
								+ "TUPLE: '(' EXPRESSION ')' \n"
								+ "LOOSETOKEN: NUMBER | STRING | LITERAL | WHITESPACE | <any> \n"
								+ "NUMBER: DIGITS '.' NUMBER | DIGITS \n"
								+ "DIGITS: DIGIT DIGITS | DIGIT \n"
								+ "STRING: '\"' MORESTRING \n"
								+ "MORESTRING: '\"' | <end> | <any> MORESTRING \n"
								+ "LITERAL: FIRSTLITERAL MORELITERAL \n"
								+ "FIRSTLITERAL: UPPERCASE | LOWERCASE | UNDERSCORE \n"
								+ "SECONDLITERAL: UPPERCASE | LOWERCASE | UNDERSCORE | DIGIT \n"
								+ "MORELITERAL: SECONDLITERAL MORELITERAL | SECONDLITERAL | '' \n"
								+ "UPPERCASE : 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'G' | 'H' | 'I' | 'J' | 'K' | 'L' | 'M' | 'N' | 'O' | 'P' | 'Q' | 'R' | 'S' | 'T' | 'U' | 'V' | 'W' | 'X' | 'Y' | 'Z' \n"
								+ "LOWERCASE : 'a' | 'b' | 'c' | 'd' | 'e' | 'f' | 'g' | 'h' | 'i' | 'j' | 'k' | 'l' | 'm' | 'n' | 'o' | 'p' | 'q' | 'r' | 's' | 't' | 'u' | 'v' | 'w' | 'x' | 'y' | 'z' \n"
								+ "DIGIT: '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' \n"
								+ "WHITESPACE: ' ' | '\t' | COMMENT | <linefeed> \n"
								+ "WS: WHITESPACE | '' \n"
								+ "COMMENT: '//' COMMENTTEXT \n"
								+ "COMMENTTEXT: <linefeed> | <end> | <any> COMMENTTEXT \n"
								+ "UNDERSCORE: '_' \n") {
												
				void init()
				{
					install("S", new Emitter() { void run() { input.setColor(offset, length, Color.black); } });
					install("COMMENT", new Emitter() { void run() { input.setColor(offset, length, Color.gray); } });
					install("NUMBER", new Emitter() { void run() { input.setColor(offset, length, new Color(0f, 0f, 0.9f)); } });
					install("STRING", new Emitter() { void run() { input.setColor(offset, length, Color.red); } });
					install("LITERAL", new Emitter() { void run() { input.setColor(offset, length, new Color(0.8f, 0, 0.8f)); } });
				}
			};
		}

		syntaxText = input.getText();
		syntaxResult = syntaxBNF.parse(syntaxText);
		
		if (syntaxResult != null)
		{
			//bnf.setText(result.toXML(text.toCharArray(), 0));
		
			bnf.root().removeAllChildren();
			
			// silly way to inline functions but whatever
			class Popper
			{
				Popper(TreePane.Node node, VarietyBNF.Rule.Result result)
				{
					node = bnf.new Node(node, result.getTitle() + " : " + syntaxText.substring(result.offset, result.offset + result.length));

					if (result.terms != null && result.terms.length != 0)
						for (int i = 0; i < result.terms.length; i++)
							new Popper(node, result.terms[i]);
				}
			}
			
			new Popper(bnf.root(), syntaxResult);
			bnf.treeModel.nodeStructureChanged(bnf.root());
		}
	}
	void calculateCFG()
	{
		// what we are doing NOW: pretend the input is a grammar and read it in.
		CFG dummyParser = new CFG();

		final CFG.GrammarParser gp = new CFG.GrammarParser();
		gp.installRules(dummyParser, input.getText());
		
		cfg.root().removeAllChildren();
		
		final ArrayList<TreePane.Node> stack = new ArrayList<TreePane.Node>();
		stack.add(cfg.root());
		
		gp.mainResult.doEmit(0, gp.new Emitter() {
			
			void emit(int mark, CFG.Rule.Result result)
			{
				TreePane.Node node = cfg.new Node(stack.get(stack.size() - 1), 
					"" + result + (result.successful() ? (" • " + result.charactersConsumed()) : "") + " !! " + result.charactersTested()
					+ " \"" + gp.text.substring(mark, mark + result.charactersConsumed()) + "\"");
				
				stack.add(node);
			}
			void afterEmit(int mark, CFG.Rule.Result result)
			{
				stack.remove(stack.size() - 1);
			}
		});
		cfg.treeModel.nodeStructureChanged(cfg.root());
	}
	
	synchronized void reselect(int offset, int length)
	{
		//System.out.println(offset + "," + length);
	}
	
	synchronized void calculationDone(CalculationThread thread)
	{
		if (thread != calcThread)
			return;
			
		thread.lexVariety.describeFancy = false;
		
		Variety.Formula[] formulas = thread.lexVariety.listFormulas();
		String output = "";
		for (int f = 0; f < formulas.length; f++)
			output += formulas[f].describe();

		// save the formulas for graphing
		geometry.setFormulas(formulas, thread.lexVariety.inequalities);
		
		// save the algebra
		algebra.setText(output);
		//algebra.setEnabled(true);
		
		// save the groebner
		thread.lexVariety.describeFancy = true;
		output = "lex: " + quickOutput(thread.lexVariety) + "\n\n";
		
		// let's show the original variety broken down
		Variety breakdownVariety = new Variety(thread.lexVariety);
		breakdownVariety.describeFancy = true;
		thread.variety.describeFancy = true;
		
		ArrayList<Variety.Variable> breakdownOrder = breakdownVariety.getVariableOrdering();
		for (int v = 0; v < thread.restrictedVariables; v++)
			breakdownOrder.add(v, breakdownVariety.new Variable(thread.variety.variableArray.get(v).title, thread.variety.variableArray.get(v).temporary));
		breakdownVariety.setVariableOrdering(breakdownOrder);
		
		//output += "breakdown: " + quickOutput(breakdownVariety) + "\n\n";

		for (int p = 0; p < thread.variety.polynomials.size(); p++)
		{
			Variety.Polynomial dividend = breakdownVariety.new Polynomial(thread.variety.polynomials.get(p));
			
			//System.out.println("before: " + dividend.describe());
			breakdownVariety.ordering.sortPolynomial(dividend);
			//System.out.println("after: " + dividend.describe());

			ArrayList<Variety.Polynomial> factorMap = new ArrayList<Variety.Polynomial>(breakdownVariety.polynomials.size());
			for (int f = 0; f < breakdownVariety.polynomials.size(); f++)
				factorMap.add(null);
			
			breakdownVariety.syntheticDivision(dividend, breakdownVariety.polynomials, factorMap, 0);
			
			output += thread.variety.polynomials.get(p).describe() + " = ";
			for (int f = 0; f < breakdownVariety.polynomials.size(); f++)
				if (factorMap.get(f) != null)
					output += "(" + factorMap.get(f).describe() + ")[" + breakdownVariety.polynomials.get(f).describe() + "] + ";
			output += dividend.describe() + "\n";
		}
		
		
		groebner.setText(thread.groebnerOutput + "\n\n" + output);
		//groebner.setEnabled(true);
	}
	String quickOutput(Variety variety)
	{
		String output = "";
		for (int v = 0; v < variety.variableCount(); v++)
			output += (v == 0 ? "" : " > ") + variety.variableArray.get(v).title;
		output += "\n{\n";
		for (int p = 0; p < variety.polynomials.size(); p++)
		{
			output += "  " + variety.polynomials.get(p).describe() + "\n";
		}
		output += "}";
		return output;
	}
	
	class CalculationThread extends Thread
	{
		String groebnerOutput = "";
		Variety variety, lexVariety, grevlexVariety;
		int restrictedVariables;
		Variety.StandardBasis basis = null;
		boolean cancel = false;
		
		synchronized public void nevermind()
		{
			cancel = true;
			if (basis != null)
				basis.cancel();
		}
		synchronized public void setBasis(Variety.StandardBasis newBasis)
		{
			basis = newBasis;
		}
		
		public void run() 
		{
			variety = new Variety();
			
			String[] system = input.getIdeal();
			
			input.setError(null);
			for (int i = 0; i < system.length; i++)
			{
				Variety.Equation eq = variety.new Equation(system[i]);
				if (eq.parseError != null)
				{
					input.setError(" " + eq.parseError);
					return;
				}
			}
			
			// clunky syntax
			Variety.Equation orEq = variety.new Equation("");
			Variety.Variable[] ordering = orEq.retrieveVariables(input.getOrdering());
			if (orEq.parseError != null)
			{
				input.setError(" " + orEq.parseError);
				return;
			}
			
			// reorder per request
			ArrayList<Variety.Variable> oldOrder = variety.getVariableOrdering();
			ArrayList<Variety.Variable> newOrder = new ArrayList<Variety.Variable>();

			/*String test = "old: ";
			for (int v = 0; v < newOrder.size(); v++)
				test += newOrder.get(v).title + " ";*/
				
			ArrayList<Variety.Variable> requestedVariables = new ArrayList<Variety.Variable>();
			
			for (int v = 0; v < ordering.length; v++)
			{
				requestedVariables.remove(ordering[v]);
				requestedVariables.add(ordering[v]);
			}
			
			for (int v = 0; v < oldOrder.size(); v++)
				if (oldOrder.get(v).temporary && requestedVariables.contains(oldOrder.get(v)) == false)
				{
					newOrder.add(oldOrder.get(v));
					oldOrder.remove(v--);
				}
			

			// unrequested variables go in backwards
			restrictedVariables = newOrder.size();
			
			for (int v = oldOrder.size() - 1; v >= 0; v--)
				if (requestedVariables.contains(oldOrder.get(v)) == false)
				{
					newOrder.add(oldOrder.get(v));
					restrictedVariables++;
				}
				
			for (int v = requestedVariables.size() - 1; v >= 0; v--)
				newOrder.add(requestedVariables.get(v));
				
				/*test += "new: ";
				for (int v = 0; v < newOrder.size(); v++)
					test += newOrder.get(v).title + " ";
				System.out.println(test);*/
				
			variety.setVariableOrdering(newOrder);
			
			/*variety.setOrdering(variety.new LexOrdering());
			SwingUtilities.invokeLater(new Runnable() {
				public void run()
					{ groebner.setText(groebnerOutput); }
			});*/
			
			grevlexVariety = new Variety(variety);
			grevlexVariety.setOrdering(grevlexVariety.new ElimOrdering(restrictedVariables));
			
			setBasis(grevlexVariety.new StandardBasis());
			basis.calculate();
			if (cancel)
				return;

			// describe
			grevlexVariety.describeFancy = true;

			groebnerOutput += "grevlex: " + quickOutput(grevlexVariety);
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run()
					{ groebner.setText(groebnerOutput); }
			});

			// can we do this after grevlex? can I find a counterexample? would it only work in elim ordering?
			lexVariety = new Variety(grevlexVariety);
			
			if (input.getStrict())
			{
				newOrder = lexVariety.getVariableOrdering();
				
				for (int i = 0; i < restrictedVariables; i++)
					newOrder.remove(0);
				
				lexVariety.setVariableOrdering(newOrder);
			}
			else
			{
				restrictedVariables = 0;
			}
			
			lexVariety.setOrdering(lexVariety.new LexOrdering());
			setBasis(lexVariety.new StandardBasis());
			basis.calculate();
			
			// the 'typing' bounce
		/*	try
			{
				Thread.sleep(250);
			}
			catch (InterruptedException ie) {}*/
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run()
					{ calculationDone(CalculationThread.this); }
			});
		}
	}

	InputPane input = new InputPane();
	TextPane algebra = new TextPane();
	TextPane groebner = new TextPane();
	TreePane bnf = new TreePane();
	TreePane cfg = new TreePane();
	VitalityPane vitality = new VitalityPane();

	class Dot
	{
		String title;
		BigRational x, y;

		Dot(String t, int xIn, int yIn)
		{ 
			title = t; 
			if (title != null)
			 	dots.add(this); 
			x = new BigRational(xIn, 1); 
			y = new BigRational(yIn, 1); 
		}
	}
	ArrayList<Dot> dots = new ArrayList<Dot>();
	
	GraphPane geometry = new GraphPane() 
	{
		class Graph
		{
			Variety.Formula formula;
			Graph[] inputGraphs;
			java.util.List<Variety.Inequality> inequalities = new ArrayList<Variety.Inequality>();
			
			// this part pretty much gets redone every draw:
			Color color;
			GeneralPath path;
			double valueThisTime;
			
			// inequality highlighting
			GeneralPath hilight;
			ArrayList<GeneralPath> hilights = new ArrayList<GeneralPath>();
		}
		ArrayList<Graph> graphs = new ArrayList<Graph>();
		Graph independentGraph = null;
		
		void setFormulas(Variety.Formula[] formulas, java.util.List<Variety.Inequality> inequalities)
		{
			graphs.clear();
			independentGraph = null;
			
			for (int f = 0; f < formulas.length; f++)
			{
				if (true || formulas[f] instanceof Variety.VariableFormula)
				{
					independentGraph = new Graph();
					independentGraph.formula = formulas[f];
					independentGraph.inputGraphs = new Graph[] { independentGraph };
					independentGraph.color = Color.gray;
					graphs.add(independentGraph);
					//System.out.println("Set my independent variable to " + independentGraph.formula.output.title);
					
					findFormula: while (f < formulas.length - 1 /*&& graphs.size() < 4*/)
					{
						f++;
						
						Graph graph = new Graph();
						graph.formula = formulas[f];
						graph.inputGraphs = new Graph[formulas[f].inputs.length];
						
						findInput: for (int i = 0; i < formulas[f].inputs.length; i++)
						{
							// has this input been graphed
							for (int p = 0; p < graphs.size(); p++)
								if (graphs.get(p).formula.output == formulas[f].inputs[i])
								{
									graph.inputGraphs[i] = graphs.get(p);
									continue findInput;
								}
							
							// don't have the input.
							continue findFormula;
						}
						
						// this one is doable
						//System.out.println("Added variable " + graph.formula.output.title + " with " + graph.inputGraphs.length + " inputs");
						graph.color = (graphs.size() % 3 == 1) ? Color.red : (graphs.size() % 3 == 2) ? Color.green : Color.blue;
						graphs.add(graph);
					}
					
					break;
				}
			}

			for (int i = 0; i < inequalities.size(); i++)
				for (int g = 0; g < graphs.size(); g++)
					if (graphs.get(g).formula.output == inequalities.get(i).variable)
						graphs.get(g).inequalities.add(inequalities.get(i));

			canvas.repaint();
		}

		Dot origin = new Dot(null, 0,0);
		int originRadius = 8;	// also used for axis grabbing
		
		Dot selection = null;	// can be origin or resizer
		int dotRadius = 5;
		BigRational dotRadiusSq = new BigRational(dotRadius*dotRadius, 1);
		BigRational originRadiusSq = new BigRational(originRadius*originRadius, 1);

		Dot resizer = new Dot(null, 0,0);
		BigRational resizerX = null, resizerY = null;
		
		class Zoom
		{
			// multiply the units by the scale to get the pixels
			BigRational scaleX = new BigRational(60,1);
			BigRational scaleY = new BigRational(-60,1);
		}
		Zoom graphZoom = new Zoom();
		
		class Marker
		{
			Color color;
			String title;
			double x, y;
			
			Marker(Color c, String t, double xP, double yP)
			{
				color = c;
				title = t;
				x = xP;
				y = yP;
				
				int l = 0;
				while (l < markers.size())
					if (markers.get(l).y < y)
						break;
					else
						l++;
						
				markers.add(l, this);
			}
		}
		ArrayList<Marker> markers = new ArrayList<Marker>();
		
		void render(Graphics2D g)
		{
			Rectangle bounds = canvas.getBounds();
			markers.clear();
			
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			// note that if origin.x is at some far distant pixel, it would be better to do translation ourselves
			g.translate(new BigRational(bounds.width, 2).add(origin.x).doubleValue(), new BigRational(bounds.height, 2).add(origin.y).doubleValue());
			
			// these are the view rectangle.
			BigRational leftPixel = new BigRational(-bounds.width/2, 1).subtract(origin.x);
			BigRational rightPixel = new BigRational(bounds.width/2, 1).subtract(origin.x);
			BigRational topPixel = new BigRational(-bounds.height/2, 1).subtract(origin.y);
			BigRational bottomPixel = new BigRational(bounds.height/2, 1).subtract(origin.y);
			
			//System.out.println("view: (" + leftPixel + "," + topPixel + "," + rightPixel + "," + bottomPixel + ")");
			
			// ticks that are 10 pixels apart or more should be drawn at 0 opacity and fade in. if scale is 1, that makes the
			// ticks 10 pixels = 10 units apart. if scale is 2, they are 20 pixels = 10 units. if scale is 12, they are 120 pixels = 10 units
			// which means the ticks should be 12 pixels = 1 unit apart. If scale is 1234, they are 12340 pixels = 10 units, which means 1234
			// pixels = 1 unit, which means 12.34 pixels = 0.01 unit. In the other direction, if scale is 0.1234, they are 1.234 pixels = 10 units
			// which means 12.34 pixels = 100 units. If scale is 0.01234, they are 0.1234 pixels = 10 units -> 1.234 pixels = 100 units -->
			// 12.34 pixels = 1000 units.
			
			// So basically get the base 10 log of the scale. if scale = 1234, log = 3.1. That means to back off the scale by 3 orders of
			// magnitude, so the pixels are 10*(1234/10^3) = 12.34 and the units are 10/(10^3) = 0.01 unit.
			// Or if scale = 0.1234, log = -0.9. Jack up the scale by 1 orders of magnitude, so the pixels are 10*(0.1234*10^1) = 12.34
			// and the units are 10*(10^1) = 100 units.
			
			// draw axies
			double scalingLog, scalingLogFloor, scalingLogFract, pixels, units;
			BigRational unitsRat;
			
			// draw the x axis
			g.setColor((selection == origin || (selection == resizer && resizerX != null)) ? Color.black : Color.gray);
			g.draw(new Line2D.Double(leftPixel.doubleValue(), 0, rightPixel.doubleValue(), 0));
			g.setColor(Color.gray);
			
			scalingLog = graphZoom.scaleX.abs().logTen();
			scalingLogFloor = (int)Math.floor(scalingLog);
			scalingLogFract = scalingLog - scalingLogFloor;
			pixels = 10 * graphZoom.scaleX.doubleValue() / Math.pow(10, scalingLogFloor);
			units = 10 / Math.pow(10, scalingLogFloor);
			unitsRat = (scalingLogFloor > 0) ? new BigRational(1, 10).pow((int)scalingLogFloor) : new BigRational(10, 1).pow(-(int)scalingLogFloor);
			
			//System.out.println(scalingLog + "," + pixels + "," + units);
			
			for (int i = 1; Math.abs(i*pixels) <= bounds.width; i++)
			{
				boolean major = (i % 10 == 0), minor = (i%5 == 0);
				if (major == false)
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(scalingLogFract)));
				g.draw(new Line2D.Double(-i*pixels,-2 - (scalingLogFract)*((minor ? 2 : 0) + (major ? 2 : 0)),-i*pixels,1 ));
				g.draw(new Line2D.Double(i*pixels,-2 - (scalingLogFract)*((minor ? 2 : 0) + (major ? 2 : 0)),i*pixels, 1));
				if (major == false)
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(scalingLogFract*scalingLogFract)));
				
				// labeling. If units is 0.1, that's 100m, 200m,..900m, 1, 1.1, ... 1.9, 2
				String not = new BigRational(i*10, 1).multiply(unitsRat).engFormat(false);
				g.drawString(not, (int)(i*pixels) -8, 14);
				g.drawString("-" + not, -(int)(i*pixels) -8, 14);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
			}
			
			if (independentGraph != null)
			{
				g.drawString(independentGraph.formula.output.title + " -> ", (int)rightPixel.doubleValue() -40, 30);
			}
			
			// draw the y axis
			g.setColor((selection == origin || (selection == resizer && resizerY != null)) ? Color.black : Color.gray);
			g.draw(new Line2D.Double(0, topPixel.doubleValue(), 0, bottomPixel.doubleValue()));
			g.setColor(Color.gray);
			
			scalingLog = graphZoom.scaleY.abs().logTen();
			scalingLogFloor = (int)Math.floor(scalingLog);
			scalingLogFract = scalingLog - scalingLogFloor;
			pixels = 10 * graphZoom.scaleY.doubleValue() / Math.pow(10, scalingLogFloor);
			units = 10 / Math.pow(10, scalingLogFloor);
			unitsRat = (scalingLogFloor > 0) ? new BigRational(1, 10).pow((int)scalingLogFloor) : new BigRational(10, 1).pow(-(int)scalingLogFloor);
			
			for (int i = 1; Math.abs(i*pixels) <= bounds.height; i++)
			{
				boolean major = (i % 10 == 0), minor = (i%5 == 0);
				if (major == false)
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(scalingLogFract)));
				g.draw(new Line2D.Double(-2 - (scalingLogFract)*((minor ? 2 : 0) + (major ? 2 : 0)),-i*pixels,1,-i*pixels));
				g.draw(new Line2D.Double(-2 - (scalingLogFract)*((minor ? 2 : 0) + (major ? 2 : 0)),i*pixels, 1, i*pixels));
				if (major == false)
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(scalingLogFract*scalingLogFract)));
				
				String not = new BigRational(i*10, 1).multiply(unitsRat).engFormat(false);
				g.drawString(not, 1, (int)(i*pixels) + 4);
				g.drawString("-" + not, 1, -(int)(i*pixels) + 4);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
			}

			// draw the origin or selection markers
			if (selection == origin)
			{
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
				g.fillRect(-3, -10, 7, 21);
				g.fillRect(-10, -3, 21, 7);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
			}
			
			if (selection == resizer && resizerX != null)
			{
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
				g.fillRect((int)resizerX.multiply(graphZoom.scaleX).doubleValue()-10, -3, 21, 7);
				g.fill(new Rectangle2D.Double(resizerX.multiply(graphZoom.scaleX).doubleValue(), -10000, 0.8, 20000));
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				
				if (independentGraph != null)
					new Marker(Color.gray, independentGraph.formula.output.title + " ( " + resizerX.engFormat(false) + " )", 
						resizerX.multiply(graphZoom.scaleX).doubleValue() - 2, -8d);
			}
			if (selection == resizer && resizerY != null)
			{
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
				g.fillRect(-3, (int)resizerY.multiply(graphZoom.scaleY).doubleValue()-10, 7, 21);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
			}
			
			// draw x boundaries
			/*g.setColor(Color.gray);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
			BigRational leftBound = new BigRational(100,1), rightBound = new BigRational(200,1);
			g.fill(new Rectangle2D.Double(leftBound.multiply(graphZoom.scaleX).doubleValue(), topPixel.doubleValue(), 
				rightBound.subtract(leftBound).multiply(graphZoom.scaleX).doubleValue(), -topPixel.doubleValue()));
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));*/
			
			// draw the graphs
			if (independentGraph != null)
			{
				for (int p = 0; p < graphs.size(); p++)
				{
					Graph graph = graphs.get(p);
					graph.path = null;
					graph.hilights = new ArrayList<GeneralPath>();
				}
				
				// we have to have a left to right thing. if leftPixel is -300 and notchPixels is 13, then go from floor(-300/13) etc.
				int notchPixels = 2;
				int leftNotch = (int)Math.floor(leftPixel.doubleValue() / notchPixels), rightNotch = (int)Math.ceil(rightPixel.doubleValue() / notchPixels);
				//System.out.println(leftNotch + " " + rightNotch);
				if (leftNotch > rightNotch)
				{
					int oldLeftNotch = leftNotch;
					leftNotch = rightNotch;
					rightNotch = oldLeftNotch;
				}
				boolean firstNotch = true;
				while (leftNotch <= rightNotch)
				{
					// say leftNotch is -9, then that is at pixel leftNotch*notchPixels, and the translation from pixels to units is...
					//String not = new BigRational(leftNotch*notchPixels, 1).divide(graphZoom.scaleX).engFormat(false);
					//g.drawString(not, (int)(leftNotch*notchPixels) -8, 100);

					independentGraph.valueThisTime = new BigRational(leftNotch*notchPixels, 1).divide(graphZoom.scaleX).doubleValue();
					
					for (int p = 0; p < graphs.size(); p++)
					{
						Graph graph = graphs.get(p);
						double[] inputs = new double[graph.inputGraphs.length];
						for (int i = 0; i < inputs.length; i++)
							inputs[i] = graph.inputGraphs[i].valueThisTime;
						graph.valueThisTime = graph.formula.evaluateDouble(inputs)[0];
						//System.out.println(graph.formula.output.title + " evaluates to " + graph.valueThisTime);
						//System.out.println("pixel = " + pixel + ", input = " + input + ", output = " + output);
						double xPixel = leftNotch*notchPixels, yPixel = graph.valueThisTime * graphZoom.scaleY.doubleValue();
						
						if (graph.path == null)
						{
							graph.path = new GeneralPath();
							graph.path.moveTo(xPixel, yPixel);
						}
						else
						{
							graph.path.lineTo(xPixel, yPixel);
						}
						
						boolean satisfied = false;
						for (int i = 0; i < graph.inequalities.size(); i++)
						{
							// evaluate the poly, compare, etc
							Variety.Inequality inequality = graph.inequalities.get(i);
							
							//System.out.println(inequality.polynomial.describe() + " with " + graph.valueThisTime);
							double[] map = new double[inequality.variable.index()+1];
							map[inequality.variable.index()] = graph.valueThisTime;
							
							double value = inequality.polynomial.evaluate(map);
							if ((value < 0) == (inequality.relation == VarietyParser.IS_LESS || inequality.relation == VarietyParser.IS_NOT_MORE))
								satisfied = true;
							else
							{
								satisfied = false;
								break;
							}
						}

						if (satisfied == false && graph.hilight != null)
						{
							graph.hilight.lineTo(xPixel, yPixel);
							graph.hilight = null;
						}
						else if (satisfied == true)
						{
							//System.out.println(inequality.polynomial.describe() + " at " + graph.valueThisTime + " is " + value);
							if (graph.hilight == null)
							{
								graph.hilight = new GeneralPath();
								graph.hilight.moveTo(xPixel, yPixel);
								graph.hilights.add(graph.hilight);
							}
							else
							{
								graph.hilight.lineTo(xPixel, yPixel);
							}
						}
						
						/*if (p != 0)
							g.fill(new Ellipse2D.Double(xPixel - 2, yPixel - 2, 4, 4));*/
					}

					firstNotch = false;
					leftNotch++;
				}
				
				if (independentGraph.formula instanceof Variety.ValueFormula)
				{
					g.setColor(Color.gray);
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
					g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

					double value = independentGraph.formula.evaluateDouble(new double[0])[0];
					value *= graphZoom.scaleX.doubleValue();
					
					g.draw(new Line2D.Double(value, topPixel.doubleValue(), value, bottomPixel.doubleValue()));

					g.setStroke(new BasicStroke(1f));
				}
				
				for (int p = 1; p < graphs.size(); p++)
				{
					Graph graph = graphs.get(p);
					//System.out.println(graph.formula.output.title + " drawn ");
					
					g.setColor(graph.color);
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
					g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

					for (int h = 0; h < graph.hilights.size(); h++)
						g.draw(graph.hilights.get(h));
					
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
					g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

					if (graph.path != null)
						g.draw(graph.path);

					g.setStroke(new BasicStroke(1f));
				}

				if (selection == resizer && resizerX != null)
				{
					independentGraph.valueThisTime = resizerX.doubleValue();
					
					for (int p = 0; p < graphs.size(); p++)
					{
						Graph graph = graphs.get(p);
						double[] inputs = new double[graph.inputGraphs.length];
						for (int i = 0; i < inputs.length; i++)
							inputs[i] = graph.inputGraphs[i].valueThisTime;
						graph.valueThisTime = graph.formula.evaluateDouble(inputs)[0];

						BigRational xRat = resizerX;
						BigRational yRat = new BigRational((int)Math.round(graph.valueThisTime*1000), 1000);
						double xPixel = xRat.multiply(graphZoom.scaleX).doubleValue(), 
							yPixel = yRat.multiply(graphZoom.scaleY).doubleValue();
						
						//System.out.println("x = " + xRat + " y = " + yRat + " xPixel = " + xPixel + " yPixel = " + yPixel);
						g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

						if (p != 0)
						{
							g.setColor(graph.color);
							g.fill(new Ellipse2D.Double(xPixel - 5, yPixel - 5, 10, 10));
							
							new Marker(graph.color, graph.formula.output.title + "( " + yRat.engFormat(false) + " )", xPixel, yPixel);
						}
					}
					
				}
			}
			
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

			double drawY = bottomPixel.doubleValue();
			for (int l = 0; l < markers.size(); l++)
			{
				Marker marker = markers.get(l);
				
				drawY = (drawY - 15 > marker.y) ? marker.y : drawY - 15;
				
				g.setColor(Color.white);
				for (int xo = -2; xo <= 2; xo += 1)
					for (int yo = -2; yo <= 2; yo += 1)
						g.drawString(marker.title, (float)marker.x + 7 + xo, (float)drawY - 1 + yo);

				g.setColor(marker.color);						g.drawString(marker.title, (float)marker.x + 7, (float)drawY - 1);
			}

			// draw the dots
			for (int d = 0; d < dots.size(); d++)
			{
				Dot dot = dots.get(d);
				g.setColor(Color.red);
				
				double dotPixelX = dot.x.multiply(graphZoom.scaleX).doubleValue();
				double dotPixelY = dot.y.multiply(graphZoom.scaleY).doubleValue();
				Ellipse2D.Double dotShape = new Ellipse2D.Double(dotPixelX - dotRadius, dotPixelY - dotRadius, dotRadius*2, dotRadius*2);
				
				if (dot == selection)
				{
					g.fill(dotShape);
					g.drawString(dot.title + "( " + dot.x.engFormat(true) + "," + dot.y.engFormat(false) + " )", (float)dotPixelX + 7, (float)dotPixelY - 4);
				}
				else
				{
					g.draw(dotShape);
					g.setColor(Color.lightGray);
					g.drawString(dot.title, (float)dotPixelX + 7, (float)dotPixelY - 4);
				}
					
			}
		}
		
		void hover(int inX, int inY)
		{
			// these are the onscreen coordinates
			BigRational x = new BigRational(inX - canvas.getWidth()/2, 1).subtract(origin.x);
			BigRational y = new BigRational(inY - canvas.getHeight()/2, 1).subtract(origin.y);
			
			canvas.setCursor(Cursor.getDefaultCursor());

			//System.out.println(x.toString() + "," + y.toString());
			for (int d = dots.size() - 1; d >= 0; d--)
			{
				Dot dot = dots.get(d);
				
				BigRational xOffset = x.subtract(dot.x.multiply(graphZoom.scaleX)), yOffset = y.subtract(dot.y.multiply(graphZoom.scaleY));

				if (xOffset.pow(2).add(yOffset.pow(2)).compareTo(dotRadiusSq) < 1)
				{
					if (selection != dot)
					{
						selection = dot;
						dots.remove(dot);
						dots.add(dot);
						canvas.repaint();
					}
					return;
				}
			}
			
			boolean overXAxis = (y.pow(2).compareTo(originRadiusSq) < 1), overYAxis = (x.pow(2).compareTo(originRadiusSq) < 1);
			
			if (overXAxis && overYAxis)
			{
				selection = origin;
				canvas.repaint();
				return;
			}

			if (overXAxis)
			{
				selection = resizer;
				resizerX = x.divide(graphZoom.scaleX);
				resizerY = null;
				canvas.repaint();
				return;
			}

			if (overYAxis)
			{
				selection = resizer;
				resizerX = null;
				resizerY = y.divide(graphZoom.scaleY);
				canvas.repaint();
				return;
			}
			
			if (true)
			{
				// experimental
				selection = resizer;
				resizerX = x.divide(graphZoom.scaleX);
				resizerY = y.divide(graphZoom.scaleY);
				canvas.repaint();
				return;
			}
			
			if (selection != null)
			{
				selection = null;
				canvas.repaint();
			}
			
		}
		void drag(int inX, int inY)
		{
			int border = 10;
			
			if (inX < border) 
				inX = border; 
			else if (inX > canvas.getWidth() - border) 
				inX = canvas.getWidth() - border;
			if (inY < border) 
				inY = border; 
			else if (inY > canvas.getHeight() - border) 
				inY = canvas.getHeight() - border;
			
			// these are the graphspace coordinates
			BigRational x = new BigRational(inX - canvas.getWidth()/2, 1);
			BigRational y = new BigRational(inY - canvas.getHeight()/2, 1);

			if (selection == origin)
			{
				origin.x = x;
				origin.y = y;
				canvas.repaint();
				return;
			}

			// these are the view coordinates
			x = x.subtract(origin.x);
			y = y.subtract(origin.y);

			if (selection == resizer)
			{
				// at hover time, the resizer was at graphspace pixel resizerX, resizerY.
				// now that the same pixel is in a new location we can easily find the new scale.
				// if we're at view pixel 100 and that is now graphspace pixel 200, the scale is 1/2.
				if (x.compareTo(new BigRational(-originRadius,1)) == 1 && x.compareTo(new BigRational(originRadius,1)) == -1)
					x = new BigRational(originRadius * graphZoom.scaleX.signum(),1);
				if (y.compareTo(new BigRational(-originRadius,1)) == 1 && y.compareTo(new BigRational(originRadius,1)) == -1)
					y = new BigRational(originRadius * graphZoom.scaleY.signum(),1);
					
				// it would be helpful to have a flip animation when you reflect the data across an axis.
				if (resizerX != null)
					graphZoom.scaleX = x.divide(resizerX);
				if (resizerY != null)
					graphZoom.scaleY = y.divide(resizerY);
					
				canvas.setCursor(Cursor.getPredefinedCursor((resizerX != null) ? Cursor.E_RESIZE_CURSOR : Cursor.N_RESIZE_CURSOR));
				canvas.repaint();
				return;
			}
			
			if (selection != null)
			{
				selection.x = x.divide(graphZoom.scaleX);
				selection.y = y.divide(graphZoom.scaleY);
				canvas.repaint();
			}
		}
	};
	
}