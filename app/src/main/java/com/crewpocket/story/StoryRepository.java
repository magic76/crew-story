package com.crewpocket.story;
import android.content.Context;
import java.util.*;
public final class StoryRepository {
private static StoryStore store=new SharedPreferencesStoryStore(); private StoryRepository(){}
static synchronized void setStoreForTesting(StoryStore r){store=r!=null?r:new SharedPreferencesStoryStore();}
public static synchronized List<StoryModel> getStories(Context c){List<StoryModel>a=new ArrayList<>();a.addAll(BuiltInStoryCatalog.all());for(StoryModel s:store.list(c))if(s!=null&&!"sample_snow_white".equals(s.id))a.add(s);return a;}
public static synchronized StoryModel getStoryById(Context c,String id){if(id==null)return null;for(StoryModel s:BuiltInStoryCatalog.all())if(id.equals(s.id))return s;return store.get(c,id);}
public static synchronized void saveStories(Context c,List<StoryModel> list){List<StoryModel>u=new ArrayList<>();if(list!=null)for(StoryModel s:list)if(s!=null&&!s.isBuiltIn())u.add(s);store.saveAll(c,u);}
public static synchronized void addStory(Context c,StoryModel s){if(s==null)return;s.sourceType=StoryModel.SOURCE_USER;store.save(c,s);}
public static synchronized void saveStory(Context c,StoryModel s){if(s!=null&&!s.isBuiltIn())store.save(c,s);}
public static synchronized void updateStory(Context c,StoryModel s){saveStory(c,s);}
public static synchronized void deleteStory(Context c,String id){StoryModel s=getStoryById(c,id);if(s!=null&&!s.isBuiltIn())store.delete(c,id);}
public static synchronized StoryModel duplicateForEditing(Context c,StoryModel source){if(source==null)return null;StoryModel x=StoryModel.fromJson(source.toJson());x.id="story_"+System.currentTimeMillis();x.title=source.title+I18n.t(c,"（我的版本）"," (My version)");x.sourceType=StoryModel.SOURCE_USER;x.copyrightNote="";x.createdAt=System.currentTimeMillis();store.save(c,x);return x;}
public static StoryModel createSnowWhiteStory(){return BuiltInStoryCatalog.all().get(0);}
}