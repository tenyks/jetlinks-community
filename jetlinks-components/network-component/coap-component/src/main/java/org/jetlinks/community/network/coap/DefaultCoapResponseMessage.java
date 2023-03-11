package org.jetlinks.community.network.coap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public class DefaultCoapResponseMessage implements CoapResponseMessage {
    @Nonnull
    @Override
    public ByteBuf getPayload() {
        return null;
    }

    @Override
    public String payloadAsString() {
        return null;
    }

    @Override
    public JSONObject payloadAsJson() {
        return null;
    }

    @Override
    public JSONArray payloadAsJsonArray() {
        return null;
    }

    @Override
    public byte[] payloadAsBytes() {
        return new byte[0];
    }

    @Override
    public byte[] getBytes(int offset, int len) {
        return new byte[0];
    }
}
