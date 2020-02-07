import java.util.*;

class VarietyBNF
{
	public static void main(String[] arg)
	{
		VarietyBNF bnf = new VarietyBNF("S : LIST '.' LIST \n"
								+ "LIST : DIGIT LIST | DIGIT \n"
								+ "DIGIT : '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ") {
								};

		String test = "12.34";
		Rule.Result result = bnf.parse(test);
		if (result != null)
			System.out.println(result.toXML(test.toCharArray(), 0));

/*		VarietyBNF bnf = new VarietyBNF("S : '-' FN | FN \n"
									+ "FN : DL '.' DL | DL \n"
									+ "DL : D DL | D \n"
									+ "D : '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ") 
		{
			int numerator, denominator, increaser;
			
			void init()
			{
				install("S", new Emitter() { void run() { numerator = 0; denominator = 1; increaser = 1; } });
				install("'-'", new Emitter() { void run() { denominator = -1; } });
				install("'.'", new Emitter() { void run() { increaser = 10; } });
				install("D", new Emitter() { void run() { numerator = numerator*10 + (text.charAt(0) - '0'); denominator *= increaser; } });
			}
			
			void finish()
			{
				BigRational br = new BigRational(numerator, denominator);
				System.out.println("result = " + numerator + "/" + denominator + " = " + br.doubleValue());
			}
		};
		
		//bnf.parse("-123.45");
		
		String test = "1.24";
		Rule.Result result = bnf.parse(test);
		if (result != null)
			System.out.println(result.toXML(test.toCharArray(), 0));*/
	}
	
	// override this to install emitters or do it later.
	void init()
	{
	}
	// override this to perform actions when parsing completes.
	void finish(Rule.Result result)
	{
	}

	final ArrayList<Rule> rules = new ArrayList<Rule>();
	Rule findOrCreateRule(String title)
	{
		for (int i = 0; i < rules.size(); i++)
			if (rules.get(i).title.equals(title))
				return rules.get(i);

		Rule rule = new Rule(title);
		//System.out.println("adding rule " + title);

		if (title.charAt(0) == '\'')
			rule.terminal = title.substring(1, title.length() - 1);
		
		rules.add(rule);
		return rule;
	}
	
	// Parsing BNF is pointlessly harder if you are expected to know rules as soon as you reference them, so there are a few built-ins
	// that we hardcode and the rest look at their fields (title, terminal) and parse on that basis.
	Rule linefeedRule = new Rule("<linefeed>") {
		Result parse(char[] str, int c, String status)
		{
			if (c < str.length && str[c] == '\n')
			{
				Result result = new Result();
				result.offset = c;
				result.length = 1;
				return result;
			}
			
			return null;
		}
	};
	Rule anyRule = new Rule("<any>") {
		Result parse(char[] str, int c, String status)
		{
			if (c < str.length)
			{
				Result result = new Result();
				result.offset = c;
				result.length = 1;
				return result;
			}
			
			return null;
		}
	};
	Rule endRule = new Rule("<end>") {
		Result parse(char[] str, int c, String status)
		{
			if (c == str.length)
			{
				System.out.println(status);
				Result result = new Result();
				result.offset = c;
				result.length = 0;
				return result;
			}
			
			return null;
		}
	};

	class Rule
	{
		String title;		// either a literal like FN or a quoted termimal like '0'
		String terminal;	// if not null, match only this terminal.
		
		Rule(String t)
		{
			title = t;
		}
		
		ArrayList<Emitter> emitterList = new ArrayList<Emitter>();
		
		ArrayList<Expr> exprList = new ArrayList<Expr>();
		class Expr
		{
			ArrayList<Rule> termList = new ArrayList<Rule>();
		}
		
		class Result
		{
			int offset, length;
			Expr expr;
			Result[] terms;
			
			String getTitle()
			{
				return Rule.this.title;
			}
			void runEmitters(char[] str)
			{
				//if (emitterList.size() != 0)
				//	System.out.println("emitting for " + title + " = " + new String(str, offset, length));
				
				for (int i = 0; i < emitterList.size(); i++)
				{
					emitterList.get(i).text = new String(str, offset, length);
					emitterList.get(i).offset = offset;
					emitterList.get(i).length = length;
					emitterList.get(i).run();
				}
				
				if (terms != null)
					for (int t = 0; t < terms.length; t++)
						if (terms[t] != null)
							terms[t].runEmitters(str);
			}
			
			String toXML(char[] str, int indent)
			{
				String space = ("                                   ").substring(0, (indent > 30) ? 30 : indent);
				
				if (terms == null || terms.length == 0)
					return space + "<" + title + ">" + new String(str, offset, length) + "</" + title + ">\n";
				
				String output = "";
				for (int i = 0; i < terms.length; i++)
					output += terms[i].toXML(str, indent+1);
				return space + "<" + title + ">\n" + output + space + "</" + title + ">\n";
			}
		}
		
		Result parse(char[] str, int c, String status)
		{
			Result result = new Result();
			result.offset = c;
			
			if (terminal != null)
			{
				int terminalLength = terminal.length();
				if (c + terminalLength <= str.length && new String(str, c, terminalLength).equals(terminal))
				{
					result.length = terminalLength;
					//System.out.println("it is " + title);
					return result;
				}
				
				//System.out.println("it's not " + title);
				return null;
			}
			
			choice: for (int e = 0; e < exprList.size(); e++)
			{
				result.expr = exprList.get(e);
				result.terms = new Result[result.expr.termList.size()];
				
				concat: for (int t = 0; t < result.terms.length; t++)
				{
					Rule term = result.expr.termList.get(t);
					
					//System.out.println(new String(str, 0, c) + " " + status + "[" + e + "]." + term.title + " " + new String(str, c, str.length - c));
					result.terms[t] = term.parse(str, c, status + "[" + e + "]." + term.title);
					
					if (result.terms[t] != null)
					{
						c += result.terms[t].length;
						//System.out.println("we're " + (c - result.offset) + " into " + title);
						continue concat;
					}
					
					c = result.offset;
					//System.out.println("trying next choice for " + title);
					continue choice;
				}
				
				result.length = c - result.offset;
				//System.out.println("it is " + title);
				return result;
			}
			
			//System.out.println("it's not " + title);
			return null;
		}
	}

	// construct a new parser of the first rule using additional rules.
	VarietyBNF(final String grammar)
	{
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
		
		String[] lines = grammar.split("\n");
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

			// rule might already exist, in which case you're actually adding a new branch.
			Rule rule = findOrCreateRule(ruleName);

			feeder.skipWhitespace();
			if (feeder.getAssignSymbol() == false)
				throw new Error("rule wasn't followed by := at " + feeder.getRemainder());
			feeder.skipWhitespace();

			consumeExpr: while (true)
			{
				Rule.Expr expr = rule.new Expr();
				rule.exprList.add(expr);
				
				while (true)
				{
					String symbol = feeder.getSymbol();
					if (symbol != null)
					{
						if (symbol.equals("linefeed"))
							expr.termList.add(linefeedRule);
						else if (symbol.equals("any"))
							expr.termList.add(anyRule);
						else if (symbol.equals("end"))
							expr.termList.add(endRule);
						else
							throw new Error("Don't recognize symbol " + symbol);
					}
					else
					{
						String term = feeder.getLiteral();
						if (term == null)
						{
							term = feeder.getTerminal();
							if (term == null)
								throw new Error("Expected a term at " + feeder.getRemainder());

							// we could just return it with quotes except we need to implement escape characters
							term = "'" + term + "'";
						}
					
						expr.termList.add(findOrCreateRule(term));
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
		
		init();
	}
				
	// Emitters fire before their subterm's emitters.
	abstract class Emitter
	{
		String text;
		int offset, length;

		abstract void run();
	}
	void install(String rule, Emitter emitter)
	{
		findOrCreateRule(rule).emitterList.add(emitter);
	}
				
	// parse a string with the first rule, which will fire emitters if successful.
	Rule.Result parse(String inStr)
	{
		char[] str = inStr.toCharArray();
		Rule.Result result = rules.get(0).parse(str, 0, rules.get(0).title);
		
		if (result == null)
		{
			System.out.println("************************* Failed to parse " + inStr + " ************************************");
			return null;
		}

		System.out.println("************************* Success parsing " + inStr + " ************************************");
		result.runEmitters(str);
		//finish();
		
		return result;
	}
}