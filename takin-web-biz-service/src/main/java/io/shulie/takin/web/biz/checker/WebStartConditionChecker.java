package io.shulie.takin.web.biz.checker;

import com.pamirs.takin.entity.domain.dto.scenemanage.SceneManageWrapperDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.Ordered;

public interface WebStartConditionChecker extends Ordered {

    default CheckResult preCheck(Long sceneId) {
        return CheckResult.success(type());
    }

    default void runningCheck(SceneManageWrapperDTO sceneData) {}

    String type();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class CheckResult {
        private String type;
        private Integer status;
        private String message;

        public static CheckResult success(String type) {
            CheckResult result = new CheckResult();
            result.setType(type);
            result.setStatus(CheckStatus.SUCCESS.ordinal());
            return result;
        }

        public static CheckResult fail(String type, String message) {
            CheckResult result = new CheckResult();
            result.setType(type);
            result.setStatus(CheckStatus.FAIL.ordinal());
            result.setMessage(message);
            return result;
        }

        public static CheckResult pending(String type) {
            CheckResult result = new CheckResult();
            result.setType(type);
            result.setStatus(CheckStatus.PENDING.ordinal());
            return result;
        }
    }

    enum CheckStatus {
        FAIL,
        SUCCESS,
        PENDING
    }
}
