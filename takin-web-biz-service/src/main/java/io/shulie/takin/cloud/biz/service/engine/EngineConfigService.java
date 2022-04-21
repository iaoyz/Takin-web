package io.shulie.takin.cloud.biz.service.engine;

/**
 * @author moriarty
 */
public interface EngineConfigService {

    /**
     * 获取需要挂载本地磁盘的场景ID
     *
     * @return -
     */
    String[] getLocalMountSceneIds();

}
