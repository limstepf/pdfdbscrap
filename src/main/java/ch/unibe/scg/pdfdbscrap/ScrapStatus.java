package ch.unibe.scg.pdfdbscrap;

import java.io.File;

/**
 * Scrap(ing) status. Mostly error codes.
 */
public enum ScrapStatus {

	/**
	 * The BibTeX entry has no key.
	 */
	FAILURE_NO_BIBTEX_KEY,
	/**
	 * The BibTeX entry has no url field.
	 */
	FAILURE_NO_BIBTEX_URL,
	/**
	 * Failed to fetch the URL to the PDF file from the BibTeX url.
	 */
	FAILURE_URL_TO_PDF_NOTFOUND,
	/**
	 * The fetched URL (supposedly) pointing to the PDF file is invalid. That
	 * is, the URL points to a html page (most likely some "sign in",
	 * "purchase", or similar pages).
	 */
	FAILURE_INVALID_URL_TO_PDF,
	/**
	 * A failing HTTP status code. Usually a runtime exception thrown by
	 * HtmlUnit.
	 */
	FAILURE_FAILING_HTTP_STATUS_CODE,
	/**
	 * Input stream IO exception.
	 */
	FAILURE_IO_INPUTSTREAM,
	/**
	 * Output stream IO exception.
	 */
	FAILURE_IO_OUTPUTSTREAM,
	/**
	 * Success. Yay.
	 */
	SUCCESS;

	/**
	 * Returns the status output directory. The directory is created if it
	 * doesn't exist yet.
	 *
	 * @param outputDirectory the output directory, parent of the status output
	 * directories.
	 * @return the status output directory.
	 */
	public File getStatusDirectory(File outputDirectory) {
		final File dir = new File(
				outputDirectory.getAbsolutePath(),
				name()
		);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

}
