import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Process plain text files that were generated using pdftotext (version 0.12.4, http://poppler.freedesktop.org).
 * <br/><br/>
 * - merge lines to recover split sentences<br/>
 * - replace special characters (UTF8 to ASCII)<br/>
 * 
 * 
 * @author joerghakenberg
 *
 */


public class PostProcessPdfToText {

	public static String replaceSpecialChars (String line) {
		line = line.replaceAll("’", "'");
		line = line.replaceAll("–", "-");
		line = line.replaceAll("†", "*");
		line = line.replaceAll("?", "*");
		line = line.replaceAll("‡", "*");
		line = line.replaceAll("§", "*");
		line = line.replaceAll("©", "(c)");
		line = line.replaceAll("ª", "(c)");
		line = line.replaceAll("?", "µ");
		line = line.replaceAll("?", "+");
		line = line.replaceAll("?", "-");
		line = line.replaceAll("?", "º"); //
		line = line.replaceAll("?", "<");
		line = line.replaceAll("°", "º");
		line = line.replaceAll("„", "n");
		line = line.replaceAll("•", "*");
		line = line.replaceAll("”", "");
		line = line.replaceAll("“", "");
		line = line.replaceAll("‘", "'");
		line = line.replaceAll("À", "-");
		line = line.replaceAll("±", "+/-");
				
		// Greek symbols:
		line = line.replaceAll("?", "alpha");
		line = line.replaceAll("a", "alpha");
		line = line.replaceAll("?", "beta");
		line = line.replaceAll("ß", "beta");
		line = line.replaceAll("?", "gamma");
		line = line.replaceAll("?", "delta");
		line = line.replaceAll("?", "delta");
		line = line.replaceAll("?", "epsilon");
		line = line.replaceAll("?", "kappa");
		
		// ligatures:
		line = line.replaceAll("?", "fi");
		line = line.replaceAll("?", "fl");
		line = line.replaceAll("?", "ff");
		
		//
		//
		//
		//line = line.replaceAll("", "");
		//line = line.replaceAll("", "");
		
		//
		line = line.replaceAll("\\t", " ");
		line = line.replaceAll("\\s\\s+", " ");
		//line = line.replaceAll("\\?", "_");
		//line = line.replaceAll("\\*", "_");
		//line = line.replaceAll("\\+", "_");
		
		return line;
	}
	
	
	public static void main (String[] args) {
		
		String infile = args[0];
		
		List<String> content = new LinkedList<String>();
		try {
			BufferedReader in = new BufferedReader(new FileReader(infile));		
			String line;
			Pattern p_multiline_heading = Pattern.compile("^[\r\n\t\\s]*[A-Z]([a-z]+|[A-Z]+)[\r\n\t\\s]*$", Pattern.MULTILINE);
			Matcher m; // = p_multiline_heading.matcher("");
			while ((line = in.readLine()) != null) {
				//if (line.equalsIgnoreCase("References") || line.toLowerCase().startsWith("acknowledgment")) break;
				m = p_multiline_heading.matcher(line);
				if (m.matches()) continue;
				
				// Meng remove '//'
				line = line.replaceAll("(?ms)[\r\n]+", "");
				
				
				if (line.equals("`")) continue;
				if (line.matches("[\r\n\t ]+")) continue;
				if (line.trim().length() == 0) continue;
				
				// Modification by Meng, declspec replaceSpecialChars  
				//line = replaceSpecialChars(line);
				
				//
				// Modification by Meng, declspec the following five replaceAll functions.
				line = line.replaceAll("\\t", " ");
				line = line.replaceAll("\\s\\s+", " ");
				line = line.replaceAll("\\?", "_");
				line = line.replaceAll("\\*", "_");
				line = line.replaceAll("\\+", "_");
				
				//Meng remove '//'
				if (line.matches("(?ms)[\r\n\t ]*[A-Z][a-z]+[\r\n\t ]*")) continue;
				
				
				if (line.matches("\\d+(\\-\\d+)?")) continue;
				
				// remove references from the end of a sentence
				line = line.replaceAll("([a-z]+)\\.\\d+([-,]\\d+)*( |$)", "$1.$3");
				line = line.replaceAll("([a-z]+)\\d+([-,]\\d+)*\\.( |$)", "$1.$3");

				int clen = content.size();
				if (clen > 0
						&& (line.matches("[a-z0-9µ\\(\\[].*") || line.matches("[A-Z]+([0-9]|[ -]).*"))
						&& content.get(clen - 1).matches("[A-Z].*")
						&& content.get(clen - 1).length() > 20
						) {
					content.set(clen - 1, content.get(clen - 1) + " " + line);
				} else
					content.add(line);
				
				//System.out.println(line);
			}
			in.close();
			in = null;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		for (String l : content)
			System.out.println(l);
		
		
	}
	
}