import java.util.*;
import java.math.*;
import java.io.*;

// we're going to put a friendlier face on it. we're going to work towards practical concerns this time. we're going to try some thoughts.
// this is not intended to be a speed demon version, just avoiding obvious inefficiencies and favoring clarity.

// the unresolved issue from the last attempt is typing. Grobner works over any field but we should be able to shoehorn mixed fields in if
// we're careful about the commutative property. Say x = (1,2,3). The triple gets parsed, the whole poly is painted as triples, you can't
// go back and assign x to a twople. Writing 10x is fine, that's defined. Writing a*x = b is unclear yet. We don't know what a is, and we
// wouldn't know how to do the multiplication if we did, but we know that b/a produces a triple. Okay, here are the rules:
// Typing is levels of strictness: unknown arity, known arity with unknown subarity, known arity with known arity. Known arity derives from
// the syntax of paren-commas and constants always have single arity. Multiplication of single arity by multiple arity is elementwise, and
// addition of like arities is elementwise. There is no addition of unlike arrities allowed, so all terms in a polynomial have equal type.
// Multiplication of multiple arities is going to be defined later, and noncommutative properties will be investigated at that time. 

/*

Suppose Variety held the user submitted ideal, and StandardBasis would copy its polynomials before its calculation for its internal use. The nice
thing is that once we had a basis, if the user changed the ideal, we could synthetic divide each member, and find out which basis polys were no
longer used, and which ideal members were not in the old ideal and recalculate. 

Okay, let's update this and make it better. First, Variable should be static 


*/

public final class Variety 
{
	public static void print(String o)
	{
		System.out.println(o);
	} 
	public static void main(String arg[])
	{
		
		
		/*divisionTest("48z^2 L^2 + 122z^2 L + 49z^2 + 20L^3 - 45L", "1105z^2 + 960L^5 + 1928L^4 - 2076L^3 - 4338L^2 - 189L");
		//if (true) return;
		divisionTest("x y^2 + 1", "x y + 1 = y + 1");
		divisionTest("x y^2 - 1", "-x y - 1 = -y - 1");
		divisionTest("-x y^2 - 1", "x y + 1 = y + 1");
		divisionTest("x y^2 + 1", "y + 1 = x y + 1");
		divisionTest("x x y + x y y + y y", "x y - 1 = y y - 1");
		if (true) return;*/
		
		String calcReport = "";
		String description = "";
		
		for (int i = 1; i <= 0; i++)
		{
			// declare a variety
			Variety v = new Variety();
			
			// declare two variables
			Variable L = v.new Variable("L"), x = v.new Variable("x"), y = v.new Variable("y"), z = v.new Variable("z");
			
			// add a relation
			Monomial justY, justX, just10;
			// Ax = b, A = ( (1,2,3), (4,5,6) )
			
			//Equation e = v.new Equation("x^3 - 2 x y = x^2 y - 2 y^2 + x = 0");
			Equation e = v.new Equation("2 x y - 2 z - 2 z L = x^2 + (y)^2 + z^2 - 1 = 3x^2 + 2 y z - 2 x L = 2 x z - 2 y L = 0");
			//Equation e = v.new Equation("z = (x+y)^2");
			
			// we need something a little better. note this doesn't resort the poly
			//v.stuffTemporaries();
			ArrayList<Variable> varList = v.getVariableOrdering();
			int temporaryCount = 0;
			for (int t = 0; t < varList.size(); t++)
			{
				Variable var = varList.get(t);
				if (var.temporary)
				{
					varList.remove(t);
					varList.add(temporaryCount++, var);
				}
			}
			v.setVariableOrdering(varList);
			
			// print the current variety
			v.setOrdering(v.new LexOrdering());
			
			// try the basis
			StandardBasis b = v.new StandardBasis();
			b.calculate();
			calcReport += b.calcReport;
			
			description = v.describe();
			/*if (i == 1)
			try {
				System.gc();
				Thread.currentThread().sleep(1000);
			}
			catch (java.lang.InterruptedException ie) { }*/
		}
		
		print(calcReport + description);
		
		if (true)
		{
			Variety v = new Variety();
			v.setOrdering(v.new LexOrdering());
			v.new Equation("x^2 + 7x + 6 = 0");
			v.new Equation("x^2 - 5x - 6 = 0");
			
			print("alpha: " + v.polynomials.get(0).describe() + " beta: " + v.polynomials.get(1).describe());
			Polynomial gcd = v.gcd(v.polynomials.get(0), v.polynomials.get(1));
			print("gcd: " + gcd.describe());
			
			for (int i = 0; i <= 1; i++)
			{
				Polynomial dividend = v.polynomials.get(i);
				
				String out = dividend.describe() + " = (" + gcd.describe() + ")";

				ArrayList<Polynomial> divisors = new ArrayList<Polynomial>(), factorMap = new ArrayList<Polynomial>();
				divisors.add(gcd);
				factorMap.add(null);

				v.syntheticDivision(dividend, divisors, factorMap, 0);
				
				print(out + "(" + factorMap.get(0).describe() + ") + " + dividend.describe());
			}
		}
		
		if (true)
		{
			Variety v = new Variety();
			v.setOrdering(v.new LexOrdering());
			v.new Equation("x^3 + 2x y z - z^2 = 0");
			
			Polynomial[] del = v.gradient(v.polynomials.get(0));
			for (int i = 0; i < v.variableCount(); i++)
				print("∂(" + v.polynomials.get(0).describe() + ")/∂" + v.variableArray.get(i).describe(1) + " = " + del[i].describe());
		}
	}
	public static void divisionTest(String dividendText, String divisorsText)
	{
		Variety v = new Variety();

		Variable x = v.new Variable("x"), y = v.new Variable("y"), z = v.new Variable("z"), L = v.new Variable("L");
		v.new Equation(divisorsText + " = 0");

		v.new Equation(dividendText + " = 0");
		Polynomial dividend = v.polynomials.get(v.polynomials.size()-1);

		Variety.Ordering o = v.new GrlexOrdering();
		o.sortPolyList = false;

		print(v.describe());
		v.setOrdering(o);
		print(v.describe());

		v.polynomials.remove(dividend);

		print(dividend.describe());
		o.sortPolynomial(dividend);
		print(dividend.describe());

		String out = dividend.describe() + " = ";

		ArrayList<Polynomial> factorMap = new ArrayList<Polynomial>(v.polynomials.size());
		for (int p = 0; p < v.polynomials.size(); p++)
			factorMap.add(null);
		v.syntheticDivision(dividend, v.polynomials, factorMap, 0);
		
		for (int p = 0; p < v.polynomials.size(); p++)
			if (factorMap.get(p) != null)
				out += "(" + factorMap.get(p).describe() + ")[" + v.polynomials.get(p).describe() + "] + ";
		out += dividend.describe();
		
		print(out + "\n");
	}
	
	Variety()
	{
	}
	
	Variety(Variety beta)
	{
		for (int v = 0; v < beta.variableArray.size(); v++)
			new Variable(beta.variableArray.get(v).title, beta.variableArray.get(v).temporary);
		
		ordering = (beta.ordering instanceof LexOrdering) ? new LexOrdering() 
			: (beta.ordering instanceof GrlexOrdering) ? new GrlexOrdering()
			: (beta.ordering instanceof GrevlexOrdering) ? new GrevlexOrdering()
			: (beta.ordering instanceof ElimOrdering) ? new ElimOrdering(((ElimOrdering)beta.ordering).k)
			: null;
		
		// inequalities .variable .polynomial .relation
		for (int p = 0; p < beta.polynomials.size(); p++)
			addPolynomial(new Polynomial(beta.polynomials.get(p)));
			
		for (int i = 0; i < beta.inequalities.size(); i++)
		{
			Inequality ineq = new Inequality();
			ineq.variable = variableArray.get(beta.variableArray.indexOf(beta.inequalities.get(i).variable));
			ineq.polynomial = new Polynomial(beta.inequalities.get(i).polynomial);
			ineq.relation = beta.inequalities.get(i).relation;
			inequalities.add(ineq);
		}
	}
	
	// description style
	boolean describeFancy = false;

	// ordered as put in
	ArrayList<Variable> variableArray = new ArrayList<Variable>();
	
	Variable findVariable(String name)
	{
		for (int v = 0; v < variableArray.size(); v++)
			if (variableArray.get(v).title.equalsIgnoreCase(name))
				return variableArray.get(v);
		return null;
	}
	int variableCount()
	{
		return variableArray.size();
	}
	
	// not the perfect API yet, see Roda 2004
	ArrayList<Variable> getVariableOrdering()
	{
		ArrayList<Variable> newOrder = new ArrayList<Variable>();
		for (int v = 0; v < variableCount(); v++)
			newOrder.add(variableArray.get(v));
		return newOrder;
	}
	void setVariableOrdering(ArrayList<Variable> newOrder)
	{
		int map[] = new int[newOrder.size()];
		for (int v = 0; v < newOrder.size(); v++)
			map[v] = variableArray.indexOf(newOrder.get(v));

		// okay. what happens to the variables that aren't in the ideal? delete those polys
		
		poly: for (int p = 0; p < polynomials.size(); p++)
			for (int m = 0; m < polynomials.get(p).monomials.size(); m++)
			{
				Monomial mono = polynomials.get(p).monomials.get(m);
				mono.ensureSpan();
				int[] oldPowers = mono.powers;
				int oldTotalPower = mono.totalPower;
				
				mono.powers = new int[map.length];
				mono.totalPower = 0;
				for (int v = 0; v < map.length; v++)
				{
					mono.powers[v] = oldPowers[map[v]];
					mono.totalPower += oldPowers[map[v]];
				}
				
				if (mono.totalPower != oldTotalPower)
				{
					polynomials.remove(p);
					p--;
					continue poly;
				}
			}
		
		inequality: for (int i = 0; i < inequalities.size(); i++)
			for (int m = 0; m < inequalities.get(i).polynomial.monomials.size(); m++)
			{
				Monomial mono = inequalities.get(i).polynomial.monomials.get(m);
				mono.ensureSpan();
				int[] oldPowers = mono.powers;
				int oldTotalPower = mono.totalPower;
				
				mono.powers = new int[map.length];
				mono.totalPower = 0;
				for (int v = 0; v < map.length; v++)
				{
					mono.powers[v] = oldPowers[map[v]];
					mono.totalPower += oldPowers[map[v]];
				}
				
				if (mono.totalPower != oldTotalPower)
				{
					inequalities.remove(i);
					i--;
					continue inequality;
				}
			}
		
		variableArray.clear();
		variableArray.addAll(newOrder);
	}
		
	class Variable
	{
		String title;
		boolean temporary;
		
		Variable()
			{ variableArray.add(this); title = "$" + index(); temporary = true; }
		Variable(String t)
			{ variableArray.add(this); title = t; temporary = false; }
		Variable(String t, boolean temp)
			{ variableArray.add(this); title = t; temporary = temp; }
			
		int index()
		{
			return variableArray.indexOf(this);
		}
	    String describe(int power)
	    {
			if (power == 1)
				return title;
				
			String output = "";

			if (describeFancy)
			{
		        // 0123456789-+
		        String powerUnicode = "\u2070\u00B9\u00B2\u00B3\u2074\u2075\u2076\u2077\u2078\u2079\u207B\u207A";
		        String input = "" + power;
		        
		        for (int u = 0; u < input.length(); u++)
		            output += powerUnicode.charAt(input.charAt(u) - '0');
		        return title + output;
			}
			
			output += title;
			for (int i = 2; i <= power; i++)
				output += "*" + title;
			return output;
	    }
	}
	
	// powers does not have to be as long as the current variableCount
	class Monomial
	{
		BigInteger coefficient = BigInteger.ONE;
		int totalPower = 0;
		int[] powers = new int[variableCount()];
		
		void ensureSpan()
		{
			if (powers.length == variableCount())
				return;
				
			int[] oldPowers = powers;
			powers = new int[variableCount()];
			System.arraycopy(oldPowers, 0, powers, 0, oldPowers.length);
		}
		void consume(Monomial beta)
		{
			coefficient = coefficient.multiply(beta.coefficient);
			
			ensureSpan();
			for (int v = 0; v < beta.powers.length; v++)
				powers[v] += beta.powers[v];
			totalPower += beta.totalPower;
		}
		boolean equalPowers(Monomial beta)
		{
			if (totalPower != beta.totalPower)
				return false;
			
			ensureSpan();
			beta.ensureSpan();
			for (int v = 0; v < variableCount(); v++)
				if (powers[v] != beta.powers[v])
					return false;
					
			return true;
		}
		
		String describe(boolean absolute)
		{
			String str = "";
			boolean addedVar = false;
			
			if (absolute == false && coefficient.signum() == -1)
				str += "-";
			if (coefficient.abs().equals(BigInteger.ONE) == false || totalPower == 0)
			{
				str += coefficient.abs().toString();
				if (describeFancy == false)
					addedVar = true;
			}

			for (int v = 0; v < powers.length; v++)
				if (powers[v] != 0)
				{
					str += (addedVar ? "*" : "") + variableArray.get(v).describe(powers[v]);
					addedVar = true;
				}
			return str;
		}
	}
	
	// the caller is responsible for maintaining the proper monomial ordering
	class Polynomial
	{
		final ArrayList<Monomial> monomials = new ArrayList<Monomial>();

		Polynomial()
		{
		}
		Polynomial(Polynomial beta)
		{
			for (int m = 0; m < beta.monomials.size(); m++)
			{
				Monomial mono = new Monomial();
				mono.consume(beta.monomials.get(m));
				consumeMonomial(mono);
			}
		}
		
		String describe()
		{
			String str = "";
			
			if (monomials.size() == 0)
				return "0";
				
			for (int m = 0; m < monomials.size(); m++)
			{
				Monomial mono = monomials.get(m);
				if (m != 0)
					str += (mono.coefficient.signum() == -1) ? " - " : " + ";
				str += mono.describe(m != 0);
			}
			
			return str;
		}
		
		int totalDegree()
		{
			int degree = 0;
			for (int m = 0; m < monomials.size(); m++)
				degree += monomials.get(m).totalPower;
			return degree;
		}

		void multiply(BigInteger multiplier)
		{
			for (int m = 0; m < monomials.size(); m++)
				monomials.get(m).coefficient = monomials.get(m).coefficient.multiply(multiplier);
		}
		
		void reduceCoefficients()
		{
			// divide all terms by the GCD and make the leading term positive.
			if (monomials.size() == 0)
				return;
				
			BigInteger gcd = monomials.get(0).coefficient.abs();
			for (int m = 1; m < monomials.size(); m++)
				gcd = gcd.gcd(monomials.get(m).coefficient);
			if (monomials.get(0).coefficient.signum() == -1) 
				gcd = gcd.negate();
			if (gcd.equals(BigInteger.ONE) == false)
				for (int m = 0; m < monomials.size(); m++)
					monomials.get(m).coefficient = monomials.get(m).coefficient.divide(gcd);
		}

		// this just tacks the new mono onto the end.
		void consumeMonomial(Monomial mono)
		{
			if (mono.totalPower == 0 && mono.coefficient.signum() == 0)
				return;
				
			for (int m = 0; m < monomials.size(); m++)
				if (monomials.get(m).equalPowers(mono))
				{
					BigInteger sum = monomials.get(m).coefficient.add(mono.coefficient);
					if (sum.signum() == 0)
						monomials.remove(m);
					else
						monomials.get(m).coefficient = sum;
					return;
				}
				
			monomials.add(mono);
		}
		
		double evaluate(double[] map)
		{
			double value = 0;
			for (int m = 0; m < monomials.size(); m++)
				value += evaluateMonomial(monomials.get(m), map);
			return value;
		}
		double evaluateMonomial(Monomial mono, double[] map)
		{
			double value = mono.coefficient.doubleValue();
			for (int v = 0; v < mono.powers.length; v++)
				if (mono.powers[v] == 1)
					value *= map[v];
				else if (mono.powers[v] > 1)
					value *= Math.pow(map[v], mono.powers[v]);
			return value;
		}

	}
	
	// ranges
	class Inequality
	{
		Variable variable;	// associated var
		Polynomial polynomial;	// evaluate this
		int relation;		// what relationship to zero?
	}
	List<Inequality> inequalities = new ArrayList<Inequality>();
			
	// ordered as put in
	List<Equation> equationList = new ArrayList<Equation>();

	class Equation
	{
		final String equation;
		VarietyParser.ParseError parseError = null;
		
		Equation(String txt)
		{
			equation = txt; 
			equationList.add(this);
			
			VarietyParser vp = new VarietyParser();
			try 
			{
				List<VarietyParser.EquationItem> items = vp.inputEquation(equation);
				
				if (items == null)	// empty string
					return;
				
				// now turn this into a polynomial, thinking about typing. when do we do vector math?
				// say your first statement is A = 2B, then you say A.x = B.y^2, how to turn into an ideal?
				// it would probably be much better to require strict typing on the Variety level, then 
				// implement some smart thinking in the parser.
				// (x, y) = (sin t, cos t) = (u, u^2) would be solvable since now u^2 + u^4 - 1 = 0
				// f(t) := (sin t, cos t), f(t0) = f(t1), t0 != t1
				
				/*String out = "";
				for (int i = 0; i < expressions.size(); i++)
					out += ((i == 0) ? "" : " = ") + expressions.get(i).describe();
				print("Expression: " + out);*/
				
				// let's turn it into polynomials
				Polynomial[] polyList = new Polynomial[items.size()];
				
				List<Monomial> monoList = new ArrayList<Monomial>();
				Monomial masterDenominator = new Monomial();
								
				for (int i = 0; i < items.size(); i++)
					polyList[i] = consumeVarietyParserExpression(items.get(i).expression, masterDenominator, monoList);

				// previously they were always all equal and we scanned for an 'anchor' polynomial. now we deal with couplings
				couplings: for (int i = 0; i < items.size() - 1; i++)
				{
					// subtract all terms in the next poly from this poly
					for (int m = 0; m < polyList[i+1].monomials.size(); m++)
					{
						Monomial anchorMono = new Monomial();
						anchorMono.consume(polyList[i+1].monomials.get(m));
						anchorMono.coefficient = anchorMono.coefficient.negate();
						polyList[i].consumeMonomial(anchorMono);
					}
					
					if (items.get(i).relation == VarietyParser.IS_EQUAL)
					{
						if (ordering != null)
							ordering.sortPolynomial(polyList[i]);
						if (polyList[i].monomials.size() != 0)
							addPolynomial(polyList[i]);
					}
					else if (items.get(i).relation == VarietyParser.IS_NOT_EQUAL)
					{
						// multiply everything by a new variable whose denominator being 0 will indicate inequality, then add 1
						Variable ineqVar = new Variable("ineq{" + polyList[i].describe() + "}");
						for (int m = 0; m < polyList[i].monomials.size(); m++)
						{
							polyList[i].monomials.get(m).ensureSpan();
							polyList[i].monomials.get(m).powers[ineqVar.index()]++;
							polyList[i].monomials.get(m).totalPower++;
						}
						polyList[i].consumeMonomial(new Monomial());

						//if (ordering != null)
						//	ordering.sortPolynomial(polyList[i]);
						if (polyList[i].monomials.size() != 0)
							addPolynomial(polyList[i]);
					}
					else
					{
						// here's an equation that needs to be compared to zero...how do we integrate this? say it's easy, like y^2 - 3, we just make a formula out of it
						// and evaluate while drawing the graph. Say it's hard, like xy < 1, we'd have to turn it into y = 1/x and use areas to draw it, complicated, so
						// for now let's enforce that you can only make a range for a single variable, then the grapher can deal with it.
						int ineqV = -1;
						for (int m = 0; m < polyList[i].monomials.size(); m++)
						{
							Monomial mono = polyList[i].monomials.get(m);
							for (int v = 0; v < mono.powers.length; v++)
								if (mono.powers[v] > 0)
								{
									if (ineqV == -1)
										ineqV = v;
									else if (ineqV != v)
									{
										System.out.println("two variables in inequality");
										continue couplings;
									}
								}
						}
						if (ineqV == -1)
						{
							System.out.println("no variables in inequality");
							continue couplings;
						}
						
						// this isn't a normal formula, we don't want a particular output variable at all.
						Inequality inequality = new Inequality();
						inequality.variable = variableArray.get(ineqV);
						inequality.polynomial = polyList[i];
						inequality.relation = items.get(i).relation;
						inequalities.add(inequality);
					}
				}
			}
			catch (VarietyParser.ParseError e)
			{
				parseError = e;
				//print("" + e + " at " + vp.c + " of '" + txt + "'");
			}
		}
		
		// potentially the most useful thing ever!
		Variable[] retrieveVariables(String orderText)
		{
			ArrayList<Variable> varList = new ArrayList<Variable>();
		
			VarietyParser vp = new VarietyParser();
			try 
			{
				VarietyParser.Token[] tokens = vp.inputOrdering(orderText);
				tokenLoop: for (int t = 0; t < tokens.length; t++)
				{
					Monomial numer = new Monomial(), denom = new Monomial();
					readVarietyParserToken(tokens[t], 1, numer, denom);
					
					//System.out.println("this expr: " + numer.describe(false) + " / " + denom.describe(false));
					
					if (numer.totalPower == 1 && numer.coefficient.equals(BigInteger.ONE) 
						&& denom.totalPower == 0 && denom.coefficient.equals(BigInteger.ONE))
					{
						for (int v = 0; v < variableCount(); v++)
							if (numer.powers[v] != 0)
							{
								varList.add(variableArray.get(v));
								//System.out.println("already a var: " + variableArray.get(v).title);
								continue tokenLoop;
							}
							System.out.println("already a var: ??");
					}
					
					Variable var = new Variable("{" + tokens[t].describe() + "}");
					Polynomial poly = new Polynomial();
					denom.ensureSpan();
					denom.powers[var.index()] = 1;
					denom.totalPower++;
					numer.coefficient = numer.coefficient.negate();
					
					poly.consumeMonomial(denom);
					poly.consumeMonomial(numer);
					addPolynomial(poly);
					
					varList.add(var);
				}
			}
			catch (VarietyParser.ParseError e) 
			{
				parseError = e;
				print("" + e + " at " + vp.c + " of '" + orderText + "'");
			}
			
			return varList.toArray(new Variable[0]);
		}

		
		// alternate. negative powers are ok
		void readVarietyParserToken(VarietyParser.Token token, int power, Monomial numer, Monomial denom)
		{
			if (token instanceof VarietyParser.Constant)
			{
				VarietyParser.Constant constant = (VarietyParser.Constant)token;
				
				// value...
				if (power > 0)
					numer.coefficient = numer.coefficient.multiply(constant.value.pow(power));
				else if (power < 0)
					denom.coefficient = denom.coefficient.multiply(constant.value.pow(-power));
				
				// exponent...
				if (constant.mantissa * power > 0)
					numer.coefficient = numer.coefficient.multiply(new BigInteger("10").pow(constant.mantissa * power));
				else if (constant.mantissa * power < 0)
					denom.coefficient = denom.coefficient.multiply(new BigInteger("10").pow(-constant.mantissa * power));
			}
			else if (token instanceof VarietyParser.Literal)
			{
				VarietyParser.Literal literal = (VarietyParser.Literal)token;
				
				// find it or create it 
				Variable variable = findVariable(literal.title);
				if (variable == null)
					variable = new Variable(literal.title);
				
				if (power > 0)
				{
					numer.ensureSpan();
					numer.powers[variable.index()] += power;
					numer.totalPower += power;
				}
				else if (power < 0)
				{
					denom.ensureSpan();
					denom.powers[variable.index()] += power;
					denom.totalPower += power;
				}
			}
			else if (token instanceof VarietyParser.Tuple)
			{
				VarietyParser.Tuple tuple = (VarietyParser.Tuple)token;
				if (tuple.contents.size() != 1)
					System.out.println("can't deal with mul-tuple");
				
				// if the innards is a single term, this could be done better
				readVarietyParserToken(tuple.contents.get(0), power, numer, denom);
			}
			else if (token instanceof VarietyParser.Unary)
			{
				VarietyParser.Unary unary = (VarietyParser.Unary)token;
				
				// in the case of (-x)^2 we skip the negation
				if (unary.negate && power % 2 == 1)
					numer.coefficient = numer.coefficient.negate();
					
				readVarietyParserToken(unary.token, power, numer, denom);
			}
			else if (token instanceof VarietyParser.Exponent)
			{
				VarietyParser.Exponent exponent = (VarietyParser.Exponent)token;
				
				readVarietyParserToken(exponent.token, exponent.power * power, numer, denom);
			}
			else if (token instanceof VarietyParser.Expression)
			{
				// note: if this expression has one term, we could apply the power to everything and treat it like one.
				// (a+b)^5 creates $ = a+b and resolves $^5. (a+b)^(1/5) creates $^5 = a+b and resolves $.
				VarietyParser.Expression expression = (VarietyParser.Expression)token;
				
				if (expression.elements.size() == 1)
				{
					readVarietyParserToken(expression.elements.get(0).term, power, numer, denom);
					if (expression.elements.get(0).subtracted)
						numer.coefficient = numer.coefficient.negate();
					return;
				}
				
				ArrayList<Monomial> monoList = new ArrayList<Monomial>();
				Monomial masterDenominator = new Monomial();
				Polynomial poly = consumeVarietyParserExpression(expression, masterDenominator, monoList);
								
				// now save off the master denominator with a variable alias
				Variable alias = new Variable();
				//alias.title = "${" + poly.describe() + "}";
				masterDenominator.ensureSpan();
				masterDenominator.powers[alias.index()] = 1;
				masterDenominator.totalPower += 1;
				masterDenominator.coefficient = masterDenominator.coefficient.negate();
				poly.consumeMonomial(masterDenominator);
				
				if (ordering != null)
					ordering.sortPolynomial(poly);
				addPolynomial(poly);
				
				if (power > 0)
				{
					numer.ensureSpan();
					numer.powers[alias.index()] += power;
					numer.totalPower += power;
				}
				else if (power < 0)
				{
					denom.ensureSpan();
					denom.powers[alias.index()] += -power;
					denom.totalPower += -power;
				}
			}
			else if (token instanceof VarietyParser.Term)
			{
				VarietyParser.Term term = (VarietyParser.Term)token;

				for (int e = 0; e < term.elements.size(); e++)
					if (term.elements.get(e).denominator == false)
						readVarietyParserToken(term.elements.get(e).token, power, numer, denom);
					else
						readVarietyParserToken(term.elements.get(e).token, power, denom, numer);
			}
			else
			{
				System.out.println("don't recognize VarietyParser.Token " + token);
			}
		}		
		Polynomial consumeVarietyParserExpression(VarietyParser.Expression expr, Monomial masterDenominator, List<Monomial> monoList)
		{
			Polynomial poly = new Polynomial();
			
			for (int t = 0; t < expr.elements.size(); t++)
			{
				VarietyParser.Term term = expr.elements.get(t).term;
				Monomial mono = new Monomial();
				
				if (expr.elements.get(t).subtracted)
					mono.coefficient = mono.coefficient.negate();
				
				// try this instead
				Monomial denom = new Monomial();
				
				for (int e = 0; e < term.elements.size(); e++)
					if (term.elements.get(e).denominator == false)
						readVarietyParserToken(term.elements.get(e).token, 1, mono, denom);
					else
						readVarietyParserToken(term.elements.get(e).token, 1, denom, mono);

				mono.consume(masterDenominator);
				
				masterDenominator.consume(denom);
				for (int m = 0; m < monoList.size(); m++)
					monoList.get(m).consume(denom);
				
				monoList.add(mono);
				poly.consumeMonomial(mono);
			}
			
			return poly;
		}
	}

	abstract class Formula
	{
		Variable output;
		Variable[] inputs;
		
		abstract double[] evaluateDouble(double[] inputValues);
		abstract BigRational[] evaluateRational(BigRational[] inputValues);
		abstract String describe();
	}
	class VariableFormula extends Formula
	{
		VariableFormula(Variable var)
		{
			output = var;
			inputs = new Variable[] { var };
		}
		double[] evaluateDouble(double[] inputValues)
		{
			return new double[] { inputValues[0] };
		}
		BigRational[] evaluateRational(BigRational[] inputValues)
		{
			return new BigRational[] { inputValues[0] };
		}
		String describe()
		{
			return "double " + output.title + ";\n";
		}
	}
	class ZeroFormula extends Formula
	{
		ZeroFormula(Variable var)
		{
			output = var;
			inputs = new Variable[0];
		}
		double[] evaluateDouble(double[] inputValues)
		{
			return new double[] { 0 };
		}
		BigRational[] evaluateRational(BigRational[] inputValues)
		{
			return new BigRational[] { new BigRational(0,1) };
		}
		String describe()
		{
			return output.title + " = 0;\n";
		}
	}
	class ValueFormula extends Formula
	{
		BigInteger numerator, denominator;
		int radical, root;
		BigRational precomputeRational;
		double precomputeDouble;
		
		ValueFormula(Variable var, BigInteger numer, BigInteger denom, int rad, int ro)
		{
			output = var;
			inputs = new Variable[0];

			numerator = numer;
			denominator = denom;
			radical = rad;
			root = ro;
			
			precomputeRational = new BigRational(numerator, denominator);
			if (root != 1)
			{
				precomputeRational.multiply(new BigRational(10,10));
			}
			precomputeDouble = Math.pow(precomputeRational.doubleValue(), (double)radical/(double)root);
			//precompute = new BigDecimal(numerator, 15).divide(new BigDecimal(denominator, 15), BigDecimal.ROUND_HALF_UP).doubleValue();
			//precompute = Math.pow(precompute, (double)radical/(double)root);
		}
		double[] evaluateDouble(double[] inputValues)
		{
			return new double[] { precomputeDouble };
		}
		BigRational[] evaluateRational(BigRational[] inputValues)
		{
			return new BigRational[] { null };
		}
		String describe()
		{
			boolean pow = (radical != 1) || (root != 1);
			if (denominator.equals(BigInteger.ONE) && pow == false)
				return output.title + " = " + numerator.toString() + " = " + precomputeDouble + ";\n";
			return output.title + (pow ? " = (" : " = ") + numerator.toString() 
				+ (denominator.equals(BigInteger.ONE) ? "" : ("/" + denominator.toString()))
				+ (pow ? (")^("+radical+"/"+root+") = ") : " = ") + precomputeDouble + ";\n";
		}
	}
	class UnirationalFormula extends Formula
	{
		// we really need an evaluate polynomial with inputs function. we should actually even reduce the terms into indexes into inputs[] first
		Polynomial numerator, denominator;
		int root;
		
		UnirationalFormula(Variable var, int[] maxPowers, Polynomial numer, Polynomial denom, int ro)
		{
			output = var;
			numerator = numer;
			denominator = denom;
			root = ro;
			
			ArrayList<Variable> params = new ArrayList<Variable>();
			for (int v = 0; v < maxPowers.length; v++)
				if (maxPowers[v] > 0)
					params.add(variableArray.get(v));
			inputs = params.toArray(new Variable[0]);
		}
		double[] evaluateDouble(double[] inputValues)
		{
			// first, power[5] in a monomial is which input?
			double[] map = new double[variableArray.size()];
			for (int i = 0; i < inputs.length; i++)
				map[variableArray.indexOf(inputs[i])] = inputValues[i];
			
			double numerValue = numerator.evaluate(map), denomValue = denominator.evaluate(map);
			return new double[] { (root == 1) ? numerValue/denomValue : Math.pow(numerValue/denomValue, 1.0/root) };
		}
		BigRational[] evaluateRational(BigRational[] inputValues)
		{
			return new BigRational[] { null };
		}
		
		String describe()
		{
			String params = "";
			for (int w = inputs.length - 1; w >= 0; w--)
				params += ((params == "") ? " " : ", ") + "double " + inputs[w].describe(1);
			
			String desc = "\ndouble " + output.title + "(" + params + " )\n{\n";
			String radicand;
			
			// if the denominator is 1, it's even more presentable to just skip it.
			Variety.Monomial loneDenom = (denominator.monomials.size() == 1) ? denominator.monomials.get(0) : null;
			if (loneDenom != null && loneDenom.coefficient.equals(BigInteger.ONE) && loneDenom.totalPower == 0)
			{
				radicand = factorAndDescribe(numerator);
			}
			else
			{
				desc += "  double numerator = " + factorAndDescribe(numerator) + ";\n";
				desc += "  double denominator = " + factorAndDescribe(denominator) + ";\n";
				radicand = "numerator/denominator";
			}
			desc += (root == 2) ? ("  return Math.sqrt(" + radicand + ");\n") 
						: (root > 1) ? ("  return Math.pow(" + radicand + ", 1.0/" + root + ");\n") 
						: "  return " + radicand + ";\n";
			return desc + "}\n\n";
		}
	}
	class SolverFormula extends Formula
	{
		ArrayList<Coefficient> coefficients;
		
		SolverFormula(Variable var, int[] maxPowers, ArrayList<Coefficient> coefList)
		{
			output = var;
			coefficients = coefList;
			
			ArrayList<Variable> params = new ArrayList<Variable>();
			for (int v = 0; v < maxPowers.length; v++)
				if (maxPowers[v] > 0)
					params.add(variableArray.get(v));
			inputs = params.toArray(new Variable[0]);
		}
		double[] evaluateDouble(double[] inputValues)
		{
			return new double[] { 0 };
		}
		BigRational[] evaluateRational(BigRational[] inputValues)
		{
			return new BigRational[] { null };
		}
		String describe()
		{
			String params = "";
			for (int w = inputs.length - 1; w >= 0; w--)
				params += ((params == "") ? " " : ", ") + "double " + inputs[w].describe(1);
			
			String desc = "\ndouble " + output.title + "(" + params + " )\n{\n";
			
			String phoney = "Poly2D.Double.solve(new int[] { ";
			for (int c = 0; c < coefficients.size(); c++)
			{
				desc += "  double coef" + coefficients.get(c).power + " = " + factorAndDescribe(coefficients.get(c).poly) + ";\n";
				phoney += (c != 0 ? ", " : "") + "coef" + coefficients.get(c).power;
			}
			desc += "  return " + phoney + " });\n}\n\n";
			return desc;
		}
	}

	// a little nicer presentation. still not perfect.
	// roll this into Polynomial, or perhaps the polynomial solution finder
	String factorAndDescribe(Variety.Polynomial poly)
	{
		if (poly.monomials.size() < 2)
			return poly.describe();
			
		Monomial common = new Monomial(), mono = poly.monomials.get(0);
		
		System.arraycopy(mono.powers, 0, common.powers, 0, variableCount());
		common.totalPower = mono.totalPower;
		common.coefficient = mono.coefficient.abs();
		
		for (int m = 1; m < poly.monomials.size(); m++)
		{
			mono = poly.monomials.get(m);
			for (int v = 0; v < variableCount(); v++)
				if (common.powers[v] > mono.powers[v])
				{
					common.totalPower -= (common.powers[v] - mono.powers[v]);
					common.powers[v] = mono.powers[v];
				}
			
			common.coefficient = common.coefficient.gcd(mono.coefficient);
		}
		
		if (common.totalPower == 0 && common.coefficient.abs().equals(BigInteger.ONE))
			return poly.describe();
		
		Polynomial newPoly = new Polynomial();
		for (int m = 0; m < poly.monomials.size(); m++)
		{
			mono = poly.monomials.get(m);
			Monomial newMono = new Monomial();
			
			for (int v = 0; v < variableCount(); v++)
				newMono.powers[v] = mono.powers[v] - common.powers[v];
			
			newMono.totalPower = mono.totalPower - common.totalPower;
			newMono.coefficient = mono.coefficient.divide(common.coefficient);
			addMonomial(newPoly, newMono);
		}

		return common.describe(false) + (describeFancy ? "" : "*") + "(" + newPoly.describe() + ")";
	}

	Formula createFormula(Polynomial poly, Variable var, int v, int[] maxPower)
	{
		ArrayList<Coefficient> coefficients = isolateCoefficients(poly, v);
		
		// ax^n - 0
		if (coefficients.size() == 1)
			return new ZeroFormula(var);

		// a + bx^n where a and b are just reals
		if (coefficients.size() == 2 && coefficients.get(0).power == 0
			&& coefficients.get(0).poly.monomials.size() == 1 && coefficients.get(0).poly.monomials.get(0).totalPower == 0
			&& coefficients.get(1).poly.monomials.size() == 1 && coefficients.get(1).poly.monomials.get(0).totalPower == 0)
		{
			if (coefficients.get(1).poly.monomials.get(0).coefficient.signum() < 0)
			{
				coefficients.get(0).poly.multiply(BigInteger.ONE.negate());
				coefficients.get(1).poly.multiply(BigInteger.ONE.negate());
			}

			coefficients.get(0).poly.multiply(BigInteger.ONE.negate());
			
			return new ValueFormula(var, coefficients.get(0).poly.monomials.get(0).coefficient,
				coefficients.get(1).poly.monomials.get(0).coefficient, 1, coefficients.get(1).power);
		}
		
		// after this point, we have to have maxPower
		if (maxPower == null)
			return null;
		
		// this poly could be a simple a() + b()x^n ... x = (-a()/b())^(1/n)
		if (coefficients.size() == 2 && coefficients.get(0).power == 0)
		{
			Variety.Monomial loneDenom = (coefficients.get(1).poly.monomials.size() == 1) ? coefficients.get(1).poly.monomials.get(0) : null;
			
			// for presentation, we like a positive denominator. 
			if (loneDenom != null && loneDenom.coefficient.signum() < 0)
			{
				coefficients.get(0).poly.multiply(BigInteger.ONE.negate());
				coefficients.get(1).poly.multiply(BigInteger.ONE.negate());
			}
			
			coefficients.get(0).poly.multiply(BigInteger.ONE.negate());
			maxPower[v] = 0;
			return new UnirationalFormula(var, maxPower, 
				coefficients.get(0).poly, coefficients.get(1).poly, coefficients.get(1).power);
		}

		maxPower[v] = 0;
		return new SolverFormula(var, maxPower, coefficients);
	}
	
	Formula[] listFormulas()
	{
		ArrayList<Formula> formulas = new ArrayList<Formula>();
		
		// r y x By Bx Ay Ax
		for (int v = variableCount() - 1; v >= 0; v--)
		{
			// find all polynomials with no variables before v that do contain v
			Variable var = variableArray.get(v);
			boolean foundAny = false;

			find: for (int p = polynomials.size() - 1; p >= 0; p--)
			{
				Polynomial poly = polynomials.get(p);
				int commonPower[] = new int[variableCount()];
				int maxPower[] = new int[variableCount()];
				
				for (int m = 0; m < poly.monomials.size(); m++)
				{
					for (int w = 0; w < v; w++)
						if (poly.monomials.get(m).powers[w] > 0)
							continue find;
							
					for (int w = v; w < variableCount(); w++)
					{
						if (maxPower[w] < poly.monomials.get(m).powers[w])
							maxPower[w] = poly.monomials.get(m).powers[w];
						
						if (poly.monomials.get(m).powers[w] != 0)
							commonPower[w] = (commonPower[w] == 0) ? poly.monomials.get(m).powers[w] : gcd(commonPower[w], poly.monomials.get(m).powers[w]);
					}
				}
				
				if (maxPower[v] == 0)
					continue find;
					
				foundAny = true;
				Formula formula = createFormula(poly, var, v, maxPower);
				formulas.add(formula);
				continue find;
				
			}
			
			if (foundAny == false)
				formulas.add(new VariableFormula(var));
		}
		
		return formulas.toArray(new Formula[0]);
	}
	
	
	int gcd(int a, int b)
	{
		return (b == 0) ? a : gcd(b, a%b);
	}
	
	// destroys both polynomials
	Polynomial gcd(Polynomial alpha, Polynomial beta)
	{
		ArrayList<Polynomial> alphaList = new ArrayList<Polynomial>(), betaList = new ArrayList<Polynomial>();
		
		alphaList.add(new Polynomial(alpha));
		betaList.add(new Polynomial(beta));
		
		while (betaList.get(0).monomials.size() != 0 && betaList.get(0).monomials.get(0).totalPower != 0)
		{
			//print("dividing " + alphaList.get(0).describe() + " by " + betaList.get(0).describe());
			
			syntheticDivision(alphaList.get(0), betaList, null, 0);
			
			ArrayList<Polynomial> swapList = alphaList;
			alphaList = betaList;
			betaList = swapList;
		}
		
		alphaList.get(0).reduceCoefficients();
		return alphaList.get(0);
	}
	
	Polynomial[] gradient(Polynomial poly)
	{
		Polynomial[] gradient = new Polynomial[variableCount()];
		
		for (int m = 0; m < poly.monomials.size(); m++)
		{
			Monomial mono = poly.monomials.get(m);
			
			for (int v = 0; v < variableCount(); v++)
				if (mono.powers[v] != 0)
				{
					Monomial term = new Monomial();
					term.consume(poly.monomials.get(m));
					term.coefficient = term.coefficient.multiply(new BigInteger("" + term.powers[v]));
					term.powers[v]--;

					if (gradient[v] == null)
						gradient[v] = new Polynomial();
					addMonomial(gradient[v], term);
				}
		}
		
		return gradient;
	}
	
	// list is sorted by smallest powers first
	class Coefficient
	{
		int power;
		Polynomial poly;
	}
	ArrayList<Coefficient> isolateCoefficients(Polynomial poly, int v)
	{
		ArrayList<Coefficient> coefficients = new ArrayList<Coefficient>();
		for (int m = 0; m < poly.monomials.size(); m++)
		{
			Variety.Monomial mono = poly.monomials.get(m);
			
			Variety.Monomial newMono = new Monomial();
			newMono.coefficient = mono.coefficient;
			System.arraycopy(mono.powers, 0, newMono.powers, 0, variableCount());
			newMono.totalPower = mono.totalPower - mono.powers[v];
			newMono.powers[v] = 0;
			
			Coefficient coefficient = null;
			int c = 0;
			while (c < coefficients.size())
			{
				if (mono.powers[v] == coefficients.get(c).power)
					coefficient = coefficients.get(c);
				if (mono.powers[v] <= coefficients.get(c).power)
					break;
				c++;
			}
			
			if (coefficient == null)
			{
				coefficient = new Coefficient();
				coefficient.power = mono.powers[v];
				coefficient.poly = new Polynomial();
				coefficients.add(c, coefficient);
			}
			
			addMonomial(coefficient.poly, newMono);
		}
		return coefficients;
	}


	String describe()
	{
		String str = "{";
		
		for (int p = 0; p < polynomials.size(); p++)
			str += ((p != 0) ? ", " : " ") + polynomials.get(p).describe();
		
		return str + " }";
	}
	
	// after this point, we're going to need monomial ordering. setting the ordering sorts polynomialList.
	
	Ordering ordering = null;
		
	ArrayList<Polynomial> polynomials = new ArrayList<Polynomial>();
	void addPolynomial(Polynomial poly)
	{
		// add to the list
		if (ordering == null || ordering.sortPolyList == false)
		{
			polynomials.add(poly);
			return;
		}
		
		int p = 0;
		while (p < polynomials.size()) 
		{
			Polynomial tryPoly = polynomials.get(p);
			int compare = ordering.comparePolynomials(poly, tryPoly);
			if (compare >= 0)
				break;
			p++;
		}
		
		polynomials.add(p, poly);
	}
	
	void addMonomial(Polynomial poly, Monomial mono)
	{
		int m = 0;
		while (m < poly.monomials.size()) 
		{
			Monomial tryMono = poly.monomials.get(m);
			int compare = ordering.compareMonomials(mono, tryMono);
			if (compare > 0)
				break;
			if (compare == 0) 
			{
				tryMono.coefficient = tryMono.coefficient.add(mono.coefficient);
				if (tryMono.coefficient.signum() == 0)
					poly.monomials.remove(m);
				return;
			}
			m++;
		}
		
		poly.monomials.add(m, mono);
	}
	
	void setOrdering(Ordering newWorldOrder)
	{
		ordering = newWorldOrder;
		
		ArrayList<Polynomial> oldPolynomials = polynomials;
		polynomials = new ArrayList<Polynomial>();
		
		for (int p = 0; p < oldPolynomials.size(); p++)
		{
			ordering.sortPolynomial(oldPolynomials.get(p));
			addPolynomial(oldPolynomials.get(p));
		}
	}
	
	abstract class Ordering
	{
		abstract int compareMonomials(Monomial positive, Monomial negative);
		
		// we're doing this as a sort of lex order but they could be stored in a different order!
		boolean sortPolyList = true;
		
		int comparePolynomials(Polynomial positive, Polynomial negative)
		{
			for (int m = 0; m < positive.monomials.size() && m < negative.monomials.size(); m++)
			{
				int order = compareMonomials(positive.monomials.get(m), negative.monomials.get(m));
				if (order != 0)
					return order;
			}
			
			// return the longer one as first i guess.
			return positive.monomials.size() - negative.monomials.size();
		}
		
		// sort one polynomial in place
		void sortPolynomial(Polynomial poly)
		{
			sort: for (int m = 0; m < poly.monomials.size(); m++)
			{
				Monomial mono = poly.monomials.get(m);
				mono.ensureSpan();
				
				int n = 0;
				
				// 30, 20, 10, where does 25 go? once mono is bigger, put it there.
				while (n < m)
				{
					int order = ordering.compareMonomials(mono, poly.monomials.get(n));
					
					//print(mono.describe(1) + " vs " + poly.monomials.get(n) + " : " + order);
					
					if (order == 0)
					{
						// match
						System.out.println("sortPolynomial match: " + mono.describe(false) + " vs " + poly.monomials.get(n).describe(false));
					}
					else if (order > 0)
					{
						poly.monomials.remove(m);
						poly.monomials.add(n, mono);
						continue sort;
					}
					
					n++;
				}
			}
		}
	}
 	class LexOrdering extends Ordering
	{
		int compareMonomials(Monomial positive, Monomial negative)
		{
			// the larger power of the first variable
			for (int i = 0; i < variableCount(); i++)
				if (positive.powers[i] != negative.powers[i])
					return positive.powers[i] - negative.powers[i];
			return 0;
		}
	}
	class GrlexOrdering extends Ordering
	{
		int compareMonomials(Monomial positive, Monomial negative)
		{
			// the larger total power
			if (positive.totalPower != negative.totalPower)
				return positive.totalPower - negative.totalPower;
			// otherwise the largest power of the first variable
			for (int i = 0; i < variableCount(); i++)
				if (positive.powers[i] != negative.powers[i])
					return (positive.powers[i] - negative.powers[i]);
			return 0;
		}
	}
	class GrevlexOrdering extends Ordering
	{
		int compareMonomials(Monomial positive, Monomial negative)
		{
			// the larger total power
			if (positive.totalPower != negative.totalPower)
				return positive.totalPower - negative.totalPower;
			// otherwise the smallest power of the last variable
			for (int i = variableCount() - 1; i >= 0; i--)
				if (positive.powers[i] != negative.powers[i])
					return -(positive.powers[i] - negative.powers[i]);
			return 0;
		}
	}
	class ElimOrdering extends Ordering
	{
		final int k;	// how many variables to eliminate.
		public ElimOrdering(int kIn)
		{
			k = kIn;
		}
		public int compareMonomials(Monomial positive, Monomial negative)
		{
			// the larger sum of first k powers
			int sum = 0;
			for (int i = 0; i < k; i++)
				sum = sum + positive.powers[i] - negative.powers[i];
			if (sum != 0)
				return sum;
				
			// otherwise this is just grevlex...grevlex is elim for all variables, or no variables too
			if (positive.totalPower != negative.totalPower)
				return positive.totalPower - negative.totalPower;
			for (int i = variableCount() - 1; i >= 0; i--)
				if (positive.powers[i] != negative.powers[i])
					return -(positive.powers[i] - negative.powers[i]);
			return 0;
		}
	}

	int divisionsTried = 0, divisionsDone = 0;
	
	// factorMap may gladly be null. poly becomes the remainder. if poly is in divisorList it will not be used.
	void syntheticDivision(Polynomial poly, ArrayList<Polynomial> divisorList, ArrayList<Polynomial> factorMap, int startTerm)
	{
		int polyIndex = startTerm;
		String sofar = "";
		
		checkNextTerm: while (polyIndex < poly.monomials.size())
		{
			Monomial polyTerm = poly.monomials.get(polyIndex);
			
			if (polyTerm.totalPower == 0)
			{
				polyIndex++;
				continue;
			}
				
			// we want to try the smallest divisors first. this could of course be improved further
			// and look into F4 algorithm too of course
			searchForDivisor: for (int p = 0; p < divisorList.size(); p++)
			{
				Polynomial divisor = divisorList.get(p);
				Monomial divisorLT = divisor.monomials.get(0);
				
				if (divisor == poly) 
					continue searchForDivisor;
				
				divisionsTried++;
				
				// here is a quickie optimization. really our search strategy should be in the ordering.
				// lex ordering might be better for sorting, even if the basis is grevlex.
				// testing for zero totalPower here prevents problems with {1} poly but should be handled elsewhere
				if (polyTerm.totalPower < divisorLT.totalPower || divisorLT.totalPower == 0)
					continue searchForDivisor;	
				
				// what do we have to multiply the divisor by to eliminate polyTerm?
				Monomial quotient = new Monomial();
				
				// say polyTerm is 14xyyz and divisor is 22xy, then quotient is 7yz, gcd is 2, polyMultiplier is 11
				
				for (int v = 0; v < variableCount(); v++)
				{
					quotient.powers[v] = polyTerm.powers[v] - divisorLT.powers[v];
					quotient.totalPower += quotient.powers[v];
					if (quotient.powers[v] < 0)
						continue searchForDivisor;
				}
				
				divisionsDone++;

				// find the gcd for the divisor and syzygy LT so we can scale both up to a common point
				BigInteger divisorCoef = divisorLT.coefficient.abs(), polyCoef = polyTerm.coefficient.abs();
				BigInteger gcd = divisorCoef.gcd(polyCoef);
				BigInteger polyMultiplier = divisorCoef.divide(gcd);
				
				quotient.coefficient = polyCoef.divide(gcd);
				if (divisorLT.coefficient.signum() == polyTerm.coefficient.signum())
					quotient.coefficient = quotient.coefficient.negate();
				
				if (factorMap != null)
				{
					sofar = "division: " + (polyMultiplier.equals(BigInteger.ONE) ? "" : ("" + polyMultiplier + "*"));
					sofar += "[" + poly.describe() + "] ";
					sofar += "+ " + quotient.describe(false) + "*[" + divisor.describe() + "]";
				}
				
				poly.multiply(polyMultiplier);
								
				if (factorMap != null)
				{
					for (int f = 0; f < factorMap.size(); f++)
						if (factorMap.get(f) != null)
							factorMap.get(f).multiply(polyMultiplier);
							
					if (factorMap.get(p) == null)
						factorMap.set(p, new Polynomial());
					Monomial showQuotient = new Monomial();
					showQuotient.consume(quotient);
					showQuotient.coefficient = showQuotient.coefficient.negate();
					factorMap.get(p).monomials.add(showQuotient);
				}
				
				//System.out.println("using divisor " + divisor.describe());
				for (int m = 0; m < divisor.monomials.size(); m++)
				{
					Monomial divisorTerm = divisor.monomials.get(m);
					Monomial subtractor = new Monomial();
					subtractor.coefficient = divisorTerm.coefficient.multiply(quotient.coefficient);
					for (int v = 0; v < variableCount(); v++) 
					{
						subtractor.powers[v] = divisorTerm.powers[v] + quotient.powers[v];
						subtractor.totalPower += subtractor.powers[v];
					}
					
					//System.out.println("divisorTerm " + divisorTerm.describe(false) + " subtractor " + subtractor.describe(false));

					// WHAT if this puts things into the remainder'd area, changing the indexing????
					// we should figure that out and also figure out mergesort for this.
					addMonomial(poly, subtractor);
				}
				
				if (factorMap != null)
				{
					//print(sofar + " = " + poly.describe());
				}
				
				continue checkNextTerm;
			}
			
			// all the way through without cancelling this term, remainderize it.
			polyIndex++;
		}
	}

	/*
	We have a list of pairs of polynomials. alpha always before beta in the list. The pairs are picked from the front but can be added anywhere. Creating a pair and
	finding out where to put it are threadsafe, and the exact point to add it to the list is rough
	*/
	/*abstract class ThreadedStandardBasis
	{
		abstract public void onCompleted();
		abstract public void onCancelled();

		ThreadedStandardBasis()
		{
			for (int i = 1; i <= 4; i++)
				workerThreads.add(new WorkerThread());
			
			for (int p = 0; p < polynomials.size(); p++)
				insertBasisPolynomial(new Polynomial(polynomials.get(p)));
			
		}
		
		ArrayList<WorkerThread> workerThreads = new ArrayList<WorkerThread>();

		class WorkerThread extends Thread
		{
			public void run()
			{
				while (true)
				{
					Pair pair = getPairToProcess();
					
					if (pair == null)
					{
						try
						{
							wait();
						}
						catch (InterruptedException ie) {}

						pair = getPairToProcess();
						if (pair == null)
							return;
					}
					
					
				}
			}
		}
		
		class Pair
		{
			Polynomial alpha, beta;
			Monomial alphaMult = new Monomial(), betaMult = new Monomial(), LCM = new Monomial();

			// create the pair and it will automatically be added to .pairs if it belongs there.
			Pair(Polynomial a, Polynomial b)
			{ 
				alpha = a; 
				beta = b; 

				Monomial alphaLead = alpha.monomials.get(0), betaLead = beta.monomials.get(0);

				for (int v = 0; v < variableCount(); v++)
				{
					int alphaPower = alphaLead.powers[v], betaPower = betaLead.powers[v];
					int diff = alphaPower - betaPower;
					
					if (diff > 0)
					{
						LCM.powers[v] = alphaPower;
						LCM.totalPower += alphaPower;
						betaMult.powers[v] += diff;
						betaMult.totalPower += diff;
					}
					else
					{
						LCM.powers[v] = betaPower;
						LCM.totalPower += betaPower;
						alphaMult.powers[v] += -diff;
						alphaMult.totalPower += -diff;
					}
				}
				
				if (LCM.totalPower == alphaLead.totalPower + betaLead.totalPower)
				{
					// do not consider relative primes
					countPairsNoCommon++;
				}
				else
				{
					// sorted by LCM, smallest first
					int p = pairIndex;
					while (p < pairs.size()) 
					{
						int compare = ordering.compareMonomials(LCM, pairs.get(p).LCM);
						if (compare < 0) 
							break;
						p++;
					}
			
					pairs.add(p, this);
				}
			}
		}
		
		final ArrayList<Pair> pairs = new ArrayList<Pair>(1000);
		int pairsIndex = 0;

		class PairToAdd
		{
			Pair newPair; 
			int desiredIndex;	// it will be added *roughly* here.
		}
		final ArrayList<PairToAdd> pairsToAdd = new ArrayList<PairToAdd>();
		
		synchronized Pair getPairToProcess()
		{
			while (pairsToAdd.size() != 0)
			{
				PairToAdd toAdd = pairsToAdd.get(0);
				pairs.add((pairsIndex > toAdd.desiredIndex) ? pairsIndex : toAdd.desiredIndex, toAdd.newPair);
				pairsToAdd.remove(0);
			}
			
			if (pairsIndex < pairs.size())
				return pairs.get(pairsIndex++);
			return null;
		} 

		final ArrayList<Polynomial> basisPolynomials = new ArrayList<Polynomial>();
		
		synchronized void insertBasisPolynomial(Polynomial poly)
		{
			for (int p = 0; p < basisPolynomials.size(); p++)
			{
				new Pair(basisPolynomials.get(p), poly);
			}
			
			addPolynomial(newPoly);
		}
	}*/
	
	// a simple algorithm, not the fast version, for debugging purposes.
	class StandardBasis
	{
		int countPairsNoCommon = 0;
		
		ArrayList<Pair> pairs = new ArrayList<Pair>(1000);
		int pairIndex = 0;
			
		class Pair
		{			
			Polynomial alpha, beta;
			Monomial alphaMult = new Monomial(), betaMult = new Monomial(), LCM = new Monomial();

			// create the pair and it will automatically be added to .pairs if it belongs there.
			Pair(Polynomial a, Polynomial b)
			{ 
				alpha = a; 
				beta = b; 

				Monomial alphaLead = alpha.monomials.get(0), betaLead = beta.monomials.get(0);

				for (int v = 0; v < variableCount(); v++)
				{
					int alphaPower = alphaLead.powers[v], betaPower = betaLead.powers[v];
					int diff = alphaPower - betaPower;
					
					if (diff > 0)
					{
						LCM.powers[v] = alphaPower;
						LCM.totalPower += alphaPower;
						betaMult.powers[v] += diff;
						betaMult.totalPower += diff;
					}
					else
					{
						LCM.powers[v] = betaPower;
						LCM.totalPower += betaPower;
						alphaMult.powers[v] += -diff;
						alphaMult.totalPower += -diff;
					}
				}
				
				if (LCM.totalPower == alphaLead.totalPower + betaLead.totalPower)
				{
					// do not consider relative primes
					countPairsNoCommon++;
				}
				else
				{
					// sorted by LCM, smallest first
					int p = pairIndex;
					while (p < pairs.size()) 
					{
						int compare = ordering.compareMonomials(LCM, pairs.get(p).LCM);
						if (compare < 0) 
							break;
						p++;
					}
			
					pairs.add(p, this);
				}
			}
		}
		
		StandardBasis()
		{
			for (int a = 0; a < polynomials.size(); a++)
				for (int b = a + 1; b < polynomials.size(); b++)
					new Pair(polynomials.get(a), polynomials.get(b));
		}
		
		String calcReport = "";
		
		boolean cancel = false;
		void cancel()
			{ cancel = true; }
		
		// how can we turn this into something pretty well threaded?
		synchronized Pair getPossiblePair()
		{
			if (pairIndex < pairs.size())
				return pairs.get(pairIndex++);
			return null;
		} 
		synchronized void declareResult(Polynomial newPoly)
		{
			for (int a = 0; a < polynomials.size(); a++)
				new Pair(polynomials.get(a), newPoly);
			addPolynomial(newPoly);
		}
		
		void calculate()
		{
			long time = System.nanoTime();
			
			int kappaChecks = 0, kappaHits = 0, calculations = 0, newOnes = 0;
			
			divisionsTried = 0; 
			divisionsDone = 0;
			
			findPair: while (pairIndex < pairs.size())
			{
				Pair pair = pairs.get(pairIndex++);
				Monomial alphaLead = pair.alpha.monomials.get(0), betaLead = pair.beta.monomials.get(0);

				// ...provided there is some k != i,j for which the pairs [i,k] and [j,k] are not in B, and LT(k) divides LCM(i,j).
				findKappa: for (int k = 0; k < polynomials.size(); k++)
				{
					Polynomial kappa = polynomials.get(k);
					if (kappa == pair.alpha || kappa == pair.beta)
						continue findKappa;
					
					kappaChecks++;
					
					Monomial kappaLead = kappa.monomials.get(0);
					for (int v = 0; v < variableCount(); v++)
						if (kappaLead.powers[v] > pair.LCM.powers[v])
							continue findKappa;
					
					// each polynomial has an associated list of the ones it is paired with. just make sure you don't see i and j.
					boolean found = false;
					for (int p = pairIndex; p < pairs.size() && found == false; p++)
					{
						Pair consider = pairs.get(p);
						found = (consider.alpha == kappa && (consider.beta == pair.alpha || consider.beta == pair.beta))
									|| (consider.beta == kappa && (consider.alpha == pair.alpha || consider.alpha == pair.beta));
					}
					if (found == false)
					{
						kappaHits++;
						continue findPair;
					}
				}
				
				if (cancel)
					break;
					
				calculations++;

				BigInteger coefGCD = alphaLead.coefficient.gcd(betaLead.coefficient);
				pair.alphaMult.coefficient = betaLead.coefficient.divide(coefGCD);
				pair.betaMult.coefficient = alphaLead.coefficient.divide(coefGCD).negate();

				Polynomial newPoly = new Polynomial();
				
				for (int m = 1; m < pair.alpha.monomials.size(); m++)
				{
					Monomial alphaMono = pair.alpha.monomials.get(m);
					Monomial newMono = new Monomial();
					
					newMono.coefficient = alphaMono.coefficient.multiply(pair.alphaMult.coefficient);
					for (int i = 0; i < variableCount(); i++) {
						newMono.powers[i] = alphaMono.powers[i] + pair.alphaMult.powers[i];
						newMono.totalPower += newMono.powers[i];
						}
					// add them directly; multiplying all by alphaMult cannot change ordering
					newPoly.monomials.add(newMono);
				}
				
				for (int m = 1; m < pair.beta.monomials.size(); m++)
				{
					Monomial betaMono = pair.beta.monomials.get(m);
					Monomial newMono = new Monomial();
					
					newMono.coefficient = betaMono.coefficient.multiply(pair.betaMult.coefficient);
					for (int i = 0; i < variableCount(); i++) {
						newMono.powers[i] = betaMono.powers[i] + pair.betaMult.powers[i];
						newMono.totalPower += newMono.powers[i];
						}
					// add with the fancy algorithm, because terms can cancel out
					addMonomial(newPoly, newMono);
				}
				
				ArrayList<Polynomial> factorMap = null;
				
				/*String out = pair.alphaMult.describe(false) + "[" + pair.alpha.describe() + "] + " + pair.betaMult.describe(false) + "[" + pair.beta.describe() + "] = ";
				factorMap = new ArrayList<Polynomial>(polynomials.size());
				for (int p = 0; p < polynomials.size(); p++)
					factorMap.add(null);*/
					
				syntheticDivision(newPoly, polynomials, factorMap, 0);
				
				/*for (int p = 0; p < polynomials.size(); p++)
					if (factorMap.get(p) != null)
						out += "(" + factorMap.get(p).describe() + ")[" + polynomials.get(p).describe() + "] + ";
						
				out += newPoly.describe();
				print(out + "\n");*/
				
				if (newPoly.monomials.size() != 0)
				{
					newOnes++;
					newPoly.reduceCoefficients();

					for (int a = 0; a < polynomials.size(); a++)
						new Pair(polynomials.get(a), newPoly);
					addPolynomial(newPoly);
				}
			}
			
			calcReport += ("...." + pairIndex + "/" + (pairIndex+countPairsNoCommon) + " pairs (" + calculations 
				+ " calcs vs. " + kappaHits + "/" + kappaChecks + " kappas), " + divisionsDone + "/" + divisionsTried 
				+ " divisions, creating " + newOnes + " in " + (double)((System.nanoTime() - time)/1000)/1000d + "ms\n");

			// reduce and minimalize.
			doMinimalize(true);
		
			// print
			for (int p = 0; p < polynomials.size(); p++)
			{
				//print(polynomials.get(p).describe());
			}
		}
		
		void doMinimalize(boolean doReduce)
		{
			minimalize: for (int p = 0; p < polynomials.size(); p++)
			{
				Polynomial poly = polynomials.get(p);
				
				//print("checking " + poly.describe());

				// does any other leading term divide perfectly into this one?
				if (doReduce)
				{
					reduce: for (int q = 0; q < polynomials.size(); q++)
					{
						if (q == p)
							continue;
							
						for (int v = 0; v < variableCount(); v++)
							if (poly.monomials.get(0).powers[v] < polynomials.get(q).monomials.get(0).powers[v])
								continue reduce;
								
						// we don't have a single lower factor; all of ours are the same or higher.
						//print("discarding " + poly.describe() + " by " + polynomials.get(q).describe());
						polynomials.remove(p--);
						continue minimalize;
					}
				}
				
				ArrayList<Polynomial> factorMap = null;
				/*factorMap = new ArrayList<Polynomial>(polynomials.size());
				for (int i = 0; i < polynomials.size(); i++)
					factorMap.add(null);*/
					
				// if we did reduction already there's no reason to check the leading term
				syntheticDivision(poly, polynomials, factorMap, doReduce ? 1 : 0);
				
				poly.reduceCoefficients();
				
				if (poly.monomials.size() == 0)
				{
				//	print("now zeroed");
					polynomials.remove(p--);
				}
			}
		}
	}
}