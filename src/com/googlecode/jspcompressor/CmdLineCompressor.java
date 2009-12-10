package com.googlecode.jspcompressor;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import jargs.gnu.CmdLineParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import com.googlecode.jspcompressor.compressor.Compressor;
import com.googlecode.jspcompressor.compressor.JspCompressor;
import com.googlecode.jspcompressor.compressor.XmlCompressor;

/**
 * Wrapper for HTML and XML compressor classes that allows using them from a command line.
 * 
 * <p>Usage: <code>java -jar htmlcompressor.jar [options] [input file]</code>
 * <p>To view a list of all available parameters please run with <code>--help</code> option:
 * <p><code>java -jar htmlcompressor.jar --help</code>
 * 
 * @author <a href="mailto:serg472@gmail.com">Sergiy Kovalchuk</a>
 */
public class CmdLineCompressor {

	public static void main(String[] args) {

		CmdLineParser parser = new CmdLineParser();

		CmdLineParser.Option helpOpt = parser.addBooleanOption('h', "help");
		CmdLineParser.Option charsetOpt = parser.addStringOption("charset");
		CmdLineParser.Option outputFilenameOpt = parser.addStringOption('o', "output");
		CmdLineParser.Option typeOpt = parser.addStringOption("type");
		CmdLineParser.Option preserveCommentsOpt = parser.addBooleanOption("preserve-comments");
		CmdLineParser.Option preserveIntertagSpacesOpt = parser.addBooleanOption("preserve-intertag-spaces");
		CmdLineParser.Option preserveMultiSpacesOpt = parser.addBooleanOption("preserve-multi-spaces");
		CmdLineParser.Option removeIntertagSpacesOpt = parser.addBooleanOption("remove-intertag-spaces");
		CmdLineParser.Option removeQuotesOpt = parser.addBooleanOption("remove-quotes");
		CmdLineParser.Option compressJsOpt = parser.addBooleanOption("compress-js");
		CmdLineParser.Option compressCssOpt = parser.addBooleanOption("compress-css");
        CmdLineParser.Option removeJspComments = parser.addBooleanOption("remove-jsp-comments");
        CmdLineParser.Option preserveStrutsFormComments = parser.addBooleanOption("preserve-struts-comments");

		CmdLineParser.Option nomungeOpt = parser.addBooleanOption("nomunge");
		CmdLineParser.Option linebreakOpt = parser.addStringOption("line-break");
		CmdLineParser.Option preserveSemiOpt = parser.addBooleanOption("preserve-semi");
		CmdLineParser.Option disableOptimizationsOpt = parser.addBooleanOption("disable-optimizations");

		Reader in = null;
		Writer out = null;

		try {

			parser.parse(args);

			// help
			Boolean help = (Boolean) parser.getOptionValue(helpOpt);
			if (help != null && help.booleanValue()) {
				printUsage();
				System.exit(0);
			}

			// charset
			String charset = (String) parser.getOptionValue(charsetOpt);
			if (charset == null || !Charset.isSupported(charset)) {
				charset = System.getProperty("file.encoding");
				if (charset == null) {
					charset = "UTF-8";
				}
			}

			// input file
			String[] fileArgs = parser.getRemainingArgs();

			// type
			String type = (String) parser.getOptionValue(typeOpt);
			if (type != null && !type.equalsIgnoreCase("html") && !type.equalsIgnoreCase("xml")) {
				printUsage();
				System.exit(1);
			}

			if (fileArgs.length == 0) {

				// html by default for stdin
				if (type == null) {
					type = "html";
				}

				in = new InputStreamReader(System.in, charset);

			} else {

				String inputFilename = fileArgs[0];

				// detect type from extension
				if (type == null) {
					int idx = inputFilename.lastIndexOf('.');
					if (idx >= 0 && idx < inputFilename.length() - 1) {
						type = inputFilename.substring(idx + 1);
					}
				}

				if (type == null || !type.equalsIgnoreCase("xml")) {
					type = "html";
				}

				in = new InputStreamReader(new FileInputStream(inputFilename), charset);
			}

			//line break
			int linebreakpos = -1;
			String linebreakstr = (String) parser.getOptionValue(linebreakOpt);
			if (linebreakstr != null) {
				try {
					linebreakpos = Integer.parseInt(linebreakstr, 10);
				} catch (NumberFormatException e) {
					printUsage();
					System.exit(1);
				}
			}

			//output file
			String outputFilename = (String) parser.getOptionValue(outputFilenameOpt);

			//set compressor options
			Compressor compressor = null;
			if (type.equalsIgnoreCase("html")) {

				JspCompressor jspCompressor = new JspCompressor();
				jspCompressor.setRemoveComments(parser.getOptionValue(preserveCommentsOpt) == null);
				jspCompressor.setRemoveMultiSpaces(parser.getOptionValue(preserveMultiSpacesOpt) == null);
				jspCompressor.setRemoveIntertagSpaces(parser.getOptionValue(removeIntertagSpacesOpt) != null);
				jspCompressor.setRemoveQuotes(parser.getOptionValue(removeQuotesOpt) != null);
				jspCompressor.setCompressJavaScript(parser.getOptionValue(compressJsOpt) != null);
				jspCompressor.setCompressCss(parser.getOptionValue(compressCssOpt) != null);

				jspCompressor.setYuiJsNoMunge(parser.getOptionValue(nomungeOpt) != null);
				jspCompressor.setYuiJsPreserveAllSemiColons(parser.getOptionValue(preserveSemiOpt) != null);
				jspCompressor.setYuiJsDisableOptimizations(parser.getOptionValue(disableOptimizationsOpt) != null);
				jspCompressor.setYuiJsLineBreak(linebreakpos);
				jspCompressor.setYuiCssLineBreak(linebreakpos);
                jspCompressor.setSkipStrutsFormComments(preserveStrutsFormComments != null);
                jspCompressor.setRemoveJspComments(removeJspComments != null);

				compressor = jspCompressor;

			} else {

				XmlCompressor xmlCompressor = new XmlCompressor();
				xmlCompressor.setRemoveComments(parser.getOptionValue(preserveCommentsOpt) == null);
				xmlCompressor.setRemoveIntertagSpaces(parser.getOptionValue(preserveIntertagSpacesOpt) == null);

				compressor = xmlCompressor;

			}

			BufferedReader input =  new BufferedReader(in);
			
			//compress
			try {
				
				//read input file
				StringBuilder source = new StringBuilder();
				
				String line = null;
				while ((line = input.readLine()) != null){
					source.append(line);
					source.append(System.getProperty("line.separator"));
				}
				

				// Close the input stream first, and then open the output
				// stream,
				// in case the output file should override the input file.
				input.close();
				input = null;
				in.close();
				in = null;

				if (outputFilename == null) {
					out = new OutputStreamWriter(System.out, charset);
				} else {
					out = new OutputStreamWriter(new FileOutputStream(outputFilename), charset);
				}

				String result = compressor.compress(source.toString());
				out.write(result);

			} catch (Exception e) {

				e.printStackTrace();
				System.exit(1);

			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		} catch (CmdLineParser.OptionException e) {

			printUsage();
			System.exit(1);

		} catch (IOException e) {

			e.printStackTrace();
			System.exit(1);

		} finally {

			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		System.exit(0);
	}

	private static void printUsage() {
		System.out.println("Usage: java -jar jspcompressor.jar [options] [input file]\n\n"

						+ "<input file>                  If not provided reads from stdin\n\n"

						+ "Global Options:\n"
						+ "  -o <output file>            If not provided outputs result to stdout\n"
						+ "  --type <html|xml>           If not provided autodetects from file extension\n"
						+ "  --charset <charset>         Read the input file using <charset>\n"
						+ "  -h, --help                  Display this screen\n\n"
                        + "JSP Options:\n"
                        + "  --remove-jsp-comments       Remove JSP comments\n"
                        + "  --preserve-struts-comments  Preserve <html:form> starting and ending tag comments.\n\n"

						+ "XML Options:\n"
						+ "  --preserve-comments         Preserve comments\n"
						+ "  --preserve-intertag-spaces  Preserve intertag spaces\n\n"

						+ "HTML Options:\n"
						+ "  --preserve-comments         Preserve comments\n"
						+ "  --preserve-multi-spaces     Preserve multiple spaces\n"
						+ "  --remove-intertag-spaces    Remove intertag spaces\n"
						+ "  --remove-quotes             Remove unneeded quotes\n"
						+ "  --compress-js               Enable JavaScript compression using YUICompressor\n"
						+ "  --compress-css              Enable CSS compression using YUICompressor\n\n"

						+ "JavaScript Options (for YUI Compressor):\n"
						+ "  --nomunge                   Minify only, do not obfuscate\n"
						+ "  --preserve-semi             Preserve all semicolons\n"
						+ "  --disable-optimizations     Disable all micro optimizations\n"
						+ "  --line-break <column num>   Insert a line break after the specified column\n\n"

						+ "CSS Options (for YUI Compressor):\n"
						+ "  --line-break <column num>   Insert a line break after the specified column\n\n"
						
						+ "Please note that if you enable JavaScript or Css compression parameters,\n"
						+"YUI Compressor jar file must be present at the same directory as this jar."

				);
	}

}
