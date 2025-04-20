package com.example.autorecorder.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.autorecorder.INVALID_ID
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


@kotlinx.serialization.Serializable
//@Parcelize
data class Template(
    val title: String = " {yyyyMMdd_HHmm}",
    val mid: Long = INVALID_ID,
    val desc: String = "",
    val tag: String = "记录",
    val tid: Int = SubTid.FANS.tid,
    val copyright: Int = 1, // 1-原创 2-转载
    val source: String = "",
    val watermark: Int = 1, // 1-开启 0-关闭
    val isOnlySelf: Int = 0, // 0-所有 1-仅自己
//    val recreate: Int = -1, // -1-不允许 1-允许
    val noReprint: Int = 0, // 0-允许转载 1-禁止转载
): Serializable {
    val entity: TemplateEntity
        get() = TemplateEntity(
            title = title,
            mid = mid,
            desc = desc,
            tag = tag,
            tid = tid,
            copyright = copyright,
            source = source,
            watermark = watermark,
            isOnlySelf = isOnlySelf,
//            recreate = recreate,
            noReprint = noReprint,
        )

    fun displayTitle(date: Date = Date()): String {
        val regex = "\\{(.*?)\\}".toRegex() // Capture text inside {}
        val formatPattern = regex.find(title)?.groupValues?.get(1) ?: return title
        val formatter = SimpleDateFormat(formatPattern, Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai") // Set to Beijing Time (UTC+8)
        }
        return title.replace(regex, formatter.format(date))
    }

    val copyrightString: String
        get() = if (copyright == 1) "自制" else "转载"

    val watermarkString: String
        get() = if (watermark == 1) "有水印" else "无水印"

    val isOnlySelfString: String
        get() = if (isOnlySelf == 1) "仅自己" else "公开"

    //    val recreateString: String
//        get() = if (recreate == 1) "允许二创" else "不允许二创"
    val noReprintString: String
        get() = if (noReprint == 0) "允许转载" else "禁止转载"

    val subTid: SubTid?
        get() = SubTid.entries.firstOrNull { it.tid == tid }
    val mainTid: MainTid?
        get() = subTid?.parent
}

enum class MainTid(val tid: Int, val title: String) {
    DOUGA(1, "动画"),
    ANIME(13, "番剧"),
    GUOCHUANG(167, "国创"),
    MUSIC(3, "音乐"),
    DANCE(129, "舞蹈"),
    GAME(4, "游戏"),
    KNOWLEDGE(36, "知识"),
    TECH(188, "科技"),
    SPORTS(234, "运动"),
    CAR(223, "汽车"),
    LIFE(160, "生活"),
    FOOD(211, "美食"),
    ANIMAL(217, "动物圈"),
    KICHIKU(119, "鬼畜"),
    FASHION(155, "时尚"),
    INFORMATION(202, "资讯"),
    ENT(5, "娱乐"),
    CINEPHILE(181, "影视"),
    DOCUMENTARY(177, "纪录片"),
    MOVIE(23, "电影"),
    TV(11, "电视剧")
}

enum class SubTid(val tid: Int, val title: String, val parent: MainTid) {
    // Animation (Douga) subcategories
    MAD(24, "MAD·AMV", MainTid.DOUGA),
    MMD(25, "MMD·3D", MainTid.DOUGA),
    HANDDRAWN(47, "短片·手书", MainTid.DOUGA),
    VOICE(257, "配音", MainTid.DOUGA),
    GARAGE_KIT(210, "手办·模玩", MainTid.DOUGA),
    TOKUSATSU(86, "特摄", MainTid.DOUGA),
    ACGNTALKS(253, "动漫杂谈", MainTid.DOUGA),
    OTHER_DOUGA(27, "综合", MainTid.DOUGA),

    // Anime subcategories
    INFORMATION_ANIME(51, "资讯", MainTid.ANIME),
    OFFICAL(152, "官方延伸", MainTid.ANIME),
    FINISH(32, "完结动画", MainTid.ANIME),
    SERIAL(33, "连载动画", MainTid.ANIME),

    // Guochuang subcategories
    CHINESE(153, "国产动画", MainTid.GUOCHUANG),
    ORIGINAL(168, "国产原创相关", MainTid.GUOCHUANG),
    PUPPETRY(169, "布袋戏", MainTid.GUOCHUANG),
    INFORMATION_GUOCHUANG(170, "资讯", MainTid.GUOCHUANG),
    MOTIONCOMIC(195, "动态漫·广播剧", MainTid.GUOCHUANG),

    // Music subcategories
    ORIGINAL_MUSIC(28, "原创音乐", MainTid.MUSIC),
    COVER(31, "翻唱", MainTid.MUSIC),
    VOCALOID(30, "VOCALOID·UTAU", MainTid.MUSIC),
    PERFORM(59, "演奏", MainTid.MUSIC),
    MV(193, "MV", MainTid.MUSIC),
    LIVE(29, "音乐现场", MainTid.MUSIC),
    OTHER_MUSIC(130, "音乐综合", MainTid.MUSIC),
    COMMENTARY(243, "乐评盘点", MainTid.MUSIC),
    TUTORIAL(244, "音乐教学", MainTid.MUSIC),

    // Dance subcategories
    OTAKU(20, "宅舞", MainTid.DANCE),
    THREE_D(154, "舞蹈综合", MainTid.DANCE),
    DEMO(156, "舞蹈教程", MainTid.DANCE),
    HIPHOP(198, "街舞", MainTid.DANCE),
    STAR(199, "明星舞蹈", MainTid.DANCE),
    CHINA(200, "国风舞蹈", MainTid.DANCE),
    GESTURES(255, "手势·网红舞", MainTid.DANCE),

    // Game subcategories
    STAND_ALONE(17, "单机游戏", MainTid.GAME),
    ESPORTS(171, "电子竞技", MainTid.GAME),
    MOBILE(172, "手机游戏", MainTid.GAME),
    ONLINE(65, "网络游戏", MainTid.GAME),
    BOARD(173, "桌游棋牌", MainTid.GAME),
    GMV(121, "GMV", MainTid.GAME),
    MUSIC_GAME(136, "音游", MainTid.GAME),
    MUGEN(19, "Mugen", MainTid.GAME),

    // Knowledge subcategories
    SCIENCE(201, "科学科普", MainTid.KNOWLEDGE),
    SOCIAL_SCIENCE(124, "社科·法律·心理", MainTid.KNOWLEDGE),
    HUMANITY_HISTORY(228, "人文历史", MainTid.KNOWLEDGE),
    BUSINESS(207, "财经商业", MainTid.KNOWLEDGE),
    CAMPUS(208, "校园学习", MainTid.KNOWLEDGE),
    CAREER(209, "职业职场", MainTid.KNOWLEDGE),
    DESIGN(229, "设计·创意", MainTid.KNOWLEDGE),
    SKILL(122, "野生技术协会", MainTid.KNOWLEDGE),

    // Tech subcategories
    DIGITAL(95, "数码", MainTid.TECH),
    APPLICATION(230, "软件应用", MainTid.TECH),
    COMPUTER_TECH(231, "计算机技术", MainTid.TECH),
    INDUSTRY(232, "科工机械", MainTid.TECH),
    DIY(233, "极客DIY", MainTid.TECH),

    // Sports subcategories
    BASKETBALL(235, "篮球", MainTid.SPORTS),
    FOOTBALL(249, "足球", MainTid.SPORTS),
    AEROBICS(164, "健身", MainTid.SPORTS),
    ATHLETIC(236, "竞技体育", MainTid.SPORTS),
    CULTURE_SPORTS(237, "运动文化", MainTid.SPORTS),
    COMPREHENSIVE(238, "运动综合", MainTid.SPORTS),

    // Car subcategories
    KNOWLEDGE_CAR(258, "汽车知识科普", MainTid.CAR),
    RACING(245, "赛车", MainTid.CAR),
    MODIFIEDVEHICLE(246, "改装玩车", MainTid.CAR),
    NEWENERGYVEHICLE(247, "新能源车", MainTid.CAR),
    TOURINGCAR(248, "房车", MainTid.CAR),
    MOTORCYCLE(240, "摩托车", MainTid.CAR),
    STRATEGY(227, "购车攻略", MainTid.CAR),
    LIFE_CAR(176, "汽车生活", MainTid.CAR),

    // Life subcategories
    FUNNY(138, "搞笑", MainTid.LIFE),
    TRAVEL(250, "出行", MainTid.LIFE),
    RURALLIFE(251, "三农", MainTid.LIFE),
    HOME(239, "家居房产", MainTid.LIFE),
    HANDMAKE(161, "手工", MainTid.LIFE),
    PAINTING(162, "绘画", MainTid.LIFE),
    DAILY(21, "日常", MainTid.LIFE),
    PARENTING(254, "亲子", MainTid.LIFE),

    // Food subcategories
    MAKE(76, "美食制作", MainTid.FOOD),
    DETECTIVE(212, "美食侦探", MainTid.FOOD),
    MEASUREMENT(213, "美食测评", MainTid.FOOD),
    RURAL(214, "田园美食", MainTid.FOOD),
    RECORD(215, "美食记录", MainTid.FOOD),

    // Animal subcategories
    CAT(218, "喵星人", MainTid.ANIMAL),
    DOG(219, "汪星人", MainTid.ANIMAL),
    SECOND_EDITION(220, "动物二创", MainTid.ANIMAL),
    WILD_ANIMAL(221, "野生动物", MainTid.ANIMAL),
    REPTILES(222, "小宠异宠", MainTid.ANIMAL),
    ANIMAL_COMPOSITE(75, "动物综合", MainTid.ANIMAL),

    // Kichiku subcategories
    GUIDE(22, "鬼畜调教", MainTid.KICHIKU),
    MAD_KICHIKU(26, "音MAD", MainTid.KICHIKU),
    MANUAL_VOCALOID(126, "人力VOCALOID", MainTid.KICHIKU),
    THEATRE(216, "鬼畜剧场", MainTid.KICHIKU),
    COURSE(127, "教程演示", MainTid.KICHIKU),

    // Fashion subcategories
    MAKEUP(157, "美妆护肤", MainTid.FASHION),
    COS(252, "仿妆cos", MainTid.FASHION),
    CLOTHING(158, "穿搭", MainTid.FASHION),
    CATWALK(159, "时尚潮流", MainTid.FASHION),

    // Information subcategories
    HOTSPOT(203, "热点", MainTid.INFORMATION),
    GLOBAL(204, "环球", MainTid.INFORMATION),
    SOCIAL(205, "社会", MainTid.INFORMATION),
    MULTIPLE(206, "综合", MainTid.INFORMATION),

    // Entertainment subcategories
    VARIETY(71, "综艺", MainTid.ENT),
    TALKER(241, "娱乐杂谈", MainTid.ENT),
    FANS(242, "粉丝创作", MainTid.ENT),
    CELEBRITY(137, "明星综合", MainTid.ENT),

    // Cinephile subcategories
    CINECISM(182, "影视杂谈", MainTid.CINEPHILE),
    MONTAGE(183, "影视剪辑", MainTid.CINEPHILE),
    SHORTFILM(85, "小剧场", MainTid.CINEPHILE),
    TRAILER_INFO(184, "预告·资讯", MainTid.CINEPHILE),
    SHORTFILM_CINEPHILE(256, "短片", MainTid.CINEPHILE),

    // Documentary subcategories
    HISTORY(37, "人文·历史", MainTid.DOCUMENTARY),
    SCIENCE_DOC(178, "科学·探索·自然", MainTid.DOCUMENTARY),
    MILITARY_DOC(179, "军事", MainTid.DOCUMENTARY),
    TRAVEL_DOC(180, "社会·美食·旅行", MainTid.DOCUMENTARY),

    // Movie subcategories
    CHINESE_MOVIE(147, "华语电影", MainTid.MOVIE),
    WEST(145, "欧美电影", MainTid.MOVIE),
    JAPAN(146, "日本电影", MainTid.MOVIE),
    MOVIE_OTHER(83, "其他国家", MainTid.MOVIE),

    // TV subcategories
    MAINLAND(185, "国产剧", MainTid.TV),
    OVERSEAS(187, "海外剧", MainTid.TV)
}

@Entity(
    tableName = "templates",
)
data class TemplateEntity(
    @PrimaryKey val title: String,
    @ColumnInfo(name = "mid") val mid: Long,
    @ColumnInfo(name = "desc") val desc: String,
    @ColumnInfo(name = "tag") val tag: String,
    @ColumnInfo(name = "tid") val tid: Int,
    @ColumnInfo(name = "copyright") val copyright: Int,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "watermark") val watermark: Int,
    @ColumnInfo(name = "isOnlySelf") val isOnlySelf: Int,
//    @ColumnInfo(name = "recreate") val recreate: Int,
    @ColumnInfo(name = "noReprint") val noReprint: Int,
) {
    val item: Template
        get() = Template(
            title = title,
            mid = mid,
            desc = desc,
            tag = tag,
            tid = tid,
            copyright = copyright,
            source = source,
            watermark = watermark,
            isOnlySelf = isOnlySelf,
//            recreate = recreate,
            noReprint = noReprint,
        )
}