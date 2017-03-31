package ch.unibe.scg.pdfdbscrap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BibTeX entry range. Working with indices starting from 1 to n BibTeX entries.
 * Range limits are including.
 */
public class EntryRange {

	/**
	 * Entry range pattern. Examples:
	 * <pre>
	 * "4-9"	-> start=4,  end=9
	 * "3"		-> start=3,  end=MAX_INT
	 * "25-INF" -> start=25, end=MAX_INT
	 * "INF"	-> start=1,  end=MAX_INT
	 * </pre>
	 */
	protected final static Pattern pattern = Pattern.compile(
			"([0-9]+)([^0-9]+)?([0-9]+)?([^0-9]*)?"
	);

	/**
	 * The first entry to process (including).
	 */
	public final int start;

	/**
	 * The last entry to process (including).
	 */
	public final int end;

	/**
	 * Creates a new, unbounded entry range.
	 */
	public EntryRange() {
		this.start = 1;
		this.end = Integer.MAX_VALUE;
	}

	/**
	 * Creates a new entry range.
	 *
	 * @param value the entry range string (pattern: "start[-end]", where start
	 * and end are integer).
	 */
	public EntryRange(String value) {
		final Matcher m = pattern.matcher(value);
		if (m.find() && m.groupCount() == 4) {
			this.start = parseInt(m.group(1), 1);
			this.end = parseInt(m.group(3), Integer.MAX_VALUE);
		} else {
			this.start = 1;
			this.end = Integer.MAX_VALUE;
		}
	}

	protected final int parseInt(String value, int defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	protected String getEnd() {
		if (end == Integer.MAX_VALUE) {
			return "END";
		}
		return String.format("%d", end);
	}

	@Override
	public String toString() {
		return String.format("%d-%s", start, getEnd());
	}

}
