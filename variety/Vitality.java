import java.util.*;

/* Time to get Algebra.html ready. 

I would like not to have to store consumed and tested data for terminals at all. This includes nonterminals that are just terminals with no choices. Really only every choice deserves 
a tested and consumed. Also what about whitespace? OK, we need a parsing iterator.

Variable: "x" / "y" / "z"
Factor: Variable / "(" Expression ")"
Term: Factor [ ("*" / "/") Term }
Expression: Term [ ("+" / "-") Expression ]

Make a Parser given a rule hierarchy? OK, the question is, what does parsing do? It needs to return a consumed length and a tested length. What does parsing do though? Individual
parsers do different things, it is that simple, they can be very general (for debugging) or optimized (specialized)! Yay! All right.

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

void wholeParser(Stream text)
{
	int numerator = 0, denominator = 1;
	
	if (text.getAndSkip("-"))
		denominator = -1;
		
	int digitCount = 0;
	while (text.get("0"-"9")) 
		{ digitCount++; numerator = numerator*10 + (text.getAndSkip() - '0'); }
	if (digitCount == 0)
		throw noDigits;
		
	if (text.getAndSkip(".")) {
		int digitCount = 0;
		while (text.get("0"-"9")) 
			{ digitCount++; numerator = numerator*10 + (text.getAndSkip() - '0'); denominator *= 10; }
		if (digitCount == 0)
			throw noDigits;
		}
		
	return BigRational(numerator, denominator);
}

BASING a language on another language? Since everything is based on {<char>}

WORD: <LETTER>*
LEVEL1: (WORD [' '*])
LEVEL2/LEVEL1: <FIRSTWORD:WORD> <SECONDWORD:WORD> <THIRDWORD:WORD>

MEMOZATION...

S: (A B | A <LETTER>) C

A B C -> A Z C


Let's make a use case. Let's define a hypercard stack! Yeah!

Rule: ("A" "B" | "A" <char>) "C"

Main: Alpha "C"
Alpha: Left | Right
Left: "A" "B"
Right: "A" <char>

Applied to "ABC", we start Main, start Alpha, start Left, succeed in Left, succeed in Alpha, succeed in Main.
Applied to "AZC", we start Main, start Alpha, start Left, fail in Left, start Right, succeed in Right, succeed in Alpha, succeed in Main.
B->Z: we enter Main, enter Alpha, enter Left, fail Left, enter Right, succeed in Right, succeed in Main

Optimized version:

Main: "A" Beta "C"
Beta: "B" | <char>

Applied to "ABC", we start Main, start Beta, succeed in Beta, succeed in Main.
Applied to "AZC", we start Main, start Beta, succeed in Beta, succeed in Main.

Question. Some of Main is filler. For example in an HTML tag the brackets are not important, the grammar is pointless, how do we want to refer to things?
Tag: Openbracket Whitespace Word Whitespace {Word Whitespace Equals Whitespace Value Whitespace} CloseBracket
 */


class Vitality
{
	class DebuggingParser
	{
		Rule mainRule, preparseRule;
		DebuggingParser(Rule r, Rule p)
			{ mainRule = r; preparseRule = p; }
		
		 int change, changeEnd, changeDelta;
		void parse(Rule rule, int mark, int markDelta)
		{
		}
	}
	
	abstract class Rule
	{
		abstract Result result();
		abstract class Result
		{
			// When Result is first created, successful(), charactersTested() and charactersConsumed() is meaningless.
			final Rule rule()
				{ return Rule.this; }
			
			abstract boolean successful();
			abstract int charactersTested();
			abstract int charactersConsumed();	// must return 0 if successful() was false
			abstract Result child(int index);
			abstract void reparse(final int change, final int changeEnd, final int changeDelta, int mark, int markDelta); 
		}
	}
	
	abstract class Emitter
	{
		// what do we actually want to do?
		abstract void insertChild(Rule.Result result, int offset);
		abstract void removeChild(Rule.Result result, int offset);
	}
	
	class Terminal extends Rule
	{
		String value;
		Terminal(String v)
			{ value = v; }
		Result result()
			{ return new TerminalResult(); }
			
		class TerminalResult extends Result
		{
			boolean match = false;
			
			boolean successful()
				{ return match; }
			int charactersTested()
				{ return value.length(); }
			int charactersConsumed()
				{ return match ? value.length() : 0; }
			Result child(int index)
				{ return null; }
			void reparse(final int change, final int changeEnd, final int changeDelta, int mark, int markDelta)
				{ match = (value.length() <= text.length() - mark) && text.substring(mark, mark + value.length()).equals(value); }
		}
	}
	class Concatenation extends Rule
	{
		Rule[] subRules;
		Concatenation(Rule[] sub)
			{ subRules = sub; }
		Result result()
			{ return new ConcatenationResult(); }
		
		class ConcatenationResult extends Result
		{
			Result[] subResults = new Result[subRules.length];
			int subResultSuccesses;
			int subCharactersTested;
			int subCharactersConsumed;

			boolean successful()
				{ return (subResultSuccesses == subRules.length); }
			int charactersTested()
				{ return subCharactersTested; }
			int charactersConsumed()
				{ return subCharactersConsumed; }
			Result child(int index)
				{ return (index < subResultSuccesses) ? subResults[index] : null; }
			void reparse(final int change, final int changeEnd, final int changeDelta, int mark, int markDelta) 
			{
				subResultSuccesses = 0;
				subCharactersTested = 0;
				subCharactersConsumed = 0;
				
				while (subResultSuccesses < subRules.length)
				{
					Result subResult = subResults[subResultSuccesses];
					
					if (subResult == null)
					{
						subResults[subResultSuccesses] = subRules[subResultSuccesses].result();
						subResult = subResults[subResultSuccesses];
						
						subResult.reparse(change, changeEnd, changeDelta, mark, markDelta);
						declareTested(mark + subResult.charactersTested());

						if (subResult.successful() == false)
							return;
						
						mark += subResult.charactersConsumed();
						subCharactersConsumed += subResult.charactersConsumed();
						subResultSuccesses++;
						
						continue;
					}
					
					if (subResult.successful() && mark + subResult.charactersTested() <= change)
					{
						declareTested(mark + subResult.charactersTested());
						mark += subResult.charactersConsumed();
						subCharactersConsumed += subResult.charactersConsumed();
						subResultSuccesses++;
						
						continue;
					}

					int oldConsumed = subResult.charactersConsumed();
					subResult.reparse(change, changeEnd, changeDelta, mark, markDelta);
					declareTested(mark + subResult.charactersTested());

					if (subResult.successful() == false)
						return;
					
					mark += subResult.charactersConsumed();
					subCharactersConsumed += subResult.charactersConsumed();
					
					markDelta += (subResult.charactersConsumed() - oldConsumed);
					
					subResultSuccesses++;
				}
			}
			
			void declareTested(int t)
				{ if (subCharactersTested < t) subCharactersTested = t; }
		}
	}
	class Choice extends Rule
	{
		Rule[] subRules;
		Choice(Rule[] sub)
			{ subRules = sub; }
		Result result()
			{ return new ChoiceResult(); }
	
		class ChoiceResult extends Result
		{
			Result[] subResults = new Result[subRules.length];
			int subResultChosen;
			int subCharactersTested;

			boolean successful()
				{ return (subResultChosen != -1); }
			int charactersTested()
				{ return subCharactersTested; }
			int charactersConsumed()
				{ return (subResultChosen != -1) ? subResults[subResultChosen].charactersConsumed() : 0; }
			Result child(int index)
				{ return (index == 0 && subResultChosen != -1) ? subResults[subResultChosen] : null; }
			void reparse(final int change, final int changeEnd, final int changeDelta, int mark, int markDelta) 
			{
				subResultChosen = -1;
				subCharactersTested = 0;
				
				for (int i = 0; i < subResults.length; i++)
				{
					Result subResult = subResults[i];
					
					if (subResult == null)
					{
						subResults[i] = subRules[i].result();
						subResult = subResults[i];
					}
					
					subResult.reparse(change, changeEnd, changeDelta, mark, markDelta);
					if (subCharactersTested < subResult.charactersTested())
						subCharactersTested = subResult.charactersTested();

					if (subResult.successful() == true)
					{
						subResultChosen = i;
						return;
					}
				}
			}
		}
	}
	class Repetition extends Rule
	{
		int min, max;
		Rule subrule;
		
		Repetition(int min, int max, Rule sub)
			{ this.min = min; this.max = max; subrule = sub; }
		Result result()
			{ return new RepetitionResult(); }

		class RepetitionResult extends Result
		{
			ArrayList<Result> subResults = new ArrayList<Result>();
			int subCharactersTested;
			int subCharactersConsumed = 0;

			boolean successful()
				{ return (subResults.size() >= min); }
			int charactersTested()
				{ return subCharactersTested; }
			int charactersConsumed()
				{ return subCharactersConsumed; }
			Result child(int index)
				{ return (index < subResults.size()) ? subResults.get(index) : null; }
			void reparse(final int change, final int changeEnd, final int changeDelta, int mark, int markDelta) 
			{
				System.out.println("START, oldChars = " + ((changeEnd - changeDelta) - (mark - markDelta)) + ", newChars = " + (changeEnd - mark)); 
				subCharactersConsumed = 0;
				subCharactersTested = 0;
				
				int index = 0;
				while (index < subResults.size() && index < max)
				{
					if (mark < changeEnd && mark - markDelta < changeEnd - changeDelta)// (oldChars > 0 && newChars > 0)
					{
						Rule.Result segment = subResults.get(index);
						if (mark + segment.charactersTested() <= change)
						{
							declareTested(mark + segment.charactersTested());
							mark += segment.charactersConsumed();
							subCharactersConsumed += segment.charactersConsumed();
							index++;
							System.out.println("REPARSED (SKIPPED), oldChars = " + ((changeEnd - changeDelta) - (mark - markDelta)) + ", newChars = " + (changeEnd - mark)); 
						}
						else
						{
							int oldLength = segment.charactersConsumed();
							segment.reparse(change, changeEnd, changeDelta, mark, markDelta);
							declareTested(mark + segment.charactersTested());
							if (segment.successful() == false)
							{
								while (subResults.size() > index) 
									subResults.remove(index);
								return;
							}
							mark += segment.charactersConsumed();
							subCharactersConsumed += segment.charactersConsumed();
							markDelta += (segment.charactersConsumed() - oldLength);
							index++;
							System.out.println("REPARSED, oldChars = " + ((changeEnd - changeDelta) - (mark - markDelta)) + ", newChars = " + (changeEnd - mark)); 
						}
					}
					else if (markDelta == changeDelta)
					{
						System.out.println("DONE, oldChars = " + ((changeEnd - changeDelta) - (mark - markDelta)) + ", newChars = " + (changeEnd - mark)); 
						return;
					}
					else if (markDelta < changeDelta)
					{
						Rule.Result segment = subrule.result();
						segment.reparse(change, changeEnd, changeDelta, mark, markDelta);
						declareTested(mark + segment.charactersTested());

						if (segment.successful() == false)
						{
							while (subResults.size() > index) 
								subResults.remove(index);
							return;
						}
						
						subResults.add(index, segment);
						mark += segment.charactersConsumed();
						subCharactersConsumed += segment.charactersConsumed();
						markDelta += segment.charactersConsumed();
						index++;
						System.out.println("PARSED AND INSERTED, oldChars = " + ((changeEnd - changeDelta) - (mark - markDelta)) + ", newChars = " + (changeEnd - mark)); 
					}
					else if (markDelta > changeDelta)
					{
						Rule.Result segment = subResults.get(index);
						markDelta -= segment.charactersConsumed();
						subResults.remove(index);
						System.out.println("DELETED, oldChars = " + ((changeEnd - changeDelta) - (mark - markDelta)) + ", newChars = " + (changeEnd - mark)); 
					}
				}

				while (index < max)
				{
					Rule.Result segment = subrule.result();
					segment.reparse(change, changeEnd, changeDelta, mark, markDelta);
					declareTested(mark + segment.charactersTested());

					if (segment.successful() == false)
						return;
					
					subResults.add(index, segment);
					mark += segment.charactersConsumed();
					subCharactersConsumed += segment.charactersConsumed();
					markDelta += segment.charactersConsumed();
					index++;
					System.out.println("PARSED AND INSERTED, oldChars = " + ((changeEnd - changeDelta) - (mark - markDelta)) + ", newChars = " + (changeEnd - mark)); 
				}
			}
			void declareTested(int t)
				{ if (subCharactersTested < t) subCharactersTested = t; }
		}
	}
	
	public static void main(String[] arg)
	{		
		Vitality v = new Vitality() 
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
				Repetition rep = new Repetition(0, 2, new Choice(new Rule[] {
					new Terminal("0"), new Terminal("1"), new Terminal("2"), new Terminal("3"), new Terminal("4"), 
					new Terminal("zero"), new Terminal("one"), new Terminal("two"), new Terminal("three"), new Terminal("four")
					}) );
				
				init(rep, "zero1two");
				debug();
			}
		};
		
		v.set(4, 1, "onepoo");
		v.debug();

		v.set(0, v.text.length(), "0dandan");
		v.debug();

		v.set(0, 0, "1#");
		v.debug();

		v.set(0, 2, "12");
		v.debug();
	}
	
	String text;
	Rule mainRule;
	Rule.Result mainTree;
	
	Vitality()
	{
		prepare();
	}
	public void prepare()	// override for convenience in anonymous constructors
	{
	}
	
	void init(Rule m, String t)
	{
		mainRule = m;
		mainTree = mainRule.result();
		
		text = t;
		mainTree.reparse(0, text.length(), text.length(), 0, 0);
	}
	void set(int offset, int length, String replace)
	{
		String oldDesc = text;
		
		text = text.substring(0, offset) + replace + text.substring(offset + length, text.length());
		
		System.out.println(oldDesc + " ->( " + offset + "," + length + "," + replace + " )-> " + text);

		// fix that part what that needs teh fixins
		
		int change = offset;
		int changeEnd = offset + replace.length();
		int changeDelta = replace.length() - length;  // how many characters we added
		int mark = 0;
		int markDelta = 0;	// how far ahead of the old mark we are now
				
		mainTree.reparse(change, changeEnd, changeDelta, mark, markDelta);
	}
	
	abstract class Walker
	{
		// call run() to walk the tree
		void run()
		{
			Rule.Result result = mainTree;
			int mark = 0;
			
			enterAndLeave(result, mark);
		}
		void enterAndLeave(Rule.Result result, int mark)
		{
			enterResult(result, mark);
			
			int i = 0;
			Rule.Result child;
			while ((child = result.child(i++)) != null)
			{
				enterAndLeave(child, mark);
				mark += child.charactersConsumed();
			}
			
			leaveResult(result, mark);
		}
		
		// override these
		abstract void enterResult(Rule.Result result, int offset);
		abstract void leaveResult(Rule.Result result, int offset);
	}
	
	void debug()
	{
		new Walker() 
		{
			int indent = 0;
			
			void enterResult(Rule.Result result, int mark)
			{
				String spaces = "                       ".substring(0,indent);
				String output = spaces + result.toString() 
					+ (result.successful() ? (" • " + result.charactersConsumed()) : "") 
					+ (" !! " + result.charactersTested()) 
					+ " '" + text.substring(mark, mark + result.charactersConsumed()) + "'";
				System.out.println(output);
				
				indent++;
			}
			void leaveResult(Rule.Result result, int mark)
			{
				indent--;
			}
		}.run();
	}
}