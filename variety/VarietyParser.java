import java.util.*;
import java.math.*;

public class VarietyParser
{
	public static void main(String arg[])
	{
		VarietyParser vp = new VarietyParser();
		String equations = "10.0 abc ( 10,10.0,1e1, (a,b,c) ) / 3a * -b + 40b*x^2 = a * b * c";
		
		System.out.println("\"" + equations + "\":");

		try
		{
			vp.inputEquation(equations);
		}
		catch (ParseError p)
		{
			System.out.println(p);
		}
		
		//Basis b = new Basis(new Basis.LexOrdering(), ip);
		//b.quickPrint();
	}

	static final char	OPENPAREN = '(', CLOSEPAREN = ')', 
						OPENBRACKET = '[', CLOSEBRACKET = ']',
						OPENBRACE = '{', CLOSEBRACE = '}',
						UNDERSCORE = '_', EXPONENT = '^',
						NEGATE = '-', POSATE = '+', DOT = '.',
						ADDITION = '+', SUBTRACTION = '-', MULTIPLICATION = '*', DIVISION = '/',
						NOT = '!', EQUAL = '=', LESS = '<', MORE = '>',
						COMMA = ',', NEWLINE = '\n', SPACE = ' ', TAB = '\t';
	
	// string and position					
	char[] str;
	int c;
						
	class ParseError extends Exception
	{
		int where;
		ParseError(String s)
			{ super(s); where = c; }
	}

	Expression inputExpression(String exprText) throws ParseError
	{
		System.out.println("\"" + exprText + "\"");
		
		str = exprText.toCharArray();
		c = 0;

		Expression expression = getExpression();
		
		if (expression == null)
			throw new ParseError("missing expr");

		getWhitespace();
		
		if (c < str.length)
		 	throw new ParseError("extra characters after equation: " + str[c]);
		 
		return expression;
	}
	
	Token[] inputOrdering(String orderText) throws ParseError
	{
		// given values separated by spaces, add temp variables to the ideal if needed
		ArrayList<Token> outTokens = new ArrayList<Token>();
		
		str = orderText.toCharArray();
		c = 0;
		
		while (true)
		{
			Token token = getValue();
			
			if (token == null && c < str.length)
				throw new ParseError("extra characters after ordering: " + str[c]);
			if (token == null)
				break;
			
			outTokens.add(token);
			//System.out.println(token.describe());
			
			getWhitespace();
			if (c < str.length && str[c] == COMMA)
			{
				c++;
			}
		}
		
		return outTokens.toArray(new Token[0]);
	}
	
	static final int IS_NOTHING = 0, IS_EQUAL = 1, IS_NOT_EQUAL = 2, IS_LESS = 3, IS_NOT_LESS = 4, IS_MORE = 5, IS_NOT_MORE = 6;
	int getRelation() throws ParseError
	{
		if (c < str.length-1 && ((str[c] == LESS && str[c+1] == EQUAL) || (str[c] == EQUAL && str[c+1] == LESS)))
			{ c += 2; return IS_NOT_MORE; }
		if (c < str.length-1 && ((str[c] == MORE && str[c+1] == EQUAL) || (str[c] == EQUAL && str[c+1] == MORE)))
			{ c += 2; return IS_NOT_LESS; }
		if (c < str.length-1 && str[c] == EQUAL && str[c+1] == EQUAL)
			{ c += 2; return IS_EQUAL; }
		if (c < str.length-1 && str[c] == NOT && str[c+1] == EQUAL)
			{ c += 2; return IS_NOT_EQUAL; }

		if (c < str.length && str[c] == LESS)
			{ c++; return IS_LESS; }
		if (c < str.length && str[c] == MORE)
			{ c++; return IS_MORE; }
		if (c < str.length && str[c] == EQUAL)
			{ c++; return IS_EQUAL; }
		
		return IS_NOTHING;
	}
	
	// chaining inequalities is weird sometimes. 3 > A < 5: 3 is more than A and A is less than 5? The parser just reports.
	class EquationItem
	{
		Expression expression;
		int relation;	// relation placed after this expression
	}
	List<EquationItem> inputEquation(String equations) throws ParseError
	{
		str = equations.toCharArray();
		c = 0;
		
		getWhitespace();
		if (c == str.length)
			return null;
			
		ArrayList<EquationItem> items = new ArrayList<EquationItem>();
		
		boolean needExpression = false;
		
		while (true)
		{
			EquationItem item = new EquationItem();
			item.expression = getExpression();
			
			if (item.expression == null && needExpression)
				throw new ParseError("missing expr after relation");
			if (item.expression == null)
				break;
			
			getWhitespace();
			item.relation = getRelation();
			
			items.add(item);
			
			if (item.relation != IS_NOTHING)
			{
				getWhitespace();
				needExpression = true;
			}
			else
				break;
		}
		
		if (c < str.length)
		 	throw new ParseError("extra characters after equation");
		 
		if (items.size() < 2)
			throw new ParseError("incomplete equation");
				
		/*String out = "expression: ";
		for (int i = 0; i < expressions.size(); i++)
			out += ((i == 0) ? "" : " = ") + expressions.get(i).describe();
		System.out.println(out);*/
		
		return items;
	}

	// whitespace is spaces and tabs.
	
	// there are three types of values.
	// a constant number such as 10, 4.4, or 8.125e-6
	// a literal, such as x, dan123, or I_LIKE_CHEESE.Q[6]
	// a tuple containing other values, like (4), ( 3,y ), or ( (1,2,3), e, (4,5,6) ). Zero arity is allowed.
	
	// a term 
	abstract class Token
	{
		abstract String describe();
	}
	class Constant extends Token
	{
		String describe()
			{ return value.toString() + ((mantissa == 0) ? "" : ("e" + mantissa)); }
		BigInteger value;
		int mantissa;	// 4.3 would be 43e-1, 4.3e3 would be 43e2
	}
	class Literal extends Token
	{
		String describe()
			{ return title; }
		String title;
	}
	class Tuple extends Token
	{
		String describe()
		{
			String out = "(";
			for (int i = 0; i < contents.size(); i++)
				out += ((i == 0) ? " " : ",") + contents.get(i).describe();
			return out += " )";
		}
		LinkedList<Expression> contents = new LinkedList<Expression>();
	}
	class Term extends Token
	{
		String describe()
		{
			String out = "";
			for (int i = 0; i < elements.size(); i++)
				out += (elements.get(i).denominator ? "/" : (i == 0) ? "" : "*") + elements.get(i).token.describe();
			return out;
		}

		class Element 
		{ 
			Token token; 
			boolean denominator; 
			Element(Token t, boolean d) 
				{ token = t; denominator = d; } 
		}
		LinkedList<Element> elements = new LinkedList<Element>();
	}
	class Expression extends Token
	{
		String describe()
		{
			String out = "";
			for (int i = 0; i < elements.size(); i++)
				out += (elements.get(i).subtracted ? " - " : (i == 0) ? "" : " + ") + elements.get(i).term.describe();
			return out;
		}

		class Element 
		{ 
			Term term; 
			boolean subtracted; 
			Element(Term t, boolean s) 
				{ term = t; subtracted = s; } 
		}
		LinkedList<Element> elements = new LinkedList<Element>();
	}
	class Unary extends Token
	{
		String describe()
		{
			return (negate ? "-" : "+") + token.describe();
		}

		boolean negate;
		Token token;
	}
	class Exponent extends Token
	{
		String describe()
		{
			return token.describe() + "^" + power;
		}

		Token token;
		int power;
	}
	
	Expression getExpression() throws ParseError
	{
		Expression expression = new Expression();
		boolean subtracted = false;
		
		getWhitespace();
		
		while (c < str.length)
		{
			Term term = getTerm();
			
			if (term == null)
				break;
			expression.elements.add(expression.new Element(term, subtracted));

			getWhitespace();
			
			if (c < str.length && (str[c] == ADDITION || str[c] == SUBTRACTION))
			{
				subtracted = (str[c] == SUBTRACTION);
				c++;
				getWhitespace();
			}
			else
				break;
		}
		
		if (expression.elements.size() == 0)
			return null;
			
		return expression;
		
	}
	
	Term getTerm() throws ParseError
	{
		Term term = new Term();
		boolean denominator = false, needValue = false;

		getWhitespace();
		
		while (c < str.length)
		{
			Token value = getValue();
			
			if (value == null && needValue)
				throw new ParseError("No value after mult/div");
			
			if (value == null)
				break;
			term.elements.add(term.new Element(value, denominator));

			needValue = false;
			getWhitespace();
			
			if (c < str.length && (str[c] == MULTIPLICATION || str[c] == DIVISION))
			{
				denominator = (str[c] == DIVISION);
				needValue = true;
				c++;
				getWhitespace();
			}
			else if (c < str.length && (str[c] == ADDITION || str[c] == SUBTRACTION))
				break;
		}
		
		if (term.elements.size() == 0)
			return null;
			
		return term;
	}
	
	Token getValue() throws ParseError
	{
		getWhitespace();
				
		if (c == str.length)
			return null;

		if (str[c] == NEGATE || str[c] == POSATE)
		{
			Unary unary = new Unary();
			unary.negate = (str[c] == NEGATE);
			
			c++;
			getWhitespace();
			unary.token = getValue();
			if (unary.token == null)
				throw new ParseError("no token after unary");
			
			return unary;
		}
		
		Token token = null;
		
		if (Character.isDigit(str[c])) 
		{
			String digits = "";
			int mantissa = 0;
			
			while (c < str.length && Character.isDigit(str[c]))
			{
				digits += str[c];
				c++;
			}
			
			if (c < str.length && str[c] == DOT)
			{
				c++; 
				while (c < str.length && Character.isDigit(str[c]))
				{
					digits += str[c];
					c++;
					mantissa--;
				}
			}
			
			if (c < str.length && (str[c] == 'e' || str[c] == 'E') && c+1 < str.length)
			{
				boolean neg = (str[c] == NEGATE), pos = (str[c] == POSATE);
				if (Character.isDigit(str[c+1]) || ((neg || pos) && c+2 < str.length && Character.isDigit(str[c+2])))
				{
					c += (neg ? 2 : 1);
						
					int thisMant = 0;
					do
					{ 
						thisMant = thisMant*10 + (str[c]-'0'); 
						c++; 
					}
					while (c < str.length && Character.isDigit(str[c]));
					
					mantissa += thisMant;
				}
			}
			
			Constant constant = new Constant();
			constant.value = new BigInteger(digits);
			constant.mantissa = mantissa;
			token = constant;
		}
		else if (Character.isLetter(str[c])) 
		{
			int startC = c;
			do
			{ 
				c++; 
			}
			while (c < str.length 
				&& (Character.isLetterOrDigit(str[c]) || str[c] == UNDERSCORE || str[c] == DOT || str[c] == OPENBRACKET || str[c] == CLOSEBRACKET));
			
			Literal literal = new Literal();
			literal.title = new String(str, startC, c - startC);
			
			token = literal;
		}
		else if (str[c] == OPENPAREN)
		{
			Tuple tuple = new Tuple();
			token = tuple;
			
			c++;
			getWhitespace();
			
			if (c < str.length && str[c] == CLOSEPAREN)
			{
				c++;
			}
			else while (true)
			{
				Expression content = getExpression();
				if (content == null)
					throw new ParseError("Expected value inside tuple");
				
				tuple.contents.add(content);
				
				getWhitespace();
				
				if (c < str.length && str[c] == CLOSEPAREN)
				{
					c++;
					break;
				}
				else if (c < str.length && str[c] == COMMA)
				{
					c++;
					getWhitespace();
				}
			}
		}
		else
		{
			return null;
		}
		
		// look for trailing exponents.
		getWhitespace();
		
		while (c < str.length && str[c] == EXPONENT) 
		{
			c++;
			getWhitespace();
			
			Exponent exponent = new Exponent();
			exponent.token = token;
			exponent.power = 0;
			token = exponent;
			
			boolean negative = (c < str.length && str[c] == NEGATE);
			if (negative)
				c++;
				
			while (c < str.length && Character.isDigit(str[c]))
			{ 
				exponent.power = exponent.power*10 + (str[c] - '0'); 
				c++; 
			}
			
			if (exponent.power == 0)
				throw new ParseError("exponent not followed by power");
			
			if (negative)
				exponent.power *= -1;
				
			getWhitespace();
		}

		return token;
	}

	void getWhitespace() throws ParseError
	{
		//System.out.println(new String(str, c, str.length - c));
		while (c < str.length && (str[c] == SPACE || str[c] == TAB || str[c] == NEWLINE))
			c++;
	}
	
}