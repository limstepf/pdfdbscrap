package ch.unibe.scg.pdfdbscrap;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.BaseFrameElement;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Scrap(ing) mode. Various scrap(ing) strategies that just might work for one
 * database or the other...
 */
public enum ScrapMode {

	/**
	 * Automatic detection. The appropritate PDFDatabase (and thus the
	 * scrap(ing) strategy) is deteced based on the BibTeX URL field.
	 */
	AUTO() {
				@Override
				public String fetchURLToPDF(WebClient client, WebClient jsclient, String url, List<String> xpathExpressions) throws IOException {
					final PDFDatabase db = PDFDatabase.getPDFDatabase(url);
					final Map<ScrapMode, List<String>> configs = db.getScrapConfigurations();
					System.out.println(String.format(
									"  auto. database detection: %s (modes=%d)",
									db,
									configs.size()
							));
					for (Map.Entry<ScrapMode, List<String>> e : configs.entrySet()) {
						final ScrapMode mode = e.getKey();
						final List<String> xpath = e.getValue();
						final String urlToPDF = fetchURLToPDFWithRetry(mode, client, jsclient, url, xpath);
						if (urlToPDF.isEmpty()) {
							continue;
						}
						return urlToPDF;
					}
					return "";
				}
			},
	/**
	 * XPath anchor strategy. This strategy looks for an html anchor element
	 * specified by an XPath expression, whose src attribute supposedly points
	 * the the PDF file.
	 */
	XPATH_ANCHOR() {
				@Override
				public boolean requiresXPathExpression() {
					return true;
				}

				@Override
				public String fetchURLToPDF(WebClient client, WebClient jsclient, String url, List<String> xpathExpressions) throws IOException {
					final HtmlPage page = client.getPage(url);
					System.out.println(String.format(
									"  %s fetching page: %s...",
									name(),
									url
							));
					for (String xpath : xpathExpressions) {
						System.out.println("  ...try XPath expression: " + xpath);
						final HtmlAnchor anchor = (HtmlAnchor) page.getFirstByXPath(xpath);
						if (anchor == null) {
							continue;
						}
						return page.getFullyQualifiedUrl(anchor.getHrefAttribute()).toString();
					}

					return "";
				}
			},
	/**
	 * Frame source strategy. The key idea is to iterate over all frames on the
	 * web page and see if some frame's src attribute points directly to the PDF
	 * file. Sometimes, however, we might have to first take a
	 * detour/redirection by clicking on some link specified by an XPath
	 * expression (optional).
	 */
	FRAME_SRC() {
				@Override
				public String fetchURLToPDF(WebClient client, WebClient jsclient, String url, List<String> xpathExpressions) throws IOException {
					HtmlPage page = client.getPage(url);

					// optional redirect first
					for (String xpath : xpathExpressions) {
						System.out.println("  ...checking anchor redirection, XPath: " + xpath);
						final HtmlAnchor a = (HtmlAnchor) page.getFirstByXPath(xpath);
						if (a != null) {
							final String next = page.getFullyQualifiedUrl(a.getHrefAttribute()).toString();
							final Page p = client.getPage(next);
							if (p.isHtmlPage()) {
								System.out.println("  XPath anchor redirection to: " + next);
								page = (HtmlPage) p;
							} else {
								System.out.println("  XPath anchor return: " + next);
								return next;
							}
						}
					}

					final List<FrameWindow> frames = page.getFrames();
					System.out.println(String.format(
									"  %s fetching page: %s...",
									name(),
									url
							));
					final int n = frames.size();
					final int i = 1;
					for (FrameWindow frame : frames) {
						final BaseFrameElement frameElement = frame.getFrameElement();
						final String src = frameElement.getSrcAttribute();
						System.out.println(String.format("  ...checking frame %d/%d: ", i, n, src));
						if (isPDFSrcAttribute(src)) {
							return src;
						}
					}
					return "";
				}

				@Override
				public boolean retryWithJavaScript() {
					return true;
				}
			};

	private static boolean isPDFSrcAttribute(String src) {
		return src.toLowerCase().contains(".pdf");
	}

	/**
	 * Check whether this scrap(ing) mode requires a custom XPath expression, if
	 * explicitly requested by the user. XPath expressions will be supplied if
	 * the method is invoked automatically.
	 *
	 * @return {@code true} if this scrap(ing) mode requires an XPath
	 * expression, if explicitly requested by the user, {@code false} otherwise.
	 */
	public boolean requiresXPathExpression() {
		return false;
	}

	/**
	 * Checks whether we're encouraged to retry this strategy with JavaScript
	 * being enabled.
	 *
	 * @return {@code true} if it's a good idea to retry this strategy with
	 * JavaScript being enabled, {@code false} otherwise.
	 */
	public boolean retryWithJavaScript() {
		return false;
	}

	/**
	 * Attempts to fetch the URL to the PDF file from a web page.
	 *
	 * @param client the web client.
	 * @param jsclient the web client with enabled JavaScript (usually not
	 * used/needed, that is, only if {@code fetchURLToPDFWithRetry} is going to
	 * be invoked).
	 * @param url the URL of the web page.
	 * @param xpathExpressions the XPath expressions (optional).
	 * @return the URL to the PDF file, or an empty string.
	 * @throws IOException
	 */
	abstract public String fetchURLToPDF(WebClient client, WebClient jsclient, String url, List<String> xpathExpressions) throws IOException;

	/**
	 * Attempts to fetch the URL to the PDF file from a web page, and retries
	 * with JavaScript enabled in case of failure.
	 *
	 * @param mode the scrap(ing) mode.
	 * @param client the web client with disabled JavaScript.
	 * @param jsclient the web client with enabled JavaScript.
	 * @param url the URL of the web page.
	 * @param xpathExpressions the XPath expressions (optional).
	 * @return the URL to the PDF file, or an empty string.
	 * @throws IOException
	 */
	public static String fetchURLToPDFWithRetry(ScrapMode mode, WebClient client, WebClient jsclient, String url, List<String> xpathExpressions) throws IOException {
		final String urlToPDF = mode.fetchURLToPDF(client, jsclient, url, xpathExpressions);
		if (!urlToPDF.isEmpty()) {
			return urlToPDF;
		}
		if (mode.retryWithJavaScript()) {
			System.out.println("  retrying with JavaScript enabled...");
			final String jsUrlToPDF = mode.fetchURLToPDF(jsclient, jsclient, url, xpathExpressions);
			System.out.println("  ...JavaScript disabled");
			return jsUrlToPDF;
		}
		return "";
	}

}
