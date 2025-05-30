package cn.garymb.ygomobile.utils;

import static cn.garymb.ygomobile.Constants.YDK_FILE_EX;
import static cn.garymb.ygomobile.ui.home.HomeActivity.pre_code_list;
import static cn.garymb.ygomobile.ui.home.HomeActivity.released_code_list;
import static cn.garymb.ygomobile.utils.ComparisonTableUtil.newIDsArray;
import static cn.garymb.ygomobile.utils.ComparisonTableUtil.oldIDsArray;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import cn.garymb.ygomobile.AppsSettings;
import cn.garymb.ygomobile.Constants;
import cn.garymb.ygomobile.bean.DeckType;
import cn.garymb.ygomobile.bean.events.DeckFile;
import cn.garymb.ygomobile.lite.R;
import cn.garymb.ygomobile.ui.cards.deck.DeckItemType;
import cn.hutool.core.util.ArrayUtil;

public class DeckUtil {

    private final static Comparator<DeckFile> nameCom = new Comparator<DeckFile>() {
        @Override
        public int compare(DeckFile ydk1, DeckFile ydk2) {

            if (!ydk1.getTypeName().equals(YGOUtil.s(R.string.category_Uncategorized))
                    && ydk2.getTypeName().equals(YGOUtil.s(R.string.category_Uncategorized)))
                return 1;
            else if (ydk1.getTypeName().equals(YGOUtil.s(R.string.category_Uncategorized))
                    && !ydk2.getTypeName().equals(YGOUtil.s(R.string.category_Uncategorized)))
                return -1;

            int id = ydk1.getTypeName().compareTo(ydk2.getTypeName());
            if (id == 0)
                return ydk1.getName().compareTo(ydk2.getName());
            else
                return id;
        }
    };

    private final static Comparator<DeckFile> dateInNameCom = new Comparator<DeckFile>() {
        @Override
        public int compare(DeckFile ydk1, DeckFile ydk2) {
            return ydk2.getName().compareTo(ydk1.getName());
        }
    };

    private final static Comparator<DeckFile> dateCom = new Comparator<DeckFile>() {
        @Override
        public int compare(DeckFile ydk1, DeckFile ydk2) {
            return ydk2.getDate().compareTo(ydk1.getDate());
        }
    };

    /**
     * 生成卡组类型的list
     *
     * @param context
     * @return
     */
    public static List<DeckType> getDeckTypeList(Context context) {
        List<DeckType> deckTypeList = new ArrayList<>();
        deckTypeList.add(new DeckType(YGOUtil.s(R.string.category_pack), AppsSettings.get().getPackDeckDir(), DeckType.ServerType.LOCAL));
        deckTypeList.add(new DeckType(YGOUtil.s(R.string.category_windbot_deck), AppsSettings.get().getAiDeckDir(), DeckType.ServerType.LOCAL));
        deckTypeList.add(new DeckType(YGOUtil.s(R.string.category_Uncategorized), AppsSettings.get().getDeckDir(), DeckType.ServerType.LOCAL));


        File[] files = new File(AppsSettings.get().getDeckDir()).listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deckTypeList.add(new DeckType(f.getName(), f.getAbsolutePath()));
                }
            }
        }
        return deckTypeList;
    }

    public static List<DeckFile> getDeckList(String path) {
        List<DeckFile> deckList = new ArrayList<>();
        File[] files = new File(path).listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(YDK_FILE_EX)) {
                    deckList.add(new DeckFile(file));
                }
            }
        }
        if (path.equals(AppsSettings.get().getPackDeckDir())) {
            Collections.sort(deckList, dateInNameCom);
        } else {
            Collections.sort(deckList, nameCom);
        }
        return deckList;
    }

    public static List<DeckFile> getDeckAllList() {
        return getDeckAllList(AppsSettings.get().getDeckDir());
    }

    public static List<DeckFile> getDeckAllList(String path) {
        return getDeckAllList(path, false);
    }

    public static List<DeckFile> getDeckAllList(String path, boolean isDir) {
        List<DeckFile> deckList = new ArrayList<>();
        File[] files = new File(path).listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deckList.addAll(getDeckAllList(file.getAbsolutePath(), true));
                }
                if (file.isFile() && file.getName().endsWith(YDK_FILE_EX)) {
                    deckList.add(new DeckFile(file));
                }
            }
        }
        if (!isDir) {
            Log.e("DeckUtil", "路径 " + path);
            Log.e("DeckUtil", "路径1 " + AppsSettings.get().getPackDeckDir());
            if (path.equals(AppsSettings.get().getPackDeckDir())) {
                Collections.sort(deckList, dateCom);
            } else {
                Collections.sort(deckList, nameCom);
            }
        }
        return deckList;
    }

    /**
     * 根据卡组绝对路径获取卡组分类名称
     *
     * @param deckPath
     * @return
     */
    public static String getDeckTypeName(String deckPath) {
        File file = new File(deckPath);
        if (file.exists()) {
            String name = file.getParentFile().getName();
            String lastName = file.getParentFile().getParentFile().getName();
            if (name.equals("pack") || name.equals("cacheDeck")) {
                //卡包
                return YGOUtil.s(R.string.category_pack);
            } else if (name.equals("Decks") && lastName.equals(Constants.WINDBOT_PATH)) {
                //ai卡组
                return YGOUtil.s(R.string.category_windbot_deck);
            } else if (name.equals("deck") && lastName.equals(Constants.PREF_DEF_GAME_DIR)) {
                //如果是deck并且上一个目录是ygocore的话，保证不会把名字为deck的卡包识别为未分类
                return YGOUtil.s(R.string.category_Uncategorized);
            } else {
                return name;
            }
        }
        return null;
    }

    //获取扩展卡的列表
    public static List<DeckFile> getExpansionsDeckList() throws IOException {
        AppsSettings appsSettings = AppsSettings.get();
        List<DeckFile> deckList = new ArrayList<>();
        File[] files = new File(appsSettings.getExpansionsPath(), Constants.CORE_DECK_PATH).listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(YDK_FILE_EX)) {
                    deckList.add(new DeckFile(file));
                }
            }
        }
        files = appsSettings.getExpansionFiles();
        for (File file : files) {
            if (file.isFile()) {
                ZipFile zipFile = null;
                try {
                    zipFile = new ZipFile(file.getAbsoluteFile());
                    Enumeration<?> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = (ZipEntry) entries.nextElement();
                        if (entry.getName().endsWith(YDK_FILE_EX)) {
                            String name = entry.getName();
                            name = name.substring(name.lastIndexOf("/"));
                            InputStream inputStream = zipFile.getInputStream(entry);
                            deckList.add(new DeckFile(IOUtils.asFile(inputStream, appsSettings.getCacheDeckDir() + "/" + name)));
                        }
                    }
                } finally {
                    IOUtils.close(zipFile);
                }
            }
        }
        Collections.sort(deckList, nameCom);
        return deckList;
    }

    public static int getFirstCardCode(String deckPath) {
        InputStreamReader in = null;
        try {
            in = new InputStreamReader(new FileInputStream(new File(deckPath)), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(in);
            String line = null;
            DeckItemType type = DeckItemType.Space;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("!side")) {
                    type = DeckItemType.SideCard;
                    continue;
                }
                if (line.startsWith("#")) {
                    if (line.startsWith("#main")) {
                        type = DeckItemType.MainCard;
                    } else if (line.startsWith("#extra")) {
                        type = DeckItemType.ExtraCard;
                    }
                    continue;
                }
                line = line.trim();
                if (line.length() == 0 || !TextUtils.isDigitsOnly(line)) {
                    if (Constants.DEBUG)
                        Log.w("kk", "read not number " + line);
                    continue;
                }
                Integer id = Integer.parseInt(line);
                if (released_code_list.contains(id)) {//先查看id对应的卡片密码是否在正式数组中存在
                    id = pre_code_list.get(released_code_list.indexOf(id));//替换成对应先行数组里的code
                }//执行完后变成先行密码，如果constants对照表里存在该密码，则如下又转换一次，所以发布app后必须及时更新在线对照表
                if (ArrayUtil.contains(oldIDsArray, id)) {
                    id = ArrayUtil.get(newIDsArray, ArrayUtil.indexOf(oldIDsArray, id));
                }
                return id;
            }
        } catch (IOException e) {
            Log.e("deckreader", "read 2", e);
        } finally {
            IOUtils.close(in);
        }

        return -1;
    }

}
