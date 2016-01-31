package com.bignerdranch.android.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.bignerdranch.anroid.photogallery.R;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/* Class to handle the network connection to Flickr */
public class FlickrFetchr {
	
	/*********************************************************/
	/*                     Constants                         */
	/*********************************************************/
	public static final String TAG = "FlickrFetchr";
	public static final String PREF_SEARCH_QUERY = "searchQuery";
	public static final String PREF_LAST_RESULT_ID = "lastResultId";
	
	private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
	private static final String API_KEY = "890954c0dbc37eb0dd9e4e66b040d0e7";
	private static final String METHOD_GET_RECENT = 
			"flickr.photos.getRecent";
	private static final String METHOD_SEARCH = "flickr.photos.search";
	private static final String EXTRA_SMALL_URL = "url_s";
	private static final String XML_PHOTO = "photo";
	private static final String PARAM_TEXT = "text";
	private static final String PARAM_EXTRAS = "extras";
	
	/*********************************************************/
	/*                     Local Data                        */
	/*********************************************************/
	boolean fetchFailed = false;

	/*********************************************************/
	/*                   Private Methods                     */
	/*********************************************************/
	
	/* Method to fetch raw data from a URL and return it as
	 * an array of bytes
	 */
	byte[] getUrlBytes(String urlSpec) throws IOException {
		
		//Create URL object from String like "www.google.com"
		URL url = new URL(urlSpec);
		
		/* Create connection object pointed at the URL. Since it is an
		 * http URL, cast it to HttpURLConnection
		 */
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			//Get input stream to connect to endpoint
			InputStream in = connection.getInputStream();
			
			//check if connection can be made
			if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null;
			}
			
			int bytesRead = 0;
			byte[] buffer = new byte[1024];
			
			//read from input stream until there is no data left
			while((bytesRead = in.read(buffer)) > 0) {
				out.write(buffer,0,bytesRead);
			}
			out.close();
			
			//return byte array of output stream
			return out.toByteArray();
		} finally {
			connection.disconnect();
		}
	}

	void parseItems(ArrayList<GalleryItem> items, XmlPullParser parser) throws
	XmlPullParserException, IOException {
		
		int eventType =  parser.next();
		
		//parse through each event till END_DOCUMENT Tag in XML
		while(eventType != XmlPullParser.END_DOCUMENT) {
			
			//Get photo tag
			if(eventType == XmlPullParser.START_TAG && 
					XML_PHOTO.equals(parser.getName())) {
				
				String id = parser.getAttributeValue(null,"id");
				String caption = parser.getAttributeValue(null,"title");
				String smallUrl = parser.getAttributeValue(null,EXTRA_SMALL_URL);
				String owner = parser.getAttributeValue(null,"owner");
				
				GalleryItem item = new GalleryItem();
				
				item.setId(id);
				item.setCaption(caption);
				item.setUrl(smallUrl);
				item.setOwner(owner);
				items.add(item);
				
			}
			
			//Got to next event/photo in XML
			eventType = parser.next();
		}
	}
	
	/*********************************************************/
	/*                   Public Methods                      */
	/*********************************************************/
	/* Method to convert Byte string from URL into string */
	public String getUrl(String urlSpec) throws IOException {
		return new String(getUrlBytes(urlSpec));
	}
	
	//Build the appropriate request URL and fetch its contents
	public ArrayList<GalleryItem> downloadGalleryItems(String url) {
		
		ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
		
		try {
						
			String xmlString = getUrl(url);
			//Log.i(TAG,"Received xml: " + xmlString);
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlString));
			parseItems(items,parser);
			fetchFailed = false;
		} catch (IOException ioe) {
			Log.e(TAG, "Failed to fetch items", ioe);
			fetchFailed = true;
			// Toast.makeText(mCallingActivity,R.string.failed_fetch_toast, Toast.LENGTH_SHORT).show();
		} catch (XmlPullParserException xppe) {
			Log.e(TAG,"Failed to parse items", xppe);
			fetchFailed = true;
			 //Toast.makeText(mCallingActivity,R.string.failed_fetch_toast, Toast.LENGTH_SHORT).show();
		}
		
		return items;
	}
	
	public ArrayList<GalleryItem> fetchItems() {
		
		//Build complete URL for the Flickr API request
		String url = Uri.parse(ENDPOINT).buildUpon().
				appendQueryParameter("method",METHOD_GET_RECENT)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.build().toString();
		
		return downloadGalleryItems(url);
	}
	
	public ArrayList<GalleryItem> search(String query) {
		String url = Uri.parse(ENDPOINT).buildUpon()
				.appendQueryParameter("method", METHOD_SEARCH)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.appendQueryParameter(PARAM_TEXT, query)
				.build().toString();
		
		return downloadGalleryItems(url);
	}

	public boolean isFetchFailed() {
		return fetchFailed;
	}
}
