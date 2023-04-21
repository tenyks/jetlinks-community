package org.jetlinks.community.network.manager.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author zhouhao
 * @since 1.0
 **/
@Service
public class NetworkConfigService extends GenericReactiveCrudService<NetworkConfigEntity, String>  {

    private static final Logger log = LoggerFactory.getLogger(NetworkConfigService.class);

    private final NetworkManager networkManager;

    public NetworkConfigService(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return this
            .findById(Flux.from(idPublisher))
            .flatMap(config -> networkManager
                .destroy(config.lookupNetworkType(), config.getId())
                .thenReturn(config.getId()))
            .as(super::deleteById)
            ;
    }


    public Mono<Void> start(String id) {
        log.info("即将启动网络组件：id={}", id);
        return this
            .findById(id)
            .switchIfEmpty(Mono.error(() -> new NotFoundException("error.configuration_does_not_exist", id)))
            .flatMap(conf -> this
                .createUpdate()
                .set(NetworkConfigEntity::getState, NetworkConfigState.enabled)
                .where(conf::getId)
                .execute()
                .thenReturn(conf))
            .flatMap(conf -> networkManager.reload(conf.lookupNetworkType(), id));
    }

    public Mono<Void> shutdown(String id) {
        return this
            .findById(id)
            .switchIfEmpty(Mono.error(() -> new NotFoundException("error.configuration_does_not_exist", id)))
            .flatMap(conf -> this
                .createUpdate()
                .set(NetworkConfigEntity::getState, NetworkConfigState.disabled)
                .where(conf::getId)
                .execute()
                .thenReturn(conf))
            .flatMap(conf -> networkManager.shutdown(conf.lookupNetworkType(), id));
    }
}
