package io.github.wuzhihao7.netty.dns.dot;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.dns.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.NetUtil;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class DoTClient {

    private static final String QUERY_DOMAIN = "www.baidu.com";
    private static final int DNS_SERVER_PORT = 853;
    private static final String DNS_SERVER_HOST = "8.8.8.8";

    private DoTClient(){

    }

    private static void handleQueryResp(DefaultDnsResponse msg){
        if(msg.count(DnsSection.QUESTION) > 0){
            DnsQuestion question = msg.recordAt(DnsSection.QUESTION, 0);
            System.out.printf("name: %s%n", question.name());
        }

        for(int i = 0, count = msg.count(DnsSection.ANSWER); i < count; i++){
            DnsRecord record = msg.recordAt(DnsSection.ANSWER, i);
            if(record.type() == DnsRecordType.A){
                DnsRawRecord raw = (DnsRawRecord) record;
                System.out.println(NetUtil.bytesToIpAddress(ByteBufUtil.getBytes(raw.content())));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            final SslContext sslContext = SslContextBuilder.forClient()
                    .protocols("TLSv1.3", "TLSv1.2")
                    .build();
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(sslContext.newHandler(ch.alloc(), DNS_SERVER_HOST, DNS_SERVER_PORT))
                                    .addLast(new TcpDnsQueryEncoder())
                                    .addLast(new TcpDnsResponseDecoder())
                                    .addLast(new SimpleChannelInboundHandler<DefaultDnsResponse>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, DefaultDnsResponse msg) throws Exception {
                                            try {
                                                handleQueryResp(msg);
                                            }finally {
                                                ctx.close();
                                            }
                                        }
                                    });
                        }
                    });

            Channel channel = b.connect(DNS_SERVER_HOST, DNS_SERVER_PORT).sync().channel();
            int randomID = new Random().nextInt(60000 - 1000) + 1000;
            DnsQuery query = new DefaultDnsQuery(randomID, DnsOpCode.QUERY)
                    .setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(QUERY_DOMAIN, DnsRecordType.A));
            channel.writeAndFlush(query).sync();
            boolean success = channel.closeFuture().await(10, TimeUnit.SECONDS);
            if(!success){
                System.err.println("dns query timeout");
                channel.close().sync();
            }
        }finally {
            group.shutdownGracefully();
        }
    }
}
