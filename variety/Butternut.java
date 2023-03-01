import java.util.*;

class Butternut
{
	public static void main(String arg[])
	{
		final Butternut b = new Butternut("S: '1' '2' '3'"
									/*+ "S: NAME ',' S | NAME \n"
									+ "NAME: FIRST ' ' LAST \n"
									+ "FIRST: 'Manuel' | 'Manny' | 'Man' \n"
									+ "LAST: 'Worker'"*/);
				
		b.tree.walk(new ResultWalker() {
			int indent = 0;
			String spaces() 
				{ return "                             ".substring(0,indent*2); }
				
			public void entering(Rule.Result result)
			{ 
				if (result instanceof TerminalRule.TerminalResult)
				{
					System.out.println(spaces() + "'" + ((TerminalRule)result.rule()).literal + "' (success = " + result.success() + ")"); 
				}
				else //if (b.ruleNames.containsKey(result.rule()))
				{
					System.out.println(spaces() + "<" + result.rule().toString() + "> (success = " + result.success() + ")"); 
					indent++;
				}
			}
			public void leaving(Rule.Result result)
			{ 
				if (result instanceof TerminalRule.TerminalResult)
				{
				}
				else //if (b.ruleNames.containsKey(result.rule()))
				{
					indent--; 
					System.out.println(spaces() + "</" + result.rule().toString() + ">"); 
				}
			}
		});
	}
	
	AliasRule main;
	char[] text;
	Rule.Result tree;
	
	// store named rules
	HashMap<String,Rule> rules = new HashMap<String,Rule>();
	HashMap<Rule,String> ruleNames = new HashMap<Rule,String>();
	
	// creates a parser with persistent, initially empty storage
	Butternut(String grammar)
	{
		main = selectGrammar(grammar);
		text = "123CAN Weldoer".toCharArray();//new char[0];
		tree = main.parse(0);
		//experimentalParser("123CAN Weldoer".toCharArray());
	}
	
	// experimental
	void experimentalParser(char[] text)
	{
	}
	
	// reparse a portion of the text
	void setText(int offset, int length, String newText)
	{
	}

	// parse result tree
	interface ResultWalker
	{
		public void entering(Rule.Result result);
		public void leaving(Rule.Result result);
	}
	
	// rules
	abstract class Rule
	{
		abstract class Result
		{
			abstract boolean success();
			abstract int length();
			abstract void walk(ResultWalker walker);
			
			int failedLength = 0;
			
			Rule rule()
				{ return Rule.this; }
		}
		abstract Result parse(int c);
		
		public String toString()
			{ String name = ruleNames.get(this); return (name != null) ? name : super.toString(); }
	}
	
	class AliasRule extends Rule
	{
		String title;
		AliasRule(String t)
			{ title = t; }
		
		class AliasResult extends Result
		{
			Result inner;
			
			boolean success()
			{
				return inner.success();
			}
			int length()
			{
				return inner.length();
			}
			void walk(ResultWalker walker)
			{
				walker.entering(this);
				inner.walk(walker);
				walker.leaving(this);
			}
		}
		
		Result parse(final int c)
		{
			AliasResult result = new AliasResult();
			
			result.inner = rules.get(title).parse(c);
			return result;
		}
	}

	class TerminalRule extends Rule
	{
		final String literal;
		TerminalRule(String t)
			{ literal = t; }
		
		class TerminalResult extends Result
		{
			boolean match;
			
			boolean success()
			{
				return match;
			}
			int length()
			{
				return literal.length();
			}
			void walk(ResultWalker walker)
			{
				walker.entering(this);
				walker.leaving(this);
			}
		}
		
		Result parse(final int c)
		{
			TerminalResult result = new TerminalResult();
			
			result.match = (c + literal.length() <= text.length && literal.equals(new String(text, c, literal.length())));
			return result;
		}
	}
	
	class ConcatRule extends Rule
	{
		ArrayList<Rule> list = new ArrayList<Rule>();
		
		class ConcatResult extends Result
		{
			int index;
			Result[] inner = new Result[list.size()];
			
			boolean success()
			{
				return (index == list.size());
			}
			int length()
			{
				return inner[index].length();
			}
			void walk(ResultWalker walker)
			{
				walker.entering(this);
				for (int i = 0; i < index; i++)
					inner[i].walk(walker);
				walker.leaving(this);
			}
		}
		
		Result parse(int c)
		{
			ConcatResult result = new ConcatResult();
			
			System.out.println("concat has " + list.size());
			for (result.index = 0; result.index < list.size(); result.index++)
			{
				System.out.println("trying concat #" + result.index); 
				result.inner[result.index] = list.get(result.index).parse(c);
				
				if (result.inner[result.index].success())
				{
					c += result.inner[result.index].length();
					System.out.println("concat #" + result.index + " successful with length " + result.inner[result.index].length()); 
					continue;
				}
				
				result.index++;
				break;
			}
			
			return result;
		}
	}

	class ChoiceRule extends Rule
	{
		ArrayList<Rule> list = new ArrayList<Rule>();

		class ChoiceResult extends Result
		{
			int index;
			Result[] inner = new Result[list.size()];
			
			boolean success()
			{
				return (index != list.size());
			}
			int length()
			{
				return inner[index].length();
			}
			void walk(ResultWalker walker)
			{
				walker.entering(this);
				for (int i = 0; i < index; i++)
					inner[i].walk(walker);
				walker.leaving(this);
			}
		}
		
		Result parse(final int c)
		{
			ChoiceResult result = new ChoiceResult();
			
			for (result.index = 0; result.index < list.size(); result.index++)
			{
				result.inner[result.index] = list.get(result.index).parse(c);
				
				if (result.inner[result.index].success())
				{
					break;
				}
			}
			return result;
		}
	}
	/*class RepeatRule extends Rule
	{
		Rule rule;
	}
	class OptionalRule extends Rule
	{
		Rule rule;
	}*/
		
	// text to grammar import
	AliasRule selectGrammar(String grammar)
	{
		String[] lines = grammar.split("\n");
		String mainRuleName = null;
		
		consumeLine: for (int l = 0; l < lines.length; l++)
		{
			CharFeeder feeder = new CharFeeder();
			feeder.str = lines[l].toCharArray();
			
			//System.out.println("feeding " + feeder.getRemainder());
			
			feeder.skipWhitespace();
			if (feeder.getEOL())
				continue consumeLine;
				
			String ruleName = feeder.getLiteral();
			if (ruleName == null)
				throw new Error("line didn't start with literal at " + feeder.getRemainder());
				
			if (mainRuleName == null)
				mainRuleName = ruleName;
				
			feeder.skipWhitespace();
			if (feeder.getAssignSymbol() == false)
				throw new Error("rule wasn't followed by := at " + feeder.getRemainder());
			feeder.skipWhitespace();

			// early version, every named rule is a choice rule. later more EBNF.
			ChoiceRule choice = (ChoiceRule)rules.get(ruleName);
			if (choice == null)
			{
				choice = new ChoiceRule();
				rules.put(ruleName, choice);
				ruleNames.put(choice, ruleName);
			}
			
			System.out.println("making " + ruleName);
				
			consumeExpr: while (true)
			{
				ConcatRule concat = new ConcatRule();
				choice.list.add(concat);
				
				while (true)
				{
					String terminal = feeder.getTerminal();
					if (terminal != null)
					{
						concat.list.add(new TerminalRule(terminal));
						System.out.println("added " + terminal);
					}
					else
					{
						String literal = feeder.getLiteral();
						if (literal != null)
						{
							concat.list.add(new AliasRule(literal));
							System.out.println("added " + literal);
						}
						else
							throw new Error("Expected a term at " + feeder.getRemainder());
					}
					
					// what's after that?
					feeder.skipWhitespace();
					if (feeder.getOrSymbol())
					{
						feeder.skipWhitespace();
						continue consumeExpr;
					}
					if (feeder.getEOL())
						continue consumeLine;
				}
			}
		}
		
		// second pass: replace all the AliasRule with references to the actual rule. NM optimize later
		//Rule[] ruleArray = rules.values().iterator();
		
		return new AliasRule(mainRuleName);
	}
	
	class CharFeeder
	{
		char[] str;
		int c = 0;

		void skipWhitespace()
		{
			while (c < str.length && str[c] == ' ')
				c++;
		}
		String getRemainder()
		{
			return new String(str, c, str.length - c);
		}
		boolean getOrSymbol()
		{
			if (c < str.length && str[c] == '|')
			{
				c++;
				return true;
			}
			return false;
		}
		boolean getAssignSymbol()
		{
			if (c < str.length && str[c] == ':')
			{
				c++;
				return true;
			}
			return false;
		}
		boolean getEOL()
		{
			return (c == str.length);
		}
		String getLiteral()
		{
			if (c == str.length || Character.isLetter(str[c]) == false)
				return null;
			
			String result = "";
			do
			{
				result += str[c];
				c++;
			}
			while (c < str.length && (Character.isLetterOrDigit(str[c]) || str[c] == '_'));
			return result;
		}
		String getTerminal()
		{
			if (c == str.length || str[c] != '\'')
				return null;
			
			int start = c;
			do
			{
				c++;
			}
			while (c < str.length && str[c] != '\'');
			
			if (c == str.length || str[c] != '\'')
				return null;
				
			c++;
			return new String(str, start + 1, c - start - 2);
		}
		String getSymbol()
		{
			if (c == str.length || str[c] != '<')
				return null;
			
			int start = c;
			do
			{
				c++;
			}
			while (c < str.length && str[c] != '>');
			
			if (c == str.length || str[c] != '>')
				return null;
				
			c++;
			return new String(str, start + 1, c - start - 2);
		}
	}

}
