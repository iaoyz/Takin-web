package io.shulie.takin.web.entrypoint.controller.cloud.resources;

import io.shulie.takin.adapter.api.entrypoint.resource.CloudResourceApi;
import io.shulie.takin.cloud.biz.service.cloud.server.resource.CloudResourcesService;
import io.shulie.takin.web.common.common.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "压力机明细接口")
@RequestMapping("/api/cloud/resources")
public class CloudResourcesController {

    @Autowired
    private CloudResourceApi cloudResourceApi;

    @Autowired
    private CloudResourcesService cloudResourcesService;

    /**
     * @param taskId     启动压测任务后返回ID
     * @param resourceId 锁定资源后会返回资源ID
     * @return
     */
    @GetMapping("/getDetails")
    @ApiOperation("明细")
    public Response getDetails(
            @RequestParam("taskId") int taskId,
            @RequestParam("resourceId") String resourceId,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortType", required = false) String sortType,
            @RequestParam(value = "current", required = false) Integer currentPage,
            @RequestParam(value = "pageSize", required = false) Integer pageSize
    ) {
        return Response.success(cloudResourcesService.getDetail(cloudResourceApi.getDetails(taskId, resourceId), taskId, resourceId, sortField, sortType, currentPage, pageSize));
    }
}
