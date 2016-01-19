import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by Asadchiy Pavel
 * on 19.01.16.
 */
public class BannerFactory {
    private static final long FILE_LIFE = 14 * 24 * 60 * 60 * 1000; // 2 week in millis

    public static ByteBuf getBannerBytesBuffer(final String filePath) throws IOException {
        return Unpooled.wrappedBuffer(Files.readAllBytes(Paths.get(filePath)));
    }

    private static BasicFileAttributes getFileAttributes(final String filePath) throws IOException {
        Path file = Paths.get(filePath);
        return Files.readAttributes(file, BasicFileAttributes.class);
    }

    /**
     * @param filePath - path to file
     * @return - last modified (created time same) time in milliseconds
     * @throws IOException
     */
    public static long getFileModifiedTime(final String filePath) throws IOException {
        return getFileAttributes(filePath).lastModifiedTime().toMillis();
    }

    public static boolean isActualBannerFile(final long createdTime) {
        return createdTime + FILE_LIFE > System.currentTimeMillis();
    }

}
