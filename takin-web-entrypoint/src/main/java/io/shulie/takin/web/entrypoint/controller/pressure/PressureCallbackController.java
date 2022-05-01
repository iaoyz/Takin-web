package io.shulie.takin.web.entrypoint.controller.pressure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.cloud.biz.notify.CloudNotifyParam;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.biz.notify.NotifyContext;
import io.shulie.takin.cloud.biz.service.sla.SlaService;
import io.shulie.takin.cloud.common.bean.collector.SlaInfo;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskCallbackDAO;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskCallbackEntity;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.utils.json.JsonHelper;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Resource
    private PressureTaskCallbackDAO pressureTaskCallbackDAO;

    @Autowired
    private SlaService slaService;

    @PostMapping(EntrypointUrl.METHOD_ENGINE_CALLBACK_TASK_RESULT_NOTIFY)
    @ApiOperation(value = "cloud回调状态")
    public ResponseResult<?> taskResultNotify(@RequestBody CloudNotifyParam param) {
        CloudNotifyProcessor processor = processorMap.get(param.getType());
        if (processor != null) {
            ResponseResult<?> result = processor.process(param.getContext());
            saveCallback(param);
            return result;
        }
        return ResponseResult.success();
    }

    @PostMapping("/detection")
    public void detection(@RequestBody List<SlaInfo> slaInfo) {
        slaService.detection(slaInfo);
    }

    @PostConstruct
    private void init() {
        processorMap = new HashMap<>(8);
        if (!CollectionUtils.isEmpty(processorList)) {
            processorList.forEach(processor -> processorMap.putIfAbsent(processor.type(), processor));
        }
    }

    private void saveCallback(CloudNotifyParam param) {
        PressureTaskCallbackEntity entity = new PressureTaskCallbackEntity();
        entity.setType(param.getType());
        NotifyContext context = param.getContext();
        entity.setTime(context.getTime().getTime());
        entity.setPodId(context.getPodId());
        entity.setSource(JsonHelper.bean2Json(param));
        entity.setResourceId(Long.valueOf(context.getResourceId()));
        String taskId = context.getTaskId();
        if (StringUtils.isNotBlank(taskId)) {
            entity.setTaskId(Long.valueOf(taskId));
        }
        entity.setStatus(context.getStatus());
        pressureTaskCallbackDAO.save(entity);
    }
}
