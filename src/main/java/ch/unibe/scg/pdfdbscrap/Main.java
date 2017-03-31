package ch.unibe.scg.pdfdbscrap;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.PluginConfiguration;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.WebResponse;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXFormatter;
import org.jbibtex.BibTeXParser;
import org.jbibtex.ParseException;
import org.jbibtex.TokenMgrException;

/**
 * PDF database Scrap(er).
 */
public class Main {

	public final static ScrapMode DEFAULT_SCRAPMODE = ScrapMode.AUTO;
	public final static IDCreator DEFAULT_IDCREATOR = IDCreator.NUMBER_AND_KEY;
	public final static String DEFAULT_XPATH_SPLIT_STRING = ";";
	public final static Browser DEFAULT_BROWSER = Browser.CHROME;

	/**
	 * Main method.
	 *
	 * @param args the command line arguments.
	 */
	public static void main(String[] args) {
		// parse command line arguments
		final CommandLineArguments cla = new CommandLineArguments(Main.class, args);
		final CommandLineArguments.Argument fileArg = cla.add(
				"The BibTeX file to process.",
				"<file>",
				"f", "file"
		);
		final CommandLineArguments.Argument rangeArg = cla.add(
				"The range (or just a start offset; inclusive) of BibTeX entries (1 to N) to process.",
				"<integer>[-<integer>] (e.g start to end: \"10-25\", or just a start-offset: \"15\")",
				"r", "range"
		);
		final CommandLineArguments.Argument outArg = cla.add(
				"The output directory.",
				"<file>",
				"o", "out"
		);
		final CommandLineArguments.Argument modeArg = cla.add(
				"The scraping mode.",
				"<string> (" + getValueList(ScrapMode.class) + "; DEFAULT=" + DEFAULT_SCRAPMODE.name() + ")",
				"m", "mode"
		);
		final CommandLineArguments.Argument xpathArg = cla.add(
				"The XPath expression(s) to get to the PDF download link.",
				"<string> (e.g. \"//a[@id='pdfLink']\")",
				"x", "xpath"
		);
		final CommandLineArguments.Argument splitArg = cla.add(
				"The split string to separate multiple XPath expressions.",
				"<string> (DEFAULT=\"" + DEFAULT_XPATH_SPLIT_STRING + "\")",
				"s", "split"
		);
		final CommandLineArguments.Argument idArg = cla.add("The ID method (used for filenames).",
				"<string> (" + getValueList(IDCreator.class) + "; DEFAULT=" + DEFAULT_IDCREATOR.name() + ")",
				"i", "id"
		);
		final CommandLineArguments.Argument numArg = cla.add(
				"The starting number/offset (just used by the ID method).",
				"<integer>",
				"n", "number"
		);
		final CommandLineArguments.Argument browserArg = cla.add("The browser (version) of the headless web client.",
				"<string> (" + getValueList(Browser.class) + "; DEFAULT=" + DEFAULT_BROWSER.name() + ")",
				"b", "browser"
		);
		final CommandLineArguments.Argument usageArg = cla.add(
				"Print the usage of this program.",
				"",
				"u", "usage"
		);

		if (args.length == 0 || usageArg.isSet() || !CommandLineArguments.areAllSet(fileArg, outArg)) {
			cla.printUsage();
			kthxbai();
		}

		final File inputFile = new File(fileArg.getString());
		if (!inputFile.exists()) {
			printError("ERROR: input file does not exists: " + inputFile);
			kthxbai();
		}
		System.out.println("input file: " + inputFile);

		final EntryRange entryRange = rangeArg.isEmpty() ? new EntryRange() : new EntryRange(rangeArg.getString());
		System.out.println("range to process: " + entryRange);

		final File outputDirectory = new File(outArg.getString());
		if (!outputDirectory.exists()) {
			System.out.println("creating output directory: " + outputDirectory);
			outputDirectory.mkdirs();
		} else {
			System.out.println("output directory: " + outputDirectory);
		}

		final ScrapMode scrapMode;
		switch (modeArg.getString().toUpperCase()) {
			case "XPATH_ANCHOR":
				scrapMode = ScrapMode.XPATH_ANCHOR;
				break;
			case "FRAME_SRC":
			case "FRAME_SOURCE":
				scrapMode = ScrapMode.FRAME_SRC;
				break;
			case "AUTO":
				scrapMode = ScrapMode.AUTO;
				break;
			default:
				scrapMode = DEFAULT_SCRAPMODE;
				break;
		}
		System.out.println("scrap(ing) mode: " + scrapMode);

		final String xpathSplitString = splitArg.isEmpty() ? DEFAULT_XPATH_SPLIT_STRING : splitArg.getString();

		final List<String> xpathExpressions;
		if (scrapMode.requiresXPathExpression()) {
			if (xpathArg.isEmpty()) {
				printError("ERROR: XPATH_ANCHOR requires the argument -X, or --XPATH to be set.");
				kthxbai();
			}
			xpathExpressions = new ArrayList<>();
			Collections.addAll(xpathExpressions, xpathArg.getString().split(xpathSplitString));
			final int len = String.format("%d", xpathExpressions.size()).length();
			int i = 1;
			System.out.println("xpath split string: " + xpathSplitString);
			for (String xpath : xpathExpressions) {
				System.out.println(String.format(
						"xpath expression (%" + len + "d): %s",
						i++, xpath
				));
			}
		} else {
			xpathExpressions = Collections.EMPTY_LIST;
		}

		final IDCreator idCreator;
		switch (idArg.getString().toUpperCase()) {
			case "ENTRY_NUMBER":
			case "NUMBER":
				idCreator = IDCreator.ENTRY_NUMBER;
				break;
			case "NUMBER_AND_KEY":
				idCreator = IDCreator.NUMBER_AND_KEY;
				break;
			case "URLENCODED_KEY":
			case "KEY":
				idCreator = IDCreator.URLENCODED_KEY;
				break;
			default:
				idCreator = DEFAULT_IDCREATOR;
				break;
		}
		System.out.println("ID method: " + idCreator.name());

		final int startingNum = numArg.isEmpty() ? 1 : numArg.getInteger();
		System.out.println("ID starting number: " + startingNum);

		final Browser browser;
		switch (browserArg.getString().toUpperCase()) {
			case "BEST_SUPPORTED":
				browser = Browser.BEST_SUPPORTED;
				break;
			case "CHROME":
				browser = Browser.CHROME;
				break;
			case "EDGE":
				browser = Browser.EDGE;
				break;
			case "FIREFOX":
			case "FF":
				browser = Browser.FIREFOX;
				break;
			case "IE":
				browser = Browser.IE;
				break;
			default:
				browser = DEFAULT_BROWSER;
		}

		// TODO: also make an option for this?
		final int numRetries = 3;
		final int retryTimeoutInSeconds = 5;

		// store the BibTeX entries by scrap(ing) status
		final List<List<BibTeXEntry>> results = new ArrayList<>();
		final int numScrapStatus = ScrapStatus.values().length;
		for (int i = 0; i < numScrapStatus; i++) {
			results.add(new ArrayList<>());
		}
		int numBibTeXEntries = -1;

		System.out.print("\n");
		System.out.println("starting up headless web client(s)...");
		// ...but turn of all those warning messages in case we have to enable JavaScript
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF);

		// there seems to be a problem with disabling JavaScript, and turning it
		// back on again occasionally... fuck it! Two web clients it is!
		final WebClient client = new WebClient(browser.getVersion());
		client.getOptions().setCssEnabled(false);
		client.getOptions().setJavaScriptEnabled(false);
		final WebClient jsclient = new WebClient(browser.getVersion());
		jsclient.getOptions().setCssEnabled(false);
		helloWebClient(client, jsclient);

		try (FileReader reader = new FileReader(inputFile)) {
			final BibTeXParser bibtexParser = new BibTeXParser();
			final BibTeXDatabase database = bibtexParser.parse(reader);
			numBibTeXEntries = database.getEntries().size();
			final Map<org.jbibtex.Key, BibTeXEntry> entryMap = database.getEntries();
			final int minmax = Math.min(numBibTeXEntries, entryRange.end);
			final int numOffset = startingNum - 1;
			final int numTotalOffset = minmax + numOffset;
			System.out.println(String.format(
					"processing %d (out of %d) BibTeX entries...",
					(minmax - entryRange.start + 1),
					numBibTeXEntries
			));
			int n = 0;
			int num = numOffset;
			for (BibTeXEntry e : entryMap.values()) {
				n++; // 1..n (used for range filtering)
				num++; // offset + 1..n (used for ID generation)

				// check entry range/processing bounds
				if (n < entryRange.start) {
					continue; // skip
				}
				if (n > entryRange.end) {
					break; // we're done here
				}

				final org.jbibtex.Key keyValue = e.getKey();
				final String key;
				if (keyValue != null) {
					key = keyValue.toString();
				} else {
					putResult(
							ScrapStatus.FAILURE_NO_BIBTEX_KEY,
							IDCreator.ENTRY_NUMBER.getID(num, "", e),
							e, results, outputDirectory
					);
					continue;
				}

				System.out.println(String.format(
						"processing entry %d/%d: %s",
						num,
						numTotalOffset,
						key
				));

				final String identifier = idCreator.getID(num, key, e);

				final org.jbibtex.Value urlValue = e.getField(BibTeXEntry.KEY_URL);
				final String url;
				if (urlValue != null) {
					url = urlValue.toUserString();
				} else {
					putResult(ScrapStatus.FAILURE_NO_BIBTEX_URL, identifier, e, results, outputDirectory);
					continue;
				}

				// attempt to extract the link to the PDF from the web page
				System.out.println("  fetching URL to PDF file from: " + url + "...");
				final String urlToPDF;
				if (scrapMode.requiresXPathExpression()) {
					urlToPDF = ScrapMode.fetchURLToPDFWithRetry(scrapMode, client, jsclient, url, xpathExpressions);
				} else {
					urlToPDF = ScrapMode.fetchURLToPDFWithRetry(scrapMode, client, jsclient, url, Collections.EMPTY_LIST);
				}

				if (urlToPDF.isEmpty()) {
					putResult(ScrapStatus.FAILURE_URL_TO_PDF_NOTFOUND, identifier, e, results, outputDirectory);
					continue;
				}

				// fetch PDF file
				System.out.println("  fetching PDF file from: " + urlToPDF + "...");
				ScrapStatus ret = ScrapStatus.FAILURE_INVALID_URL_TO_PDF;
				int t = 0;
				while (t < numRetries) {
					if (t > 0) {
						try {
							TimeUnit.SECONDS.sleep(retryTimeoutInSeconds);
						} catch (InterruptedException ex) {
							printError(ex);
						}
						System.out.println(String.format(
								"  retrying (%d/%d) to fetch PDF file from: %s",
								t + 1, numRetries,
								urlToPDF
						));
					}
					t++;
					try {
						final Page p = client.getPage(urlToPDF);
						if (p.isHtmlPage()) {
							putResult(ScrapStatus.FAILURE_INVALID_URL_TO_PDF, identifier, e, results, outputDirectory);
							break; // no need to retry
						}
						final UnexpectedPage pdfPage = (UnexpectedPage) p;
						final WebResponse response = pdfPage.getWebResponse();
						final File successDirectory = ScrapStatus.SUCCESS.getStatusDirectory(outputDirectory);
						final File out = new File(
								successDirectory.getAbsolutePath(),
								identifier + ".pdf"
						);
						ret = writeWebResponseToFile(response, out);
						break;
					} catch (FailingHttpStatusCodeException ex) {
						ret = ScrapStatus.FAILURE_FAILING_HTTP_STATUS_CODE;
						printError(ex, "WARNING: failing HTTP status code");
						if (t < numRetries) {
							printError("...trying again in about " + retryTimeoutInSeconds + " seconds.");
						} else {
							printError("...giving up.");
						}
					}
				}

				putResult(ret, identifier, e, results, outputDirectory);
				System.out.print("\n");
			}
		} catch (IOException | ParseException | TokenMgrException ex) {
			printError(ex, "ERROR: failed to parse the BibTeX file: " + inputFile);
		}

		client.close();
		jsclient.close();

		System.out.print("\n");

		final List<BibTeXEntry> successEntries = results.get(ScrapStatus.SUCCESS.ordinal());
		final int numSuccessEntries = successEntries.size();
		System.out.println("number of successfully processed BibTeX entries: " + numSuccessEntries);
		final int numUnprocessedEntries = numBibTeXEntries - numSuccessEntries;
		System.out.println("number of unprocessed BibTeX entries: " + numUnprocessedEntries);

		// write success BibTeX database
		if (numSuccessEntries > 0) {
			writeBibTexEntries(ScrapStatus.SUCCESS, successEntries, inputFile, outputDirectory);
		}

		int numFailedEntries = 0;
		for (int i = 0; i < numScrapStatus; i++) {
			final ScrapStatus status = ScrapStatus.values()[i];
			final List<BibTeXEntry> entries = results.get(i);
			final int n = entries.size();

			// write failure BibTeX databases
			if (n > 0) {
				writeBibTexEntries(status, entries, inputFile, outputDirectory);
			}

			if (n > 0 && !ScrapStatus.SUCCESS.equals(status)) {
				numFailedEntries += n;
				System.out.println(String.format(
						" - %d failed due to %s",
						n, status.name()
				));
			}
		}

		final int total = numFailedEntries + numSuccessEntries;
		if (total != numBibTeXEntries) {
			printError(String.format(
					"WARNING: the number of BibTeX entries (%d) does not match the number of processed BibTeX entries (succeeded=%d, failed=%d, total=%d)!",
					numBibTeXEntries, numSuccessEntries, numFailedEntries, total
			));
		}

		kthxbai();
	}

	public static void kthxbai() {
		System.out.println("\nkthxbai.");
		System.exit(0);
	}

	public static void printError(String... messages) {
		printError(null, messages);
	}

	public static void printError(Exception ex, String... messages) {
		for (String msg : messages) {
			System.err.println(msg);
		}
		if (ex != null) {
			ex.printStackTrace(System.err);
		}
	}

	public static <T extends Enum<T>> String getValueList(Class<T> e) {
		final StringBuilder sb = new StringBuilder();
		final T[] values = e.getEnumConstants();
		for (int i = 0, n = values.length; i < n; i++) {
			final T c = values[i];
			sb.append(c.name());
			if ((i + 1) < n) {
				sb.append(((i + 2) < n) ? ", " : ", or ");
			}
		}
		return sb.toString();
	}

	public static String getBibTeXFilename(File file, ScrapStatus status) {
		final String filename = file.getName();
		final int n = filename.lastIndexOf('.');
		return filename.substring(0, n) + "-" + status.name() + ".bib";
	}

	public static void putResult(ScrapStatus status, String identifier, BibTeXEntry entry, List<List<BibTeXEntry>> results, File outputDirectory) {
		System.out.println("  " + status.name());
		System.out.print("\n");
		results.get(status.ordinal()).add(entry);

		// write BibTeX file
		final File statusDirectory = status.getStatusDirectory(outputDirectory);
		final File bibout = new File(
				statusDirectory.getAbsolutePath(),
				identifier + ".bib"
		);
		System.out.println("  writing BibTeX file to: " + bibout + "...");
		writeBibTeXEntry(entry, bibout);
	}

	public static Writer newFileWriter(File file) throws UnsupportedEncodingException, FileNotFoundException {
		return new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(file),
				StandardCharsets.UTF_8
		));
	}

	public static void writeBibTeXEntry(BibTeXEntry entry, File file) {
		writeBibTeXEntries(Arrays.asList(entry), file);
	}

	public static void writeBibTexEntries(ScrapStatus status, List<BibTeXEntry> entries, File file, File directory) {
		final String filename = getBibTeXFilename(file, status);
		final File dbfile = new File(
				directory.getAbsolutePath(),
				filename
		);
		System.out.println(String.format(
				"writing %s BibTeX database (%d entries) to: %s...",
				status.name(),
				entries.size(),
				dbfile
		));
		writeBibTeXEntries(entries, dbfile);
	}

	public static void writeBibTeXEntries(List<BibTeXEntry> entries, File file) {
		final BibTeXDatabase database = new BibTeXDatabase();
		for (BibTeXEntry entry : entries) {
			database.addObject(entry);
		}
		writeBibTeXDatabase(database, file);
	}

	public static void writeBibTeXDatabase(BibTeXDatabase database, File file) {
		try (Writer writer = newFileWriter(file)) {
			final BibTeXFormatter formatter = new BibTeXFormatter();
			formatter.format(database, writer);
		} catch (UnsupportedEncodingException ex) {
			printError(ex, "ERROR: failed to write database to: " + file);
		} catch (IOException ex) {
			printError(ex, "ERROR: failed to write database to: " + file);
		}
	}

	public static ScrapStatus writeWebResponseToFile(WebResponse response, File out) {
		try (InputStream is = response.getContentAsStream()) {
			try (OutputStream os = new FileOutputStream(out)) {
				byte[] bytes = new byte[4096];
				int read;
				while ((read = is.read(bytes)) >= 0) {
					os.write(bytes, 0, read);
				}
				return ScrapStatus.SUCCESS;
			} catch (IOException ex) {
				System.err.println("ERROR: failed to write to output stream");
				ex.printStackTrace(System.err);
				return ScrapStatus.FAILURE_OUTPUTSTREAM_IO;
			}
		} catch (IOException ex) {
			System.err.println("ERROR: failed to read from input stream");
			ex.printStackTrace(System.err);
			return ScrapStatus.FAILURE_INPUTSTREAM_IO;
		}
	}

	public static void helloWebClient(WebClient client, WebClient jsclient) {
		final BrowserVersion browser = client.getBrowserVersion();
		System.out.println("ApplicationName: " + browser.getApplicationName());
		System.out.println("ApplicationCodeName: " + browser.getApplicationCodeName());
		System.out.println("ApplicationVersion: " + browser.getApplicationVersion());
		System.out.println("ApplicationMinorVersion: " + browser.getApplicationMinorVersion());
		System.out.println("BrowserLanguage: " + browser.getBrowserLanguage());
		System.out.println("BuildId: " + browser.getBuildId());
		System.out.println("CpuClass: " + browser.getCpuClass());
		System.out.println("CssAcceptHeader: " + browser.getCssAcceptHeader());
		System.out.println("HtmlAcceptHeader: " + browser.getHtmlAcceptHeader());
		System.out.println("ImgAcceptHeader: " + browser.getImgAcceptHeader());
		System.out.println("Nickname: " + browser.getNickname());
		System.out.println("Platform: " + browser.getPlatform());
		System.out.println("ScriptAcceptHeader: " + browser.getScriptAcceptHeader());
		System.out.println("SystemLanguage: " + browser.getSystemLanguage());
		System.out.println("UserAgent: " + browser.getUserAgent());
		System.out.println("UserLanguage: " + browser.getUserLanguage());
		System.out.println("Vendor: " + browser.getVendor());
		System.out.println("XmlHttpRequestAcceptHeader: " + browser.getXmlHttpRequestAcceptHeader());
		final int n = browser.getPlugins().size();
		int i = 0;
		System.out.println("Plugins(" + n + "):");
		for (PluginConfiguration pc : browser.getPlugins()) {
			i++;
			System.out.println(String.format(
					"  name=%s\n  version=%s\n  description=%s\n  filename=%s\n  mime-types=%s",
					pc.getName(),
					pc.getVersion(),
					pc.getDescription(),
					pc.getFilename(),
					Arrays.toString(pc.getMimeTypes().toArray())
			));
			if (i < n) {
				System.out.println(" --");
			}
		}

		final WebClientOptions clientOptions = client.getOptions();
		System.out.println("Timeout: " + clientOptions.getTimeout());
		System.out.println("MaxInMemory: " + clientOptions.getMaxInMemory());
		System.out.println("HistorySizeLimit: " + clientOptions.getHistorySizeLimit());
		System.out.println("HistoryPageCacheLimit: " + clientOptions.getHistoryPageCacheLimit());
		System.out.println("isActiveXNative: " + clientOptions.isActiveXNative());
		System.out.println("isAppletEnabled: " + clientOptions.isAppletEnabled());
		System.out.println("isCssEnabled: " + clientOptions.isCssEnabled());
		System.out.println("isDoNotTrackEnabled: " + clientOptions.isDoNotTrackEnabled());
		System.out.println("isDownloadImages: " + clientOptions.isDownloadImages());
		System.out.println("isGeolocationEnabled: " + clientOptions.isGeolocationEnabled());
		System.out.println(String.format(
				"isJavaScriptEnabled: %b | %b",
				clientOptions.isJavaScriptEnabled(),
				jsclient.getOptions().isJavaScriptEnabled()
		));
		System.out.println("isPopupBlockerEnabled: " + clientOptions.isPopupBlockerEnabled());
		System.out.println("isRedirectEnabled: " + clientOptions.isRedirectEnabled());
		System.out.println("isThrowExceptionOnFailingStatusCode: " + clientOptions.isThrowExceptionOnFailingStatusCode());
		System.out.println("isThrowExceptionOnScriptError: " + clientOptions.isThrowExceptionOnScriptError());
		System.out.println("isUseInsecureSSL: " + clientOptions.isUseInsecureSSL());
		System.out.print("\n");
	}

}
