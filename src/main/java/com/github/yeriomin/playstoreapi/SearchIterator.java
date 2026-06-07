package com.github.yeriomin.playstoreapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Iterates through search result pages
 * Each next() call gets you a next page of search results for the provided query
 */
public class SearchIterator extends AppListIterator {

    static private final String DOCID_FRAGMENT_MORE_RESULTS = "more_results";

    private Item mainResult;
    private String query;

    public SearchIterator(GooglePlayAPI googlePlayApi, String query) {
        super(googlePlayApi);
        this.query = query;
        String url = GooglePlayAPI.SEARCH_URL;
        Map<String, String> params = new HashMap<String, String>();
        params.put("c", "3");
        params.put("q", query);
        firstPageUrl = googlePlayApi.getClient().buildUrl(url, params);
    }

    public String getQuery() {
        return query;
    }

    @Override
    public List<Item> next() {
        List<Item> next = new ArrayList<Item>(super.next());
        if (null != mainResult) {
            if (next.size() > 0 && !next.get(0).getDetails().getAppDetails().getPackageName().equals(mainResult.getDetails().getAppDetails().getPackageName())) {
                next.add(0, mainResult);
            }
            mainResult = null;
        }
        return next;
    }

    @Override
    protected Item getRootDoc(Item doc) {
        Item.Builder builder = null;
        Item mainResult = null;
        for (Item child: doc.getSubItemList()) {
            if (!isRootDoc(child)) {
                continue;
            }
            if (child.getSubItemCount() == 1) {
                mainResult = child.getSubItem(0);
            }
            if (child.getId().contains(DOCID_FRAGMENT_MORE_RESULTS)) {
                builder = child.toBuilder();
            }
        }
        if (null != mainResult && null != builder) {
            this.mainResult = mainResult;
            return builder.addSubItem(0, mainResult).build();
        }
        return super.getRootDoc(doc);
    }

    @Override
    protected boolean isRootDoc(Item doc) {
        return super.isRootDoc(doc) && doc.getId().contains("search");
    }
}
