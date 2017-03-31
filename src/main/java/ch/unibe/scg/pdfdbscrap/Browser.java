package ch.unibe.scg.pdfdbscrap;

import com.gargoylesoftware.htmlunit.BrowserVersion;

/**
 * Available browsers (or browser versions) in HtmlUnit.
 */
public enum Browser {

	/**
	 * Best supported one. Uh.. surprise!
	 */
	BEST_SUPPORTED(BrowserVersion.BEST_SUPPORTED),
	/**
	 * Chrome.
	 */
	CHROME(BrowserVersion.CHROME),
	/**
	 * Edge.
	 */
	EDGE(BrowserVersion.EDGE),
	/**
	 * Firefox.
	 */
	FIREFOX(BrowserVersion.FIREFOX_45),
	/**
	 * Internet Explorer (IE).
	 */
	IE(BrowserVersion.INTERNET_EXPLORER);

	private final BrowserVersion version;

	private Browser(BrowserVersion version) {
		this.version = version;
	}

	/**
	 * Returns the wrapped HtmlUnit's {@code BrowserVersion}.
	 *
	 * @return the wrapped HtmlUnit's {@code BrowserVersion}.
	 */
	public BrowserVersion getVersion() {
		return this.version;
	}

}
