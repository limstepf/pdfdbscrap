package ch.unibe.scg.pdfdbscrap;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.BaseFrameElement;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * HtmlUnit experiments. This is not a unit test, or what not. Just some
 * experiments to figure out the scraping basics...
 */
public class HtmlUnitExperiments {

	public final static File outputDirectory = new File("D:\\inbox\\tmp");

	public void assertOutputDirectoryExists() {
		assertTrue("output directory exists", outputDirectory.exists());
	}

	/**
	 * Scraping base experiment. To be used as starting point.
	 *
	 * First things first:
	 * <ol>
	 * <li>
	 * Make sure you're able to retrieve the page. Try again with JS disabled if
	 * the client crashes.
	 * </li>
	 * <li>
	 * Make sure you're actually getting what you want, and that there are no
	 * redirect-, proxy-, or login shenanigans or what not. Have a look at the
	 * retrieved text.
	 * </li>
	 * <li>
	 * Also consider disabling CSS to speed things up a bit.
	 * </li>
	 * </ol>
	 */
	@Test
	public void scrapingBaseExperiment() {
		assertOutputDirectoryExists();

		String url = "http://www.google.com";

		try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
//			client.getOptions().setCssEnabled(false);
//			client.getOptions().setJavaScriptEnabled(false);

			try {
				HtmlPage page = client.getPage(url);
				System.out.println("HtmlPage:");
				System.out.println(page.asText());
				System.out.print("\n");
			} catch (IOException ex) {
				System.err.println("WARNING: failed to visit: " + url);
				ex.printStackTrace(System.err);
			}
		}
	}

	/**
	 * Scraping experiment ScienceDirect.
	 *
	 * So... this was a funny one. Inspecting the page with devtools in the
	 * browser will tell you to look for {@code $x("//a[@id='pdfLink']")}, which
	 * corresponds to the "Download full text PDF" link, top-left. Yet, that
	 * anchor is not to be found...
	 *
	 * What do? List and inspect all anchors in the document, search for "pdf",
	 * et voila... there it is. Turns out there is another download link further
	 * down the page, that we're actually able to retrieve. And that top-left
	 * download button probably gets inserted with some JavaScript bullshittery
	 * (which we turned off), or what not.
	 *
	 * JavaScript needs to be disabled, or the webclient crashes, throwing a
	 * ScriptException (Invalid JavaScript value of type
	 * com.gargoylesoftware.htmlunit.ScriptException...).
	 */
	@Test
	public void scrapingExperimentSD() {
		assertOutputDirectoryExists();

//		String url = "http://www.sciencedirect.com/science/article/pii/S0925772108000540";
//		String xpathExpression = "//a[@id='pdfLink']"; // not found!
//		String xpathExpression = "//a[@class='download-pdf-link']";

//		String url = "http://www.sciencedirect.com/science/article/pii/S1045926X16300623";
		String url = "http://www.sciencedirect.com/science/article/pii/S0164121215002265";
		String xpathExpression = "//a[@id='pdfLink']"; // found!

		try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
			client.getOptions().setCssEnabled(false);
			client.getOptions().setJavaScriptEnabled(false);

			try {
				HtmlPage page = client.getPage(url);
				System.out.println("HtmlPage:");
				System.out.println(page.asText());
				System.out.print("\n");

				final HtmlAnchor anchor = (HtmlAnchor) page.getFirstByXPath(xpathExpression);
				System.out.println(" - PDF anchor: " + anchor);

				if (anchor != null) {
//					WebResponse response = anchor.click().getWebResponse();
//					File out = new File(outputDirectory.getAbsolutePath(), "sd-paper.pdf");
//					writeWebResponseToFile(response, out);

					System.out.println(" - q: " + page.getFullyQualifiedUrl(""));
					System.out.println(" - href: " + anchor.getHrefAttribute());

					final String pdfUrl = page.getFullyQualifiedUrl(anchor.getHrefAttribute()).toString();
					System.out.println(" - pdfUrl:  " + pdfUrl);
					UnexpectedPage pdfPage = client.getPage(pdfUrl);
					WebResponse response = pdfPage.getWebResponse();
					File out = new File(outputDirectory.getAbsolutePath(), "sd-paper.pdf");
					writeWebResponseToFile(response, out);
				} else {
					System.err.println("WARNING: link to Full Text PDF not found!");
				}

				List<?> anchors = page.getByXPath("//a");
				System.out.println("number of anchors: " + anchors.size());
				for (int i = 0; i < anchors.size(); i++) {
					HtmlAnchor a = (HtmlAnchor) anchors.get(i);
					System.out.println(" - " + a);
				}

			} catch (IOException ex) {
				System.err.println("WARNING: failed to visit: " + url);
				ex.printStackTrace(System.err);
			}
		}
	}

	/**
	 * Scraping experiment ACM DL.
	 *
	 * JavaScript needs to be disabled, or the webclient crashes, throwing a
	 * ScriptException (TypeError: Cannot set property "baseVal" of undefined to
	 * "at-icon at-icon-email"...).
	 */
	@Test
	public void scrapingExperimentACM() {
		assertOutputDirectoryExists();

		String url = "http://doi.acm.org/10.1145/1409720.1409750";
		String xpathExpression = "//a[@name='FullTextPDF']";

		try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
			client.getOptions().setCssEnabled(false);
			client.getOptions().setJavaScriptEnabled(false);

			try {
				HtmlPage page = client.getPage(url);
				System.out.println("HtmlPage:");
				System.out.println(page.asText());
				System.out.print("\n");

				final HtmlAnchor anchor = (HtmlAnchor) page.getFirstByXPath(xpathExpression);
				System.out.println(" - PDF anchor: " + anchor);

				if (anchor != null) {
//					WebResponse response = anchor.click().getWebResponse();
//					File out = new File(outputDirectory.getAbsolutePath(), "acm-paper.pdf");
//					writeWebResponseToFile(response, out);

					System.out.println(" - q: " + page.getFullyQualifiedUrl(""));
					System.out.println(" - href: " + anchor.getHrefAttribute());

					final String pdfUrl = page.getFullyQualifiedUrl(anchor.getHrefAttribute()).toString();
					System.out.println(" - pdfUrl:  " + pdfUrl);
					UnexpectedPage pdfPage = client.getPage(pdfUrl);
					WebResponse response = pdfPage.getWebResponse();
					File out = new File(outputDirectory.getAbsolutePath(), "acm-paper.pdf");
					writeWebResponseToFile(response, out);
				} else {
					System.err.println("WARNING: link to Full Text PDF not found!");
				}

			} catch (IOException ex) {
				System.err.println("WARNING: failed to visit: " + url);
				ex.printStackTrace(System.err);
			}
		}
	}

	/**
	 * Scraping experiment IEEE. If logged in the link points to a page with
	 * frames, where they load the pdf directly into the second frame. Thus, the
	 * idea here is to simply retrieve all frames, and look for the frame that
	 * has the src attribute pointing to the pdf file.
	 *
	 * ...turns out we might need Javascript in case we're landing on the
	 * abstract/overview page (e.g. dx.doi.org links), instead of the framed
	 * pdf page.
	 */
	@Test
	public void scrapingExperimentIEEE() {
		assertOutputDirectoryExists();

//		String url = "http://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=4782527";
//		String url = "http://dx.doi.org/10.1109/ASE.2011.6100042";
//		String url = "http://ieeexplore.ieee.org/document/6100042/";
//		String url = "http://dx.doi.org/10.1109/ICSE.2009.5070558";
		String url = "http://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=919164";
		String dlXPathExpression = "//a[contains(@class, 'stats-document-lh-action-downloadPdf_2')]";

		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF);

		try (WebClient jsclient = new WebClient(BrowserVersion.CHROME)) {
			jsclient.getOptions().setCssEnabled(false);
		try (WebClient client = new WebClient(BrowserVersion.CHROME)) {
			client.getOptions().setCssEnabled(false);
			client.getOptions().setJavaScriptEnabled(false);


			try {
				System.out.println("fetching page: " + url + "...");
				HtmlPage page = client.getPage(url);
				System.out.println("HtmlPage:");
				System.out.println(page.asText());
				System.out.print("\n");

//				// re-enable JS -> not found, while it finds stuff if not disabled in the first place?!
//				client.getOptions().setJavaScriptEnabled(true);
				page = jsclient.getPage(url);
//				client.waitForBackgroundJavaScript(10000);
//				client.waitForBackgroundJavaScriptStartingBefore(10000);

				String pdfUrl = null;

				// check if there's a download pdf anchor
				HtmlAnchor dlAnchor = (HtmlAnchor) page.getFirstByXPath(dlXPathExpression);
				System.out.println(" - PDF anchor (1): " + dlAnchor);

				if (dlAnchor != null) {
					pdfUrl = page.getFullyQualifiedUrl(dlAnchor.getHrefAttribute()).toString();

					Page p = client.getPage(pdfUrl);
					if (p.isHtmlPage()) {
						page = (HtmlPage) p;
						pdfUrl = null;
					}
				}


				if (pdfUrl == null) {
					final List<FrameWindow> window = page.getFrames();
					final int n = window.size();
					int i = 1;
					for (FrameWindow frameWindow : window) {
						System.out.println(String.format("checking frame %d/%d", i++, n));
						HtmlPage fp = (HtmlPage) frameWindow.getEnclosingPage();
						BaseFrameElement bfe = frameWindow.getFrameElement();
						String src = bfe.getSrcAttribute();

						System.out.println("FrameWindow: " + frameWindow);
						System.out.println(" - EnclosingPage: " + fp);
						System.out.println(" - BaseFrameElement: " + bfe);
						System.out.println(" - SrcAttribute: " + src);
						System.out.print("\n");

						if (isPDFSrcAttribute(src)) {
							pdfUrl = src;
						}
					}
				}

				System.out.print("\n");

				if (pdfUrl != null) {
					System.out.println("link to PDF found: " + pdfUrl);
					Page p = client.getPage(pdfUrl);
					System.out.println("p.isHtml> " + p.isHtmlPage());

					UnexpectedPage pdfPage = client.getPage(pdfUrl);
					WebResponse response = pdfPage.getWebResponse();
					File out = new File(outputDirectory.getAbsolutePath(), "ieee-paper.pdf");
					writeWebResponseToFile(response, out);
				} else {
					System.out.println("WARNING: link to Full Text PDF not found!");
				}

//				List<?> anchors = page.getByXPath("//a");
//				System.out.println("number of anchors: " + anchors.size());
//				for (int i = 0; i < anchors.size(); i++) {
//					HtmlAnchor a = (HtmlAnchor) anchors.get(i);
//					System.out.println(" - " + a);
//				}

			} catch (IOException ex) {
				System.err.println("WARNING: failed to visit: " + url);
				ex.printStackTrace(System.err);
			}
		}
		}
	}

	public static boolean isPDFSrcAttribute(String src) {
		return src.toLowerCase().contains(".pdf");
	}

	public static void writeWebResponseToFile(WebResponse response, File out) {
		try (InputStream is = response.getContentAsStream()) {
			try (OutputStream os = new FileOutputStream(out)) {
				byte[] bytes = new byte[4096];
				int read;
				while ((read = is.read(bytes)) >= 0) {
					os.write(bytes, 0, read);
				}
			} catch (IOException ex) {
				System.err.println("ERROR: failed to write to output stream");
				ex.printStackTrace(System.err);
			}
		} catch (IOException ex) {
			System.err.println("ERROR: failed to read from input stream");
			ex.printStackTrace(System.err);
		}
	}

}
