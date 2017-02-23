package edu.soton.ecs.arxivscraper;

import com.google.gson.Gson;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndLink;
import com.rometools.rome.feed.synd.SyndPerson;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import edu.soton.ecs.arxivscraper.util.IniWrapper;
import edu.soton.ecs.arxivscraper.util.MqWrapper;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

public class ArxivScraper {

    public static void main(String[] args) throws Exception {
        String configLocation = System.getProperty("app.configurationFile");
        if (StringUtils.isBlank(configLocation)) {
            configLocation = "./config/config.ini";
        }

        File configFile = new File(configLocation);
        if (!configFile.exists()) {
            LOGGER.error("Unable to find config file at {}", configLocation);
            System.exit(1);
        }

        IniWrapper.load(configFile);
        String dbFile = IniWrapper.optString("DB", "db_file", "db/db.sqlite");

        String url = IniWrapper.optString("Arxiv", "url", "http://export.arxiv.org/api/query");
        String maxResults = IniWrapper.optString("Arxiv", "max_results", "10");
        String categories = IniWrapper.optString("Arxiv", "categories", "");

        boolean isOutFileEnabled = IniWrapper.optBoolean("Output_File", "enabled", false);
        String outFile = IniWrapper.optString("Output_File", "out_file", "output");

        boolean isAmqpEnabled = IniWrapper.optBoolean("Output_AMQP", "enabled", false);
        String amqpConnectionUrl = IniWrapper.getString("Output_AMQP", "connection_url");
        String amqpQueueName = IniWrapper.getString("Output_AMQP", "queue_name");
        String amqpClientId = IniWrapper.getString("Output_AMQP", "client_id");

        ArxivScraper scraper = new ArxivScraper(url, maxResults, categories);
        List<ArxivEntry> arxivEntries = scraper.scrape();

        List<String> newArxivEntriesJson = new ArrayList<>();
        try (ArxivDbWrapper dbwrapper = new ArxivDbWrapper(dbFile, "arxiv_raw");) {
            dbwrapper.initalize();
            int numInserted = 0;
            for (ArxivEntry arxivEntry : arxivEntries) {
                String id = arxivEntry.getId();
                if (!dbwrapper.isExtracted(id)) {
                    LOGGER.debug("Inserting {}", id);
                    newArxivEntriesJson.add(new Gson().toJson(arxivEntry));
                    numInserted += dbwrapper.defaultInsert(id, arxivEntry);
                }
            }
            LOGGER.info("Inserted {} new entries", numInserted);
        }

        if (isOutFileEnabled) {
            for (String json : newArxivEntriesJson) {
                FileUtils.writeStringToFile(new File(outFile), json + System.lineSeparator(), StandardCharsets.UTF_8, true);
            }
        }

        if (isAmqpEnabled) {
            Hashtable<Object, Object> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
            env.put("connectionfactory.activemqFactory", amqpConnectionUrl);
            Context context = new InitialContext(env);

            ConnectionFactory factory = (ConnectionFactory) context.lookup("activemqFactory");
            try (MqWrapper mqWrapper = new MqWrapper(factory, amqpClientId, amqpQueueName, false);) {
                for (String json : newArxivEntriesJson) {
                    mqWrapper.sendTextMessage(json);
                }
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private String url;
    private String maxResults;
    private String categories;

    public ArxivScraper(String url, String maxResults, String categories) {
        this.url = url;
        this.maxResults = maxResults;
        this.categories = categories;
    }

    public List<ArxivEntry> scrape() throws URISyntaxException, IOException, FeedException {
        List<ArxivEntry> arxivEntries = new ArrayList<>();

        CloseableHttpClient httpclient = HttpClients.custom()
                .setRetryHandler(getHttpRequestRetryHandler())
                .build();
        URI arxivUri = new URIBuilder(url)
                .setParameter("sortBy", "lastUpdatedDate")
                .setParameter("sortOrder", "descending")
                .setParameter("max_results", maxResults)
                .setParameter("search_query", buildCategoryQuery(categories))
                .build();
        LOGGER.info("Grabbing feed from URL: {}", arxivUri);

        HttpGet httpget = new HttpGet(arxivUri);
        CloseableHttpResponse response = httpclient.execute(httpget);
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream is = entity.getContent();
                try {
                    SyndFeedInput input = new SyndFeedInput();
                    XmlReader xmlReader = new XmlReader(is);
                    SyndFeed feed = input.build(xmlReader);

                    List<SyndEntry> entries = feed.getEntries();
                    LOGGER.info("Grabbed {} raw entries", entries.size());
                    arxivEntries = entries.stream().map(this::syndEntryToArxivEntry).collect(Collectors.toList());
                } finally {
                    if (is != null)
                        is.close();
                }
            }
        } finally {
            response.close();
        }
        return arxivEntries;
    }

    private ArxivEntry syndEntryToArxivEntry(SyndEntry syndEntry) {
        ArxivEntry arxivEntry = new ArxivEntry();

        arxivEntry.setId(syndEntry.getUri());
        arxivEntry.setTitle(syndEntry.getTitle());
        arxivEntry.setPublished(syndEntry.getPublishedDate());
        arxivEntry.setUpdated(syndEntry.getUpdatedDate());
        arxivEntry.setSummary(syndEntry.getDescription().getValue());

        List<String> authors = syndEntry.getAuthors().stream().map(SyndPerson::getName).collect(Collectors.toList());
        arxivEntry.setAuthors(authors);

        List<ArxivEntry.Link> links = new ArrayList<>();
        for (SyndLink syndLink : syndEntry.getLinks()) {
            ArxivEntry.Link link = new ArxivEntry.Link();
            try {
                BeanUtils.copyProperties(link, syndLink);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOGGER.error(e);
            }
            links.add(link);
        }
        arxivEntry.setLinks(links);

        List<String> categories = syndEntry.getCategories().stream().map(SyndCategory::getName).collect(Collectors.toList());
        arxivEntry.setCategories(categories);

        for (Element markup : syndEntry.getForeignMarkup()) {
            if (markup.getName().equals("primary_category")) {
                String primaryCategory = markup.getAttribute("term").getValue();
                arxivEntry.setPrimaryCategory(primaryCategory);
            } else if (markup.getName().equals("comment")) {
                arxivEntry.setComment(markup.getValue());
            } else if (markup.getName().equals("doi")) {
                arxivEntry.setDoi(markup.getValue());
            } else if (markup.getName().equals("journal_ref")) {
                arxivEntry.setJournalRef(markup.getValue());
            } else if (markup.getName().equals("affiliation")) {
                // unknown
            }
        }
        return arxivEntry;
    }

    private String buildCategoryQuery(String categories) {
        String[] categoryList = categories.split(",");
        StringBuffer sb = new StringBuffer();
        String prefix = "";
        for (String category : categoryList) {
            sb.append(prefix + "cat:" + category);
            prefix = " OR ";
        }
        return sb.toString();
    }

    private static HttpRequestRetryHandler getHttpRequestRetryHandler() {
        HttpRequestRetryHandler retryHandler = (exception, executionCount, context) -> {
            if (executionCount >= 5) {
                // Do not retry if over max retry count
                return false;
            }
            if (exception instanceof InterruptedIOException) {
                // Timeout
                return false;
            }
            if (exception instanceof UnknownHostException) {
                // Unknown host
                return false;
            }
            if (exception instanceof ConnectTimeoutException) {
                // Connection refused
                return false;
            }
            if (exception instanceof SSLException) {
                // SSL handshake exception
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            if (idempotent) {
                // Retry if the request is considered idempotent
                return true;
            }
            return false;
        };
        return retryHandler;
    }

}
