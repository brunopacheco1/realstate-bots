package com.github.brunopacheco1.realstate.bots.immotop;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.brunopacheco1.realstate.PubSubConstants;
import com.github.brunopacheco1.realstate.api.PropertyDto;
import com.github.brunopacheco1.realstate.api.TransactionType;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
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
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy())
                .build()) {
            int page = 1;
            Integer maxPages = 20;
            // TODO - Navigate through pages using post.
            for (TransactionType transactionType : TransactionType.values()) {
                while (true) {
                    if (page > maxPages) {
                        break;
                    }
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
                    // TODO - post.addHeader("", ""); get cookie first?

                    List<NameValuePair> params = new ArrayList<NameValuePair>();
                    params.add(new BasicNameValuePair("f[sort_field]", "date_modifyed"));
                    params.add(new BasicNameValuePair("f[sort_type]", "desc"));
                    params.add(new BasicNameValuePair("f[Type]", "sale"));
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

                    CloseableHttpResponse response = httpClient.execute(post);
                    // TODO - Get coockies and propagate to next pages.
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        Document doc = Jsoup.parse(EntityUtils.toString(entity));

                        Elements elements = doc.select("div.search-agency-item");
                        elements.forEach(el -> {
                            getProperty(el, transactionType);
                        });
                        page++;
                        continue;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
        log.info("Finished crawling.");
    }

    private void getProperty(Element el, TransactionType transactionType) {
        try {
            // incomingPropertyEmitter.send(property);
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
