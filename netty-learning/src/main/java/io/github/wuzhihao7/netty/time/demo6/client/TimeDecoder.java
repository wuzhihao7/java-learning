package io.github.wuzhihao7.netty.time.demo6.client;

import io.github.wuzhihao7.netty.time.demo6.UnixTime;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class TimeDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes() < 8){
            return;
        }

        out.add(new UnixTime(in.readLong()));
    }
}
