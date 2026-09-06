package com.crewpocket.story;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StoryModel implements Serializable {
    public static final String SOURCE_BUILT_IN="built_in", SOURCE_USER="user";
    public String id,title,summary,coverEmoji,coverImageUri;
    public long createdAt;
    public String sourceType=SOURCE_USER,ageGroup="",copyrightNote="";
    public int estimatedMinutes=0;
    public final List<String> tags=new ArrayList<>();
    public List<Page> pages=new ArrayList<>();
    public boolean isBuiltIn(){return SOURCE_BUILT_IN.equals(sourceType);}

    public static class Page implements Serializable {
        public int pageIndex; public String imageUri,text,emotion,characterName,dialogue;
        public StoryContext context=new StoryContext();
        public JSONObject toJson(){JSONObject o=new JSONObject();try{
            o.put("pageIndex",pageIndex);o.put("imageUri",imageUri!=null?imageUri:"");o.put("text",text!=null?text:"");
            o.put("emotion",emotion!=null?emotion:"normal");o.put("characterName",characterName!=null?characterName:"");
            o.put("dialogue",dialogue!=null?dialogue:"");o.put("context",context!=null?context.toJson():new JSONObject());
        }catch(Exception ignored){}return o;}
        public static Page fromJson(JSONObject o){Page p=new Page();p.pageIndex=o.optInt("pageIndex",0);p.imageUri=o.optString("imageUri","");
            p.text=o.optString("text","");p.emotion=o.optString("emotion","normal");p.characterName=o.optString("characterName","");
            p.dialogue=o.optString("dialogue","");p.context=StoryContext.fromJson(o.optJSONObject("context"));return p;}
    }
    public JSONObject toJson(){JSONObject o=new JSONObject();try{
        o.put("id",id);o.put("title",title);o.put("summary",summary);o.put("coverEmoji",coverEmoji!=null?coverEmoji:"📖");
        o.put("coverImageUri",coverImageUri!=null?coverImageUri:"");o.put("createdAt",createdAt);o.put("sourceType",sourceType);
        o.put("ageGroup",ageGroup);o.put("estimatedMinutes",estimatedMinutes);o.put("tags",new JSONArray(tags));o.put("copyrightNote",copyrightNote);
        JSONArray a=new JSONArray();for(Page p:pages)a.put(p.toJson());o.put("pages",a);
    }catch(Exception ignored){}return o;}
    public static StoryModel fromJson(JSONObject o){StoryModel s=new StoryModel();s.id=o.optString("id",String.valueOf(System.currentTimeMillis()));
        s.title=o.optString("title","未命名故事");s.summary=o.optString("summary","");s.coverEmoji=o.optString("coverEmoji","📖");
        s.coverImageUri=o.optString("coverImageUri","");s.createdAt=o.optLong("createdAt",System.currentTimeMillis());
        s.sourceType=o.optString("sourceType",SOURCE_USER);s.ageGroup=o.optString("ageGroup","");s.estimatedMinutes=o.optInt("estimatedMinutes",0);
        s.copyrightNote=o.optString("copyrightNote","");JSONArray t=o.optJSONArray("tags");if(t!=null)for(int i=0;i<t.length();i++)s.tags.add(t.optString(i));
        JSONArray a=o.optJSONArray("pages");if(a!=null)for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p!=null)s.pages.add(Page.fromJson(p));}return s;}
}
