import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Asadchiy Pavel
 * on 19.01.16.
 */
public class StatisticFactory {
    private static final Logger LOG = Logger.getLogger(StatisticFactory.class.getName());
    private static final String STATISTIC_DIRECTORY_PATH = "stat";
    private static final int MAX_COUNT = 10;

    /**
     * @return true if and only if directory was created
     */
    public static boolean createDirectory() {
        File file = new File(STATISTIC_DIRECTORY_PATH);
        return (file.exists() && file.isDirectory()) || file.mkdirs();
    }

    public static void updateStatisticFile(final String fileStatPath) {
        File file = new File(fileStatPath);
        try {
            final boolean newFile = file.exists() || file.createNewFile();
            LOG.info("File " + fileStatPath + ", created/updated = " + newFile);
            try {
                String msg = new Date(System.currentTimeMillis()).toString() + "\n";
                Files.write(Paths.get(fileStatPath), msg.getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                LOG.severe("Can't append new time value in file, msg = " + e.getMessage());
            }
        } catch (IOException e) {
            LOG.severe("Can't create statistic file or write timestamp in it, msg = " + e.getMessage());
        }
    }

    public static void updateStatistic(final String filePath) {
        createDirectory();
        final String fileStatPath = STATISTIC_DIRECTORY_PATH + File.separatorChar +
                filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1, filePath.lastIndexOf(".")) + ".txt";
        LOG.info("Statistic file path " + fileStatPath);
        updateStatisticFile(fileStatPath);
    }

    public static int getAllBannerFiles() {
        final File[] banners = new File("banner").listFiles();
        return banners == null ? 0 : banners.length;
    }

    public static List<String> getLastNMinutesTopBanners(int minutes) throws IOException, ParseException {
        List<Banner> statistic = new ArrayList<>();
        File[] bannerFiles = new File(STATISTIC_DIRECTORY_PATH).listFiles();
        if (bannerFiles == null) {
            return new ArrayList<>();
        }
        for (File bannerFile : bannerFiles) {
            statistic.add(new Banner(bannerFile, minutes * 60 * 1000));
        }
        Collections.sort(statistic);
        Collections.reverse(statistic);
        List<String> res = new ArrayList<>();
        for (Banner aStatistic : statistic) {
            res.add("id = " + aStatistic.getId() + "\t\t\t" +
                    "was opened " + aStatistic.getOpenCount() + " times\t\t\t" +
                    "last opened " + aStatistic.getLastTime());
            if (res.size() > MAX_COUNT) {
                break;
            }
        }
        return res;
    }

    public static String getLastNminutesTopBannersString(int minutes) {
        try {
            String message = "Last " + minutes + " minutes (" + minutes * 1.0 / 60 + " hours) statistic:\n";
            final List<String> last2hoursTopBanners = getLastNMinutesTopBanners(minutes);
            for (String stat : last2hoursTopBanners) {
                message += stat + "\n";
            }
            return message;
        } catch (IOException | ParseException e) {
            LOG.severe("Can't get statistic data, msg = " + e.getMessage());
            return null;
        }
    }
}

class Banner implements Comparable<Banner> {
    private final String id;
    private final String lastTime;
    private final int openCount;

    public Banner(final File file, long actualOffset) throws IOException, ParseException {
        BufferedReader input = new BufferedReader(new FileReader(file));
        String strFile = file.getName();
        id = strFile.substring(strFile.lastIndexOf(File.separatorChar) + 1, strFile.lastIndexOf("."));
        String last = null;
        String line;
        int counter = 0;
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        while ((line = input.readLine()) != null) {
            last = line;
            if (simpleDateFormat.parse(line).getTime() + actualOffset > System.currentTimeMillis()) {
                ++counter;
            }
        }
        input.close();
        lastTime = last;
        openCount = counter;
    }

    public String getId() {
        return id;
    }

    public String getLastTime() {
        return lastTime;
    }

    public int getOpenCount() {
        return openCount;
    }

    @Override
    public int compareTo(Banner o) {
        return openCount - o.getOpenCount();
    }

    @Override
    public String toString() {
        return "Banner{" +
                "id='" + id + '\'' +
                ", lastTime='" + lastTime + '\'' +
                ", openCount=" + openCount +
                '}';
    }
}