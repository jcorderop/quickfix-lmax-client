package org.quickfix.client.marketdata.lmax.client.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quickfix.client.common.adaptar.QuickfixConfig;
import org.springframework.stereotype.Service;
import quickfix.Session;
import quickfix.SessionNotFound;
import quickfix.field.*;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.Message;

import javax.annotation.PostConstruct;

@RequiredArgsConstructor
@Slf4j
@Service
public class MarketDataService {

    private final QuickfixConfig quickfixConfig;
    private final SubscriptionConfig subscriptionConfig;

    @PostConstruct
    void ConfigurationLoaded() {
        log.info("subscriptionConfig: {}", subscriptionConfig);
        log.info("quickfixConfig: {}", quickfixConfig);
    }

    public boolean subscribe(String ticker, boolean subscribe) throws SessionNotFound  {
        return createMarketDataSubscription(getSymbolId(ticker), subscribe);
    }

    private String getSymbolId(String ticker) {
        return subscriptionConfig.getTickers().get(ticker);
    }

    private boolean createMarketDataSubscription(String symbolId, boolean subscribe) throws SessionNotFound {
        final MarketDataRequest marketDataRequest = createMarketDataRequest(symbolId, subscribe);
        createHeader(marketDataRequest);
        addCustomMarketDataFields(marketDataRequest);
        createSymbol(marketDataRequest, symbolId);
        createEntryTypes(marketDataRequest);
        return Session.sendToTarget(marketDataRequest);
    }

    private MarketDataRequest createMarketDataRequest(String symbolId, boolean subscribe) {
        //https://quickfixj.org/usermanual/2.3.0/usage/sending_messages.html
        //https://www.quickfixj.org/usermanual/2.3.0/usage/repeating_groups.html
        if (subscribe) {
            return new MarketDataRequest(new MDReqID(symbolId),
                    new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_UPDATES),
                    new MarketDepth(0));
        } else {
            return new MarketDataRequest(new MDReqID(symbolId),
                    new SubscriptionRequestType(SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_UPDATE_REQUEST),
                    new MarketDepth(0));
        }

    }

    private void createEntryTypes(MarketDataRequest marketDataRequest) {
        MarketDataRequest.NoMDEntryTypes entryTypes = new MarketDataRequest.NoMDEntryTypes();  // create group to request both bid and offers
        entryTypes.set(new MDEntryType(MDEntryType.BID));
        marketDataRequest.addGroup(entryTypes);
        entryTypes.set(new MDEntryType(MDEntryType.OFFER));
        marketDataRequest.addGroup(entryTypes);
    }

    private void createSymbol(final MarketDataRequest marketDataRequest, final String symbolId) {
        //https://www.tabnine.com/code/java/methods/quickfix.fix44.MarketDataRequest/addGroup
        final MarketDataRequest.NoRelatedSym noRelatedSym = new MarketDataRequest.NoRelatedSym(); // create group to add list of symbols
        noRelatedSym.set(new SecurityID(symbolId));
        noRelatedSym.set(new SecurityIDSource("8"));
        marketDataRequest.addGroup(noRelatedSym);
    }

    private void addCustomMarketDataFields(final MarketDataRequest marketDataRequest) {
        marketDataRequest.setField(new MDUpdateType(MDUpdateType.FULL_REFRESH));
        marketDataRequest.setField(new AggregatedBook(AggregatedBook.BOOK_ENTRIES_TO_BE_AGGREGATED));
    }

    private void createHeader(final Message message) {
        final quickfix.Message.Header header = message.getHeader();
        header.setField(new SenderCompID(quickfixConfig.getSenderCompID()));
        header.setField(new TargetCompID(quickfixConfig.getTargetCompID()));
    }
}
