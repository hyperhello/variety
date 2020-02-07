import java.math.BigInteger;
import java.math.BigDecimal;

public final class BigRational {
    private BigInteger num;   // the numerator
    private BigInteger den;   // the denominator

    // create and initialize a new Rational object
    public BigRational(int numerator, int denominator) {
        // BigInteger constructor takes a string, not an int
        num = new BigInteger(numerator + "");
        den = new BigInteger(denominator + "");
		if (num.signum() != 0 && den.signum() != 0)
		{
	        BigInteger g = num.gcd(den);
	        num = num.divide(g);
	        den = den.divide(g);
		}
    }

    // create and initialize a new Rational object
    public BigRational(BigInteger numerator, BigInteger denominator) {
        num = numerator;
        den = denominator;
		if (num.signum() != 0 && den.signum() != 0)
		{
	       BigInteger g = num.gcd(den);
	       num = num.divide(g);
	       den = den.divide(g);
		}
   }

    // return string representation of (this)
    public String toString() { 
        if (den.equals(BigInteger.ONE)) return num + "";
        else                            return num + "/" + den;
    }
	
	public double doubleValue()
	{
		return num.doubleValue() / den.doubleValue();
	}
	
    // return a * b
    public BigRational multiply(BigRational b) {
        BigRational a = this;
        BigInteger numerator   = a.num.multiply(b.num);
        BigInteger denominator = a.den.multiply(b.den);
        return new BigRational(numerator, denominator);
    }

    // return a + b
    public BigRational add(BigRational b) {
        BigRational a = this;
        BigInteger numerator   = a.num.multiply(b.den).add(a.den.multiply(b.num));
        BigInteger denominator = a.den.multiply(b.den);
        return new BigRational(numerator, denominator);
    }
    // return a - b
    public BigRational subtract(BigRational b) {
        BigRational a = this;
        BigInteger numerator   = a.num.multiply(b.den).subtract(a.den.multiply(b.num));
        BigInteger denominator = a.den.multiply(b.den);
        return new BigRational(numerator, denominator);
    }

    // return 1 / a
    public BigRational reciprocal() { return new BigRational(den, num);  }

    // return a / b
    public BigRational divide(BigRational b) {
        BigRational a = this;
        return a.multiply(b.reciprocal());
    }

    // return this ^ b
    public BigRational pow(int b) {
        return new BigRational(num.pow(b), den.pow(b));
    }

	// return the base 10 logarithm accurate within 0.001 or so
	public double logTen() {
		double log10 = Math.log(10), log;
		int leadingDigitsAccuracy = 4;
		
		if (num.bitLength() > 16)
		{
			String text = num.toString();
			log = Math.log(new Double(text.substring(0,leadingDigitsAccuracy)))/log10 + (text.length() - leadingDigitsAccuracy);
		}
		else
		{
			log = Math.log(num.doubleValue())/log10;
		}
		
		if (den.bitLength() > 16)
		{
			String text = den.toString();
			log -= Math.log(new Double(text.substring(0,leadingDigitsAccuracy)))/log10 + (text.length() - leadingDigitsAccuracy);
		}
		else
		{
			log -= Math.log(den.doubleValue())/log10;
		}
		
		return log;
	}
	
	// -1 if a < b, 0 if a==b, 1 if a > b
	public int compareTo(BigRational b)
	{
		BigRational a = this;
		int result = a.num.multiply(b.den).compareTo(b.num.multiply(a.den));
		return (a.den.signum() != b.den.signum()) ? (1-result) : result;
	}
	
	public int signum()
	{
		return num.signum() * den.signum();
	}
	
	public BigRational abs()
	{
		return new BigRational((num.signum() == -1) ? num.negate() : num, (den.signum() == -1) ? den.negate() : den);
	}
	
	public String engFormat(boolean enforceSigfigs)
	{
		if (num.signum() == 0)
			return "0";
			
		int log = (int)Math.floor(abs().logTen()), rem = (log + 3000) % 3, uni = log - rem;
		
		BigRational scaler = new BigRational((uni < 0) ? 10 : 1, (uni > 0) ? 10 : 1).pow((int)Math.abs(uni));

		//System.out.println(toString() + " " + log + " " + rem + " " + uni + " " + scaler.toString());

		BigRational sig = this.multiply(scaler);				
		String decimal = new BigDecimal(sig.num, 2-rem).divide(new BigDecimal(sig.den, 2-rem), BigDecimal.ROUND_HALF_UP).toString();
		if (enforceSigfigs == false && decimal.contains("."))
		{
			while (decimal.endsWith("0"))
				decimal = decimal.substring(0, decimal.length() - 1);
			if (decimal.endsWith("."))
				decimal = decimal.substring(0, decimal.length() - 1);
		}

		String[] bigs = new String[] { "K", "M", "G", "T", "P", "E", "Z", "Y" };
		String[] smalls = new String[] { "m", "µ", "n", "p", "f", "a", "z", "y" };
		decimal += (uni == 0) ? "" 
			: (uni > 0 && uni/3-1 < bigs.length) ? bigs[uni/3-1] : (uni < 0 && -uni/3-1 < smalls.length) ? smalls[-uni/3-1] : ("e" + uni);

		return decimal;
	}
	
	
   /*************************************************************************
    *  Computes rational approximation to e using Taylor series
    *
    *      e = 1 + 1/1 + 1/2! + 1/3! + ... + 1/N!
    *
    *************************************************************************/

    public static void main(String[] args) {
	System.out.println(new BigRational(0,0).engFormat(true));
		/*for (int i = 0; i <= 10; i++)
		{
			int b = (int)Math.round(Math.random()*1000), a = (int)Math.round(Math.random()*1000000000);
			System.out.println(a + "," + b + "=" + ((double)a/b) + ": " + new BigRational(a,b).logTen() + " " + (Math.log((double)a/(double)b)/Math.log(10)));
		}
		if (true) return;*/
	
		System.out.println(new BigRational(1, 8).engFormat(true));
		
        int N = 20;//Integer.parseInt(args[0]);
        BigRational r         = new BigRational(1, 1);
        BigRational factorial = new BigRational(1, 1); 
        for (int i = 1; i <= N; i++) {
            factorial = factorial.multiply(new BigRational(i, 1));
            r = r.add(factorial.reciprocal());
          //  System.out.println(r + " = " + r.doubleValue() + " = " + r.multiply(new BigRational(10,1)));
            System.out.println(factorial + " = " + factorial.engFormat(true));
	
        }
    }
}