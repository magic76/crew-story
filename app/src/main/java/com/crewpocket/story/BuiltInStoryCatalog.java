package com.crewpocket.story;
import java.util.*;
public final class BuiltInStoryCatalog {
private BuiltInStoryCatalog(){}
public static List<StoryModel> all(){List<StoryModel> a=new ArrayList<>();
a.add(makedino());
a.add(makemoon());
a.add(makefox());
a.add(makerobot());
a.add(makebear());
a.add(makeredcap());
a.add(makefrog());
a.add(makebremen());
a.add(makecinderella());
a.add(makehansel());
return a;}
private static StoryModel makedino(){StoryModel s=story("builtin_dino","🦕","小恐龍不敢關燈","害怕與勇氣","3–5",5,"Crew Story 原創",new String[]{"勇氣","情緒","睡前"});
s.pages.add(page(0,"warm","豆豆","每天睡前，豆豆都希望房間亮亮的。只要媽媽碰到開關，他就趕快說，黑暗裡好像藏著奇怪的東西。","我不是不想睡，我只是還沒準備好。","豆豆害怕關燈",new String[]{"豆豆害怕黑暗","目前沒有怪獸出現"},new String[]{"黑暗裡真的有怪獸嗎？","害怕很丟臉嗎？"},"不要透露豆豆後來如何克服害怕"));
s.pages.add(page(1,"mysterious","豆豆","媽媽陪他拿手電筒巡邏。床底沒有怪獸，牆上的大怪獸原來只是椅子和外套疊出的影子。","原來影子也會假裝自己很可怕！","豆豆檢查房間",new String[]{"可怕影子其實是家具"},new String[]{"影子為什麼變大？"},"不要透露豆豆何時關燈"));
s.pages.add(page(2,"tender","媽媽","媽媽留下一盞星星夜燈，告訴豆豆：勇敢不是完全不怕，而是害怕時願意試一點點。豆豆自己關掉大燈。","那我今天先留一顆小星星。","豆豆第一次嘗試只留夜燈",new String[]{"豆豆仍有點怕","媽媽允許慢慢練習"},new String[]{"勇敢一定不能怕嗎？"},"不要透露今晚結果"));
s.pages.add(page(3,"joyful","豆豆","黑暗沒有追過來，房間只是換了一種樣子。第二天晚上，豆豆又自己關掉大燈，鑽進被窩前還對黑暗說了晚安。","我還有一點怕，可是我知道我做得到。","豆豆帶著害怕完成挑戰",new String[]{"勇敢不等於完全不害怕"},new String[]{"我今晚也可以試嗎？"},"故事已完成"));
return s;}
private static StoryModel makemoon(){StoryModel s=story("builtin_moon","🌙","月亮掉進我的口袋","想像與好奇","3–5",5,"Crew Story 原創",new String[]{"想像","好奇","睡前"});
s.pages.add(page(0,"mysterious","米米","米米發現口袋裡亮亮的，摸到一小片銀白色的光。她看看天上的月亮，心裡跳出一個大膽念頭。","月亮是不是掉了一小塊給我？","米米發現神秘光點",new String[]{"口袋裡真的有光"},new String[]{"月亮會掉下來嗎？"},"不要揭露光點來源"));
s.pages.add(page(1,"excited","米米","她拿著光靠近積木和玻璃杯，桌上竟出現一圈又一圈的小月亮。","你會魔法嗎？","米米用光探索反射",new String[]{"亮面會出現反光"},new String[]{"為什麼杯子也有月亮？"},"不要揭露是亮片"));
s.pages.add(page(2,"tender","米米","睡前她終於發現縫線旁黏著一顆銀色亮片，月光剛好照在上面。","原來不是月亮掉下來，是月亮找到一面小鏡子。","米米發現真相",new String[]{"亮片反射月光"},new String[]{"那前面算想錯嗎？"},"真相已揭露，不要嘲笑想像"));
s.pages.add(page(3,"joyful","米米","米米沒有失望。她把亮片放回口袋，決定每次看到它發光，就提醒自己普通東西也能長出很不普通的故事。","晚安，沒有掉下來的月亮。","米米珍惜想像",new String[]{"想像與真相可以共存"},new String[]{"我也能自己編故事嗎？"},"故事已完成"));
return s;}
private static StoryModel makefox(){StoryModel s=story("builtin_fox","🦊","小狐狸第一次迷路","獨立與求助","3–5",5,"Crew Story 原創",new String[]{"安全","獨立","森林"});
s.pages.add(page(0,"excited","栗栗","栗栗追著藍蝴蝶跑過大樹和圓石頭，等蝴蝶飛走，媽媽和熟悉的小路都不見了。","咦，我剛剛從哪邊來？","栗栗發現迷路",new String[]{"栗栗追蝴蝶離開路線"},new String[]{"迷路要一直走嗎？"},"不要透露誰來幫忙"));
s.pages.add(page(1,"warm","栗栗","他很想亂跑找媽媽，但想起迷路時先停下來，於是坐在一棵特別大的樹旁觀察四周。","先停下來，我才不會越走越遠。","栗栗停下並觀察",new String[]{"栗栗留在醒目的大樹旁"},new String[]{"為什麼不能到處找？"},"不要透露下一角色"));
s.pages.add(page(2,"warm","貓頭鷹郵差","戴著郵差帽的貓頭鷹飛來。栗栗沒有亂跟著走，而是說出媽媽名字和圓石頭。郵差用森林廣播幫忙。","我們就在這棵大樹旁等。","栗栗向有身分的工作人員求助",new String[]{"郵差協助聯絡媽媽"},new String[]{"什麼樣的大人可以求助？"},"不要透露媽媽何時出現"));
s.pages.add(page(3,"joyful","媽媽","不久媽媽跑到大樹旁抱住栗栗。回家後，他們畫了一張迷路小卡：停下來、找明顯地點、找可信任的人求助。","下次我要先看路，不只看蝴蝶！","栗栗平安回家",new String[]{"停留原地幫助尋找","栗栗學會求助"},new String[]{"商場迷路怎麼辦？"},"故事已完成"));
return s;}
private static StoryModel makerobot(){StoryModel s=story("builtin_robot","🤖","機器人為什麼不會哭","理解情緒","6–8",7,"Crew Story 原創",new String[]{"情緒","友情","科技"});
s.pages.add(page(0,"mysterious","阿洛","阿洛問自己做的小機器人點點：「你難過時會哭嗎？」點點的螢幕轉了好幾圈。","我的眼睛沒有眼淚裝置。","阿洛問機器人是否會哭",new String[]{"點點沒有流淚硬體"},new String[]{"不哭就不難過嗎？","機器人有感情嗎？"},"現實 AI 是否有主觀感受不可當成已知事實"));
s.pages.add(page(1,"tender","點點","隔天阿洛最愛的紙飛機被雨淋壞。他沒有哭，只安靜坐著。點點把音量調低，也坐在旁邊。","我不知道你需不需要修理，但我可以先陪你。","點點觀察阿洛的難過",new String[]{"阿洛沒哭但很難過"},new String[]{"陪著有什麼用？"},"不要宣稱點點代表現實 AI 真有感受"));
s.pages.add(page(2,"warm","阿洛","阿洛明白自己沒流眼淚，心裡卻真的很捨不得。他們把不同心情畫成符號：難過是小雨，緊張是打結的線。","哭只是很多表達方式裡的一種。","兩人探索情緒表達",new String[]{"情緒可用多種方式表達"},new String[]{"男生可以哭嗎？"},"不要對孩子情緒方式下單一標準"));
s.pages.add(page(3,"joyful","阿洛","阿洛重新折了紙飛機，把舊飛機的一小片貼在新機翼上。他沒有忘記難過，也沒有一直停在難過裡。","我想記得它，也想繼續往前。","阿洛帶著失落前進",new String[]{"難過與繼續生活可同時存在"},new String[]{"為什麼留舊紙片？"},"故事已完成"));
return s;}
private static StoryModel makebear(){StoryModel s=story("builtin_bear","🐻","不想分享的小熊","分享與界線","3–5",5,"Crew Story 原創",new String[]{"友情","界線","分享"});
s.pages.add(page(0,"warm","球球","球球把最喜歡的紅色小車緊緊抱在胸前，朋友碰一下都不行。","這是我的！我現在不想借！","球球不想分享小車",new String[]{"小車對球球很重要"},new String[]{"一定要分享嗎？"},"不要說孩子必須交出所有私人物品"));
s.pages.add(page(1,"tender","媽媽","媽媽沒有搶走小車，只問有沒有別的東西願意一起玩。球球看了玩具箱。","積木可以一起玩，小車我還不想。","球球說出界線",new String[]{"分享可以有界線"},new String[]{"可以說不嗎？"},"不要把拒絕分享說成錯"));
s.pages.add(page(2,"excited","球球","大家用積木蓋出長隧道。球球想了想，提議自己開紅色小車，朋友們當交通指揮員。","這樣我們可以一起玩！","球球提出可接受的共同玩法",new String[]{"球球仍保有小車控制權"},new String[]{"這算分享嗎？"},"不要透露他最後是否借車"));
s.pages.add(page(3,"joyful","球球","玩了一會兒，球球自己把車借給朋友一圈。不是有人逼他，而是他現在覺得放心了。","分享也可以先說清楚舒服的方式。","球球自願分享",new String[]{"自願分享和被迫分享不同"},new String[]{"下次還可以不借嗎？"},"故事已完成"));
return s;}
private static StoryModel makeredcap(){StoryModel s=story("builtin_redcap","🧺","小紅帽：森林裡的選擇","觀察與安全選擇","6–8",7,"依格林兄弟公版童話重新創作；Crew Story 全新文本",new String[]{"經典","森林","安全"});
s.pages.add(page(0,"warm","小紅帽","小紅帽帶著點心去探望外婆。媽媽提醒她走熟悉的小路，不要因好奇忘記方向。","我會記得先想一想再做決定。","小紅帽出發探望外婆",new String[]{"外婆住森林另一頭"},new String[]{"為什麼不能亂跑？"},"不要透露狼的計畫"));
s.pages.add(page(1,"mysterious","狼","一隻狼裝作很有禮貌，問她要去哪裡。小紅帽沒有說詳細位置。狼又指向遠處漂亮的花田。","那邊的花可真漂亮。","狼試著讓小紅帽離開原路",new String[]{"小紅帽沒有透露詳細地址"},new String[]{"狼真的好心嗎？"},"不要透露狼接下來做什麼"));
s.pages.add(page(2,"excited","小紅帽","她正想走向花田，忽然看見熟悉木牌，也注意到狼一直打聽事情。她決定留在原路，走向前方巡林的護林員。","我還是走我認識的路。","小紅帽向護林員靠近",new String[]{"小紅帽選擇不跟狼走"},new String[]{"她怎麼知道不對勁？"},"不要透露外婆狀況"));
s.pages.add(page(3,"joyful","外婆","護林員陪她走到小屋，外婆正在院子曬棉被。狼看到護林員就離開了。小紅帽平安送到點心。","你平安到達，就是今天最好的禮物。","小紅帽安全抵達",new String[]{"外婆平安","求助讓旅程更安全"},new String[]{"跟原版不一樣嗎？"},"故事已完成；可說這是 Crew Story 全新改寫"));
return s;}
private static StoryModel makefrog(){StoryModel s=story("builtin_frog","🐸","青蛙與金球","承諾與尊重","6–8",7,"依格林兄弟公版童話重新創作；Crew Story 全新文本",new String[]{"經典","承諾","友情"});
s.pages.add(page(0,"warm","公主","公主的金球掉進深井，一隻青蛙探出頭。","如果我幫你拿回來，你願意跟我做朋友嗎？","青蛙提出幫忙",new String[]{"金球掉進井裡"},new String[]{"公主要答應嗎？"},"不要透露後續"));
s.pages.add(page(1,"mysterious","公主","青蛙拿回金球後，公主承認自己剛才答應得太快，不知道怎麼跟青蛙做朋友。","我想守信用，但也想先說清楚。","公主面對匆忙承諾",new String[]{"青蛙已履行幫忙"},new String[]{"答應了就什麼都要做嗎？"},"承諾不代表放棄界線"));
s.pages.add(page(2,"tender","青蛙","青蛙沒有要求她做不舒服的事，只提議在花園聊一會兒。","真正的承諾，應該是兩個人都知道答應了什麼。","兩人重新協商",new String[]{"青蛙尊重公主界線"},new String[]{"可以重新約定嗎？"},"此改寫不需要變王子"));
s.pages.add(page(3,"joyful","公主","他們聊了井底的魚和屋頂的鳥，發現彼此很有趣。夕陽下，青蛙跳回井邊，約好明天再見。","明天見，朋友。","兩人成為朋友",new String[]{"友誼來自相互理解"},new String[]{"為什麼沒變王子？"},"故事已完成；可說這是全新改寫"));
return s;}
private static StoryModel makebremen(){StoryModel s=story("builtin_bremen","🎵","四個不退休的音樂家","合作與新舞台","6–8",7,"依格林兄弟公版《不來梅樂隊》重新創作；Crew Story 全新文本",new String[]{"經典","合作","音樂"});
s.pages.add(page(0,"tender","驢子","老驢子跑不快了，決定去不來梅試試當音樂家。","腿慢一點，不代表我的歌也老了。","老驢子尋找新生活",new String[]{"驢子年紀大仍想嘗試"},new String[]{"老了就沒用了嗎？"},"不要透露隊友"));
s.pages.add(page(1,"warm","狗","路上他遇到老狗、老貓和嗓門很大的公雞。每個都被嫌棄，卻發現大家都很會發出聲音。","一個人的缺點，放進樂團也許就是特色！","四個動物組隊",new String[]{"四個角色想找新舞台"},new String[]{"他們真的能合奏嗎？"},"不要透露森林小屋"));
s.pages.add(page(2,"mysterious","公雞","夜裡他們看到一間被壞傢伙占著的小屋。四個朋友在窗外一起唱出驚人的和弦，把屋裡的人嚇跑。","一、二、三——唱！","四個朋友合作製造聲響",new String[]{"合作產生巨大效果"},new String[]{"為什麼一起唱？"},"不要鼓勵危險堆疊模仿"));
s.pages.add(page(3,"joyful","驢子","他們沒有繼續趕路，而是在森林小屋辦起音樂會，鳥和兔子成了第一批觀眾。","我們不是不能做事，只是需要新的舞台。","四個朋友找到新生活",new String[]{"價值不只由原本工作決定"},new String[]{"還會去不來梅嗎？"},"故事已完成"));
return s;}
private static StoryModel makecinderella(){StoryModel s=story("builtin_cinderella","👠","灰姑娘與午夜舞會","勇氣與選擇","6–8",8,"依歐洲公版灰姑娘傳統故事重新創作；Crew Story 全新文本",new String[]{"經典","勇氣","選擇"});
s.pages.add(page(0,"tender","艾拉","艾拉每天做很多家事。城裡要辦舞會，她也想去，卻被要求留下工作。","我希望有人也在乎我的想法。","艾拉想參加舞會",new String[]{"艾拉承擔不公平家務"},new String[]{"她可以拒絕嗎？"},"不要透露魔法"));
s.pages.add(page(1,"mysterious","神秘園丁","曾受艾拉幫助的神秘園丁借她月光禮服和午夜會恢復原樣的馬車。","魔法只借你一晚，怎麼用由你決定。","艾拉得到限時魔法",new String[]{"魔法午夜結束","艾拉仍是自己"},new String[]{"為什麼午夜？"},"不要透露舞會結果"));
s.pages.add(page(2,"excited","艾拉","舞會上她跳舞、聊天，也向圖書館的人詢問工作。她和王子談的不是衣服，而是各自想改變的事。","我想先有一份屬於自己的工作。","艾拉利用舞會尋找機會",new String[]{"艾拉想更自主"},new String[]{"王子會愛上她嗎？"},"不要把婚姻當唯一解救"));
s.pages.add(page(3,"joyful","艾拉","午夜她離開時掉下一隻鞋。後來王子找到她，而艾拉也拿到了圖書館工作。兩人成為朋友，未來不急著一次決定。","我的未來不是鞋子替我決定的。","艾拉開始自主生活",new String[]{"艾拉改變不只依賴王子"},new String[]{"這還是灰姑娘嗎？"},"故事已完成；可說這是重新想像版本"));
return s;}
private static StoryModel makehansel(){StoryModel s=story("builtin_hansel","🍞","糖果屋外的麵包屑","合作與求助","6–8",8,"依格林兄弟公版《糖果屋》重新創作；Crew Story 全新文本",new String[]{"經典","合作","森林"});
s.pages.add(page(0,"mysterious","漢斯","漢斯和葛麗特在森林採野莓時遇上大霧，和家人走散。漢斯丟麵包屑做記號，鳥卻很快吃掉了。","好吃的東西不一定是好路標。","兄妹在森林走散",new String[]{"麵包屑會被鳥吃"},new String[]{"迷路怎麼辦？"},"不要透露糖果屋"));
s.pages.add(page(1,"warm","葛麗特","葛麗特提議停下來，用樹枝排箭頭，記住被雷劈過的大樹，兩人也不分開亂跑。","我們一起想，總比各自亂跑好。","兄妹合作留下地標",new String[]{"兄妹選擇一起行動"},new String[]{"為什麼不能分頭找？"},"不要透露下一發現"));
s.pages.add(page(2,"excited","老婆婆","霧散後，他們找到一間像糖果屋的森林點心研究站。屋主老婆婆先問有沒有大人知道他們在哪，再用無線電聯絡護林站。","先讓家人知道你們平安。","兄妹遇到研究站",new String[]{"老婆婆先協助聯絡家人"},new String[]{"她可信嗎？"},"可信任要看行為與求助管道，不只外表"));
s.pages.add(page(3,"joyful","葛麗特","護林員很快帶家人來到研究站。回家路上，漢斯把最後一塊麵包吃掉，葛麗特決定下次進森林前先學看地圖。","麵包還是拿來吃比較可靠！","兄妹與家人團聚",new String[]{"地標與求助幫助回家"},new String[]{"原版有巫婆嗎？"},"故事已完成；可說這是較溫和的全新改寫"));
return s;}
private static StoryModel story(String id,String e,String t,String sum,String age,int min,String note,String[] tags){
StoryModel s=new StoryModel();s.id=id;s.coverEmoji=e;s.title=t;s.summary=sum;s.ageGroup=age;s.estimatedMinutes=min;s.sourceType=StoryModel.SOURCE_BUILT_IN;s.copyrightNote=note;s.createdAt=1;s.tags.addAll(Arrays.asList(tags));return s;}
private static StoryModel.Page page(int i,String emotion,String ch,String text,String dia,String event,String[] facts,String[] hints,String spoiler){
StoryModel.Page p=new StoryModel.Page();p.pageIndex=i;p.emotion=emotion;p.characterName=ch;p.text=text;p.dialogue=dia;p.context.characters.add(ch);p.context.currentEvent=event;p.context.revealedFacts.addAll(Arrays.asList(facts));p.context.interactionHints.addAll(Arrays.asList(hints));p.context.spoilerBoundary=spoiler;return p;}
}