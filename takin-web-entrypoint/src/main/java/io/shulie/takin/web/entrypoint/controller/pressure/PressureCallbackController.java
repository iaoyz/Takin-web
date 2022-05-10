package io.shulie.takin.web.entrypoint.controller.pressure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;

import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyParam;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/" + EntrypointUrl.MODULE_CALLBACK)
@Slf4j
public class PressureCallbackController {

    @Resource
    private List<CloudNotifyProcessor<?>> processorList;

    @Resource(name = "cloudCallbackThreadPool")
    private ExecutorService cloudCallbackThreadPool;

    private Map<CallbackType, CloudNotifyProcessor<?>> processorMap;

    @PostMapping(EntrypointUrl.METHOD_ENGINE_CALLBACK_TASK_RESULT_NOTIFY)
    @ApiOperation(value = "cloud回调状态")
    public <T extends CloudNotifyParam> ResponseResult<?> taskResultNotify(@RequestBody T param) {
        CloudNotifyProcessor processor = processorMap.get(param.getType());
        if (processor != null) {
            log.info("接口cloud回调:【{}】", JSON.toJSONString(param));
            cloudCallbackThreadPool.execute(() -> processor.process(param));
        }
        return ResponseResult.success("SUCCESS");
    }

    @PostConstruct
    private void init() {
        processorMap = new HashMap<>(8);
        if (!CollectionUtils.isEmpty(processorList)) {
            processorList.forEach(processor -> processorMap.putIfAbsent(processor.type(), processor));
        }
    }
}
