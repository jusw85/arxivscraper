# arxivscraper

Arxiv scraper in Java.

Scrapes metadata from Arxiv, writes to a SQLite database, then prints new records to file (in JSON).

Requires Java 1.8+

## Operation

[Config file](https://github.com/jusw85/arxivscraper/blob/master/src/main/config/config.ini)

Sample output JSON

    {
      "id": "http://arxiv.org/abs/1505.02114v2",
      "title": "Adaptive Higher-order Spectral Estimators",
      "published": "May 9, 2015 2:07:09 AM",
      "updated": "Feb 23, 2017 1:39:37 AM",
      "summary": "  Many applications involve estimation of a signal matrix from a noisy data\nmatrix. In such cases, it has been observed that estimators that shrink or\ntruncate the singular values of the data matrix perform well when the signal\nmatrix has approximately low rank. In this article, we generalize this approach\nto the estimation of a tensor of parameters from noisy tensor data. We develop\nnew classes of estimators that shrink or threshold the mode-specific singular\nvalues from the higher-order singular value decomposition. These classes of\nestimators are indexed by tuning parameters, which we adaptively choose from\nthe data by minimizing Stein's unbiased risk estimate. In particular, this\nprocedure provides a way to estimate the multilinear rank of the underlying\nsignal tensor. Using simulation studies under a variety of conditions, we show\nthat our estimators perform well when the mean tensor has approximately low\nmultilinear rank, and perform competitively when the signal tensor does not\nhave approximately low multilinear rank. We illustrate the use of these methods\nin an application to multivariate relational data.\n",
      "authors": [
        "David Gerard",
        "Peter Hoff"
      ],
      "links": [
        {
          "href": "http://arxiv.org/abs/1505.02114v2",
          "rel": "alternate",
          "type": "text/html"
        },
        {
          "title": "pdf",
          "href": "http://arxiv.org/pdf/1505.02114v2",
          "rel": "related",
          "type": "application/pdf"
        }
      ],
      "categories": [
        "stat.ME",
        "62H12 (Primary) 15A69, 62C99, 91D30, 62H35 (Secondary)"
      ],
      "primaryCategory": "stat.ME",
      "comment": "29 pages, 3 figures"
    }
    
To setup streaming, the following method can be employed:

Setup crontab

    @ 30s /path/to/arxivscraper


Create a named pipe, and pipe output to destination

    # Terminal 1
    $ mkfifo output
    $ tail -f output > /dev/udp/127.0.0.1/50001
    
    # Terminal 2
    $ nc -lu 50001
    

### Technical Notes

On each invocation, the scraper will GET records from Arxiv using the following sample URL:

    http://export.arxiv.org/api/query?sortBy=lastUpdatedDate&sortOrder=descending&max_results={config.max_results}&search_query={config.categories}
    
{config.categories} is a concatenation of all categories in https://arxiv.org/help/api/user-manual#subject_classifications; it's the only way AFAIK to get all latest documents from the API. The concatenated string looks like `search_query=cat:stat.AP OR cat:stat.CO OR cat:stat.ML OR ...`

The response is an Atom feed; it's parsed and persisted in a SQLite database.

Columns are:

Column | Description
--- | ---
id | Auto generated UUID; Surrogate key
ts | Received timestamp
uri | Arxiv ID
raw | Blob of content

uri is used to determine uniqueness. Records whose uri exists are considered duplicates, otherwise they are considered new.

This has the following implications:
* Updated records with similar uri are not updated
* Updated records with different uri are inserted again i.e. duplicates

Technically, only the uri is required for this table.

New records are converted to JSON using GSON, then output to file://, although output protocol can/should change.

Pruning the SQLite database for old records can be done as follows:

    #!/bin/bash
    echo "DELETE FROM arxiv_raw WHERE strftime('%Y-%m-%d %H:%M:%f',ts) <= strftime('%Y-%m-%d %H:%M:%f','2017-01-01 12:00:00');"\
    | sqlite3 db/db.sqlite

### Design Notes

For my personal reference in the future. Can be ignored.

#### OS Scheduling vs Application Scheduling

OS scheduling e.g. *cron, Windows scheduler

Application scheduling e.g. Quartz scheduler, Obsidian scheduler

OS scheduling
* Each invocation is independent
* State between invocations persisted to disk
* OS scheduler likely to be reliable
* Once off application bugs/crashes will not interrupt schedule
* Relatively easy to change schedule frequency in production

Application scheduling
* Scheduler is integrated into application
* Application runs persistently in the background
* Memory is held by the process
* Bugs possibly introduced during scheduler integration
* Crashes might interrupt schedule
* Relatively difficult to change schedule during runtime

