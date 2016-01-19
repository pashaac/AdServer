import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


/**
 * Created by Asadchiy Pavel
 * on 19.01.16.
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = Logger.getLogger(ServerHandler.class.getName());
    private static final String ERROR_MSG = "404: Not Found";

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            LOG.info("URI = " + req.getUri());
            if (HttpHeaders.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }
            boolean keepAlive = HttpHeaders.isKeepAlive(req);
            final String filePath = req.getUri().substring(1) + ".html";
            boolean isActualFile;
            ByteBuf byteBuf;
            try {
                LOG.info("Search banner " + req.getUri() + ".html");
                final long fileCreatedTime = BannerFactory.getFileModifiedTime(filePath);
                isActualFile = BannerFactory.isActualBannerFile(fileCreatedTime);
                LOG.info("File " + req.getUri() + ".html was created " + new Date(fileCreatedTime));
                byteBuf = BannerFactory.getBannerBytesBuffer(filePath);
            } catch (IOException e) {
                LOG.severe("Incorrect path to file or can't get file created time, msg = " + e.getMessage());
                byteBuf = null;
                isActualFile = false;
            }
            if (req.getUri().equals("/")) {
                String message = "Application contains " + StatisticFactory.getAllBannerFiles() + " banners.\n\n\n" +
                        StatisticFactory.getLastNminutesTopBannersString(1) + "\n\n\n" +
                        StatisticFactory.getLastNminutesTopBannersString(60) + "\n\n\n" +
                        StatisticFactory.getLastNminutesTopBannersString(4 * 60) + "\n\n\n" +
                        StatisticFactory.getLastNminutesTopBannersString(24 * 60) + "\n\n\n";
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(message.getBytes()));
                response.headers().set(CONTENT_TYPE, "text/plain");
                response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                if (!keepAlive) {
                    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                } else {
                    response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                    ctx.write(response);
                }
                return;
            }
            if (byteBuf == null || !isActualFile) {
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND, Unpooled.wrappedBuffer(ERROR_MSG.getBytes()));
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                StatisticFactory.updateStatistic(filePath);
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
                response.headers().set(CONTENT_TYPE, "text/html");
                response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                if (!keepAlive) {
                    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                } else {
                    response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                    ctx.write(response);
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}