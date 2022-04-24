package io.shulie.takin.web.biz.job;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import io.shulie.takin.cloud.biz.checker.EngineEnvChecker;
import io.shulie.takin.job.annotation.ElasticSchedulerJob;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ElasticSchedulerJob(jobName = "pressureEnvInspectionJob", cron = "0 * * * * ?", description = "检测压力机环境是否正常")
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
            engineEnvChecker.check(null);
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
    private void nfsSpaceCheck() throws Exception {
        if (!StringUtils.isBlank(nfsMountPoint)) {
            File file = new File(nfsMountPoint);
            if (file.exists()) {
                Integer useRate = queryDiskInfo().getUseRate();
                if (new BigDecimal(String.valueOf(useRate)).compareTo(warningPercent) >= 0) {
                    throw new RuntimeException(String.format("NFS磁盘资源已占用%s%%, 请及时清理", useRate));
                }
            }
        }
    }

    private DiskInfo queryDiskInfo() throws Exception {
        Process pro = Runtime.getRuntime().exec("df -h " + nfsMountPoint);
        pro.waitFor();
        List<String> lines = new BufferedReader(new InputStreamReader(pro.getInputStream())).lines().collect(
            Collectors.toList());
        List<String> titles = getTitles(lines.get(0));
        String[] values = lines.get(1).split("\\s+");
        DiskInfo df = new DiskInfo();
        for (int i = 0; i < titles.size(); i++) {
            String title = titles.get(i);
            switch (title.toLowerCase()) {
                case "filesystem":
                    df.setFilesystem(values[i]);
                    break;
                case "used":
                    df.setUsed(Long.parseLong(values[i]));
                    break;
                case "available":
                    df.setAvailable(Long.parseLong(values[i]));
                    break;
                case "use%":
                    df.setUseRate(Integer.parseInt(values[i].replace("%", "")));
                    break;
            }
        }
        return df;
    }

    private static List<String> getTitles(String titlesLine) {
        List<String> titles = new ArrayList<>();
        String[] titleArray = titlesLine.split("\\s+");
        for (String title : titleArray) {
            if (title.equalsIgnoreCase("on")) {
                if (!titles.isEmpty()) {
                    int lastIdx = titles.size() - 1;
                    titles.set(lastIdx, titles.get(lastIdx) + "On");
                }
            } else {
                titles.add(title);
            }
        }
        return titles;
    }

    @Override
    public void afterPropertiesSet() {
        if (warningPercent.compareTo(WARNING_PERCENT_MIN) < 0 || warningPercent.compareTo(WARNING_PERCENT_MAX) > 0) {
            warningPercent = new BigDecimal(WARNING_PERCENT_DEFAULT);
        }
    }

    @Data
    private static class DiskInfo {
        private String filesystem;
        private Long used;
        private Long available;
        private Integer useRate;
    }
}
