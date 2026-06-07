package com.github.yeriomin.playstoreapi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

abstract public class AppListIterator implements Iterator {

    protected GooglePlayAPI googlePlayApi;
    protected boolean firstQuery = true;
    protected String firstPageUrl;
    protected String nextPageUrl;

    public AppListIterator(GooglePlayAPI googlePlayApi) {
        setGooglePlayApi(googlePlayApi);
    }

    public void setGooglePlayApi(GooglePlayAPI googlePlayApi) {
        this.googlePlayApi = googlePlayApi;
    }

    public List<Item> next() {
        Payload payload;
        Item rootDoc;
        try {
            payload = getPayload();
            rootDoc = getRootDoc(payload);
            this.firstQuery = false;
        } catch (IOException e) {
            throw new IteratorGooglePlayException(e);
        }
        nextPageUrl = findNextPageUrl(payload);
        if (null == nextPageUrl && null != rootDoc) {
            nextPageUrl = findNextPageUrl(rootDoc);
        }
        if (nextPageStartsFromZero()) {
            return next();
        }
        if (null != rootDoc) {
            return rootDoc.getSubItemList();
        } else {
            return new ArrayList<Item>();
        }
    }

    public boolean hasNext() {
        return this.firstQuery || (null != this.nextPageUrl && this.nextPageUrl.length() > 0);
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    protected Payload getPayload() throws IOException {
        String url;
        if (firstQuery && null != firstPageUrl) {
            url = firstPageUrl;
        } else if (null != nextPageUrl && nextPageUrl.length() > 0) {
            url = nextPageUrl;
        } else {
            throw new NoSuchElementException();
        }
        return googlePlayApi.genericGet(url, null);
    }

    protected String findNextPageUrl(Payload payload) {
        if (null == payload) {
            return null;
        }
        if (payload.hasSearchResponse()) {
            return findNextPageUrl(payload.getSearchResponse());
        } else if (payload.hasListResponse()) {
            return findNextPageUrl(payload.getListResponse());
        }
        return null;
    }

    protected String findNextPageUrl(SearchResponse searchResponse) {
        if (searchResponse.hasNextPageUrl()) {
            return GooglePlayAPI.FDFE_URL + searchResponse.getNextPageUrl();
        } else if (searchResponse.getItemCount() > 0) {
            return findNextPageUrl(searchResponse.getItem(0));
        }
        return null;
    }
/*
    protected String findNextPageUrl(ListResponse listResponse) {
        if (listResponse.getItemCount() > 0) {
            return findNextPageUrl(listResponse.getItem(0));
        }
        return null;
    }*/
    protected String findNextPageUrl(ListResponse listResponse) {
     //   if (listResponse.getItemCount() > 0) {
            return findNextPageUrl(listResponse.getItem());
     //   }
    //    return null;
    }

    protected String findNextPageUrl(Item rootDoc) {
        if (rootDoc.hasContainerMetadata() && rootDoc.getContainerMetadata().hasNextPageUrl()) {
            return GooglePlayAPI.FDFE_URL + rootDoc.getContainerMetadata().getNextPageUrl();
        }
        if (rootDoc.hasAnnotations()
            && rootDoc.getAnnotations().hasAnnotationLink()
            && rootDoc.getAnnotations().getAnnotationLink().hasResolvedLink()
            && rootDoc.getAnnotations().getAnnotationLink().getResolvedLink().hasSearchUrl()
        ) {
            return GooglePlayAPI.FDFE_URL + rootDoc.getAnnotations().getAnnotationLink().getResolvedLink().getSearchUrl();
        }
        for (Item child: rootDoc.getSubItemList()) {
            if (!isRootDoc(child)) {
                continue;
            }
            String nextPageUrl = findNextPageUrl(child);
            if (null != nextPageUrl) {
                return nextPageUrl;
            }
        }
        return null;
    }

    /**
     * Sometimes not a list of apps is returned by search, but a list of content types (music and apps, for example)
     * each of them having a list of items
     * In this case we have to find the apps list and return it
     */
     /*
    protected Item getRootDoc(Payload payload) {
        if (null == payload) {
            return null;
        }
        if (payload.hasSearchResponse() && payload.getSearchResponse().getItemCount() > 0) {
            return getRootDoc(payload.getSearchResponse().getItem(0));
        } else if (payload.hasListResponse() && payload.getListResponse().getItemCount() > 0) {
            return getRootDoc(payload.getListResponse().getItem(0));
        }
        return null;
    }*/
    protected Item getRootDoc(Payload payload) {
        if (null == payload) {
            return null;
        }
        if (payload.hasSearchResponse() && payload.getSearchResponse().getItemCount() > 0) {
            return getRootDoc(payload.getSearchResponse().getItem(0));
        } else if (payload.hasListResponse() /*&& payload.getListResponse().getItemCount() > 0*/) {
            return getRootDoc(payload.getListResponse().getItem());
        }
        return null;
    }

    protected Item getRootDoc(Item doc) {
        if (isRootDoc(doc)) {
            return doc;
        }
        for (Item child: doc.getSubItemList()) {
            Item root = getRootDoc(child);
            if (null != root) {
                return root;
            }
        }
        return null;
    }

    protected boolean isRootDoc(Item doc) {
        return doc.getSubItemCount() > 0 && doc.getSubItem(0).getCategoryId() == 3 && doc.getSubItem(0).getType() == 1;
    }

    private boolean nextPageStartsFromZero() {
        if (null == nextPageUrl) {
            return false;
        }
        try {
            return new URI(nextPageUrl).getQuery().contains("o=0");
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
