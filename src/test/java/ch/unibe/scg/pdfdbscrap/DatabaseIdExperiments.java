package ch.unibe.scg.pdfdbscrap;

import static ch.unibe.scg.pdfdbscrap.Main.printError;
import static ch.unibe.scg.pdfdbscrap.Main.putResult;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXParser;
import org.jbibtex.ParseException;
import org.jbibtex.TokenMgrException;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Database identification experiments.
 */
public class DatabaseIdExperiments {

	public final static File testBibTeXFile = new File("D:\\inbox\\tmp\\merged-bibsani.bib");

	public void assertBibTeXFileExists() {
		assertTrue("test BibTeX file exists", testBibTeXFile.exists());
	}

	@Test
	public void urlClassifierExperiment() {
		assertBibTeXFileExists();
		final Map<String, List<String>> classes = new HashMap<>();
		int num = 0;
		int numNoUrl = 0;

		try (FileReader reader = new FileReader(testBibTeXFile)) {
			final BibTeXParser bibtexParser = new BibTeXParser();
			final BibTeXDatabase database = bibtexParser.parse(reader);
			final Map<org.jbibtex.Key, BibTeXEntry> entryMap = database.getEntries();
			for (BibTeXEntry e : entryMap.values()) {
				num++;
				final org.jbibtex.Value urlValue = e.getField(BibTeXEntry.KEY_URL);
				final String url;
				if (urlValue != null) {
					url = urlValue.toUserString();
					final String c = getDatabaseByURL(url);
					putValue(c, url, classes);
				} else {
					numNoUrl++;
				}
			}
		} catch (IOException | ParseException | TokenMgrException ex) {
			printError(ex, "ERROR: failed to parse the BibTeX file: " + testBibTeXFile);
		}

		final int numClasses = classes.size();

		System.out.println("num: " + num);
		System.out.println("numNoUrl: " + numNoUrl);
		System.out.println("numClassses: " + numClasses);
		for (String c : classes.keySet()) {
			System.out.println(" - " + c);
		}

		for (Map.Entry<String, List<String>> e : classes.entrySet()) {
			final String c = e.getKey();
			final List<String> urls = e.getValue();
			System.out.println(String.format(
					"%s: %d",
					c, urls.size()
			));
			for (String url : urls) {
				System.out.println(" - " + url);
			}
		}
	}

	public static void putValue(String key, String value, Map<String, List<String>> map) {
		final List<String> list = map.containsKey(key) ? map.get(key) : newList(key, map);
		list.add(value);
	}

	public static List<String> newList(String key, Map<String, List<String>> map) {
		final List<String> list = new ArrayList<>();
		map.put(key, list);
		return list;
	}

	public static String getDatabaseByURL(String url) {
		if (url.contains("www.sciencedirect.com")) {
			return "SCIENCEDIRECT";
		}
		if (url.contains("doi.acm.org") || url.contains("dl.acm.org")) {
			return "ACM";
		}
		if (url.contains("ieeexplore.ieee.org")) {
			return "IEEE";
		}
		// dx.doi.org: 2x from IEEE, 1x from ACM
		if (url.contains("dx.doi.org")) {
			return "DOI";
		}
		return "UNKNOWN";
	}

}
