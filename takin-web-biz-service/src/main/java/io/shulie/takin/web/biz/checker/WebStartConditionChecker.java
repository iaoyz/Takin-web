package io.shulie.takin.web.biz.checker;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

public interface WebStartConditionChecker {

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

        // 合并两个相同类型的result
        public CheckResult merge(CheckResult other) {
            if (Objects.equals(getStatus(), other.getStatus())) {
                if (StringUtils.isNotBlank(other.getMessage())) {
                    this.setMessage(this.getMessage() + "|" + other.getMessage());
                }
                return this;
            }
            if (getStatus() == CheckStatus.FAIL.ordinal()) {
                return this;
            }
            if (other.getStatus() == CheckStatus.FAIL.ordinal()) {
                return other;
            }
            if (getStatus() == CheckStatus.SUCCESS.ordinal()) {
                return this;
            }
            if (other.getStatus() == CheckStatus.SUCCESS.ordinal()) {
                return other;
            }
            return this;
        }
    }

    enum CheckStatus {
        FAIL,
        SUCCESS,
        PENDING
    }
}
