package io.shulie.takin.web.biz.checker;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.Ordered;

public interface WebStartConditionChecker extends Ordered {

    default CheckResult check(WebConditionCheckerContext context) {
        return CheckResult.success(type());
    }

    String type();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class CheckResult {
        private String type;
        private Integer status;
        private String resourceId;
        private String message;

        public CheckResult(String type, Integer status, String message) {
            this.type = type;
            this.status = status;
            this.message = message;
        }

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

        public static String getCheckResultKey(Long sceneId) {
            return "scene:check:result:" + sceneId;
        }
    }

    enum CheckStatus {
        FAIL,
        SUCCESS,
        PENDING
    }
}
