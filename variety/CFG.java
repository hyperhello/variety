/*

Dan, you're doing a great job. Keep backups of all your files repeatedly.

Move all this back into Variety and write syntax in the input area, then break down the syntax. Then we can more interestingly work on Reparser and Translator.


If the subresult GREW in size, then it either stole the front chars from the next subresult, or it just lengthened the entire thing.
If the subresult SHRUNK in size, then it either changed the start point of the next subresult, or just shortened the entire thing.

We don't want to waste space saving Results of terminals you know. 

When the text is parsed, that is the opportunity to set up reparse triggers. Rules can have these attached.




*/

import java.util.*;

class CFG
{	
	static String spaces = "                                                                        "; 
	
	/* Bootstrap Test */
	
	public static void main(String[] arg)
	{
		CFG cfg = new CFG();
		cfg.debugOn();
		
		if (false)
		{
			Rule mainRule = cfg.inputTextGrammar("S : ('dan gelder' | 'dan') [' apples' | ' carrot'] ' farms'");
			return;
		}
		
		if (false)
		{
			Repetition rep = cfg.new Repetition(0, 0, cfg.getTerminal("Boing "));
			cfg.init(rep, "Boing Boing ");
			cfg.setText(6, 0, "Buh-");
			cfg.setText(6, 4, "");
			return;
		}
		
		Terminal ter = cfg.getTerminal("zero");
		Choice cho = cfg.new Choice(new Rule[] { cfg.getTerminal("0"), cfg.getTerminal("zero") });
		Concatenation con = cfg.new Concatenation(new Rule[] { 
			cfg.new Choice(new Rule[] { cfg.getTerminal("zero"), cfg.getTerminal("0") }),
			cfg.new Choice(new Rule[] { cfg.getTerminal("one"), cfg.getTerminal("1") }),
			cfg.new Choice(new Rule[] { cfg.getTerminal("two"), cfg.getTerminal("2") }),
			cfg.new Choice(new Rule[] { cfg.getTerminal("three"), cfg.getTerminal("3") }),
			cfg.new Choice(new Rule[] { cfg.getTerminal("four"), cfg.getTerminal("4") })
			});
		
		// numbers test
		Choice digits = cfg.new Choice(new Rule[] { cfg.getTerminal("111111"), cfg.getTerminal("11111"), cfg.getTerminal("1111"), cfg.getTerminal("111"), cfg.getTerminal("11"), cfg.getTerminal("1"), 
												cfg.getTerminal("222222"), cfg.getTerminal("22222"), cfg.getTerminal("2222"), cfg.getTerminal("222"), cfg.getTerminal("22"), cfg.getTerminal("2"), 
												cfg.getTerminal("333333"), cfg.getTerminal("33333"), cfg.getTerminal("3333"), cfg.getTerminal("333"), cfg.getTerminal("33"), cfg.getTerminal("3") });
		//Repetition digitsTest = cfg.new Repetition(0,0, digits);
		Concatenation digitsTest = cfg.new Concatenation(new Rule[] { digits, digits, digits });
		
		cfg.init(digitsTest, "11112222");
		cfg.printDebug();
		
		//cfg.betterSetText(6, 4, "2222");
		cfg.setText(2, 4, "1111");
		cfg.printDebug();
		
		// we're getting reparse() right first and then we can do shift reduce. under consideration: when oldLength and newLength are both 0 we are done with reparse.
		// what is the right time to check? After every attempt? Does whatever I decide stay solid when there are NESTED concats? Remember it's the caller's job to
		// optimize away calling for reparse-check in the first place, so probably looking for 0-0 after moving the Lengths, not before.
		
		/*if (true)
		{
			cfg.init(cho, "0");
			cfg.printDebug();
			
			cfg.setText(0, 1, "zero");
			cfg.printDebug();
			
			cfg.setText(0, 4, "zero");
			cfg.printDebug();
			
			return;
		}*/
		
		/*cfg.init(con, "01234");
		cfg.printDebug();
		
		cfg.setText(3, 1, "throe");
		cfg.printDebug();
		
		cfg.setText(3, 5, "3");
		cfg.printDebug();*/
		
		/*cfg.init(con, "zero123");
		
		cfg.setText(2, 2, "o");
		cfg.setText(2, 0, "r");
		cfg.setText(4, 0, "123");
		cfg.setText(5, 0, "");
		cfg.setText(5, 1, "two");
		cfg.setText(5, 3, "2");	// this triggers a problem!
		cfg.setText(3, 3, "o12");*/
	}
	
	/* Debugger */
	
	String debugLog = null;
	int debugIndent = 0;
	void debugOn()
	{
		debugLog = "";
		debugIndent = 0;
	}
	void printDebug()
	{
		System.out.println(debugLog + "\n");
		debugLog = "";
	}
	void debug(String item)
	{
		debugLog += spaces.substring(0,debugIndent) + item + "\n";
	}
	
	/* User Functions */
	
	Rule mainRule;
	Rule.Result mainResult;
	String text = "";

	void init(Rule r, String t)
	{
		mainRule = r;
		mainResult = null;
		text = t;
		
		debug("init: " + text);
		
		mainResult = mainRule.doParse(0);
	}
	
	void setText(int offset, int length, String t)
	{
		debug("setText: " + text.substring(0, offset) + " (" + text.substring(offset, offset+length) + "->" + t + ") " + text.substring(offset+length, text.length()));
		
		text = text.substring(0, offset) + t + text.substring(offset + length, text.length());
		mainResult.doReparse(offset, 0, length, t.length());
	}
	
	void betterSetText(int offset, int length, String t)
	{
		debug("betterSetText: " + text.substring(0, offset) + " (" + text.substring(offset, offset+length) + "->" + t + ") " + text.substring(offset+length, text.length()));
		
		text = text.substring(0, offset) + t + text.substring(offset + length, text.length());
		mainResult.doBetterReparse(0, offset, t.length(), t.length() - length);
	}
	
	void printOutline()
	{
		mainResult.doEmit(0, new Emitter() {
			int depth = 0;
			
			void emit(int mark, Rule.Result result)
			{
				System.out.println(
					("                                      ").substring(0,depth) + result.rule()
					+ (result.successful() ? (" • " + result.charactersConsumed()) : "") + " !! " + result.charactersTested() 
					+ " \"" + text.substring(mark, mark + result.charactersConsumed()) + "\"");
				depth++;
			}
			void afterEmit(int mark, Rule.Result result)
			{
				depth--;
			}
			void reEmit(int mark, Rule.Result result)
			{
			}
		});
	}
	
	/* The Internals */
	
	HashMap<String, Terminal> terminalHashMap = new HashMap<String,Terminal>();
	Terminal getTerminal(String value)
	{
		Terminal terminal = terminalHashMap.get(value);
		if (terminal == null)
		{
			terminal = new Terminal(value);
			terminalHashMap.put(value, terminal);
		}
		return terminal;
	}
	
	/*
	
	Lesse. First we have 'emit' which basically lets you get ready to parse, lets you stage for the internal productions. 
	Then afterEmit which lets you reap what you have and tear down the stage.
	
	Great for making outlines after you have all the results.
	
	We could argue that a reparse would use the same strategy. Brand new Results would inherit no behavior at parse time
	so they'd have to be "blessed" in this fashion afterwards. A reparse would have to inform its Rule that something in
	its nature must have changed (a +40 turning into -40, perhaps) and needed to be updated. something like reEmit? And
	anything successfully reparsed would then need to call reEmit on its parents...
	
	OK. Something that is parsed in reEmit has The Local Emitter called on it? Yes, it does. So there is a chance here to
	make something more elegant...have init() include the emitter, and have setText() include the reemitter, and they are
	the same?
	
	And when something is reparsed in reEmit, it is aware that its information has changed, so it calls reEmit on itself.
	
	This sounds doable. What would it imply for the side of the problem where the emitter is supplied? What are some simple
	cases of emitters that could be designed? How about this. 
	
	BooleanVariable : "true" | "false"
	
	So an emitter that follows this strategy would receive emit() and initialize a variable that could be 
	cross-referenced somewhere but for the lack of any identifying information. It's really completely self-contained and
	in terms of analysis, really has the jump on exactly what its children mean. After "true" and "false" have been 
	meaningfully parsed in some way and the information presumably accessible via the Rule.Result handle (PROPERTIES), 
	then the emitter would receive afterEmit(). This is a chance to do whatever now that any child objects are known to
	have been created. Perhaps also you have some synchronizing or reporting to do to your parent, if you know what it is.
	In the event of a change, you are now presumably positioned to handle reEmit().
	
	This makes sense to me. It can perform the basic outlining functions, it can work with a stack (its own local stack!)
	and it can work as a subgraph too.
	
	All right! In that case, we gotta talk about classes. Specifically, class here means an individual enough rule that you
	can attach an emitter to it. Within the syntax you can name or classify something by preceding it with a name followed
	by a colon, like this: 
	
	BooleanVariable : True: "true" | False: "false"
	LikesIceCream : BooleanVariable ("!" * exclamationCount)
	
	Then there would be a referrable object called True and called False inside BooleanVariable. Conversely, every instance
	of a LikesIceCream will contain a BooleanVariable, AND, a BooleanVariable.True, AND, a BooleanVariable.False. Basically
	parsed from right to left. Every time a token in the input stream is eoncountered it's a chance to add a new emitter so
	you can extend the stack forward. 
	At BooleanVariable (assuming you start there) your stack is BooleanVariable. 
	At : your stack is BooleanVariable:
	At True your stack is BooleanVariable : True. True looks back at your stack, see the :, and remove yourself and it but extends right, so we're at BooleanVariable.True
	At : your stack is BooleanVariable : True : 
	At "true" you look back at your stack, see the :, and remove yourself and it. So the stack is ... WAIT WHAT this is too complicated for right now. Let's see
	
	How about the old printOutline? And this bit of cooleness:
	
			VarietyBNF bnf = new VarietyBNF("S : '-' FN | FN \n"
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
			
			bnf.parse("-123.45"); 

			S : ['-'] DL [ '.' DL ]
			DL : 0 * { D }
			D : '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'

	So there are techniques. You can parse the whole thing, get a Result tree that is known to be successful, and then:
	
	S.emit { numerator = 0; denominator = 1; increaser = 1; }
	'-'.emit { denominator = -1; }
	'.'.emit { increaser = 10; }
	D.emit { numerator = numerator*10 + (text.charAt(0) - '0'); denominator *= increaser; } 

	So we could have this actual code that responds to parsing:
	
	Parser p = new Parser("S : ['-'] DL ['.' DL]; DL : {D}; D : '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'")
	{
		int numerator = 0, denominator = 1, increaser = 1;
		
		void installEmitters()
		{
			new Emitter("S") { 
				void emit() { numerator = 0; denominator = 1; increaser = 1; } 
				void afterEmit() { doSomething(numerator/denominator); } 
				};
			new Emitter("'-'") { void emit() { denominator = -1; } };
			new Emitter("'.'") { void emit() { increaser = 10; } };
			new Emitter("'D'") { void emit() { numerator = numerator*10 + (text.charAt(0) - '0'); denominator *= increaser; } };
		}
		void doSomething(float f)
		{
			System.out.println("result: " + f);
		}
	};
	
	// these trigger emit() and afterEmit() which triggers doSomething()
	p.init("-12.345");
	p.init("12");
	
	// these trigger emit() but S never has afterEmit() called
	p.init("12.");
	p.init("bad input");
	
	Now we need a scheme for reparsing where if you don't catch reEmit, it defaults to just emitting. Also this scheme needs to be functional. Destroying the Result
	object in memory and rebuilding it later should not even be noticed by the emitter object in Java. 
	
	The result tree is a transitory thing, completely transitory. It might exist only as an iterator. emit() should really be sent with with specific data (or at least
	a way to get them) like the text, offset, and length (properties of the text and nothing much more) and the children, or what exactly do we pass along? at emit()
	we don't know the children. What about a shift-reduce parser? 
	
	Parser.Rule.Result result = p.init("-12.345");
	p.setText("6", 6, 1);
	
	The best way to integrate the emitters, whatever form they will take, is to look at how the reparse() flow mirrors the mainResult.doEmit() function. Okay, the implicit
	thing of context free grammar is that nothing can see outside itself until it's been parsed. That's why doEmit recurses through the syntax tree, because that extra
	step is essential to getting anything done in terms of structure. The emitter follows every correct answer: emit() on entering, then the same emitter on each child in 
	a depth-first iteration, then afterEmit() to get a chance to wind structures back. The real question then, how to tear down these things after a reparse. Remember
	the point of reparse is to notify all parallel structures about the change. This parallel structure ought to be able to retrace any necessary hierarchy to update itself;
	for example, if the results were represented by nodes in a tree, then the node handle would have to be repopulated.
	
	*/
	
	abstract class Emitter
	{
		/* The Emitter Model:
			emit - Your information is given.
			afterEmit - Your children's information has been given.
			reEmit - Your information has changed.
		*/
		abstract void emit(int offset, Rule.Result result);
		abstract void afterEmit(int offset, Rule.Result result);
		//abstract void reEmit(int offset, Rule.Result result);
	}

	abstract class Rule
	{
		abstract Result parse(int mark);
		
		Result doParse(int mark)
		{
			String oldStateVector =  "doParse: " + this + " ( " + mark + " )";
			debug(oldStateVector + "...");
			
			debugIndent++;
			Result result = parse(mark);
			debugIndent--;
			
			if (result.successful())
				debug(oldStateVector + (result.successful() ? (" • " + result.charactersConsumed()) : "") + " !! " + result.charactersTested());
			
			return result;
		}
		
		// it is the contract of a Result to stay correct in its and all its subresults' parsing as long as no characters within
		// the range of charactersConsumed() are changed. that means that during reparse(), if the range overlaps, that subresult
		// must be reparsed() or invalidated.

		abstract class Result 
		{ 
			Rule rule()
				{ return Rule.this; }
				
			abstract boolean successful();
			abstract int charactersTested();
			abstract int charactersConsumed();	// must return 0 if successful() was false
			abstract void doEmit(int mark, Emitter emitter);
			
			void betterReparse(int mark, int changeMark, int newSpan, int balance)
			{
				debug("Write betterReparse for this: " + rule());
			}
			
			void doBetterReparse(int mark, int changeMark, int newSpan, int balance)
			{
				String oldStateVector =  "doBetterReparse: " + rule() + (successful() ? (" • " + charactersConsumed()) : "") + " !! " + charactersTested()
					+ " ( mark=" + mark + " changeMark=" + changeMark + " newSpan=" + newSpan + " balance=" + balance + " )";
				debug(oldStateVector + "...");
				
				// to calc number of chars in range, it's count = (consumed - (changeMark - mark)), 0 <= count <= spanLength

				int oldConsumed = charactersConsumed();
				int oldCharsInsideRange = oldConsumed - (changeMark - mark);
				int oldSpan = newSpan - balance;
				oldCharsInsideRange = (oldCharsInsideRange < 0) ? 0 : (oldCharsInsideRange > oldSpan) ? oldSpan : oldCharsInsideRange;
				
				debugIndent++;
				betterReparse(mark, changeMark, newSpan, balance);
				debugIndent--;
				
				int newConsumed = charactersConsumed();
				int newCharsInsideRange = newConsumed - (changeMark - mark);
				newCharsInsideRange = (newCharsInsideRange < 0) ? 0 : (newCharsInsideRange > newSpan) ? newSpan : newCharsInsideRange;
				
				if (successful())
				{
					// note. if this was not previously part of the success subtree then the new state vector doesn't carry over to the parent this way.
					// of course this is just debugging anyway.
					mark += newConsumed;
					//if (changeMark < mark)
						//changeMark += oldConsumed;
					changeMark += oldCharsInsideRange;
					
					newSpan -= newCharsInsideRange;
					balance += (oldCharsInsideRange - newCharsInsideRange);
					
					String newStateVector =  (successful() ? (" • " + charactersConsumed()) : "") + " !! " + charactersTested()
						+ " ( mark=" + mark + " changeMark=" + changeMark + " newSpan=" + newSpan + " balance=" + balance + " )";
					debug(oldStateVector + " -> " + newStateVector);
				}
			}
			
			// onus is on caller, not this, to skip if the entire mark+charactersTested() is unchanged
			abstract void reparse(int changeMark, int mark, int oldSpanLength, int newSpanLength);
			
			// this is, right now, just a wrapper to track reparse() programming. 
			void doReparse(int changeMark, int mark, int oldSpanLength, int newSpanLength)
			{
				String oldStateVector =  "doReparse: " + rule() + (successful() ? (" • " + charactersConsumed()) : "") + " !! " + charactersTested()
					+ " ( " + changeMark + "," + mark + "," + oldSpanLength + "," + newSpanLength + " )";
				debug(oldStateVector + "...");
				
				// to calc number of chars in range, it's count = (consumed - (changeMark - mark)), 0 <= count <= spanLength

				int oldConsumed = charactersConsumed();
				int oldCharsInsideRange = oldConsumed - (changeMark - mark);
				oldCharsInsideRange = (oldCharsInsideRange < 0) ? 0 : (oldCharsInsideRange > oldSpanLength) ? oldSpanLength : oldCharsInsideRange;
				
				debugIndent++;
				reparse(changeMark, mark, oldSpanLength, newSpanLength);
				debugIndent--;
				
				int newConsumed = charactersConsumed();
				int newCharsInsideRange = newConsumed - (changeMark - mark);
				newCharsInsideRange = (newCharsInsideRange < 0) ? 0 : (newCharsInsideRange > newSpanLength) ? newSpanLength : newCharsInsideRange;
				
				if (successful())
				{
					// note. if this was not previously part of the success subtree then the new state vector doesn't carry over to the parent this way.
					// of course this is just debugging anyway.
					mark += newConsumed;
					//if (newCharsInsideRange != 0)
					if (changeMark < mark)
						changeMark = mark;

					oldSpanLength -= oldCharsInsideRange;
					newSpanLength -= newCharsInsideRange;
					
					String newStateVector =  (successful() ? (" • " + charactersConsumed()) : "") + " !! " + charactersTested()
						+ " ( " + changeMark + "," + mark + "," + oldSpanLength + "," + newSpanLength + " )";
					debug(oldStateVector + " -> " + newStateVector);
				}
			}
		}
	}

	class Terminal extends Rule
	{
		String value;
		Terminal(String v)
		{
			value = v;
		}
		public String toString()
		{
			return "'" + value + "'";
		}
		
		Result parse(int mark)
		{
			return new TerminalResult(mark);
		}
		
		class TerminalResult extends Result
		{
			boolean match;
			
			TerminalResult(int mark)
			{
				match = (value.length() <= text.length() - mark) && text.substring(mark, mark + value.length()).equals(value);
			}
			
			void reparse(int changeMark, int mark, int oldSpanLength, int newSpanLength)
			{
				match = (value.length() <= text.length() - mark) && text.substring(mark, mark + value.length()).equals(value);
			}
			
			void betterReparse(int mark, int changeMark, int newSpan, int balance)
			{
				match = (value.length() <= text.length() - mark) && text.substring(mark, mark + value.length()).equals(value);
			}
			
			boolean successful()
				{ return match; }
			int charactersTested()
				{ return value.length(); }
			int charactersConsumed()
				{ return match ? value.length() : 0; }
			void doEmit(int mark, Emitter emitter)
				{ emitter.emit(mark, this); emitter.afterEmit(mark, this); }
		}
	}

	class Choice extends Rule
	{
		Rule[] components;
		
		Choice(Rule[] c)
		{
			components = c;
		}
		/*public String toString()
		{
			return "Choice " + value + (successful() ? (" • " + charactersConsumed()) : "") + " !! " + charactersTested();
		}*/

		// two choices. we could just store charactersTested for the whole concatenation, and if a change overlapped, we'd have to 
		// recheck it all from the beginning. or we could store the individual results that fail, so we only have to skip through and
		// recheck the ones that overlap. we're going to do both, and in a low memory situation we could purge the individual results.
		Result parse(int mark)
		{
			return new ChoiceResult(mark);
		}
		
		class ChoiceResult extends Result
		{
			// any componentResult may be null or not. "forgotten" successes are okay for now.
			// componentCharactersTested must be the max of everything including the first success.
			Result[] componentResults = new Result[components.length];
			int componentResultFailures = 0;
			int componentCharactersTested = 0;
			
			void markAsTested(int t)
				{ if (componentCharactersTested < t) componentCharactersTested = t; }
				
			ChoiceResult(int mark)
			{
				for (int i = 0; i < components.length; i++)
				{
					componentResults[i] = components[i].doParse(mark);
					markAsTested(componentResults[i].charactersTested());
						
					if (componentResults[i].successful() == true)
						break;

					componentResultFailures++;
				}
			}
			
			void reparse(int changeMark, int mark, int oldSpanLength, int newSpanLength)
			{
				componentCharactersTested = 0;
				componentResultFailures = 0;
				
				for (int i = 0; i < components.length; i++)
				{
					// each possibility may or may not need to be rechecked
					if (componentResults[i] == null)
						componentResults[i] = components[i].doParse(mark);
					else if (mark + componentResults[i].charactersTested() > changeMark)
						componentResults[i].doReparse(changeMark, mark, oldSpanLength, newSpanLength);
					
					markAsTested(componentResults[i].charactersTested());
					if (componentResults[i].successful())
					{
						// later results, whether successful or failed, that overlap the change range need to be either
						// reparsed (which might be a waste of time) or invalidated (because they would no longer be valid).
						for (int j = i + 1; j < components.length; j++)
							if (componentResults[j] != null && mark + componentResults[j].charactersTested() > changeMark)
								componentResults[j] = null;
						break;
					}
					
					componentResultFailures++;
				}
			}
			
			void betterReparse(int mark, int changeMark, int newSpan, int balance)
			{
				componentCharactersTested = 0;
				componentResultFailures = 0;
				
				for (int i = 0; i < components.length; i++)
				{
					// each possibility may or may not need to be rechecked
					if (componentResults[i] == null)
						componentResults[i] = components[i].doParse(mark);
					else if (mark + componentResults[i].charactersTested() > changeMark)
						componentResults[i].doBetterReparse(mark, changeMark, newSpan, balance);
					
					markAsTested(componentResults[i].charactersTested());
					if (componentResults[i].successful())
					{
						// later results, whether successful or failed, that overlap the change range need to be either
						// reparsed (which might be a waste of time) or invalidated (because they would no longer be valid).
						for (int j = i + 1; j < components.length; j++)
							if (componentResults[j] != null && mark + componentResults[j].charactersTested() > changeMark)
								componentResults[j] = null;
						break;
					}
					
					componentResultFailures++;
				}
			}

			boolean successful()
				{ return (componentResultFailures != components.length); }
			int charactersTested()
				{ return componentCharactersTested; }
			int charactersConsumed()
				{ return (componentResultFailures != components.length) ? componentResults[componentResultFailures].charactersConsumed() : 0; }
			void doEmit(int mark, Emitter emitter)
				{ emitter.emit(mark, this); if (successful()) componentResults[componentResultFailures].doEmit(mark, emitter); emitter.afterEmit(mark, this); }
		}
	}

	class Concatenation extends Rule
	{
		Rule[] components;
		
		Concatenation(Rule[] c)
		{
			components = c;
		}
		
		// two choices. we could just store charactersTested for the whole concatenation, and if a change overlapped, we'd have to 
		// recheck it all from the beginning. or we could store the individual results that fail, so we only have to skip through and
		// recheck the ones that overlap. we're going to do both, and in a low memory situation we could purge the individual results.
		Result parse(int mark)
		{
			return new ConcatenationResult(mark);
		}
		
		class ConcatenationResult extends Result
		{
			Result[] componentResults = new Result[components.length];
			int componentResultSuccesses = 0;
			int componentCharactersTested = 0;
			int componentCharactersConsumed = 0;
			
			void markAsTested(int t)
				{ if (componentCharactersTested < t) componentCharactersTested = t; }

			ConcatenationResult(int mark)
			{
				int originalMark = mark;
				int i;
				
				for (i = 0; i < components.length; i++)
				{
					componentResults[i] = components[i].doParse(mark);
					markAsTested((mark - originalMark) + componentResults[i].charactersTested());
									
					if (componentResults[i].successful() == false)
						break;
					
					mark += componentResults[i].charactersConsumed();
					componentResultSuccesses++;
				}
				
				componentResultSuccesses = i;
				componentCharactersConsumed = (mark - originalMark);
			}
			
			void reparse(int changeMark, int mark, int oldSpanLength, int newSpanLength)
			{
				int originalMark = mark;
				int i;
				
				componentCharactersTested = 0;
				
				String stateVector = " ( " + changeMark + "," + mark + "," + oldSpanLength + "," + newSpanLength + " )";

				retester: for (i = 0; i < components.length; i++)
				{
					int oldConsumed = 0;
					int oldCharsInsideRange = 0;
					
					if (componentResults[i] == null)
					{
						componentResults[i] = components[i].doParse(mark);
					}
					else 
					{
						oldConsumed = componentResults[i].charactersConsumed();
						oldCharsInsideRange = oldConsumed - (changeMark - mark);
						oldCharsInsideRange = (oldCharsInsideRange < 0) ? 0 : (oldCharsInsideRange > oldSpanLength) ? oldSpanLength : oldCharsInsideRange;
					
						if (mark + componentResults[i].charactersTested() <= changeMark)
						{
							markAsTested((mark - originalMark) + componentResults[i].charactersTested());
							mark += oldConsumed;
							continue;
						}
						
						// perform the reparse. we don't know if anything was changed.
						componentResults[i].doReparse(changeMark, mark, oldSpanLength, newSpanLength);
					}
						
					markAsTested((mark - originalMark) + componentResults[i].charactersTested());
					
					int newConsumed = componentResults[i].charactersConsumed();
					int newCharsInsideRange = newConsumed - (changeMark - mark);
					newCharsInsideRange = (newCharsInsideRange < 0) ? 0 : (newCharsInsideRange > newSpanLength) ? newSpanLength : newCharsInsideRange;
					
					if (componentResults[i].successful() == false)
					{							
						componentResultSuccesses = i;
						componentCharactersConsumed = 0;

						// it's possible that some results after this one are still valid, but we ditch them completely instead of pointlessly reparsing them.
						for (int j = i + 1; j < components.length; j++)
							componentResults[j] = null;
						return;
					}

					mark += newConsumed;
					//if (newCharsInsideRange != 0)
					if (changeMark < mark)
						changeMark = mark;
					
					oldSpanLength -= oldCharsInsideRange;
					newSpanLength -= newCharsInsideRange;
					
					String newStateVector = " ( " + changeMark + "," + mark + "," + oldSpanLength + "," + newSpanLength + " )";
					debug("concatenation.reparse: " + stateVector + " -> " + newStateVector);
					stateVector = newStateVector;

					/*if (oldSpanLength == 0 && newSpanLength == 0)
					{
						// it's time to bail out of the reparse since we know nothing else could have changed. 						 
						while (++i < componentResultSuccesses)
						{
							debug("concatenation.reparse: after 0,0, skipping " + componentResults[i].rule());
							markAsTested((mark - originalMark) + componentResults[i].charactersTested());
							mark += componentResults[i].charactersConsumed();
						} 
						
						break retester;
					}*/
				}
				
				componentResultSuccesses = i;
				componentCharactersConsumed = (mark - originalMark);
			}

			void betterReparse(int mark, int changeMark, int newSpan, int balance)
			{
				int originalMark = mark;
				int i;
				
				componentCharactersTested = 0;
				String stateVector =  " ( mark=" + mark + " changeMark=" + changeMark + " newSpan=" + newSpan + " balance=" + balance + " )";
				
				retester: for (i = 0; i < components.length; i++)
				{
					int oldConsumed = 0;
					int oldCharsInsideRange = 0;
					
					if (componentResults[i] == null)
					{
						componentResults[i] = components[i].doParse(mark);
					}
					else 
					{
						oldConsumed = componentResults[i].charactersConsumed();
						oldCharsInsideRange = oldConsumed - (changeMark - mark);
						int oldSpan = newSpan - balance;
						oldCharsInsideRange = (oldCharsInsideRange < 0) ? 0 : (oldCharsInsideRange > oldSpan) ? oldSpan : oldCharsInsideRange;
					
						if (mark + componentResults[i].charactersTested() <= changeMark)
						{
							markAsTested((mark - originalMark) + componentResults[i].charactersTested());
							mark += oldConsumed;
							
							stateVector = " ( mark=" + mark + " changeMark=" + changeMark + " newSpan=" + newSpan + " balance=" + balance + " )";
							debug("concatenation.betterReparse: test not in range, skipping " + componentResults[i].rule() + " ->" + stateVector);
							continue;
						}
						
						// perform the reparse. we don't know if anything was changed.
						componentResults[i].doBetterReparse(mark, changeMark, newSpan, balance);
					}
						
					markAsTested((mark - originalMark) + componentResults[i].charactersTested());
					
					int newConsumed = componentResults[i].charactersConsumed();
					int newCharsInsideRange = newConsumed - (changeMark - mark);
					newCharsInsideRange = (newCharsInsideRange < 0) ? 0 : (newCharsInsideRange > newSpan) ? newSpan : newCharsInsideRange;
					
					if (componentResults[i].successful() == false)
					{							
						componentResultSuccesses = i;
						componentCharactersConsumed = 0;

						// it's possible that some results after this one are still valid, but we ditch them completely instead of pointlessly reparsing them.
						for (int j = i + 1; j < components.length; j++)
							componentResults[j] = null;
						return;
					}

					if (changeMark < mark + oldConsumed)
						changeMark += oldConsumed;
						
					mark += newConsumed;
					
					newSpan -= newCharsInsideRange;
					balance += (oldCharsInsideRange - newCharsInsideRange);
					
					String newStateVector = " ( mark=" + mark + " changeMark=" + changeMark + " newSpan=" + newSpan + " balance=" + balance + " )";
					debug("concatenation.betterReparse: " + stateVector + " -> " + newStateVector);
					stateVector = newStateVector;

					if (newSpan == 0 && balance == 0)
					{
						// it's time to bail out of the reparse since we know nothing else could have changed. 
						 
						while (++i < componentResultSuccesses)
						{
							debug("concatenation.betterReparse: after 0,0, skipping " + componentResults[i].rule());
							markAsTested((mark - originalMark) + componentResults[i].charactersTested());
							mark += componentResults[i].charactersConsumed();
						} 
						
						break retester;
					}
				}
				
				componentResultSuccesses = i;
				componentCharactersConsumed = (mark - originalMark);
			}

			boolean successful()
				{ return (componentResultSuccesses == components.length); }
			int charactersTested()
				{ return componentCharactersTested; }
			int charactersConsumed()
				{ return componentCharactersConsumed; }
			void doEmit(int mark, Emitter emitter)
			{ 
				emitter.emit(mark, this); 
				//System.out.println("emitting " + componentResultSuccesses);
				for (int i = 0; i < componentResultSuccesses; i++)
				{
					componentResults[i].doEmit(mark, emitter);
					mark += componentResults[i].charactersConsumed();
				}
				emitter.afterEmit(mark, this); 
			}
		}
	}

	class Repetition extends Rule
	{
		Rule rule;
		int minimum, maximum;		// maximum can be 0, meaning no maximum
		
		Repetition(int l, int u, Rule r)
		{
			rule = r;
			minimum = l;
			maximum = u;
		}

		Result parse(int mark)
		{
			return new RepetitionResult(mark);
		}
		
		class RepetitionResult extends Result
		{
			// i'm going to support nulls in the list, as placeholders for previously parsed items
			ArrayList<Result> resultList = new ArrayList<Result>();
			int componentCharactersTested = 0;
			int componentCharactersConsumed = 0;
			
			void markAsTested(int t)
				{ if (componentCharactersTested < t) componentCharactersTested = t; }

			RepetitionResult(int mark)
			{
				int originalMark = mark;
				
				while (maximum == 0 || resultList.size() < maximum)
				{
					Result thisResult = rule.doParse(mark);
					markAsTested((mark - originalMark) + thisResult.charactersTested());

					// the final failure is included in charactersTested
					if (thisResult.successful() == false)
						break;
						
					resultList.add(thisResult);
					mark += thisResult.charactersConsumed();
				}
				
				componentCharactersConsumed = mark - originalMark;
			}
			void reparse(int changeMark, int mark, int oldSpanLength, int newSpanLength)
			{
				int originalMark = mark;
				componentCharactersTested = 0;
				
				retester: for (int i = 0; i < resultList.size(); i++)
				{
					Result thisResult = resultList.get(i);
					int oldConsumed = 0;
					int oldCharsInsideRange = 0;

					String oldStateVector =  "( " + changeMark + "," + mark + "," + oldSpanLength + "," + newSpanLength + " )";

					if (thisResult == null)
					{
						thisResult = rule.doParse(mark);
						resultList.set(i, thisResult);
					}
					else 
					{
						if (mark + thisResult.charactersTested() <= changeMark)
						{
							markAsTested((mark - originalMark) + thisResult.charactersTested());
							mark += thisResult.charactersConsumed();
							continue;
						}
						
						oldConsumed = thisResult.charactersConsumed();
						oldCharsInsideRange = oldConsumed - (changeMark - mark);
						oldCharsInsideRange = (oldCharsInsideRange < 0) ? 0 : (oldCharsInsideRange > oldSpanLength) ? oldSpanLength : oldCharsInsideRange;
					
						thisResult.doReparse(changeMark, mark, oldSpanLength, newSpanLength);
					}

					markAsTested((mark - originalMark) + thisResult.charactersTested());
					
					int newConsumed = thisResult.charactersConsumed();
					int newCharsInsideRange = newConsumed - (changeMark - mark);
					newCharsInsideRange = (newCharsInsideRange < 0) ? 0 : (newCharsInsideRange > newSpanLength) ? newSpanLength : newCharsInsideRange;

					if (thisResult.successful() == false)
					{							
						// it's possible that some results after this one are still valid, but we ditch them completely for simplicity.
						while (resultList.size() > i)
							resultList.remove(i);
							
						componentCharactersConsumed = mark - originalMark;
						return;
					}
					
					mark += newConsumed;
					if (newCharsInsideRange != 0)
						changeMark = mark;
					oldSpanLength -= oldCharsInsideRange;
					newSpanLength -= newCharsInsideRange;
					
					// okay, what just happened?
					
					String newStateVector =  "( " + changeMark + "," + mark + "," + oldSpanLength + "," + newSpanLength + " )";
					debug("repetition.reparse: " + oldStateVector + "->" + newStateVector);
				}
				
				// the final failure, if it exists, is saved in charactersTested, so we'll just parse it again
				while (maximum == 0 || resultList.size() < maximum)
				{
					Result thisResult = rule.doParse(mark);
					markAsTested((mark - originalMark) + thisResult.charactersTested());

					if (thisResult.successful() == false)
						break;
						
					resultList.add(thisResult);
					mark += thisResult.charactersConsumed();
				}
				
				componentCharactersConsumed = (mark - originalMark);
			}

			boolean successful()
				{ return (resultList.size() >= minimum); }
			int charactersTested()
				{ return componentCharactersTested; }
			int charactersConsumed()
				{ return successful() ? componentCharactersConsumed : 0; }
			void doEmit(int mark, Emitter emitter)
			{ 
				emitter.emit(mark, this); 
				for (int i = 0; i < resultList.size(); i++)
				{
					resultList.get(i).doEmit(mark, emitter);
					mark += resultList.get(i).charactersConsumed();
				}
				emitter.afterEmit(mark, this); 
			}
		}
	}
	
	static class GrammarParser extends CFG
	{
		Rule alphaUnderscore, numeric, alphaUnderscoreNumeric, literal, terminal, space, reference, syntax;
		Choice definitionTerm;
		Concatenation definition;

		GrammarParser()
		{
			Terminal[] alphaUnderscoreList = new Terminal[53];
			for (int i = 0; i < 26; i++)
			{
				alphaUnderscoreList[2*i] = getTerminal("" + (char)('A' + i));
				alphaUnderscoreList[2*i + 1] = getTerminal("" + (char)('a' + i));
			}
			alphaUnderscoreList[52] = getTerminal("_");
		 	alphaUnderscore = new Choice(alphaUnderscoreList);
			
			Terminal[] numericList = new Terminal[10];
			for (int i = 0; i < 10; i++)
				numericList[i] = getTerminal("" + (char)('0' + i));
			numeric = new Choice(numericList);
			
			alphaUnderscoreNumeric = new Choice(new Rule[] { alphaUnderscore, numeric });
			
			literal = new Concatenation(new Rule[] { alphaUnderscore, new Repetition(0, 0, alphaUnderscoreNumeric) });
			
			Rule anyExceptApostrophe = new Rule() 
			{
				Result parse(final int mark)
				{
					//System.out.println("anyExceptApostrophe parsing " + (mark + 1 <= text.length() ? text.substring(mark, mark + 1) : "fail"));
					return new Result() 
					{
						boolean match = (mark + 1 <= text.length()) && (text.substring(mark, mark + 1).equals("'") == false);
						
						void reparse(int changeMark, int mark, int oldSpanLength, int newSpanLength)
						{
							match = (mark + 1 <= text.length()) && (text.substring(mark, mark + 1).equals("'") == false);
						}
						
						boolean successful()
							{ return match; }
						int charactersTested()
							{ return 1; }
						int charactersConsumed()
							{ return match ? 1 : 0; }
						void doEmit(int mark, Emitter emitter)
							{ emitter.emit(mark, this); emitter.afterEmit(mark, this); }
					};
				}
			}; 
			terminal = new Concatenation(new Rule[] { getTerminal("'"), new Repetition(0, 0, anyExceptApostrophe), getTerminal("'") });

			space = new Repetition(0, 0, getTerminal(" "));
			
			reference = new Concatenation(new Rule[] { space, new Choice(new Rule[] { terminal, literal} ), space });
			
			// term : "(" definition ")" | "{" definition "}" | "[" definition "]" | {definition}
			// definition : term {[ "|" term ]}
			
			definitionTerm = new Choice(null);
			definition = new Concatenation(null); 

			// () for grouping, {} for one-or-more, [] for optional, {[ ]} or [{ }] for zero-or-more, A B C for concatenation, D | E | F for choice
			definitionTerm.components = new Rule[] { 
				new Concatenation( new Rule[] { getTerminal("("), definition, getTerminal(")") } ),
				new Concatenation( new Rule[] { getTerminal("{"), definition, getTerminal("}") } ),
				new Concatenation( new Rule[] { getTerminal("["), definition, getTerminal("]") } ),
				reference
				};
			
			definition.components = new Rule[] { 
				new Repetition(1,0,definitionTerm),
				new Repetition(0,0, new Concatenation( new Rule[] { space, getTerminal("|"), space, new Repetition(1,0,definitionTerm), space } ))
			};
			
			syntax = new Repetition(1,0, 
				new Concatenation(new Rule[] { space, literal, space, getTerminal(":"), space, definition, new Repetition(0,0, getTerminal("\n")) })
			);
		}
		
		void installRules(CFG cfg, String rules)
		{
			// input the rules
			init(syntax, rules);
			
			// now the emitter needs to install rules into cfg
		}
	}
	
	Rule inputTextGrammar(String grammar)
	{
		/*
		
		Rules. () for grouping. [] is optional. {} means one or more. [{ }] means zero or more
		
		alphaUnderscore: 'A' | 'a' | 'B' | ... | '_'
		numeric: '0' | ... | '9'
		alphaUnderscoreNumeric: alphaUnderscore | numeric
		literal: alphaUnderscore [{alphaUnderscoreNumeric}]
		anyExceptApostrophe: <any char except apostrophe>
		terminal: <apostrophe> [{anyExceptApostrophe}] <apostrophe>
		space: [{ ' ' }]
		reference: space (terminal | literal) space
		definitionTerm: ('(' definition ')') | ('{' definition '}') | ('[' definition ']') | reference
		definition: {definitionTerm} [{ '|' {definitionTerm} }]
		syntax: space literal space ':' space definition {[ <linefeed> ]}
		
		
		OK, time to think about how to get the data out. The reasonable way to do this is to build a record as we parse with the important info; kind of a subgraph
		of the syntax tree. Remember we have Terminal, Concatenation, Choice, and Repetition
		
		syntax: literal definition (literal selects the record)
		definition: definitionTerm (both the initial and the repeats add to the choice and the concatenation)
		definitionTerm: the bracket types as respectively Rule, Repetition(1,0), Repetition(0,1) or the reference as adding to the choice or concatenation 
		
		All this suggests a dot syntax if you will
		
		syntax.literal -> go to an existing name record, or save a new name record
		definition -> make a new rule that could end up being in any of the forms you don't know yet
		definitionTerm.definition -> we know the forms here for Rule and two kinds of Reps that will be the definition
		definitionTerm.reference -> we're going to add either .terminal or .literal to the current rule
		definition.| -> make this rule a choice or make it the first option of a new choice
		
		For purity-of-current-scheme reasons: I would like something that respects scope. It would be good if the code which handled "hits" used ruleName as the code
		for the just-parsed data that still has no larger context at all it can reference, while using something.ruleName as the context that absorbs IT. But .. it can't
		access something!
		
		OK, a rule that only has code when it is a subrule, like literal of syntax, 
		
		new EmittingRule(literal) { 
			syntax.new Emitter(literal) { public void emit() { 
				currentRule = findOrCreateRule(text());
				} };

			definition.new Emitter() { public void emit() { 
				currentRule = findOrCreateRule(text());
				} };
			
			
		
		
		*/
		
		
		grammar = "S : ('dan gelder' | 'dan') [' apples' | ' carrot'] ' farms'";
		
		GrammarParser gp = new GrammarParser();
		gp.installRules(this, grammar);
		gp.printOutline();
		
		//gp.init("dan gelder carrot farms");
		
		return null;
	}

}

