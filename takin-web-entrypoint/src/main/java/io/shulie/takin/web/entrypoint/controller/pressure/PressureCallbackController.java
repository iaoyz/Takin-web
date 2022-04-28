package io.shulie.takin.web.entrypoint.controller.pressure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.cloud.biz.notify.CloudNotifyParam;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/" + EntrypointUrl.MODULE_ENGINE_CALLBACK)
public class PressureCallbackController {

    @Resource
    private List<CloudNotifyProcessor> processorList;

    private Map<String, CloudNotifyProcessor> processorMap;

    @PostMapping(EntrypointUrl.METHOD_ENGINE_CALLBACK_TASK_RESULT_NOTIFY)
    @ApiOperation(value = "cloud回调状态")
    public ResponseResult<?> taskResultNotify(@RequestBody CloudNotifyParam param) {
        CloudNotifyProcessor processor = processorMap.get(param.getType());
        if (processor != null) {
            return processor.process(param.getContext());
        }
        return ResponseResult.success();
    }

    @PostConstruct
    private void init() {
        processorMap = new HashMap<>(8);
        if (!CollectionUtils.isEmpty(processorList)) {
            processorList.forEach(processor -> processorMap.putIfAbsent(processor.type(), processor));
        }
    }
}
