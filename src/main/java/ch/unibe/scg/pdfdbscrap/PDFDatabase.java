package ch.unibe.scg.pdfdbscrap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PDF databases. And how to scrap 'em.
 */
public enum PDFDatabase {

	/**
	 * The ACM Digital Library.
	 *
	 * @see <a href="http://dl.acm.org/">http://dl.acm.org/</a>
	 */
	ACM() {
				@Override
				public boolean isDatabaseByURL(String url) {
					return (url.contains("doi.acm.org") || url.contains("dl.acm.org"));
				}

				@Override
				public Map<ScrapMode, List<String>> getScrapConfigurations() {
					return ACM_CONFIG;
				}
			},
	/**
	 * The Digital Object Identifier (DOI) system.
	 *
	 * @see <a href="http://www.doi.org/">http://www.doi.org/</a>
	 * @see <a href="http://dx.doi.org/">http://dx.doi.org/</a>
	 */
	DOI() {
				@Override
				public boolean isDatabaseByURL(String url) {
					return url.contains("dx.doi.org");
				}

				@Override
				public Map<ScrapMode, List<String>> getScrapConfigurations() {
					return DOI_CONFIG;
				}
			},
	/**
	 * IEEE Xplore Digital Library.
	 *
	 * @see
	 * <a href="http://ieeexplore.ieee.org/">http://ieeexplore.ieee.org/</a>
	 */
	IEEE() {
				@Override
				public boolean isDatabaseByURL(String url) {
					return url.contains("ieeexplore.ieee.org");
				}

				@Override
				public Map<ScrapMode, List<String>> getScrapConfigurations() {
					return IEEE_CONFIG;
				}
			},
	/**
	 * ScienceDirect.
	 *
	 * @see
	 * <a href="http://www.sciencedirect.com/">http://www.sciencedirect.com/</a>
	 */
	SCIENCEDIRECT() {
				@Override
				public boolean isDatabaseByURL(String url) {
					return url.contains("www.sciencedirect.com");
				}

				@Override
				public Map<ScrapMode, List<String>> getScrapConfigurations() {
					return SCIENCEDIRECT_CONFIG;
				}
			},
	/**
	 * Unknown/unidentified database. "You know, Hobbes, some days even my lucky
	 * rocketship underpants don’t help." -- Bill Watterson, Calvin & Hobbes.
	 */
	UNKNOWN() {
				@Override
				public boolean isDatabaseByURL(String url) {
					return false;
				}

				@Override
				public Map<ScrapMode, List<String>> getScrapConfigurations() {
					return UNKNOWN_CONFIG;
				}
			};

	private final static Map<ScrapMode, List<String>> ACM_CONFIG = new HashMap<>();
	private final static Map<ScrapMode, List<String>> IEEE_CONFIG = new HashMap<>();
	private final static Map<ScrapMode, List<String>> DOI_CONFIG = new HashMap<>();
	private final static Map<ScrapMode, List<String>> SCIENCEDIRECT_CONFIG = new HashMap<>();
	private final static Map<ScrapMode, List<String>> UNKNOWN_CONFIG = new HashMap<>();

	static {
		ACM_CONFIG.put(
				ScrapMode.XPATH_ANCHOR,
				Arrays.asList(
						"//a[@name='FullTextPDF']"
				)
		);
		IEEE_CONFIG.put(
				ScrapMode.FRAME_SRC,
				Arrays.asList(
						"//a[contains(@class, 'stats-document-lh-action-downloadPdf_2')]"
				)
		);
		DOI_CONFIG.put(ScrapMode.FRAME_SRC, IEEE_CONFIG.get(ScrapMode.FRAME_SRC));
		DOI_CONFIG.put(ScrapMode.XPATH_ANCHOR, ACM_CONFIG.get(ScrapMode.XPATH_ANCHOR));
		SCIENCEDIRECT_CONFIG.put(
				ScrapMode.XPATH_ANCHOR,
				Arrays.asList(
						"//a[@id='pdfLink']",
						"//a[@class='download-pdf-link']"
				)
		);
		UNKNOWN_CONFIG.put(ScrapMode.FRAME_SRC, Collections.EMPTY_LIST);
	}

	/**
	 * Check whether the given URL points to this database.
	 *
	 * @param url the URL of a BibTeX entry.
	 * @return {@code true} if the URL points to this database, {@code false}
	 * otherwise.
	 */
	abstract public boolean isDatabaseByURL(String url);

	/**
	 * Returns the scrap(ing) configurations. Ideally only one scrap(ing)
	 * configuration is needed, but feel free to define more as fallback
	 * scrap(ing) modes, in case the previous one didn't work.
	 *
	 * @return the scrap(ing) configurations, which is a map of scrap(ing) modes
	 * associated with a list of XPath expressions.
	 */
	abstract public Map<ScrapMode, List<String>> getScrapConfigurations();

	/**
	 * Attempts to return a known PDF database, given the URL of a BibTeX entry.
	 *
	 * @param url the URL of a BibTeX entry.
	 * @return the known PDF database, or {@code UNKNOWN}.
	 */
	public static PDFDatabase getPDFDatabase(String url) {
		for (PDFDatabase db : values()) {
			if (db.isDatabaseByURL(url)) {
				return db;
			}
		}
		return UNKNOWN;
	}

}
