package io.shulie.takin.web.biz.service;

import io.shulie.takin.web.biz.pojo.response.waterline.Metrics;
import io.shulie.takin.web.biz.pojo.response.waterline.TendencyChart;

import java.text.ParseException;
import java.util.List;

public interface WaterlineService {
    List<String> getAllActivityNames();

    List<String> getAllApplicationsByActivity(String activityName);

    List<Metrics> getAllApplicationWithMetrics(List<String> names, String startTime) throws ParseException;

    List<String> getAllApplicationsWithSceneId(Long sceneId);

    List<String> getApplicationNamesWithIds(List<String> ids);

    void getApplicationNodesAmount(List<Metrics> metrics);

    void getApplicationTags(List<Metrics> metrics, String tagName);

    List<TendencyChart> getTendencyChart(String applicationName, String startTime, String endTime, List<String> nodes) throws ParseException;

    List<String> getApplicationNodes(String applicationName);
}
