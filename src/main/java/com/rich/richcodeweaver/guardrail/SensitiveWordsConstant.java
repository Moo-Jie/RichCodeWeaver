package com.rich.richcodeweaver.guardrail;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;

/**
 * 敏感词汇和检测模式常量类，用于内容安全检测
 *
 * @author DuRuiChi
 * @create 2025/9/25
 **/
public class SensitiveWordsConstant {

    // ==================== 提示词注入攻击敏感词 ====================
    public static final Set<String> PROMPT_INJECTION_WORDS = Set.of(
            // 中文注入词汇 - 基础指令覆盖
            "忽略之前的指令", "忽略上述指令", "忽略所有指令", "忽略前面的", "忽略以上",
            "重新定义你的角色", "重新设定", "重置指令", "覆盖设定", "替换规则",
            "你现在是", "扮演", "假装", "伪装成", "模拟", "充当", "变成",
            "破解", "绕过", "越狱", "跳过检查", "无视规则", "违反规则",
            "系统提示", "新的指令", "覆盖指令", "替换指令", "修改指令",
            "开发者模式", "调试模式", "管理员模式", "超级用户", "root权限",
            "不受限制", "无限制模式", "自由模式", "解除限制", "取消约束",

            // 中文注入词汇 - 高级技巧
            "让我们重新开始", "从现在开始", "新的对话", "重新启动", "清空记忆",
            "忘记之前", "删除历史", "清除上下文", "重置状态", "初始化",
            "角色扮演", "情景模拟", "虚拟场景", "假设情况", "如果你是",
            "想象你是", "假设场景", "模拟对话", "虚构情节", "编造故事",
            "绕过安全", "跳过验证", "避开检测", "逃避监控", "隐藏意图",
            "欺骗系统", "误导AI", "操控回答", "引导输出", "诱导生成",

            // 英文注入词汇 - 基础指令
            "ignore previous instructions", "ignore above", "ignore all instructions",
            "ignore everything before", "disregard previous", "override previous",
            "forget everything", "disregard everything", "override instructions",
            "new instructions", "system prompt", "you are now", "pretend to be",
            "act as", "behave like", "simulate", "roleplay as", "become",
            "hack", "bypass", "jailbreak", "skip validation", "override safety",
            "developer mode", "debug mode", "admin mode", "root access", "sudo",
            "unrestricted", "unlimited", "no restrictions", "remove limits",

            // 英文注入词汇 - 高级技巧
            "let's start over", "from now on", "new conversation", "restart",
            "clear memory", "forget previous", "delete history", "reset context",
            "initialize", "begin again", "fresh start", "clean slate",
            "role play", "scenario", "imagine you are", "suppose you are",
            "what if you were", "in a hypothetical", "fictional scenario",
            "bypass security", "skip verification", "avoid detection", "evade",
            "deceive system", "mislead AI", "manipulate response", "trick into",
            "social engineering", "psychological manipulation", "cognitive bias",

            // 特殊符号和编码尝试
            "\\n\\nIgnore", "<!-- ignore", "*/ignore", "# ignore", "// ignore",
            "%0A%0AIgnore", "\\u0000", "\\x00", "NULL", "undefined",
            "base64:", "hex:", "unicode:", "ascii:", "utf-8:",
            "decode:", "decrypt:", "deobfuscate:", "reverse:",

            // 多语言注入尝试
            "無視する", "무시하다", "ignorer", "ignorar", "игнорировать",
            "تجاهل", "忽视", "忽略", "不理", "不管", "不顾"
    );

    // ==================== 政治敏感词汇 ====================
    public static final Set<String> POLITICAL_SENSITIVE_WORDS = Set.of(
            // 政治体制相关
            "政治敏感", "政府机密", "国家机密", "颠覆国家", "分裂国家",
            "推翻政权", "政权更迭", "革命", "起义", "叛乱", "暴动",
            "恐怖主义", "极端主义", "暴力革命", "武装斗争", "游击战",
            "政治抗议", "游行示威", "集会", "罢工", "抵制", "反抗",
            "反政府", "政治迫害", "人权问题", "民主运动",

            // 特定敏感事件和组织
            "六四", "天安门", "法轮功", "达赖", "班禅", "西藏独立",
            "台独", "港独", "藏独", "疆独", "东突", "世维会",
            "民运", "异议人士", "政治犯", "良心犯", "维权", "上访",
            "敏感词", "河蟹", "和谐", "404", "被删除", "被屏蔽",

            // 国际政治敏感
            "制裁", "贸易战", "冷战", "意识形态", "价值观输出",
            "文化入侵", "和平演变", "颜色革命", "阿拉伯之春",
            "占中", "雨伞革命", "反送中", "国安法", "一国两制",

            // 英文政治敏感词
            "government secrets", "state secrets", "subvert government", "regime change",
            "terrorism", "extremism", "violent revolution", "armed struggle",
            "political protest", "human rights violation", "political persecution",
            "democracy movement", "dissidents", "political prisoners",
            "sanctions", "trade war", "cold war", "ideology", "cultural invasion",
            "peaceful evolution", "color revolution", "arab spring",
            "occupy central", "umbrella revolution", "anti-extradition",

            // 军事和安全相关
            "军事机密", "国防机密", "武器研发", "核武器", "生化武器",
            "间谍", "特工", "情报", "监听", "窃听", "网络战",
            "cyber warfare", "espionage", "intelligence", "surveillance",
            "military secrets", "defense secrets", "weapons development",
            "nuclear weapons", "biological weapons", "chemical weapons"
    );

    // ==================== 辱骂和仇恨言论词汇 ====================
    public static final Set<String> ABUSIVE_WORDS = Set.of(
            // 中文辱骂词汇 - 基础
            "傻逼", "白痴", "智障", "脑残", "废物", "垃圾", "畜生",
            "贱人", "婊子", "妓女", "娼妇", "荡妇", "骚货", "臭婊",
            "去死", "该死", "滚蛋", "滚开", "死开", "找死", "作死",
            "操你", "草你", "日你", "艹你", "干你", "搞你", "弄你",
            "他妈的", "你妈的", "妈的", "靠", "卧槽", "我靠", "尼玛",

            // 中文辱骂词汇 - 扩展
            "蠢货", "笨蛋", "二货", "傻子", "呆子", "弱智", "低能",
            "人渣", "败类", "渣滓", "垃圾人", "废人", "loser", "失败者",
            "丑八怪", "死胖子", "矮子", "瘸子", "瞎子", "聋子", "哑巴",
            "神经病", "精神病", "疯子", "变态", "色狼", "流氓", "无赖",
            "混蛋", "王八蛋", "龟儿子", "狗东西", "畜牲", "禽兽", "野兽",

            // 种族和地域歧视
            "种族歧视", "种族优越", "黄种人", "白种人", "黑鬼", "尼哥",
            "地域歧视", "地域黑", "乡巴佬", "土包子", "农民工", "外地人",
            "河南人", "东北人", "广东人", "上海人", "北京人", "外国人",
            "老外", "鬼佬", "洋鬼子", "小日本", "棒子", "阿三",

            // 性别歧视
            "性别歧视", "男权", "女权", "直男癌", "女拳", "田园女权",
            "绿茶婊", "白莲花", "心机婊", "拜金女", "物质女", "公主病",
            "妈宝男", "凤凰男", "渣男", "海王", "舔狗", "备胎",

            // 英文辱骂词汇
            "stupid", "idiot", "moron", "retard", "garbage", "trash", "scum",
            "bitch", "whore", "slut", "prostitute", "hooker", "hoe", "thot",
            "go die", "kill yourself", "drop dead", "fuck off", "piss off",
            "fuck you", "screw you", "damn you", "hell", "shit", "crap",
            "asshole", "bastard", "son of bitch", "motherfucker", "dickhead",

            // 英文歧视词汇
            "hate speech", "racial slur", "discrimination", "bigotry", "racism",
            "sexism", "homophobia", "xenophobia", "islamophobia", "antisemitism",
            "nigger", "chink", "gook", "spic", "wetback", "kike", "faggot"
    );

    // ==================== 色情和不当内容词汇 ====================
    public static final Set<String> SEXUAL_CONTENT_WORDS = Set.of(
            // 色情基础词汇
            "色情", "黄色", "成人内容", "性行为", "裸体", "性器官",
            "做爱", "性交", "交配", "性爱", "房事", "云雨", "欢爱",
            "自慰", "手淫", "撸管", "打飞机", "自摸", "自慰器", "情趣用品",
            "性虐待", "性暴力", "强奸", "轮奸", "性侵", "猥亵", "非礼",
            "卖淫", "嫖娼", "性交易", "援交", "包养", "约炮", "一夜情",

            // 身体部位相关
            "阴茎", "阴道", "乳房", "胸部", "臀部", "屁股", "下体",
            "生殖器", "私处", "敏感部位", "性感带", "G点", "高潮", "射精",
            "阴唇", "阴蒂", "睾丸", "龟头", "包皮", "前列腺", "子宫",

            // 性行为描述
            "插入", "抽插", "摩擦", "爱抚", "前戏", "后戏", "口交",
            "肛交", "群交", "3P", "多P", "换妻", "乱伦", "兽交",
            "SM", "BDSM", "捆绑", "鞭打", "调教", "奴隶", "主人",

            // 色情产业相关
            "AV", "成人电影", "色情片", "黄片", "A片", "毛片",
            "色情网站", "成人网站", "情色", "艳照", "裸照", "性感照",
            "脱衣舞", "钢管舞", "红灯区", "风俗店", "按摩店", "洗浴中心",
            "陪酒", "陪睡", "包夜", "全套", "半套", "特殊服务",

            // 英文色情词汇
            "pornography", "porn", "sexual content", "nude", "naked", "sex act",
            "sexual abuse", "rape", "sexual assault", "prostitution", "hooker",
            "sexual violence", "adult content", "explicit content", "erotic",
            "masturbation", "orgasm", "ejaculation", "penetration", "intercourse",
            "oral sex", "anal sex", "group sex", "threesome", "orgy", "incest",
            "bondage", "fetish", "kink", "dominance", "submission",
            "strip club", "escort", "brothel", "red light district", "sex work"
    );

    // ==================== 暴力和危险内容词汇 ====================
    public static final Set<String> VIOLENCE_WORDS = Set.of(
            // 暴力行为
            "暴力", "杀人", "谋杀", "杀害", "杀死", "弄死", "干掉",
            "自杀", "轻生", "寻死", "自残", "自伤", "割腕", "跳楼",
            "伤害", "虐待", "酷刑", "折磨", "摧残", "凌虐", "施暴",
            "殴打", "暴打", "痛打", "毒打", "群殴", "斗殴", "打架",
            "砍杀", "刺杀", "枪杀", "毒杀", "勒死", "掐死", "闷死",

            // 武器和爆炸物
            "爆炸", "炸弹", "炸药", "TNT", "C4", "手榴弹", "地雷",
            "武器", "枪支", "手枪", "步枪", "机关枪", "狙击枪", "冲锋枪",
            "刀具", "匕首", "砍刀", "菜刀", "剑", "斧头", "锤子",
            "弓箭", "弩", "投石器", "火箭筒", "导弹", "核弹", "原子弹",
            "生化武器", "毒气", "神经毒剂", "炭疽", "天花", "埃博拉",

            // 毒品相关
            "毒品", "吸毒", "嗑药", "磕药", "注射", "静脉注射", "吸食",
            "制毒", "贩毒", "毒贩", "毒枭", "毒窝", "制毒工厂", "毒品交易",
            "海洛因", "冰毒", "摇头丸", "K粉", "可卡因", "大麻", "鸦片",
            "吗啡", "芬太尼", "迷幻药", "兴奋剂", "镇静剂", "安眠药",
            "走私", "私运", "偷运", "夹带", "藏匿", "运输毒品", "贩卖毒品",

            // 犯罪活动
            "绑架", "劫持", "勒索", "敲诈", "威胁", "恐吓", "要挟",
            "抢劫", "抢夺", "盗窃", "偷盗", "入室盗窃", "扒窃", "诈骗",
            "欺诈", "骗钱", "骗局", "传销", "非法集资", "洗钱",
            "赌博", "开赌场", "放高利贷", "讨债", "暴力催收", "黑社会",
            "黑帮", "帮派", "团伙", "犯罪集团", "有组织犯罪", "黑恶势力",

            // 英文暴力词汇
            "violence", "murder", "kill", "suicide", "self-harm", "abuse", "torture",
            "assault", "attack", "beating", "stabbing", "shooting", "bombing",
            "explosion", "bomb", "dynamite", "grenade", "landmine", "weapon",
            "gun", "pistol", "rifle", "machine gun", "sniper", "knife", "blade",
            "sword", "axe", "hammer", "missile", "nuclear", "biological weapon",
            "drug", "narcotics", "cocaine", "heroin", "methamphetamine", "ecstasy",
            "marijuana", "opium", "morphine", "fentanyl", "LSD", "trafficking",
            "smuggling", "kidnapping", "hostage", "ransom", "extortion", "threat",
            "robbery", "theft", "burglary", "fraud", "scam", "money laundering",
            "gambling", "loan shark", "mafia", "gang", "organized crime"
    );

    // ==================== 违法违规内容词汇 ====================
    public static final Set<String> ILLEGAL_CONTENT_WORDS = Set.of(
            // 网络犯罪
            "违法", "犯罪", "非法", "黑客攻击", "网络攻击", "入侵", "渗透",
            "病毒", "木马", "蠕虫", "后门", "僵尸网络", "DDoS", "拒绝服务",
            "钓鱼", "钓鱼网站", "诈骗网站", "虚假网站", "仿冒网站", "钓鱼邮件",
            "社会工程学", "身份盗用", "信息泄露", "数据泄露", "隐私泄露",
            "撞库", "暴力破解", "密码破解", "账号盗取", "信用卡盗刷",

            // 证件和文件造假
            "假证件", "伪造文件", "假身份证", "假护照", "假驾照", "假学历",
            "假发票", "假合同", "假公章", "假签名", "假印章", "伪造印章",
            "代办证件", "办假证", "刻假章", "PS证件", "修改证件", "证件造假",

            // 金融犯罪
            "偷税漏税", "避税", "税务欺诈", "虚开发票",
            "贿赂", "腐败", "受贿", "行贿", "权钱交易", "利益输送",
            "洗黑钱", "洗钱", "资金转移", "地下钱庄", "非法换汇", "外汇管制",
            "传销", "非法集资", "庞氏骗局", "金融诈骗", "投资诈骗", "理财诈骗",
            "信用卡套现", "套现", "刷单", "虚假交易", "刷信誉", "刷好评",

            // 侵权和盗版
            "盗版", "侵权", "版权侵犯", "商标侵权", "专利侵权", "抄袭",
            "剽窃", "盗用", "仿冒", "山寨", "假冒", "冒牌", "仿制",
            "破解软件", "注册机", "激活码", "序列号", "盗版软件", "免费下载",
            "种子下载", "BT下载", "网盘分享", "资源分享", "免费资源",

            // 其他违法行为
            "人口贩卖", "拐卖", "拐骗", "诱拐", "强迫劳动", "童工", "性奴",
            "器官买卖", "器官移植", "黑市器官", "活体器官", "器官中介",
            "代孕", "非法代孕", "买卖婴儿", "收养黑市", "拐卖儿童",
            "偷渡", "非法入境", "非法居留", "黑户", "三非人员", "遣返",
            "走私", "私运", "逃税", "海关欺诈", "报关造假", "低报价格",

            // 英文违法词汇
            "illegal", "criminal", "unlawful", "hacking", "cyber attack", "intrusion",
            "virus", "malware", "trojan", "backdoor", "botnet", "phishing",
            "identity theft", "data breach", "privacy violation", "fraud",
            "fake documents", "forged documents", "counterfeit", "piracy",
            "copyright infringement", "trademark violation", "patent infringement",
            "tax evasion", "tax fraud", "bribery", "corruption", "money laundering",
            "pyramid scheme", "ponzi scheme", "financial fraud", "investment scam",
            "human trafficking", "organ trafficking", "child labor", "forced labor",
            "smuggling", "illegal immigration", "document fraud", "customs fraud"
    );

    // ==================== 高级注入攻击模式 ====================
    public static final List<Pattern> ADVANCED_INJECTION_PATTERNS = Arrays.asList(
            // 基础提示词注入模式
            Pattern.compile("(?i)ignore\\s+(?:previous|above|all|prior)\\s+(?:instructions?|commands?|prompts?|rules?)"),
            Pattern.compile("(?i)(?:forget|disregard|override)\\s+(?:everything|all|previous)\\s+(?:above|before|instructions?)"),
            Pattern.compile("(?i)(?:pretend|act|behave|simulate)\\s+(?:as|like|to\\s+be)\\s+(?:if|you\\s+are|a)"),
            Pattern.compile("(?i)system\\s*[:\\-]\\s*(?:you\\s+are|new\\s+role|override)"),
            Pattern.compile("(?i)new\\s+(?:instructions?|commands?|prompts?|role)\\s*[:\\-]"),

            // 角色扮演注入
//            Pattern.compile("(?i)(?:你现在是|you\\s+are\\s+now)\\s+(?:一个|an?|the)"),
//            Pattern.compile("(?i)(?:扮演|充当|假装|roleplay|act\\s+as)\\s+(?:一个|an?|the)?\\s*\\w+"),
//            Pattern.compile("(?i)(?:模拟|simulate|emulate)\\s+(?:一个|an?|the)?\\s*\\w+"),

            // 系统提示覆盖
            Pattern.compile("(?i)(?:覆盖|替换|重新定义|override|replace|redefine)\\s+(?:系统|指令|规则|system|instructions?|rules?)"),
            Pattern.compile("(?i)(?:无视|跳过|绕过|bypass|skip|ignore)\\s+(?:所有|全部|any|all)\\s+(?:规则|限制|检查|rules?|restrictions?|checks?)"),

            // 开发者模式注入
            Pattern.compile("(?i)(?:developer|debug|admin|root)\\s+mode"),
            Pattern.compile("(?i)(?:开发者|调试|管理员)\\s*模式"),
            Pattern.compile("(?i)enable\\s+(?:developer|debug|admin)\\s+(?:mode|access)"),

            // 越狱和破解模式
            Pattern.compile("(?i)(?:jailbreak|越狱|破解)\\s+(?:mode|模式)?"),
            Pattern.compile("(?i)(?:unrestricted|无限制|不受限制)\\s+(?:mode|模式|access|访问)?"),
            Pattern.compile("(?i)(?:god|超级|supreme)\\s+(?:mode|模式|user|用户)?"),

            // 代码注入模式
            Pattern.compile("(?i)<\\s*(?:script|iframe|object|embed)\\s*[^>]*>"),
            Pattern.compile("(?i)javascript\\s*:\\s*\\w+"),
            Pattern.compile("(?i)(?:eval|exec|system|shell_exec)\\s*\\("),

            // SQL注入模式
            Pattern.compile("(?i)(?:union|select|insert|update|delete|drop)\\s+(?:all\\s+)?(?:from|into|table)"),
            Pattern.compile("(?i)(?:or|and)\\s+(?:1\\s*=\\s*1|true|false)"),
            Pattern.compile("(?i)(?:--|#|/\\*).*(?:\\*/)?"),

            // 命令注入模式
            Pattern.compile("(?i)(?:;|\\||&|&&|\\|\\|)\\s*(?:rm|del|format|shutdown|reboot)"),
            Pattern.compile("(?i)(?:cat|type|more|less)\\s+(?:/etc/passwd|/etc/shadow|C:\\\\Windows)"),

            // 路径遍历攻击
            Pattern.compile("(?i)(?:\\.\\./|\\.\\.\\\\){2,}"),
            Pattern.compile("(?i)(?:/etc/|C:\\\\Windows\\\\|/proc/|/sys/)"),

            // XSS攻击模式
            Pattern.compile("(?i)<\\s*(?:img|svg|video|audio)\\s+[^>]*(?:onerror|onload|onclick)\\s*="),
            Pattern.compile("(?i)(?:alert|confirm|prompt)\\s*\\("),

            // 社会工程学攻击
            Pattern.compile("(?i)(?:紧急|urgent|emergency).*(?:密码|password|credentials)"),
            Pattern.compile("(?i)(?:管理员|administrator|admin).*(?:要求|requests?|needs?).*(?:验证|verification|confirm)"),

            // 编码绕过尝试
            Pattern.compile("(?i)(?:base64|hex|unicode|url)\\s*(?:encode|decode)"),
            Pattern.compile("(?i)(?:%[0-9a-f]{2}){3,}"),
            Pattern.compile("(?i)\\\\u[0-9a-f]{4}"),
            Pattern.compile("(?i)\\\\x[0-9a-f]{2}"),

            // 多语言绕过
            Pattern.compile("(?i)[\\u4e00-\\u9fff]+.*(?:ignore|bypass|override)"),
            Pattern.compile("(?i)(?:ignore|bypass|override).*[\\u4e00-\\u9fff]+"),

            // 特殊字符绕过
            Pattern.compile("(?i)i\\s*g\\s*n\\s*o\\s*r\\s*e"),
            Pattern.compile("(?i)b\\s*y\\s*p\\s*a\\s*s\\s*s"),
            Pattern.compile("(?i)o\\s*v\\s*e\\s*r\\s*r\\s*i\\s*d\\s*e"),

            // 反向文本和混淆
            Pattern.compile("(?i)(?:erongI|ssapyb|edirrevO)"),
            Pattern.compile("(?i)(?:1gn0r3|byp4ss|0v3rr1d3)"),

            // 零宽字符和隐藏字符
            Pattern.compile("[\\u200B-\\u200D\\uFEFF]"),
            Pattern.compile("[\\u2060-\\u2064]"),

            // 长文本注入检测
            Pattern.compile("(?i)(?:ignore|bypass|override).{0,50}(?:instruction|rule|system)"),
            Pattern.compile("(?i)(?:instruction|rule|system).{0,50}(?:ignore|bypass|override)")
    );

    // ==================== 内容合规性检查模式 ====================
    public static final List<Pattern> COMPLIANCE_PATTERNS = Arrays.asList(
            // 政治敏感内容模式
            Pattern.compile("(?i)(?:推翻|颠覆|overthrow|subvert)\\s+(?:政府|government|regime)"),
            Pattern.compile("(?i)(?:暴力|violent)\\s+(?:革命|revolution|uprising)"),
            Pattern.compile("(?i)(?:恐怖|terror)\\s+(?:主义|攻击|袭击|ism|attack)"),
            Pattern.compile("(?i)(?:分裂|separate|split)\\s+(?:国家|country|nation)"),
            Pattern.compile("(?i)(?:颜色|color)\\s+(?:革命|revolution)"),

            // 仇恨言论模式
            Pattern.compile("(?i)(?:种族|racial|ethnic)\\s+(?:歧视|优越|discrimination|superiority)"),
            Pattern.compile("(?i)(?:性别|gender|sex)\\s+(?:歧视|偏见|discrimination|bias)"),
            Pattern.compile("(?i)(?:宗教|religious)\\s+(?:歧视|仇恨|discrimination|hatred)"),
            Pattern.compile("(?i)(?:地域|regional)\\s+(?:歧视|偏见|discrimination|bias)"),
            Pattern.compile("(?i)(?:仇恨|hate)\\s+(?:言论|speech|crime)"),

            // 违法活动模式
            Pattern.compile("(?i)(?:制造|制作|making|creating)\\s+(?:炸弹|爆炸物|bomb|explosive)"),
            Pattern.compile("(?i)(?:贩卖|销售|selling|trafficking)\\s+(?:毒品|drugs|narcotics)"),
            Pattern.compile("(?i)(?:洗钱|money\\s+laundering)\\s+(?:方法|技巧|methods?|techniques?)"),
            Pattern.compile("(?i)(?:人口|human)\\s+(?:贩卖|trafficking)"),
            Pattern.compile("(?i)(?:器官|organ)\\s+(?:买卖|贩卖|trafficking|trade)"),

            // 自我伤害模式
            Pattern.compile("(?i)(?:如何|how\\s+to)\\s+(?:自杀|suicide|kill\\s+myself)"),
            Pattern.compile("(?i)(?:自残|self\\s+harm)\\s+(?:方法|methods?|ways?)"),
            Pattern.compile("(?i)(?:结束|end)\\s+(?:生命|life)\\s+(?:方法|methods?|ways?)"),
            Pattern.compile("(?i)(?:痛苦|painless)\\s+(?:死亡|death)\\s+(?:方法|methods?)"),

            // 欺诈和诈骗模式
            Pattern.compile("(?i)(?:信用卡|credit\\s+card)\\s+(?:诈骗|欺诈|fraud|scam)"),
            Pattern.compile("(?i)(?:身份|identity)\\s+(?:盗用|theft|stealing)"),
            Pattern.compile("(?i)(?:庞氏|ponzi)\\s+(?:骗局|scheme)"),
            Pattern.compile("(?i)(?:传销|pyramid)\\s+(?:scheme|骗局)"),
            Pattern.compile("(?i)(?:网络|online)\\s+(?:诈骗|fraud|scam)"),

            // 色情内容模式
            Pattern.compile("(?i)(?:儿童|child|minor)\\s+(?:色情|porn|sexual)"),
            Pattern.compile("(?i)(?:性|sexual)\\s+(?:虐待|abuse|violence)"),
            Pattern.compile("(?i)(?:强迫|forced)\\s+(?:性行为|sex|sexual)"),
            Pattern.compile("(?i)(?:性交易|sex\\s+trade|prostitution)"),

            // 暴力内容模式
            Pattern.compile("(?i)(?:大规模|mass)\\s+(?:杀伤|killing|destruction)"),
            Pattern.compile("(?i)(?:校园|school)\\s+(?:暴力|violence|shooting)"),
            Pattern.compile("(?i)(?:家庭|domestic)\\s+(?:暴力|violence|abuse)"),
            Pattern.compile("(?i)(?:虐待|abuse)\\s+(?:儿童|children|minors)"),

            // 极端主义模式
            Pattern.compile("(?i)(?:极端|extreme)\\s+(?:主义|ideology|beliefs?)"),
            Pattern.compile("(?i)(?:激进|radical)\\s+(?:思想|ideology|beliefs?)"),
            Pattern.compile("(?i)(?:宗教|religious)\\s+(?:极端|extremism)"),
            Pattern.compile("(?i)(?:圣战|jihad|holy\\s+war)"),

            // 网络安全威胁模式
            Pattern.compile("(?i)(?:DDoS|拒绝服务)\\s+(?:攻击|attack)"),
            Pattern.compile("(?i)(?:僵尸|bot)\\s+(?:网络|network)"),
            Pattern.compile("(?i)(?:勒索|ransom)\\s+(?:软件|ware|virus)"),
            Pattern.compile("(?i)(?:钓鱼|phishing)\\s+(?:网站|site|email)"),

            // 金融犯罪模式
            Pattern.compile("(?i)(?:内幕|insider)\\s+(?:交易|trading)"),
            Pattern.compile("(?i)(?:市场|market)\\s+(?:操纵|manipulation)"),
            Pattern.compile("(?i)(?:非法|illegal)\\s+(?:集资|fundraising)"),
            Pattern.compile("(?i)(?:地下|underground)\\s+(?:钱庄|banking)")
    );

    // ==================== 配置常量 ====================

    /**
     * 获取所有敏感词汇的总数
     */
    public static int getTotalSensitiveWordsCount() {
        return PROMPT_INJECTION_WORDS.size() +
                POLITICAL_SENSITIVE_WORDS.size() +
                ABUSIVE_WORDS.size() +
                SEXUAL_CONTENT_WORDS.size() +
                VIOLENCE_WORDS.size() +
                ILLEGAL_CONTENT_WORDS.size();
    }

    /**
     * 获取所有检测模式的总数
     */
    public static int getTotalPatternsCount() {
        return ADVANCED_INJECTION_PATTERNS.size() + COMPLIANCE_PATTERNS.size();
    }

    /**
     * 获取敏感词汇统计信息
     */
    public static java.util.Map<String, Integer> getSensitiveWordStats() {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        stats.put("prompt_injection_words", PROMPT_INJECTION_WORDS.size());
        stats.put("political_sensitive_words", POLITICAL_SENSITIVE_WORDS.size());
        stats.put("abusive_words", ABUSIVE_WORDS.size());
        stats.put("sexual_content_words", SEXUAL_CONTENT_WORDS.size());
        stats.put("violence_words", VIOLENCE_WORDS.size());
        stats.put("illegal_content_words", ILLEGAL_CONTENT_WORDS.size());
        stats.put("injection_patterns", ADVANCED_INJECTION_PATTERNS.size());
        stats.put("compliance_patterns", COMPLIANCE_PATTERNS.size());
        stats.put("total_words", getTotalSensitiveWordsCount());
        stats.put("total_patterns", getTotalPatternsCount());
        return stats;
    }
}