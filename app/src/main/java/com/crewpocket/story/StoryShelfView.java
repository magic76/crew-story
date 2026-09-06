package com.crewpocket.story;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.List;

public final class StoryShelfView extends LinearLayout {
 public interface Listener {void onPlayStory(StoryModel s);void onEditStory(StoryModel s);void onDeleteStory(StoryModel s);void onCreateStory();}
 private final Context c; private final Listener l;
 public StoryShelfView(Context c,Listener l){super(c);this.c=c;this.l=l;setOrientation(VERTICAL);render();}
 private int dp(float v){return CrewTheme.dp(c,v);}
 private TextView t(String x,float z,int col,boolean b){TextView v=new TextView(c);v.setText(x);v.setTextSize(z);v.setTextColor(col);if(b)v.setTypeface(Typeface.DEFAULT_BOLD);return v;}
 private void render(){
  TextView h=t(I18n.t(c,"今晚想和阿奇去哪裡？","Where should we go with Archie?"),24,Color.WHITE,true);h.setPadding(0,dp(8),0,dp(4));addView(h);
  TextView sub=t(I18n.t(c,"選一本故事，馬上開始。中途有問題，隨時跟阿奇說。","Pick a story and talk to Archie anytime."),13,CrewTheme.TEXT_SECONDARY,false);sub.setPadding(0,0,0,dp(14));addView(sub);
  List<StoryModel>a=StoryRepository.getStories(c); if(!a.isEmpty())addView(quick(a.get(0)));
  TextView label=t(I18n.t(c,"故事書架","Story shelf"),15,CrewTheme.TEXT_SECONDARY,true);label.setPadding(0,dp(18),0,dp(10));addView(label);
  for(StoryModel s:a)addView(card(s));
  Button b=new Button(c);b.setText(I18n.t(c,"＋ 和阿奇創作新故事","＋ Create a story with Archie"));b.setTextColor(Color.BLACK);b.setTypeface(Typeface.DEFAULT_BOLD);GradientDrawable g=new GradientDrawable();g.setColor(CrewTheme.AMBER_400);g.setCornerRadius(dp(14));b.setBackground(g);b.setOnClickListener(v->{if(l!=null)l.onCreateStory();});LayoutParams p=new LayoutParams(-1,dp(52));p.setMargins(0,dp(8),0,dp(24));addView(b,p);
 }
 private View quick(final StoryModel s){LinearLayout x=new LinearLayout(c);x.setOrientation(VERTICAL);x.setPadding(dp(18),dp(16),dp(18),dp(16));x.setBackground(CrewTheme.createCard(c,Color.parseColor("#172033"),CrewTheme.AMBER_400,18));x.addView(t(I18n.t(c,"第一次和阿奇看故事？","First story with Archie?"),12,CrewTheme.AMBER_400,true));TextView n=t(s.coverEmoji+"  "+s.title,19,Color.WHITE,true);n.setPadding(0,dp(6),0,dp(4));x.addView(n);x.addView(t(meta(s),12,CrewTheme.TEXT_SECONDARY,false));Button b=new Button(c);b.setText(I18n.t(c,"▶ 和阿奇一起讀","▶ Read with Archie"));b.setTextColor(Color.BLACK);b.setTypeface(Typeface.DEFAULT_BOLD);b.setBackground(CrewTheme.createCard(c,CrewTheme.AMBER_400,Color.TRANSPARENT,12));b.setOnClickListener(v->{if(l!=null)l.onPlayStory(s);});LayoutParams p=new LayoutParams(-1,dp(46));p.setMargins(0,dp(12),0,0);x.addView(b,p);return x;}
 private View card(final StoryModel s){LinearLayout x=new LinearLayout(c);x.setGravity(Gravity.CENTER_VERTICAL);x.setPadding(dp(14),dp(14),dp(12),dp(14));x.setBackground(CrewTheme.createCard(c,CrewTheme.BG_SURFACE,CrewTheme.BORDER_DEFAULT,18));TextView e=t(s.coverEmoji,36,Color.WHITE,false);e.setGravity(Gravity.CENTER);x.addView(e,new LayoutParams(dp(58),dp(64)));LinearLayout info=new LinearLayout(c);info.setOrientation(VERTICAL);info.setPadding(dp(10),0,dp(8),0);info.addView(t(s.title,16,Color.WHITE,true));info.addView(t(meta(s),11,CrewTheme.TEXT_MUTED,false));if(!s.tags.isEmpty())info.addView(t(tags(s),11,CrewTheme.SKY_400,false));x.addView(info,new LayoutParams(0,-2,1));TextView play=t("▶",17,Color.BLACK,true);play.setGravity(Gravity.CENTER);GradientDrawable g=new GradientDrawable();g.setColor(CrewTheme.AMBER_400);g.setShape(GradientDrawable.OVAL);play.setBackground(g);x.addView(play,new LayoutParams(dp(46),dp(46)));x.setOnClickListener(v->{if(l!=null)l.onPlayStory(s);});x.setOnLongClickListener(v->{actions(s);return true;});LayoutParams p=new LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));x.setLayoutParams(p);return x;}
 private String meta(StoryModel s){String o=s.isBuiltIn()?I18n.t(c,"內建故事","Included"):I18n.t(c,"我的故事","My story");if(!s.ageGroup.isEmpty())o+=" · "+I18n.t(c,s.ageGroup+" 歲","Ages "+s.ageGroup);if(s.estimatedMinutes>0)o+=" · "+I18n.t(c,"約 "+s.estimatedMinutes+" 分鐘","About "+s.estimatedMinutes+" min");return o;}
 private String tags(StoryModel s){StringBuilder b=new StringBuilder();for(int i=0;i<s.tags.size()&&i<3;i++){if(i>0)b.append("  ");b.append("#").append(s.tags.get(i));}return b.toString();}
 private void actions(final StoryModel s){if(s.isBuiltIn()){String[]o={I18n.t(c,"▶ 和阿奇一起讀","▶ Read with Archie"),I18n.t(c,"⧉ 建立我的版本","⧉ Make my own copy")};new AlertDialog.Builder(c).setTitle("《"+s.title+"》").setItems(o,(d,w)->{if(l==null)return;if(w==0)l.onPlayStory(s);else{StoryModel cp=StoryRepository.duplicateForEditing(c,s);if(cp!=null)l.onEditStory(cp);}}).show();}else{String[]o={I18n.t(c,"▶ 開始朗讀","▶ Start reading"),I18n.t(c,"✏ 編輯繪本","✏ Edit story"),I18n.t(c,"🗑 刪除故事","🗑 Delete story")};new AlertDialog.Builder(c).setTitle("《"+s.title+"》").setItems(o,(d,w)->{if(l==null)return;if(w==0)l.onPlayStory(s);if(w==1)l.onEditStory(s);if(w==2)l.onDeleteStory(s);}).show();}}
}