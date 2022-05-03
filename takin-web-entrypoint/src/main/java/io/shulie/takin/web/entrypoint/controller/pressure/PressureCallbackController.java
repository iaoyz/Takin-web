package io.shulie.takin.web.entrypoint.controller.pressure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyParam;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.biz.notify.processor.jmeter.PressureErrorNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.jmeter.PressureHeartbeatNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.jmeter.PressureStartNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.jmeter.PressureStopNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.pod.PodErrorNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.pod.PodHeartbeatNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.pod.PodStartNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.pod.PodStopNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.sla.SlaNotifyParam;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.utils.json.JsonHelper;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/" + EntrypointUrl.MODULE_CALLBACK)
public class PressureCallbackController {

    @Resource
    private List<CloudNotifyProcessor<?>> processorList;

    @Resource(name = "cloudCallbackThreadPool")
    private ExecutorService cloudCallbackThreadPool;

    private Map<Integer, CloudNotifyProcessor<?>> processorMap;

    @PostMapping(EntrypointUrl.METHOD_ENGINE_CALLBACK_TASK_RESULT_NOTIFY)
    @ApiOperation(value = "cloud回调状态")
    public <T extends CloudNotifyParam> ResponseResult<?> taskResultNotify(@RequestParam Integer type,
        @RequestParam String sign, @RequestBody Map<String, Object> params) {
        CloudNotifyProcessor processor = processorMap.get(type);
        if (processor != null) {
            CloudNotifyParam notifyParam = parseParam(type, params);
            if (notifyParam != null) {
                cloudCallbackThreadPool.execute(() -> processor.process(notifyParam));
            }
        }
        return ResponseResult.success();
    }

    @PostConstruct
    private void init() {
        processorMap = new HashMap<>(8);
        if (!CollectionUtils.isEmpty(processorList)) {
            processorList.forEach(processor -> processorMap.putIfAbsent(processor.type().getCode(), processor));
        }
    }

    private CloudNotifyParam parseParam(Integer type, Map<String, Object> params) {
        CallbackType callbackType;
        if (type == null || Objects.isNull(callbackType = CallbackType.of(type))) {
            return null;
        }
        Class<? extends CloudNotifyParam> clazz = CloudNotifyParam.class;
        switch (callbackType) {
            case RESOURCE_EXAMPLE_HEARTBEAT:
                clazz = PodHeartbeatNotifyParam.class;
                break;
            case RESOURCE_EXAMPLE_START:
                clazz = PodStartNotifyParam.class;
                break;
            case RESOURCE_EXAMPLE_STOP:
                clazz = PodStopNotifyParam.class;
                break;
            case RESOURCE_EXAMPLE_ERROR:
                clazz = PodErrorNotifyParam.class;
                break;
            case JMETER_HEARTBEAT:
                clazz = PressureHeartbeatNotifyParam.class;
                break;
            case JMETER_START:
                clazz = PressureStartNotifyParam.class;
                break;
            case JMETER_STOP:
                clazz = PressureStopNotifyParam.class;
                break;
            case JMETER_ERROR:
                clazz = PressureErrorNotifyParam.class;
                break;
            case SLA:
                clazz = SlaNotifyParam.class;
                break;
        }
        return JsonHelper.json2Bean(JsonHelper.bean2Json(params), clazz);
    }
}
