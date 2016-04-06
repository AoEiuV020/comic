/* ***************************************************
	^> File Name: Reptile.java
	^> Author: AoEiuV020
	^> Mail: 490674483@qq.com
	^> Created Time: 2016/04/05 - 00:22:32
*************************************************** */
package com.aoeiuv020.reptile;
import com.aoeiuv020.stream.Stream;
import com.aoeiuv020.comic.Item;
import android.content.Context;
import android.content.*;
import android.widget.*;
import android.view.*;
import android.graphics.drawable.Drawable;
import org.json.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
/*
  * Reptile.MAIN
  * Reptile.CLASSIFICATION
  * Reptile.CATALOG
  * Reptile.COMIC
  * Reptile reptile=new Reptile(jsonobject);
  * List<Item> list;
  * //Map.keys {"icon","text1"}
  * list=reptile.getData(Reptile.MAIN);
  * list=reptile.getData(Reptile.CATALOG,index);
  * list=reptile.getData(Reptile.COMIC,index);
  * SimpleAdapter.ViewBinder binder;
  * binder=reptile.getViewBinder();
  */
public class Reptile
{
	public static final int MAIN=1;
	public static final int CLASSIFICATION=2;
	public static final int CATALOG=3;
	public static final int COMIC=4;
	private static JSONObject sitesJson=null;
	private JSONObject mJsonSite=null;
	private String encoding=null;
	private String method=null;
	private String baseuri=null;
	private URL baseUrl=null;
	private URL pageUrl=null;
	private URL catalogUrl=null;
	private URL comicUrl=null;
	private Selector selector=null;
	private Connection mConnection=null;
	private Context mContext=null;
	public Reptile(Context context,JSONObject json)
	{
		this.mContext=context;
		mJsonSite=json;
		init();
	}
	private void init()
	{
		method="GET";
		try
		{
			baseuri=mJsonSite.getString("baseuri");
			baseUrl=new URL(baseuri);
			mConnection=Jsoup.connect(baseUrl.toString());
			if(mJsonSite.has("method"))
			{
				method=mJsonSite.getString("method");
			}
			mConnection.method(Connection.Method.valueOf(method));
		}
		catch(JSONException e)
		{
			throw new RuntimeException(e);
		}
		catch(MalformedURLException e)
		{
			throw new RuntimeException("baseuri不规范:"+baseuri,e);
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	public boolean loadNext()
	{
		boolean hasNext=false;
		if(selector!=null)
		{
			hasNext=selector.loadNext();
		}
		return hasNext;
	}
	public void loadStart()
	{
			selector.loadStart();
	}
	public int getCount()
	{
		return selector.getCount();
	}
	public List<Item> getPage(int page)
	{
		List<Item> list=null;
		JSONObject pageJson=null;
		Document document=null;
		try
		{
			switch(page)
			{
				case MAIN:
					pageJson=mJsonSite.getJSONObject("main");
					URL pageUrl=new URL(baseUrl,pageJson.getString("uri"));
					document=mConnection.url(pageUrl).execute().parse();
					if(selector==null)
					{
						selector=new Selector(mContext,pageJson.getJSONObject("selector"),mConnection,document);
					}
					list=selector.getItems();
					break;
			}
		}
		catch(MalformedURLException e)
		{
			throw new RuntimeException("uri参数错误",e);
		}
		catch(JSONException e)
		{
			throw new RuntimeException(e);
		}
		catch(IOException e)
		{
			throw new RuntimeException("可能是网络不通",e);
		}
		return list;
	}
	public List<Item> getData(int page)
	{
		return getData(page,0);
	}
	public List<Item> getData(int page,int index)
	{
		List<Item> list=null;
		JSONObject pageJson=null;
		Document document=null;
		try
		{
			switch(page)
			{
				case MAIN:
					pageJson=mJsonSite.getJSONObject("main");
					pageUrl=new URL(baseUrl,pageJson.getString("uri"));
					document=mConnection.url(pageUrl).execute().parse();
					selector=new Selector(mContext,pageJson.getJSONObject("selector"),mConnection,document);
					list=selector.getData();
					break;
			}
		}
		catch(MalformedURLException e)
		{
			throw new RuntimeException(e);
		}
		catch(JSONException e)
		{
			throw new RuntimeException(e);
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
		return list;
	}
	public SimpleAdapter.ViewBinder getViewBinder()
	{
		return new ComicViewBinder();
	}
}
class ComicViewBinder implements SimpleAdapter.ViewBinder
{
	@Override
	public boolean setViewValue(View view,Object data,String textRepresentation)
	{
		if(view instanceof ImageView&&data instanceof Drawable)
		{
			((ImageView)view).setImageDrawable((Drawable)data);
			return true;
		}
		return false;
	}
}
class Selector implements Runnable
{
	private String elementsQuery=null;
	private String aQuery=null;
	private String imgQuery=null;
	private String textQuery=null;
	private String nextQuery=null;
	private JSONObject mJson=null;
	private Element mElement=null;
	private Thread loader=null;
	private Context mContext=null;
	private boolean loading=false;
	private boolean ok=false;
	List<Item> mList=new LinkedList<Item>();
	private Connection mConnection=null;
	public Selector(Context context,JSONObject json,Connection parmConnection,Element parmElement)
	{
		this.mContext=context;
		this.mJson=json;
		mConnection=parmConnection;
		try
		{
			elementsQuery=mJson.getString("elements");
			aQuery=mJson.getString("a");
			imgQuery=mJson.getString("img");
			textQuery=mJson.getString("text");
			if(mJson.has("next"))
			{
				nextQuery=mJson.getString("next");
			}
		}
		catch(JSONException e)
		{
			throw new RuntimeException(e);
		}
		mElement=parmElement;
		mList=new LinkedList<Item>();
		loader=new Thread(this);
	}
	public List<Item> getItems()
	{
		List<Item> list=new LinkedList<Item>();
		Item item=null;
		for(Element element:mElement.select(elementsQuery))
		{
			Element aElement,imgElement,textElement;
			aElement=element.select(aQuery).first();
			imgElement=element.select(imgQuery).first();
			textElement=element.select(textQuery).first();
			item=new Item();
			if(imgElement!=null)
			{
				item.image=imgElement.absUrl("src");
			}
			if(aElement!=null)
			{
				item.url=aElement.absUrl("href");
				item.content=item.url;
			}
			if(textElement!=null)
			{
				item.title=textElement.text();
			}
			System.out.println(""+item);
			list.add(item);
		}
		return list;
	}
	public int getCount()
	{
		return mList.size();
	}
	public boolean loadNext()
	{
		boolean hasNext=false;
		if(nextQuery==null||"".equals(nextQuery))
		{
			return false;
		}
		Element next=mElement.select(nextQuery).first();
		if(next!=null)
		{
			loading=false;
			/*
			try
			{
				loader.join();
			}
			catch(InterruptedException e)
			{
				//不可到达，
				throw new RuntimeException(e);
			}
			*/
			try
			{
				mElement=mConnection.url(next.absUrl("href")).execute().parse();
				loader=new Thread(this);
				loader.start();
				hasNext=true;
			}
			catch(IOException e)
			{
				hasNext=false;
			}
		}
		return hasNext;
	}
	public List<Item> getData()
	{
		return mList;
	}
	public void notifyDataSetChanged()
	{
		Intent intent=new Intent();
		intent.setAction("com.aoeiuv020.comic.notifyDataSetChanged");
		mContext.sendBroadcast(intent);
	}
	@Override
	public void run()
	{
		loading=true;
		ok=false;
		Item item=null;
		for(int i=0;i<10;++i)
		{

			item=new Item();
			item.image="";
			item.title="title "+i;
			item.content="content "+i;
			mList.add(item);
			notifyDataSetChanged();
		}
		ok=true;
		/*
		for(Element element:mElement.select(elementsQuery))
		{
			Element aElement,imgElement,textElement;
			aElement=element.select(aQuery).first();
			imgElement=element.select(imgQuery).first();
			textElement=element.select(textQuery).first();
			Drawable drawable=null;
			try
			{
				if(imgElement!=null)
				{
					drawable=Drawable.createFromStream(new ByteArrayInputStream(Jsoup.connect(imgElement.absUrl("src")).ignoreContentType(true).execute().bodyAsBytes()),null);
				}
			}
			catch(Exception e)
			{
				//throw new RuntimeException(e);
			}
			item=new HashMap<String,Object>();
			item.put("img",drawable);
			if(aElement!=null)
			{
				item.put("url",aElement.absUrl("href"));
			}
			if(textElement!=null)
			{
				item.put("text",textElement.text());
			}
			mList.add(item);
			System.out.println(""+item);
		}
		notifyDataSetChanged();
		ok=true;
		*/
	}
	public void loadStart()
	{
		loader.start();
	}
}