# PDF Database Scrap(er)

`pdfdbscrap` is a command line tool to scrap PDF files from (academic) online databases, given a BibTeX database (and BibTeX entries with an `url` field) as input. The URLs to the PDF files are located and scraped serially, processing one after the other BibTeX entry. No pause/delay, unless we retry due to recieving a failing HTTP status code (max. 2 retries, with a 5 second timeout).

There are many reasons scrap(ing) could fail. Depending on the outcome, `pdfdbscrap` will create the following directories:

* `FAILURE_NO_BIBTEX_KEY`
* `FAILURE_NO_BIBTEX_URL`
* `FAILURE_URL_TO_PDF_NOTFOUND`
* `FAILURE_INVALID_URL_TO_PDF`
* `FAILURE_FAILING_HTTP_STATUS_CODE`
* `FAILURE_IO_INPUTSTREAM`
* `FAILURE_IO_OUTPUTSTREAM`
* `SUCCESS`

Each processed BibTeX entry will be saved individually in his own BibTeX file, and placed in the corresponding `ScrapStatus` directory. If that file just happens to be in the `SUCCESS` directory, it will be accompanied by the scraped PDF file. Both files are named according to the specified `IDCreator` (or ID method).

Thus, no entry will be lost, and failure cases may be manually processed (retry to scrap them, try to find and download the missing PDF files manually, ...).


## Use Case

The basic use case of this tool is to download all PDF files corresponding to the search results of from such an (academic) online database (e.g. to carry out a systematic literature review, or what not...).

1. **Search an (academic) online database.**
2. **Save/export search results.**
3. Convert the retrieved file to BibTeX (optional).
4. Merge multiple such BibTeX files (optional, but helps pruning duplicates).
5. Sanitize the BibTeX file (optional):
  - Find and prune duplicates (e.g. with [JabRef](http://www.jabref.org/))
  - Prune incomplete entries, especially ones without `author` field. Those tend to be of the sort of: "copyright notice", "front cover", "program committee", "title page", "author index", ... anyways. The false-positive rate is next to zero. Again JabRef might help with this.
6. If multiple BibTeX files from different databases have been merged in step 4, you might want to consider to split the file back up again in order to have one BibTeX file per database. This allows you to scrap them in parallel. The `--partition` option does just that for you. The `--number` option can be used to still end up with a proper numbering/filenames.
7. **Scrap the PDF files**, following the `url` field of each BibTeX entry
8. Retry later and continue the scrap(ing) since you just got fucking blocked again. The `--range` option (can be combined with the `--number` option) might come in handy here to restart the process from a certain BibTeX entry.

### Prerequisites

1. Search results must be exported to BibTeX, or a format that can be converted to BibTeX
2. BibTeX entries require a proper entry `key`, and an `url` field used as a starting point to find the URL to the PDF file
3. Access to the (academic) online databases, possibly by using the virtual private network (VPN) of your university


## Supported (academic) online databases

Currently supported databases that should work just fine with `AUTO` scrap(ing) mode (March 31, 2017):

* [The ACM Digital Library](http://dl.acm.org/)
  
  - You're likely to get an `403 Error - Access Forbidden` something after maybe 200 or 300 downloads in succession. If you're lucky, you might get unbanned after maybe 5 hours, and continue with your deeds.
  - Anecdotal success rate: 94.17% (for 360 BibTeX entries)
  - Anecdotal performance: 13 processed BibTeX entries per minute (~8 min. for 100 entries)
  
* [The Digital Object Identifier (DOI) system](http://dx.doi.org/)
  
  - DOI hasn't been targeted specifically. Chances are this will only work for redirects to any of the other listed databases. Good luck.
  
  - Anecdotal success rate: -
  - Anecdotal performance: -
  
* [IEEE Xplore Digital Library](http://ieeexplore.ieee.org/)
  
  - Anecdotal success rate: 100.0% (for 427 BibTeX entries)
  - Anecdotal performance: 14 processed BibTeX entries per minute (~7 min. for 100 entries)
  
* [ScienceDirect](http://www.sciencedirect.com/)
  
  - Anecdotal success rate: 88.81% (for 402 BibTeX entries)
  - Anecdotal performance: 16 processed BibTeX entries per minute (~6 min. for 100 entries)


### I'm not able to scrap from X, what do?

1. Adjust, or implement a new scrap(ing) mode.
2. Add a new database entry to `PDFDatabase` and assign a suitable scrap(ing) configuration.
3. Use an existing scrap(ing) method explicitly (i.e. don't use the `AUTO` scrap(ing) mode), and supply custom XPath expression(s). The `XPATH_ANCHOR` mode might just work for you (open the URL from a BibTeX `url` field in a browser, locate the link to download the PDF file, and use the developer tools/JavaScript console of your browser to figure out the XPath to that HTML anchor).


## Usage

```
$ java -jar pdfdbscrap-1.0.0-SNAPSHOT.jar <options>

-f, --file <file>
    The BibTeX file to process.

-r, --range <integer>[-<integer>] (e.g start to end: "10-25", or just a start-offset: "15")
    The range (or just a start offset; inclusive) of BibTeX entries (1 to N) to process.

-o, --out <file>
    The output directory.

-m, --mode <string> (AUTO, XPATH_ANCHOR, or FRAME_SRC; DEFAULT=AUTO)
    The scrap(ing) mode.

-x, --xpath <string> (e.g. "//a[@id='pdfLink']")
    The XPath expression(s) to get to the PDF download link.

-s, --split <string> (DEFAULT=";")
    The split string to separate multiple XPath expressions.

-i, --id <string> (ENTRY_NUMBER, NUMBER_AND_KEY, or URLENCODED_KEY; DEFAULT=NUMBER_AND_KEY)
    The ID method (used for filenames).

-n, --number <integer>
    The starting number/offset (just used by the ID method).

-b, --browser <string> (BEST_SUPPORTED, CHROME, EDGE, FIREFOX, or IE; DEFAULT=CHROME)
    The browser (version) of the headless web client.

-p, --partition
    Partitions the given BibTeX file into multiple BibTeX files; one for each known/unkown database.

-u, --usage
    Print the usage of this program.
```

## Examples

Scrap(ing) with default settings. Scrap(ing) on `AUTO` mode will try to find the appropriate scrap(ing) strategy based on the `url` field of the BibTeX entry:

```bash
#!/bin/bash
app=./pdfdbscrap.git/target/pdfdbscrap-1.0.0-SNAPSHOT.jar
file=./database.bib
out=./scrap_out
log=${out}/pdfdbscrap.log

java -jar ${app} --file ${file} --out ${out} | tee ${log}
```

In case you've merged the BibTeX entries of multiple search results into a single file, you might want to split it back up again using the `--partition` option, s.t. you can scrap each database individually (and maybe in parallel). The following example:

```bash
#!/bin/bash
app=./pdfdbscrap.git/target/pdfdbscrap-1.0.0-SNAPSHOT.jar
file=./merged.bib
out=./

java -jar ${app} --file ${file} --out ${out} --partition
```

...will create new BibTeX files; one for each known (and unknown) database:

```
...
processing entry Gomes:2007:EIP:1330598.1330691   (173/1189):  ACM
processing entry Lungu:2006:SCE:1148493.1148533   (174/1189):  ACM
processing entry Burnett:2006:1148493             (175/1189):  UNKNOWN
processing entry Hundhausen:2008:1409720          (176/1189):  UNKNOWN
processing entry Baloian:2005:AVU:1056018.1056020 (177/1189):  ACM...
...
processing entry Visser2005831                    (1186/1189):  SCIENCEDIRECT
processing entry Corbett1997849                   (1187/1189):  SCIENCEDIRECT
processing entry Malczewski20043                  (1188/1189):  SCIENCEDIRECT
processing entry Deek2000223                      (1189/1189):  SCIENCEDIRECT

writing ACM-partition with 350 BibTeX entries to:
  .\merged-ACM.bib...
writing DOI-partition with 2 BibTeX entries to:
  .\merged-DOI.bib...
writing IEEE-partition with 427 BibTeX entries to:
  .\merged-IEEE.bib...
writing SCIENCEDIRECT-partition with 402 BibTeX entries to:
  .\merged-SCIENCEDIRECT.bib...
writing UNKNOWN-partition with 8 BibTeX entries to:
  .\merged-UNKNOWN.bib...

kthxbai.
```


In case you need to continue the scrap(ing), you can use the `--range` option to start the scrap(ing) from a certain entry (a starting offset, with unspecified rang end, is just fine. Entries are counted from `1` to `n`):

```bash
#!/bin/bash
app=./pdfdbscrap.git/target/pdfdbscrap-1.0.0-SNAPSHOT.jar
file=./database.bib
range=179
out=./scrap_out
log=${out}/pdfdbscrap.log

java -jar ${app} --file ${file} --range ${range} --out ${out} | tee ${log}
```

The above will scrap entries `179` to `n`. But you might as well specify a full range as in: `179-350` to scrap entries 179 to 350 (all inclusive).

The number of an entry is nice to use as `IDCreator` (used for filenames of produced PDF and BibTeX files), so sometimes you just might want to offset that number while still processing/scraping all the entries in some BibTeX file. Just use the `--number` option as in:

```bash
#!/bin/bash
app=./pdfdbscrap.git/target/pdfdbscrap-1.0.0-SNAPSHOT.jar
file=./database.bib
number=250
out=./scrap_out
log=${out}/pdfdbscrap.log

java -jar ${app} --file ${file} --number ${number} --out ${out} | tee ${log}
```

With 100 entries in a BibTeX file this will produce the IDs 350 to 450. If you have to query different databases separately (i.e. no merged BibTeX file running on `AUTO`), and still want to have nice, consecutive IDs, then this might just be the option for you.

Final example, this time using an explicit scrap(ing) mode with two custom XPath expression separated by the `;` character (in `XPATH_ANCHOR` mode the XPath expressions - supposed to refer to HTML anchors pointing to the PDF files - are tried one after the other):

```bash
#!/bin/bash
app=./pdfdbscrap.git/target/pdfdbscrap-1.0.0-SNAPSHOT.jar
mode=XPATH_ANCHOR
xpath="//a[@name='FullTextPDF'];//a[@class='download-pdf-link']"
file=./database.bib
out=./scrap_out
log=${out}/pdfdbscrap.log

java -jar ${app} --file ${file} --mode ${mode} --xpath ${xpath} --out ${out} | tee ${log}
```

## Related Projects

* [bibsani](https://github.com/limstepf/bibsani): Bib(TeX) Sani(tizer)
* [csvtobib](https://github.com/limstepf/csvtobib): Converts (IEEE) CSV to BibTeX
* [pdfhiscore](https://github.com/limstepf/pdfhiscore): PDF Hi(stogram) Score
