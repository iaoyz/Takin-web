package io.shulie.takin.web.app.conf;

import javax.annotation.Resource;

import io.shulie.takin.web.biz.service.sys.VersionService;
import io.shulie.takin.web.data.model.mysql.VersionEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 自动发布当前版本
 */
@Component
public class VersionInitializer implements ApplicationListener<ApplicationStartedEvent> {

    @Value("${takin.web.version:}")
    private String version;

    @Value("${takin.web.upgrade.addr:}")
    private String url;

    @Value("${takin.web.upgrade.ignore-snapshot:true}")
    private boolean ignoreSnapshot;

    @Resource
    private VersionService versionService;

    /**
     * 新增控制台版本信息，并清除缓存
     *
     * @param event 事件源
     */
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        if (!ignore()) {
            VersionEntity entity = new VersionEntity();
            entity.setVersion(version);
            entity.setUrl(url);
            versionService.publish(entity);
        }
    }

    private boolean ignore() {
        return StringUtils.isBlank(version) || (ignoreSnapshot && StringUtils.endsWithIgnoreCase(version, "SNAPSHOT"));
    }
}
