package com.github.brunopacheco1.realstate.bots.immotop;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.brunopacheco1.realstate.PubSubConstants;
import com.github.brunopacheco1.realstate.api.PropertyDto;
import com.github.brunopacheco1.realstate.api.PropertyType;
import com.github.brunopacheco1.realstate.api.Source;
import com.github.brunopacheco1.realstate.api.TransactionType;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import io.quarkus.scheduler.Scheduled;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class ImmotopCrawler {

    @Inject
    @Channel(PubSubConstants.INCOMING_PROPERTY)
    Emitter<PropertyDto> incomingPropertyEmitter;

    @Scheduled(cron = "{scheduler.immotop}")
    public void produces() {
        log.info("Starting crawling.");
        for (TransactionType transactionType : TransactionType.values()) {
            try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build())
                    .build()) {
                int page = 1;
                Integer maxPages = 20;
                initialSearchPost(httpClient, transactionType);
                while (true) {
                    if (page > maxPages) {
                        break;
                    }
                    String url = "https://www.immotop.lu/en/search/index" + page + ".html";

                    HttpGet get = new HttpGet(url);

                    get.addHeader(":authority", "www.immotop.lu");
                    get.addHeader(":method", "GET");
                    get.addHeader(":path", "/en/search/index" + page + ".html");
                    get.addHeader(":scheme", "https");
                    get.addHeader("accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
                    get.addHeader("accept-encoding", "gzip, deflate, br");
                    get.addHeader("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,pl-PL;q=0.6,pl;q=0.5");
                    get.addHeader("cache-control", "max-age=0");
                    get.addHeader("referer", page == 1 ? "https://www.immotop.lu/en/"
                            : "https://www.immotop.lu/en/search/index" + (page - 1) + ".html");
                    get.addHeader("sec-fetch-dest", "document");
                    get.addHeader("sec-fetch-mode", "navigate");
                    get.addHeader("sec-fetch-site", "same-origin");
                    get.addHeader("sec-fetch-user", "?1");
                    get.addHeader("upgrade-insecure-requests", "1");
                    get.addHeader("user-agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36");

                    CloseableHttpResponse response = httpClient.execute(get);
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        Document doc = Jsoup.parse(EntityUtils.toString(entity));

                        Elements elements = doc.select("div.search-agency-item");
                        elements.forEach(el -> {
                            getProperty(el, transactionType);
                        });
                        page++;
                        Thread.sleep(1000);
                        continue;
                    }
                    break;
                }
            } catch (Exception e) {
                log.log(Level.WARNING, e.getMessage(), e);
            }
        }
        log.info("Finished crawling.");
    }

    private void initialSearchPost(CloseableHttpClient httpClient, TransactionType transactionType) throws Exception {
        String url = "https://www.immotop.lu/en/search/";
        HttpPost post = new HttpPost(url);
        post.addHeader("authority", "www.immotop.lu");
        post.addHeader("cache-control", "max-age=0");
        post.addHeader("upgrade-insecure-requests", "1");
        post.addHeader("origin", "https://www.immotop.lu");
        post.addHeader("content-type", "application/x-www-form-urlencoded");
        post.addHeader("user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36");
        post.addHeader("accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        post.addHeader("sec-fetch-site", "same-origin");
        post.addHeader("sec-fetch-mode", "navigate");
        post.addHeader("sec-fetch-user", "?1");
        post.addHeader("sec-fetch-dest", "document");
        post.addHeader("referer", "https://www.immotop.lu/en/search/index1.html");
        post.addHeader("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,pl-PL;q=0.6,pl;q=0.5");

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("f[sort_field]", "date_modifyed"));
        params.add(new BasicNameValuePair("f[sort_type]", "desc"));
        String transaction = TransactionType.BUY == transactionType ? "sale" : "rent";
        params.add(new BasicNameValuePair("f[Type]", transaction));
        params.add(new BasicNameValuePair("form", "simple_search"));
        params.add(new BasicNameValuePair("sort_field", "ts"));
        params.add(new BasicNameValuePair("sort_type", "desc"));
        params.add(new BasicNameValuePair("f[text_search]", ""));
        params.add(new BasicNameValuePair("f[city]", "Luxembourg"));
        params.add(new BasicNameValuePair("f[rooms][from]", ""));
        params.add(new BasicNameValuePair("f[surface][from]", ""));
        params.add(new BasicNameValuePair("f[price][from]", ""));
        params.add(new BasicNameValuePair("f[price][to]", ""));
        params.add(new BasicNameValuePair("f[by_transport]", ""));
        params.add(new BasicNameValuePair("form", "main_search_form"));
        params.add(new BasicNameValuePair("f[geodata][0][country]", "luxembourg"));
        params.add(new BasicNameValuePair("f[geodata][0][name]", "Luxembourg"));
        params.add(new BasicNameValuePair("f[geodata][0][label]", "Country"));
        params.add(new BasicNameValuePair("f[geodata][0][c_label]", "Luxembourg"));
        params.add(new BasicNameValuePair("f[geodata][0][level]", "country"));

        post.setEntity(new UrlEncodedFormEntity(params));

        httpClient.execute(post);
    }

    private Set<String> urls = new HashSet<>();
    private List<String> urlsOrdered = new ArrayList<>();

    private void getProperty(Element el, TransactionType transactionType) {
        try {
            String propertyUrl = el.select("a").attr("href");
            PropertyType propertyType = getPropertyType(propertyUrl);
            String location = getLocation(el.select("a").text());
            BigDecimal value = getPrice(el.select("div.price").text());
            Source source = Source.IMMOTOP;
            PropertyDto property = new PropertyDto(location, value, propertyType, transactionType, propertyUrl, source);
            if (!urls.contains(property.getUrl() + " - " + property.getTransactionType())) {
                urls.add(property.getUrl() + " - " + property.getTransactionType());
                urlsOrdered.add(property.getUrl() + " - " + property.getTransactionType());
            }
            incomingPropertyEmitter.send(property);
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private String getLocation(String value) {
        String cleanedValue = value.toUpperCase().trim();
        if (cleanedValue.contains("FOR SALE IN")) {
            return cleanedValue.split("FOR SALE IN")[1].trim();
        }
        if (cleanedValue.contains("FOR RENT IN")) {
            return cleanedValue.split("FOR RENT IN")[1].trim();
        }
        throw new RuntimeException("Location not found");
    }

    private BigDecimal getPrice(String value) {
        String cleanedValue = value.toUpperCase().trim();
        if (cleanedValue.contains("FROM")) {
            cleanedValue = cleanedValue.split("TO")[0].trim();
        }
        if (cleanedValue.contains(".")) {
            cleanedValue = cleanedValue.split("\\.")[1];
        }
        return new BigDecimal(cleanedValue.replaceAll("\\D", ""));
    }

    private PropertyType getPropertyType(String value) {
        if (value.contains("/new/")) {
            return PropertyType.NEW_PROPERTY;
        }
        String cleanedValue = value.split("\\/")[6];
        if (cleanedValue.equals("house")) {
            return PropertyType.HOUSE;
        }
        if (cleanedValue.equals("apartment")) {
            return PropertyType.APPARTMENT;
        }
        if (cleanedValue.equals("building-of-flats") || cleanedValue.equals("residence")) {
            return PropertyType.PROPERTY;
        }
        if (cleanedValue.equals("garage")) {
            return PropertyType.PARKING;
        }
        if (cleanedValue.equals("commercial-property") || cleanedValue.equals("business")) {
            return PropertyType.COMMERCIAL_PREMISES;
        }
        if (cleanedValue.equals("ground")) {
            return PropertyType.LAND;
        }
        throw new RuntimeException("PropertyType not found");
    }
}
