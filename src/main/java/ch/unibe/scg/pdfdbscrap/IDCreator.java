package ch.unibe.scg.pdfdbscrap;

import static ch.unibe.scg.pdfdbscrap.Main.printError;
import java.io.UnsupportedEncodingException;
import org.jbibtex.BibTeXEntry;

/**
 * Unique identifier creator.
 */
public enum IDCreator {

	/**
	 * Simple ID creator using just the entry number (starting with 1).
	 */
	ENTRY_NUMBER() {
				@Override
				public String getID(int n, String key, BibTeXEntry e) {
					return String.format("%d", n);
				}
			},
	/**
	 * Combined ID using {@code ENTRY_NUMBER} and {@code URLENCODED_KEY}.
	 */
	NUMBER_AND_KEY() {
				@Override
				public String getID(int n, String key, BibTeXEntry e) {
					return String.format(
							"%d_%s",
							n,
							URLENCODED_KEY.getID(n, key, e)
					);
				}
			},
	/**
	 * URL encodes the BibTeX entry key.
	 */
	URLENCODED_KEY() {
				@Override
				public String getID(int n, String key, BibTeXEntry e) {
					try {
						return java.net.URLEncoder.encode(key, "UTF-8");
					} catch (UnsupportedEncodingException ex) {
						final String fallback = ENTRY_NUMBER.getID(n, key, e);
						printError(
								ex,
								"WARNING: failed to URL encode the key: " + key,
								"         falling back to: " + fallback
						);
						return fallback;
					}
				}
			};

	/**
	 * Returns a unique identifier.
	 *
	 * @param n the entry number.
	 * @param key the BibTeX entry key.
	 * @param e the BibTeX entry.
	 * @return a unique identifier.
	 */
	abstract public String getID(int n, String key, BibTeXEntry e);

}
