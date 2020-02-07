import java.util.*;

/*

 <syntax> ::= <rule> | <rule> <syntax>
 <rule>   ::= <opt-whitespace> "<" <rule-name> ">" <opt-whitespace> "::=" 
                 <opt-whitespace> <expression> <line-end>
 <opt-whitespace> ::= " " <opt-whitespace> | ""  // "" is empty string, i.e. no whitespace
 <expression>     ::= <list> | <list> "|" <expression>
 <line-end>       ::= <opt-whitespace> <EOL> | <line-end> <line-end>
 <list>    ::= <term> | <term> <opt-whitespace> <list>
 <term>    ::= <literal> | "<" <rule-name> ">"
 <literal> ::= '"' <text> '"' | "'" <text> "'" // actually, the original BNF did not use quotes


input	::= ws expr ws eoi;

expr	::= ws powterm [{ws '^' ws powterm}];
powterm	::= ws factor [{ws ('*'|'/') ws factor}];
factor	::= ws term [{ws ('+'|'-') ws term}];
term	::= '(' ws expr ws ')' | '-' ws expr | number;

number	::= {dgt} ['.' {dgt}] [('e'|'E') ['-'] {dgt}];
dgt	::= '0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9';
ws	::= [{' '|'\t'|'\n'|'\r'}];

*/

class BNF
{
	public static BNF createBNFParser(String inputRules)
	{
		/*
			Given a BNF grammar, turn it into a BNF object! Woo hoo! This is a simple recursive descent
			version because to do it in BNF first would get horribly tangled up when I try to change 
			something and I'm old enough not to want to get tangled up. Build EBNF on top of this.
			
			Rules can be a single expression or a choice:
			
			RULE := EXPRESSION
			RULE := EXPRESSION | EXPRESSION | EXPRESSION
			
			Expressions are TERMs which can be a single rule or terminal, or a concatenation of them:
			 
			EXPRESSION := RULE
			EXPRESSION := RULE1 '+' RULE2
		*/
		
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
				if (c+1 < str.length && str[c] == ':' && str[c+1] == '=')
				{
					c += 2;
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
		}
		class RuleHolder
		{
			String ruleName;
			BNF.Term term;
			
			ArrayList<Expr> exprList = new ArrayList<Expr>();
			class Expr
			{
				ArrayList<Term> termList = new ArrayList<Term>();
			}
			class Term
			{
				String ruleName = null;	// one or the other
				String terminal = null;
			}
		}
		
		final ArrayList<RuleHolder> holders = new ArrayList<RuleHolder>();
		
		String[] lines = inputRules.split("\n");
		for (int l = 0; l < lines.length; l++)
		{
			CharFeeder feeder = new CharFeeder();
			feeder.str = lines[l].toCharArray();
			
			System.out.println("parsing " + feeder.getRemainder());
			
			feeder.skipWhitespace();
			
			RuleHolder holder = new RuleHolder();
			holder.ruleName = feeder.getLiteral();
			if (holder.ruleName == null)
				throw new Error("line didn't start with literal at " + feeder.getRemainder());
			feeder.skipWhitespace();
			if (feeder.getAssignSymbol() == false)
				throw new Error("rule wasn't followed by := at " + feeder.getRemainder());
			feeder.skipWhitespace();

			consumeExpr: while (true)
			{
				RuleHolder.Expr expr = holder.new Expr();
				holder.exprList.add(expr);
				
				while (true)
				{
					RuleHolder.Term term = holder.new Term();
					expr.termList.add(term);
					
					// either a terminal or a rule name
					term.ruleName = feeder.getLiteral();
					if (term.ruleName == null)
					{
						term.terminal = feeder.getTerminal();
						if (term.terminal == null)
							throw new Error("Expected a term at " + feeder.getRemainder());
						System.out.println("parsed terminal " + term.terminal);
					}
					else
						System.out.println("parsed rulename " + term.ruleName);
					
					// what's after that?
					feeder.skipWhitespace();
					if (feeder.getOrSymbol())
					{
						feeder.skipWhitespace();
						continue consumeExpr;
					}
					if (feeder.getEOL())
						break consumeExpr;
				}
			}
			
			holders.add(holder);
		}
		
		return new BNF() {
			
			void init()
			{
				for (int i = 0; i < holders.size(); i++)
					getTermForRuleHolder(holders.get(i));
			}
			Object parse(String text)
			{
				char[] array = text.toCharArray();
				
				Term.Result result = getTermForRuleName("S").Parse(array, 0);
				System.out.println(result.describeAsXML(0));
				
				if (result.length < array.length)
					System.out.println("Remainder: " + new String(array, result.length, array.length - result.length));
					
				return result;
			}
			
			Term getTermForRuleHolder(RuleHolder holder)
			{
				// make it now if it does not exist already
				if (holder.term == null)
				{
					if (holder.exprList.size() > 1)
					{
						Branch branch = new Branch(holder.ruleName);
						holder.term = branch;	// now it exists and can be referenced shallowly
						
						for (int i = 0; i < holder.exprList.size(); i++)
							branch.list.add(getTermForRuleHolderExpr(holder.exprList.get(i)));
					}
					else if (holder.exprList.get(0).termList.size() > 1)
					{
						Concat concat = new Concat(holder.ruleName);
						holder.term = concat;	// now it exists and can be referenced shallowly

						for (int i = 0; i < holder.exprList.get(0).termList.size(); i++)
							concat.list.add(getTermForRuleHolderTerm(holder.exprList.get(0).termList.get(i)));
					}
					else if (holder.exprList.get(0).termList.get(0).terminal != null)
					{
						Text text = new Text(holder.ruleName, holder.exprList.get(0).termList.get(0).terminal);
						holder.term = text;
					}
					else
					{
						// this would recurse forver right here and now if there was a cycle of single rule names.
						holder.term = getTermForRuleName(holder.exprList.get(0).termList.get(0).ruleName);
						// it would recurse forever at parse time if one link wasn't a single rule name
					}
				}
				
				return holder.term;
			}
			Term getTermForRuleHolderExpr(RuleHolder.Expr expr)
			{
				if (expr.termList.size() > 1)
				{
					Concat concat = new Concat("");
					for (int i = 0; i < expr.termList.size(); i++)
						concat.list.add(getTermForRuleHolderTerm(expr.termList.get(i)));
					return concat;
				}
				return getTermForRuleHolderTerm(expr.termList.get(0));
			}
			Term getTermForRuleHolderTerm(RuleHolder.Term term)
			{
				if (term.ruleName != null)
					return getTermForRuleName(term.ruleName);
				else
					return new Text(term.terminal, term.terminal);
			}
			
			Term getTermForRuleName(String ruleName)
			{
				for (int i = 0; i < holders.size(); i++)
					if (holders.get(i).ruleName.equals(ruleName))
						return getTermForRuleHolder(holders.get(i));
						
				throw new Error("can't get term for " + ruleName);
			}
		};
	}
	
	public static void main(String arg[])
	{
		/*
		  This is partly inadequate because FN and DL need to have their terms reversed, or S needs an END marker
		
		  S  := '-' FN |
		        FN
		  FN := DL |
		        DL '.' DL
		  DL := D |
		        D DL
		  D  := '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'

		There is also EBNF which has 3 right operators: ? optional, * repeat >=0 times, + repeat >=1 times
		  S := '-'? D+ ('.' D+)?
		  D := '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
		
		What about emitting output as a reverse process? Something like this, which is neat because it could go the other way.
		S -> numerator = 0; denominator = 1; increaseDenominator = 1;
		S.'-' -> denominator = -1;
		S.'.' -> increaseDenominator = 10;
		D -> numerator = numerator*10 + $$; denominator *= increaseDenominator;
		
		JISON uses such as e '+' e {$$ = $1 + $3;} | e '-' e {$$ = $1 - $3;} to create a JS function directly. Interesting and useful and
		worth exploring for AST rewriting but for createBNFParser we need to add code directly in Java.
		
		S := '-'? D+ ('.' D+)?
		D := '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
		
		*/
		//testDigitsParser();
		testCreator();
	}
	
	static void testCreator()
	{	
		String starter = "S := '-' FN | FN \n"
						+ "FN := DL '.' DL | DL \n"
						+ "DL := D DL | D \n"
						+ "D := '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ";
		
		BNF myBNF = createBNFParser(starter);
		myBNF.parse("-123.45");
		
		/*BNF.Emitter myEmit = myBNF.new Emitter() {
			int numerator, denominator, increaser;
			
			void init()
			{
				install("S", new Rule() { void go() { numerator = 0; denominator = 1; increaser = 1; } });
				install("'-'", new Rule() { void go() { denominator = -1; } });
				install("'.'", new Rule() { void go() { increaser = 10 } });
				install("D", new Rule() { void go() { numerator *= 10; numerator += ('0' - text().charAt(0)); denominator *= increaser; } });
			}
			Object result()
			{
				return new BigRational(numerator, denominator);
			}
		};
		BigRational br = (BigRational)myBNF.parse("S", "-123.45", myEmit);*/
	}
	
	static void testDigitsParser()
	{
		BNF digitsParser = new BNF() {
			
			Branch S;
			int numerator, denominator, increaseDenominator;
			
			void init()
			{
				S = new Branch("S") {
					void Execute(int offset, int length)
					{
						numerator = 0;
						denominator = 1;
						increaseDenominator = 1;
					}
				};

				Text HYPHEN = new Text("HYPHEN", "-") {
						void Execute(int offset, int length)
						{
							denominator = -1;
						}
					};

				Text[] DIGIT = new Text[10];
				for (char i = 0; i <= 9; i++)
				{
					final int value = i;
					DIGIT[i] = new Text("DIGIT", "" + (char)((int)'0'+i)) {
						void Execute(int offset, int length)
						{
							numerator = numerator*10 + value;
							denominator *= increaseDenominator;
						}
					};
				}
				
				Text DOT = new Text("DOT", ".") {
						void Execute(int offset, int length)
						{
							increaseDenominator = 10;
						}
					};


				Branch D = new Branch("D");
				for (char i = 0; i <= 9; i++)
					D.list.add(DIGIT[i]);
				
				Branch DL = new Branch("DL");
				Concat D_DL = new Concat("D_DL");
				D_DL.list.add(D);
				D_DL.list.add(DL);
				DL.list.add(D_DL);
				DL.list.add(D);
				
				Branch FN = new Branch("FN");
				Concat DL_DOT_DL = new Concat("DL_DOT_DL");
				DL_DOT_DL.list.add(DL);
				DL_DOT_DL.list.add(DOT);
				DL_DOT_DL.list.add(DL);
				FN.list.add(DL_DOT_DL);
				FN.list.add(DL);
				
				Concat HYPHEN_FN = new Concat("HYPHEN_FN");
				HYPHEN_FN.list.add(HYPHEN);
				HYPHEN_FN.list.add(FN);
				S.list.add(HYPHEN_FN);
				S.list.add(FN);
			}
			
			Object parse(String text)
			{
				Term.Result result;
				
				result = S.Parse(text.toCharArray(), 0);
				System.out.println(result.describeAsXML(0));
				result.execute();
				System.out.println("" + numerator + "/" + denominator);
				
				return result;
			}
		};
		
		digitsParser.parse("123");
		digitsParser.parse("-123.45");
		
		// the result will be HYPHEN_FN { HYPHEN, FN { DL { 1 DL { 2 DL { 3 } } } } DOT DL { 4 DL { 5 } } } }
		/*Term.Result result;
		
		result = S.Parse("123".toCharArray(), 0);
		System.out.println(result.describe(0));
		result.execute();
		System.out.println("" + numerator + "/" + denominator);
		
		result = S.Parse("-123.45".toCharArray(), 0);
		System.out.println(result.describe(0));
		result.execute();
		System.out.println("" + numerator + "/" + denominator);*/
		
		// ok. successfully parsing a 1 should just return the term, location, and length.
		// parsing -1 as a HYPHEN_DIGIT should return an array with results from the hyphen and the digit.
		// so I guess you return a result[] and each result has a result[], term, location, length.
		
		// Now output. Apparently the basic idea is attach what to do when successfully parsed.
		/*
		
		This does all the same work exactly but no syntax checking
		while (c = anotherchar())
		{
			if (c == '.')
				increaseDenom = 10;
			else if (c == '-')
				denom = -1;
			else if ('0' <= c <= '9')
			{
				num = num*10 + c;
				denom *= increaseDenom;
			}
			else
				return false;
		}
		*/
		
	}
	
	BNF()
	{
		init();
	}
	void init()
	{
	}
	Object parse(String text)
	{
		return null;
	} 
	
	abstract class Term
	{
		abstract Result Parse(char[] str, int c);

		String title;
		Term(String t)
		{
			title = t;
		}
		void Execute(int offset, int length)
		{
		}
		
		class Result
		{
			final Term term;
			final Result[] sub;	// can be null
			final int offset, length;
			
			Result(Result[] s, int o, int l)
				{ term = Term.this; sub = s; offset = 0; length = l; }
				
			String describeAsFunctions()
			{
				String output = term.title;
				
				if (sub != null)
				{
					output += "( ";
					for (int i = 0; i < sub.length; i++)
						output += sub[i].describeAsFunctions() + " ";
					output += ")";
				}
				
				return output;
			}
			
			String describeAsSpans()
			{
				String output = "";
				output += "<span class=\"" + term.title + "\">";
				
				if (term instanceof Text)
				{
					output += new String(((Text)term).text);
				}
				else if (sub == null)
				{
					output += "</" + term.title + ">";
				}
				else if (sub != null)
				{
					for (int i = 0; i < sub.length; i++)
						output += sub[i].describeAsSpans();
				}
				
				output += "</span>";
				return output;
			}

			String describeAsXML(int indent)
			{
				String output = "";
				output += ("               ").substring(0,indent);  
				output += "<" + term.title;
				
				if (term instanceof Text)
				{
					output += ">";
					output += new String(((Text)term).text);
				}
				else
				{
					if (term instanceof Branch)
					{
						output += " : Branch { ";
						for (int i = 0; i < ((Branch)term).list.size(); i++)
							output += ((Branch)term).list.get(i).title + " ";
						output += "}>";
					}
					else if (term instanceof Concat)
					{
						output += " : Concat { ";
						for (int i = 0; i < ((Concat)term).list.size(); i++)
							output += ((Concat)term).list.get(i).title + " ";
						output += "}>";
					}

					if (sub != null)
					{
						output += "\n";
						for (int i = 0; i < sub.length; i++)
							output += sub[i].describeAsXML(indent+1);
						output += ("               ").substring(0,indent);
					}
				}
				
				output += "</" + term.title + ">";
				output += "\n";
				return output;
			}
			
			void execute()
			{
				term.Execute(offset, length);
				
				if (sub != null)
					for (int i = 0; i < sub.length; i++)
						sub[i].execute();
			}
		}

	}
	class Text extends Term
	{
		char[] text;
		Text(String t, String inText)
		{ 
			super(t);
			text = inText.toCharArray(); 
		}
		Result Parse(char[] str, int c)
		{
			//System.out.println("trying to parse " + title);
			
			if (str.length - c < text.length)
				return null;
				
			for (int i = 0; i < text.length; i++)
				if (str[c + i] != text[i])
					return null;
			
			return new Result(null, c, text.length);
		}
	}
	class Branch extends Term
	{
		List<Term> list = new ArrayList<Term>();
		
		Branch(String t)
		{
			super(t);
		}
		Result Parse(char[] str, int c)
		{
			//System.out.println("trying to parse " + title);
		
			for (int i = 0; i < list.size(); i++)
			{
				Result result = list.get(i).Parse(str, c);
				if (result != null)
					return new Result(new Result[] { result }, result.offset, result.length);
			}
			return null;
		}
	}
	class Concat extends Term
	{
		List<Term> list = new ArrayList<Term>();
		
		Concat(String t)
		{
			super(t);
		}
		Result Parse(char[] str, int c)
		{
			//System.out.println("trying to parse " + title);
		
			int depth = 0;
			Result[] results = new Result[list.size()];
			
			for (int i = 0; i < list.size(); i++)
			{
				results[i] = list.get(i).Parse(str, c + depth);
				if (results[i] == null)
					return null;
				depth += results[i].length;
			}
			
			return new Result(results, c, depth);
		}
	}
}