import java.util.*;
import java.math.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

class ShellTask
{
	public static void changed()
	{
	}
	public static void main(String[] arg)
	{
		JFrame frame = new JFrame("processing");
		frame.setLayout(new BorderLayout());
		
		final JTextPane area = new JTextPane();
		area.setFont(new Font("Menlo", Font.PLAIN, 11));
		area.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		area.getStyledDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e)
				{ changed(); }
			public void insertUpdate(DocumentEvent e)
				{ changed(); }
			public void removeUpdate(DocumentEvent e)
				{ changed(); }
		});
		
		final JScrollPane scroll = new JScrollPane(area, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		frame.add("Center", scroll);
		
		frame.setSize(1000,700);
		frame.setVisible(true);
		
		ShellTask task = new ShellTask("java VarietyApp") {
			int update()
			{
				System.out.println("" + output.length() + " bytes in buffer");
				area.setText(output);
				return 0;
			}
			void finish(int result)
			{
				System.out.println("finished with " + result + ":\n" + output);
				area.setText(output);
			}
		};
	}
	
	void finish(int result)
	{
	}
	int update()
	{
		return 0;
	}
	
	Process process;
	InputStream stream;
	String output = "";
	
	ShellTask(String command)
	{
		try
		{
			process = Runtime.getRuntime().exec(command); 
			stream = process.getErrorStream();
			
			/*new Thread() {
				public void run()
				{
					// blocks until it's over
					int result = 0;
					try
					{
						result = process.waitFor();
					}
					catch (InterruptedException ie)
					{
					}
					
					process = null;
					if (output.length() > 0)
						update();
					finish(result);
				}
			}.start();*/

			new Thread() {
				public void run()
				{
					byte[] data = new byte[1000];
					int size = 0;
					do
					{
						try
						{
							size = stream.read(data);
							if (size > 0)
							{
								output += new String(data, 0, size);
								System.out.println("got " + size + " bytes");
								output = output.substring(update());
							}
						}
						catch (IOException ioe)
						{
							size = -1;
						}
					}
					while (size != -1);

					if (output.length() > 0)
						update();
					try
					{
						finish(process.waitFor());
					}
					catch (InterruptedException ie)
					{
						finish(-1);
					}
				}
			}.start();
		}
		catch (IOException ioe)
		{
			System.out.println(ioe);
		}
	}
}

	/*Thread thready = new Thread() {
		public void run()
		{
			Process process;
			try
			{
				String builder = "";
				process = Runtime.getRuntime().exec("ls");

				BufferedInputStream bis = new BufferedInputStream(process.getInputStream());
				while (true)
				{
					if (bis.available() != 0)
					{
						builder += (char)bis.read();
					}
					else
					{
						try
						{
							int exitValue = process.exitValue();
							System.out.println(builder);
							System.out.println("terminate with " + exitValue);
							return;
						}
						catch (IllegalThreadStateException itse)
						{
						}
					}
					Thread.sleep(10);
				}
			}
			catch (Exception ioe)
			{
				System.out.println(ioe);
				return;
			}
			
		}
	};
	thready.start();
	
	return;*/

