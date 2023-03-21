package org.jetlinks.community.network.coap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.MultiPart;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultCoapRequestMessage implements CoapRequestMessage {
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

    @Nonnull
    @Override
    public String getPath() {
        return CoapRequestMessage.super.getPath();
    }

    @Nonnull
    @Override
    public String getUrl() {
        return null;
    }

    @Nonnull
    @Override
    public HttpMethod getMethod() {
        return null;
    }

    @Nullable
    @Override
    public MediaType getContentType() {
        return null;
    }

    @Nonnull
    @Override
    public List<Header> getHeaders() {
        return null;
    }

    @Nullable
    @Override
    public Map<String, String> getQueryParameters() {
        return null;
    }

    @Nullable
    @Override
    public Map<String, String> getRequestParam() {
        return CoapRequestMessage.super.getRequestParam();
    }

    @Override
    public Optional<MultiPart> multiPart() {
        return CoapRequestMessage.super.multiPart();
    }

    @Override
    public Object parseBody() {
        return CoapRequestMessage.super.parseBody();
    }

    @Override
    public Optional<Header> getHeader(String name) {
        return CoapRequestMessage.super.getHeader(name);
    }

    @Override
    public Optional<String> getQueryParameter(String name) {
        return CoapRequestMessage.super.getQueryParameter(name);
    }

    @Override
    public String print() {
        return CoapRequestMessage.super.print();
    }
}
