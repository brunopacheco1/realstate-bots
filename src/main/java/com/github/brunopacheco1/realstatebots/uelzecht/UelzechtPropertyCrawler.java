package com.github.brunopacheco1.realstatebots.uelzecht;

import java.math.BigDecimal;
import javax.enterprise.context.ApplicationScoped;
import com.github.brunopacheco1.realstatebots.domain.Property;
import com.github.brunopacheco1.realstatebots.domain.PropertyType;
import com.github.brunopacheco1.realstatebots.domain.Source;
import com.github.brunopacheco1.realstatebots.domain.TransactionType;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import io.smallrye.reactive.messaging.annotations.Broadcast;

@ApplicationScoped
public class UelzechtPropertyCrawler {
    
    @Incoming("uelzecht-crawler")
    @Outgoing("incoming-property")
    @Broadcast
    public Property crawl(Message<String> message) {
        String url = message.getPayload();
        String location = null;
        BigDecimal value = null;
        PropertyType propertyType = null;
        TransactionType transactionType = null;
        Source source = Source.UELZECHT;
        return new Property(location, value, propertyType, transactionType, url, source);
    }
}
