/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sscvt.helper;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class SscvtParser {
    private static final String ns = null;
    private final String TAG = "SscvtParser";

    // We don't use namespaces

    public ArrayList<Item> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
        	XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            Log.d(TAG, "nextTag()");
            parser.nextTag();
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }

    private ArrayList<Item> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
    	ArrayList<Item> items = new ArrayList<Item>();
        
        Log.d(TAG, "parser.require()");
        parser.require(XmlPullParser.START_TAG, ns, "channel");
        //Log.d(TAG, "next: " + parser.next());
        while (parser.next() != XmlPullParser.END_TAG) {
        	Log.d(TAG, "while(parser.next()");
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            Log.d(TAG, "name: " + name);
            if (name.equals("item")) {
            	Log.d(TAG, "Found an item");
            	items.add(readEntry(parser));
            } else {
            	Log.d(TAG, "skip");
                skip(parser);
            }
        }
        return items;
    }

    // This class represents a single entry (post) in the XML feed.
    // It includes the data members "title," "link," "date," "content."
    public static class Item {
        private final String title;
        private final String date;
        private final String content;
        private final String link;

        public Item(String title, String date, String content, String link) {
            this.title = title;
            this.date = date;
            this.content = content;//.replaceAll("<form[^>]*?>.*?</form[^>]*?>", ""); // remove all forms from content
            this.link = link;
        }
        
        public String getTitle(){
        	return title;
        }
        public String getDate(){
        	return date;
        }
        public String getLink(){
        	return link;
        }
        public String getContent(){
        	return content;
        }
        
    }

    // Parses the contents of an item. If it encounters a title, date, content, or link tag, hands them
    // off
    // to their respective &quot;read&quot; methods for processing. Otherwise, skips the tag.
    private Item readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "item");
        String title = null;
        String date = null;
        String content = null;
        String link = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
            	Log.d(TAG, "Found title");
                title = readTitle(parser);
            } else if (name.equals("pubDate")) {
            	Log.d(TAG, "Found date");
                date = readDate(parser);
            } else if (name.equals("content:encoded")) {
            	Log.d(TAG, "Found content");
                content = readContent(parser);
            } else if (name.equals("link")) {
            	Log.d(TAG, "Found link");
                link = readLink(parser);
            } else {
                skip(parser);
            }
        }
        return new Item(title, date, content, link);
    }

    // Processes title tags in the feed.
    private String readTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "title");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "title");
        return title;
    }

    // Processes link tags in the feed.
    private String readLink(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "link");
        String link = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "link");
        return link;
    }
    
 // Processes date tags in the feed.
    private String readDate(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "pubDate");
        String date = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "pubDate");
        return date;
    }
    
 // Processes content tags in the feed.
    private String readContent(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "content:encoded");
        String content = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "content:encoded");
        return content;
    }

    // For the tags title and content, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
    // finds the matching END_TAG (as indicated by the value of "depth" being 0).
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                    depth--;
                    break;
            case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
