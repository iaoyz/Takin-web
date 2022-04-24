package io.shulie.takin.web.biz.job;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import io.shulie.takin.cloud.biz.checker.EngineEnvChecker;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.job.annotation.ElasticSchedulerJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ElasticSchedulerJob(jobName = "pressureEnvInspectionJob", cron = "0 0 0 * * ?", description = "检测压力机环境是否正常")
@Slf4j
public class PressureEnvInspectionJob implements SimpleJob, InitializingBean {

    public static final String SCHEDULED_PRESSURE_ENV_KEY = "scheduled:pressure:env:check";

    public static final String NORMAL_STATE = "prefect";

    private static final String WARNING_PERCENT_DEFAULT = "80";

    private static final BigDecimal WARNING_PERCENT_MIN = new BigDecimal(WARNING_PERCENT_DEFAULT);

    private static final BigDecimal WARNING_PERCENT_MAX = new BigDecimal("95");

    @Resource
    private EngineEnvChecker engineEnvChecker;

    @Resource
    private RedisTemplate redisTemplate;

    @Value("${data.path}")
    private String nfsMountPoint;

    @Value("${nfs.warning.percent:" + WARNING_PERCENT_DEFAULT + "}")
    private BigDecimal warningPercent;

    @Override
    public void execute(ShardingContext shardingContext) {
        List<String> errorMessage = new ArrayList<>(2);
        try {
            engineEnvChecker.preCheck(null);
        } catch (Exception e) {
            errorMessage.add(e.getMessage());
        }
        try {
            nfsSpaceCheck();
        } catch (Exception e) {
            errorMessage.add(e.getMessage());
        }
        if (errorMessage.isEmpty()) {
            redisTemplate.opsForValue().set(SCHEDULED_PRESSURE_ENV_KEY, NORMAL_STATE);
        } else {
            redisTemplate.opsForValue().set(SCHEDULED_PRESSURE_ENV_KEY, StringUtils.join(errorMessage, ","));
        }
    }

    // 检测nfs空间是否足够
    private void nfsSpaceCheck() {
        if (!StringUtils.isBlank(nfsMountPoint)) {
            File file = new File(nfsMountPoint);
            if (file.exists()) {
                long totalSpace = file.getTotalSpace();
                long usableSpace = file.getUsableSpace();
                BigDecimal percent = new BigDecimal(String.valueOf(usableSpace)).divide(
                    new BigDecimal(String.valueOf(totalSpace)), 2, RoundingMode.HALF_EVEN);
                if (percent.compareTo(warningPercent) >= 0) {
                    long free = file.getFreeSpace() / 1024;
                    throw new RuntimeException(
                        String.format("NFS磁盘资源已占用%s%%, 剩余%sMB, 请及时清理", percent.toPlainString(), free));
                }
            }
        }
    }

    @Override
    public void afterPropertiesSet() {
        if (warningPercent.compareTo(WARNING_PERCENT_MIN) < 0 || warningPercent.compareTo(WARNING_PERCENT_MAX) > 0) {
            warningPercent = new BigDecimal(WARNING_PERCENT_DEFAULT);
        }
    }
}
