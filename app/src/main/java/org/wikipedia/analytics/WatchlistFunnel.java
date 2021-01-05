package org.wikipedia.analytics;

import org.wikipedia.WikipediaApp;
import org.wikipedia.util.StringUtil;

public class WatchlistFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppWatchlist";
    private static final int REV_ID = 20797104;

    public WatchlistFunnel() {
        super(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL);
    }

    public void logAction(String action) {
        log(
                "action", action,
                "languages", StringUtil.listToJsonArrayString(getApp().language().getAppLanguageCodes())
        );
    }

    public void logShowTooltipMore() {
        logAction("show_tooltip_more");
    }

    public void logShowTooltip() {
        logAction("show_tooltip");
    }

    public void logAddArticle() {
        logAction("add_article");
    }

    public void logAddExpiry() {
        logAction("add_expiry");
    }

    public void logAddSuccess() {
        logAction("add_success");
    }

    public void logViewWatchlist() {
        logAction("view_watchlist");
    }

    public void logOpenWatchlist() {
        logAction("open_watchlist");
    }

    public void logRemoveArticle() {
        logAction("remove_article");
    }

    public void logRemoveSuccess() {
        logAction("remove_success");
    }
}
