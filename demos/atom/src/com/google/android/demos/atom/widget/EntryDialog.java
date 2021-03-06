/*-
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.demos.atom.widget;

import com.google.android.demos.atom.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Displays the title and HTML content of an Atom feed.
 * <p>
 * Entries are shown in a {@link Dialog} instead of an {@link Activity} for
 * performance; it is faster to load all of the entries at once in
 * {@link FeedActivity} than it is to load them one at a time as they are
 * opened.
 */
public class EntryDialog extends Dialog {

    /**
     * Configures a {@link TextView} to host clickable links (as in
     * {@link android.text.util.Linkify}).
     */
    private static final void addLinkMovementMethod(TextView text) {
        MovementMethod method = text.getMovementMethod();
        if (!(method instanceof LinkMovementMethod)) {
            if (text.getLinksClickable()) {
                method = LinkMovementMethod.getInstance();
                text.setMovementMethod(method);
            }
        }
    }

    private static final String STATE_TITLE = "atom:title";

    private static final String STATE_URL = "atom:url";

    private static final String STATE_SUMMARY = "atom:summary";

    private static final String STATE_CONTENT = "atom:content";

    private static final String STATE_FEED = "atom:feed";

    // Constants for WebView
    private static final String MIME_TYPE = "text/html";

    private static final String ENCODING = "utf-8";

    private MenuInflater mMenuInflater;

    private ScrollView mScrollView;

    private TextView mTitleView;

    private WebView mContentView;

    private String mTitle;

    private String mUrl;

    private String mSummary;

    private String mContent;

    private String mFeed;

    public EntryDialog(Activity context) {
        super(context, android.R.style.Theme_Light);
        mMenuInflater = context.getMenuInflater();

        // Request a progress bar to display while the HTML content is loading.
        Window window = getWindow();
        ProgressMonitor.requestWindowFeatures(window);

        setContentView(R.layout.atom_entry);

        // Find the ScrollView
        mScrollView = (ScrollView) findViewById(android.R.id.tabcontent);

        // Find the title view, and make it a clickable link
        mTitleView = (TextView) findViewById(android.R.id.text1);
        addLinkMovementMethod(mTitleView);

        // Find the content view, and configure the progress monitor.
        mContentView = (WebView) findViewById(android.R.id.text2);
        WebChromeClient monitor = new ProgressMonitor(window);
        mContentView.setWebChromeClient(monitor);
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle savedInstanceState = super.onSaveInstanceState();
        savedInstanceState.putString(STATE_TITLE, mTitle);
        savedInstanceState.putString(STATE_URL, mUrl);
        savedInstanceState.putString(STATE_SUMMARY, mSummary);
        savedInstanceState.putString(STATE_CONTENT, mContent);
        savedInstanceState.putString(STATE_FEED, mFeed);
        return savedInstanceState;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTitle = savedInstanceState.getString(STATE_TITLE);
        mUrl = savedInstanceState.getString(STATE_URL);
        mSummary = savedInstanceState.getString(STATE_SUMMARY);
        mContent = savedInstanceState.getString(STATE_CONTENT);
        mFeed = savedInstanceState.getString(STATE_FEED);
        updateViews();
    }

    public void setData(String title, String url, String summary, String content, String feed) {
        mTitle = title;
        mUrl = url;
        mSummary = summary;
        mContent = content;
        mFeed = feed;
        updateViews();
    }

    private void updateViews() {
        if (mTitle != null) {
            // Link the title to the entry URL
            SpannableString link = new SpannableString(mTitle);
            if (mUrl != null) {
                int start = 0;
                int end = mTitle.length();
                int flags = 0;
                link.setSpan(new URLSpan(mUrl), start, end, flags);
            }
            mTitleView.setText(link);

            // Show the content, or the summary if no content is available.
            String body = !TextUtils.isEmpty(mContent) ? mContent
                    : !TextUtils.isEmpty(mSummary) ? mSummary : "";

            // Show the feed title in the window decorator.
            if (!TextUtils.isEmpty(mTitle)) {
                setTitle(mTitle);
            } else {
                Context context = getContext();
                setTitle(context.getText(R.string.atom_title_entry));
            }

            // Use loadDataWithBaseURL instead of loadData for unsanitized HTML:
            // http://code.google.com/p/android/issues/detail?id=1733
            mContentView.clearView();
            mContentView.loadDataWithBaseURL(null, body, MIME_TYPE, ENCODING, null);
        }
    }

    public void scrollToTop() {
        mScrollView.scrollTo(0, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenuInflater.inflate(R.menu.entry_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // This code is missing in the implementation of Dialog.
        // It was copied from Activity's implementation of onMenuItemSelected.
        switch (featureId) {
            case Window.FEATURE_OPTIONS_PANEL:
                return onOptionsItemSelected(item);
            case Window.FEATURE_CONTEXT_MENU:
                return onContextItemSelected(item);
            default:
                return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share:
                shareLink();
                return true;
            default:
                return false;
        }
    }

    private void shareLink() {
        if (mTitle != null && mUrl != null) {
            Context context = getContext();

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, mTitle);
            intent.putExtra(Intent.EXTRA_TEXT, mUrl);

            String chooserTitle = null;

            Intent chooser = Intent.createChooser(intent, chooserTitle);

            context.startActivity(chooser);
        }
    }

    /**
     * Displays the page loading progress in the window title bar.
     */
    private static class ProgressMonitor extends WebChromeClient {
        private final Window mWindow;

        public ProgressMonitor(Window window) {
            super();
            mWindow = window;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            mWindow.setFeatureInt(Window.FEATURE_PROGRESS, newProgress * 100);
        }

        public static void requestWindowFeatures(Window window) {
            window.requestFeature(Window.FEATURE_PROGRESS);
        }
    }
}
