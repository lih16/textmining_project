import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;

import org.xml.sax.SAXException;

public class PdfParse {

   public static void main(final String[] args) throws IOException,TikaException, SAXException {

      BodyContentHandler handler = new BodyContentHandler();
      Metadata metadata = new Metadata();
      BufferedWriter bw = null;
	  FileWriter fw = null;
 	  try {
			fw = new FileWriter(args[1]);
			bw = new BufferedWriter(fw);
   	  } catch (IOException e) {
    	    System.out.println("Please dev1 test check your output folder "+e.getMessage());
      }
      FileInputStream inputstream = new FileInputStream(new File(args[0]));
      ParseContext pcontext = new ParseContext();
      PDFParser pdfparser = new PDFParser(); 
      pdfparser.parse(inputstream, handler, metadata,pcontext);
      bw.write(handler.toString());
      bw.close();
      
    
   }
}