package org.jetlinks.community.network.parser;

import org.jetlinks.community.network.parser.PayloadParser;
import org.jetlinks.community.network.parser.PayloadParserType;
import org.jetlinks.community.network.parser.strateies.DelimitedPayloadParserBuilder;
import org.jetlinks.community.network.parser.strateies.FixLengthPayloadParserBuilder;
import org.jetlinks.community.network.parser.strateies.ScriptPayloadParserBuilder;
import org.jetlinks.community.ValueObject;

import java.util.function.Supplier;

/**
 * 解析器构造器策略，用于实现不同类型的解析器构造逻辑
 *
 * @author zhouhao
 * @since 1.0
 * @see FixLengthPayloadParserBuilder
 * @see DelimitedPayloadParserBuilder
 * @see ScriptPayloadParserBuilder
 */
public interface PayloadParserBuilderStrategy {
    /**
     * @return 解析器类型
     */
    PayloadParserType getType();

    /**
     * 构造解析器
     *
     * @param config 配置信息
     * @return 解析器
     */
    Supplier<PayloadParser> buildLazy(ValueObject config);

   default PayloadParser build(ValueObject config){
       return buildLazy(config).get();
   }
}
